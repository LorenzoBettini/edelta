package edelta.tests

import com.google.inject.Inject
import com.google.inject.Injector
import edelta.interpreter.EdeltaSafeInterpreter
import org.eclipse.xtext.testing.InjectWith
import org.eclipse.xtext.testing.XtextRunner
import org.junit.Test
import org.junit.runner.RunWith

import static org.junit.Assert.*
import edelta.interpreter.EdeltaSafeInterpreter.EdeltaInterpreterRuntimeException

@RunWith(XtextRunner)
@InjectWith(EdeltaInjectorProviderDerivedStateComputerWithoutInterpreter)
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
			
			createEClass First in foo
			eclass First
		'''
		input.assertAfterInterpretationOfEdeltaManipulationExpression(false) [ derivedEClass |
			assertEquals("First", derivedEClass.name)
		]
	}

	@Test
	override void testCreateEClassAndCallOperationFromUseAsReferringToUnknownType() {
		'''
			metamodel "foo"
			
			use NonExistant as my
			
			createEClass NewClass in foo {
				my.createANewEAttribute(it)
			}
		'''.assertAfterInterpretationOfEdeltaManipulationExpression(false) [ derivedEClass |
			assertEquals("NewClass", derivedEClass.name)
		]
	}

	@Test(expected=EdeltaInterpreterRuntimeException)
	def void testEdeltaInterpreterRuntimeExceptionIsThrown() {
		'''
			import org.eclipse.emf.ecore.EClass
			import edelta.interpreter.EdeltaSafeInterpreter.EdeltaInterpreterRuntimeException
			
			metamodel "foo"
			
			def op(EClass c) : void {
				throw new EdeltaInterpreterRuntimeException("test")
			}
			
			createEClass NewClass in foo {
				op(it)
			}
		'''.assertAfterInterpretationOfEdeltaManipulationExpression [ derivedEClass |
			// never gets here
		]
	}
}
