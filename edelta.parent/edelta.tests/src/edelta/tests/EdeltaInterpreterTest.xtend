package edelta.tests

import com.google.inject.Inject
import com.google.inject.Injector
import edelta.edelta.EdeltaPackage
import edelta.interpreter.EdeltaInterpreter
import edelta.interpreter.IEdeltaInterpreter
import edelta.tests.additional.MyCustomException
import edelta.validation.EdeltaValidator
import org.eclipse.emf.ecore.EClass
import org.eclipse.xtext.testing.InjectWith
import org.eclipse.xtext.testing.XtextRunner
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

import static org.junit.Assert.*
import org.eclipse.emf.ecore.EPackage

@RunWith(XtextRunner)
@InjectWith(EdeltaInjectorProviderDerivedStateComputerWithoutInterpreter)
class EdeltaInterpreterTest extends EdeltaAbstractTest {

	protected IEdeltaInterpreter interpreter

	@Inject Injector injector

	def IEdeltaInterpreter createInterpreter() {
		injector.getInstance(EdeltaInterpreter)
	}

	@Before
	def void setupInterpreter() {
		interpreter = createInterpreter
		// for standard tests we disable the timeout
		// actually we set it to several minutes
		// this also makes it easier to debug tests
		interpreter.interpreterTimeout = 1200000;
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
		createEClassAndAddEAttributeUsingLibMethod.
		assertAfterInterpretationOfEdeltaManipulationExpression [ derivedEClass |
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

	@Test
	def void testCreateEClassAndCallJvmOperationFromSuperclass() {
		'''
			metamodel "foo"
			
			createEClass NewClass in foo {
				// call method from superclass AbstractEdelta
				EStructuralFeatures += ^createEAttribute(it, "aNewAttr", null)
			}
		'''.assertAfterInterpretationOfEdeltaManipulationExpression [ derivedEClass |
			assertEquals("NewClass", derivedEClass.name)
			assertEquals(1, derivedEClass.EStructuralFeatures.size)
			val attr = derivedEClass.EStructuralFeatures.head
			assertEquals("aNewAttr", attr.name)
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
		interpreter.interpreterTimeout = 2000;
		val input = '''
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
		'''
		input.assertAfterInterpretationOfEdeltaManipulationExpression [ derivedEClass |
			assertEquals("NewClass", derivedEClass.name)
			assertEquals(false, derivedEClass.abstract)
			derivedEClass.assertWarning(
				EdeltaPackage.eINSTANCE.edeltaEcoreCreateEClassExpression,
				EdeltaValidator.INTERPRETER_TIMEOUT,
				input.lastIndexOf("{"), 11,
				"Timeout interpreting initialization block"
			)
		]
	}

	@Test
	def void testNullBody() {
		val input = '''
			import org.eclipse.emf.ecore.EClass

			metamodel "foo"

			createEClass NewClass1 in foo
			
			// here the body is null, but the interpreter
			// avoids NPE
			createEClass NewClass in 
		'''
		input.assertAfterInterpretationOfEdeltaManipulationExpression(false) [ derivedEClass |
			assertEquals("NewClass1", derivedEClass.name)
		]
	}

	@Test(expected=IllegalArgumentException)
	def void testOperationWithErrorsDueToWrongParsing() {
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
	def void testUnresolvedEcoreReference() {
		val input = '''
			import org.eclipse.emf.ecore.EClass

			metamodel "foo"

			createEClass NewClass1 in foo {
				ecoreref(nonexist) // this won't break the interpreter
			}
		'''
		input.assertAfterInterpretationOfEdeltaManipulationExpression(false) [ derivedEClass |
			assertEquals("NewClass1", derivedEClass.name)
		]
	}

	@Test
	def void testUnresolvedEcoreReferenceQualified() {
		val input = '''
			import org.eclipse.emf.ecore.EClass

			metamodel "foo"

			createEClass NewClass1 in foo {
				ecoreref(nonexist.bang) // this won't break the interpreter
			}
		'''
		input.assertAfterInterpretationOfEdeltaManipulationExpression(false) [ derivedEClass |
			assertEquals("NewClass1", derivedEClass.name)
		]
	}

	@Test
	def void testEcoreModifyOperation() {
		val input = '''
			package test
			
			metamodel "foo"
			
			modifyEcore aModificationTest epackage foo {
				EClassifiers += newEClass("ANewClass") [
					ESuperTypes += newEClass("Base")
				]
			}
		'''
		input.assertAfterInterpretationOfEdeltaModifyEcoreOperation [ derivedEPackage |
			derivedEPackage.EClassifiers.last as EClass => [
				assertEquals("ANewClass", name)
				assertEquals("Base", ESuperTypes.last.name)
			]
		]
	}

	@Test
	def void testModifyEcoreAndCallOperation() {
		'''
			import org.eclipse.emf.ecore.EClass
			
			metamodel "foo"
			
			def op(EClass c) : void {
				c.abstract = true
			}
			
			modifyEcore aModificationTest epackage foo {
				EClassifiers += newEClass("ANewClass") [
					ESuperTypes += newEClass("Base")
					op(it)
				]
			}
		'''.assertAfterInterpretationOfEdeltaModifyEcoreOperation [ derivedEPackage |
			derivedEPackage.EClassifiers.last as EClass => [
				assertEquals("ANewClass", name)
				assertEquals("Base", ESuperTypes.last.name)
				assertTrue(isAbstract)
			]
		]
	}

	@Test
	def void testModifyEcoreRenameClassAndAddAttribute() {
		'''
			import org.eclipse.emf.ecore.EClass
			
			metamodel "foo"
			
			def op(EClass c) : void {
				c.abstract = true
			}
			
			modifyEcore aTest epackage foo {
				ecoreref(foo.FooClass).name = "RenamedClass"
				ecoreref(RenamedClass).ESuperTypes += newEClass("Base")
				op(ecoreref(RenamedClass))
				ecoreref(RenamedClass).getEStructuralFeatures += newEAttribute("added")
			}
		'''.
		assertAfterInterpretationOfEdeltaModifyEcoreOperation(true) [ derivedEPackage |
			derivedEPackage.EClassifiers.head as EClass => [
				assertEquals("RenamedClass", name)
				assertEquals("Base", ESuperTypes.last.name)
				assertTrue(isAbstract)
				assertEquals("added", EStructuralFeatures.last.name)
			]
		]
	}

	@Test
	def void testModifyEcoreRenameClassAndAddAttribute2() {
		'''
			import org.eclipse.emf.ecore.EClass
			
			metamodel "foo"
			
			modifyEcore aTest epackage foo {
				ecoreref(foo.FooClass).name = "RenamedClass"
				ecoreref(RenamedClass).getEStructuralFeatures += newEAttribute("added")
				ecoreref(RenamedClass.added)
			}
		'''.
		assertAfterInterpretationOfEdeltaModifyEcoreOperation(true) [ derivedEPackage |
			derivedEPackage.EClassifiers.head as EClass => [
				assertEquals("RenamedClass", name)
				assertEquals("added", EStructuralFeatures.last.name)
			]
		]
	}

	@Test
	def void testEcoreModifyTimeoutInCancelIndicator() {
		// in this test we really need the timeout
		interpreter.interpreterTimeout = 2000;
		val input = '''
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
			
			modifyEcore aModificationTest epackage foo {
				EClassifiers += newEClass("ANewClass")
				op(EClassifiers.last as EClass)
			}
		'''
		input.assertAfterInterpretationOfEdeltaModifyEcoreOperation [ derivedEPackage |
			derivedEPackage.EClassifiers.last as EClass => [
				assertEquals("ANewClass", name)
				assertEquals(false, abstract)
				val initialIndex = input.lastIndexOf("{")
				assertWarning(
					EdeltaPackage.eINSTANCE.edeltaModifyEcoreOperation,
					EdeltaValidator.INTERPRETER_TIMEOUT,
					initialIndex, input.lastIndexOf("}") - initialIndex + 1,
					"Timeout interpreting initialization block"
				)
			]
		]
	}

	def protected assertAfterInterpretationOfEdeltaManipulationExpression(CharSequence input, (EClass)=>void testExecutor) {
		assertAfterInterpretationOfEdeltaManipulationExpression(input, true, testExecutor)
	}

	def protected assertAfterInterpretationOfEdeltaManipulationExpression(CharSequence input, boolean doValidate, (EClass)=>void testExecutor) {
		val program = input.parseWithTestEcore
		if (doValidate) {
			program.assertNoErrors
		}
		assertAfterInterpretationOfEdeltaManipulationExpression(interpreter, program, doValidate, testExecutor)
	}

	def protected assertAfterInterpretationOfEdeltaModifyEcoreOperation(
		CharSequence input, (EPackage)=>void testExecutor
	) {
		assertAfterInterpretationOfEdeltaModifyEcoreOperation(input, true, testExecutor)
	}

	def protected assertAfterInterpretationOfEdeltaModifyEcoreOperation(
		CharSequence input, boolean doValidate, (EPackage)=>void testExecutor
	) {
		val program = input.parseWithTestEcore
		if (doValidate) {
			program.assertNoErrors
		}
		assertAfterInterpretationOfEdeltaModifyEcoreOperation(interpreter, program, doValidate, testExecutor)
	}

}
