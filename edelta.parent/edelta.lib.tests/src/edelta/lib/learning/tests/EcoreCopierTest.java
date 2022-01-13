package edelta.lib.learning.tests;

import static edelta.testutils.EdeltaTestUtils.assertFilesAreEquals;
import static edelta.testutils.EdeltaTestUtils.cleanDirectoryAndFirstSubdirectories;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.eclipse.emf.ecore.EClass;
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
	 * @author bettini
	 *
	 */
	static class EdeltaEmfCopier extends Copier {
		private static final long serialVersionUID = 1L;
		private Collection<EPackage> packages;

		public EdeltaEmfCopier(Collection<EPackage> packages) {
			this.packages = packages;
		}

		public EdeltaEmfCopier(Resource resource) {
			this(List.of((EPackage) resource.getContents().get(0)));
		}

		@Override
		protected EClass getTarget(EClass eClass) {
			return getEClassByName(eClass)
					.orElse(null);
		}

		@Override
		protected EStructuralFeature getTarget(EStructuralFeature feature) {
			return getEClassByName(feature.getEContainingClass())
				.flatMap(c -> getEStructuralFeatureByName(c, feature))
				.orElse(null);
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

		// must redefine the targets for the modified ecore
		var copier = new EdeltaEmfCopier(modifiedEcore) {
			private static final long serialVersionUID = 1L;
			private Map<String, EClass> map = 
				Map.of(
					"MyRoot", runtimeForModified.getEClass(
							"mypackage", "MyRootRenamed"),
					"MyClass", runtimeForModified.getEClass(
							"mypackage", "MyClassRenamed")
					);

			@Override
			protected Optional<EClass> getEClassByName(EClass eClass) {
				var result = map.get(eClass.getName());
				if (result != null)
					return Optional.of(result);
				return super.getEClassByName(eClass);
			}
		};
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

		// must redefine the targets for the modified ecore
		var copier = new EdeltaEmfCopier(modifiedEcore) {
			private static final long serialVersionUID = 1L;
			
			@Override
			protected Optional<EStructuralFeature> getEStructuralFeatureByName(EClass c, EStructuralFeature feature) {
				var name = feature.getName();
				if (name.equals("myContents") || name.equals("myReferences"))
					return Optional.ofNullable(c.getEStructuralFeature(name + "Renamed"));
				return super.getEStructuralFeatureByName(c, feature);
			}
		};
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
