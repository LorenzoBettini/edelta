package edelta.tests;

import org.eclipse.xtext.diagnostics.Diagnostic;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.XtextRunner;
import org.eclipse.xtext.xbase.validation.IssueCodes;
import org.junit.Test;
import org.junit.runner.RunWith;

import edelta.edelta.EdeltaPackage;
import edelta.lib.EdeltaRuntime;
import edelta.tests.injectors.EdeltaInjectorProviderCustom;
import edelta.validation.EdeltaValidator;

@RunWith(XtextRunner.class)
@InjectWith(EdeltaInjectorProviderCustom.class)
public class EdeltaValidatorTest extends EdeltaAbstractTest {
	@Test
	public void testEmptyProgram() throws Exception {
		validationTestHelper.assertNoErrors(parseHelper.parse(""));
	}

	@Test
	public void testCanReferToMetamodel() throws Exception {
		validationTestHelper.assertNoErrors(
			parseWithTestEcore(inputs.referenceToMetamodel()));
	}

	@Test
	public void testUseImportedJavaTypes() throws Exception {
		validationTestHelper.assertNoErrors(
			parseHelper.parse(inputs.useImportedJavaTypes()));
	}

	@Test
	public void testReferenceToCreatedEClass() throws Exception {
		validationTestHelper.assertNoErrors(
			parseWithTestEcore(inputs.referenceToCreatedEClass()));
	}

	@Test
	public void testReferenceToCreatedEAttribute() throws Exception {
		validationTestHelper.assertNoErrors(
			parseWithTestEcore(inputs.referenceToCreatedEAttributeRenamed()));
	}

	@Test
	public void testValidUseAs() throws Exception {
		validationTestHelper.assertNoIssues(
			parseHelper.parse("""
				import edelta.tests.additional.MyCustomEdelta;
				use MyCustomEdelta as foo
				"""));
	}

	@Test
	public void testInvalidUseAsNotAnEdelta() throws Exception {
		var input = """
			import java.util.List;
			use List as foo
			""";
		validationTestHelper.assertError(
			parseHelper.parse(input),
			EdeltaPackage.Literals.EDELTA_USE_AS,
			EdeltaValidator.TYPE_MISMATCH,
			input.lastIndexOf("List"),
			4,
			"Not a valid type: must be an " + EdeltaRuntime.class.getName());
	}

	@Test
	public void testInvalidUseAsEdeltaRuntime() throws Exception {
		var input = """
			import edelta.tests.additional.MyCustomAbstractEdelta;
			use MyCustomAbstractEdelta as foo
			""";
		validationTestHelper.assertError(
			parseHelper.parse(input),
			EdeltaPackage.Literals.EDELTA_USE_AS,
			EdeltaValidator.TYPE_MISMATCH,
			input.lastIndexOf("MyCustomAbstractEdelta"),
			"MyCustomAbstractEdelta".length(),
			"Cannot be an abstract type");
	}

	@Test
	public void testInvalidUseAsUnresolvedProxy() throws Exception {
		var input = "use Unknown as foo";
		assertErrorsAsStrings(parseHelper.parse(input),
			"Unknown cannot be resolved to a type.");
	}

	@Test
	public void testUnresolvedEcoreReference() throws Exception {
		assertErrorsAsStrings(
			parseWithTestEcore("""
				metamodel "foo"

				modifyEcore aTest epackage foo {
					ecoreref(NonExistant)
					ecoreref(FooClass)
				}
				"""),
			"NonExistant cannot be resolved.");
	}

	@Test
	public void testNoDanglingReferencesAfterInterpretation() throws Exception {
		validationTestHelper.assertNoErrors(
			parseWithTestEcore("""
			metamodel "foo"

			modifyEcore aTest epackage foo {
				ecoreref(foo.FooClass).EPackage.EClassifiers.remove(ecoreref(foo.FooClass))
			}
			"""));
	}

	@Test
	public void testCallMethodOnRenanedEClassInModifyEcore() throws Exception {
		validationTestHelper.assertNoErrors(parseWithTestEcore("""
			metamodel "foo"

			modifyEcore aTest epackage foo {
				ecoreref(foo.FooClass).name = "RenamedClass"
				ecoreref(RenamedClass).getEAllStructuralFeatures
			}
			"""));
	}

