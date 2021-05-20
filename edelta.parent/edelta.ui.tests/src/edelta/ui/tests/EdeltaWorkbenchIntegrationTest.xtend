package edelta.ui.tests

import com.google.inject.Inject
import edelta.ui.tests.utils.PluginProjectHelper
import edelta.ui.tests.utils.ProjectImportUtil
import org.eclipse.core.resources.IProject
import org.eclipse.xtext.testing.Flaky
import org.eclipse.xtext.testing.InjectWith
import org.eclipse.xtext.testing.XtextRunner
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

import static org.eclipse.xtext.ui.testing.util.IResourcesSetupUtil.*
import org.junit.BeforeClass

/**
 * This test requires an empty workspace
 * 
 * @author Lorenzo Bettini
 *
 */
@RunWith(XtextRunner)
@InjectWith(EdeltaUiInjectorProvider)
class EdeltaWorkbenchIntegrationTest extends CustomAbstractWorkbenchTest {

	var static IProject project

	@Inject PluginProjectHelper projectHelper

	@Rule
	public Flaky.Rule testRule = new Flaky.Rule();

	static val TEST_PROJECT = "edelta.ui.tests.project"

	@BeforeClass
	def static void importTestProject() {
		cleanWorkspace
		project = ProjectImportUtil.importProject(TEST_PROJECT)
		waitForBuild
	}

	@Test @Flaky
	def void testValidProject() {
		createFile(
			TEST_PROJECT + "/src/Test.edelta",
			'''
			package foo
			
			metamodel "mypackage"
			
			modifyEcore aTest epackage mypackage {
				ecoreref(MyClass)
			}
			'''
		)
		waitForBuild
		projectHelper.assertNoErrors
		assertSrcGenFolderFile("foo", "Test.java")
	}

	@Test @Flaky
	def void testInvalidProject() {
		createFile(
			TEST_PROJECT + "/src/Test.edelta",
			'''
			package foo
			
			metamodel "mypackage"
			
			modifyEcore aTest epackage mypackage {
				ecoreref(Foo)
			}
			'''
		)
		waitForBuild
		projectHelper.assertErrors(
		'''Foo cannot be resolved.'''
		)
	}

	@Test @Flaky
	def void testDerivedStateEPackagesDontInterfereWithOtherEdeltaFiles() {
		createFile(
			TEST_PROJECT + "/src/Test.edelta",
			'''
			package foo
			
			metamodel "mypackage"
			
			modifyEcore aTest epackage mypackage {
				addNewEClass("NewClass")
			}
			'''
		)
		waitForBuild
		projectHelper.assertNoErrors
		createFile(
			TEST_PROJECT + "/src/Test2.edelta",
			'''
			package foo
			
			metamodel "mypackage"
			
			modifyEcore aTest epackage mypackage {
				addNewEClass("NewClass")
			}
			'''
		)
		waitForBuild
		projectHelper.assertNoErrors
		assertSrcGenFolderFile("foo", "Test.java")
		assertSrcGenFolderFile("foo", "Test2.java")
	}

	def private assertSrcGenFolderFile(String expectedSubDir, String expectedFile) {
		val expectedSrcGenFolderSubDir = "edelta-gen/" + expectedSubDir
		val srcGenFolder = project.getFolder(expectedSrcGenFolderSubDir)
		assertTrue(expectedSrcGenFolderSubDir + " does not exist", srcGenFolder.exists)
		val genfile = srcGenFolder.getFile(expectedFile)
		assertTrue(expectedFile + " does not exist", genfile.exists())
	}
}
