
package edelta.tests

import edelta.edelta.EdeltaPackage
import edelta.lib.AbstractEdelta
import edelta.validation.EdeltaValidator
import org.eclipse.xtext.diagnostics.Diagnostic
import org.eclipse.xtext.testing.InjectWith
import org.eclipse.xtext.testing.XtextRunner
import org.eclipse.xtext.xbase.validation.IssueCodes
import org.junit.Test
import org.junit.runner.RunWith

import static edelta.edelta.EdeltaPackage.Literals.*

@RunWith(XtextRunner)
@InjectWith(EdeltaInjectorProviderCustom)
class EdeltaValidatorTest extends EdeltaAbstractTest {

	@Test
	def void testEmptyProgram() {
		''''''.parse.assertNoErrors
	}

	@Test
	def void testCanReferToMetamodel() {
		referenceToMetamodel.parseWithTestEcore.assertNoErrors
	}

	@Test
	def void testUseImportedJavaTypes() {
		useImportedJavaTypes.parse.assertNoErrors
	}

	@Test
	def void testReferenceToCreatedEClass() {
		referenceToCreatedEClass.parseWithTestEcore.assertNoErrors
	}

	@Test
	def void testReferenceToCreatedEAttribute() {
		referenceToCreatedEAttributeRenamed.parseWithTestEcore.assertNoErrors
	}

	@Test
	def void testValidUseAs() {
		'''
		import edelta.tests.additional.MyCustomEdelta;
		use MyCustomEdelta as foo
		'''.parse.assertNoIssues
	}

	@Test
	def void testInvalidUseAsNotAnEdelta() {
		val input = '''
		import java.util.List;
		use List as foo
		'''
		input.parse.assertError(
			EDELTA_USE_AS,
			EdeltaValidator.TYPE_MISMATCH,
			input.lastIndexOf("List"), 4,
			"Not a valid type: must be an " + AbstractEdelta.name
		)
	}

	@Test
	def void testInvalidUseAsAbstractEdelta() {
		val input = '''
		import edelta.tests.additional.MyCustomAbstractEdelta;
		use MyCustomAbstractEdelta as foo
		'''
		input.parse.assertError(
			EDELTA_USE_AS,
			EdeltaValidator.TYPE_MISMATCH,
			input.lastIndexOf("MyCustomAbstractEdelta"), "MyCustomAbstractEdelta".length,
			"Cannot be an abstract type"
		)
	}

	@Test
	def void testInvalidUseAsUnresolvedProxy() {
		val input = '''
		use Unknown as foo
		'''
		input.parse.assertErrorsAsStrings(
			"Unknown cannot be resolved to a type."
		)
	}

	@Test
	def void testUnresolvedEcoreReference() {
		'''
		metamodel "foo"
		
		modifyEcore aTest epackage foo {
			ecoreref(NonExistant)
			ecoreref(FooClass)
		}
		'''.parseWithTestEcore.assertErrorsAsStrings("NonExistant cannot be resolved.")
	}

	@Test
	def void testNoDanglingReferencesAfterInterpretation() {
		'''
		metamodel "foo"
		
		modifyEcore aTest epackage foo {
			ecoreref(foo.FooClass).EPackage.EClassifiers.remove(ecoreref(foo.FooClass))
		}
		'''.parseWithTestEcore.assertNoErrors
	}

	@Test
	def void testCallMethodOnRenanedEClassInModifyEcore() {
		val prog =
		'''
		metamodel "foo"

		modifyEcore aTest epackage foo {
			ecoreref(foo.FooClass).name = "RenamedClass"
			ecoreref(RenamedClass).getEAllStructuralFeatures
		}
		'''.parseWithTestEcore
		prog.assertNoErrors
	}

	@Test
	def void testCallMethodOnQualifiedRenanedEClassInModifyEcore() {
		val prog =
		'''
		metamodel "foo"

		modifyEcore aTest epackage foo {
			ecoreref(foo.FooClass).name = "RenamedClass"
			ecoreref(foo.RenamedClass).getEAllStructuralFeatures
		}
		'''.parseWithTestEcore
		prog.assertNoErrors
	}

	@Test
	def void testCallNonExistingMethodOnRenanedEClassInModifyEcore() {
		val prog =
		'''
		metamodel "foo"

		modifyEcore aTest epackage foo {
			ecoreref(foo.FooClass).name = "RenamedClass"
			ecoreref(RenamedClass).nonExistant("an arg")
			ecoreref(RenamedClass).sugarSet = "an arg"
			"a string".sugarSet = "an arg"
		}
		'''.parseWithTestEcore
		prog.assertErrorsAsStrings
		('''
		The method nonExistant(String) is undefined for the type EClass
		The method sugarSet(String) is undefined for the type EClass
		The method sugarSet(String) is undefined for the type String
		''')
	}

