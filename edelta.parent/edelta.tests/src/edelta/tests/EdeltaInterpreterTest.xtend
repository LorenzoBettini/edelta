package edelta.tests

import com.google.inject.Inject
import edelta.interpreter.EdeltaInterpreter
import org.eclipse.emf.ecore.EClass
import org.eclipse.xtext.testing.InjectWith
import org.eclipse.xtext.testing.XtextRunner
import org.eclipse.xtext.xbase.interpreter.impl.DefaultEvaluationResult
import org.eclipse.xtext.xbase.jvmmodel.IJvmModelAssociations
import org.junit.Test
import org.junit.runner.RunWith

import static org.junit.Assert.*
import org.eclipse.xtext.common.types.JvmGenericType
import edelta.tests.additional.MyCustomException

@RunWith(XtextRunner)
@InjectWith(EdeltaInjectorProviderCustom)
class EdeltaInterpreterTest extends EdeltaAbstractTest {

	@Inject EdeltaInterpreter interpreter

	@Inject extension IJvmModelAssociations

	@Test
	def void testCreateEClass() {
		'''
			metamodel "foo"
			
			createEClass NewClass in foo {
				abstract = true
			}
		'''.assertAfterInterpretationOfEdeltaCreateExpression [ derivedEClass |
			assertEquals("NewClass", derivedEClass.name)
			assertEquals(true, derivedEClass.abstract)
		]
	}

	@Test
	def void testCreateEClassAndCallLibMethod() {
		'''
			metamodel "foo"
			
			createEClass NewClass in foo {
				EStructuralFeatures += newEAttribute("newTestAttr") [
					EType = ecoreref(FooDataType)
				]
			}
		'''.assertAfterInterpretationOfEdeltaCreateExpression [ derivedEClass |
			assertEquals("NewClass", derivedEClass.name)
			assertEquals(1, derivedEClass.EStructuralFeatures.size)
			val attr = derivedEClass.EStructuralFeatures.head
			assertEquals("newTestAttr", attr.name)
			assertEquals("FooDataType", attr.EType.name)
		]
	}

	@Test
	def void testCreateEClassAndCallOperation() {
		'''
			import org.eclipse.emf.ecore.EClass
			
			metamodel "foo"
			
			def op(EClass c) : void {
				c.abstract = true
			}
			
			createEClass NewClass in foo {
				op(it)
			}
		'''.assertAfterInterpretationOfEdeltaCreateExpression [ derivedEClass |
			assertEquals("NewClass", derivedEClass.name)
			assertEquals(true, derivedEClass.abstract)
		]
	}

	@Test(expected=MyCustomException)
	def void testCreateEClassAndCallOperationThatThrows() {
		'''
			import org.eclipse.emf.ecore.EClass
			import edelta.tests.additional.MyCustomException
			
			metamodel "foo"
			
			def op(EClass c) : void {
				throw new MyCustomException
			}
			
			createEClass NewClass in foo {
				op(it)
			}
		'''.assertAfterInterpretationOfEdeltaCreateExpression [ derivedEClass |
		]
	}

	def assertAfterInterpretationOfEdeltaCreateExpression(CharSequence input, (EClass)=>void testExecutor) {
		val program = input.parseWithTestEcore
		program.lastExpression.createEClassExpression => [
			val derivedEClass = program.getDerivedStateLastEClass
			val inferredJavaClass = program.jvmElements.filter(JvmGenericType).head
			val result = interpreter.run(it, derivedEClass, inferredJavaClass)
			if (result.exception !== null)
				throw result.exception
			testExecutor.apply(derivedEClass)
			assertTrue(
				"not expected result of type " + result.class.name,
				result instanceof DefaultEvaluationResult
			)
		]
	}
}
