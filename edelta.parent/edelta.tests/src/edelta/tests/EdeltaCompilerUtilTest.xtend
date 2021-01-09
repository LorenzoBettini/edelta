package edelta.tests

import com.google.inject.Inject
import edelta.compiler.EdeltaCompilerUtil
import org.eclipse.emf.ecore.EcoreFactory
import org.eclipse.xtext.testing.InjectWith
import org.eclipse.xtext.testing.extensions.InjectionExtension
import org.junit.jupiter.api.^extension.ExtendWith

import org.junit.jupiter.api.Test
import static extension org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

@ExtendWith(InjectionExtension)
@InjectWith(EdeltaInjectorProviderCustom)
class EdeltaCompilerUtilTest extends EdeltaAbstractTest {

	@Inject extension EdeltaCompilerUtil edeltaCompilerUtil

	@Test
	def void testGetEPackageNameOrNull() {
		val factory = EcoreFactory.eINSTANCE
		null.EPackageNameOrNull.assertNull
		val p = factory.createEPackage => [
			name = "test"
		]
		"test".assertEquals(p.EPackageNameOrNull)
	}

	@ParameterizedTest
	@CsvSource(#[
		"ecoreref(FooClass), 'getEClass(\"foo\", \"FooClass\")'",
		"ecoreref(myAttribute), 'getEAttribute(\"foo\", \"FooClass\", \"myAttribute\")'",
		"ecoreref(FooEnumLiteral), 'getEEnumLiteral(\"foo\", \"FooEnum\", \"FooEnumLiteral\")'",
		"ecoreref(foo), 'getEPackage(\"foo\")'",
		"ecoreref, 'null'", // incomplete -> null
		"ecoreref(), 'getENamedElement()'", // incomplete
		"ecoreref(NonExistant), 'getENamedElement(\"\", \"\", \"\")'"
	])
	def void testGetStringForEcoreReferenceExpression(String input, String expected) {
		val ecoreRefExp = input.ecoreReferenceExpression
		expected.
			assertEquals(
				edeltaCompilerUtil.getStringForEcoreReferenceExpression(ecoreRefExp)
			)
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
