package edelta.ui.tests

import com.google.inject.Inject
import edelta.testutils.PDETargetPlatformUtils
import edelta.ui.tests.utils.EdeltaPluginProjectHelper
import edelta.ui.tests.utils.PluginProjectHelper
import org.eclipse.core.resources.IProject
import org.eclipse.xtext.testing.InjectWith
import org.eclipse.xtext.testing.XtextRunner
import org.eclipse.xtext.ui.testing.AbstractWorkbenchTest
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith

import static org.eclipse.xtext.ui.testing.util.IResourcesSetupUtil.*

@RunWith(XtextRunner)
@InjectWith(EdeltaUiInjectorProvider)
class EdeltaWorkbenchIntegrationTest extends AbstractWorkbenchTest {

	var IProject project

	@Inject PluginProjectHelper projectHelper

	@Inject EdeltaPluginProjectHelper edeltaProjectHelper

	val TEST_PROJECT = "mytestproject"

	@BeforeClass
	def static void beforeClass() {
		PDETargetPlatformUtils.setTargetPlatform();
	}

	@Before
	override void setUp() {
		super.setUp
		project = edeltaProjectHelper.createEdeltaPluginProject(TEST_PROJECT).project
	}

	@Test def void testValidProject() {
		createFile(
			TEST_PROJECT + "/src/Test.edelta",
			'''
			package foo
			
			metamodel "mypackage"
			
			ecoreref(MyClass)
			'''
		)
		// we need to wait for build twice when we run all the UI tests
		waitForBuild
		projectHelper.assertNoErrors
		waitForBuild
		projectHelper.assertNoErrors
		assertSrcGenFolderFile("foo", "Test.java")
	}

	@Test def void testInvalidProject() {
		createFile(
			TEST_PROJECT + "/src/Test.edelta",
			'''
			package foo
			
			metamodel "mypackage"
			
			ecoreref(Foo)
			'''
		)
		// we need to wait for build twice when we run all the UI tests
		waitForBuild
		waitForBuild
		projectHelper.assertErrors(
		'''Foo cannot be resolved.'''
		)
	}

	@Test def void testDerivedStateEPackagesDontInterfereWithOtherEdeltaFiles() {
		createFile(
			TEST_PROJECT + "/src/Test.edelta",
			'''
			package foo
			
			metamodel "mypackage"
			
			createEClass NewClass in mypackage {
				
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
			
			createEClass NewClass in mypackage {
				
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
		val expectedSrcGenFolderSubDir = "src-gen/" + expectedSubDir
		val srcGenFolder = project.getFolder(expectedSrcGenFolderSubDir)
		assertTrue(expectedSrcGenFolderSubDir + " does not exist", srcGenFolder.exists)
		val genfile = srcGenFolder.getFile(expectedFile)
		assertTrue(expectedFile + " does not exist", genfile.exists())
	}
}
