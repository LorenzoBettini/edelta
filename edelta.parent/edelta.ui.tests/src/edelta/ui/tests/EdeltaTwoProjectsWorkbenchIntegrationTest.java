package edelta.ui.tests;

import static edelta.ui.testutils.EdeltaUiTestUtils.importProject;
import static org.eclipse.xtext.ui.testing.util.IResourcesSetupUtil.*;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.XtextRunner;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import edelta.ui.tests.utils.PluginProjectHelper;

/**
 * This test requires an empty workspace and it tests two projects, the second
 * one depends on the first one and makes sure that the interpreter can access
 * the classes of the first project.
 * 
 * @author Lorenzo Bettini
 */
@RunWith(XtextRunner.class)
@InjectWith(EdeltaUiInjectorProvider.class)
public class EdeltaTwoProjectsWorkbenchIntegrationTest extends CustomAbstractWorkbenchTest {
	private static final String FIRST_PROJECT = "edelta.testprojects.first";
	private static final String SECOND_PROJECT = "edelta.testprojects.second";

	@BeforeClass
	public static void clean() throws Exception {
		cleanWorkspace();
		waitForBuild();
	}

	@Test
	public void testDependentProjects() throws Exception {
		var project1 = importProject("../" + EdeltaTwoProjectsWorkbenchIntegrationTest.FIRST_PROJECT);
		var project2 = importProject("../" + EdeltaTwoProjectsWorkbenchIntegrationTest.SECOND_PROJECT);
		cleanup(project1);
		cleanup(project2);
		waitForBuild();
		PluginProjectHelper.assertNoErrors();
		assertSrcGenFolderFile(project1, "com/example1", "Example1.java");
		assertSrcGenFolderFile(project2, "com/example2", "Example2.java");
	}

	private void cleanup(IProject project1) throws CoreException {
		project1.getFolder("edelta-gen/com").delete(true, monitor());
		project1.getFolder("modified").delete(true, monitor());
	}

	private void assertSrcGenFolderFile(IProject project, String expectedSubDir, String expectedFile) {
		var expectedSrcGenFolderSubDir = "edelta-gen/" + expectedSubDir;
		var srcGenFolder = project.getFolder(expectedSrcGenFolderSubDir);
		assertTrue(expectedSrcGenFolderSubDir + " does not exist", srcGenFolder.exists());
		var genfile = srcGenFolder.getFile(expectedFile);
		assertTrue(expectedFile + " does not exist", genfile.exists());
	}
}