	@Test
	public void testCallMethodOnQualifiedRenanedEClassInModifyEcore() throws Exception {
		validationTestHelper.assertNoErrors(parseWithTestEcore("""
		metamodel "foo"

		modifyEcore aTest epackage foo {
			ecoreref(foo.FooClass).name = "RenamedClass"
			ecoreref(foo.RenamedClass).getEAllStructuralFeatures
		}
		"""));
	}

	@Test
	public void testCallNonExistingMethodOnRenanedEClassInModifyEcore() throws Exception {
		final var prog = parseWithTestEcore("""
			metamodel "foo"

			modifyEcore aTest epackage foo {
				ecoreref(foo.FooClass).name = "RenamedClass"
				ecoreref(RenamedClass).nonExistant("an arg")
				ecoreref(RenamedClass).sugarSet = "an arg"
				"a string".sugarSet = "an arg"
			}
			""");
		assertErrorsAsStrings(prog, """
			The method nonExistant(String) is undefined for the type EClass
			The method sugarSet(String) is undefined for the type EClass
			The method sugarSet(String) is undefined for the type String
			""");
	}

	@Test
	public void testReferenceToAddedAttributeofRenamedClassInModifyEcore() throws Exception {
		validationTestHelper.assertNoErrors(
			parseWithTestEcore("""
				metamodel "foo"

				modifyEcore aTest epackage foo {
					ecoreref(foo.FooClass).name = "RenamedClass"
					ecoreref(RenamedClass).EStructuralFeatures.add(
						newEAttribute("addedAttribute", ecoreref(FooDataType)))
					ecoreref(RenamedClass.addedAttribute)
				}
				"""));
	}

	@Test
	public void testReferenceToAddedAttributeofRenamedClassInModifyEcore2() throws Exception {
		validationTestHelper.assertNoErrors(
			parseWithTestEcore("""
			import org.eclipse.emf.ecore.EClass

			metamodel "foo"

			modifyEcore aTest epackage foo {
				ecoreref(foo.FooClass).name = "RenamedClass"
				ecoreref(RenamedClass).EStructuralFeatures += newEAttribute("added", ecoreref(FooDataType))
				ecoreref(RenamedClass.added)
			}
			"""));
	}

	@Test
	public void testReferenceToRenamedClassInModifyEcore() throws Exception {
		validationTestHelper.assertNoErrors(
			parseWithTestEcore("""
			import org.eclipse.emf.ecore.EClass

			metamodel "foo"

			modifyEcore aTest epackage foo {
				ecoreref(foo.FooClass).name = "RenamedClass"
				ecoreref(foo.RenamedClass) => [abstract = true]
				ecoreref(foo.RenamedClass).setAbstract(true)
				ecoreref(foo.RenamedClass).abstract = true
			}
			"""));
	}

	@Test
	public void testReferenceToUnknownEPackageInModifyEcore() throws Exception {
		validationTestHelper.assertError(
			parseWithTestEcore("""
			import org.eclipse.emf.ecore.EClass

			modifyEcore aTest epackage foo {	
			}
			"""),
			EdeltaPackage.eINSTANCE.getEdeltaModifyEcoreOperation(),
			Diagnostic.LINKING_DIAGNOSTIC,
			"foo cannot be resolved.");
	}

	@Test
	public void testValidLibMethodsInModifyEcore() throws Exception {
		validationTestHelper.assertNoErrors(
			parseWithTestEcore(inputs.modifyEcoreUsingLibMethods()));
	}