	@Test
	def void testReferenceToAddedAttributeofRenamedClassInModifyEcore() {
		'''
		metamodel "foo"

		modifyEcore aTest epackage foo {
			ecoreref(foo.FooClass).name = "RenamedClass"
			ecoreref(RenamedClass).EStructuralFeatures.add(
				newEAttribute("addedAttribute", ecoreref(FooDataType)))
			ecoreref(RenamedClass.addedAttribute)
		}
		'''.parseWithTestEcore.assertNoErrors
	}

	@Test
	def void testReferenceToAddedAttributeofRenamedClassInModifyEcore2() {
		'''
		import org.eclipse.emf.ecore.EClass

		metamodel "foo"

		modifyEcore aTest epackage foo {
			ecoreref(foo.FooClass).name = "RenamedClass"
			ecoreref(RenamedClass).EStructuralFeatures += newEAttribute("added", ecoreref(FooDataType))
			ecoreref(RenamedClass.added)
		}
		'''.parseWithTestEcore.assertNoErrors
	}

	@Test
	def void testReferenceToRenamedClassInModifyEcore() {
		'''
		import org.eclipse.emf.ecore.EClass

		metamodel "foo"

		modifyEcore aTest epackage foo {
			ecoreref(foo.FooClass).name = "RenamedClass"
			ecoreref(foo.RenamedClass) => [abstract = true]
			ecoreref(foo.RenamedClass).setAbstract(true)
			ecoreref(foo.RenamedClass).abstract = true
		}
		'''.parseWithTestEcore.assertNoErrors
	}

	@Test
	def void testReferenceToUnknownEPackageInModifyEcore() {
		'''
		import org.eclipse.emf.ecore.EClass

		modifyEcore aTest epackage foo {
			
		}
		'''.parseWithTestEcore.assertError(
			EdeltaPackage.eINSTANCE.edeltaModifyEcoreOperation,
			Diagnostic.LINKING_DIAGNOSTIC,
			"foo cannot be resolved."
		)
	}

	@Test
	def void testValidLibMethodsInModifyEcore() {
		modifyEcoreUsingLibMethods.parseWithTestEcore.assertNoErrors
	}

	@Test
	def void testDuplicateDeclarations() {
		val input = '''
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
		'''
		input.parseWithTestEcore => [
			assertError(
				EdeltaPackage.eINSTANCE.edeltaOperation,
				EdeltaValidator.DUPLICATE_DECLARATION,
				input.indexOf("anotherDuplicate"), "anotherDuplicate".length,
				"Duplicate definition 'anotherDuplicate'"
			)
			assertError(
				EdeltaPackage.eINSTANCE.edeltaModifyEcoreOperation,
				EdeltaValidator.DUPLICATE_DECLARATION,
				input.lastIndexOf("anotherDuplicate"), "anotherDuplicate".length,
				"Duplicate definition 'anotherDuplicate'"
			)
			assertErrorsAsStrings(
				'''
				Duplicate definition 'aTest'
				Duplicate definition 'aTest'
				Duplicate definition 'anotherDuplicate'
				Duplicate definition 'anotherDuplicate'
				Duplicate definition 'myFun'
				Duplicate definition 'myFun'
				'''
			)
		]
	}

	@Test
	def void testDuplicateMetamodelImport() {
		val input = '''
		metamodel "foo"
		metamodel "bar"
		metamodel "nonexistent"
		metamodel "nonexistent" // also check unresolved imports
		metamodel "foo"
		'''
		input.parseWithTestEcores => [
			assertError(
				EdeltaPackage.eINSTANCE.edeltaProgram,
				EdeltaValidator.DUPLICATE_METAMODEL_IMPORT,
				input.lastIndexOf('"nonexistent"'), '"nonexistent"'.length,
				'Duplicate metamodel import "nonexistent"'
			)
			assertError(
				EdeltaPackage.eINSTANCE.edeltaProgram,
				EdeltaValidator.DUPLICATE_METAMODEL_IMPORT,
				input.lastIndexOf('"foo"'), '"foo"'.length,
				'Duplicate metamodel import "foo"'
			)
		]
	}

	@Test
	def void testInvalidSubPackageImportedMetamodel() {
		val input =
		'''
		metamodel "mainpackage.mainsubpackage"
		'''
		val start = input.indexOf('"')
		input
		.parseWithTestEcoreWithSubPackage
		.assertError(
			EdeltaPackage.eINSTANCE.edeltaProgram,
			EdeltaValidator.INVALID_SUBPACKAGE_IMPORT,
			start, input.lastIndexOf('"') - start + 1,
			"Invalid subpackage import 'mainsubpackage'"
		)
	}

