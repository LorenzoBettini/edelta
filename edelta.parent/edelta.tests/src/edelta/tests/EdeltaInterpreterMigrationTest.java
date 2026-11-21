package edelta.tests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.function.Predicate;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.xtext.resource.XtextResourceSet;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.XtextRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Provider;

import edelta.edelta.EdeltaProgram;
import edelta.interpreter.EdeltaDefaultInterpreter;
import edelta.interpreter.EdeltaInterpreter;
import edelta.interpreter.EdeltaInterpreterWrapperException;
import edelta.lib.EdeltaDefaultRuntime;
import edelta.lib.EdeltaModelManager;
import edelta.lib.EdeltaModelMigrator;
import edelta.lib.EdeltaRuntime;
import edelta.resource.derivedstate.EdeltaCopiedEPackagesMap;
import edelta.tests.injectors.EdeltaInjectorProviderDerivedStateComputerWithoutInterpreter;

/**
 * Exercises model migration through the headless Xbase interpreter.
 *
 * @author Lorenzo Bettini
 */
@RunWith(XtextRunner.class)
@InjectWith(EdeltaInjectorProviderDerivedStateComputerWithoutInterpreter.class)
public class EdeltaInterpreterMigrationTest extends EdeltaAbstractTest {

	private static final String DATA = "../edelta.testdata/testdata/simpleTestData/";

	@Inject
	private Provider<XtextResourceSet> resourceSetProvider;

	@Inject
	private Injector injector;

	@Inject
	private EdeltaInterpreter defaultInterpreter;

	private EdeltaModelManager originalManager;

	private XtextResourceSet resourceSet;

	/**
	 * Counts actual migration-rule calls made by interpreted callbacks.
	 *
	 * @author Lorenzo Bettini
	 */
	private static class CountingMigrator extends EdeltaModelMigrator {
		private int copyRuleInvocations;

		private CountingMigrator(EdeltaModelManager originalManager,
				EdeltaModelManager evolvingManager, EdeltaCopiedEPackagesMap copies) {
			super(originalManager, evolvingManager, copies.getOriginalToCopyMap());
		}

		@Override
		public void copyRule(Predicate<EStructuralFeature> predicate, CopyProcedure procedure) {
			copyRuleInvocations++;
			super.copyRule(predicate, procedure);
		}
	}

	/**
	 * Uses the interpreter's Ecore copies when constructing a migration runtime.
	 *
	 * @author Lorenzo Bettini
	 */
	private static class MigrationInterpreter extends EdeltaDefaultInterpreter {
		private final EdeltaModelManager originalManager;
		private CountingMigrator migrator;
		private EPackage evolvingPackage;
		private Resource sandboxResource;

		private MigrationInterpreter(EdeltaModelManager originalManager) {
			this.originalManager = originalManager;
		}

		@Override
		protected EdeltaRuntime createRuntime(EdeltaCopiedEPackagesMap copies) {
			var evolvingManager = new EdeltaModelManager();
			for (var ePackage : copies.values()) {
				evolvingManager.loadEPackage(ePackage);
			}
			evolvingPackage = copies.get("mypackage");
			sandboxResource = evolvingPackage.eResource();
			migrator = new CountingMigrator(originalManager, evolvingManager, copies);
			return new EdeltaDefaultRuntime(migrator);
		}
	}

	@Before
	public void loadModelInstances() {
		originalManager = new EdeltaModelManager();
		var ecoreResource = originalManager.loadEcoreFile(DATA + "My.ecore");
		originalManager.loadModelFile(DATA + "MyClass.xmi");
		originalManager.loadModelFile(DATA + "MyRoot.xmi");
		resourceSet = resourceSetProvider.get();
		// The program must import the precise EPackage used by the loaded models.
		resourceSet.getResources().add(ecoreResource);
	}

	/**
	 * A direct Xbase callback must transform a value in the copied model.
	 */
	@Test
	public void directMigrationLambdaChangesModel() throws Exception {
		var program = parse("""
			metamodel "mypackage"
			modifyEcore evolve epackage mypackage {
				val attribute = ecoreref(MyClass.myClassStringAttribute)
				modelMigration[
					transformAttributeValueRule(isRelatedTo(attribute), [value |
						value.toString.toUpperCase
					])
				]
			}
			""");
		var interpreter = migrationInterpreter();
		interpreter.evaluateModifyEcoreOperations(program);
		interpreter.migrator.copyModels();

		var migrated = model(interpreter, "MyClass.xmi");
		var attribute = migrated.eClass().getEStructuralFeature("myClassStringAttribute");
		assertThat(migrated.eGet(attribute)).isEqualTo("MY ATTRIBUTE VALUE");
		assertSameEcore(program, interpreter);
	}

	/**
	 * The second rule must consume the first rule's result without losing references.
	 */
	@Test
	public void successiveRulesPreserveValuesAndReferences() throws Exception {
		var program = parse("""
			metamodel "mypackage"
			modifyEcore evolve epackage mypackage {
				val attribute = ecoreref(MyClass.myClassStringAttribute)
				modelMigration[
					transformAttributeValueRule(isRelatedTo(attribute), [value |
						value.toString.toUpperCase
					])
				]
				modelMigration[
					transformAttributeValueRule(isRelatedTo(attribute), [value |
						value.toString + "!"
					])
				]
			}
			""");
		var interpreter = migrationInterpreter();
		interpreter.evaluateModifyEcoreOperations(program);
		interpreter.migrator.copyModels();

		var migrated = model(interpreter, "MyClass.xmi");
		assertThat(migrated.eGet(migrated.eClass().getEStructuralFeature("myClassStringAttribute")))
			.isEqualTo("MY ATTRIBUTE VALUE!");
		var root = model(interpreter, "MyRoot.xmi");
		var contents = referenced(root, "myContents");
		assertThat(contents).hasSize(2);
		assertThat(referenced(root, "myReferences")).containsExactly(contents.get(1));
		assertThat(contents.get(0).eGet(contents.get(0).eClass()
			.getEStructuralFeature("myClassStringAttribute")))
			.isEqualTo("CONTAINED NOT REFERRED!");
		assertSameEcore(program, interpreter);
	}

