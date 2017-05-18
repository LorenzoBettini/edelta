package edelta.tests

import com.google.inject.Inject
import edelta.interpreter.EdeltaInterpreter
import edelta.tests.additional.MyCustomException
import org.eclipse.emf.ecore.EClass
import org.eclipse.xtext.common.types.JvmGenericType
import org.eclipse.xtext.testing.InjectWith
import org.eclipse.xtext.testing.XtextRunner
import org.eclipse.xtext.xbase.interpreter.impl.DefaultEvaluationResult
import org.eclipse.xtext.xbase.jvmmodel.IJvmModelAssociations
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

import static edelta.interpreter.EdeltaInterpreter.*
import static org.junit.Assert.*

@RunWith(XtextRunner)
@InjectWith(EdeltaInjectorProviderCustom)
class EdeltaInterpreterTest extends EdeltaAbstractTest {

	@Inject EdeltaInterpreter interpreter

	@Inject extension IJvmModelAssociations

	@Before
	def void disableTimeout() {
		// for standard tests we disable the timeout
		// actually we set it to several minutes
		// this also makes it easier to debug tests
		EdeltaInterpreter.INTERPRETER_TIMEOUT = 1200000;
	}

	@Test
	def void testCreateEClass() {
		'''
			metamodel "foo"
			
			createEClass NewClass in foo {
				abstract = true
			}
		'''.assertAfterInterpretationOfEdeltaManipulationExpression [ derivedEClass |
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
		'''.assertAfterInterpretationOfEdeltaManipulationExpression [ derivedEClass |
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
		'''.assertAfterInterpretationOfEdeltaManipulationExpression [ derivedEClass |
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
		'''.assertAfterInterpretationOfEdeltaManipulationExpression [ derivedEClass |
			// never gets here
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
		'''.assertAfterInterpretationOfEdeltaManipulationExpression [ derivedEClass |
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
		'''.assertAfterInterpretationOfEdeltaManipulationExpression(false) [ derivedEClass |
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
		'''.assertAfterInterpretationOfEdeltaManipulationExpression [ derivedEClass |
			assertEquals("NewClass", derivedEClass.name)
			assertEquals(1, derivedEClass.EStructuralFeatures.size)
			val attr = derivedEClass.EStructuralFeatures.head
			assertEquals("newTestAttr", attr.name)
			assertEquals(-1, attr.lowerBound)
			assertEquals("FooDataType", attr.EType.name)
		]
	}

	@Test
	def void testCreateEClassAndCreateEAttributeAndCallOperationFromUseAs() {
		'''
			import edelta.tests.additional.MyCustomEdelta
			
			metamodel "foo"
			
			use MyCustomEdelta as my
			
			createEClass NewClass in foo {
				createEAttribute newTestAttr type FooDataType {
					my.setAttributeBounds(it, 1, -1)
				}
			}
		'''.assertAfterInterpretationOfEdeltaManipulationExpression [ derivedEClass |
			assertEquals("NewClass", derivedEClass.name)
			assertEquals(1, derivedEClass.EStructuralFeatures.size)
			val attr = derivedEClass.EStructuralFeatures.head
			assertEquals("newTestAttr", attr.name)
			assertEquals(1, attr.lowerBound)
			assertEquals(-1, attr.upperBound)
			assertEquals("FooDataType", attr.EType.name)
		]
	}

	@Test
	def void testChangeEClassAndCreateEAttributeAndCallOperationFromUseAs() {
		'''
			import edelta.tests.additional.MyCustomEdelta
			
			metamodel "foo"
			
			use MyCustomEdelta as my
			
			changeEClass foo.FooClass {
				createEAttribute newTestAttr type FooDataType {
					my.setAttributeBounds(it, 1, -1)
				}
			}
		'''.assertAfterInterpretationOfEdeltaManipulationExpression [ derivedEClass |
			assertEquals("FooClass", derivedEClass.name)
			val attr = derivedEClass.EStructuralFeatures.last
			assertEquals("newTestAttr", attr.name)
			assertEquals(1, attr.lowerBound)
			assertEquals(-1, attr.upperBound)
			assertEquals("FooDataType", attr.EType.name)
		]
	}

	@Test
	def void testRenameEClassAndCreateEAttributeAndCallOperationFromUseAs() {
		'''
			import edelta.tests.additional.MyCustomEdelta
			
			metamodel "foo"
			
			use MyCustomEdelta as my
			
			changeEClass foo.FooClass newName Renamed {
				createEAttribute newTestAttr type FooDataType {
					my.setAttributeBounds(it, 1, -1)
				}
			}
		'''.assertAfterInterpretationOfEdeltaManipulationExpression [ derivedEClass |
			assertEquals("Renamed", derivedEClass.name)
			val attr = derivedEClass.EStructuralFeatures.last
			assertEquals("newTestAttr", attr.name)
			assertEquals(1, attr.lowerBound)
			assertEquals(-1, attr.upperBound)
			assertEquals("FooDataType", attr.EType.name)
		]
	}

	@Test
	def void testTimeoutInCancelIndicator() {
		// in this test we really need the timeout
		EdeltaInterpreter.INTERPRETER_TIMEOUT = 2000;
		'''
			import org.eclipse.emf.ecore.EClass

			metamodel "foo"
			
			def op(EClass c) : void {
				var i = 10;
				while (i >= 0) {
					Thread.sleep(1000);
					i++
				}
				// this will never be executed
				c.abstract = true
			}
			
			createEClass NewClass in foo {
				op(it)
			}
		'''.assertAfterInterpretationOfEdeltaManipulationExpression [ derivedEClass |
			assertEquals("NewClass", derivedEClass.name)
			assertEquals(false, derivedEClass.abstract)
		]
	}

	def assertAfterInterpretationOfEdeltaManipulationExpression(CharSequence input, (EClass)=>void testExecutor) {
		assertAfterInterpretationOfEdeltaManipulationExpression(input, true, testExecutor)
	}

	def assertAfterInterpretationOfEdeltaManipulationExpression(CharSequence input, boolean doValidate, (EClass)=>void testExecutor) {
		val program = input.parseWithTestEcore
		if (doValidate) {
			program.assertNoErrors
		}
		program.lastExpression.getManipulationEClassExpression => [
			val derivedEClass = program.getDerivedStateLastEClass
			val inferredJavaClass = program.jvmElements.filter(JvmGenericType).head
			val result = interpreter.run(it, derivedEClass, inferredJavaClass)
			// result can be null due to a timeout
			if (result?.exception !== null)
				throw result.exception
			testExecutor.apply(derivedEClass)
			if (result !== null)
				assertTrue(
					"not expected result of type " + result.class.name,
					result instanceof DefaultEvaluationResult
				)
		]
	}
}
