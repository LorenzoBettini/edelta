package edelta.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.eclipse.emf.ecore.EcoreFactory;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.extensions.InjectionExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.google.inject.Inject;

import edelta.compiler.EdeltaCompilerUtil;
import edelta.lib.EdeltaModelManager;
import edelta.tests.injectors.EdeltaInjectorProviderCustom;

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
	void testGetStringForEcoreReferenceExpression(final String input, final String expected) throws Exception {
		assertEquals(expected,
			edeltaCompilerUtil.getStringForEcoreReferenceExpression(
					ecoreReferenceExpression(input)));
	}

	@Test
	void testGetStringForEcoreReferenceExpressionEAttributeInSubPackage() throws Exception {
		var ecoreRefExp = 
			lastEcoreReferenceExpression(
				parseWithTestEcoreWithSubPackage(
				"""
					metamodel "mainpackage"
					
					modifyEcore aTest epackage mainpackage {
					    ecoreref(mySubPackageAttribute)
					}"""
				));
		assertEquals(
			"getEAttribute(\"mainpackage.mainsubpackage\", \"MainSubPackageFooClass\", \"mySubPackageAttribute\")",
			edeltaCompilerUtil.getStringForEcoreReferenceExpression(ecoreRefExp));
	}

	@Test
	void testGetEcoreversionsRelativePathEmpty() {
		var modelManager = new EdeltaModelManager();
		var resource = modelManager.loadEcoreFile(METAMODEL_PATH + SIMPLE_ECORE);
		String path = edeltaCompilerUtil.getEcoreversionsRelativePath(resource);
		assertEquals(SIMPLE_ECORE, path);
	}

	@Test
	void testGetEcoreversionsRelativePathNonEmpty() {
		var modelManager = new EdeltaModelManager();
		var resource = modelManager.loadEcoreFile(METAMODEL_PATH + ECOREVERSIONS + ECORE_IN_ECORE_VERSIONS_ECORE);
		String path = edeltaCompilerUtil.getEcoreversionsRelativePath(resource);
		assertEquals(ECORE_IN_ECORE_VERSIONS_ECORE, path);
	}

	@Test
	void testGetEcoreversionsRelativePathInSubdir() {
		var modelManager = new EdeltaModelManager();
		var resource = modelManager.loadEcoreFile(METAMODEL_PATH + ECOREVERSIONS_V1 + ECORE_IN_ECORE_VERSIONS_SUBDIR_ECORE);
		String path = edeltaCompilerUtil.getEcoreversionsRelativePath(resource);
		assertEquals("v1/EcoreInEcoreVersionsSubdir.ecore", path);
	}
}
