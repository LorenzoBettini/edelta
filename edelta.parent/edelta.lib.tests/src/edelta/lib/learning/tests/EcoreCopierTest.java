package edelta.lib.learning.tests;

import static edelta.testutils.EdeltaTestUtils.assertFilesAreEquals;
import static edelta.testutils.EdeltaTestUtils.cleanDirectoryAndFirstSubdirectories;
import static java.util.Collections.singletonList;
import static org.eclipse.emf.ecore.EcorePackage.Literals.EINT;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.assertj.core.api.Assertions;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.ENamedElement;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.EStructuralFeature.Setting;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.ecore.util.EcoreUtil.Copier;
import org.eclipse.emf.ecore.xmi.XMIResource;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import edelta.lib.EdeltaEPackageManager;
import edelta.lib.EdeltaResourceUtils;
import edelta.lib.EdeltaUtils;

public class EcoreCopierTest {

	private static final String ORIGINAL = "original/";
	private static final String TESTDATA = "testdata/";
	private static final String OUTPUT = "output/";
	private static final String EXPECTATIONS = "expectations/";

	EdeltaEPackageManager packageManagerOriginal;
	EdeltaEPackageManager packageManagerModified;

	/**
	 * A candidate for the copier used for model migration.
	 * 
	 * @author Lorenzo Bettini
	 *
	 */
	static class EdeltaEmfCopier extends Copier {
		private static final long serialVersionUID = 1L;

		/**
		 * @author Lorenzo Bettini
		 *
		 * @param <R> the type of the returned value when applying the function
		 */
		public static class ModelMigrator<R> {
			private Predicate<EObject> predicate;
			private Function<EObject, R> function;

			@SuppressWarnings("unchecked")
			public <T extends EObject> ModelMigrator(Predicate<T> predicate, Function<T, R> function) {
				this.predicate = (Predicate<EObject>) predicate;
				this.function = (Function<EObject, R>) function;
			}

			public boolean canApply(EObject arg) {
				return predicate.test(arg);
			}

			public R apply(EObject arg) {
				return function.apply(arg);
			}

			public static <R> ModelMigrator<R> migrateById(String id, Supplier<R> targetSupplier) {
				return new ModelMigrator<R>(
					e -> EdeltaUtils.getFullyQualifiedName(e).equals(id),
					e -> targetSupplier.get()
				);
			}

			public static <R> ModelMigrator<R> migrateById(String id, Function<EObject, R> function) {
				return new ModelMigrator<R>(
					e -> EdeltaUtils.getFullyQualifiedName(e).equals(id),
					e -> function.apply(e)
				);
			}
		}

		private Collection<EPackage> packages;

		private Collection<ModelMigrator<ENamedElement>> migrators = new ArrayList<>();

		private Collection<ModelMigrator<Object>> valueMigrators = new ArrayList<>();

		public EdeltaEmfCopier(Collection<EPackage> packages) {
			this.packages = packages;
		}

		public static EdeltaEmfCopier createFromResources(Collection<Resource> resources) {
			return new EdeltaEmfCopier(EdeltaResourceUtils.getEPackages(resources));
		}

		public void addMigrator(Predicate<ENamedElement> predicate, Function<ENamedElement, ENamedElement> function) {
			addMigrator(new ModelMigrator<>(predicate, function));
		}

		public void addEClassMigrator(Predicate<EClass> predicate, Function<EClass, EClass> function) {
			addMigrator(
				o -> EClass.class.isAssignableFrom(o.getClass())
						&& predicate.test((EClass) o),
				t -> function.apply((EClass) t));
		}

		public void addEStructuralFeatureMigrator(Predicate<EStructuralFeature> predicate, Function<EStructuralFeature, EStructuralFeature> function) {
			addMigrator(
				o -> EStructuralFeature.class.isAssignableFrom(o.getClass())
						&& predicate.test((EStructuralFeature) o),
				t -> function.apply((EStructuralFeature) t));
		}

