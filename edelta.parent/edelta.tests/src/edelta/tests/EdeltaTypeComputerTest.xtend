package edelta.tests

import com.google.inject.Inject
import org.eclipse.emf.ecore.EAttribute
import org.eclipse.emf.ecore.EClass
import org.eclipse.emf.ecore.EDataType
import org.eclipse.emf.ecore.EEnum
import org.eclipse.emf.ecore.EEnumLiteral
import org.eclipse.emf.ecore.ENamedElement
import org.eclipse.emf.ecore.EPackage
import org.eclipse.emf.ecore.EReference
import org.eclipse.xtext.testing.InjectWith
import org.eclipse.xtext.testing.XtextRunner
import org.eclipse.xtext.xbase.typesystem.IBatchTypeResolver
import org.junit.Test
import org.junit.runner.RunWith

import static extension org.junit.Assert.*

@RunWith(XtextRunner)
@InjectWith(EdeltaInjectorProviderCustom)
class EdeltaTypeComputerTest extends EdeltaAbstractTest {

	@Inject extension IBatchTypeResolver

	@Test
	def void testTypeOfReferenceToEPackage() {
		"ecoreref(foo)".assertType(EPackage)
	}

	@Test
	def void testTypeOfReferenceToEClass() {
		"ecoreref(FooClass)".assertType(EClass)
	}

	@Test
	def void testTypeOfReferenceToEDataType() {
		"ecoreref(FooDataType)".assertType(EDataType)
	}

	@Test
	def void testTypeOfReferenceToEEnum() {
		"ecoreref(FooEnum)".assertType(EEnum)
	}

	@Test
	def void testTypeOfReferenceToEAttribute() {
		"ecoreref(myAttribute)".assertType(EAttribute)
	}

	@Test
	def void testTypeOfReferenceToEReference() {
		"ecoreref(myReference)".assertType(EReference)
	}

	@Test
	def void testTypeOfReferenceToEEnumLiteral() {
		"ecoreref(FooEnumLiteral)".assertType(EEnumLiteral)
	}

	@Test
	def void testTypeOfReferenceToUnresolvedENamedElement() {
		"ecoreref(NonExistant)".assertType(ENamedElement)
	}

	@Test
	def void testTypeOfReferenceToUnresolvedENamedElementWithExpectations() {
		"val org.eclipse.emf.ecore.EClass c = ecoreref(NonExistant)".
			assertTypeOfRightExpression(EClass)
	}

	@Test
	def void testTypeOfReferenceToUnresolvedQualifiedENamedElementWithExpectations() {
		'''
			val org.eclipse.emf.ecore.EClass c = ecoreref(FooClass.NonExistant)
		'''.
			assertTypeOfRightExpression(EClass)
	}

	@Test
	def void testTypeOfReferenceToUnresolvedENamedElementAtLeastENamedElement() {
		"val Object c = ecoreref(NonExistant)".
			assertTypeOfRightExpression(ENamedElement)
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

	def private assertType(CharSequence input, Class<?> expected) {
		input.ecoreReferenceExpression => [
			expected.canonicalName.assertEquals(
				resolveTypes.getActualType(it).identifier
			)
		]
	}

	def private assertENamedElement(CharSequence input) {
		input.ecoreReferenceExpression => [
			ENamedElement.canonicalName.assertEquals(
				resolveTypes.getActualType(it).identifier
			)
		]
	}

	def private assertTypeOfRightExpression(CharSequence input, Class<?> expected) {
		'''
			metamodel "foo"
			
			modifyEcore aTest epackage foo {
				«input»
			}
		'''
		.parseWithTestEcore
		.lastModifyEcoreOperation.body.blockLastExpression => [
			expected.canonicalName.assertEquals(
				resolveTypes.getActualType(
					variableDeclaration.right
				).identifier
			)
		]
	}
}
