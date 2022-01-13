package edelta.lib.learning.tests;

import static edelta.testutils.EdeltaTestUtils.assertFilesAreEquals;
import static edelta.testutils.EdeltaTestUtils.cleanDirectoryAndFirstSubdirectories;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.ENamedElement;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.util.EcoreUtil.Copier;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import edelta.lib.EdeltaDefaultRuntime;
import edelta.lib.EdeltaUtils;

public class EcoreCopierTest {

	private static final String MODIFIED = "modified/";
	private static final String ORIGINAL = "original/";
	private static final String TESTDATA = "testdata/";
	private static final String OUTPUT = "output/";
	private static final String EXPECTATIONS = "expectations/";

	EdeltaDefaultRuntime runtimeForOriginal;
	EdeltaDefaultRuntime runtimeForModified;

	/**
	 * A candidate for the copier used for model migration.
	 * 
	 * @author Lorenzo Bettini
	 *
	 */
	static class EdeltaEmfCopier extends Copier {
		private static final long serialVersionUID = 1L;

		public static class ModelMigrator {
			private Predicate<EObject> predicate;
			private Function<EObject, EObject> function;

			@SuppressWarnings("unchecked")
			public <T extends EObject> ModelMigrator(Predicate<T> predicate, Function<T, T> function) {
				this.predicate = (Predicate<EObject>) predicate;
				this.function = (Function<EObject, EObject>) function;
			}

			public boolean canApply(EObject arg) {
				return predicate.test(arg);
			}

			public EObject apply(EObject arg) {
				return function.apply(arg);
			}

			public static ModelMigrator migrateById(String id, Supplier<EObject> targetSupplier) {
				return new ModelMigrator(
					e -> EdeltaUtils.getFullyQualifiedName(e).equals(id),
					e -> targetSupplier.get()
				);
			}
		}

		private Collection<EPackage> packages;

		private Collection<ModelMigrator> migrators = new ArrayList<>();

		public EdeltaEmfCopier(Collection<EPackage> packages) {
			this.packages = packages;
		}

		public EdeltaEmfCopier(Resource resource) {
			this(List.of((EPackage) resource.getContents().get(0)));
		}

		public void addMigrator(Predicate<ENamedElement> predicate, Function<ENamedElement, ENamedElement> function) {
			addMigrator(new ModelMigrator(predicate, function));
		}

		public void addMigrator(ModelMigrator migrator) {
			migrators.add(migrator);
		}

		@Override
		protected EClass getTarget(EClass eClass) {
			return getTargetEClass(eClass)
				.orElse(null);
		}

		protected Optional<EClass> getTargetEClass(EClass eClass) {
			var map = migrators.stream()
				.filter(m -> m.canApply(eClass))
				.map(m -> m.apply(eClass));
			return EdeltaUtils.filterByType(map, EClass.class)
				.findFirst()
				.or(() -> getEClassByName(eClass));
		}

		@Override
		protected EStructuralFeature getTarget(EStructuralFeature feature) {
			return getTargetEStructuralFeature(feature)
				.orElse(null);
		}

		protected Optional<EStructuralFeature> getTargetEStructuralFeature(EStructuralFeature feature) {
			var map = migrators.stream()
				.filter(m -> m.canApply(feature))
				.map(m -> m.apply(feature));
			return EdeltaUtils.filterByType(map, EStructuralFeature.class)
				.findFirst()
				.or(() -> getTargetEClass(feature.getEContainingClass())
						.flatMap(c -> getEStructuralFeatureByName(c, feature)));
		}

		protected Optional<EStructuralFeature> getEStructuralFeatureByName(EClass c, EStructuralFeature feature) {
			return Optional.ofNullable(c.getEStructuralFeature(feature.getName()));
		}

		protected Optional<EClass> getEClassByName(EClass eClass) {
			return packages.stream()
					.map(p -> p.getEClassifier(eClass.getName()))
					.filter(EClass.class::isInstance)
					.findFirst()
					.map(EClass.class::cast);
		}
	}

	@BeforeClass
	public static void clearOutput() throws IOException {
		cleanDirectoryAndFirstSubdirectories(OUTPUT);
	}

	@Before
	public void setup() {
		runtimeForOriginal = new EdeltaDefaultRuntime();
		runtimeForModified = new EdeltaDefaultRuntime();
	}

	@Test
	public void testCopyUnchanged() throws IOException {
		var subdir = "unchanged/";
		// in this case we load the same models with twice in different resource sets
		runtimeForOriginal.loadEcoreFile(TESTDATA + subdir + "My.ecore");
		var modifiedEcore = runtimeForModified.loadEcoreFile(TESTDATA + subdir + "My.ecore");

		// this is actually XMI
		var original = runtimeForOriginal.loadEcoreFile(TESTDATA + subdir + "MyRoot.xmi");
		var original2 = runtimeForOriginal.loadEcoreFile(TESTDATA + subdir + "MyClass.xmi");
		var modified = runtimeForModified.loadEcoreFile(TESTDATA + subdir + "MyRoot.xmi");
		var modified2 = runtimeForModified.loadEcoreFile(TESTDATA + subdir + "MyClass.xmi");

		var copier = new EdeltaEmfCopier(modifiedEcore);
		copyIntoModified(copier, original, modified);
		copyIntoModified(copier, original2, modified2);
		copier.copyReferences();

		var output = OUTPUT + subdir;
		runtimeForModified.saveModifiedEcores(output);
		assertGeneratedFiles(subdir, output, "MyRoot.xmi");
		assertGeneratedFiles(subdir, output, "MyClass.xmi");
		assertGeneratedFiles(subdir, output, "My.ecore");
	}

