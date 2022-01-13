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

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.ENamedElement;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.util.EcoreUtil.Copier;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import edelta.lib.EdeltaDefaultRuntime;

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

		public static class ModelMigrator<T extends ENamedElement> {
			private Predicate<T> predicate;
			private Function<T, T> function;

			public ModelMigrator(Predicate<T> predicate, Function<T, T> function) {
				this.predicate = predicate;
				this.function = function;
			}

			public boolean canApply(T arg) {
				return predicate.test(arg);
			}

			public T apply(T arg) {
				return function.apply(arg);
			}
		}

		private Collection<EPackage> packages;

		private Collection<ModelMigrator<EClass>> classMigrators = new ArrayList<>();

		private Collection<ModelMigrator<EStructuralFeature>> featureMigrators = new ArrayList<>();

		public EdeltaEmfCopier(Collection<EPackage> packages) {
			this.packages = packages;
		}

		public EdeltaEmfCopier(Resource resource) {
			this(List.of((EPackage) resource.getContents().get(0)));
		}

		public void addEClassMigrator(ModelMigrator<EClass> classMigrator) {
			classMigrators.add(classMigrator);
		}

		public void addEStructuralFeatureMigrator(ModelMigrator<EStructuralFeature> featureMigrator) {
			featureMigrators.add(featureMigrator);
		}

		@Override
		protected EClass getTarget(EClass eClass) {
			return getTargetEClass(eClass)
				.orElse(null);
		}

		protected Optional<EClass> getTargetEClass(EClass eClass) {
			return classMigrators.stream()
				.filter(m -> m.canApply(eClass))
				.findFirst()
				.map(m -> m.apply(eClass))
				.or(() -> getEClassByName(eClass));
		}

		@Override
		protected EStructuralFeature getTarget(EStructuralFeature feature) {
			return getTargetEStructuralFeature(feature)
				.orElse(null);
		}

		protected Optional<EStructuralFeature> getTargetEStructuralFeature(EStructuralFeature feature) {
			return featureMigrators.stream()
				.filter(m -> m.canApply(feature))
				.findFirst()
				.map(m -> m.apply(feature))
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
		copier.addEClassMigrator(
			new EdeltaEmfCopier.ModelMigrator<>(
				c -> c.getName().equals("MyRoot"),
					c -> runtimeForModified.getEClass(
							"mypackage", "MyRootRenamed"))
		);
		copier.addEClassMigrator(
			new EdeltaEmfCopier.ModelMigrator<>(
				c -> c.getName().equals("MyClass"),
				c -> runtimeForModified.getEClass(
						"mypackage", "MyClassRenamed"))
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
		copier.addEStructuralFeatureMigrator(
			new EdeltaEmfCopier.ModelMigrator<>(
				f -> {
					var name = f.getName();
					return name.equals("myContents") || name.equals("myReferences");
				},
				f -> runtimeForModified.getEStructuralFeature(
						"mypackage", "MyRoot", f.getName() + "Renamed")
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
