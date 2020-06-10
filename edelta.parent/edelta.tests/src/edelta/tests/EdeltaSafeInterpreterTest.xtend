package edelta.tests

import com.google.inject.Inject
import com.google.inject.Injector
import edelta.edelta.EdeltaProgram
import edelta.interpreter.EdeltaInterpreterFactory
import edelta.interpreter.EdeltaInterpreterRuntimeException
import edelta.interpreter.EdeltaSafeInterpreter
import edelta.resource.derivedstate.EdeltaCopiedEPackagesMap
import org.eclipse.emf.ecore.EPackage
import org.eclipse.xtext.testing.InjectWith
import org.eclipse.xtext.testing.XtextRunner
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

import static org.assertj.core.api.Assertions.*
import static org.junit.Assert.*

@RunWith(XtextRunner)
@InjectWith(EdeltaInjectorProviderDerivedStateComputerWithoutSafeInterpreter)
class EdeltaSafeInterpreterTest extends EdeltaAbstractTest {

	@Inject EdeltaSafeInterpreter interpreter

	@Inject Injector injector

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
	def void testCorrectInterpretation() {
		val input = '''
			package test
			
			metamodel "foo"
			
			modifyEcore aTest epackage foo {
				addNewEClass("First")
			}
		'''
		input
		.parseWithTestEcore
		.assertAfterInterpretationOfEdeltaModifyEcoreOperation [ ePackage |
			val derivedEClass = ePackage.lastEClass
			assertEquals("First", derivedEClass.name)
		]
	}

	@Test
	def void testOperationWithErrorsDueToWrongParsing() {
		// differently from EdeltaInterpreterTest,
		// IllegalArgumentException is swallowed
		val input = '''
			package test
			
			metamodel "foo"
			
			modifyEcore aTest epackage foo {
				addNewEClass("First")
				eclass First
			}
		'''
		input
		.parseWithTestEcore
		.assertAfterInterpretationOfEdeltaModifyEcoreOperation [ ePackage |
			val derivedEClass = ePackage.lastEClass
			assertEquals("First", derivedEClass.name)
		]
	}

	@Test
	def void testCreateEClassAndCallOperationFromUseAsReferringToUnknownType() {
		// differently from EdeltaInterpreterTest,
		// IllegalStateException is swallowed
		'''
			metamodel "foo"
			
			use NonExistant as my
			
			modifyEcore aTest epackage foo {
				val c = addNewEClass("NewClass")
				my.createANewEAttribute(c)
			}
		'''
		.parseWithTestEcore
		.assertAfterInterpretationOfEdeltaModifyEcoreOperation [ ePackage |
			val derivedEClass = ePackage.lastEClass
			assertEquals("NewClass", derivedEClass.name)
		]
	}

	@Test(expected=EdeltaInterpreterRuntimeException)
	def void testEdeltaInterpreterRuntimeExceptionIsThrown() {
		'''
			import org.eclipse.emf.ecore.EClass
			import edelta.interpreter.EdeltaInterpreterRuntimeException
			
			metamodel "foo"
			
			def op(EClass c) : void {
				throw new EdeltaInterpreterRuntimeException("test")
			}
			
			modifyEcore aTest epackage foo {
				op(addNewEClass("NewClass"))
			}
		'''
		.parseWithTestEcore
		.assertAfterInterpretationOfEdeltaModifyEcoreOperation [
			// never gets here
		]
	}

	@Test // remove expected exception which the safe interpreter swallows
	def void testCreateEClassAndCallOperationThatThrows() {
		// differently from EdeltaInterpreterTest,
		// MyCustomException is swallowed
		'''
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
		'''
		.parseWithTestEcore
		.assertAfterInterpretationOfEdeltaModifyEcoreOperation [
			// never gets here
		]
	}

	def private assertAfterInterpretationOfEdeltaModifyEcoreOperation(
		EdeltaProgram program,
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
}