		public void addMigrator(ModelMigrator<ENamedElement> migrator) {
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
			return getEPackageByName(eClass.getEPackage())
					.map(p -> p.getEClassifier(eClass.getName()))
					.filter(EClass.class::isInstance)
					.map(EClass.class::cast);
		}

		protected Optional<EPackage> getEPackageByName(EPackage ePackage) {
			return packages.stream()
					.filter(p -> p.getName().equals(ePackage.getName()))
					.findFirst();
		}

		public void addEAttributeMigrator(Predicate<EAttribute> predicate, Function<EObject, Object> function) {
			valueMigrators.add(
				new ModelMigrator<>(
					o -> predicate.test((EAttribute) o),
					function
				)
			);
		}

		public void addEAttributeMigrator(ModelMigrator<Object> migrator) {
			valueMigrators.add(migrator);
		}

		@Override
		protected void copyAttributeValue(EAttribute eAttribute, EObject eObject, Object value, Setting setting) {
			// we must getTarget(eAttribute) to canApply because the original
			// predicate might have been created with a renamed attribute
			// see testChangedAttributeNameAndType
			var targetAttribute = getTarget(eAttribute);
			var map = valueMigrators.stream()
				.filter(m -> m.canApply(targetAttribute) || m.canApply(eAttribute))
				.map(m -> m.apply(eObject));
			var newValue = map
				.findFirst()
				.orElse(value);
			super.copyAttributeValue(eAttribute, eObject, newValue, setting);
		}
	}

	@BeforeClass
	public static void clearOutput() throws IOException {
		cleanDirectoryAndFirstSubdirectories(OUTPUT);
	}

	@Before
	public void setup() {
		packageManagerOriginal = new EdeltaEPackageManager();
		packageManagerModified = new EdeltaEPackageManager();
	}

	/**
	 * This shows that renaming a class directly in the original
	 * package also affects immediately the loaded models.
	 */
	@Test
	public void testManualRenaming() {
		var subdir = "unchanged/";
		var originalPackage = EdeltaResourceUtils.getEPackage(
				packageManagerOriginal.loadEcoreFile(TESTDATA + subdir + "My.ecore"));
		var originalModelResource = packageManagerOriginal.loadEcoreFile(TESTDATA + subdir + "MyClass.xmi");
		var eObject = originalModelResource.getContents().get(0);
		assertEquals("MyClass", eObject.eClass().getName());
		originalPackage.getEClassifier("MyClass").setName("Changed");
		assertEquals("Changed", eObject.eClass().getName());
	}

	@Test
	public void testCopyUnchanged() throws IOException {
		var subdir = "unchanged/";
		var basedir = TESTDATA + subdir;
		packageManagerOriginal.loadEcoreFile(basedir + "My.ecore");
		var modifiedEcore = packageManagerModified.loadEcoreFile(basedir + "My.ecore");

		// models are loaded only in the original package manager
		packageManagerOriginal.loadModelFile(basedir + "MyRoot.xmi");
		packageManagerOriginal.loadModelFile(basedir + "MyClass.xmi");

		var copier = EdeltaEmfCopier.createFromResources(singletonList(modifiedEcore));
		copyModels(copier, basedir);

		var output = OUTPUT + subdir;
		packageManagerModified.saveEcores(output);
		packageManagerModified.saveModels(output);
		assertGeneratedFiles(subdir, output, "MyRoot.xmi");
		assertGeneratedFiles(subdir, output, "MyClass.xmi");
		assertGeneratedFiles(subdir, output, "My.ecore");
	}

