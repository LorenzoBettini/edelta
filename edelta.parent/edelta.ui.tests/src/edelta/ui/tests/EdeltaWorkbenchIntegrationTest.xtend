package edelta.ui.tests

import com.google.inject.Inject
import edelta.ui.tests.utils.EdeltaPluginProjectHelper
import edelta.ui.tests.utils.PluginProjectHelper
import org.eclipse.core.resources.IProject
import org.eclipse.xtext.testing.Flaky
import org.eclipse.xtext.testing.InjectWith
import org.eclipse.xtext.testing.XtextRunner
import org.eclipse.xtext.ui.testing.AbstractWorkbenchTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

import static org.eclipse.xtext.ui.testing.util.IResourcesSetupUtil.*

@RunWith(XtextRunner)
@InjectWith(EdeltaUiInjectorProvider)
class EdeltaWorkbenchIntegrationTest extends AbstractWorkbenchTest {

	var IProject project

	@Inject PluginProjectHelper projectHelper

	@Inject EdeltaPluginProjectHelper edeltaProjectHelper

	@Rule
	public Flaky.Rule testRule = new Flaky.Rule();

	val TEST_PROJECT = "mytestproject"

	override void setUp() {
		super.setUp
		project = edeltaProjectHelper.createEdeltaPluginProject(TEST_PROJECT).project
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
		// we need to wait for build twice when we run all the UI tests
		waitForBuild
		projectHelper.assertNoErrors
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
		// we need to wait for build twice when we run all the UI tests
		waitForBuild
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
		// we need to wait for build twice when we run all the UI tests
		waitForBuild
		projectHelper.assertNoErrors
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
		// we need to wait for build twice when we run all the UI tests
		waitForBuild
		projectHelper.assertNoErrors
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
