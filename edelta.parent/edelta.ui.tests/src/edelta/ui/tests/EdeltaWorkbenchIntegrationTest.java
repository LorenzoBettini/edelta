package edelta.ui.tests;

import static edelta.ui.tests.utils.ProjectImportUtil.importProject;
import static org.eclipse.xtext.ui.testing.util.IResourcesSetupUtil.*;

import org.eclipse.core.resources.IProject;
import org.eclipse.xtext.testing.Flaky;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.XtextRunner;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.inject.Inject;

import edelta.ui.tests.utils.PluginProjectHelper;

/**
 * This test requires an empty workspace
 * 
 * @author Lorenzo Bettini
 */
@RunWith(XtextRunner.class)
@InjectWith(EdeltaUiInjectorProvider.class)
public class EdeltaWorkbenchIntegrationTest extends CustomAbstractWorkbenchTest {
	@Inject
	private PluginProjectHelper projectHelper;

	@Rule
	public Flaky.Rule testRule = new Flaky.Rule();

	private static IProject project;

	private static String TEST_PROJECT = "edelta.ui.tests.project";

	@BeforeClass
	public static void importTestProject() throws Exception {
		cleanWorkspace();
		project = importProject(EdeltaWorkbenchIntegrationTest.TEST_PROJECT);
		waitForBuild();
	}

	@Test
	@Flaky
	public void testValidProject() throws Exception {
		createFile(
			TEST_PROJECT + "/src/Test.edelta",
			"package foo\n"
			+ "\n"
			+ "metamodel \"mypackage\"\n"
			+ "\n"
			+ "modifyEcore aTest epackage mypackage {\n"
			+ "    ecoreref(MyClass)\n"
			+ "}");
		waitForBuild();
		projectHelper.assertNoErrors();
		assertSrcGenFolderFile("foo", "Test.java");
	}

	@Test
	@Flaky
	public void testInvalidProject() throws Exception {
		createFile(
			TEST_PROJECT + "/src/Test.edelta",
			"package foo\n"
			+ "\n"
			+ "metamodel \"mypackage\"\n"
			+ "\n"
			+ "modifyEcore aTest epackage mypackage {\n"
			+ "    ecoreref(Foo)\n"
			+ "}");
		waitForBuild();
		projectHelper.assertErrors("Foo cannot be resolved.");
	}

	@Test
	@Flaky
	public void testDerivedStateEPackagesDontInterfereWithOtherEdeltaFiles() throws Exception {
		var contents =
			"package foo\n"
			+ "\n"
			+ "metamodel \"mypackage\"\n"
			+ "\n"
			+ "modifyEcore aTest epackage mypackage {\n"
			+ "    addNewEClass(\"NewClass\")\n"
			+ "}";
		createFile(
			TEST_PROJECT + "/src/Test.edelta",
			contents);
		createFile(
			TEST_PROJECT + "/src/Test2.edelta",
			contents);
		waitForBuild();
		projectHelper.assertNoErrors();
		assertSrcGenFolderFile("foo", "Test.java");
		assertSrcGenFolderFile("foo", "Test2.java");
	}

	private void assertSrcGenFolderFile(String expectedSubDir, String expectedFile) {
		var expectedSrcGenFolderSubDir = "edelta-gen/" + expectedSubDir;
		var srcGenFolder = project.getFolder(expectedSrcGenFolderSubDir);
		assertTrue(expectedSrcGenFolderSubDir + " does not exist", srcGenFolder.exists());
		var genfile = srcGenFolder.getFile(expectedFile);
		assertTrue(expectedFile + " does not exist", genfile.exists());
	}
}