	@Test
	public void testCopyMutualReferencesUnchanged() throws IOException {
		var subdir = "mutualReferencesUnchanged/";
		var basedir = TESTDATA + subdir;
		packageManagerOriginal.loadEcoreFile(basedir + "PersonForReferences.ecore");
		packageManagerOriginal.loadEcoreFile(basedir + "WorkPlaceForReferences.ecore");
		var modifiedEcore1 = packageManagerModified
				.loadEcoreFile(basedir + "PersonForReferences.ecore");
		var modifiedEcore2 = packageManagerModified
				.loadEcoreFile(basedir + "WorkPlaceForReferences.ecore");

		// models are loaded only in the original package manager
		packageManagerOriginal.loadModelFile(basedir + "Person1.xmi");
		packageManagerOriginal.loadModelFile(basedir + "Person2.xmi");
		packageManagerOriginal.loadModelFile(basedir + "WorkPlace1.xmi");

		var copier = EdeltaEmfCopier
			.createFromResources(
				List.of(modifiedEcore1, modifiedEcore2));
		copyModels(copier, basedir);

		var output = OUTPUT + subdir;
		packageManagerModified.saveEcores(output);
		packageManagerModified.saveModels(output);
		assertGeneratedFiles(subdir, output, "Person1.xmi");
		assertGeneratedFiles(subdir, output, "Person2.xmi");
		assertGeneratedFiles(subdir, output, "WorkPlace1.xmi");
		assertGeneratedFiles(subdir, output, "PersonForReferences.ecore");
		assertGeneratedFiles(subdir, output, "WorkPlaceForReferences.ecore");
	}

	@Test
	public void testCopyUnchangedClassesWithTheSameNameInDifferentPackages() throws IOException {
		var subdir = "classesWithTheSameName/";
		var basedir = TESTDATA + subdir;
		packageManagerOriginal.loadEcoreFile(basedir + "My1.ecore");
		packageManagerOriginal.loadEcoreFile(basedir + "My2.ecore");
		var modifiedEcore1 = packageManagerModified.loadEcoreFile(basedir + "My1.ecore");
		var modifiedEcore2 = packageManagerModified.loadEcoreFile(basedir + "My2.ecore");

		packageManagerOriginal.loadModelFile(basedir + "MyRoot1.xmi");
		packageManagerOriginal.loadModelFile(basedir + "MyClass1.xmi");
		packageManagerOriginal.loadModelFile(basedir + "MyRoot2.xmi");
		packageManagerOriginal.loadModelFile(basedir + "MyClass2.xmi");

		var copier = EdeltaEmfCopier.createFromResources
				(List.of(modifiedEcore1, modifiedEcore2));
		copyModels(copier, basedir);

		var output = OUTPUT + subdir;
		packageManagerModified.saveEcores(output);
		packageManagerModified.saveModels(output);
		assertGeneratedFiles(subdir, output, "MyRoot1.xmi");
		assertGeneratedFiles(subdir, output, "MyClass1.xmi");
		assertGeneratedFiles(subdir, output, "MyRoot2.xmi");
		assertGeneratedFiles(subdir, output, "MyClass2.xmi");
		assertGeneratedFiles(subdir, output, "My1.ecore");
		assertGeneratedFiles(subdir, output, "My2.ecore");
	}

