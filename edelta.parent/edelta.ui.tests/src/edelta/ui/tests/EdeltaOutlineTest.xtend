package edelta.ui.tests

import edelta.ui.internal.EdeltaActivator
import org.eclipse.xtext.junit4.ui.AbstractOutlineTest
import org.eclipse.xtext.testing.InjectWith
import org.eclipse.xtext.testing.XtextRunner
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(XtextRunner)
@InjectWith(EdeltaUiInjectorProvider)
class EdeltaOutlineTest extends AbstractOutlineTest {

	override protected getEditorId() {
		EdeltaActivator.EDELTA_EDELTA
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
		  createClass(String) : Object
		'''
		// EClass is not available in the test project,
		// so the return type is inferred as Object
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
		  createClass(String) : Object
		  doExecute() : void
		'''
		// EClass is not available in the test project,
		// so the return type is inferred as Object
		)
	}

	@Test
	def void testOutlineWithCreateEClass() {
		'''
		createEClass A in foo {
			createEAttribute attr type EString {
			}
		}
		'''.assertAllLabels(
		'''
		test
		  doExecute() : void
		  null
		    A
		      attr
		'''
		// The package cannot be resolved, that's why it's shown as null
		)
	}
}
