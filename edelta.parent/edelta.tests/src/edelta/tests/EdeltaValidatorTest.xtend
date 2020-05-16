
package edelta.tests

import edelta.edelta.EdeltaPackage
import edelta.lib.AbstractEdelta
import edelta.validation.EdeltaValidator
import org.eclipse.xtext.diagnostics.Diagnostic
import org.eclipse.xtext.testing.InjectWith
import org.eclipse.xtext.testing.XtextRunner
import org.junit.Test
import org.junit.runner.RunWith

import static edelta.edelta.EdeltaPackage.Literals.*
import org.eclipse.xtext.xbase.validation.IssueCodes

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
	def void testTimeoutInCancelIndicator() {
		val input = '''
			import org.eclipse.emf.ecore.EPackage

			metamodel "foo"
			
			def op(EPackage c) : void {
				var i = 10;
				while (i >= 0) {
					Thread.sleep(1000);
					i++
				}
				// this will never be executed
				c.abstract = true
			}
			
			modifyEcore aTest epackage foo {
				op(it)
			}
		'''
		val lastOpenCurlyBracket = input.lastIndexOf("{")
		val lastClosedCurlyBracket = input.lastIndexOf("}")
		input.parseWithTestEcore.assertWarning(
			EdeltaPackage.eINSTANCE.edeltaModifyEcoreOperation,
			EdeltaValidator.INTERPRETER_TIMEOUT,
			lastOpenCurlyBracket, lastClosedCurlyBracket - lastOpenCurlyBracket + 1,
			"Timeout interpreting initialization block"
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
	def void testReferenceToEClassRemoved() {
		val input = referenceToEClassRemoved.toString
		input.parseWithTestEcore =>[
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
	def void testInvalidAmbiguousEcoreref() {
		val input =
		'''
		metamodel "mainpackage"
		
		modifyEcore aTest epackage mainpackage {
			ecoreref(MyClass)
		}
		'''
		input
		.parseWithTestEcoreWithSubPackage
		.assertErrorsAsStrings(
			'''
			Ambiguous reference 'MyClass':
			  mainpackage.MyClass
			  mainpackage.mainsubpackage.MyClass
			  mainpackage.mainsubpackage.subsubpackage.MyClass
			'''
		)
	}

	@Test
	def void testInvalidAmbiguousEcorerefWithCreatedElements() {
		val input =
		'''
		metamodel "mainpackage"
		
		modifyEcore aTest epackage mainpackage {
			addNewEClass("created") [
				addNewEAttribute("created", null)
			]
			ecoreref(created)
		}
		'''
		input
		.parseWithTestEcoreWithSubPackage
		.assertErrorsAsStrings(
			'''
			Ambiguous reference 'created':
			  mainpackage.created
			  mainpackage.created.created
			'''
		)
	}

	@Test
	def void testNonAmbiguousEcoreref() {
		val input =
		'''
		metamodel "mainpackage"
		
		modifyEcore aTest epackage mainpackage {
			addNewEClass("WorkPlace")
			addNewEClass("LivingPlace")
			addNewEClass("Place")
			ecoreref(Place) // NON ambiguous
		}
		'''
		input
		.parseWithTestEcoreWithSubPackage
		.assertNoErrors
	}

}