	@Test
	public void testCopyRenamedClass() throws IOException {
		var subdir = "renamedClass/";
		var basedir = TESTDATA + subdir;
		packageManagerOriginal.loadEcoreFile(basedir + ORIGINAL + "My.ecore");
		var modifiedEcore = packageManagerModified.loadEcoreFile(basedir + "My.ecore");

		packageManagerOriginal.loadModelFile(basedir + ORIGINAL + "MyRoot.xmi");
		packageManagerOriginal.loadModelFile(basedir + ORIGINAL + "MyClass.xmi");

		var copier = EdeltaEmfCopier.createFromResources(singletonList(modifiedEcore));
		// this one is wrong since it returns an EStructuralFeature
		// but the copier should discard it
		copier.addMigrator(
			EdeltaEmfCopier.ModelMigrator.migrateById(
				"mypackage.MyRoot.myReferences",
					() -> packageManagerModified.getEPackage(
							"mypackage").getEClassifier("MyRootRenamed"))
		);
		// this one is wrong since it returns null
		// but the copier should discard it
		copier.addMigrator(
			EdeltaEmfCopier.ModelMigrator.migrateById(
				"mypackage.MyRoot",
					() -> null)
		);
		// this one is wrong since it returns null
		// but the copier should discard it
		copier.addMigrator(
			c -> c.getName().equals("MyClass"),
			c -> null
		);
		copier.addMigrator(
			EdeltaEmfCopier.ModelMigrator.migrateById(
				"mypackage.MyRoot",
					() -> packageManagerModified.getEPackage(
							"mypackage").getEClassifier("MyRootRenamed"))
		);
		// this will not match
		copier.addEClassMigrator(
			c -> c.getName().equals("Non Existent"),
			c -> getEClass(packageManagerModified,
					"mypackage", "MyClassRenamed")
		);
		copier.addEClassMigrator(
			c -> c.getName().equals("MyClass"),
			c -> getEClass(packageManagerModified,
					"mypackage", "MyClassRenamed")
		);
		copyModels(copier, basedir);

		var output = OUTPUT + subdir;
		packageManagerModified.saveEcores(output);
		packageManagerModified.saveModels(output);
		assertGeneratedFiles(subdir, output, "MyRoot.xmi");
		assertGeneratedFiles(subdir, output, "MyClass.xmi");
		assertGeneratedFiles(subdir, output, "My.ecore");
	}

	@Test
	public void testCopyRenamedFeature() throws IOException {
		var subdir = "renamedFeature/";
		var basedir = TESTDATA + subdir;
		packageManagerOriginal.loadEcoreFile(basedir + ORIGINAL + "My.ecore");
		var modifiedEcore = packageManagerModified.loadEcoreFile(basedir + "My.ecore");

		packageManagerOriginal.loadModelFile(basedir + ORIGINAL + "MyRoot.xmi");
		packageManagerOriginal.loadModelFile(basedir + ORIGINAL + "MyClass.xmi");

		var copier = EdeltaEmfCopier.createFromResources(singletonList(modifiedEcore));
		copier.addEStructuralFeatureMigrator(
			f -> {
				var name = f.getName();
				return name.equals("myContents");
			},
			f -> getFeature(packageManagerModified,
				"mypackage", "MyRoot", f.getName() + "Renamed")
		);
		copier.addMigrator(
			EdeltaEmfCopier.ModelMigrator.migrateById(
				"mypackage.MyRoot.myReferences",
				() -> getFeature(packageManagerModified,
						"mypackage", "MyRoot", "myReferencesRenamed")
			)
		);
		copyModels(copier, basedir);

		var output = OUTPUT + subdir;
		packageManagerModified.saveEcores(output);
		packageManagerModified.saveModels(output);
		assertGeneratedFiles(subdir, output, "MyRoot.xmi");
		assertGeneratedFiles(subdir, output, "MyClass.xmi");
		assertGeneratedFiles(subdir, output, "My.ecore");
	}

	@Test
	public void testCopyMutualReferencesRenamed() throws IOException {
		var subdir = "mutualReferencesUnchanged/";
		var basedir = TESTDATA + subdir;
		packageManagerOriginal.loadEcoreFile(basedir + "PersonForReferences.ecore");
		packageManagerOriginal.loadEcoreFile(basedir + "WorkPlaceForReferences.ecore");
		var modifiedEcore1 = packageManagerModified
				.loadEcoreFile(basedir + "PersonForReferences.ecore");
		var modifiedEcore2 = packageManagerModified
				.loadEcoreFile(basedir + "WorkPlaceForReferences.ecore");

		// models are loaded only in the original package manager
		packageManagerOriginal.loadModelFile(basedir + "Person1.xmi");
		packageManagerOriginal.loadModelFile(basedir + "Person2.xmi");
		packageManagerOriginal.loadModelFile(basedir + "WorkPlace1.xmi");

		var copier = EdeltaEmfCopier
			.createFromResources(
				List.of(modifiedEcore1, modifiedEcore2));

		// it's crucial all cross references are resolved before
		// performing any renaming, otherwise later the proxies (to the now renamed class)
		// won't be resolved during the copy of the models.
		EcoreUtil.resolveAll(modifiedEcore1.getResourceSet());

		renameElement(
				copier,
				getEClass(packageManagerModified, "personforreferences", "Person"),
				"PersonRenamed");

		copyModels(copier, basedir);

		subdir = "mutualReferencesRenamed/";
		var output = OUTPUT + subdir;
		packageManagerModified.saveEcores(output);
		packageManagerModified.saveModels(output);
		assertGeneratedFiles(subdir, output, "Person1.xmi");
		assertGeneratedFiles(subdir, output, "Person2.xmi");
		assertGeneratedFiles(subdir, output, "WorkPlace1.xmi");
		assertGeneratedFiles(subdir, output, "PersonForReferences.ecore");
		assertGeneratedFiles(subdir, output, "WorkPlaceForReferences.ecore");
	}

