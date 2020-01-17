
package edelta.tests

import edelta.edelta.EdeltaPackage
import edelta.lib.AbstractEdelta
import edelta.validation.EdeltaValidator
import org.eclipse.xtext.testing.InjectWith
import org.eclipse.xtext.testing.XtextRunner
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
	def void testCanReferToEClass() {
		referenceToEClass.parseWithTestEcore.assertNoErrors
	}

	@Test
	def void testUseImportedJavaTypes() {
		useImportedJavaTypes.parse.assertNoErrors
	}

	@Test
	def void testCreateEClass() {
		createEClass.parseWithTestEcore.assertNoErrors
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
	def void testCreateEClassWithSuperTypesOk() {
		createEClassWithSuperTypes.parseWithTestEcore.assertNoErrors
	}

	@Test
	def void testCreateEClassWithSuperTypes2Ok() {
		createEClassWithSuperTypes2.parseWithTestEcore.assertNoErrors
	}

	@Test
	def void testCreateEClassWithSuperTypesNotEClass() {
		'''
			metamodel "foo"
			
			createEClass MyNewClass in foo
				extends FooDataType, FooClass, FooEnum {}
		'''.parseWithTestEcore.assertErrorsAsStrings(
			'''
			Type mismatch: cannot convert from EDataType to EClass
			Type mismatch: cannot convert from EDataType to EClass
			Type mismatch: cannot convert from EEnum to EClass
			Type mismatch: cannot convert from EEnum to EClass
			'''
		)
		// mismatch errors are duplicate due to the interpreter
	}

	@Test
	def void testCreateEClassWithSuperTypesNotEClassWithNullRef() {
		'''
			metamodel "foo"
			
			createEClass MyNewClass in foo
				extends FooDataType, , FooEnum {}
		'''.parseWithTestEcore.assertErrorsAsStrings(
			'''
			Type mismatch: cannot convert from EDataType to EClass
			Type mismatch: cannot convert from EDataType to EClass
			Type mismatch: cannot convert from EEnum to EClass
			Type mismatch: cannot convert from EEnum to EClass
			extraneous input ',' expecting RULE_ID
			'''
		)
		// mismatch errors are duplicate due to the interpreter
	}

	@Test
	def void testCreateEClassWithSuperTypesNotEClassWithUnresolvedRef() {
		'''
			metamodel "foo"
			
			createEClass MyNewClass in foo
				extends FooDataType, AAA, FooEnum {}
		'''.parseWithTestEcore.assertErrorsAsStrings(
			'''
			AAA cannot be resolved.
			Type mismatch: cannot convert from EDataType to EClass
			Type mismatch: cannot convert from EDataType to EClass
			Type mismatch: cannot convert from EEnum to EClass
			Type mismatch: cannot convert from EEnum to EClass
			'''
		)
		// type mismatch error has not been reported on AAA
		// since it can't be resolved
		// mismatch errors are duplicate due to the interpreter
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
	def void testChangeEClassCannotReferToCreatedEClass() {
		val input = '''
		metamodel "foo"
		
		createEClass NewClass in foo {}
		
		changeEClass foo.NewClass {} // ERROR
		changeEClass foo.FooClass {} // OK
		'''
		input.parseWithTestEcore.assertErrorsAsStrings(
			"NewClass cannot be resolved."
		)
	}

	@Test
	def void testCreateEClassCanReferToRenamedEClass() {
		val input = '''
		metamodel "foo"
		
		createEClass NewClass in foo extends Renamed {}
		
		changeEClass foo.FooClass newName Renamed {}
		'''
		input.parseWithTestEcore.assertNoIssues
	}

	@Test
	def void testTimeoutInCancelIndicator() {
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
		input.parseWithTestEcore.assertWarning(
			EdeltaPackage.eINSTANCE.edeltaEcoreCreateEClassExpression,
			EdeltaValidator.INTERPRETER_TIMEOUT,
			input.lastIndexOf("{"), 11,
			"Timeout interpreting initialization block"
		)
	}

	@Test
	def void testUnresolvedEcoreReference() {
		'''
		metamodel "foo"
		
		ecoreref(NonExistant)
		ecoreref(FooClass)
		'''.parseWithTestEcore.assertErrorsAsStrings("NonExistant cannot be resolved.")
	}

	@Test
	def void testNoDanglingReferencesAfterInterpretation() {
		'''
		metamodel "foo"
		
		createEClass NewClass in foo {
			ecoreref(foo.FooClass).EPackage.EClassifiers.remove(ecoreref(foo.FooClass))
		}
		'''.parseWithTestEcore.assertNoErrors
	}

	@Test
	def void testCallMethodOnRenanedEClass() {
		val prog =
		'''
		metamodel "foo"

		changeEClass foo.FooClass {
			name = "RenamedClass"
			ecoreref(RenamedClass).getEAllStructuralFeatures
		}
		'''.parseWithTestEcore
		prog.assertNoErrors
	}

	@Test
	def void testCallNonExistingMethodOnRenanedEClass() {
		val prog =
		'''
		metamodel "foo"

		changeEClass foo.FooClass {
			name = "RenamedClass"
			ecoreref(RenamedClass).nonExistant("an arg")
		}
		'''.parseWithTestEcore
		prog.assertErrorsAsStrings
			("The method or field nonExistant(String) is undefined for the type EClass")
	}

	@Test
	def void testReferenceToAddedAttributeofRenamedClass() {
		'''
		import org.eclipse.emf.ecore.EClass

		metamodel "foo"

		changeEClass foo.FooClass {
			name = "RenamedClass"
			ecoreref(RenamedClass).getEStructuralFeatures += newEAttribute("added")
			ecoreref(RenamedClass.added)
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
		}
		'''.parseWithTestEcore
		prog.assertErrorsAsStrings
		('''
		The method or field nonExistant(String) is undefined for the type EClass
		The method or field sugarSet(String) is undefined for the type EClass
		''')
	}

	@Test
	def void testReferenceToAddedAttributeofRenamedClassInModifyEcore() {
		'''
		metamodel "foo"

		modifyEcore aTest epackage foo {
			ecoreref(foo.FooClass).name = "RenamedClass"
			ecoreref(RenamedClass).EStructuralFeatures.add(
				newEAttribute("addedAttribute"))
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
			ecoreref(RenamedClass).EStructuralFeatures += newEAttribute("added")
			ecoreref(RenamedClass.added)
		}
		'''.parseWithTestEcore.assertNoErrors
	}

	@Test
	def void testReferenceToAttributeofRenamedClassInModifyEcore() {
		'''
		import org.eclipse.emf.ecore.EClass

		metamodel "foo"

		modifyEcore aTest epackage foo {
			ecoreref(foo.FooClass).name = "RenamedClass"
			ecoreref(foo.RenamedClass).setAbstract(true)
			ecoreref(foo.RenamedClass).abstract = true
		}
		'''.parseWithTestEcore.assertNoErrors
	}

	@Test
	def void testValidLibMethodsInCreateEClass() {
		createEClassUsingLibMethods.parseWithTestEcore.assertNoErrors
	}

	@Test
	def void testValidLibMethodsInModifyEcore() {
		modifyEcoreUsingLibMethods.parseWithTestEcore.assertNoErrors
	}
}
