package edelta.tests

import com.google.inject.Inject
import com.google.inject.Injector
import edelta.edelta.EdeltaPackage
import edelta.edelta.EdeltaProgram
import edelta.interpreter.EdeltaInterpreter
import edelta.interpreter.EdeltaInterpreter.EdeltaInterpreterWrapperException
import edelta.interpreter.EdeltaInterpreterRuntimeException
import edelta.interpreter.IEdeltaInterpreter
import edelta.tests.additional.MyCustomEdeltaThatCannotBeLoadedAtRuntime
import edelta.tests.additional.MyCustomException
import edelta.util.EdeltaCopiedEPackagesMap
import edelta.validation.EdeltaValidator
import org.eclipse.emf.ecore.EAttribute
import org.eclipse.emf.ecore.EPackage
import org.eclipse.xtext.testing.InjectWith
import org.eclipse.xtext.testing.XtextRunner
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

import static org.assertj.core.api.Assertions.*
import static org.junit.Assert.*

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
	def void testCreateEClassAndCallLibMethod() {
		'''
			metamodel "foo"
			
			modifyEcore aTest epackage foo {
				addNewEClass("NewClass") [
					EStructuralFeatures += newEAttribute("newTestAttr") [
						EType = ecoreref(FooDataType)
					]
				]
			}
		'''
		.assertAfterInterpretationOfEdeltaModifyEcoreOperation[ePackage |
			val derivedEClass = ePackage.lastEClass
			assertEquals("NewClass", derivedEClass.name)
			assertEquals(1, derivedEClass.EStructuralFeatures.size)
			val attr = derivedEClass.EStructuralFeatures.head
			assertEquals("newTestAttr", attr.name)
			assertEquals("FooDataType", attr.EType.name)
		]
	}

	@Test
	def void testCreateEClassAndCallJvmOperationFromSuperclass() {
		'''
			metamodel "foo"
			
			modifyEcore aTest epackage foo {
				addNewEClass("NewClass") [
					// call method from superclass AbstractEdelta
					EStructuralFeatures += ^createEAttribute(it, "aNewAttr", null)
				]
			}
		'''.assertAfterInterpretationOfEdeltaModifyEcoreOperation[ePackage |
			val derivedEClass = ePackage.lastEClass
			assertEquals("NewClass", derivedEClass.name)
			assertEquals(1, derivedEClass.EStructuralFeatures.size)
			val attr = derivedEClass.EStructuralFeatures.head
			assertEquals("aNewAttr", attr.name)
		]
	}

	@Test
	def void testCreateEClassAndCallOperationThatThrows() {
		assertThatThrownBy['''
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
		].isInstanceOf(EdeltaInterpreterWrapperException)
			.hasCauseExactlyInstanceOf(MyCustomException)
	}

	@Test
	def void testCreateEClassAndCallOperationFromUseAsReferringToUnknownType() {
		assertThatThrownBy[
		'''
			metamodel "foo"
			
			use NonExistant as my
			
			modifyEcore aTest epackage foo {
				val c = addNewEClass("NewClass")
				my.createANewEAttribute(c)
			}
		'''.assertAfterInterpretationOfEdeltaModifyEcoreOperation(false) [ /* will not get here */ ]
		].isInstanceOf(IllegalStateException)
			.hasMessageContaining("Cannot resolve proxy")
	}

	@Test
	def void testCreateEClassAndCallOperationFromUseAsButNotFoundAtRuntime() {
		// this is a simulation of what would happen if a type is resolved
		// but the interpreter cannot load it with Class.forName
		// because the ClassLoader cannot find it
		// https://github.com/LorenzoBettini/edelta/issues/69
		assertThatThrownBy[
		'''
			import edelta.tests.additional.MyCustomEdeltaThatCannotBeLoadedAtRuntime

			metamodel "foo"

			use MyCustomEdeltaThatCannotBeLoadedAtRuntime as my
			
			modifyEcore aTest epackage foo {
				my.aMethod()
			}
		'''.assertAfterInterpretationOfEdeltaModifyEcoreOperation(false) [ /* will not get here */ ]
		].isInstanceOf(EdeltaInterpreterRuntimeException)
			.hasMessageContaining('''The type '«MyCustomEdeltaThatCannotBeLoadedAtRuntime.name»' has been resolved but cannot be loaded by the interpreter''')
	}

	@Test
	def void testCreateEClassAndCreateEAttribute() {
		'''
			metamodel "foo"
			
			modifyEcore aTest epackage foo {
				addNewEClass("NewClass") [
					addNewEAttribute("newTestAttr", ecoreref(FooDataType)) [
						lowerBound = -1
					]
				]
			}
		'''.assertAfterInterpretationOfEdeltaModifyEcoreOperation[ePackage |
			val derivedEClass = ePackage.lastEClass
			assertEquals("NewClass", derivedEClass.name)
			assertEquals(1, derivedEClass.EStructuralFeatures.size)
			val attr = derivedEClass.EStructuralFeatures.head
			assertEquals("newTestAttr", attr.name)
			assertEquals(-1, attr.lowerBound)
			assertEquals("FooDataType", attr.EType.name)
		]
	}

	@Test
	def void testRenameEClassAndCreateEAttributeAndCallOperationFromUseAs() {
		'''
			import edelta.tests.additional.MyCustomEdelta
			
			metamodel "foo"
			
			use MyCustomEdelta as my
			
			modifyEcore aTest epackage foo {
				ecoreref(foo.FooClass).name = "Renamed"
				ecoreref(Renamed).addNewEAttribute("newTestAttr", ecoreref(FooDataType)) [
					my.setAttributeBounds(it, 1, -1)
				]
			}
		'''.assertAfterInterpretationOfEdeltaModifyEcoreOperation[ePackage |
			val derivedEClass = ePackage.firstEClass
			assertEquals("Renamed", derivedEClass.name)
			val attr = derivedEClass.EStructuralFeatures.last
			assertEquals("newTestAttr", attr.name)
			assertEquals(1, attr.lowerBound)
			assertEquals(-1, attr.upperBound)
			assertEquals("FooDataType", attr.EType.name)
		]
	}

	@Test
	def void testEClassCreatedFromUseAs() {
		useAsCustomEdeltaCreatingEClass
		.assertAfterInterpretationOfEdeltaModifyEcoreOperation[ePackage |
			val eClass = ePackage.lastEClass
			assertEquals("ANewClass", eClass.name)
			assertEquals("aNewAttr",
				(eClass.EStructuralFeatures.head as EAttribute).name
			)
		]
	}

	@Test
	def void testEClassCreatedFromUseAsAsExtension() {
		useAsCustomEdeltaAsExtensionCreatingEClass
		.assertAfterInterpretationOfEdeltaModifyEcoreOperation[ePackage |
			val eClass = ePackage.lastEClass
			assertEquals("ANewClass", eClass.name)
			assertEquals("aNewAttr",
				(eClass.EStructuralFeatures.head as EAttribute).name
			)
		]
	}

	@Test
	def void testEClassCreatedFromStatefulUseAs() {
		useAsCustomStatefulEdeltaCreatingEClass
		.assertAfterInterpretationOfEdeltaModifyEcoreOperation[ePackage |
			val eClass = ePackage.lastEClass
			assertEquals("ANewClass3", eClass.name)
			assertEquals("aNewAttr4",
				(eClass.EStructuralFeatures.head as EAttribute).name
			)
		]
	}

	@Test
	def void testNullBody() {
		val input = '''
			import org.eclipse.emf.ecore.EClass

			metamodel "foo"

			modifyEcore aTest epackage foo {
				addNewEClass("NewClass1")
			}
			// here the body is null, but the interpreter
			// avoids NPE
			modifyEcore aTest2 epackage foo
		'''
		input.assertAfterInterpretationOfEdeltaModifyEcoreOperation(false) [ ]
	}

	@Test
	def void testNullEcoreRefNamedElement() {
		val input = '''
			import org.eclipse.emf.ecore.EClass

			metamodel "foo"

			modifyEcore aTest epackage foo {
				addNewEClass("ANewClass1")
				ecoreref(
			}
		'''
		input.assertAfterInterpretationOfEdeltaModifyEcoreOperation(false) [ePackage |
			val eClass = ePackage.lastEClass
			assertEquals("ANewClass1", eClass.name)
		]
	}

	@Test(expected=IllegalArgumentException)
	def void testOperationWithErrorsDueToWrongParsing() {
		val input = '''
			package test
			
			metamodel "foo"
			
			modifyEcore aTest epackage foo {
				addNewEClass("First")
				eclass First
			}
		'''
		input.assertAfterInterpretationOfEdeltaModifyEcoreOperation(false) [ ]
	}

	@Test
	def void testUnresolvedEcoreReference() {
		val input = '''
			import org.eclipse.emf.ecore.EClass

			metamodel "foo"

			modifyEcore aTest epackage foo {
				addNewEClass("NewClass1")
				ecoreref(nonexist) // this won't break the interpreter
			}
		'''
		input.assertAfterInterpretationOfEdeltaModifyEcoreOperation(false) [ derivedEPackage |
			derivedEPackage.lastEClass => [
				assertEquals("NewClass1", name)
			]
		]
	}

	@Test
	def void testUnresolvedEcoreReferenceQualified() {
		val input = '''
			import org.eclipse.emf.ecore.EClass

			metamodel "foo"

			modifyEcore aTest epackage foo {
				addNewEClass("NewClass1")
				ecoreref(nonexist.bang) // this won't break the interpreter
			}
		'''
		input.assertAfterInterpretationOfEdeltaModifyEcoreOperation(false) [ derivedEPackage |
			derivedEPackage.lastEClass => [
				assertEquals("NewClass1", name)
			]
		]
	}

	@Test
	def void testModifyOperationCreateEClass() {
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
			derivedEPackage.lastEClass => [
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
			derivedEPackage.lastEClass => [
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
			derivedEPackage.firstEClass => [
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
			derivedEPackage.firstEClass => [
				assertEquals("RenamedClass", name)
				assertEquals("added", EStructuralFeatures.last.name)
			]
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
			
			modifyEcore aModificationTest epackage foo {
				EClassifiers += newEClass("ANewClass")
				op(EClassifiers.last as EClass)
			}
		'''
		input.assertAfterInterpretationOfEdeltaModifyEcoreOperation [ derivedEPackage |
			derivedEPackage.lastEClass => [
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

	@Test
	def void testCreateEClassInSubPackage() {
		'''
			metamodel "mainpackage"
			
			modifyEcore aTest epackage mainpackage {
				ecoreref(mainsubpackage).addNewEClass("NewClass") [
					EStructuralFeatures += newEAttribute("newTestAttr") [
						EType = ecoreref(MainFooDataType)
					]
				]
			}
		'''
		.parseWithTestEcoreWithSubPackage
		.assertAfterInterpretationOfEdeltaModifyEcoreOperation(true)[ePackage |
			val derivedEClass =
				ePackage.ESubpackages.head.lastEClass
			assertEquals("NewClass", derivedEClass.name)
			assertEquals(1, derivedEClass.EStructuralFeatures.size)
			val attr = derivedEClass.EStructuralFeatures.head
			assertEquals("newTestAttr", attr.name)
			assertEquals("MainFooDataType", attr.EType.name)
		]
	}

	@Test
	def void testCreateEClassInSubSubPackage() {
		'''
			metamodel "mainpackage"
			
			modifyEcore aTest epackage mainpackage {
				ecoreref(subsubpackage).addNewEClass("NewClass") [
					EStructuralFeatures += newEAttribute("newTestAttr") [
						EType = ecoreref(MainFooDataType)
					]
				]
			}
		'''
		.parseWithTestEcoreWithSubPackage
		.assertAfterInterpretationOfEdeltaModifyEcoreOperation(true)[ePackage |
			val derivedEClass =
				ePackage.ESubpackages.head.ESubpackages.head.lastEClass
			assertEquals("NewClass", derivedEClass.name)
			assertEquals(1, derivedEClass.EStructuralFeatures.size)
			val attr = derivedEClass.EStructuralFeatures.head
			assertEquals("newTestAttr", attr.name)
			assertEquals("MainFooDataType", attr.EType.name)
		]
	}

	@Test
	def void testCreateEClassInNewSubPackage() {
		'''
			import org.eclipse.emf.ecore.EcoreFactory
			
			metamodel "mainpackage"
			
			modifyEcore aTest epackage mainpackage {
				ESubpackages += EcoreFactory.eINSTANCE.createEPackage => [
					name = "anewsubpackage"
				]
				ecoreref(anewsubpackage).addNewEClass("NewClass") [
					EStructuralFeatures += newEAttribute("newTestAttr") [
						EType = ecoreref(MainFooDataType)
					]
				]
			}
		'''
		.parseWithTestEcoreWithSubPackage
		.assertAfterInterpretationOfEdeltaModifyEcoreOperation(true)[ePackage |
			val newSubPackage = ePackage.ESubpackages.last
			assertEquals("anewsubpackage", newSubPackage.name)
			val derivedEClass =
				ePackage.ESubpackages.last.lastEClass
			assertEquals("NewClass", derivedEClass.name)
			assertEquals(1, derivedEClass.EStructuralFeatures.size)
			val attr = derivedEClass.EStructuralFeatures.head
			assertEquals("newTestAttr", attr.name)
			assertEquals("MainFooDataType", attr.EType.name)
		]
	}

	@Test
	def void testComplexInterpretationWithRenamingAndSubPackages() {
		'''
			import org.eclipse.emf.ecore.EcoreFactory
			
			metamodel "mainpackage"
			
			modifyEcore aTest epackage mainpackage {
				ESubpackages += EcoreFactory.eINSTANCE.createEPackage => [
					name = "anewsubpackage"
				]
				ecoreref(anewsubpackage).addNewEClass("NewClass") [
					EStructuralFeatures += newEAttribute("newTestAttr") [
						EType = ecoreref(MainFooDataType)
					]
				]
				ecoreref(NewClass).name = "RenamedClass"
				ecoreref(RenamedClass).getEStructuralFeatures +=
					newEAttribute("added", ecoreref(MainFooDataType))
			}
		'''
		.parseWithTestEcoreWithSubPackage
		.assertAfterInterpretationOfEdeltaModifyEcoreOperation(true)[ePackage |
			val newSubPackage = ePackage.ESubpackages.last
			assertEquals("anewsubpackage", newSubPackage.name)
			val derivedEClass =
				ePackage.ESubpackages.last.lastEClass
			assertEquals("RenamedClass", derivedEClass.name)
			assertEquals(2, derivedEClass.EStructuralFeatures.size)
			val attr1 = derivedEClass.EStructuralFeatures.head
			assertEquals("newTestAttr", attr1.name)
			assertEquals("MainFooDataType", attr1.EType.name)
			val attr2 = derivedEClass.EStructuralFeatures.last
			assertEquals("added", attr2.name)
			assertEquals("MainFooDataType", attr2.EType.name)
		]
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
		assertAfterInterpretationOfEdeltaModifyEcoreOperation(program, doValidate, testExecutor)
	}

	def protected assertAfterInterpretationOfEdeltaModifyEcoreOperation(
		EdeltaProgram program, boolean doValidate, (EPackage)=>void testExecutor
	) {
		assertAfterInterpretationOfEdeltaModifyEcoreOperation(interpreter, program, doValidate, testExecutor)
		// validation after interpretation, since the interpreter
		// can make new elements available during validation
		if (doValidate) {
			program.assertNoErrors
		}
	}

	def private assertAfterInterpretationOfEdeltaModifyEcoreOperation(
		IEdeltaInterpreter interpreter, EdeltaProgram program,
		boolean doValidate, (EPackage)=>void testExecutor
	) {
		val it = program.lastModifyEcoreOperation
		// mimic the behavior of derived state computer that runs the interpreter
		// on copied EPackages, not on the original ones
		val copiedEPackagesMap =
			new EdeltaCopiedEPackagesMap(copiedEPackages.toMap[name])
		interpreter.evaluateModifyEcoreOperations(program, copiedEPackagesMap)
		val packageName = it.epackage.name
		val epackage = copiedEPackagesMap.get(packageName)
		testExecutor.apply(epackage)
	}

}
