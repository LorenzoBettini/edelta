package edelta.tests

import com.google.inject.Inject
import com.google.inject.Injector
import edelta.interpreter.EdeltaInterpreterRuntimeException
import edelta.interpreter.EdeltaSafeInterpreter
import org.eclipse.xtext.testing.InjectWith
import org.eclipse.xtext.testing.XtextRunner
import org.junit.Test
import org.junit.runner.RunWith

import static org.junit.Assert.*

@RunWith(XtextRunner)
@InjectWith(EdeltaInjectorProviderDerivedStateComputerWithoutSafeInterpreter)
class EdeltaSafeInterpreterTest extends EdeltaInterpreterTest {

	@Inject Injector injector

	override createInterpreter() {
		injector.getInstance(EdeltaSafeInterpreter)
	}

	@Test
	override void testOperationWithErrorsDueToWrongParsing() {
		val input = '''
			package test
			
			metamodel "foo"
			
			modifyEcore aTest epackage foo {
				addNewEClass("First")
				eclass First
			}
		'''
		input.assertAfterInterpretationOfEdeltaModifyEcoreOperation(false) [ ePackage |
			val derivedEClass = ePackage.lastEClass
			assertEquals("First", derivedEClass.name)
		]
	}

	@Test
	override void testCreateEClassAndCallOperationFromUseAsReferringToUnknownType() {
		'''
			metamodel "foo"
			
			use NonExistant as my
			
			modifyEcore aTest epackage foo {
				val c = addNewEClass("NewClass")
				my.createANewEAttribute(c)
			}
		'''.assertAfterInterpretationOfEdeltaModifyEcoreOperation(false) [ ePackage |
			val derivedEClass = ePackage.lastEClass
			assertEquals("NewClass", derivedEClass.name)
		]
	}

	@Test(expected=EdeltaInterpreterRuntimeException)
	def void testEdeltaInterpreterRuntimeExceptionIsThrown() {
		'''
			import org.eclipse.emf.ecore.EClass
			import edelta.interpreter.EdeltaInterpreterRuntimeException
			
			metamodel "foo"
			
			def op(EClass c) : void {
				throw new EdeltaInterpreterRuntimeException("test")
			}
			
			modifyEcore aTest epackage foo {
				op(addNewEClass("NewClass"))
			}
		'''.assertAfterInterpretationOfEdeltaModifyEcoreOperation [
			// never gets here
		]
	}

	@Test // remove expected exception which the safe interpreter swallows
	override void testCreateEClassAndCallOperationThatThrows() {
		'''
			import org.eclipse.emf.ecore.EClass
			import edelta.tests.additional.MyCustomException
			
			metamodel "foo"
			
			def op(EClass c) : void {
				throw new MyCustomException
			}
			
			modifyEcore aTest epackage foo {
				addNewEClass("NewClass") [
					op(it)
				]
			}
		'''.assertAfterInterpretationOfEdeltaModifyEcoreOperation [
			// never gets here
		]
	}
}
