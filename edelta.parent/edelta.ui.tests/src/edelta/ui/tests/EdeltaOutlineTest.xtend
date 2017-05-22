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

	@Test
	def void testOutlineWithCreateEClassAndInterpretedNewAttribute() {
		// wait for build so that ecores are indexed
		// and then found by the test programs
		waitForBuild

		'''
		import org.eclipse.emf.ecore.EClass
		
		metamodel "mypackage"
		// don't rely on ecore, since the input files are not saved
		// during the test, thus external libraries are not seen
		// metamodel "ecore"
		
		def myNewAttribute(EClass c, String name) {
			c.EStructuralFeatures += newEAttribute(name) => [
				EType = ecoreref(MyDataType)
			]
		}
		
		createEClass A in mypackage {
			myNewAttribute(it, "foo")
		}
		'''.assertAllLabels(
		'''
		test
		  myNewAttribute(EClass, String) : boolean
		  doExecute() : void
		  mypackage
		    A
		      foo
		'''
		)
	}

	@Test
	def void testOutlineWithCreateEClassAndInterpretedNewAttributeWithUseAs() {
		createFile(
			TEST_PROJECT + "/src/Refactorings.edelta",
			'''
			import org.eclipse.emf.ecore.EClass
			
			package com.example
			
			metamodel "mypackage"
			
			def myNewAttribute(EClass c, String name) {
				c.EStructuralFeatures += newEAttribute(name) => [
					EType = ecoreref(MyDataType)
				]
			}
			'''
		)
		// wait for build so that ecores are indexed
		// and then found by the test programs
		waitForBuild

		'''
		package com.example
		
		metamodel "mypackage"
		// don't rely on ecore, since the input files are not saved
		// during the test, thus external libraries are not seen
		// metamodel "ecore"
		
		use Refactorings as my
		
		createEClass A in mypackage {
			my.myNewAttribute(it, "foo")
		}
		'''.assertAllLabels(
		'''
		com.example
		  doExecute() : void
		  mypackage
		    A
		'''
		)
		// TODO: when interpreter's classloader is fixed
		// we should be able to interpret call to operations
		// in the source folders.
	}
}