	@Test
	def void testInvalidModifyEcoreOfSubPackage() {
		val input =
		'''
		metamodel "mainpackage.mainsubpackage"
		
		modifyEcore aTest epackage mainsubpackage {
			
		}
		'''
		val start = input.lastIndexOf('mainsubpackage')
		input
		.parseWithTestEcoreWithSubPackage
		.assertError(
			EdeltaPackage.eINSTANCE.edeltaModifyEcoreOperation,
			EdeltaValidator.INVALID_SUBPACKAGE_MODIFICATION,
			start, input.indexOf(' {') - start,
			"Invalid direct subpackage modification 'mainsubpackage'"
		)
	}

	@Test
	def void testTypeMismatchOfEcoreRefExp() {
		val input = '''
		import org.eclipse.emf.ecore.EClass
		import org.eclipse.emf.ecore.EPackage
		
		metamodel "foo"

		modifyEcore aTest epackage foo {
			ecoreref(foo.FooClass).name = "RenamedClass"
			val EClass c = ecoreref(RenamedClass) // OK after interpretation
			val EPackage p = ecoreref(RenamedClass) // ERROR also after interpretation
		}
		'''
		input.parseWithTestEcore => [
			assertError(
				EDELTA_ECORE_REFERENCE_EXPRESSION,
				IssueCodes.INCOMPATIBLE_TYPES,
				input.lastIndexOf("ecoreref(RenamedClass)"), "ecoreref(RenamedClass)".length,
				"Type mismatch: cannot convert from EClass to EPackage"
			)
			assertErrorsAsStrings("Type mismatch: cannot convert from EClass to EPackage")
		]
	}

	@Test
	def void testAccessToNotYetExistingElement() {
		val input =
		'''
		metamodel "foo"
		
		modifyEcore aTest epackage foo {
			ecoreref(ANewClass) // doesn't exist yet
			ecoreref(NonExisting) // doesn't exist at all
			addNewEClass("ANewClass")
			ecoreref(ANewClass) // this is OK
		}
		'''
		input
		.parseWithTestEcore => [
			assertError(
				EDELTA_ECORE_DIRECT_REFERENCE,
				EdeltaValidator.INTERPRETER_ACCESS_NOT_YET_EXISTING_ELEMENT,
				input.indexOf("ANewClass"),
				"ANewClass".length,
				"Element not yet available in this context: foo.ANewClass"
			)
			assertErrorsAsStrings(
				'''
				Element not yet available in this context: foo.ANewClass
				NonExisting cannot be resolved.
				'''
			)
		]
	}

	@Test
	def void testAccessToNotYetExistingElementInComplexExpression() {
		val input =
		'''
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
		'''
		input
		.parseWithTestEcore => [
			assertErrorsAsStrings(
				'''
				Element not yet available in this context: foo.ANewClass
				Element not yet available in this context: foo.ANewSuperClass
				The method ESuperTypes(EClass) is undefined for the type EClass
				'''
			)
			assertError(
				EDELTA_ECORE_DIRECT_REFERENCE,
				EdeltaValidator.INTERPRETER_ACCESS_NOT_YET_EXISTING_ELEMENT,
				input.indexOf("ANewClass"),
				"ANewClass".length,
				"Element not yet available in this context: foo.ANewClass"
			)
			assertError(
				EDELTA_ECORE_DIRECT_REFERENCE,
				EdeltaValidator.INTERPRETER_ACCESS_NOT_YET_EXISTING_ELEMENT,
				input.indexOf("ANewSuperClass"),
				"ANewSuperClass".length,
				"Element not yet available in this context: foo.ANewSuperClass"
			)
		]
	}

	@Test
	def void testAccessToNotYetExistingElementInComplexExpression2() {
		val input =
		'''
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
		'''
		input
		.parseWithTestEcore => [
			assertErrorsAsStrings(
				'''
				Element not yet available in this context: foo.ANewClass
				Element not yet available in this context: foo.ANewSuperClass
				'''
			)
			assertError(
				EDELTA_ECORE_DIRECT_REFERENCE,
				EdeltaValidator.INTERPRETER_ACCESS_NOT_YET_EXISTING_ELEMENT,
				input.indexOf("ANewClass"),
				"ANewClass".length,
				"Element not yet available in this context: foo.ANewClass"
			)
			assertError(
				EDELTA_ECORE_DIRECT_REFERENCE,
				EdeltaValidator.INTERPRETER_ACCESS_NOT_YET_EXISTING_ELEMENT,
				input.indexOf("ANewSuperClass"),
				"ANewSuperClass".length,
				"Element not yet available in this context: foo.ANewSuperClass"
			)
		]
	}
}
