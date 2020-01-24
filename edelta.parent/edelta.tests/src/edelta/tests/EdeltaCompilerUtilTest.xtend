package edelta.tests

import com.google.inject.Inject
import edelta.compiler.EdeltaCompilerUtil
import org.eclipse.xtext.testing.InjectWith
import org.eclipse.xtext.testing.XtextRunner
import org.junit.Test
import org.junit.runner.RunWith

import static extension org.junit.Assert.*
import org.eclipse.emf.ecore.EcoreFactory

@RunWith(XtextRunner)
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
			'null'.
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

}
