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
		referenceToEPackage.assertType(EPackage)
	}

	@Test
	def void testTypeOfReferenceToEClass() {
		referenceToEClass.assertType(EClass)
	}

	@Test
	def void testTypeOfReferenceToEDataType() {
		referenceToEDataType.assertType(EDataType)
	}

	@Test
	def void testTypeOfReferenceToEEnum() {
		referenceToEEnum.assertType(EEnum)
	}

	@Test
	def void testTypeOfReferenceToEAttribute() {
		referenceToEAttribute.assertType(EAttribute)
	}

	@Test
	def void testTypeOfReferenceToEReference() {
		referenceToEReference.assertType(EReference)
	}

	@Test
	def void testTypeOfReferenceToEEnumLiteral() {
		referenceToEEnumLiteral.assertType(EEnumLiteral)
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
			metamodel "foo"
			
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
		"ecoreref".assertPrimitiveVoid
	}

	@Test
	def void testTypeOfReferenceToNullNamedElement2() {
		"ecoreref()".assertPrimitiveVoid
	}

	@Test
	def void testTypeOfCreateEClassExpression() {
		"createEClass Test in foo".assertType(EClass)
	}

	@Test
	def void testTypeOfCreateEAttributeExpression() {
		"createEClass Test in foo {
			createEAttribute myAttribute
		}".assertTypeOfCreateEClassBody(EAttribute)
	}

	@Test
	def void testTypeOfCreateEAttributeExpressionType() {
		'''
		metamodel "foo"
		
		createEClass Test in foo {
			createEAttribute myAttribute type FooDataType {}
		}
		'''.assertTypeOfCreateEAttributeType(EDataType)
	}


	@Test
	def void testTypeOfUnresolvedEcoreRefSuperTypeIsStillEClass() {
		'''
			metamodel "foo"
			
			createEClass MyNewClass in foo
				extends AAA {}
		'''.assertTypeOfCreateEClassSuperType(EClass)
	}

	@Test
	def void testTypeOfNullEcoreRefSuperTypeIsStillEClass() {
		'''
			metamodel "foo"
			
			createEClass MyNewClass in foo
				extends {}
		'''.assertTypeOfCreateEClassSuperType(EClass)
	}

	@Test
	def void testTypeOfChangeEClassExpression() {
		"changeEClass foo.Test {}".assertType(EClass)
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
		input.parseWithTestEcore.lastExpression => [
			expected.canonicalName.assertEquals(
				resolveTypes.getActualType(it).identifier
			)
		]
	}

	def private assertPrimitiveVoid(CharSequence input) {
		input.parseWithTestEcore.lastExpression => [
			"void".assertEquals(
				resolveTypes.getActualType(it).identifier
			)
		]
	}

	def private assertTypeOfCreateEClassBody(CharSequence input, Class<?> expected) {
		input.parseWithTestEcore.lastExpression => [
			expected.canonicalName.assertEquals(
				resolveTypes.getActualType(
					createEClassExpression.body.expressions.last
				).identifier
			)
		]
	}

	def private assertTypeOfCreateEClassSuperType(CharSequence input, Class<?> expected) {
		input.parseWithTestEcore.lastExpression => [
			expected.canonicalName.assertEquals(
				resolveTypes.getActualType(
					createEClassExpression.ecoreReferenceSuperTypes.last
				).identifier
			)
		]
	}

	def private assertTypeOfCreateEAttributeType(CharSequence input, Class<?> expected) {
		input.parseWithTestEcore.lastExpression.createEClassExpression.
			body.expressions.last => [
			expected.canonicalName.assertEquals(
				resolveTypes.getActualType(
					createEAttributExpression.ecoreReferenceDataType
				).identifier
			)
		]
	}

	def private assertTypeOfRightExpression(CharSequence input, Class<?> expected) {
		input.parseWithTestEcore.lastExpression => [
			expected.canonicalName.assertEquals(
				resolveTypes.getActualType(
					variableDeclaration.right
				).identifier
			)
		]
	}
}
