package edelta.tests;

import static com.google.common.collect.Iterables.filter;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.xtext.xbase.lib.IterableExtensions.head;
import static org.eclipse.xtext.xbase.lib.IterableExtensions.lastOrNull;
import static org.eclipse.xtext.xbase.lib.IterableExtensions.map;
import static org.eclipse.xtext.xbase.lib.ListExtensions.map;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.XtextRunner;
import org.eclipse.xtext.xbase.XbasePackage;
import org.eclipse.xtext.xbase.lib.IterableExtensions;
import org.eclipse.xtext.xbase.lib.Procedures.Procedure1;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.inject.Inject;
import com.google.inject.Injector;

import edelta.edelta.EdeltaEcoreArgument;
import edelta.edelta.EdeltaPackage;
import edelta.edelta.EdeltaProgram;
import edelta.interpreter.EdeltaInterpreter;
import edelta.interpreter.EdeltaInterpreterFactory;
import edelta.interpreter.EdeltaInterpreterRuntimeException;
import edelta.interpreter.EdeltaInterpreterWrapperException;
import edelta.resource.derivedstate.EdeltaCopiedEPackagesMap;
import edelta.tests.additional.EdeltaEContentAdapter.EdeltaEContentAdapterException;
import edelta.tests.additional.MyCustomEdeltaThatCannotBeLoadedAtRuntime;
import edelta.tests.additional.MyCustomException;
import edelta.tests.injectors.EdeltaInjectorProviderDerivedStateComputerWithoutInterpreter;
import edelta.validation.EdeltaValidator;

@RunWith(XtextRunner.class)
@InjectWith(EdeltaInjectorProviderDerivedStateComputerWithoutInterpreter.class)
public class EdeltaInterpreterTest extends EdeltaAbstractTest {
	@Inject
	private EdeltaInterpreter interpreter;

	@Inject
	private Injector injector;

