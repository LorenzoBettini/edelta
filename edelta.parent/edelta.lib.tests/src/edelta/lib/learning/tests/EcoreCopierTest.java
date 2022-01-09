package edelta.lib.learning.tests;

import static edelta.testutils.EdeltaTestUtils.assertFilesAreEquals;
import static edelta.testutils.EdeltaTestUtils.cleanDirectoryAndFirstSubdirectories;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EPackage;
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

	static class TestCopier extends Copier {
		private static final long serialVersionUID = 1L;
		private Collection<EPackage> packages;

		public TestCopier(Collection<EPackage> packages) {
			this.packages = packages;
		}

		public TestCopier(Resource resource) {
			this(List.of((EPackage) resource.getContents().get(0)));
		}

		@Override
		protected EClass getTarget(EClass eClass) {
			return getEClassByName(eClass)
					.orElse(null);
		}

		private Optional<EClass> getEClassByName(EClass eClass) {
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
		var originalEcore = runtimeForOriginal.loadEcoreFile(TESTDATA + "renamedClass/" + ORIGINAL + "My.ecore");
		var modifiedEcore = runtimeForModified.loadEcoreFile(TESTDATA + "renamedClass/" + ORIGINAL + "My.ecore");

		// this is actually XMI
		var original = runtimeForOriginal.loadEcoreFile(TESTDATA + "renamedClass/" + ORIGINAL + "MyRoot.xmi");
		var modified = runtimeForModified.loadEcoreFile(TESTDATA + "renamedClass/" + ORIGINAL + "MyRoot.xmi");

		var copier = new TestCopier(modifiedEcore);
		var root = original.getContents().get(0);
		var copy = copier.copy(root);
		modified.getContents().clear();
		modified.getContents().add(copy);

		var subdir = "unchanged/";
		var output = OUTPUT + subdir;
		runtimeForModified.saveModifiedEcores(output);
		assertFilesAreEquals(
			EXPECTATIONS + subdir +"MyRoot.xmi",
			output + "MyRoot.xmi");
	}

	@Test
	public void testCopyChanged() throws IOException {
		var originalEcore = runtimeForOriginal.loadEcoreFile(TESTDATA + "renamedClass/" + ORIGINAL + "My.ecore");
		var modifiedEcore = runtimeForModified.loadEcoreFile(TESTDATA + "renamedClass/" + MODIFIED + "My.ecore");

		// this is actually XMI
		var original = runtimeForOriginal.loadEcoreFile(TESTDATA + "renamedClass/" + ORIGINAL + "MyRoot.xmi");
		var modified = runtimeForModified.loadEcoreFile(TESTDATA + "renamedClass/" + MODIFIED + "MyRoot.xmi");

		// must use redefine the targets for the modified ecore
		var copier = new TestCopier(modifiedEcore) {
			private static final long serialVersionUID = 1L;

			@Override
			protected EClass getTarget(EClass eClass) {
				if (eClass.getName().equals("MyRoot")) {
					return runtimeForModified.getEClass(
						"mypackage", "MyRootRenamed");
				}
				return super.getTarget(eClass);
			}
		};
		var root = original.getContents().get(0);
		var copy = copier.copy(root);
		modified.getContents().clear();
		modified.getContents().add(copy);

		var subdir = "renamedClass/";
		var output = OUTPUT + subdir;
		runtimeForModified.saveModifiedEcores(output);
		assertFilesAreEquals(
			EXPECTATIONS + subdir +"MyRoot.xmi",
			output + "MyRoot.xmi");

	}
}
