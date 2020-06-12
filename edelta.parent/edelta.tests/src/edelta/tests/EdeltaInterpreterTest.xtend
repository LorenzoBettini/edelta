package edelta.tests

import com.google.inject.Inject
import com.google.inject.Injector
import edelta.edelta.EdeltaEcoreReference
import edelta.edelta.EdeltaPackage
import edelta.edelta.EdeltaProgram
import edelta.interpreter.EdeltaInterpreter
import edelta.interpreter.EdeltaInterpreterFactory
import edelta.interpreter.EdeltaInterpreterRuntimeException
import edelta.interpreter.EdeltaInterpreterWrapperException
import edelta.resource.derivedstate.EdeltaCopiedEPackagesMap
import edelta.resource.derivedstate.EdeltaDerivedStateHelper
import edelta.tests.additional.MyCustomEdeltaThatCannotBeLoadedAtRuntime
import edelta.tests.additional.MyCustomException
import edelta.validation.EdeltaValidator
import java.util.List
import org.eclipse.emf.ecore.EAttribute
import org.eclipse.emf.ecore.EPackage
import org.eclipse.xtext.testing.InjectWith
import org.eclipse.xtext.testing.XtextRunner
import org.eclipse.xtext.xbase.XbasePackage
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

import static org.assertj.core.api.Assertions.*
import static org.junit.Assert.*

@RunWith(XtextRunner)
@InjectWith(EdeltaInjectorProviderDerivedStateComputerWithoutInterpreter)
class EdeltaInterpreterTest extends EdeltaAbstractTest {

	@Inject EdeltaInterpreter interpreter

	@Inject Injector injector

	@Inject EdeltaDerivedStateHelper derivedStateHelper

	@Before
	def void setupInterpreter() {
		// for standard tests we disable the timeout
		// actually we set it to several minutes
		// this also makes it easier to debug tests
		interpreter.interpreterTimeout = 1200000;
	}

	@Test
	def void sanityTestCheck() {
		// make sure we use the same interpreter implementation
		// in fact the interpreter can create another interpreter using the factory
		val interpreterFactory = injector.getInstance(EdeltaInterpreterFactory)
		val anotherInterprter = interpreterFactory.create("".parse.eResource)
		assertThat(anotherInterprter.class)
			.isSameAs(interpreter.class)
	}