	@Test
	public void testCopyMutualReferencesRenamed2() throws IOException {
		var subdir = "mutualReferencesUnchanged/";
		var basedir = TESTDATA + subdir;
		packageManagerOriginal.loadEcoreFile(basedir + "PersonForReferences.ecore");
		packageManagerOriginal.loadEcoreFile(basedir + "WorkPlaceForReferences.ecore");
		var modifiedEcore1 = packageManagerModified
				.loadEcoreFile(basedir + "PersonForReferences.ecore");
		var modifiedEcore2 = packageManagerModified
				.loadEcoreFile(basedir + "WorkPlaceForReferences.ecore");

		// models are loaded only in the original package manager
		packageManagerOriginal.loadModelFile(basedir + "Person1.xmi");
		packageManagerOriginal.loadModelFile(basedir + "Person2.xmi");
		packageManagerOriginal.loadModelFile(basedir + "WorkPlace1.xmi");

		var copier = EdeltaEmfCopier
			.createFromResources(
				List.of(modifiedEcore1, modifiedEcore2));

		// it's crucial all cross references are resolved before
		// performing any renaming, otherwise later the proxies (to the now renamed class)
		// won't be resolved during the copy of the models.
		EcoreUtil.resolveAll(modifiedEcore1.getResourceSet());

		// execute this before...
		renameElement(
				copier,
				getFeature(packageManagerModified, "personforreferences", "Person", "works"),
				"workplace");
		// renaming the class
		renameElement(
				copier,
				getEClass(packageManagerModified, "personforreferences", "Person"),
				"PersonRenamed");
		renameElement(
				copier,
				getFeature(packageManagerModified, "WorkPlaceForReferences", "WorkPlace", "persons"),
				"employees");

		copyModels(copier, basedir);

		subdir = "mutualReferencesRenamed2/";
		var output = OUTPUT + subdir;
		packageManagerModified.saveEcores(output);
		packageManagerModified.saveModels(output);
		assertGeneratedFiles(subdir, output, "Person1.xmi");
		assertGeneratedFiles(subdir, output, "Person2.xmi");
		assertGeneratedFiles(subdir, output, "WorkPlace1.xmi");
		assertGeneratedFiles(subdir, output, "PersonForReferences.ecore");
		assertGeneratedFiles(subdir, output, "WorkPlaceForReferences.ecore");
	}

	@Test
	public void testChangedAttributeTypeWithoutProperMigration() {
		var subdir = "changedAttributeType/";
		var basedir = TESTDATA + subdir;
		packageManagerOriginal.loadEcoreFile(basedir + ORIGINAL + "My.ecore");
		var modifiedEcore = packageManagerModified.loadEcoreFile(basedir + "My.ecore");

		packageManagerOriginal.loadModelFile(basedir + ORIGINAL + "MyClass.xmi");

		var copier = EdeltaEmfCopier.createFromResources(singletonList(modifiedEcore));

		// actual refactoring
		getAttribute(packageManagerModified, "mypackage", "MyClass", "myAttribute")
			.setEType(EcorePackage.eINSTANCE.getEInt());

		Assertions.assertThatThrownBy(() -> copyModels(copier, basedir))
			.isInstanceOf(ClassCastException.class)
			.hasMessageContaining(
				"The value of type 'class java.lang.String' must be of type 'class java.lang.Integer'");
	}