	/**
	 * An imported refactoring must run its migration rule when making a reference single.
	 */
	@Test
	public void importedRefactoringExecutesItsMigrationRule() throws Exception {
		var originalRoot = originalModel("MyRoot.xmi");
		assertThat(originalRoot.eClass().getEStructuralFeature("myReferences").isMany()).isTrue();
		var contents = referenced(originalRoot, "myContents");
		// Give the many-valued reference two targets so retaining the first is observable.
		referenced(originalRoot, "myReferences").add(0, contents.get(0));
		resourceSet.getResource(createFileURIFromPath(METAMODEL_PATH + ECORE_ECORE), true);
		var program = parse("""
			import edelta.refactorings.lib.EdeltaRefactorings
			metamodel "mypackage"
			metamodel "ecore"
			use EdeltaRefactorings as extension refactorings
			modifyEcore evolve epackage mypackage {
				refactorings.changeToSingle(ecoreref(MyRoot.myReferences))
			}
			""");
		var interpreter = migrationInterpreter();
		interpreter.evaluateModifyEcoreOperations(program);
		interpreter.migrator.copyModels();

		var root = model(interpreter, "MyRoot.xmi");
		var firstContained = referenced(root, "myContents").get(0);
		var migratedReference = root.eClass().getEStructuralFeature("myReferences");
		assertThat(interpreter.migrator.copyRuleInvocations).isEqualTo(1);
		assertThat(migratedReference.isMany()).isFalse();
		assertThat(root.eGet(migratedReference)).isSameAs(firstContained);
		assertSameEcore(program, interpreter);
	}

	/**
	 * Without an opt-in runtime, Ecore changes still run and migration remains inactive.
	 */
	@Test
	public void withoutPreviewModelsMigrationRemainsInactive() throws Exception {
		var program = parse("""
			metamodel "mypackage"
			modifyEcore evolve epackage mypackage {
				ecoreref(MyClass).name = "Renamed"
				modelMigration[throw new IllegalStateException("must not execute")]
			}
			""");
		defaultInterpreter.evaluateModifyEcoreOperations(program);
		var copied = derivedStateHelper.getCopiedEPackagesMap(program.eResource()).get("mypackage");
		assertThat(copied.getEClassifier("Renamed")).isNotNull();
		assertThat(originalManager.getEPackage("mypackage").getEClassifier("MyClass"))
			.isNotNull();
	}

	/**
	 * A callback failure must propagate while the sandbox returns its EPackage.
	 */
	@Test
	public void callbackFailureRestoresCopiedPackageToProgram() throws Exception {
		var program = parse("""
			metamodel "mypackage"
			modifyEcore evolve epackage mypackage {
				modelMigration[throw new IllegalStateException("migration failed")]
			}
			""");
		var interpreter = migrationInterpreter();
		assertThatThrownBy(() -> interpreter.evaluateModifyEcoreOperations(program))
			.isInstanceOf(EdeltaInterpreterWrapperException.class)
			.hasCauseInstanceOf(IllegalStateException.class);
		assertThat(interpreter.evolvingPackage.eResource()).isSameAs(program.eResource());
		assertThat(interpreter.sandboxResource.getContents()).isEmpty();
	}

	private EdeltaProgram parse(String source) throws Exception {
		var program = parseHelper.parse(source, resourceSet);
		validationTestHelper.assertNoErrors(program);
		assertThat(program.getEPackages().getFirst())
			.isSameAs(originalManager.getEPackage("mypackage"));
		return program;
	}

	private MigrationInterpreter migrationInterpreter() {
		var interpreter = new MigrationInterpreter(originalManager);
		injector.injectMembers(interpreter);
		interpreter.setInterpreterTimeout(1200000);
		return interpreter;
	}

	private EObject originalModel(String fileName) {
		return originalManager.getModelResources().stream()
			.filter(r -> r.getURI().lastSegment().equals(fileName))
			.findFirst().orElseThrow().getContents().getFirst();
	}

	private EObject model(MigrationInterpreter interpreter, String fileName) {
		return interpreter.migrator.getEvolvingModelManager().getModelResources().stream()
			.filter(r -> r.getURI().lastSegment().equals(fileName))
			.findFirst().orElseThrow().getContents().getFirst();
	}

	@SuppressWarnings("unchecked")
	private List<EObject> referenced(EObject object, String featureName) {
		return (List<EObject>) object.eGet(object.eClass().getEStructuralFeature(featureName));
	}

	private void assertSameEcore(EdeltaProgram program, MigrationInterpreter interpreter) {
		var copied = derivedStateHelper.getCopiedEPackagesMap(program.eResource()).get("mypackage");
		assertThat(copied).isSameAs(interpreter.evolvingPackage);
		assertThat(copied.eResource()).isSameAs(program.eResource());
		assertThat(interpreter.sandboxResource.getContents()).isEmpty();
		var myClass = (EClass) copied.getEClassifier("MyClass");
		var myRoot = (EClass) copied.getEClassifier("MyRoot");
		assertThat(model(interpreter, "MyClass.xmi").eClass()).isSameAs(myClass);
		var root = model(interpreter, "MyRoot.xmi");
		assertThat(root.eClass()).isSameAs(myRoot);
		for (var child : referenced(root, "myContents")) {
			assertThat(child.eClass()).isSameAs(myClass);
		}
	}
}
