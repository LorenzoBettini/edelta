package edelta.lib.learning.tests;

import static edelta.testutils.EdeltaTestUtils.assertFilesAreEquals;
import static edelta.testutils.EdeltaTestUtils.cleanDirectoryAndFirstSubdirectories;
import static java.util.Collections.singletonList;
import static java.util.Comparator.comparing;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

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

import edelta.lib.EdeltaEPackageManager;
import edelta.lib.EdeltaResourceUtils;
import edelta.lib.EdeltaUtils;

public class EcoreCopierTest {

	private static final String MODIFIED = "modified/";
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

		public static EdeltaEmfCopier createFromResources(Collection<Resource> resources) {
			return new EdeltaEmfCopier(getEPackages(resources));
		}

		/**
		 * TODO: extract into EdeltaUtils, since it's used also in
		 * EdeltaDependencyAnalyzer
		 * 
		 * @param resources
		 * @return
		 */
		private static Collection<EPackage> getEPackages(Collection<Resource> resources) {
			return resources.stream()
				.map(r -> (EPackage) r.getContents().get(0))
				.sorted(ePackageComparator()) // we must be deterministic
				.collect(Collectors.toList());
		}

		private static Comparator<EPackage> ePackageComparator() {
			return comparing(EPackage::getNsURI);
		}

		public void addMigrator(Predicate<ENamedElement> predicate, Function<ENamedElement, ENamedElement> function) {
			addMigrator(new ModelMigrator(predicate, function));
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
		// in this case we load the same models twice in different resource sets
		packageManagerOriginal.loadEcoreFile(TESTDATA + subdir + "My.ecore");
		var modifiedEcore = packageManagerModified.loadEcoreFile(TESTDATA + subdir + "My.ecore");

		// this is actually XMI
		var original = packageManagerOriginal.loadModelFile(TESTDATA + subdir + "MyRoot.xmi");
		var original2 = packageManagerOriginal.loadModelFile(TESTDATA + subdir + "MyClass.xmi");
		var modified = packageManagerModified.loadModelFile(TESTDATA + subdir + "MyRoot.xmi");
		var modified2 = packageManagerModified.loadModelFile(TESTDATA + subdir + "MyClass.xmi");

		var copier = EdeltaEmfCopier.createFromResources(singletonList(modifiedEcore));
		copyIntoModified(copier, original, modified);
		copyIntoModified(copier, original2, modified2);
		copier.copyReferences();

		var output = OUTPUT + subdir;
		packageManagerModified.saveEcores(output);
		packageManagerModified.saveModels(output);
		assertGeneratedFiles(subdir, output, "MyRoot.xmi");
		assertGeneratedFiles(subdir, output, "MyClass.xmi");
		assertGeneratedFiles(subdir, output, "My.ecore");
	}

