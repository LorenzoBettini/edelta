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
			val derivedEClass = program.getDerivedStateLastEClass
			val result = interpreter.run(it, derivedEClass)
			println(result)
			assertEquals("NewClass", derivedEClass.name)
			assertEquals(true, derivedEClass.abstract)
		]
	}

}