	@Test
	def void testCreateEClassAndCallLibMethod() {
		'''
			metamodel "foo"
			
			modifyEcore aTest epackage foo {
				addNewEClass("NewClass") [
					EStructuralFeatures += newEAttribute("newTestAttr", ecoreref(FooDataType))
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
	def void testThrowNullPointerException() {
		assertThatThrownBy['''
				import org.eclipse.emf.ecore.EClass
				import edelta.tests.additional.MyCustomException
				
				metamodel "foo"
				
				def op(EClass c) : void {
					throw new NullPointerException
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
			.hasCauseExactlyInstanceOf(NullPointerException)
	}


	@Test
	def void testCreateEClassAndCallOperationFromUseAsReferringToUnknownType() {
		'''
			metamodel "foo"
			
			use NonExistant as my
			
			modifyEcore aTest epackage foo {
				val c = addNewEClass("NewClass")
				my.createANewEAttribute(c) // this won't break the interpreter
				addNewEClass("AnotherNewClass")
			}
		'''.assertAfterInterpretationOfEdeltaModifyEcoreOperation(false) [derivedEPackage |
			derivedEPackage.lastEClass => [
				assertEquals("AnotherNewClass", name)
			]
		]
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
				ecoreref( // EdeltaEcoreReference.enamedelement is null
			}
		'''
		input.assertAfterInterpretationOfEdeltaModifyEcoreOperation(false) [ePackage |
			val eClass = ePackage.lastEClass
			assertEquals("ANewClass1", eClass.name)
		]
	}

	@Test
	def void testNullEcoreRef() {
		val input = '''
			import org.eclipse.emf.ecore.EClass

			metamodel "foo"

			modifyEcore aTest epackage foo {
				addNewEClass("ANewClass1")
				ecoreref // EdeltaEcoreReference is null
			}
		'''
		input.assertAfterInterpretationOfEdeltaModifyEcoreOperation(false) [ePackage |
			val eClass = ePackage.lastEClass
			assertEquals("ANewClass1", eClass.name)
		]
	}

	@Test
	def void testOperationWithErrorsDueToWrongParsing() {
		val input = '''
			package test
			
			metamodel "foo"
			
			modifyEcore aTest epackage foo {
				addNewEClass("NewClass1")
				eclass NewClass1 // this won't break the interpreter
				ecoreref(NewClass1).abstract = true
			}
		'''
		input.assertAfterInterpretationOfEdeltaModifyEcoreOperation(false) [derivedEPackage |
			derivedEPackage.lastEClass => [
				assertEquals("NewClass1", name)
				assertThat(isAbstract).isTrue
			]
		]
	}

	@Test
	def void testUnresolvedEcoreReference() {
		val input = '''
			import org.eclipse.emf.ecore.EClass

			metamodel "foo"

			modifyEcore aTest epackage foo {
				addNewEClass("NewClass1")
				ecoreref(nonexist) // this won't break the interpreter
				ecoreref(NewClass1).abstract = true
			}
		'''
		input.assertAfterInterpretationOfEdeltaModifyEcoreOperation(false) [ derivedEPackage |
			derivedEPackage.lastEClass => [
				assertEquals("NewClass1", name)
				assertThat(isAbstract).isTrue
			]
		]
	}

	@Test
	def void testUnresolvedEcoreReferenceMethodCall() {
		val input = '''
			import org.eclipse.emf.ecore.EClass

			metamodel "foo"

			modifyEcore aTest epackage foo {
				ecoreref(nonexist).abstract = true // this won't break the interpreter
				addNewEClass("NewClass1")
				ecoreref(NewClass1).abstract = true
			}
		'''
		input.assertAfterInterpretationOfEdeltaModifyEcoreOperation(false) [ derivedEPackage |
			derivedEPackage.lastEClass => [
				assertEquals("NewClass1", name)
				assertThat(isAbstract).isTrue
			]
		]
	}

	@Test
	def void testUnresolvedEcoreReferenceMethodCall2() {
		val input = '''
			import org.eclipse.emf.ecore.EClass

			metamodel "foo"

			modifyEcore aTest epackage foo {
				ecoreref(nonexist).ESuperTypes += ecoreref(MyClass) // this won't break the interpreter
				addNewEClass("NewClass1")
				ecoreref(NewClass1).abstract = true
			}
		'''
		input.assertAfterInterpretationOfEdeltaModifyEcoreOperation(false) [ derivedEPackage |
			derivedEPackage.lastEClass => [
				assertEquals("NewClass1", name)
				assertThat(isAbstract).isTrue
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
				ecoreref(RenamedClass).getEStructuralFeatures +=
					newEAttribute("added", ecoreref(FooDataType))
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
				ecoreref(RenamedClass).getEStructuralFeatures +=
					newEAttribute("added", ecoreref(FooDataType))
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
	def void testTimeoutWarning() {
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
				val offendingString = "Thread.sleep(1000)"
				val initialIndex = input.lastIndexOf(offendingString)
				assertWarning(
					XbasePackage.eINSTANCE.XMemberFeatureCall,
					EdeltaValidator.INTERPRETER_TIMEOUT,
					initialIndex, offendingString.length,
					"Timeout while interpreting"
				)
			]
		]
	}

	@Test
	def void testTimeoutWarningWhenCallingJavaCode() {
		// in this test we really need the timeout
		interpreter.interpreterTimeout = 2000;
		val input = '''
			import org.eclipse.emf.ecore.EClass
			import edelta.tests.additional.MyCustomEdeltaWithTimeout

			metamodel "foo"

			use MyCustomEdeltaWithTimeout as extension mylib
			
			modifyEcore aModificationTest epackage foo {
				EClassifiers += newEClass("ANewClass")
				op(EClassifiers.last as EClass)
			}
		'''
		input.assertAfterInterpretationOfEdeltaModifyEcoreOperation [ derivedEPackage |
			derivedEPackage.lastEClass => [
				assertEquals("ANewClass", name)
				assertEquals(false, abstract)
				val offendingString = "op(EClassifiers.last as EClass)"
				val initialIndex = input.lastIndexOf(offendingString)
				assertWarning(
					XbasePackage.eINSTANCE.XFeatureCall,
					EdeltaValidator.INTERPRETER_TIMEOUT,
					initialIndex, offendingString.length,
					"Timeout while interpreting"
				)
			]
		]
	}

	@Test
	def void testTimeoutWarningWithSeveralFiles() {
		// in this test we really need the timeout
		interpreter.interpreterTimeout = 2000;
		val lib1 = '''
			import org.eclipse.emf.ecore.EClass

			def op1(EClass c) : void {
				var i = 10;
				while (i >= 0) {
					Thread.sleep(1000);
					i++
				}
				// this will never be executed
				c.abstract = true
			}
		'''
		val lib2 = '''
			import org.eclipse.emf.ecore.EClass

			import edelta.__synthetic0

			use __synthetic0 as extension mylib1

			def op(EClass c) : void {
				op1(c)
			}
		'''
		val input = '''
			import org.eclipse.emf.ecore.EClass
			import edelta.__synthetic1
			
			metamodel "foo"
			
			use __synthetic1 as extension mylib
			
			modifyEcore aModificationTest epackage foo {
				EClassifiers += newEClass("ANewClass")
				op(EClassifiers.last as EClass)
			}
		'''
		assertAfterInterpretationOfEdeltaModifyEcoreOperation
		(#[lib1, lib2, input], true)
		[ derivedEPackage |
			derivedEPackage.lastEClass => [
				assertEquals("ANewClass", name)
				assertEquals(false, abstract)
				val offendingString = "op(EClassifiers.last as EClass)"
				val initialIndex = input.lastIndexOf(offendingString)
				assertWarning(
					XbasePackage.eINSTANCE.XFeatureCall,
					EdeltaValidator.INTERPRETER_TIMEOUT,
					initialIndex, offendingString.length,
					"Timeout while interpreting"
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
					EStructuralFeatures += newEAttribute("newTestAttr", ecoreref(MainFooDataType))
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
					EStructuralFeatures += newEAttribute("newTestAttr", ecoreref(MainFooDataType))
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
					EStructuralFeatures += newEAttribute("newTestAttr", ecoreref(MainFooDataType))
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
					EStructuralFeatures += newEAttribute("newTestAttr", ecoreref(MainFooDataType))
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

	@Test
	def void testInterpreterOnSubPackageIsNotExecuted() {
		'''
			metamodel "mainpackage.mainsubpackage"
			
			modifyEcore aTest epackage mainsubpackage {
				// this should not be executed
				throw new MyCustomException
			}
		'''
		.parseWithTestEcoreWithSubPackage
		.assertAfterInterpretationOfEdeltaModifyEcoreOperation(false)[ePackage |
			// nothing to check as long as no exception is thrown
			// (meaning that the interpreter is not executed on that modifyEcore op)
		]
	}

	@Test
	def void testModifyEcoreAndCallOperationFromExternalUseAs() {
		assertAfterInterpretationOfEdeltaModifyEcoreOperation(
		#['''
			import org.eclipse.emf.ecore.EClass

			package test1
			
			def op(EClass c) : void {
				c.op2
			}
			def op2(EClass c) : void {
				c.abstract = true
			}
		''',
		'''
			import org.eclipse.emf.ecore.EClass
			import test1.__synthetic0
			
			package test2
			
			metamodel "foo"
			
			use test1.__synthetic0 as my
			
			modifyEcore aModificationTest epackage foo {
				my.op(ecoreref(FooClass))
			}
		'''], true) [ derivedEPackage |
			derivedEPackage.firstEClass => [
				assertTrue(isAbstract)
			]
		]
	}

	@Test
	def void testModifyEcoreAndCallOperationFromExternalUseAsExtension() {
		assertAfterInterpretationOfEdeltaModifyEcoreOperation(
		#['''
			import org.eclipse.emf.ecore.EClass

			package test1
			
			def op(EClass c) : void {
				c.op2
			}
			def op2(EClass c) : void {
				c.abstract = true
			}
		''',
		'''
			import org.eclipse.emf.ecore.EClass
			import test1.__synthetic0
			
			package test2
			
			metamodel "foo"
			
			use test1.__synthetic0 as extension my
			
			modifyEcore aModificationTest epackage foo {
				ecoreref(FooClass).op
			}
		'''], true) [ derivedEPackage |
			derivedEPackage.firstEClass => [
				assertTrue(isAbstract)
			]
		]
	}

	@Test
	def void testModifyEcoreAndCallOperationFromExternalUseAsWithSeveralFiles() {
		assertAfterInterpretationOfEdeltaModifyEcoreOperation(
		#[
		'''
			import org.eclipse.emf.ecore.EClass

			package test1
			
			def op2(EClass c) : void {
				c.abstract = true
			}
		''',
		'''
			import org.eclipse.emf.ecore.EClass
			import test1.__synthetic0

			package test2
			
			use test1.__synthetic0 as extension my

			def op(EClass c) : void {
				c.op2
			}
		''',
		'''
			import org.eclipse.emf.ecore.EClass
			import test2.__synthetic1
			
			package test3
			
			metamodel "foo"
			
			use test2.__synthetic1 as extension my
			
			modifyEcore aModificationTest epackage foo {
				ecoreref(FooClass).op
			}
		'''], true) [ derivedEPackage |
			derivedEPackage.firstEClass => [
				assertTrue(isAbstract)
			]
		]
	}

	@Test def void testElementExpressionForCreatedEClassWithEdeltaAPI() {
		'''
			import org.eclipse.emf.ecore.EcoreFactory
			
			metamodel "foo"
			
			modifyEcore aTest epackage foo {
				addNewEClass("NewClass")
			}
		'''.parseWithTestEcore => [
			val map = interpretProgram
			val createClass = map.get("foo").getEClassifier("NewClass")
			val exp = derivedStateHelper
				.getEnamedElementXExpressionMap(eResource)
				.get(createClass)
			assertNotNull(exp)
			assertEquals("addNewEClass", exp.featureCall.feature.simpleName)
		]
	}

	@Test def void testEcoreRefExpExpressionForCreatedEClassWithEdeltaAPI() {
		'''
			import org.eclipse.emf.ecore.EcoreFactory
			
			metamodel "foo"
			
			modifyEcore aTest epackage foo {
				addNewEClass("NewClass")
			}
			modifyEcore anotherTest epackage foo {
				ecoreref(NewClass)
			}
		'''.parseWithTestEcore => [
			interpretProgram
			allEcoreReferenceExpressions.last => [
				// ecoreref(NewClass) -> addNewEClass
				assertEcoreRefExpElementMapsToXExpression
					(reference, "addNewEClass")
			]
		]
	}

	@Test def void testEcoreRefExpExpressionForCreatedEClassWithOperation() {
		'''
			import org.eclipse.emf.ecore.EPackage
			
			metamodel "foo"
			
			def create(EPackage it) {
				addNewEClass("NewClass")
			}
			modifyEcore anotherTest epackage foo {
				create(it)
				ecoreref(NewClass)
			}
		'''.parseWithTestEcore => [
			interpretProgram
			allEcoreReferenceExpressions.last => [
				// ecoreref(NewClass) -> addNewEClass
				assertEcoreRefExpElementMapsToXExpression
					(reference, "addNewEClass")
			]
		]
	}

	@Test def void testEcoreRefExpExpressionForCreatedEClassWithOperationInAnotherFile() {
		parseSeveralWithTestEcore(
		#[
		'''
			import org.eclipse.emf.ecore.EPackage
			
			def create(EPackage it) {
				addNewEClass("NewClass")
			}
		''',
		'''
			metamodel "foo"
			
			use edelta.__synthetic0 as extension my
			
			modifyEcore anotherTest epackage foo {
				create(it)
				ecoreref(NewClass)
			}
		'''
		]) => [
			interpretProgram
			allEcoreReferenceExpressions.last => [
				// ecoreref(NewClass) -> create
				assertEcoreRefExpElementMapsToXExpression
					(reference, "create")
			]
		]
	}

	@Test def void testEcoreRefExpForCreatedEClassRenamed() {
		'''
			import org.eclipse.emf.ecore.EcoreFactory
			
			metamodel "foo"
			
			modifyEcore aTest epackage foo {
				addNewEClass("NewClass")
				ecoreref(NewClass).name = "Renamed"
				ecoreref(Renamed)
			}
		'''.parseWithTestEcore => [
			interpretProgram
			val ecoreRefs = allEcoreReferenceExpressions
			ecoreRefs.head => [
				// ecoreref(NewClass) -> addNewEClass
				assertEcoreRefExpElementMapsToXExpression
					(reference, "addNewEClass")
			]
			ecoreRefs.last => [
				// ecoreref(Renamed) -> name = "Renamed"
				assertEcoreRefExpElementMapsToXExpression
					(reference, "setName")
			]
		]
	}

	@Test def void testEcoreRefExpForCreatedSubPackage() {
		'''
			import org.eclipse.emf.ecore.EcoreFactory
			
			metamodel "foo"
			
			modifyEcore aTest epackage foo {
				addNewEClass("NewClass")
				ecoreref(NewClass) // 0
				addNewESubpackage("subpackage", "subpackage", "subpackage") [
					addEClass(ecoreref(NewClass)) // 1
				]
				ecoreref(NewClass) // 2
				ecoreref(subpackage) // 3
			}
		'''.parseWithTestEcore => [
			interpretProgram
			allEcoreReferenceExpressions => [
				get(0) => [
					assertEcoreRefExpElementMapsToXExpression
						(reference, "addNewEClass")
				]
				get(1) => [
					assertEcoreRefExpElementMapsToXExpression
						(reference, "addNewEClass")
				]
				get(2) => [
					assertEcoreRefExpElementMapsToXExpression
						(reference, "addEClass")
				]
				get(3) => [
					assertEcoreRefExpElementMapsToXExpression
						(reference, "addNewESubpackage")
				]
			]
		]
	}

	@Test def void testEcoreRefExpForExistingEClass() {
		'''
			import org.eclipse.emf.ecore.EcoreFactory
			
			metamodel "foo"
			
			modifyEcore aTest epackage foo {
				ecoreref(FooClass)
			}
		'''.parseWithTestEcore => [
			interpretProgram
			val ecoreRefs = allEcoreReferenceExpressions
			ecoreRefs.head => [
				val exp = derivedStateHelper
					.getResponsibleExpression(reference)
				assertNull(exp)
			]
		]
	}

	@Test def void testEcoreRefExpForCreatedEClassRenamedInInitializer() {
		'''
			import org.eclipse.emf.ecore.EcoreFactory
			
			metamodel "foo"
			
			modifyEcore aTest epackage foo {
				addNewEClass("NewClass") [
					ecoreref(NewClass)
					name = "Renamed"
					abstract = true
				]
				ecoreref(Renamed)
			}
		'''.parseWithTestEcore => [
			interpretProgram
			val ecoreRefs = allEcoreReferenceExpressions
			ecoreRefs.head => [
				// ecoreref(NewClass) -> addNewEClass
				assertEcoreRefExpElementMapsToXExpression
					(reference, "addNewEClass")
			]
			ecoreRefs.last => [
				// ecoreref(Renamed) -> name = "Renamed"
				assertEcoreRefExpElementMapsToXExpression
					(reference, "setName")
			]
		]
	}

	@Test def void testElementExpressionMapForCreatedEClassWithEMFAPI() {
		'''
			import org.eclipse.emf.ecore.EcoreFactory
			
			metamodel "foo"
			
			modifyEcore aTest epackage foo {
				EClassifiers += EcoreFactory.eINSTANCE.createEClass
				EClassifiers.last.name = "NewClass"
			}
			modifyEcore anotherTest epackage foo {
				ecoreref(NewClass)
			}
		'''.parseWithTestEcore => [
			val map = interpretProgram
			val createClass = map.get("foo").getEClassifier("NewClass")
			val exp = derivedStateHelper
				.getEnamedElementXExpressionMap(eResource)
				.get(createClass)
			assertNotNull(exp)
			assertEquals("setName", exp.featureCall.feature.simpleName)
		]
	}

	@Test def void testElementExpressionMapForCreatedEClassWithDoubleArrow() {
		'''
			import org.eclipse.emf.ecore.EcoreFactory
			
			metamodel "foo"
			
			modifyEcore aTest epackage foo {
				EClassifiers += EcoreFactory.eINSTANCE.createEClass => [
					name = "NewClass"
				]
			}
			modifyEcore anotherTest epackage foo {
				ecoreref(NewClass)
			}
		'''.parseWithTestEcore => [
			val map = interpretProgram
			val createClass = map.get("foo").getEClassifier("NewClass")
			val exp = derivedStateHelper
				.getEnamedElementXExpressionMap(eResource)
				.get(createClass)
			assertNotNull(exp)
			assertEquals("setName", exp.featureCall.feature.simpleName)
		]
	}

	@Test def void testElementExpressionMapForCreatedEClassWithoutName() {
		'''
			import org.eclipse.emf.ecore.EcoreFactory
			
			metamodel "foo"
			
			modifyEcore aTest epackage foo {
				EClassifiers += EcoreFactory.eINSTANCE.createEClass
			}
		'''.parseWithTestEcore => [
			val map = interpretProgram
			val createClass = map.get("foo").lastEClass
			val exp = derivedStateHelper
				.getEnamedElementXExpressionMap(eResource)
				.get(createClass)
			assertNotNull(exp)
			assertEquals("operator_add", exp.featureCall.feature.simpleName)
		]
	}

	@Test def void testElementExpressionMapForCreatedEClassWithMethodCall() {
		'''
			import org.eclipse.emf.ecore.EcoreFactory
			
			metamodel "foo"
			
			modifyEcore aTest epackage foo {
				getEClassifiers().add(EcoreFactory.eINSTANCE.createEClass)
			}
		'''.parseWithTestEcore => [
			val map = interpretProgram
			val createClass = map.get("foo").lastEClass
			val exp = derivedStateHelper
				.getEnamedElementXExpressionMap(eResource)
				.get(createClass)
			assertNotNull(exp)
			assertEquals("add", exp.featureCall.feature.simpleName)
		]
	}

	@Test
	def void testReferenceToEClassRemoved() {
		val input = referenceToEClassRemoved.toString
		input.parseWithTestEcore =>[
			assertThatThrownBy[interpretProgram]
				.isInstanceOf(EdeltaInterpreterWrapperException)
			assertError(
				EdeltaPackage.eINSTANCE.edeltaEcoreReferenceExpression,
				EdeltaValidator.INTERPRETER_ACCESS_REMOVED_ELEMENT,
				input.lastIndexOf("FooClass"),
				"FooClass".length,
				"The element is not available anymore in this context: 'FooClass'"
			)
			assertErrorsAsStrings("The element is not available anymore in this context: 'FooClass'")
		]
	}

	@Test
	def void testReferenceToEClassRemovedInLoop() {
		val input = '''
			metamodel "foo"
			
			modifyEcore creation epackage foo {
				addNewEClass("NewClass1")
				for (var i = 0; i < 3; i++)
					EClassifiers -= ecoreref(NewClass1) // the second time it doesn't exist anymore
				addNewEClass("NewClass2")
				for (var i = 0; i < 3; i++)
					EClassifiers -= ecoreref(NewClass2) // the second time it doesn't exist anymore
			}
		'''
		input.parseWithTestEcore =>[
			interpretProgram
			assertError(
				EdeltaPackage.eINSTANCE.edeltaEcoreReferenceExpression,
				EdeltaValidator.INTERPRETER_ACCESS_REMOVED_ELEMENT,
				input.lastIndexOf("NewClass1"),
				"NewClass1".length,
				"The element is not available anymore in this context: 'NewClass1'"
			)
			assertError(
				EdeltaPackage.eINSTANCE.edeltaEcoreReferenceExpression,
				EdeltaValidator.INTERPRETER_ACCESS_REMOVED_ELEMENT,
				input.lastIndexOf("NewClass2"),
				"NewClass2".length,
				"The element is not available anymore in this context: 'NewClass2'"
			)
			// the error must appear only once per ecoreref expression
			assertErrorsAsStrings(
				'''
				The element is not available anymore in this context: 'NewClass1'
				The element is not available anymore in this context: 'NewClass2'
				'''
			)
		]
	}

	@Test
	def void testReferenceToCreatedEClassRemoved() {
		val input = '''
			metamodel "foo"
			
			modifyEcore creation epackage foo {
				addNewEClass("NewClass")
			}
			modifyEcore removed epackage foo {
				EClassifiers -= ecoreref(NewClass)
			}
			modifyEcore accessing epackage foo {
				ecoreref(NewClass) // this doesn't exist anymore
			}
		'''
		input.parseWithTestEcore =>[
			interpretProgram
			assertError(
				EdeltaPackage.eINSTANCE.edeltaEcoreReferenceExpression,
				EdeltaValidator.INTERPRETER_ACCESS_REMOVED_ELEMENT,
				input.lastIndexOf("NewClass"),
				"NewClass".length,
				"The element is not available anymore in this context: 'NewClass'"
			)
			assertErrorsAsStrings("The element is not available anymore in this context: 'NewClass'")
		]
	}

	@Test
	def void testReferenceToEClassRenamed() {
		val input = referenceToEClassRenamed.toString
		input
		.parseWithTestEcore => [
			interpretProgram
			assertError(
				EdeltaPackage.eINSTANCE.edeltaEcoreReferenceExpression,
				EdeltaValidator.INTERPRETER_ACCESS_RENAMED_ELEMENT,
				input.lastIndexOf("FooClass"),
				"FooClass".length,
				"The element 'FooClass' is now available as 'foo.Renamed'"
			)
			assertErrorsAsStrings("The element 'FooClass' is now available as 'foo.Renamed'")
		]
	}

	@Test
	def void testReferenceToCreatedEClassRenamed() {
		val input = referenceToCreatedEClassRenamed.toString
		input
		.parseWithTestEcore => [
			interpretProgram
			assertError(
				EdeltaPackage.eINSTANCE.edeltaEcoreReferenceExpression,
				EdeltaValidator.INTERPRETER_ACCESS_RENAMED_ELEMENT,
				input.lastIndexOf("NewClass"),
				"NewClass".length,
				"The element 'NewClass' is now available as 'foo.changed'"
			)
			assertErrorsAsStrings("The element 'NewClass' is now available as 'foo.changed'")
		]
	}

	@Test
	def void testShowErrorOnExistingEClass() {
		val input = '''
			metamodel "foo"
			
			modifyEcore aTest epackage foo {
				val found = EClassifiers.findFirst[
					name == "FooClass"
				]
				if (found !== null)
					showError(
						found,
						"Found class FooClass")
			}
		'''.toString
		input
		.parseWithTestEcore => [
			interpretProgram
			assertError(
				XbasePackage.eINSTANCE.XIfExpression,
				EdeltaValidator.LIVE_VALIDATION_ERROR,
				"Found class FooClass"
			)
			assertErrorsAsStrings("Found class FooClass")
		]
	}

	@Test
	def void testShowErrorOnCreatedEClass() {
		val input = '''
			metamodel "foo"
			
			modifyEcore aTest epackage foo {
				addNewEClass("NewClass")
				val found = EClassifiers.findFirst[
					name == "NewClass"
				]
				if (found !== null)
					showError(
						found,
						"Found class " + found.name)
			}
		'''.toString
		input
		.parseWithTestEcore => [
			interpretProgram
			assertError(
				XbasePackage.eINSTANCE.XFeatureCall,
				EdeltaValidator.LIVE_VALIDATION_ERROR,
				"Found class NewClass"
			)
			assertErrorsAsStrings("Found class NewClass")
		]
	}

	@Test
	def void testShowErrorOnCreatedEClassGeneratedByOperation() {
		val input = '''
			import org.eclipse.emf.ecore.EPackage

			metamodel "foo"
			
			def myCheck(EPackage it) {
				val found = EClassifiers.findFirst[
					name == "NewClass"
				]
				if (found !== null)
					showError(
						found,
						"Found class " + found.name)
			}
			
			modifyEcore aTest epackage foo {
				addNewEClass("NewClass")
				myCheck()
			}
		'''.toString
		input
		.parseWithTestEcore => [
			interpretProgram
			assertError(
				XbasePackage.eINSTANCE.XFeatureCall,
				EdeltaValidator.LIVE_VALIDATION_ERROR,
				input.lastIndexOf('addNewEClass("NewClass")'),
				'addNewEClass("NewClass")'.length,
				"Found class NewClass"
			)
			assertErrorsAsStrings("Found class NewClass")
		]
	}

	@Test
	def void testIntroducedCycles() {
		val input = '''
			metamodel "foo"
			
			modifyEcore aTest epackage foo {
				addNewEClass("C1")
				addNewEClass("C2") [ ESuperTypes += ecoreref(C1) ]
				addNewEClass("C3") [ ESuperTypes += ecoreref(C2) ]
				// cycle!
				ecoreref(C1).ESuperTypes += ecoreref(C3)
				addNewESubpackage("subpackage", "", "")
				// cycle
				ecoreref(subpackage).ESubpackages += it
				// the listener broke the cycle
				ecoreref(foo.subpackage) // valid
				ecoreref(subpackage.foo) // NOT valid
			}
		'''.toString
		input
		.parseWithTestEcore => [
			interpretProgram
			assertError(
				XbasePackage.eINSTANCE.XBinaryOperation,
				EdeltaValidator.ECLASS_CYCLE,
				input.lastIndexOf('ecoreref(C1).ESuperTypes += ecoreref(C3)'),
				'ecoreref(C1).ESuperTypes += ecoreref(C3)'.length,
				"Cycle in inheritance hierarchy: foo.C3"
			)
			assertError(
				XbasePackage.eINSTANCE.XBinaryOperation,
				EdeltaValidator.EPACKAGE_CYCLE,
				input.lastIndexOf('ecoreref(subpackage).ESubpackages += it'),
				'ecoreref(subpackage).ESubpackages += it'.length,
				"Cycle in superpackage/subpackage: foo.subpackage.foo"
			)
			assertErrorsAsStrings('''
			Cycle in inheritance hierarchy: foo.C3
			Cycle in superpackage/subpackage: foo.subpackage.foo
			foo cannot be resolved.
			''')
		]
	}

	def private assertAfterInterpretationOfEdeltaModifyEcoreOperation(
		CharSequence input, (EPackage)=>void testExecutor
	) {
		assertAfterInterpretationOfEdeltaModifyEcoreOperation(input, true, testExecutor)
	}

	def private assertAfterInterpretationOfEdeltaModifyEcoreOperation(
		CharSequence input, boolean doValidate, (EPackage)=>void testExecutor
	) {
		val program = input.parseWithTestEcore
		assertAfterInterpretationOfEdeltaModifyEcoreOperation(program, doValidate, testExecutor)
	}

	def private assertAfterInterpretationOfEdeltaModifyEcoreOperation(
		List<CharSequence> inputs, boolean doValidate, (EPackage)=>void testExecutor
	) {
		val program = parseSeveralWithTestEcore(inputs)
		assertAfterInterpretationOfEdeltaModifyEcoreOperation(program, doValidate, testExecutor)
	}

	def private assertAfterInterpretationOfEdeltaModifyEcoreOperation(
		EdeltaProgram program, boolean doValidate, (EPackage)=>void testExecutor
	) {
		assertAfterInterpretationOfEdeltaModifyEcoreOperation(interpreter, program, testExecutor)
		// validation after interpretation, since the interpreter
		// can make new elements available during validation
		if (doValidate) {
			program.assertNoErrors
		}
	}

	def private assertAfterInterpretationOfEdeltaModifyEcoreOperation(
		EdeltaInterpreter interpreter, EdeltaProgram program,
		(EPackage)=>void testExecutor
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

	def private interpretProgram(EdeltaProgram program) {
		// mimic the behavior of derived state computer that runs the interpreter
		// on copied EPackages, not on the original ones
		val copiedEPackagesMap =
			new EdeltaCopiedEPackagesMap(program.copiedEPackages.toMap[name])
		interpreter.evaluateModifyEcoreOperations(program, copiedEPackagesMap)
		return copiedEPackagesMap
	}

	private def void assertEcoreRefExpElementMapsToXExpression(
		EdeltaEcoreReference reference,
		String expectedFeatureCallSimpleName
	) {
		val exp = derivedStateHelper
			.getResponsibleExpression(reference)
		assertNotNull(exp)
		assertEquals(expectedFeatureCallSimpleName, exp.featureCall.feature.simpleName)
	}

}