	@Test
	public void testChangedAttributeType() throws IOException {
		var subdir = "changedAttributeType/";
		var basedir = TESTDATA + subdir;
		packageManagerOriginal.loadEcoreFile(basedir + ORIGINAL + "My.ecore");
		var modifiedEcore = packageManagerModified.loadEcoreFile(basedir + "My.ecore");

		packageManagerOriginal.loadModelFile(basedir + ORIGINAL + "MyClass.xmi");

		var copier = EdeltaEmfCopier.createFromResources(singletonList(modifiedEcore));

		// actual refactoring
		var attribute = getAttribute(packageManagerModified, "mypackage", "MyClass", "myAttribute");

		changeAttributeType(copier,
			attribute,
			EINT,
			o -> 
				// o is the old object,
				// so we must use the original feature to retrieve the value to copy
				// that is, don't use attribute, which is the one of the new package
				Integer.parseInt(
					o.eGet(o.eClass().getEStructuralFeature("myAttribute")).toString()));

		copyModels(copier, basedir);

		var output = OUTPUT + subdir;
		packageManagerModified.saveEcores(output);
		packageManagerModified.saveModels(output);
		assertGeneratedFiles(subdir, output, "MyClass.xmi");
		assertGeneratedFiles(subdir, output, "My.ecore");
	}

	@Test
	public void testChangedMultiAttributeType() throws IOException {
		var subdir = "changedMultiAttributeType/";
		var basedir = TESTDATA + subdir;
		packageManagerOriginal.loadEcoreFile(basedir + ORIGINAL + "My.ecore");
		var modifiedEcore = packageManagerModified.loadEcoreFile(basedir + "My.ecore");

		packageManagerOriginal.loadModelFile(basedir + ORIGINAL + "MyClass.xmi");

		var copier = EdeltaEmfCopier.createFromResources(singletonList(modifiedEcore));

		copier.addEAttributeMigrator(
			a -> a.getName().equals("myAttribute"),
			o -> 
			// o is the old object,
			// so we must use the original feature to retrieve the value to copy
			// that is, don't use attribute, which is the one of the new package
			((Collection<?>) o.eGet(o.eClass().getEStructuralFeature("myAttribute")))
				.stream()
				.map(Object::toString)
				.map(Integer::parseInt)
				.collect(Collectors.toList())
		);

		copyModels(copier, basedir);

		var output = OUTPUT + subdir;
		packageManagerModified.saveEcores(output);
		packageManagerModified.saveModels(output);
		assertGeneratedFiles(subdir, output, "MyClass.xmi");
		assertGeneratedFiles(subdir, output, "My.ecore");
	}

	@Test
	public void testChangedMultiAttributeType2() throws IOException {
		var subdir = "changedMultiAttributeType2/";
		var basedir = TESTDATA + subdir;
		packageManagerOriginal.loadEcoreFile(basedir + ORIGINAL + "My.ecore");
		var modifiedEcore = packageManagerModified.loadEcoreFile(basedir + "My.ecore");

		packageManagerOriginal.loadModelFile(basedir + ORIGINAL + "MyClass.xmi");

		var copier = EdeltaEmfCopier.createFromResources(singletonList(modifiedEcore));

		copier.addEAttributeMigrator(
			a -> a.getName().equals("myAttribute"),
			o -> 
			// o is the old object,
			// so we must use the original feature to retrieve the value to copy
			// that is, don't use attribute, which is the one of the new package
			((Collection<?>) o.eGet(o.eClass().getEStructuralFeature("myAttribute")))
				.stream()
				.map(Object::toString)
				.collect(Collectors.toList())
		);

		copyModels(copier, basedir);

		var output = OUTPUT + subdir;
		packageManagerModified.saveEcores(output);
		packageManagerModified.saveModels(output);
		assertGeneratedFiles(subdir, output, "MyClass.xmi");
		assertGeneratedFiles(subdir, output, "My.ecore");
	}