	@Test
	public void testCopyRenamedClass() throws IOException {
		var subdir = "renamedClass/";
		runtimeForOriginal.loadEcoreFile(TESTDATA + subdir + ORIGINAL + "My.ecore");
		var modifiedEcore = runtimeForModified.loadEcoreFile(TESTDATA + subdir + MODIFIED + "My.ecore");

		// this is actually XMI
		var original = runtimeForOriginal.loadEcoreFile(TESTDATA + subdir + ORIGINAL + "MyRoot.xmi");
		var original2 = runtimeForOriginal.loadEcoreFile(TESTDATA + subdir + ORIGINAL + "MyClass.xmi");
		var modified = runtimeForModified.loadEcoreFile(TESTDATA + subdir + MODIFIED + "MyRoot.xmi");
		var modified2 = runtimeForModified.loadEcoreFile(TESTDATA + subdir + MODIFIED + "MyClass.xmi");

		var copier = new EdeltaEmfCopier(modifiedEcore);
		copier.addMigrator(
			EdeltaEmfCopier.ModelMigrator.migrateById(
				"mypackage.MyRoot",
					() -> runtimeForModified.getEClass(
							"mypackage", "MyRootRenamed"))
		);
		copier.addMigrator(
			c -> c.getName().equals("MyClass"),
			c -> runtimeForModified.getEClass(
					"mypackage", "MyClassRenamed")
		);
		copyIntoModified(copier, original, modified);
		copyIntoModified(copier, original2, modified2);
		copier.copyReferences();

		var output = OUTPUT + subdir;
		runtimeForModified.saveModifiedEcores(output);
		assertGeneratedFiles(subdir, output, "MyRoot.xmi");
		assertGeneratedFiles(subdir, output, "MyClass.xmi");
		assertGeneratedFiles(subdir, output, "My.ecore");
	}

	@Test
	public void testCopyRenamedFeature() throws IOException {
		var subdir = "renamedFeature/";
		runtimeForOriginal.loadEcoreFile(TESTDATA + subdir + ORIGINAL + "My.ecore");
		var modifiedEcore = runtimeForModified.loadEcoreFile(TESTDATA + subdir + MODIFIED + "My.ecore");

		// this is actually XMI
		var original = runtimeForOriginal.loadEcoreFile(TESTDATA + subdir + ORIGINAL + "MyRoot.xmi");
		var original2 = runtimeForOriginal.loadEcoreFile(TESTDATA + subdir + ORIGINAL + "MyClass.xmi");
		var modified = runtimeForModified.loadEcoreFile(TESTDATA + subdir + MODIFIED + "MyRoot.xmi");
		var modified2 = runtimeForModified.loadEcoreFile(TESTDATA + subdir + MODIFIED + "MyClass.xmi");

		var copier = new EdeltaEmfCopier(modifiedEcore);
		copier.addMigrator(
			f -> {
				var name = f.getName();
				return name.equals("myContents");
			},
			f -> runtimeForModified.getEStructuralFeature(
					"mypackage", "MyRoot", f.getName() + "Renamed")
		);
		copier.addMigrator(
			EdeltaEmfCopier.ModelMigrator.migrateById(
				"mypackage.MyRoot.myReferences",
				() -> runtimeForModified.getEStructuralFeature(
						"mypackage", "MyRoot", "myReferencesRenamed")
			)
		);
		copyIntoModified(copier, original, modified);
		copyIntoModified(copier, original2, modified2);
		copier.copyReferences();

		var output = OUTPUT + subdir;
		runtimeForModified.saveModifiedEcores(output);
		assertGeneratedFiles(subdir, output, "MyRoot.xmi");
		assertGeneratedFiles(subdir, output, "MyClass.xmi");
		assertGeneratedFiles(subdir, output, "My.ecore");
	}

	private void assertGeneratedFiles(String subdir, String output, String fileName) throws IOException {
		assertFilesAreEquals(
			EXPECTATIONS + subdir + fileName,
			output + fileName);
	}

	/**
	 * The modified resource is pre-populated with the contents of the testdata
	 * directory but it will be completely cleared and replaced with the copy taken
	 * from the original resource.
	 * 
	 * @param copier
	 * @param original
	 * @param modified
	 */
	private void copyIntoModified(EdeltaEmfCopier copier, Resource original, Resource modified) {
		var root = original.getContents().get(0);
		var copy = copier.copy(root);
		modified.getContents().clear();
		modified.getContents().add(copy);
	}
}