	@Test
	public void testDuplicateDeclarations() throws Exception {
		var input = """
		import java.util.List
		import org.eclipse.emf.ecore.EPackage

		metamodel "foo"

		def myFun(List<Integer> l) {}
		def myFun(List<String> l) {}
		def anotherFun(List<String> l) {} // OK, different params
		def anotherDuplicate(EPackage p) {} // conflicts with modifyEcore

		modifyEcore aTest epackage foo {}
		modifyEcore aTest epackage foo {}
		modifyEcore anotherDuplicate epackage foo {} // implicit Java method param: EPackage
		modifyEcore anotherFun epackage foo {} // OK, different params
		""";
		var prog = parseWithTestEcore(input);
		validationTestHelper.assertError(
				prog, EdeltaPackage.eINSTANCE.getEdeltaOperation(),
				EdeltaValidator.DUPLICATE_DECLARATION,
				input.indexOf("anotherDuplicate"),
				"anotherDuplicate".length(),
				"Duplicate definition \'anotherDuplicate\'");
		validationTestHelper.assertError(
				prog, EdeltaPackage.eINSTANCE.getEdeltaModifyEcoreOperation(),
				EdeltaValidator.DUPLICATE_DECLARATION,
				input.lastIndexOf("anotherDuplicate"),
				"anotherDuplicate".length(),
				"Duplicate definition \'anotherDuplicate\'");
		assertErrorsAsStrings(prog, """
		Duplicate definition 'aTest'
		Duplicate definition 'aTest'
		Duplicate definition 'anotherDuplicate'
		Duplicate definition 'anotherDuplicate'
		Duplicate definition 'myFun'
		Duplicate definition 'myFun'
		""");
	}

	@Test
	public void testDuplicateMetamodelImport() throws Exception {
		var input = """
		metamodel "foo"
		metamodel "bar"
		metamodel "nonexistent"
		metamodel "nonexistent" // also check unresolved imports
		metamodel "foo"
		""";
		var prog = parseWithTestEcores(input);
		validationTestHelper.assertError(prog,
				EdeltaPackage.eINSTANCE.getEdeltaProgram(),
				EdeltaValidator.DUPLICATE_METAMODEL_IMPORT,
				input.lastIndexOf("\"nonexistent\""),
				"\"nonexistent\"".length(),
				"Duplicate metamodel import \"nonexistent\"");
		validationTestHelper.assertError(prog,
				EdeltaPackage.eINSTANCE.getEdeltaProgram(),
				EdeltaValidator.DUPLICATE_METAMODEL_IMPORT,
				input.lastIndexOf("\"foo\""), "\"foo\"".length(),
				"Duplicate metamodel import \"foo\"");
	}

	@Test
	public void testInvalidSubPackageImportedMetamodel() throws Exception {
		var input = "metamodel \"mainpackage.mainsubpackage\"";
		final int start = input.indexOf("\"");
		validationTestHelper.assertError(
			parseWithTestEcoreWithSubPackage(input),
			EdeltaPackage.eINSTANCE.getEdeltaProgram(),
			EdeltaValidator.INVALID_SUBPACKAGE_IMPORT,
			start,
			input.lastIndexOf("\"") - start + 1,
			"Invalid subpackage import \'mainsubpackage\'");
	}

	@Test
	public void testInvalidModifyEcoreOfSubPackage() throws Exception {
		var input = """
		metamodel "mainpackage.mainsubpackage"

		modifyEcore aTest epackage mainsubpackage {

		}
		""";
		final int start = input.lastIndexOf("mainsubpackage");
		validationTestHelper.assertError(
			parseWithTestEcoreWithSubPackage(input),
			EdeltaPackage.eINSTANCE.getEdeltaModifyEcoreOperation(),
			EdeltaValidator.INVALID_SUBPACKAGE_MODIFICATION,
			start,
			input.indexOf(" {") - start,
			"Invalid direct subpackage modification \'mainsubpackage\'");
	}

	@Test
	public void testTypeMismatchOfEcoreRefExp() throws Exception {
		var input = """
		import org.eclipse.emf.ecore.EClass
		import org.eclipse.emf.ecore.EPackage

		metamodel "foo"

		modifyEcore aTest epackage foo {
			ecoreref(foo.FooClass).name = "RenamedClass"
			val EClass c = ecoreref(RenamedClass) // OK after interpretation
			val EPackage p = ecoreref(RenamedClass) // ERROR also after interpretation
		}
		""";
		var prog = parseWithTestEcore(input);
		validationTestHelper.assertError(prog,
				EdeltaPackage.Literals.EDELTA_ECORE_REFERENCE_EXPRESSION,
				IssueCodes.INCOMPATIBLE_TYPES,
				input.lastIndexOf("ecoreref(RenamedClass)"),
				"ecoreref(RenamedClass)".length(),
				"Type mismatch: cannot convert from EClass to EPackage");
		assertErrorsAsStrings(prog,
				"Type mismatch: cannot convert from EClass to EPackage");
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
		var prog = parseWithTestEcore(input);
		validationTestHelper.assertError(prog,
				EdeltaPackage.Literals.EDELTA_ECORE_DIRECT_REFERENCE,
				EdeltaValidator.INTERPRETER_ACCESS_NOT_YET_EXISTING_ELEMENT,
				input.indexOf("ANewClass"),
				"ANewClass".length(),
				"Element not yet available in this context: foo.ANewClass");
		assertErrorsAsStrings(prog, """
			Element not yet available in this context: foo.ANewClass
			NonExisting cannot be resolved.
			""");
	}