	/**
	 * This performs the refactoring manual: first change the name of the
	 * attribute then change the type.
	 * 
	 * @throws IOException
	 */
	@Test
	public void testChangedAttributeNameAndType() throws IOException {
		var subdir = "changedAttributeType/";
		var basedir = TESTDATA + subdir + ORIGINAL;
		packageManagerOriginal.loadEcoreFile(basedir + "My.ecore");
		var modifiedEcore = packageManagerModified.loadEcoreFile(basedir + "My.ecore");

		packageManagerOriginal.loadModelFile(basedir + "MyClass.xmi");

		var copier = EdeltaEmfCopier.createFromResources(singletonList(modifiedEcore));

		// actual refactoring
		var attribute = getAttribute(packageManagerModified, "mypackage", "MyClass", "myAttribute");
		// first rename
		renameElement(copier, attribute, "newName");
		// then change type
		changeAttributeType(copier,
			attribute,
			EINT,
			o -> 
				// o is the old object,
				// so we must use the original feature to retrieve the value to copy
				// that is, don't use attribute, which is the one of the new package
				Integer.parseInt(
					o.eGet(o.eClass().getEStructuralFeature("myAttribute")).toString()));

		copyModels(copier, basedir);

		subdir = "changedAttributeNameAndType/";
		var output = OUTPUT + subdir;
		packageManagerModified.saveEcores(output);
		packageManagerModified.saveModels(output);
		assertGeneratedFiles(subdir, output, "MyClass.xmi");
		assertGeneratedFiles(subdir, output, "My.ecore");
	}

	/**
	 * This performs the refactoring manual: first change the type of the
	 * attribute then change the name.
	 * 
	 * @throws IOException
	 */
	@Test
	public void testChangedAttributeTypeAndName() throws IOException {
		var subdir = "changedAttributeType/";
		var basedir = TESTDATA + subdir + ORIGINAL;
		packageManagerOriginal.loadEcoreFile(basedir + "My.ecore");
		var modifiedEcore = packageManagerModified.loadEcoreFile(basedir + "My.ecore");

		packageManagerOriginal.loadModelFile(basedir + "MyClass.xmi");

		var copier = EdeltaEmfCopier.createFromResources(singletonList(modifiedEcore));

		// actual refactoring
		var attribute = getAttribute(packageManagerModified, "mypackage", "MyClass", "myAttribute");
		// first change type
		changeAttributeType(copier,
			attribute,
			EINT,
			o -> 
				// o is the old object,
				// so we must use the original feature to retrieve the value to copy
				// that is, don't use attribute, which is the one of the new package
				Integer.parseInt(
					o.eGet(o.eClass().getEStructuralFeature("myAttribute")).toString()));
		// then rename
		renameElement(copier, attribute, "newName");

		copyModels(copier, basedir);

		subdir = "changedAttributeTypeAndName/";
		var output = OUTPUT + subdir;
		packageManagerModified.saveEcores(output);
		packageManagerModified.saveModels(output);
		assertGeneratedFiles(subdir, output, "MyClass.xmi");
		assertGeneratedFiles(subdir, output, "My.ecore");
	}

