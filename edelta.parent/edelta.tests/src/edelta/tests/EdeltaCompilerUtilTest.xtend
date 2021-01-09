package edelta.tests

import com.google.inject.Inject
import edelta.compiler.EdeltaCompilerUtil
import org.eclipse.emf.ecore.EcoreFactory
import org.eclipse.xtext.testing.InjectWith
import org.eclipse.xtext.testing.extensions.InjectionExtension
import org.junit.jupiter.api.^extension.ExtendWith

import org.junit.jupiter.api.Test
import static extension org.junit.jupiter.api.Assertions.*

@ExtendWith(InjectionExtension)
@InjectWith(EdeltaInjectorProviderCustom)
class EdeltaCompilerUtilTest extends EdeltaAbstractTest {

	@Inject extension EdeltaCompilerUtil

	@Test
	def void testGetEPackageNameOrNull() {
		val factory = EcoreFactory.eINSTANCE
		null.EPackageNameOrNull.assertNull
		val p = factory.createEPackage => [
			name = "test"
		]
		"test".assertEquals(p.EPackageNameOrNull)
	}

	@Test
	def void testGetStringForEcoreReferenceExpressionEClass() {
		'''ecoreref(FooClass)'''.ecoreReferenceExpression => [
			'getEClass("foo", "FooClass")'.
				assertEquals(stringForEcoreReferenceExpression)
		]
	}

	@Test
	def void testGetStringForEcoreReferenceExpressionEAttribute() {
		'''ecoreref(myAttribute)'''.ecoreReferenceExpression => [
			'getEAttribute("foo", "FooClass", "myAttribute")'.
				assertEquals(stringForEcoreReferenceExpression)
		]
	}

	@Test
	def void testGetStringForEcoreReferenceExpressionEEnumLiteral() {
		'''ecoreref(FooEnumLiteral)'''.ecoreReferenceExpression => [
			'getEEnumLiteral("foo", "FooEnum", "FooEnumLiteral")'.
				assertEquals(stringForEcoreReferenceExpression)
		]
	}

	@Test
	def void testGetStringForEcoreReferenceExpressionEPackage() {
		'''ecoreref(foo)'''.ecoreReferenceExpression=> [
			'getEPackage("foo")'.
				assertEquals(stringForEcoreReferenceExpression)
		]
	}

	@Test
	def void testGetStringForEcoreReferenceExpressionIncomplete() {
		'''ecoreref'''.ecoreReferenceExpression => [
			'null'.
				assertEquals(stringForEcoreReferenceExpression)
		]
	}

	@Test
	def void testGetStringForEcoreReferenceExpressionIncomplete2() {
		'''ecoreref()'''.ecoreReferenceExpression => [
			'getENamedElement()'.
				assertEquals(stringForEcoreReferenceExpression)
		]
	}

	@Test
	def void testGetStringForEcoreReferenceExpressionUnresolved() {
		'''ecoreref(NonExistant)'''.ecoreReferenceExpression => [
			'getENamedElement("", "", "")'.
				assertEquals(stringForEcoreReferenceExpression)
		]
	}

	@Test
	def void testGetStringForEcoreReferenceExpressionEAttributeInSubPackage() {
		'''
		metamodel "mainpackage"
		
		modifyEcore aTest epackage mainpackage {
			ecoreref(mySubPackageAttribute)
		}
		'''.parseWithTestEcoreWithSubPackage
			.lastEcoreReferenceExpression => [
			'getEAttribute("mainpackage.mainsubpackage", "MainSubPackageFooClass", "mySubPackageAttribute")'.
				assertEquals(stringForEcoreReferenceExpression)
		]
	}

}
