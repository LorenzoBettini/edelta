package edelta.tests;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.nio.file.Paths;

import org.eclipse.emf.ecore.EcoreFactory;
import org.eclipse.emf.ecore.resource.impl.ResourceImpl;
import org.eclipse.emf.common.util.URI;
import org.eclipse.xtext.resource.XtextResourceSet;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.extensions.InjectionExtension;
import org.eclipse.xtext.workspace.FileProjectConfig;
import org.eclipse.xtext.workspace.ProjectConfigAdapter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.google.inject.Inject;
import com.google.inject.Provider;

import edelta.compiler.EdeltaCompilerUtil;
import edelta.lib.EdeltaModelManager;
import edelta.tests.injectors.EdeltaInjectorProviderCustom;

@ExtendWith(InjectionExtension.class)
@InjectWith(EdeltaInjectorProviderCustom.class)
class EdeltaCompilerUtilTest extends EdeltaAbstractTest {
	@Inject
	private EdeltaCompilerUtil edeltaCompilerUtil;

	@Inject
	private Provider<XtextResourceSet> resourceSetProvider;

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
			getLastEcoreReferenceExpression(
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
	void testGetRelativeSourcePathFallsBackToLastSegment() {
		var modelManager = new EdeltaModelManager();

		assertAll("Relative source path tests without project config",
			() -> assertEquals(SIMPLE_ECORE,
				edeltaCompilerUtil.getRelativeSourcePath(
					modelManager.loadEcoreFile(METAMODEL_PATH + SIMPLE_ECORE))),
			() -> assertEquals(ECORE_IN_ECORE_VERSIONS_ECORE,
				edeltaCompilerUtil.getRelativeSourcePath(
					modelManager.loadEcoreFile(METAMODEL_PATH + ECOREVERSIONS + ECORE_IN_ECORE_VERSIONS_ECORE))),
			() -> assertEquals(ECORE_IN_ECORE_VERSIONS_SUBDIR_ECORE,
				edeltaCompilerUtil.getRelativeSourcePath(
					modelManager.loadEcoreFile(METAMODEL_PATH + ECOREVERSIONS_V1 + ECORE_IN_ECORE_VERSIONS_SUBDIR_ECORE)))
		);
	}

	@Test
	void testGetRelativeSourcePathWithNoResourceSet() {
		var resource = new ResourceImpl(URI.createURI("test.ecore"));
		assertEquals("test.ecore", edeltaCompilerUtil.getRelativeSourcePath(resource));
	}

	@Test
	void testGetRelativeSourcePathWithEcoreversionsSourceFolder() {
		var projectPath = Paths.get(METAMODEL_PATH).toAbsolutePath().toFile();
		var projectConfig = new FileProjectConfig(projectPath, "testProject");
		projectConfig.addSourceFolder("ecoreversions");

		var resourceSet = resourceSetProvider.get();
		ProjectConfigAdapter.install(resourceSet, projectConfig);

		assertAll("Relative source path tests with ecoreversions source folder",
			() -> assertEquals(ECORE_IN_ECORE_VERSIONS_ECORE,
				edeltaCompilerUtil.getRelativeSourcePath(
					resourceSet.createResource(
						createFileURIFromPath(METAMODEL_PATH + ECOREVERSIONS + ECORE_IN_ECORE_VERSIONS_ECORE)))),
			() -> assertEquals("v1/" + ECORE_IN_ECORE_VERSIONS_SUBDIR_ECORE,
				edeltaCompilerUtil.getRelativeSourcePath(
					resourceSet.createResource(
						createFileURIFromPath(METAMODEL_PATH + ECOREVERSIONS_V1 + ECORE_IN_ECORE_VERSIONS_SUBDIR_ECORE)))),
			// resource not in any declared source folder falls back to lastSegment
			() -> assertEquals(SIMPLE_ECORE,
				edeltaCompilerUtil.getRelativeSourcePath(
					resourceSet.createResource(
						createFileURIFromPath(METAMODEL_PATH + SIMPLE_ECORE))))
		);
	}

	@Test
	void testGetRelativeSourcePathWithOtherSourceFolder() {
		var projectPath = Paths.get(METAMODEL_PATH).toAbsolutePath().toFile();
		var projectConfig = new FileProjectConfig(projectPath, "testProject");
		projectConfig.addSourceFolder("ecoreother");

		var resourceSet = resourceSetProvider.get();
		ProjectConfigAdapter.install(resourceSet, projectConfig);

		assertEquals(ECORE_IN_ECORE_OTHER_ECORE,
			edeltaCompilerUtil.getRelativeSourcePath(
				resourceSet.createResource(
					createFileURIFromPath(METAMODEL_PATH + ECOREOTHER + ECORE_IN_ECORE_OTHER_ECORE))));
	}

}