	@Test
	public void testMergetAttributes() throws IOException {
		var subdir = "mergeAttributes/";
		var basedir = TESTDATA + subdir;
		packageManagerOriginal.loadEcoreFile(basedir + "Person.ecore");
		var modifiedEcore = packageManagerModified.loadEcoreFile(basedir + "Person.ecore");

		packageManagerOriginal.loadModelFile(basedir + "Person.xmi");

		var copier = EdeltaEmfCopier
				.createFromResources(singletonList(modifiedEcore));

		var firstName = getAttribute(packageManagerModified,
				"person", "Person", "firstname");
		var lastName = getAttribute(packageManagerModified,
				"person", "Person", "lastname");
		// refactoring
		EcoreUtil.remove(lastName);
		// rename the first attribute among the ones to merge
		renameElement(copier, firstName, "fullName");
		// specify the converter using firstname and lastname original values
		copier.addEAttributeMigrator(
			a -> a.getName().equals("fullName"),
			o -> 
			// o is the old object,
			// so we must use the original feature to retrieve the value to copy
			// that is, don't use attribute, which is the one of the new package
			o.eGet(o.eClass().getEStructuralFeature("firstname")) +
			" " +
			o.eGet(o.eClass().getEStructuralFeature("lastname"))
		);

		copyModels(copier, basedir);

		var output = OUTPUT + subdir;
		packageManagerModified.saveEcores(output);
		packageManagerModified.saveModels(output);
		assertGeneratedFiles(subdir, output, "Person.xmi");
		assertGeneratedFiles(subdir, output, "Person.ecore");
	}

	/**
	 * This simulates the renameElement in our library
	 * 
	 * @param copier
	 * @param e
	 * @param newName
	 */
	private void renameElement(EdeltaEmfCopier copier, ENamedElement e, String newName) {
		var originalId = EdeltaUtils.getFullyQualifiedName(e);
		e.setName(newName);
		copier.addMigrator(
			EdeltaEmfCopier.ModelMigrator.migrateById(
				originalId,
				() -> e
			));
	}

	/**
	 * This simulates the changeAttributeType in our library
	 * 
	 * @param copier
	 * @param attribute
	 * @param type
	 * @param valueConverter
	 */
	private void changeAttributeType(EdeltaEmfCopier copier, EAttribute attribute, EDataType type, Function<EObject, Object> valueConverter) {
		var originalId = EdeltaUtils.getFullyQualifiedName(attribute);
		attribute.setEType(type);
		copier.addEAttributeMigrator(
			EdeltaEmfCopier.ModelMigrator.migrateById(
				originalId,
				valueConverter
			)
		);
	}

	private EAttribute getAttribute(EdeltaEPackageManager packageManager, String packageName, String className, String attributeName) {
		return (EAttribute) getFeature(packageManager, packageName, className, attributeName);
	}

	private EStructuralFeature getFeature(EdeltaEPackageManager packageManager, String packageName, String className, String featureName) {
		return getEClass(packageManager, packageName, className)
				.getEStructuralFeature(featureName);
	}

	private EClass getEClass(EdeltaEPackageManager packageManager, String packageName, String className) {
		return (EClass) packageManager.getEPackage(
				packageName).getEClassifier(className);
	}

	private void assertGeneratedFiles(String subdir, String outputDir, String fileName) throws IOException {
		assertFilesAreEquals(
			EXPECTATIONS + subdir + fileName,
			outputDir + fileName);
	}

	/**
	 * This simulates what the final model migration should do.
	 * 
	 * IMPORTANT: the original Ecores and models must be in a subdirectory
	 * of the directory that stores the modified Ecores.
	 * 
	 * It is crucial to strip the original path and use the baseDir
	 * to create the new {@link Resource} URI, so that, upon saving,
	 * the schema location is computed correctly.
	 * 
	 * @param copier
	 */
	private void copyModels(EdeltaEmfCopier copier, String baseDir) {
		var map = packageManagerOriginal.getModelResourceMap();
		for (var entry : map.entrySet()) {
			var originalResource = (XMIResource) entry.getValue();
			var p = Paths.get(entry.getKey());
			final var fileName = p.getFileName().toString();
			var newResource = packageManagerModified.createModelResource
				(baseDir + fileName, originalResource);
			var root = originalResource.getContents().get(0);
			newResource.getContents().add(copier.copy(root));
		}
		copier.copyReferences();
	}
}