	@Test
	public void testAccessToNotYetExistingElementInComplexExpression() throws Exception {
		var input = """
		metamodel "foo"

		modifyEcore aTest epackage foo {
			// doesn't exist yet
			ecoreref(ANewClass).ESuperTypes =
				ecoreref(ANewSuperClass) // doesn't exist yet
			addNewEClass("ANewClass")
			addNewEClass("ANewSuperClass")
			ecoreref(ANewClass) // this is OK
			ecoreref(ANewSuperClass) // this is OK
		}
		""";
		var prog = parseWithTestEcore(input);
		assertErrorsAsStrings(prog, """
			Element not yet available in this context: foo.ANewClass
			Element not yet available in this context: foo.ANewSuperClass
			The method ESuperTypes(EClass) is undefined for the type EClass
			""");
		validationTestHelper.assertError(prog,
				EdeltaPackage.Literals.EDELTA_ECORE_DIRECT_REFERENCE,
				EdeltaValidator.INTERPRETER_ACCESS_NOT_YET_EXISTING_ELEMENT,
				input.indexOf("ANewClass"),
				"ANewClass".length(),
				"Element not yet available in this context: foo.ANewClass");
		validationTestHelper.assertError(prog,
				EdeltaPackage.Literals.EDELTA_ECORE_DIRECT_REFERENCE,
				EdeltaValidator.INTERPRETER_ACCESS_NOT_YET_EXISTING_ELEMENT,
				input.indexOf("ANewSuperClass"),
				"ANewSuperClass".length(),
				"Element not yet available in this context: foo.ANewSuperClass");
	}

	@Test
	public void testAccessToNotYetExistingElementInComplexExpression2() throws Exception {
		var input = """
		metamodel "foo"

		modifyEcore aTest epackage foo {
			// doesn't exist yet
			ecoreref(ANewClass).ESuperTypes +=
				ecoreref(ANewSuperClass) // doesn't exist yet
			addNewEClass("ANewClass")
			addNewEClass("ANewSuperClass")
			ecoreref(ANewClass) // this is OK
			ecoreref(ANewSuperClass) // this is OK
		}
		""";
		var prog = parseWithTestEcore(input);
		assertErrorsAsStrings(prog, """
			Element not yet available in this context: foo.ANewClass
			Element not yet available in this context: foo.ANewSuperClass
			""");
		validationTestHelper.assertError(prog,
				EdeltaPackage.Literals.EDELTA_ECORE_DIRECT_REFERENCE,
				EdeltaValidator.INTERPRETER_ACCESS_NOT_YET_EXISTING_ELEMENT,
				input.indexOf("ANewClass"),
				"ANewClass".length(),
				"Element not yet available in this context: foo.ANewClass");
		validationTestHelper.assertError(prog,
				EdeltaPackage.Literals.EDELTA_ECORE_DIRECT_REFERENCE,
				EdeltaValidator.INTERPRETER_ACCESS_NOT_YET_EXISTING_ELEMENT,
				input.indexOf("ANewSuperClass"),
				"ANewSuperClass".length(),
				"Element not yet available in this context: foo.ANewSuperClass");
	}

	@Test
	public void testInvalidUseOfEcorerefInModelMigration() throws Exception {
		var input = """
		metamodel "foo"

		modifyEcore aTest epackage foo {
			ecoreref(FooClass) // OK
			modelMigration[
				createInstanceRule(
					isRelatedTo(ecoreref(FooClass)), // INVALID
					[ o |
						return edelta.lib.EdeltaEcoreUtil.createInstance
							(ecoreref(FooClass)) []  // INVALID
					]
				)
			]
		}
		""";
		var prog = parseWithTestEcore(input);
		// TODO this should fail
		validationTestHelper.assertNoErrors(prog);
	}
}
