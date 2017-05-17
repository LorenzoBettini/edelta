package edelta.tests

import com.google.inject.Inject
import edelta.interpreter.EdeltaInterpreter
import org.eclipse.emf.ecore.EClass
import org.eclipse.xtext.common.types.JvmGenericType
import org.eclipse.xtext.testing.InjectWith
import org.eclipse.xtext.testing.XtextRunner
import org.eclipse.xtext.xbase.interpreter.impl.DefaultEvaluationResult
import org.eclipse.xtext.xbase.jvmmodel.IJvmModelAssociations
import org.junit.Test
import org.junit.runner.RunWith

import static org.junit.Assert.*

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

	@Test
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
			// nothing bad happens, exception is not shown
		]
	}

	@Test
	def void testCreateEClassAndCallOperationFromUseAs() {
		'''
			import edelta.tests.additional.MyCustomEdelta
			
			metamodel "foo"
			
			use MyCustomEdelta as my
			
			createEClass NewClass in foo {
				my.createANewEAttribute(it)
			}
		'''.assertAfterInterpretationOfEdeltaCreateExpression [ derivedEClass |
			assertEquals("NewClass", derivedEClass.name)
			assertEquals(1, derivedEClass.EStructuralFeatures.size)
			val attr = derivedEClass.EStructuralFeatures.head
			assertEquals("aNewAttr", attr.name)
			assertEquals("EString", attr.EType.name)
		]
	}

	@Test(expected=IllegalStateException)
	def void testCreateEClassAndCallOperationFromUseAsReferringToUnknownType() {
		'''
			metamodel "foo"
			
			use NonExistant as my
			
			createEClass NewClass in foo {
				my.createANewEAttribute(it)
			}
		'''.assertAfterInterpretationOfEdeltaCreateExpression(false) [ derivedEClass |
			// will not get here
		]
	}

	@Test
	def void testCreateEClassAndCreateEAttribute() {
		'''
			metamodel "foo"
			
			createEClass NewClass in foo {
				createEAttribute newTestAttr type FooDataType {
					lowerBound = -1
				}
			}
		'''.assertAfterInterpretationOfEdeltaCreateExpression [ derivedEClass |
			assertEquals("NewClass", derivedEClass.name)
			assertEquals(1, derivedEClass.EStructuralFeatures.size)
			val attr = derivedEClass.EStructuralFeatures.head
			assertEquals("newTestAttr", attr.name)
			assertEquals(-1, attr.lowerBound)
			assertEquals("FooDataType", attr.EType.name)
		]
	}

	def assertAfterInterpretationOfEdeltaCreateExpression(CharSequence input, (EClass)=>void testExecutor) {
		assertAfterInterpretationOfEdeltaCreateExpression(input, true, testExecutor)
	}

	def assertAfterInterpretationOfEdeltaCreateExpression(CharSequence input, boolean doValidate, (EClass)=>void testExecutor) {
		val program = input.parseWithTestEcore
		if (doValidate) {
			program.assertNoErrors
		}
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