	@Test
	public void testCopyUnchangedClassesWithTheSameNameInDifferentPackages() throws IOException {
		var subdir = "classesWithTheSameName/";
		// in this case we load the same models with twice in different resource sets
		packageManagerOriginal.loadEcoreFile(TESTDATA + subdir + "My1.ecore");
		packageManagerOriginal.loadEcoreFile(TESTDATA + subdir + "My2.ecore");
		var modifiedEcore1 = packageManagerModified.loadEcoreFile(TESTDATA + subdir + "My1.ecore");
		var modifiedEcore2 = packageManagerModified.loadEcoreFile(TESTDATA + subdir + "My2.ecore");

		// this is actually XMI
		var root1 = packageManagerOriginal.loadModelFile(TESTDATA + subdir + "MyRoot1.xmi");
		var class1 = packageManagerOriginal.loadModelFile(TESTDATA + subdir + "MyClass1.xmi");
		var root2 = packageManagerOriginal.loadModelFile(TESTDATA + subdir + "MyRoot2.xmi");
		var class2 = packageManagerOriginal.loadModelFile(TESTDATA + subdir + "MyClass2.xmi");
		var modifiedRoot1 = packageManagerModified.loadModelFile(TESTDATA + subdir + "MyRoot1.xmi");
		var modifiedClass1 = packageManagerModified.loadModelFile(TESTDATA + subdir + "MyClass1.xmi");
		var modifiedRoot2 = packageManagerModified.loadModelFile(TESTDATA + subdir + "MyRoot2.xmi");
		var modifiedClass2 = packageManagerModified.loadModelFile(TESTDATA + subdir + "MyClass2.xmi");

		var copier = EdeltaEmfCopier.createFromResources
				(List.of(modifiedEcore1, modifiedEcore2));
		copyIntoModified(copier, root1, modifiedRoot1);
		copyIntoModified(copier, class1, modifiedClass1);
		copyIntoModified(copier, root2, modifiedRoot2);
		copyIntoModified(copier, class2, modifiedClass2);
		copier.copyReferences();

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
		packageManagerOriginal.loadEcoreFile(TESTDATA + subdir + ORIGINAL + "My.ecore");
		var modifiedEcore = packageManagerModified.loadEcoreFile(TESTDATA + subdir + MODIFIED + "My.ecore");

		// this is actually XMI
		var original = packageManagerOriginal.loadModelFile(TESTDATA + subdir + ORIGINAL + "MyRoot.xmi");
		var original2 = packageManagerOriginal.loadModelFile(TESTDATA + subdir + ORIGINAL + "MyClass.xmi");
		var modified = packageManagerModified.loadModelFile(TESTDATA + subdir + MODIFIED + "MyRoot.xmi");
		var modified2 = packageManagerModified.loadModelFile(TESTDATA + subdir + MODIFIED + "MyClass.xmi");

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
			c -> (EClass) packageManagerModified.getEPackage(
					"mypackage").getEClassifier("MyClassRenamed")
		);
		copier.addEClassMigrator(
			c -> c.getName().equals("MyClass"),
			c -> (EClass) packageManagerModified.getEPackage(
					"mypackage").getEClassifier("MyClassRenamed")
		);
		copyIntoModified(copier, original, modified);
		copyIntoModified(copier, original2, modified2);
		copier.copyReferences();

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
		packageManagerOriginal.loadEcoreFile(TESTDATA + subdir + ORIGINAL + "My.ecore");
		var modifiedEcore = packageManagerModified.loadEcoreFile(TESTDATA + subdir + MODIFIED + "My.ecore");

		// this is actually XMI
		var original = packageManagerOriginal.loadModelFile(TESTDATA + subdir + ORIGINAL + "MyRoot.xmi");
		var original2 = packageManagerOriginal.loadModelFile(TESTDATA + subdir + ORIGINAL + "MyClass.xmi");
		var modified = packageManagerModified.loadModelFile(TESTDATA + subdir + MODIFIED + "MyRoot.xmi");
		var modified2 = packageManagerModified.loadModelFile(TESTDATA + subdir + MODIFIED + "MyClass.xmi");

		var copier = EdeltaEmfCopier.createFromResources(singletonList(modifiedEcore));
		copier.addEStructuralFeatureMigrator(
			f -> {
				var name = f.getName();
				return name.equals("myContents");
			},
			f -> ((EClass) packageManagerModified.getEPackage(
					"mypackage").getEClassifier("MyRoot"))
					.getEStructuralFeature(f.getName() + "Renamed")
		);
		copier.addMigrator(
			EdeltaEmfCopier.ModelMigrator.migrateById(
				"mypackage.MyRoot.myReferences",
				() -> ((EClass) packageManagerModified.getEPackage(
						"mypackage").getEClassifier("MyRoot"))
						.getEStructuralFeature("myReferencesRenamed")
			)
		);
		copyIntoModified(copier, original, modified);
		copyIntoModified(copier, original2, modified2);
		copier.copyReferences();

		var output = OUTPUT + subdir;
		packageManagerModified.saveEcores(output);
		packageManagerModified.saveModels(output);
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
