package edelta.tests

import com.google.inject.Inject
import edelta.interpreter.EdeltaInterpreter
import org.eclipse.xtext.testing.InjectWith
import org.eclipse.xtext.testing.XtextRunner
import org.junit.Test
import org.junit.runner.RunWith

import static org.junit.Assert.*

@RunWith(XtextRunner)
@InjectWith(EdeltaInjectorProviderCustom)
class EdeltaInterpreterTest extends EdeltaAbstractTest {

	@Inject EdeltaInterpreter interpreter

	@Test
	def void testCreateEClass() {
		val program = '''
			metamodel "ecore"
			metamodel "foo"
			
			createEClass NewClass in foo {
				abstract = true
			}
		'''.parse
		program.lastExpression.createEClassExpression => [
			val result = interpreter.run(it)
			println(result)
			val derivedEClass = program.getDerivedStateLastEClass
			assertEquals("NewClass", derivedEClass.name)
			// when this works this must be true
			assertEquals(false, derivedEClass.abstract)
		]
	}

}
