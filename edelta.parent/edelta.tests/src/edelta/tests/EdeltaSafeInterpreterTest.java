package edelta.tests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

import java.util.function.Consumer;

import org.eclipse.emf.ecore.EPackage;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.XtextRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.inject.Inject;
import com.google.inject.Injector;

import edelta.edelta.EdeltaProgram;
import edelta.interpreter.EdeltaInterpreterFactory;
import edelta.interpreter.EdeltaInterpreterRuntimeException;
import edelta.interpreter.EdeltaSafeInterpreter;
import edelta.tests.injectors.EdeltaInjectorProviderDerivedStateComputerWithoutSafeInterpreter;

@RunWith(XtextRunner.class)
@InjectWith(EdeltaInjectorProviderDerivedStateComputerWithoutSafeInterpreter.class)
public class EdeltaSafeInterpreterTest extends EdeltaAbstractTest {
	@Inject
	private EdeltaSafeInterpreter interpreter;

	@Inject
	private Injector injector;

	@Before
	public void setupInterpreter() {
		// for standard tests we disable the timeout
		// actually we set it to several minutes
		// this also makes it easier to debug tests
		interpreter.setInterpreterTimeout(1200000);
	}

	@Test
	public void sanityTestCheck() throws Exception {
		// make sure we use the same interpreter implementation
		// in fact the interpreter can create another interpreter using the factory
		var interpreterFactory = injector
				.getInstance(EdeltaInterpreterFactory.class);
		var anotherInterprter = interpreterFactory.create(parseHelper.parse("").eResource());
		assertThat(anotherInterprter.getClass())
			.isSameAs(interpreter.getClass());
	}

	@Test
	public void testCorrectInterpretation() throws Exception {
		var input = """
				package test
				
				metamodel "foo"
				
				modifyEcore aTest epackage foo {
					addNewEClass("First")
				}
				""";
		assertAfterInterpretationOfEdeltaModifyEcoreOperation(
			parseWithTestEcore(input), ePackage -> 
				assertEquals("First",
					getLastEClass(ePackage).getName()));
	}

	@Test
	public void testOperationWithErrorsDueToWrongParsing() throws Exception {
		// differently from EdeltaInterpreterTest,
		// IllegalArgumentException is swallowed
		var input = """
				package test
				
				metamodel "foo"
				
				modifyEcore aTest epackage foo {
					addNewEClass("First")
					eclass First
				}
				""";
		assertAfterInterpretationOfEdeltaModifyEcoreOperation(
			parseWithTestEcore(input), ePackage -> {
				assertEquals("First", getLastEClass(ePackage).getName());
			});
	}

	@Test
	public void testCreateEClassAndCallOperationFromUseAsReferringToUnknownType() throws Exception {
		assertAfterInterpretationOfEdeltaModifyEcoreOperation(
			parseWithTestEcore("""
				metamodel "foo"
				
				use NonExistant as my
				
				modifyEcore aTest epackage foo {
					val c = addNewEClass("NewClass")
					my.createANewEAttribute(c)
				}
				"""),
			ePackage -> {
				var derivedEClass = getLastEClass(ePackage);
				assertEquals("NewClass", derivedEClass.getName());
			});
	}

	@Test(expected = EdeltaInterpreterRuntimeException.class)
	public void testEdeltaInterpreterRuntimeExceptionIsThrown() throws Exception {
		assertAfterInterpretationOfEdeltaModifyEcoreOperation(
			parseWithTestEcore("""
			import org.eclipse.emf.ecore.EClass
			import edelta.interpreter.EdeltaInterpreterRuntimeException
			
			metamodel "foo"
			
			def op(EClass c) : void {
				throw new EdeltaInterpreterRuntimeException("test")
			}
			
			modifyEcore aTest epackage foo {
				op(addNewEClass("NewClass"))
			}
			"""),
			it -> {
				// never gets here
			});
	}

	@Test
	public void testCreateEClassAndCallOperationThatThrows() throws Exception {
		assertAfterInterpretationOfEdeltaModifyEcoreOperation(
			parseWithTestEcore("""
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
			"""),
			it -> {
				// never gets here
			});
	}

	@Test
	public void testThrowNullPointerException() throws Exception {
		assertAfterInterpretationOfEdeltaModifyEcoreOperation(
			parseWithTestEcore("""
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
			"""),
			it -> {
				// never gets here
			});
	}

	private void assertAfterInterpretationOfEdeltaModifyEcoreOperation(EdeltaProgram program,
			Consumer<EPackage> testExecutor) throws Exception {
		interpreter.evaluateModifyEcoreOperations(program);
		var packageName = lastModifyEcoreOperation(program)
				.getEpackage().getName();
		var epackage =
			derivedStateHelper.getCopiedEPackagesMap(
				program.eResource()).get(packageName);
		testExecutor.accept(epackage);
	}
}
