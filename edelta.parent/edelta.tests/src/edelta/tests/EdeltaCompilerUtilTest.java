package edelta.tests;

import static org.junit.jupiter.api.Assertions.*;

import org.eclipse.emf.ecore.EcoreFactory;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.extensions.InjectionExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.google.inject.Inject;

import edelta.compiler.EdeltaCompilerUtil;

@ExtendWith(InjectionExtension.class)
@InjectWith(EdeltaInjectorProviderCustom.class)
class EdeltaCompilerUtilTest extends EdeltaAbstractTest {
	@Inject
	private EdeltaCompilerUtil edeltaCompilerUtil;

	@Test
	void testGetEPackageNameOrNull() {
		var factory = EcoreFactory.eINSTANCE;
		assertNull(edeltaCompilerUtil.getEPackageNameOrNull(null));
		var p = factory.createEPackage();
		p.setName("test");
		assertEquals("test", edeltaCompilerUtil.getEPackageNameOrNull(p));
	}

	@ParameterizedTest
	@CsvSource({
		"ecoreref(FooClass), 'getEClass(\"foo\", \"FooClass\")'",
		"ecoreref(myAttribute), 'getEAttribute(\"foo\", \"FooClass\", \"myAttribute\")'",
		"ecoreref(FooEnumLiteral), 'getEEnumLiteral(\"foo\", \"FooEnum\", \"FooEnumLiteral\")'",
		"ecoreref(foo), 'getEPackage(\"foo\")'",
		"ecoreref, 'null'", // incomplete -> null
		"ecoreref(), 'getENamedElement()'", // incomplete
		"ecoreref(NonExistant), 'getENamedElement(\"\", \"\", \"\")'"
	})
	void testGetStringForEcoreReferenceExpression(final String input, final String expected) {
		assertEquals(expected,
			edeltaCompilerUtil.getStringForEcoreReferenceExpression(
					ecoreReferenceExpression(input)));
	}

	@Test
	void testGetStringForEcoreReferenceExpressionEAttributeInSubPackage() {
		var ecoreRefExp = 
			lastEcoreReferenceExpression(
				parseWithTestEcoreWithSubPackage(
				"metamodel \"mainpackage\"\n"
				+ "\n"
				+ "modifyEcore aTest epackage mainpackage {\n"
				+ "    ecoreref(mySubPackageAttribute)\n"
				+ "}"
				));
		assertEquals(
			"getEAttribute(\"mainpackage.mainsubpackage\", \"MainSubPackageFooClass\", \"mySubPackageAttribute\")",
			edeltaCompilerUtil.getStringForEcoreReferenceExpression(ecoreRefExp));
	}
}
