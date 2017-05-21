package edelta.tests

import com.google.inject.Inject
import com.google.inject.Injector
import edelta.interpreter.EdeltaSafeInterpreter
import org.eclipse.xtext.testing.InjectWith
import org.eclipse.xtext.testing.XtextRunner
import org.junit.Test
import org.junit.runner.RunWith

import static extension org.junit.Assert.*

@RunWith(XtextRunner)
@InjectWith(EdeltaInjectorProviderCustom)
class EdeltaSafeInterpreterTest extends EdeltaInterpreterTest {

	@Inject Injector injector

	override void createInterpreter() {
		interpreter = injector.getInstance(EdeltaSafeInterpreter)
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
}