	private EdeltaProgram currentProgram;

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
		EdeltaInterpreterFactory interpreterFactory = injector
				.getInstance(EdeltaInterpreterFactory.class);
		var anotherInterprter = interpreterFactory.create(parseHelper.parse("").eResource());
		assertThat(anotherInterprter.getClass())
			.isSameAs(interpreter.getClass());
	}

	@Test
	public void makeSureModificationsToOriginalEPackageAreDetected() throws Exception {
		var prog = parseWithTestEcore("""
		metamodel "foo"

		modifyEcore aTest epackage foo {
		}
		""");
		var firstMetamodel = prog.getEPackages().get(0);
		assertThatThrownBy(() -> {
			firstMetamodel.setName("changed");
		})
		.isInstanceOf(EdeltaEContentAdapterException.class);
	}

	@Test
	public void testCreateEClassAndCallLibMethod() throws Exception {
		assertAfterInterpretationOfEdeltaModifyEcoreOperation("""
		metamodel "foo"

		modifyEcore aTest epackage foo {
			addNewEClass("NewClass") [
				EStructuralFeatures += newEAttribute("newTestAttr", ecoreref(FooDataType))
			]
		}
		""", ePackage -> {
			var derivedEClass = getLastEClass(ePackage);
			assertEquals("NewClass", derivedEClass.getName());
			assertEquals(1, derivedEClass.getEStructuralFeatures().size());
			var attr = derivedEClass.getEStructuralFeatures().get(0);
			assertEquals("newTestAttr", attr.getName());
			assertEquals("FooDataType", attr.getEType().getName());
		});
	}

	@Test
	public void testCreateEClassAndCallOperationThatThrows() {
		assertThatThrownBy(() -> {
			assertAfterInterpretationOfEdeltaModifyEcoreOperation("""
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
			""", it -> {
				// never gets here
			});
		}).isInstanceOf(EdeltaInterpreterWrapperException.class)
				.hasCauseExactlyInstanceOf(MyCustomException.class);
	}

	@Test
	public void testThrowNullPointerException() {
		assertThatThrownBy(() -> {
			assertAfterInterpretationOfEdeltaModifyEcoreOperation("""
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
			""", it -> {
				// never gets here
			});
		}).isInstanceOf(EdeltaInterpreterWrapperException.class)
				.hasCauseExactlyInstanceOf(NullPointerException.class);
	}

	@Test
	public void testCreateEClassAndCallOperationFromUseAsReferringToUnknownType() throws Exception {
		assertAfterInterpretationOfEdeltaModifyEcoreOperation("""
		metamodel "foo"

		use NonExistant as my

		modifyEcore aTest epackage foo {
			val c = addNewEClass("NewClass")
			my.createANewEAttribute(c) // this won't break the interpreter
			addNewEClass("AnotherNewClass")
		}
		""",
		false,
		derivedEPackage -> {
			assertEquals("AnotherNewClass",
				getLastEClass(derivedEPackage).getName());
		});
	}

	@Test
	public void testCreateEClassAndCallOperationFromUseAsButNotFoundAtRuntime() {
		// this is a simulation of what would happen if a type is resolved
		// but the interpreter cannot load it with Class.forName
		// because the ClassLoader cannot find it
		// https://github.com/LorenzoBettini/edelta/issues/69
		assertThatThrownBy(() -> {
			assertAfterInterpretationOfEdeltaModifyEcoreOperation(
			"""
			import edelta.tests.additional.MyCustomEdeltaThatCannotBeLoadedAtRuntime

			metamodel "foo"

			use MyCustomEdeltaThatCannotBeLoadedAtRuntime as my

			modifyEcore aTest epackage foo {
				my.aMethod()
			}
			""",
			false,
			it -> {
				// never gets here
			});
		})
		.isInstanceOf(EdeltaInterpreterRuntimeException.class)
		.hasMessageContaining(
			"The type \'" +
			MyCustomEdeltaThatCannotBeLoadedAtRuntime.class.getName() +
			"\' has been resolved but cannot be loaded by the interpreter");
	}

	@Test
	public void testCreateEClassAndCreateEAttribute() throws Exception {
		assertAfterInterpretationOfEdeltaModifyEcoreOperation("""
		metamodel "foo"

		modifyEcore aTest epackage foo {
			addNewEClass("NewClass") [
				addNewEAttribute("newTestAttr", ecoreref(FooDataType)) [
					lowerBound = -1
				]
			]
		}
		""", ePackage -> {
			var derivedEClass = getLastEClass(ePackage);
			assertEquals("NewClass", derivedEClass.getName());
			assertEquals(1, derivedEClass.getEStructuralFeatures().size());
			var attr = derivedEClass.getEStructuralFeatures().get(0);
			assertEquals("newTestAttr", attr.getName());
			assertEquals((-1), attr.getLowerBound());
			assertEquals("FooDataType", attr.getEType().getName());
		});
	}

	@Test
	public void testRenameEClassAndCreateEAttributeAndCallOperationFromUseAs() throws Exception {
		assertAfterInterpretationOfEdeltaModifyEcoreOperation("""
		import edelta.tests.additional.MyCustomEdelta

		metamodel "foo"

		use MyCustomEdelta as my

		modifyEcore aTest epackage foo {
			ecoreref(foo.FooClass).name = "Renamed"
			ecoreref(Renamed).addNewEAttribute("newTestAttr", ecoreref(FooDataType)) [
				my.setAttributeBounds(it, 1, -1)
			]
		}
		""",
		ePackage -> {
			var derivedEClass = getFirstEClass(ePackage);
			assertEquals("Renamed", derivedEClass.getName());
			var attr =
					lastOrNull(derivedEClass.getEStructuralFeatures());
			assertEquals("newTestAttr", attr.getName());
			assertEquals(1, attr.getLowerBound());
			assertEquals(-1, attr.getUpperBound());
			assertEquals("FooDataType", attr.getEType().getName());
		});
	}

	@Test
	public void testEClassCreatedFromUseAs() throws Exception {
		assertAfterInterpretationOfEdeltaModifyEcoreOperation(
			inputs.useAsCustomEdeltaCreatingEClass(),
		ePackage -> {
			var eClass = getLastEClass(ePackage);
			assertEquals("ANewClass", eClass.getName());
			assertEquals("aNewAttr",
				((EAttribute) eClass.getEStructuralFeatures().get(0)).getName());
		});
	}

	@Test
	public void testEClassCreatedFromUseAsAsExtension() throws Exception {
		assertAfterInterpretationOfEdeltaModifyEcoreOperation(
			inputs.useAsCustomEdeltaAsExtensionCreatingEClass(),
		ePackage -> {
			var eClass = getLastEClass(ePackage);
			assertEquals("ANewClass", eClass.getName());
			assertEquals("aNewAttr",
				((EAttribute) eClass.getEStructuralFeatures().get(0)).getName());
		});
	}

	@Test
	public void testEClassCreatedFromStatefulUseAs() throws Exception {
		assertAfterInterpretationOfEdeltaModifyEcoreOperation(
			inputs.useAsCustomStatefulEdeltaCreatingEClass(),
		ePackage -> {
			var eClass = getLastEClass(ePackage);
			assertEquals("ANewClass3", eClass.getName());
			assertEquals("aNewAttr4",
				((EAttribute) eClass.getEStructuralFeatures().get(0)).getName());
		});
	}

	@Test
	public void testNullBody() throws Exception {
		assertAfterInterpretationOfEdeltaModifyEcoreOperation("""
		import org.eclipse.emf.ecore.EClass

		metamodel "foo"

		modifyEcore aTest epackage foo {
			addNewEClass("NewClass1")
		}
		// here the body is null, but the interpreter
		// avoids NPE
		modifyEcore aTest2 epackage foo
		""",
		false, it -> {});
	}

	@Test
	public void testNullEcoreRefNamedElement() throws Exception {
		var input = """
		import org.eclipse.emf.ecore.EClass

		metamodel "foo"

		modifyEcore aTest epackage foo {
			addNewEClass("ANewClass1")
			ecoreref( // EdeltaEcoreReference.enamedelement is null
		}
		""";
		assertAfterInterpretationOfEdeltaModifyEcoreOperation(
			input,
			false,
			ePackage -> {
				var eClass = getLastEClass(ePackage);
				assertEquals("ANewClass1", eClass.getName());
			});
	}

	@Test
	public void testNullEcoreRef() throws Exception {
		var input = """
		import org.eclipse.emf.ecore.EClass

		metamodel "foo"

		modifyEcore aTest epackage foo {
			addNewEClass("ANewClass1")
			ecoreref // EdeltaEcoreReference is null
		}
		""";
		assertAfterInterpretationOfEdeltaModifyEcoreOperation(
			input,
			false,
			ePackage -> {
				var eClass = getLastEClass(ePackage);
				assertEquals("ANewClass1", eClass.getName());
			});
	}

	@Test
	public void testOperationWithErrorsDueToWrongParsing() throws Exception {
		var input = """
		package test

		metamodel "foo"

		modifyEcore aTest epackage foo {
			addNewEClass("NewClass1")
			eclass NewClass1 // this won't break the interpreter
			ecoreref(NewClass1).abstract = true
		}
		""";
		assertAfterInterpretationOfEdeltaModifyEcoreOperation(
			input,
			false,
			derivedEPackage -> {
				var lastEClass = getLastEClass(derivedEPackage);
				assertEquals("NewClass1", lastEClass.getName());
				assertThat(lastEClass.isAbstract()).isTrue();
			});
	}

	@Test
	public void testUnresolvedEcoreReference() throws Exception {
		var input = """
		import org.eclipse.emf.ecore.EClass

		metamodel "foo"

		modifyEcore aTest epackage foo {
			addNewEClass("NewClass1")
			ecoreref(nonexist) // this won't break the interpreter
			ecoreref(NewClass1).abstract = true
		}
		""";
		assertAfterInterpretationOfEdeltaModifyEcoreOperation(
			input,
			false,
			derivedEPackage -> {
				var lastEClass = getLastEClass(derivedEPackage);
				assertEquals("NewClass1", lastEClass.getName());
				assertThat(lastEClass.isAbstract()).isTrue();
			});
	}

	@Test
	public void testUnresolvedEcoreReferenceMethodCall() throws Exception {
		var input = """
		import org.eclipse.emf.ecore.EClass

		metamodel "foo"

		modifyEcore aTest epackage foo {
			ecoreref(nonexist).abstract = true // this won't break the interpreter
			addNewEClass("NewClass1")
			ecoreref(NewClass1).abstract = true
		}
		""";
		assertAfterInterpretationOfEdeltaModifyEcoreOperation(
			input,
			false,
			derivedEPackage -> {
				var lastEClass = getLastEClass(derivedEPackage);
				assertEquals("NewClass1", lastEClass.getName());
				assertThat(lastEClass.isAbstract()).isTrue();
			});
	}

	@Test
	public void testUnresolvedEcoreReferenceMethodCall2() throws Exception {
		var input = """
		import org.eclipse.emf.ecore.EClass

		metamodel "foo"

		modifyEcore aTest epackage foo {
			ecoreref(nonexist).ESuperTypes += ecoreref(MyClass) // this won't break the interpreter
			addNewEClass("NewClass1")
			ecoreref(NewClass1).abstract = true
		}
		""";
		assertAfterInterpretationOfEdeltaModifyEcoreOperation(
			input,
			false,
			derivedEPackage -> {
				var lastEClass = getLastEClass(derivedEPackage);
				assertEquals("NewClass1", lastEClass.getName());
				assertThat(lastEClass.isAbstract()).isTrue();
			});
	}

	@Test
	public void testUnresolvedEcoreReferenceMethodCall3() throws Exception {
		var input = """
		import org.eclipse.emf.ecore.EClass

		metamodel "foo"

		modifyEcore creation epackage foo {
			addNewEClass("NewClass")
			// note that ESuperTypes is resolved, but not the argument
			ecoreref(NewClass).ESuperTypes += ecoreref(AnotherNewClass) // this won't break the interpreter
			addNewEClass("AnotherNewClass")
			// the next one is not resolved, BAD
			ecoreref(AnotherNewClass).abstract = true
		}
		""";
		assertAfterInterpretationOfEdeltaModifyEcoreOperation(
			input,
			false,
			derivedEPackage -> {
				var lastEClass = getLastEClass(derivedEPackage);
				assertEquals("AnotherNewClass", lastEClass.getName());
				assertThat(lastEClass.isAbstract()).isTrue();
			});
	}

	@Test
	public void testUnresolvedEcoreReferenceQualified() throws Exception {
		var input = """
		import org.eclipse.emf.ecore.EClass

		metamodel "foo"

		modifyEcore aTest epackage foo {
			addNewEClass("NewClass1")
			ecoreref(nonexist.bang) // this won't break the interpreter
		}
		""";
		assertAfterInterpretationOfEdeltaModifyEcoreOperation(
			input,
			false,
			derivedEPackage -> {
				assertEquals("NewClass1",
					getLastEClass(derivedEPackage).getName());
			});
	}

	@Test
	public void testModifyOperationCreateEClass() throws Exception {
		var input = """
		package test

		metamodel "foo"

		modifyEcore aModificationTest epackage foo {
			EClassifiers += newEClass("ANewClass") [
				ESuperTypes += newEClass("Base")
			]
		}
		""";
		assertAfterInterpretationOfEdeltaModifyEcoreOperation(
			input,
			derivedEPackage -> {
				var lastEClass = getLastEClass(derivedEPackage);
				assertEquals("ANewClass", lastEClass.getName());
				assertEquals("Base",
						lastOrNull(lastEClass.getESuperTypes()).getName());
			});
	}

	@Test
	public void testModifyEcoreAndCallOperation() throws Exception {
		assertAfterInterpretationOfEdeltaModifyEcoreOperation(
			"""
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
			""",
			derivedEPackage -> {
				var lastEClass = getLastEClass(derivedEPackage);
				assertEquals("ANewClass", lastEClass.getName());
				assertEquals("Base",
						lastOrNull(lastEClass.getESuperTypes()).getName());
				assertTrue(lastEClass.isAbstract());
			});
	}

	@Test
	public void testModifyEcoreRenameClassAndAddAttribute() throws Exception {
		assertAfterInterpretationOfEdeltaModifyEcoreOperation(
			"""
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
			""",
			true,
			derivedEPackage -> {
				var firstEClass = getFirstEClass(derivedEPackage);
				assertEquals("RenamedClass", firstEClass.getName());
				assertEquals("Base",
					lastOrNull(firstEClass.getESuperTypes()).getName());
				assertTrue(firstEClass.isAbstract());
				assertEquals("added",
					lastOrNull(firstEClass.getEStructuralFeatures()).getName());
			});
	}

	@Test
	public void testModifyEcoreRenameClassAndAddAttribute2() throws Exception {
		assertAfterInterpretationOfEdeltaModifyEcoreOperation(
			"""
			import org.eclipse.emf.ecore.EClass

			metamodel "foo"

			modifyEcore aTest epackage foo {
				ecoreref(foo.FooClass).name = "RenamedClass"
				ecoreref(RenamedClass).getEStructuralFeatures +=
					newEAttribute("added", ecoreref(FooDataType))
				ecoreref(RenamedClass.added)
			}
			""",
			true,
			derivedEPackage -> {
				var firstEClass = getFirstEClass(derivedEPackage);
				assertEquals("RenamedClass", firstEClass.getName());
				assertEquals("added",
					lastOrNull(firstEClass.getEStructuralFeatures()).getName());
			});
	}

	@Test
	public void testTimeoutWarning() throws Exception {
		// in this test we really need the timeout
		interpreter.setInterpreterTimeout(2000);
		var input = """
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
		""";
		assertAfterInterpretationOfEdeltaModifyEcoreOperation(
			input,
			derivedEPackage -> {
				var lastEClass = getLastEClass(derivedEPackage);
				assertEquals("ANewClass", lastEClass.getName());
				assertEquals(false, lastEClass.isAbstract());
				var offendingString = "Thread.sleep(1000)";
				var initialIndex = input.lastIndexOf(offendingString);
				validationTestHelper.assertWarning(
					currentProgram,
					XbasePackage.eINSTANCE.getXMemberFeatureCall(),
					EdeltaValidator.INTERPRETER_TIMEOUT,
					initialIndex,
					offendingString.length(),
					"Timeout while interpreting");
			});
	}

	@Test
	public void testTimeoutWarningWhenCallingJavaCode() throws Exception {
		// in this test we really need the timeout
		interpreter.setInterpreterTimeout(2000);
		var input = """
		import org.eclipse.emf.ecore.EClass
		import edelta.tests.additional.MyCustomEdeltaWithTimeout

		metamodel "foo"

		use MyCustomEdeltaWithTimeout as extension mylib

		modifyEcore aModificationTest epackage foo {
			EClassifiers += newEClass("ANewClass")
			op(EClassifiers.last as EClass)
		}
		""";
		assertAfterInterpretationOfEdeltaModifyEcoreOperation(
			input,
			derivedEPackage -> {
				var lastEClass = getLastEClass(derivedEPackage);
				assertEquals("ANewClass", lastEClass.getName());
				assertEquals(false, lastEClass.isAbstract());
				var offendingString = "op(EClassifiers.last as EClass)";
				var initialIndex = input.lastIndexOf(offendingString);
				validationTestHelper.assertWarning(
					currentProgram,
					XbasePackage.eINSTANCE.getXFeatureCall(),
					EdeltaValidator.INTERPRETER_TIMEOUT,
					initialIndex, offendingString.length(),
					"Timeout while interpreting");
			});
	}

	@Test
	public void testTimeoutWarningWithSeveralFiles() throws Exception {
		// in this test we really need the timeout
		interpreter.setInterpreterTimeout(2000);
		var lib1 = """
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
		""";
		var lib2 = """
		import org.eclipse.emf.ecore.EClass

		import edelta.__synthetic0

		use __synthetic0 as extension mylib1

		def op(EClass c) : void {
			op1(c)
		}
		""";
		var input = """
		import org.eclipse.emf.ecore.EClass
		import edelta.__synthetic1

		metamodel "foo"

		use __synthetic1 as extension mylib

		modifyEcore aModificationTest epackage foo {
			EClassifiers += newEClass("ANewClass")
			op(EClassifiers.last as EClass)
		}
		""";
		assertAfterInterpretationOfEdeltaModifyEcoreOperation(
			List.of(lib1, lib2, input),
			true,
			derivedEPackage -> {
				var lastEClass = getLastEClass(derivedEPackage);
				assertEquals("ANewClass", lastEClass.getName());
				assertEquals(Boolean.valueOf(false), Boolean.valueOf(lastEClass.isAbstract()));
				var offendingString = "op(EClassifiers.last as EClass)";
				var initialIndex = input.lastIndexOf(offendingString);
				validationTestHelper.assertWarning(
					currentProgram,
					XbasePackage.eINSTANCE.getXFeatureCall(),
					EdeltaValidator.INTERPRETER_TIMEOUT,
					initialIndex, offendingString.length(),
					"Timeout while interpreting");
			});
	}

	@Test
	public void testCreateEClassInSubPackage() throws Exception {
		assertAfterInterpretationOfEdeltaModifyEcoreOperation(
			parseWithTestEcoreWithSubPackage("""
			metamodel "mainpackage"

			modifyEcore aTest epackage mainpackage {
				ecoreref(mainsubpackage).addNewEClass("NewClass") [
					EStructuralFeatures += newEAttribute("newTestAttr", ecoreref(MainFooDataType))
				]
			}
			"""),
			true,
			ePackage -> {
				var derivedEClass = getLastEClass(ePackage.getESubpackages().get(0));
				assertEquals("NewClass", derivedEClass.getName());
				assertEquals(1, derivedEClass.getEStructuralFeatures().size());
				var attr = derivedEClass.getEStructuralFeatures().get(0);
				assertEquals("newTestAttr", attr.getName());
				assertEquals("MainFooDataType", attr.getEType().getName());
			});
	}

	@Test
	public void testCreateEClassInSubSubPackage() throws Exception {
		assertAfterInterpretationOfEdeltaModifyEcoreOperation(
			parseWithTestEcoreWithSubPackage("""
			metamodel "mainpackage"

			modifyEcore aTest epackage mainpackage {
				ecoreref(subsubpackage).addNewEClass("NewClass") [
					EStructuralFeatures += newEAttribute("newTestAttr", ecoreref(MainFooDataType))
				]
			}
			"""),
			true,
			ePackage -> {
				var derivedEClass = getLastEClass(ePackage.getESubpackages().get(0).getESubpackages().get(0));
				assertEquals("NewClass", derivedEClass.getName());
				assertEquals(1, derivedEClass.getEStructuralFeatures().size());
				var attr = derivedEClass.getEStructuralFeatures().get(0);
				assertEquals("newTestAttr", attr.getName());
				assertEquals("MainFooDataType", attr.getEType().getName());
			});
	}

	@Test
	public void testCreateEClassInNewSubPackage() throws Exception {
		assertAfterInterpretationOfEdeltaModifyEcoreOperation(
			parseWithTestEcoreWithSubPackage("""
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
			"""),
			true,
			ePackage -> {
				var newSubPackage = lastOrNull(ePackage.getESubpackages());
				assertEquals("anewsubpackage", newSubPackage.getName());
				var derivedEClass = getLastEClass(IterableExtensions.<EPackage>lastOrNull(ePackage.getESubpackages()));
				assertEquals("NewClass", derivedEClass.getName());
				assertEquals(1, derivedEClass.getEStructuralFeatures().size());
				var attr = derivedEClass.getEStructuralFeatures().get(0);
				assertEquals("newTestAttr", attr.getName());
				assertEquals("MainFooDataType", attr.getEType().getName());
			});
	}

	@Test
	public void testComplexInterpretationWithRenamingAndSubPackages() throws Exception {
		assertAfterInterpretationOfEdeltaModifyEcoreOperation(
			parseWithTestEcoreWithSubPackage("""
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
			"""),
			true,
			ePackage -> {
				var newSubPackage = IterableExtensions.<EPackage>lastOrNull(ePackage.getESubpackages());
				assertEquals("anewsubpackage", newSubPackage.getName());
				var derivedEClass = getLastEClass(IterableExtensions.<EPackage>lastOrNull(ePackage.getESubpackages()));
				assertEquals("RenamedClass", derivedEClass.getName());
				assertEquals(2, derivedEClass.getEStructuralFeatures().size());
				var attr1 = derivedEClass.getEStructuralFeatures().get(0);
				assertEquals("newTestAttr", attr1.getName());
				assertEquals("MainFooDataType", attr1.getEType().getName());
				var attr2 =
						lastOrNull(derivedEClass.getEStructuralFeatures());
				assertEquals("added", attr2.getName());
				assertEquals("MainFooDataType", attr2.getEType().getName());
			});
	}

	@Test
	public void testInterpreterOnSubPackageIsNotExecuted() throws Exception {
		assertAfterInterpretationOfEdeltaModifyEcoreOperation(
			parseWithTestEcoreWithSubPackage("""
			metamodel "mainpackage.mainsubpackage"

			modifyEcore aTest epackage mainsubpackage {
				// this should not be executed
				throw new MyCustomException
			}
			"""),
			false,
			it -> {
				// nothing to check as long as no exception is thrown
				// (meaning that the interpreter is not executed on that modifyEcore op)
			});
	}

	@Test
	public void testModifyEcoreAndCallOperationFromExternalUseAs() throws Exception {
		assertAfterInterpretationOfEdeltaModifyEcoreOperation(
			List.of("""
			import org.eclipse.emf.ecore.EClass

			package test1

			def op(EClass c) : void {
				c.op2
			}
			def op2(EClass c) : void {
				c.abstract = true
			}
			""", """
			import org.eclipse.emf.ecore.EClass
			import test1.__synthetic0

			package test2

			metamodel "foo"

			use test1.__synthetic0 as my

			modifyEcore aModificationTest epackage foo {
				my.op(ecoreref(FooClass))
			}
			"""),
			true,
			derivedEPackage -> {
				assertTrue(getFirstEClass(derivedEPackage).isAbstract());
			});
	}

	@Test
	public void testModifyEcoreAndCallOperationFromExternalUseAsExtension() throws Exception {
		assertAfterInterpretationOfEdeltaModifyEcoreOperation(
			List.of("""
			import org.eclipse.emf.ecore.EClass

			package test1

			def op(EClass c) : void {
				c.op2
			}
			def op2(EClass c) : void {
				c.abstract = true
			}
			""", """
			import org.eclipse.emf.ecore.EClass
			import test1.__synthetic0

			package test2

			metamodel "foo"

			use test1.__synthetic0 as extension my

			modifyEcore aModificationTest epackage foo {
				ecoreref(FooClass).op
			}
			"""),
			true,
			derivedEPackage -> {
				assertTrue(getFirstEClass(derivedEPackage).isAbstract());
			});
	}

	@Test
	public void testModifyEcoreAndCallOperationFromExternalUseAsWithSeveralFiles() throws Exception {
		assertAfterInterpretationOfEdeltaModifyEcoreOperation(
			List.of("""
			import org.eclipse.emf.ecore.EClass

			package test1

			def op2(EClass c) : void {
				c.abstract = true
			}
			""", """
			import org.eclipse.emf.ecore.EClass
			import test1.__synthetic0

			package test2

			use test1.__synthetic0 as extension my

			def op(EClass c) : void {
				c.op2
			}
			""", """
			import org.eclipse.emf.ecore.EClass
			import test2.__synthetic1

			package test3

			metamodel "foo"

			use test2.__synthetic1 as extension my

			modifyEcore aModificationTest epackage foo {
				ecoreref(FooClass).op
			}
			"""),
			true,
			derivedEPackage -> {
				assertTrue(getFirstEClass(derivedEPackage).isAbstract());
			});
	}

	@Test
	public void testModificationsOfMetamodelsAcrossSeveralFilesIntroducingDepOnAnotherMetamodel() throws Exception {
		var program = parseSeveralWithTestEcores(List.of(
		"""
		import org.eclipse.emf.ecore.EClass

		package test1

		metamodel "bar"

		def setBaseClass(EClass c) : void {
			c.getESuperTypes += ecoreref(BarClass)
		}
		""", """
		import org.eclipse.emf.ecore.EClass
		import test1.__synthetic0

		package test2

		metamodel "foo"

		use test1.__synthetic0 as extension my

		modifyEcore aModificationTest epackage foo {
			// the other file's operation will set the
			// base class of foo.FooClass to bar.BarClass
			ecoreref(FooClass).setBaseClass
			// now the foo package refers to bar package

			// now modify the bar's class
			ecoreref(FooClass).ESuperTypes.head.abstract = true

			// modify again the imported metamodel
			ecoreref(FooClass).name = "Renamed"
		}
		"""));
		assertAfterInterpretationOfEdeltaModifyEcoreOperation(
			program,
			true,
			derivedEPackage -> {
				var firstEClass = getFirstEClass(derivedEPackage);
				assertThat(map(firstEClass.getESuperTypes(), EClass::getName))
						.containsExactly("BarClass");
				assertThat(firstEClass.getESuperTypes().get(0).isAbstract()).isTrue();
				assertEquals("Renamed", firstEClass.getName());
			});
		assertThat(getCopiedEPackages(program))
			.extracting(EPackage::getName)
			.containsExactlyInAnyOrder("foo", "bar");
	}

	@Test
	public void testRenameReferencesAcrossEPackages() throws Exception {
		var it = parseWithTestEcoresWithReferences("""
		package test

		metamodel "testecoreforreferences1"
		metamodel "testecoreforreferences2"

		modifyEcore aTest1 epackage testecoreforreferences1 {
			// renames WorkPlace.persons to renamedPersons
			ecoreref(Person.works).EOpposite.name = "renamedPersons"
		}
		modifyEcore aTest2 epackage testecoreforreferences2 {
			// renames Person.works to renamedWorks
			// using the already renamed feature (was persons)
			ecoreref(renamedPersons).EOpposite.name = "renamedWorks"
		}
		""");
		var map = interpretProgram(it);
		var testecoreforreferences1 = map.get("testecoreforreferences1");
		var testecoreforreferences2 = map.get("testecoreforreferences2");
		var person = getEClassByName(testecoreforreferences1, "Person");
		assertThat(
			map(
			filter(person.getEStructuralFeatures(), EReference.class),
			EReference::getName))
				.containsOnly("renamedWorks");
		var workplace = getEClassByName(testecoreforreferences2, "WorkPlace");
		assertThat(
			map(
			filter(workplace.getEStructuralFeatures(), EReference.class),
			 EReference::getName))
				.containsOnly("renamedPersons");
		var unresolvedEcoreRefs = derivedStateHelper
				.getUnresolvedEcoreReferences(it.eResource());
		assertThat(unresolvedEcoreRefs).isEmpty();
	}

	@Test
	public void testRenameReferencesAcrossEPackagesModifyingOnePackageOnly() throws Exception {
		var model = parseWithTestEcoresWithReferences("""
		package test

		metamodel "testecoreforreferences1"
		metamodel "testecoreforreferences2"

		modifyEcore aTest1 epackage testecoreforreferences1 {
			// renames WorkPlace.persons to renamedPersons
			ecoreref(Person.works).EOpposite.name = "renamedPersons"
		}
		""");
		var map = interpretProgram(model);
		var testecoreforreferences1 = map.get("testecoreforreferences1");
		var testecoreforreferences2 = map.get("testecoreforreferences2");
		var person = getEClassByName(testecoreforreferences1, "Person");
		assertThat(
			map(
			filter(person.getEStructuralFeatures(), EReference.class),
			it -> it.getEOpposite().getName()))
				.containsOnly("renamedPersons");
		var workplace = getEClassByName(testecoreforreferences2, "WorkPlace");
		assertThat(
			map(
			filter(workplace.getEStructuralFeatures(), EReference.class),
			EReference::getName))
				.containsOnly("renamedPersons");
		var eOpposite =
			head(filter(person.getEStructuralFeatures(), EReference.class))
			.getEOpposite();
		var expected =
			head(filter(workplace.getEStructuralFeatures(), EReference.class));
		assertThat(eOpposite)
				.isSameAs(expected);
		var unresolvedEcoreRefs = derivedStateHelper
				.getUnresolvedEcoreReferences(model.eResource());
		assertThat(unresolvedEcoreRefs).isEmpty();
	}

	@Test
	public void testElementExpressionForCreatedEClassWithEdeltaAPI() // NOSONAR: parameterized test would not be feasible
			throws Exception {
		var it = parseWithTestEcore("""
		import org.eclipse.emf.ecore.EcoreFactory

		metamodel "foo"

		modifyEcore aTest epackage foo {
			addNewEClass("NewClass")
		}
		""");
		var map = interpretProgram(it);
		var createClass = map.get("foo").getEClassifier("NewClass");
		var exp =
			derivedStateHelper
			.getEnamedElementXExpressionMap(it.eResource()).get(createClass);
		assertNotNull(exp);
		assertEquals("addNewEClass",
			getFeatureCall(exp).getFeature().getSimpleName());
	}

	@Test
	public void testEcoreRefExpExpressionForCreatedEClassWithEdeltaAPI() throws Exception {
		var prog = parseWithTestEcore("""
		import org.eclipse.emf.ecore.EcoreFactory

		metamodel "foo"

		modifyEcore aTest epackage foo {
			addNewEClass("NewClass")
		}
		modifyEcore anotherTest epackage foo {
			ecoreref(NewClass)
		}
		""");
		interpretProgram(prog);
		var it =
				getLastOfAllEcoreReferenceExpressions(prog);
		// ecoreref(NewClass) -> addNewEClass
		assertEcoreRefExpElementMapsToXExpression(it.getArgument(),
				"addNewEClass");
	}

	@Test
	public void testEcoreRefExpExpressionForCreatedEClassWithOperation() throws Exception {
		var prog = parseWithTestEcore("""
		import org.eclipse.emf.ecore.EPackage

		metamodel "foo"

		def create(EPackage it) {
			addNewEClass("NewClass")
		}
		modifyEcore anotherTest epackage foo {
			create(it)
			ecoreref(NewClass)
		}
		""");
		interpretProgram(prog);
		var it =
				getLastOfAllEcoreReferenceExpressions(prog);
		// ecoreref(NewClass) -> addNewEClass
		assertEcoreRefExpElementMapsToXExpression(it.getArgument(),
				"addNewEClass");
	}

	@Test
	public void testEcoreRefExpExpressionForCreatedEClassWithOperationInAnotherFile() throws Exception {
		var prog = parseSeveralWithTestEcore(List.of(
		"""
		import org.eclipse.emf.ecore.EPackage

		def create(EPackage it) {
			addNewEClass("NewClass")
		}
		""", """
		metamodel "foo"

		use edelta.__synthetic0 as extension my

		modifyEcore anotherTest epackage foo {
			create(it)
			ecoreref(NewClass)
		}
		"""));
		interpretProgram(prog);
		var it =
				getLastOfAllEcoreReferenceExpressions(prog);
		// ecoreref(NewClass) -> create
		assertEcoreRefExpElementMapsToXExpression(it.getArgument(),
				"create");
	}

	@Test
	public void testEcoreRefExpForCreatedEClassRenamed() // NOSONAR: parameterized test would not be feasible
			throws Exception {
		var prog = parseWithTestEcore("""
		import org.eclipse.emf.ecore.EcoreFactory

		metamodel "foo"

		modifyEcore aTest epackage foo {
			addNewEClass("NewClass")
			ecoreref(NewClass).name = "Renamed"
			ecoreref(Renamed)
		}
		""");
		interpretProgram(prog);
		var ecoreRefs = getAllEcoreReferenceExpressions(prog);
		// ecoreref(NewClass) -> addNewEClass
		assertEcoreRefExpElementMapsToXExpression(
			ecoreRefs.get(0).getArgument(), "addNewEClass");
		// ecoreref(Renamed) -> name = "Renamed"
		assertEcoreRefExpElementMapsToXExpression(
			lastOrNull(ecoreRefs).getArgument(), "setName");
	}

	@Test
	public void testEcoreRefExpForCreatedSubPackage() throws Exception {
		var prog = parseWithTestEcore("""
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
		""");
		interpretProgram(prog);
		var it = getAllEcoreReferenceExpressions(prog);
		assertEcoreRefExpElementMapsToXExpression(
				it.get(0).getArgument(), "addNewEClass");
		assertEcoreRefExpElementMapsToXExpression(
				it.get(1).getArgument(), "addNewEClass");
		assertEcoreRefExpElementMapsToXExpression(
				it.get(2).getArgument(), "addEClass");
		assertEcoreRefExpElementMapsToXExpression(
				it.get(3).getArgument(), "addNewESubpackage");
	}

	@Test
	public void testEcoreRefExpForExistingEClass() throws Exception {
		var it = parseWithTestEcore("""
		import org.eclipse.emf.ecore.EcoreFactory

		metamodel "foo"

		modifyEcore aTest epackage foo {
			ecoreref(FooClass)
		}
		""");
		interpretProgram(it);
		assertNull(
			derivedStateHelper
				.getResponsibleExpression(
					getFirstOfAllEcoreReferenceExpressions(it)
						.getArgument()));
	}

	@Test
	public void testEcoreRefExpForCreatedEClassRenamedInInitializer() throws Exception {
		var prog = parseWithTestEcore("""
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
		""");
		interpretProgram(prog);
		var ecoreRefs = getAllEcoreReferenceExpressions(prog);
		// ecoreref(NewClass) -> addNewEClass
		assertEcoreRefExpElementMapsToXExpression(
			ecoreRefs.get(0).getArgument(), "addNewEClass");
		// ecoreref(Renamed) -> name = "Renamed"
		assertEcoreRefExpElementMapsToXExpression(
				lastOrNull(ecoreRefs).getArgument(), "setName");
	}

	@Test
	public void testEcoreRefExpForCreatedEClassRenamedInInitializer2() throws Exception {
		var prog = parseWithTestEcore("""
		import org.eclipse.emf.ecore.EcoreFactory

		metamodel "foo"

		modifyEcore aTest epackage foo {
			addNewEClass("NewClass") [
				ecoreref(NewClass).name = "Renamed"
				abstract = true
			]
			ecoreref(Renamed)
		}
		""");
		interpretProgram(prog);
		var ecoreRefs = getAllEcoreReferenceExpressions(prog);
		// ecoreref(NewClass) -> addNewEClass
		assertEcoreRefExpElementMapsToXExpression(
			ecoreRefs.get(0).getArgument(), "addNewEClass");
		// ecoreref(Renamed) -> name = "Renamed"
		assertEcoreRefExpElementMapsToXExpression(
			lastOrNull(ecoreRefs).getArgument(), "setName");
	}

	@Test
	public void testElementExpressionMapForCreatedEClassWithEMFAPI() throws Exception {
		var it = parseWithTestEcore("""
		import org.eclipse.emf.ecore.EcoreFactory

		metamodel "foo"

		modifyEcore aTest epackage foo {
			EClassifiers += EcoreFactory.eINSTANCE.createEClass
			EClassifiers.last.name = "NewClass"
		}
		modifyEcore anotherTest epackage foo {
			ecoreref(NewClass)
		}
		""");
		var map = interpretProgram(it);
		var createClass =
				map.get("foo").getEClassifier("NewClass");
		var exp =
			derivedStateHelper
			.getEnamedElementXExpressionMap(it.eResource()).get(createClass);
		assertNotNull(exp);
		assertEquals("setName",
			getFeatureCall(exp).getFeature().getSimpleName());
	}

	@Test
	public void testElementExpressionMapForCreatedEClassWithDoubleArrow() throws Exception {
		var it = parseWithTestEcore("""
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
		""");
		var map = interpretProgram(it);
		var createClass =
			map.get("foo").getEClassifier("NewClass");
		var exp =
			derivedStateHelper
			.getEnamedElementXExpressionMap(it.eResource()).get(createClass);
		assertNotNull(exp);
		assertEquals("setName",
			getFeatureCall(exp).getFeature().getSimpleName());
	}

	@Test
	public void testElementExpressionMapForCreatedEClassWithoutName() throws Exception {
		var it = parseWithTestEcore("""
		import org.eclipse.emf.ecore.EcoreFactory

		metamodel "foo"

		modifyEcore aTest epackage foo {
			EClassifiers += EcoreFactory.eINSTANCE.createEClass
		}
		""");
		var map = interpretProgram(it);
		var createClass = getLastEClass(map.get("foo"));
		var exp =
			derivedStateHelper
			.getEnamedElementXExpressionMap(it.eResource()).get(createClass);
		assertNotNull(exp);
		assertEquals("operator_add",
			getFeatureCall(exp).getFeature().getSimpleName());
	}

	@Test
	public void testElementExpressionMapForCreatedEClassWithMethodCall() throws Exception {
		var it = parseWithTestEcore("""
		import org.eclipse.emf.ecore.EcoreFactory

		metamodel "foo"

		modifyEcore aTest epackage foo {
			getEClassifiers().add(EcoreFactory.eINSTANCE.createEClass)
		}
		""");
		var map = interpretProgram(it);
		var createClass = getLastEClass(map.get("foo"));
		var exp =
			derivedStateHelper
			.getEnamedElementXExpressionMap(it.eResource()).get(createClass);
		assertNotNull(exp);
		assertEquals("add",
				getFeatureCall(exp).getFeature().getSimpleName());
	}

	@Test
	public void testReferenceToEClassRemoved() throws Exception {
		var input = inputs.referenceToEClassRemoved().toString();
		var prog = parseWithTestEcore(input);
		assertThatThrownBy(
			() -> interpretProgram(prog))
			.isInstanceOf(EdeltaInterpreterWrapperException.class);
		validationTestHelper.assertError(
			prog,
			EdeltaPackage.Literals.EDELTA_ECORE_REFERENCE_EXPRESSION,
			EdeltaValidator.INTERPRETER_ACCESS_REMOVED_ELEMENT,
			input.lastIndexOf("FooClass"), "FooClass".length(),
			"The element is not available anymore in this context: \'FooClass\'");
		assertErrorsAsStrings(prog,
			"The element is not available anymore in this context: \'FooClass\'");
	}

	@Test
	public void testReferenceToEClassDeleted() throws Exception {
		// EcoreUtil.delete sets to null also the ENamedElement of the ecoreref
		// see https://github.com/LorenzoBettini/edelta/issues/271
		var input = """
		import static org.eclipse.emf.ecore.util.EcoreUtil.delete

		metamodel "foo"

		modifyEcore aTest epackage foo {
			delete(ecoreref(FooClass))
			ecoreref(FooClass).abstract // this doesn't exist anymore
		}
		""";
		var it = parseWithTestEcore(input);
		assertThatThrownBy(() -> {
			interpretProgram(it);
		})
		.isInstanceOf(EdeltaInterpreterWrapperException.class);
		validationTestHelper.assertError(
			it,
			EdeltaPackage.Literals.EDELTA_ECORE_REFERENCE_EXPRESSION,
			EdeltaValidator.INTERPRETER_ACCESS_REMOVED_ELEMENT,
			input.lastIndexOf("FooClass"), "FooClass".length(),
			"The element is not available anymore in this context: \'FooClass\'");
		assertErrorsAsStrings(it,
			"The element is not available anymore in this context: \'FooClass\'");
	}

	@Test
	public void testReferenceToEClassRemovedInLoop() throws Exception {
		var input = """
		metamodel "foo"

		modifyEcore creation epackage foo {
			addNewEClass("NewClass1")
			for (var i = 0; i < 3; i++)
				EClassifiers -= ecoreref(NewClass1) // the second time it doesn't exist anymore
			addNewEClass("NewClass2")
			for (var i = 0; i < 3; i++)
				EClassifiers -= ecoreref(NewClass2) // the second time it doesn't exist anymore
		}
		""";
		var it = parseWithTestEcore(input);
		interpretProgram(it);
		validationTestHelper.assertError(
			it,
			EdeltaPackage.Literals.EDELTA_ECORE_REFERENCE_EXPRESSION,
			EdeltaValidator.INTERPRETER_ACCESS_REMOVED_ELEMENT,
			input.lastIndexOf("NewClass1"),
			"NewClass1".length(),
			"The element is not available anymore in this context: \'NewClass1\'");
		validationTestHelper.assertError(
			it,
			EdeltaPackage.Literals.EDELTA_ECORE_REFERENCE_EXPRESSION,
			EdeltaValidator.INTERPRETER_ACCESS_REMOVED_ELEMENT,
			input.lastIndexOf("NewClass2"),
			"NewClass2".length(),
			"The element is not available anymore in this context: \'NewClass2\'");
		// the error must appear only once per ecoreref expression
		assertErrorsAsStrings(it, """
		The element is not available anymore in this context: 'NewClass1'
		The element is not available anymore in this context: 'NewClass2'
		""");
	}

	@Test
	public void testReferenceToCreatedEClassRemoved() throws Exception {
		var input = """
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
		""";
		var it = parseWithTestEcore(input);
		interpretProgram(it);
		validationTestHelper.assertError(it,
			EdeltaPackage.Literals.EDELTA_ECORE_REFERENCE_EXPRESSION,
			EdeltaValidator.INTERPRETER_ACCESS_REMOVED_ELEMENT,
			input.lastIndexOf("NewClass"), "NewClass".length(),
			"The element is not available anymore in this context: \'NewClass\'");
		assertErrorsAsStrings(it,
			"The element is not available anymore in this context: \'NewClass\'");
	}

	@Test
	public void testReferenceToEClassRenamed() throws Exception {
		var input = inputs.referenceToEClassRenamed().toString();
		var it = parseWithTestEcore(input);
		interpretProgram(it);
		validationTestHelper.assertError(it,
			EdeltaPackage.Literals.EDELTA_ECORE_REFERENCE_EXPRESSION,
			EdeltaValidator.INTERPRETER_ACCESS_RENAMED_ELEMENT,
			input.lastIndexOf("FooClass"), "FooClass".length(),
			"The element \'FooClass\' is now available as \'foo.Renamed\'");
		assertErrorsAsStrings(it,
			"The element \'FooClass\' is now available as \'foo.Renamed\'");
	}

	@Test
	public void testReferenceToCreatedEClassRenamed() throws Exception {
		var input = inputs.referenceToCreatedEClassRenamed().toString();
		var it = parseWithTestEcore(input);
		interpretProgram(it);
		validationTestHelper.assertError(it,
			EdeltaPackage.Literals.EDELTA_ECORE_REFERENCE_EXPRESSION,
			EdeltaValidator.INTERPRETER_ACCESS_RENAMED_ELEMENT,
			input.lastIndexOf("NewClass"), "NewClass".length(),
			"The element \'NewClass\' is now available as \'foo.changed\'");
		assertErrorsAsStrings(it,
			"The element \'NewClass\' is now available as \'foo.changed\'");
	}

	@Test
	public void testShowErrorOnExistingEClass() throws Exception {
		var input = """
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
		""";
		var it = parseWithTestEcore(input);
		interpretProgram(it);
		validationTestHelper.assertError(it,
			XbasePackage.eINSTANCE.getXIfExpression(),
			EdeltaValidator.LIVE_VALIDATION_ERROR,
			"Found class FooClass");
		assertErrorsAsStrings(it, "Found class FooClass");
	}

	@Test
	public void testShowErrorOnCreatedEClass() throws Exception {
		var input = """
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
		""";
		var it = parseWithTestEcore(input);
		interpretProgram(it);
		validationTestHelper.assertError(it,
			XbasePackage.eINSTANCE.getXFeatureCall(),
			EdeltaValidator.LIVE_VALIDATION_ERROR,
			"Found class NewClass");
		assertErrorsAsStrings(it, "Found class NewClass");
	}

	@Test
	public void testShowErrorOnCreatedEClassGeneratedByOperation() throws Exception {
		var input = """
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
		""";
		var it = parseWithTestEcore(input);
		interpretProgram(it);
		validationTestHelper.assertError(it,
			XbasePackage.eINSTANCE.getXFeatureCall(),
			EdeltaValidator.LIVE_VALIDATION_ERROR,
			input.lastIndexOf("addNewEClass(\"NewClass\")"),
			"addNewEClass(\"NewClass\")".length(),
			"Found class NewClass");
		assertErrorsAsStrings(it, "Found class NewClass");
	}

	@Test
	public void testShowErrorOnCreatedEClassGeneratedByJavaOperation() throws Exception {
		// see https://github.com/LorenzoBettini/edelta/issues/289
		var input = """
		import edelta.tests.additional.MyCustomEdeltaShowingError

		metamodel "foo"

		use MyCustomEdeltaShowingError as extension my

		modifyEcore aTest epackage foo {
			checkClassName(addNewEClass("NewClass"))
			checkClassName(addNewEClass("anotherNewClass"))
		}
		""";
		var it = parseWithTestEcore(input);
		interpretProgram(it);
		var offendingString = "checkClassName(addNewEClass(\"anotherNewClass\"))";
		validationTestHelper.assertError(it,
			XbasePackage.eINSTANCE.getXFeatureCall(),
			EdeltaValidator.LIVE_VALIDATION_ERROR,
			input.lastIndexOf(offendingString), offendingString.length(),
			"Name should start with a capital: anotherNewClass");
		assertErrorsAsStrings(it,
			"Name should start with a capital: anotherNewClass");
	}

	@Test
	public void testShowErrorOnCreatedEClassGeneratedByExternalOperation() throws Exception {
		// see https://github.com/LorenzoBettini/edelta/issues/348
		List<String> inputs = List.of(
		"""
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
		""", """
		metamodel "foo"

		use edelta.__synthetic0 as extension my

		modifyEcore aTest epackage foo {
			addNewEClass("NewClass")
			myCheck()
		}
		""");
		var it = parseSeveralWithTestEcore(inputs);
		interpretProgram(it);
		validationTestHelper.assertError(it,
			XbasePackage.eINSTANCE.getXFeatureCall(),
			EdeltaValidator.LIVE_VALIDATION_ERROR,
			lastOrNull(inputs).lastIndexOf("addNewEClass(\"NewClass\")"),
			"addNewEClass(\"NewClass\")".length(),
			"Found class NewClass");
		assertErrorsAsStrings(it, "Found class NewClass");
	}

	@Test
	public void testShowErrorOnCreatedEClassGeneratedByExternalOperation2() throws Exception {
		// see https://github.com/LorenzoBettini/edelta/issues/348
		// like the previous one, but with one more level of indirection
		List<String> inputs = List.of(
		"""
		import org.eclipse.emf.ecore.EPackage

		metamodel "foo"

		def myCheckInternal(EPackage it) {
			val found = EClassifiers.findFirst[
				name == "NewClass"
			]
			if (found !== null)
				showError(
					found,
					"Found class " + found.name)
		}
		""", """
		import org.eclipse.emf.ecore.EPackage

		use edelta.__synthetic0 as extension my

		def myCheck(EPackage it) {
			myCheckInternal
		}
		""", """
		metamodel "foo"

		use edelta.__synthetic1 as extension my

		modifyEcore aTest epackage foo {
			addNewEClass("NewClass")
			myCheck()
		}
		""");
		var it = parseSeveralWithTestEcore(inputs);
		interpretProgram(it);
		validationTestHelper.assertError(it,
			XbasePackage.eINSTANCE.getXFeatureCall(),
			EdeltaValidator.LIVE_VALIDATION_ERROR,
			lastOrNull(inputs).lastIndexOf("addNewEClass(\"NewClass\")"),
			"addNewEClass(\"NewClass\")".length(),
			"Found class NewClass");
		assertErrorsAsStrings(it, "Found class NewClass");
	}

	@Test
	public void testIntroducedCycles() throws Exception {
		var input = """
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
		""";
		var prog = parseWithTestEcore(input);
		interpretProgram(prog);
		validationTestHelper.assertError(prog,
			XbasePackage.Literals.XBINARY_OPERATION,
			EdeltaValidator.ECLASS_CYCLE,
			input.lastIndexOf("ecoreref(C1).ESuperTypes += ecoreref(C3)"),
			"ecoreref(C1).ESuperTypes += ecoreref(C3)".length(),
			"Cycle in inheritance hierarchy: foo.C3");
		validationTestHelper.assertError(prog,
			XbasePackage.Literals.XBINARY_OPERATION,
			EdeltaValidator.EPACKAGE_CYCLE,
			input.lastIndexOf("ecoreref(subpackage).ESubpackages += it"),
			"ecoreref(subpackage).ESubpackages += it".length(),
			"Cycle in superpackage/subpackage: foo.subpackage.foo");
		assertErrorsAsStrings(prog, """
		Cycle in inheritance hierarchy: foo.C3
		Cycle in superpackage/subpackage: foo.subpackage.foo
		foo cannot be resolved.
		""");
	}

	@Test
	public void testAccessToNotYetExistingElement() throws Exception {
		var input = """
		metamodel "foo"

		modifyEcore aTest epackage foo {
			ecoreref(ANewClass) // doesn't exist yet
			ecoreref(NonExisting) // doesn't exist at all
			addNewEClass("ANewClass")
			ecoreref(ANewClass) // this is OK
		}
		""";
		var it = parseWithTestEcore(input);
		interpretProgram(it);
		var ecoreref1 = getAllEcoreReferenceExpressions(it).get(0).getArgument();
		var ecoreref2 = getAllEcoreReferenceExpressions(it).get(1).getArgument();
		var unresolved = derivedStateHelper
				.getUnresolvedEcoreReferences(it.eResource());
		assertThat(unresolved).containsOnly(ecoreref1, ecoreref2);
		// also check what's resolved in the end
		assertThat(ecoreref1.getElement().eIsProxy()).isFalse();
		assertThat(ecoreref2.getElement().eIsProxy()).isTrue();
		var map = derivedStateHelper.getEnamedElementXExpressionMap(it.eResource());
		// we can access the expression that created the element
		// that is not available in the current context
		assertThat(map.get(ecoreref1.getElement())).isNotNull()
			.isSameAs(
				getLastModifyEcoreOperationBlock(it).getExpressions().get(2));
	}

	@Test
	public void testAccessibleElements() throws Exception {
		var input = """
		metamodel "foo"

		modifyEcore aTest epackage foo {
			ecoreref(FooClass) // 0
			addNewEClass("ANewClass")
			ecoreref(ANewClass) // 1
			EClassifiers -= ecoreref(FooClass) // 2
			ecoreref(ANewClass) // 3
		}
		""";
		var it = parseWithTestEcore(input);
		interpretProgram(it);
		var exps = getAllEcoreReferenceExpressions(it);
		var ecoreref1 = exps.get(0);
		var ecoreref2 = exps.get(1);
		var ecoreref3 = exps.get(2);
		var ecoreref4 = exps.get(3);
		var elements1 = derivedStateHelper.getAccessibleElements(ecoreref1);
		assertAccessibleElements(elements1, """
		foo
		foo.FooClass
		foo.FooClass.myAttribute
		foo.FooClass.myReference
		foo.FooDataType
		foo.FooEnum
		foo.FooEnum.FooEnumLiteral
		""");
		var elements2 = derivedStateHelper.getAccessibleElements(ecoreref2);
		assertAccessibleElements(elements2, """
		foo
		foo.ANewClass
		foo.FooClass
		foo.FooClass.myAttribute
		foo.FooClass.myReference
		foo.FooDataType
		foo.FooEnum
		foo.FooEnum.FooEnumLiteral
		""");
		var elements3 = derivedStateHelper.getAccessibleElements(ecoreref3);
		// nothing has changed between ecoreref 2 and 3
		// so the available elements must be the same
		assertThat(elements2).isSameAs(elements3);
		var elements4 = derivedStateHelper.getAccessibleElements(ecoreref4);
		assertAccessibleElements(elements4, """
		foo
		foo.ANewClass
		foo.FooDataType
		foo.FooEnum
		foo.FooEnum.FooEnumLiteral
		""");
	}

	@Test
	public void testInvalidAmbiguousEcoreref() // NOSONAR: parameterized test would not be feasible
			throws Exception {
		var input = """
		metamodel "mainpackage"

		modifyEcore aTest epackage mainpackage {
			ecoreref(MyClass)
		}
		""";
		var it = parseWithTestEcoreWithSubPackage(input);
		interpretProgram(it);
		assertErrorsAsStrings(it, """
			Ambiguous reference 'MyClass':
			  mainpackage.MyClass
			  mainpackage.mainsubpackage.MyClass
			  mainpackage.mainsubpackage.subsubpackage.MyClass
			""");
	}

	@Test
	public void testAmbiguousEcorerefAfterRemoval() throws Exception {
		var input = """
		metamodel "mainpackage"

		modifyEcore aTest epackage mainpackage {
			EClassifiers -= ecoreref(mainpackage.MyClass)
			ecoreref(MyClass) // still ambiguous
		}
		""";
		var it = parseWithTestEcoreWithSubPackage(input);
		interpretProgram(it);
		assertErrorsAsStrings(it, """
			Ambiguous reference 'MyClass':
			  mainpackage.mainsubpackage.MyClass
			  mainpackage.mainsubpackage.subsubpackage.MyClass
			""");
	}

	@Test
	public void testNonAmbiguousEcorerefAfterRemoval() throws Exception {
		var input = """
		import static org.eclipse.emf.ecore.util.EcoreUtil.remove

		metamodel "mainpackage"

		modifyEcore aTest epackage mainpackage {
			EClassifiers -= ecoreref(mainpackage.MyClass)
			remove(ecoreref(mainpackage.mainsubpackage.subsubpackage.MyClass))
			ecoreref(MyClass) // non ambiguous
		}
		""";
		var it = parseWithTestEcoreWithSubPackage(input);
		var map = interpretProgram(it);
		validationTestHelper.assertNoErrors(it);
		// mainpackage.mainsubpackage.MyClass
		var mainSubPackageClass = getLastEClass(
				head(map.values()).getESubpackages().get(0));
		var lastEcoreRef = getLastOfAllEcoreReferenceExpressions(it).getArgument();
		assertNotNull(lastEcoreRef.getElement());
		// the non ambiguous ecoreref should be correctly linked
		// to the only available element in that context
		assertSame(mainSubPackageClass, lastEcoreRef.getElement());
	}

	@Test
	public void testNonAmbiguousEcorerefAfterRemovalIsCorrectlyTypedInAssignment() throws Exception {
		var input = """
		import org.eclipse.emf.ecore.EAttribute
		import org.eclipse.emf.ecore.EReference

		metamodel "mainpackage"

		modifyEcore aTest epackage mainpackage {
			addNewEClass("ANewClass") [
				addNewEAttribute("created", null)
			]
			addNewEClass("AnotherNewClass") [
				addNewEReference("created", null)
			]
			EClassifiers -= ecoreref(ANewClass)
			// "created" is not ambiguous anymore
			ecoreref(created)
			// and it's correctly typed (EReference, not EAttribute)
			val EAttribute a = ecoreref(created) // ERROR
			val EReference r = ecoreref(created) // OK
		}
		""";
		var it = parseWithTestEcoreWithSubPackage(input);
		interpretProgram(it);
		assertErrorsAsStrings(it,
			"Type mismatch: cannot convert from EReference to EAttribute");
	}

	@Test
	public void testNonAmbiguousEcorerefAfterRemovalIsCorrectlyTypedInFeatureCall2() throws Exception {
		var input = """
		import static org.eclipse.emf.ecore.util.EcoreUtil.remove

		metamodel "mainpackage"

		modifyEcore aTest epackage mainpackage {
			addNewEClass("created")
			ESubpackages.head.addNewESubpackage("created", null, null)
			// "created" is ambiguous now
			ecoreref(created)
			remove(EClassifiers.last)
			// "created" is not ambiguous anymore: linked to EPackage "created"
			ecoreref(created).EStructuralFeatures // ERROR
			ecoreref(created) => [
				abstract = true // ERROR
			]
			ecoreref(created).ESubpackages // OK
			ecoreref(created).nonExistent // ERROR to cover the last case in the interpreter
		}
		""";
		var it = parseWithTestEcoreWithSubPackage(input);
		interpretProgram(it);
		assertErrorsAsStrings(it, """
			Ambiguous reference 'created':
			  mainpackage.created
			  mainpackage.mainsubpackage.created
			Cannot refer to org.eclipse.emf.ecore.EClass.getEStructuralFeatures()
			Cannot refer to org.eclipse.emf.ecore.EClass.setAbstract(boolean)
			The method or field nonExistent is undefined for the type EPackage
			""");
	}

	@Test
	public void testInvalidAmbiguousEcorerefWithCreatedElements() throws Exception {
		var input = """
		metamodel "mainpackage"

		modifyEcore aTest epackage mainpackage {
			addNewEClass("created") [
				addNewEAttribute("created", null)
			]
			ecoreref(created)
		}
		""";
		var it = parseWithTestEcoreWithSubPackage(input);
		interpretProgram(it);
		assertErrorsAsStrings(it, """
			Ambiguous reference 'created':
			  mainpackage.created
			  mainpackage.created.created
			""");
	}

	@Test
	public void testNonAmbiguousEcorerefWithQualification() throws Exception {
		var input = """
		metamodel "mainpackage"

		modifyEcore aTest epackage mainpackage {
			addNewEClass("created") [
				addNewEAttribute("created", null)
			]
			ecoreref(created.created) // NON ambiguous
			ecoreref(mainpackage.created) // NON ambiguous
		}
		""";
		var it = parseWithTestEcoreWithSubPackage(input);
		interpretProgram(it);
		validationTestHelper.assertNoErrors(it);
	}

	@Test
	public void testNonAmbiguousEcoreref() throws Exception {
		var input = """
		metamodel "mainpackage"

		modifyEcore aTest epackage mainpackage {
			addNewEClass("WorkPlace")
			addNewEClass("LivingPlace")
			addNewEClass("Place")
			ecoreref(Place) // NON ambiguous
		}
		""";
		var it = parseWithTestEcoreWithSubPackage(input);
		interpretProgram(it);
		validationTestHelper.assertNoErrors(it);
	}

	@Test
	public void testCallOperationThatCallsAnotherNonVoidOperation() throws Exception {
		assertAfterInterpretationOfEdeltaModifyEcoreOperation("""
			metamodel "foo"

			def createANewClassInMyEcore(String name) {
				if (aCheck()) // this will always return false
					return null // so this won't be executed
				if (aCheck2()) // this will always return false
					return null // so this won't be executed
				ecoreref(foo).addNewEClass(name)
			}

			def aCheck() {
				return false
			}

			def aCheck2() : boolean {
				false
			}

			modifyEcore SomeChanges epackage foo {
				// the ANewClass is not actually created
				// not shown in the outline
				"ANewClass".createANewClassInMyEcore() => [
					abstract = true
				]
			}
			""",
			derivedEPackage -> {
				var lastEClass = getLastEClass(derivedEPackage);
				assertEquals("ANewClass", lastEClass.getName());
				assertTrue(lastEClass.isAbstract());
			});
	}

	@Test
	public void testCallOperationThatCallsAnotherNonVoidOperationInAnotherFile() throws Exception {
		// see https://github.com/LorenzoBettini/edelta/issues/268
		var it = parseSeveralWithTestEcore(
			List.of(
			"""
			import org.eclipse.emf.ecore.EPackage

			def create(EPackage it) {
				if (aCheck()) // this will always return false
					return null // so this won't be executed
				if (aCheck2()) // this will always return false
					return null // so this won't be executed
				addNewEClass("NewClass")
			}

			def aCheck() {
				return false
			}

			def aCheck2() : boolean {
				false
			}
			""", """
			metamodel "foo"

			use edelta.__synthetic0 as extension my

			modifyEcore anotherTest epackage foo {
				create(it)
				ecoreref(NewClass)
			}
			"""));
		var created =
			getEClassByName(interpretProgram(it).get("foo"), "NewClass");
		assertNotNull(created);
	}

	@Test
	public void testAccessToResourceSet() throws Exception {
		assertAfterInterpretationOfEdeltaModifyEcoreOperation("""
		import org.eclipse.emf.ecore.EPackage

		metamodel "foo"

		modifyEcore aTest epackage foo {
			// access the ResourceSet
			// but during the interpration we use a sandbox ResourceSet
			// so that the interpreted code can access only copied EPackages
			val rs = eResource.getResourceSet
			// remove all EClass in any EPackage with name "FooClass"
			// this must not touch the original EPackages
			rs.resources
				.map[contents]
				.flatten
				.filter(EPackage)
				.forEach[EClassifiers.removeIf[name == "FooClass"]]
		}
		""",
		true,
		derivedEPackage -> {
			var fooClass = derivedEPackage.getEClassifier("FooClass");
			assertNull(fooClass);
		});
	}

	private void assertAfterInterpretationOfEdeltaModifyEcoreOperation(CharSequence input,
			Procedure1<? super EPackage> testExecutor) throws Exception {
		assertAfterInterpretationOfEdeltaModifyEcoreOperation(input, true, testExecutor);
	}

	private void assertAfterInterpretationOfEdeltaModifyEcoreOperation(CharSequence input,
			boolean doValidate, Procedure1<? super EPackage> testExecutor) throws Exception {
		var program = parseWithTestEcore(input);
		assertAfterInterpretationOfEdeltaModifyEcoreOperation(program, doValidate, testExecutor);
	}

	private void assertAfterInterpretationOfEdeltaModifyEcoreOperation(List<CharSequence> inputs,
			boolean doValidate, Procedure1<? super EPackage> testExecutor) throws Exception {
		var program = parseSeveralWithTestEcore(inputs);
		assertAfterInterpretationOfEdeltaModifyEcoreOperation(program, doValidate, testExecutor);
	}

	private void assertAfterInterpretationOfEdeltaModifyEcoreOperation(EdeltaProgram program,
			boolean doValidate, Procedure1<? super EPackage> testExecutor) {
		assertAfterInterpretationOfEdeltaModifyEcoreOperation(program, it -> {
			if (doValidate) {
				validationTestHelper.assertNoErrors(program);
			}
			testExecutor.apply(it);
		});
	}

	private void assertAfterInterpretationOfEdeltaModifyEcoreOperation(EdeltaProgram program,
			Procedure1<? super EPackage> testExecutor) {
		currentProgram = program;
		var it = lastModifyEcoreOperation(program);
		interpreter.evaluateModifyEcoreOperations(program);
		var packageName = it.getEpackage().getName();
		var epackage = derivedStateHelper.getCopiedEPackagesMap(program.eResource()).get(packageName);
		testExecutor.apply(epackage);
	}

	private EdeltaCopiedEPackagesMap interpretProgram(EdeltaProgram program) {
		interpreter.evaluateModifyEcoreOperations(program);
		return derivedStateHelper.getCopiedEPackagesMap(program.eResource());
	}

	private void assertEcoreRefExpElementMapsToXExpression(EdeltaEcoreArgument reference,
			String expectedFeatureCallSimpleName) {
		var exp = derivedStateHelper.getResponsibleExpression(reference);
		assertNotNull(exp);
		assertEquals(expectedFeatureCallSimpleName, getFeatureCall(exp).getFeature().getSimpleName());
	}
}
