package edelta.ui.tests

import com.google.inject.Inject
import edelta.ui.internal.EdeltaActivator
import edelta.ui.tests.utils.EdeltaPluginProjectHelper
import org.eclipse.core.runtime.CoreException
import org.eclipse.xtext.junit4.ui.AbstractOutlineTest
import org.eclipse.xtext.testing.InjectWith
import org.eclipse.xtext.testing.XtextRunner
import org.junit.Test
import org.junit.runner.RunWith

import static org.eclipse.xtext.junit4.ui.util.IResourcesSetupUtil.*

@RunWith(XtextRunner)
@InjectWith(EdeltaUiInjectorProvider)
class EdeltaOutlineTest extends AbstractOutlineTest {

	@Inject EdeltaPluginProjectHelper edeltaProjectHelper

	override protected getEditorId() {
		EdeltaActivator.EDELTA_EDELTA
	}

	override protected createjavaProject(String projectName) throws CoreException {
		// we use a Plug-in project so that types are resolved (e.g., EClass)
		edeltaProjectHelper.createEdeltaPluginProject(TEST_PROJECT)
	}

	@Test
	def void testOutlineWithNoContents() {
		''''''.assertAllLabels(
		'''
		test
		'''
		)
	}

	@Test
	def void testOutlineWithOperation() {
		'''
		def createClass(String name) {
			newEClass(name)
		}
		'''.assertAllLabels(
		'''
		test
		  createClass(String) : EClass
		'''
		)
	}

	@Test
	def void testOutlineWithOperationAndMain() {
		'''
		def createClass(String name) {
			newEClass(name)
		}
		
		println("")
		'''.assertAllLabels(
		'''
		test
		  createClass(String) : EClass
		  doExecute() : void
		'''
		)
	}

	@Test
	def void testOutlineWithCreateEClass() {
		// wait for build so that ecores are indexed
		// and then found by the test programs
		waitForBuild

		'''
		metamodel "mypackage"
		
		createEClass A in mypackage {
			createEAttribute attr type EString {
			}
		}
		'''.assertAllLabels(
		'''
		test
		  doExecute() : void
		  mypackage
		    A
		      attr
		'''
		)
	}
}
