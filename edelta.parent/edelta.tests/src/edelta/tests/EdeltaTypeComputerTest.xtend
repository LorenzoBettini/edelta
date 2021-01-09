package edelta.tests

import com.google.inject.Inject
import org.eclipse.emf.ecore.EClass
import org.eclipse.xtext.testing.InjectWith
import org.eclipse.xtext.testing.extensions.InjectionExtension
import org.eclipse.xtext.xbase.typesystem.IBatchTypeResolver
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.^extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

import static extension org.junit.jupiter.api.Assertions.*

@ExtendWith(InjectionExtension)
@InjectWith(EdeltaInjectorProviderCustom)
class EdeltaTypeComputerTest extends EdeltaAbstractTest {

	@Inject extension IBatchTypeResolver typeResolver

	@ParameterizedTest
	@CsvSource(#[
		"foo, EPackage",
		"FooClass, EClass",
		"FooDataType, EDataType",
		"FooEnum, EEnum",
		"myAttribute, EAttribute",
		"myReference, EReference",
		"FooEnumLiteral, EEnumLiteral",
		"NonExistant, ENamedElement"
	])
	def void testTypeOfEcoreReference(String ecoreRefArg, String expectedType) {
		assertType(
			"ecoreref(" + ecoreRefArg + ")",
			"org.eclipse.emf.ecore." + expectedType)
	}

	@ParameterizedTest
	@CsvSource(#[
		"'val org.eclipse.emf.ecore.EClass c = ecoreref(NonExistant)', EClass",
		"'val org.eclipse.emf.ecore.EClass c = ecoreref(FooClass.NonExistant)', EClass",
		"'val Object c = ecoreref(NonExistant)', ENamedElement"
	])
	def void testTypeOfEcoreReferenceWithExpectation(String input, String expectedType) {
		assertTypeOfRightExpression(
			input,
			"org.eclipse.emf.ecore." + expectedType)
	}

	@Test
	def void testTypeOfReferenceToNullNamedElement() {
		"ecoreref".assertENamedElement
	}

	@Test
	def void testTypeOfReferenceToNullNamedElement2() {
		"ecoreref()".assertENamedElement
	}

	@Test
	def void testTypeForRenamedEClassInModifyEcore() {
		val prog =
		'''
		metamodel "foo"

		modifyEcore aTest epackage foo {
			ecoreref(foo.FooClass).name = "RenamedClass"
			ecoreref(RenamedClass)
		}
		'''.parseWithTestEcore
		val ecoreref = prog.lastModifyEcoreOperation.body.blockLastExpression.
			edeltaEcoreReferenceExpression
		assertEquals(EClass.canonicalName,
			ecoreref.resolveTypes.getActualType(ecoreref).identifier
		)
	}

	@Test
	def void testTypeForRenamedQualifiedEClassInModifyEcore() {
		val prog =
		'''
		metamodel "foo"

		modifyEcore aTest epackage foo {
			ecoreref(foo.FooClass).name = "RenamedClass"
			ecoreref(foo.RenamedClass)
		}
		'''.parseWithTestEcore
		val ecoreref = prog.lastModifyEcoreOperation.body.blockLastExpression.
			edeltaEcoreReferenceExpression
		assertEquals(EClass.canonicalName,
			ecoreref.resolveTypes.getActualType(ecoreref).identifier
		)
	}

	def private assertType(CharSequence input, String expectedTypeFQN) {
		val ecoreRefExp = input.ecoreReferenceExpression
		expectedTypeFQN.assertEquals(
			ecoreRefExp.resolveTypes.getActualType(ecoreRefExp).identifier
		)
	}

	def private assertENamedElement(CharSequence input) {
		input.assertType("org.eclipse.emf.ecore.ENamedElement")
	}

	def private assertTypeOfRightExpression(CharSequence input, String expectedTypeFQN) {
		"
			metamodel \"foo\"
			
			modifyEcore aTest epackage foo {
				«input»
			}
		".
		replace("«input»", input)
		.parseWithTestEcore
		.lastModifyEcoreOperation.body.blockLastExpression => [
			expectedTypeFQN.assertEquals(
				resolveTypes.getActualType(
					variableDeclaration.right
				).identifier
			)
		]
	}
}
