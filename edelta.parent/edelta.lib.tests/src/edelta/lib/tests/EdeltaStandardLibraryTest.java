package edelta.lib.tests;

import static edelta.lib.EdeltaEcoreUtil.createInstance;
import static edelta.lib.EdeltaEcoreUtil.getValueAsList;
import static edelta.testutils.EdeltaTestUtils.assertFilesAreEquals;
import static edelta.testutils.EdeltaTestUtils.cleanDirectoryRecursive;
import static java.util.List.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.emf.ecore.EcorePackage.Literals.EINT;
import static org.eclipse.emf.ecore.EcorePackage.Literals.EOBJECT;
import static org.eclipse.emf.ecore.EcorePackage.Literals.ESTRING;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import org.eclipse.emf.ecore.ENamedElement;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.ETypedElement;
import org.eclipse.emf.ecore.EcoreFactory;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.ecore.util.EcoreUtil.EqualityHelper;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.function.ThrowingRunnable;

import edelta.lib.EdeltaDefaultRuntime;
import edelta.lib.EdeltaEcoreUtil;
import edelta.lib.EdeltaEngine;
import edelta.lib.EdeltaEngine.EdeltaRuntimeProvider;
import edelta.lib.EdeltaIssuePresenter;
import edelta.lib.EdeltaModelManager;
import edelta.lib.EdeltaRuntime;
import edelta.lib.EdeltaStandardLibrary;

/**
 * Library functions for manipulating an Ecore model.
 *
 * @author Lorenzo Bettini
 *
 */
public class EdeltaStandardLibraryTest {

	private static final String TESTDATA = "../edelta.testdata/testdata/";
	private static final String OUTPUT = "output/";
	private static final String EXPECTATIONS = "../edelta.testdata/expectations/";

	private static final String MYPACKAGE = "mypackage";
	private static final String MYOTHERPACKAGE = "myotherpackage";
	private static final String MY2_ECORE = "My2.ecore";
	private static final String MY_ECORE = "My.ecore";
	private static final String TESTECORES = "testecores/";

	private static EcoreFactory ecoreFactory = EcoreFactory.eINSTANCE;

	private EdeltaStandardLibrary lib;

	private EdeltaIssuePresenter issuePresenter;

	private EdeltaModelManager modelManager;

	@BeforeClass
	public static void clearOutput() throws IOException {
		cleanDirectoryRecursive(OUTPUT);
	}

	@Before
	public void setup() {
		issuePresenter = mock(EdeltaIssuePresenter.class);
		modelManager = new EdeltaModelManager();
		lib = new EdeltaStandardLibrary(modelManager);
		lib.setIssuePresenter(issuePresenter);
	}

	private EdeltaEngine setupEngine(
			String subdir,
			Collection<String> ecoreFiles,
			Collection<String> modelFiles,
			EdeltaRuntimeProvider runtimeProvider
		) {
		var basedir = TESTDATA + subdir;
		var engine = new EdeltaEngine(runtimeProvider);
		ecoreFiles
			.forEach(fileName -> engine.loadEcoreFile(basedir + fileName));
		modelFiles
			.forEach(fileName -> engine.loadModelFile(basedir + fileName));
		return engine;
	}

	@Test
	public void testGetEPackageWithOtherEdelta() {
		EdeltaRuntime other = new EdeltaRuntime(modelManager) {
		};
		lib = new EdeltaStandardLibrary(other);
		// now lib and other share the same package manager
		// so if we load something with lib we can get it with other
		loadTestEcore(MY_ECORE);
		loadTestEcore(MY2_ECORE);
		var ePackage = other.getEPackage(MYPACKAGE);
		assertEquals(MYPACKAGE, ePackage.getName());
		assertNotNull(ePackage);
		assertNotNull(other.getEPackage(MYOTHERPACKAGE));
		assertNull(other.getEPackage("foo"));
	}

	@Test
	public void test_addEClass() {
		var ePackage = ecoreFactory.createEPackage();
		var eClass = ecoreFactory.createEClass();
		lib.addEClass(ePackage, eClass);
		assertSame(eClass,
				ePackage.getEClassifiers().get(0));
	}

	@Test
	public void test_addEClass_alreadExisting() {
		var ePackage = ecoreFactory.createEPackage();
		ePackage.setName("TestPackage");
		var eClass = ecoreFactory.createEClass();
		eClass.setName("TestClass");
		lib.addEClass(ePackage, eClass);
		var eClass1 = ecoreFactory.createEClass();
		eClass1.setName("TestClass");
		assertThrowsIAE(
			() -> lib.addEClass(ePackage, eClass1),
			eClass1,
			"TestPackage already contains EClass TestPackage.TestClass");
	}

	@Test
	public void test_addNewEClass() {
		var ePackage = ecoreFactory.createEPackage();
		var eClass = lib.addNewEClass(ePackage, "test");
		assertEquals("test", eClass.getName());
		assertSame(eClass,
			ePackage.getEClassifiers().get(0));
	}

	@Test
	public void test_addNewEClassWithInitializer() {
		var ePackage = ecoreFactory.createEPackage();
		var eClass = lib.addNewEClass(ePackage, "test",
				cl -> {
					assertNotNull(cl.getEPackage());
					cl.setName("changed");
				});
		assertEquals("changed", eClass.getName());
		assertSame(eClass,
				ePackage.getEClassifiers().get(0));
	}

	@Test
	public void test_addNewEClassAsSibling() {
		var ePackage = ecoreFactory.createEPackage();
		var sibling = ecoreFactory.createEClass();
		ePackage.getEClassifiers().add(sibling);
		var eClass = lib.addNewEClassAsSibling(sibling, "test");
		assertEquals("test", eClass.getName());
		assertSame(eClass,
			ePackage.getEClassifiers().get(1));
	}

	@Test
	public void test_addNewEClassAsSiblingWithInitializer() {
		var ePackage = ecoreFactory.createEPackage();
		var sibling = ecoreFactory.createEClass();
		ePackage.getEClassifiers().add(sibling);
		var eClass = lib.addNewEClassAsSibling(sibling, "test",
				cl -> {
					assertNotNull(cl.getEPackage());
					cl.setName("changed");
				});
		assertEquals("changed", eClass.getName());
		assertSame(eClass,
				ePackage.getEClassifiers().get(1));
	}

	@Test
	public void test_addNewAbstractEClass() {
		var ePackage = ecoreFactory.createEPackage();
		var eClass = lib.addNewAbstractEClass(ePackage, "test");
		assertEquals("test", eClass.getName());
		assertThat(eClass.isAbstract())
			.isTrue();
		assertSame(eClass,
			ePackage.getEClassifiers().get(0));
	}

	@Test
	public void test_addNewAbstractEClassWithInitializer() {
		var ePackage = ecoreFactory.createEPackage();
		var eClass = lib.addNewAbstractEClass(ePackage, "test",
				cl -> {
					assertNotNull(cl.getEPackage());
					cl.setName("changed");
				});
		assertEquals("changed", eClass.getName());
		assertThat(eClass.isAbstract())
			.isTrue();
		assertSame(eClass,
				ePackage.getEClassifiers().get(0));
	}

	@Test
	public void test_addEEnum() {
		var ePackage = ecoreFactory.createEPackage();
		var eEnum = ecoreFactory.createEEnum();
		lib.addEEnum(ePackage, eEnum);
		assertSame(eEnum,
				ePackage.getEClassifiers().get(0));
	}

	@Test
	public void test_addNewEEnum() {
		var ePackage = ecoreFactory.createEPackage();
		var eEnum = lib.addNewEEnum(ePackage, "test");
		assertEquals("test", eEnum.getName());
		assertSame(eEnum,
			ePackage.getEClassifiers().get(0));
	}

	@Test
	public void test_addNewEEnumWithInitializer() {
		var ePackage = ecoreFactory.createEPackage();
		var eEnum = lib.addNewEEnum(ePackage, "test",
				cl -> {
					assertNotNull(cl.getEPackage());
					cl.setName("changed");
				});
		assertEquals("changed", eEnum.getName());
		assertSame(eEnum,
				ePackage.getEClassifiers().get(0));
	}

	@Test
	public void test_addNewEEnumAsSibling() {
		var ePackage = ecoreFactory.createEPackage();
		var sibling = ecoreFactory.createEEnum();
		ePackage.getEClassifiers().add(sibling);
		var eEnum = lib.addNewEEnumAsSibling(sibling, "test");
		assertEquals("test", eEnum.getName());
		assertSame(eEnum,
			ePackage.getEClassifiers().get(1));
	}

	@Test
	public void test_addNewEEnumAsSiblingWithInitializer() {
		var ePackage = ecoreFactory.createEPackage();
		var sibling = ecoreFactory.createEEnum();
		ePackage.getEClassifiers().add(sibling);
		var eEnum = lib.addNewEEnumAsSibling(sibling, "test",
				cl -> {
					assertNotNull(cl.getEPackage());
					cl.setName("changed");
				});
		assertEquals("changed", eEnum.getName());
		assertSame(eEnum,
				ePackage.getEClassifiers().get(1));
	}

	@Test
	public void test_addEEnumLiteral() {
		var eEnum = ecoreFactory.createEEnum();
		var eEnumLiteral = ecoreFactory.createEEnumLiteral();
		lib.addEEnumLiteral(eEnum, eEnumLiteral);
		assertSame(eEnumLiteral,
				eEnum.getELiterals().get(0));
	}

	@Test
	public void test_addEEnumLiteral_alreadyExisting() {
		var eEnum = ecoreFactory.createEEnum();
		eEnum.setName("TestEnum");
		var eEnumLiteral = ecoreFactory.createEEnumLiteral();
		eEnumLiteral.setName("TestLiteral");
		lib.addEEnumLiteral(eEnum, eEnumLiteral);
		assertSame(eEnumLiteral,
				eEnum.getELiterals().get(0));
		var eEnumLiteral1 = ecoreFactory.createEEnumLiteral();
		eEnumLiteral1.setName("TestLiteral");
		assertThrowsIAE(() -> lib.addEEnumLiteral(eEnum, eEnumLiteral1),
			eEnumLiteral1,
			"TestEnum already contains EEnumLiteral TestEnum.TestLiteral");
	}

	@Test
	public void test_addNewEEnumLiteral() {
		var eEnum = ecoreFactory.createEEnum();
		var eEnumLiteral =
				lib.addNewEEnumLiteral(eEnum, "test");
		assertEquals("test", eEnumLiteral.getName());
		assertSame(eEnumLiteral,
				eEnum.getELiterals().get(0));
	}

	@Test
	public void test_addNewEEnumLiteralWithInitializer() {
		var eEnum = ecoreFactory.createEEnum();
		var eEnumLiteral = lib.addNewEEnumLiteral(eEnum, "test",
				lit -> {
					assertNotNull(lit.getEEnum());
					lit.setName("changed");
				});
		assertEquals("changed", eEnumLiteral.getName());
		assertSame(eEnumLiteral,
				eEnum.getELiterals().get(0));
	}

	@Test
	public void test_addNewEEnumLiteralAsSibling() {
		var eEnum = ecoreFactory.createEEnum();
		var sibling = ecoreFactory.createEEnumLiteral();
		eEnum.getELiterals().add(sibling);
		var eEnumLiteral =
				lib.addNewEEnumLiteralAsSibling(sibling, "test");
		assertEquals("test", eEnumLiteral.getName());
		assertSame(eEnumLiteral,
				eEnum.getELiterals().get(1));
	}

	@Test
	public void test_addNewEEnumLiteralAsSiblingWithInitializer() {
		var eEnum = ecoreFactory.createEEnum();
		var sibling = ecoreFactory.createEEnumLiteral();
		eEnum.getELiterals().add(sibling);
		var eEnumLiteral = lib.addNewEEnumLiteralAsSibling(sibling, "test",
				lit -> {
					assertNotNull(lit.getEEnum());
					lit.setName("changed");
				});
		assertEquals("changed", eEnumLiteral.getName());
		assertSame(eEnumLiteral,
				eEnum.getELiterals().get(1));
	}

	@Test
	public void test_addEDataType() {
		var ePackage = ecoreFactory.createEPackage();
		var eEnum = ecoreFactory.createEDataType();
		lib.addEDataType(ePackage, eEnum);
		assertSame(eEnum,
				ePackage.getEClassifiers().get(0));
	}

	@Test
	public void test_addNewEDataType() {
		var ePackage = ecoreFactory.createEPackage();
		var eDataType = lib.addNewEDataType(ePackage, "test", "java.lang.String");
		assertEquals("test", eDataType.getName());
		assertSame(eDataType,
			ePackage.getEClassifiers().get(0));
		assertEquals("java.lang.String", eDataType.getInstanceTypeName());
		assertEquals("java.lang.String", eDataType.getInstanceClassName());
		assertEquals(String.class, eDataType.getInstanceClass());
	}

	@Test
	public void test_addNewEDataTypeWithInitializer() {
		var ePackage = ecoreFactory.createEPackage();
		var eDataType = lib.addNewEDataType(ePackage, "test", "java.lang.String",
				cl -> {
					assertNotNull(cl.getEPackage());
					cl.setName("changed");
				});
		assertEquals("changed", eDataType.getName());
		assertSame(eDataType,
			ePackage.getEClassifiers().get(0));
		assertEquals("java.lang.String", eDataType.getInstanceTypeName());
		assertEquals("java.lang.String", eDataType.getInstanceClassName());
		assertEquals(String.class, eDataType.getInstanceClass());
	}

	@Test
	public void test_addNewEDataTypeAsSibling() {
		var ePackage = ecoreFactory.createEPackage();
		var sibling = ecoreFactory.createEClass();
		ePackage.getEClassifiers().add(sibling);
		var eDataType = lib.addNewEDataTypeAsSibling(sibling, "test", "java.lang.String");
		assertEquals("test", eDataType.getName());
		assertSame(eDataType,
			ePackage.getEClassifiers().get(1));
		assertEquals("java.lang.String", eDataType.getInstanceTypeName());
		assertEquals("java.lang.String", eDataType.getInstanceClassName());
		assertEquals(String.class, eDataType.getInstanceClass());
	}

	@Test
	public void test_addNewEDataTypeAsSiblingWithInitializer() {
		var ePackage = ecoreFactory.createEPackage();
		var sibling = ecoreFactory.createEClass();
		ePackage.getEClassifiers().add(sibling);
		var eDataType = lib.addNewEDataTypeAsSibling(sibling, "test", "java.lang.String",
				cl -> {
					assertNotNull(cl.getEPackage());
					cl.setName("changed");
				});
		assertEquals("changed", eDataType.getName());
		assertSame(eDataType,
			ePackage.getEClassifiers().get(1));
		assertEquals("java.lang.String", eDataType.getInstanceTypeName());
		assertEquals("java.lang.String", eDataType.getInstanceClassName());
		assertEquals(String.class, eDataType.getInstanceClass());
	}

	@Test
	public void test_addEAttribute() {
		var eClass = ecoreFactory.createEClass();
		var eAttribute = ecoreFactory.createEAttribute();
		lib.addEAttribute(eClass, eAttribute);
		assertSame(eAttribute,
				eClass.getEStructuralFeatures().get(0));
	}

	@Test
	public void test_addEAttribute_alreadExisting() {
		var ePackage = ecoreFactory.createEPackage();
		ePackage.setName("TestPackage");
		var eClass = ecoreFactory.createEClass();
		eClass.setName("TestClass");
		lib.addEClass(ePackage, eClass);
		var eAttribute = ecoreFactory.createEAttribute();
		eAttribute.setName("TestAttribute");
		lib.addEAttribute(eClass, eAttribute);
		var eAttribute1 = ecoreFactory.createEAttribute();
		eAttribute1.setName("TestAttribute");
		assertThrowsIAE(
			() -> lib.addEAttribute(eClass, eAttribute1),
			eAttribute1,
			"TestPackage.TestClass already contains EAttribute TestPackage.TestClass.TestAttribute");
	}

	@Test
	public void test_addNewEAttribute() {
		var eClass = ecoreFactory.createEClass();
		var eAttribute =
				lib.addNewEAttribute(eClass, "test", ESTRING);
		assertEquals("test", eAttribute.getName());
		assertEquals(ESTRING, eAttribute.getEType());
		assertEquals(ESTRING, eAttribute.getEAttributeType());
		assertSame(eAttribute,
				eClass.getEStructuralFeatures().get(0));
	}

	@Test
	public void test_addNewEAttributeWithInitializer() {
		var eClass = ecoreFactory.createEClass();
		var eAttribute =
			lib.addNewEAttribute(eClass, "test", ESTRING,
				attr -> {
					assertNotNull(attr.getEContainingClass());
					attr.setName("changed");
				});
		assertEquals("changed", eAttribute.getName());
		assertEquals(ESTRING, eAttribute.getEType());
		assertEquals(ESTRING, eAttribute.getEAttributeType());
		assertSame(eAttribute,
				eClass.getEStructuralFeatures().get(0));
	}

	@Test
	public void test_addNewEAttributeAsSibling() {
		var eClass = ecoreFactory.createEClass();
		var sibling = ecoreFactory.createEAttribute();
		eClass.getEStructuralFeatures().add(sibling);
		var eAttribute =
				lib.addNewEAttributeAsSibling(sibling, "test", ESTRING);
		assertEquals("test", eAttribute.getName());
		assertEquals(ESTRING, eAttribute.getEType());
		assertEquals(ESTRING, eAttribute.getEAttributeType());
		assertSame(eAttribute,
				eClass.getEStructuralFeatures().get(1));
	}

	@Test
	public void test_addNewEAttributeAsSiblingWithInitializer() {
		var eClass = ecoreFactory.createEClass();
		var sibling = ecoreFactory.createEAttribute();
		eClass.getEStructuralFeatures().add(sibling);
		var eAttribute =
			lib.addNewEAttributeAsSibling(sibling, "test", ESTRING,
				attr -> {
					assertNotNull(attr.getEContainingClass());
					attr.setName("changed");
				});
		assertEquals("changed", eAttribute.getName());
		assertEquals(ESTRING, eAttribute.getEType());
		assertEquals(ESTRING, eAttribute.getEAttributeType());
		assertSame(eAttribute,
				eClass.getEStructuralFeatures().get(1));
	}

	@Test
	public void test_addEReference() {
		var eClass = ecoreFactory.createEClass();
		var eReference = ecoreFactory.createEReference();
		lib.addEReference(eClass, eReference);
		assertSame(eReference,
				eClass.getEStructuralFeatures().get(0));
	}

	@Test
	public void test_addNewEReference() {
		var eClass = ecoreFactory.createEClass();
		var eReference =
				lib.addNewEReference(eClass, "test", EOBJECT);
		assertEquals("test", eReference.getName());
		assertEquals(EOBJECT, eReference.getEType());
		assertEquals(EOBJECT, eReference.getEReferenceType());
		assertSame(eReference,
				eClass.getEStructuralFeatures().get(0));
	}

	@Test
	public void test_addNewEReferenceWithInitializer() {
		var eClass = ecoreFactory.createEClass();
		var eReference =
			lib.addNewEReference(eClass, "test", EOBJECT,
				ref -> {
					assertNotNull(ref.getEContainingClass());
					ref.setName("changed");
				});
		assertEquals("changed", eReference.getName());
		assertEquals(EOBJECT, eReference.getEType());
		assertEquals(EOBJECT, eReference.getEReferenceType());
		assertSame(eReference,
				eClass.getEStructuralFeatures().get(0));
	}

	@Test
	public void test_addNewEReferenceAsSibling() {
		var eClass = ecoreFactory.createEClass();
		var sibling = ecoreFactory.createEAttribute();
		eClass.getEStructuralFeatures().add(sibling);
		var eReference =
				lib.addNewEReferenceAsSibling(sibling, "test", EOBJECT);
		assertEquals("test", eReference.getName());
		assertEquals(EOBJECT, eReference.getEType());
		assertEquals(EOBJECT, eReference.getEReferenceType());
		assertSame(eReference,
				eClass.getEStructuralFeatures().get(1));
	}

	@Test
	public void test_addNewEReferenceAsSiblingWithInitializer() {
		var eClass = ecoreFactory.createEClass();
		var sibling = ecoreFactory.createEAttribute();
		eClass.getEStructuralFeatures().add(sibling);
		var eReference =
			lib.addNewEReferenceAsSibling(sibling, "test", EOBJECT,
				ref -> {
					assertNotNull(ref.getEContainingClass());
					ref.setName("changed");
				});
		assertEquals("changed", eReference.getName());
		assertEquals(EOBJECT, eReference.getEType());
		assertEquals(EOBJECT, eReference.getEReferenceType());
		assertSame(eReference,
				eClass.getEStructuralFeatures().get(1));
	}

	@Test
	public void test_addNewContainmentEReference() {
		var eClass = ecoreFactory.createEClass();
		var eReference =
				lib.addNewContainmentEReference(eClass, "test", EOBJECT);
		assertEquals("test", eReference.getName());
		assertEquals(EOBJECT, eReference.getEType());
		assertEquals(EOBJECT, eReference.getEReferenceType());
		assertThat(eReference.isContainment()).isTrue();
		assertSame(eReference,
				eClass.getEStructuralFeatures().get(0));
	}

	@Test
	public void test_addNewContainmentEReferenceWithInitializer() {
		var eClass = ecoreFactory.createEClass();
		var eReference =
			lib.addNewContainmentEReference(eClass, "test", EOBJECT,
				ref -> {
					assertNotNull(ref.getEContainingClass());
					ref.setName("changed");
				});
		assertEquals("changed", eReference.getName());
		assertEquals(EOBJECT, eReference.getEType());
		assertEquals(EOBJECT, eReference.getEReferenceType());
		assertThat(eReference.isContainment()).isTrue();
		assertSame(eReference,
				eClass.getEStructuralFeatures().get(0));
	}

	@Test
	public void test_addESuperType() {
		var superClass = ecoreFactory.createEClass();
		var subClass = ecoreFactory.createEClass();
		lib.addESuperType(subClass, superClass);
		assertThat(subClass.getESuperTypes()).containsExactly(superClass);
	}

	@Test
	public void test_addNewSubclass() {
		var ePackage = ecoreFactory.createEPackage();
		var superClass = ecoreFactory.createEClass();
		superClass.setName("Superclass");
		ePackage.getEClassifiers().add(superClass);
		var subClass = lib.addNewSubclass(superClass, "test");
		assertEquals("test", subClass.getName());
		assertThat(subClass.getESuperTypes())
			.containsExactly(superClass);
		assertThat(ePackage.getEClassifiers())
			.containsExactlyInAnyOrder(superClass, subClass);
	}

	@Test
	public void test_addNewSubclassWithInitializer() {
		var ePackage = ecoreFactory.createEPackage();
		var superClass = ecoreFactory.createEClass();
		superClass.setName("Superclass");
		ePackage.getEClassifiers().add(superClass);
		var subClass = lib.addNewSubclass(superClass, "test",
				cl -> {
					assertNotNull(cl.getEPackage());
					cl.setName("changed");
				});
		assertEquals("changed", subClass.getName());
		assertThat(subClass.getESuperTypes())
			.containsExactly(superClass);
		assertThat(ePackage.getEClassifiers())
			.containsExactlyInAnyOrder(superClass, subClass);
	}

	@Test
	public void test_removeESuperType() {
		var superClass = ecoreFactory.createEClass();
		var subClass = ecoreFactory.createEClass();
		subClass.getESuperTypes().add(superClass);
		assertThat(subClass.getESuperTypes()).containsExactly(superClass);
		lib.removeESuperType(subClass, superClass);
		assertThat(subClass.getESuperTypes()).isEmpty();
	}

	@Test
	public void test_addNewESubpackage() {
		var superPackage = ecoreFactory.createEPackage();
		var subPackage =
			lib.addNewESubpackage(superPackage, "newSubpackage", "prefix", "nsURI");
		assertThat(subPackage)
			.returns("newSubpackage", EPackage::getName)
			.returns("prefix", EPackage::getNsPrefix)
			.returns("nsURI", EPackage::getNsURI);
		assertThat(superPackage.getESubpackages()).containsOnly(subPackage);
		assertThat(subPackage.getESuperPackage()).isSameAs(superPackage);
	}

	@Test
	public void test_addNewESubpackageWithInitializer() {
		var superPackage = ecoreFactory.createEPackage();
		var subPackage =
			lib.addNewESubpackage(superPackage, "newSubpackage", "prefix", "nsURI",
				(EPackage p) -> p.setName("changed"));
		assertThat(subPackage)
			.returns("changed", EPackage::getName)
			.returns("prefix", EPackage::getNsPrefix)
			.returns("nsURI", EPackage::getNsURI);
		assertThat(superPackage.getESubpackages()).containsOnly(subPackage);
		assertThat(subPackage.getESuperPackage()).isSameAs(superPackage);
	}

	@Test
	public void test_copyTo() {
		var eClassSrc = ecoreFactory.createEClass();
		var eClassDest = ecoreFactory.createEClass();
		EStructuralFeature feature = ecoreFactory.createEAttribute();
		eClassSrc.getEStructuralFeatures().add(feature);
		// before
		assertThat(eClassDest.getEStructuralFeatures()).isEmpty();
		assertThat(eClassSrc.getEStructuralFeatures())
			.contains(feature);
		var copy = lib.copyTo(feature, eClassDest);
		// after
		assertThat(eClassSrc.getEStructuralFeatures())
			.contains(feature);
		assertThat(eClassDest.getEStructuralFeatures())
			.hasSize(1)
			.containsOnly(copy);
	}

	@Test
	public void test_copyTo_alreadyExisting() {
		var eClassSrc = ecoreFactory.createEClass();
		eClassSrc.setName("Src");
		EStructuralFeature feature = ecoreFactory.createEAttribute();
		feature.setName("Attr");
		eClassSrc.getEStructuralFeatures().add(feature);
		var eClassDest = ecoreFactory.createEClass();
		eClassDest.setName("Dest");
		EStructuralFeature existing = ecoreFactory.createEAttribute();
		existing.setName("Attr");
		eClassDest.getEStructuralFeatures().add(existing);
		// before
		assertThat(eClassDest.getEStructuralFeatures())
			.contains(existing);
		assertThat(eClassSrc.getEStructuralFeatures())
			.contains(feature);
		assertThrowsIAE(() -> lib.copyTo(feature, eClassDest),
			"Attr",
			"Dest already contains EAttribute Dest.Attr");
	}

	@Test
	public void test_copyToAs() {
		var eClassSrc = ecoreFactory.createEClass();
		var eClassDest = ecoreFactory.createEClass();
		EStructuralFeature feature = ecoreFactory.createEAttribute();
		eClassSrc.getEStructuralFeatures().add(feature);
		feature.setName("originalName");
		// before
		assertThat(eClassDest.getEStructuralFeatures()).isEmpty();
		assertThat(eClassSrc.getEStructuralFeatures())
			.contains(feature);
		var copy = lib.copyToAs(feature, eClassDest, "newName");
		// after
		assertThat(eClassSrc.getEStructuralFeatures())
			.containsOnly(feature)
			.first()
			.returns("originalName", ENamedElement::getName);
		assertThat(eClassDest.getEStructuralFeatures())
			.containsOnly(copy)
			.first()
			.returns("newName", ENamedElement::getName);
	}

	@Test
	public void test_copyToAsWithType() {
		var eClassSrc = ecoreFactory.createEClass();
		var eClassDest = ecoreFactory.createEClass();
		EStructuralFeature feature = ecoreFactory.createEAttribute();
		eClassSrc.getEStructuralFeatures().add(feature);
		feature.setName("originalName");
		// before
		assertThat(eClassDest.getEStructuralFeatures()).isEmpty();
		assertThat(eClassSrc.getEStructuralFeatures())
			.contains(feature);
		var copy = lib.copyToAs(feature, eClassDest, "newName",
				EOBJECT);
		// after
		assertThat(eClassSrc.getEStructuralFeatures())
			.containsOnly(feature)
			.first()
			.returns("originalName", ENamedElement::getName)
			.returns(null, ETypedElement::getEType);
		assertThat(eClassDest.getEStructuralFeatures())
			.containsOnly(copy)
			.first()
			.returns("newName", ENamedElement::getName)
			.returns(EOBJECT, ETypedElement::getEType);
	}

	@Test
	public void test_copyToAsForClassifier() {
		var ePackage = ecoreFactory.createEPackage();
		var eClassSrc = ecoreFactory.createEClass();
		eClassSrc.setName("originalName");
		ePackage.getEClassifiers().add(eClassSrc);
		// before
		assertThat(ePackage.getEClassifiers())
			.containsExactly(eClassSrc);
		var copy = lib.copyToAs(eClassSrc, ePackage, "newName");
		// after
		assertThat(ePackage.getEClassifiers())
			.containsExactly(eClassSrc, copy);
		assertEquals("newName", copy.getName());
	}

	@Test
	public void test_copyAllTo() {
		var eClassSrc = ecoreFactory.createEClass();
		var eClassDest = ecoreFactory.createEClass();
		EStructuralFeature feature1 = ecoreFactory.createEAttribute();
		eClassSrc.getEStructuralFeatures().add(feature1);
		EStructuralFeature feature2 = ecoreFactory.createEReference();
		eClassSrc.getEStructuralFeatures().add(feature2);
		// before
		assertThat(eClassDest.getEStructuralFeatures()).isEmpty();
		assertThat(eClassSrc.getEStructuralFeatures())
			.contains(feature1, feature2);
		lib.copyAllTo(Arrays.asList(feature1, feature2), eClassDest);
		// after
		assertThat(eClassSrc.getEStructuralFeatures())
			.contains(feature1, feature2);
		assertThat(eClassDest.getEStructuralFeatures())
			.hasSize(2)
			.allMatch(f -> new EqualityHelper().equals(f, feature1)
						|| new EqualityHelper().equals(f, feature2));
	}

	@Test
	public void test_copyAllTo_alreadyExisting() {
		var eClassSrc = ecoreFactory.createEClass();
		eClassSrc.setName("Src");
		EStructuralFeature feature = ecoreFactory.createEAttribute();
		feature.setName("Attr");
		eClassSrc.getEStructuralFeatures().add(feature);
		var eClassDest = ecoreFactory.createEClass();
		eClassDest.setName("Dest");
		EStructuralFeature existing = ecoreFactory.createEAttribute();
		existing.setName("Attr");
		eClassDest.getEStructuralFeatures().add(existing);
		// before
		assertThat(eClassDest.getEStructuralFeatures())
			.contains(existing);
		assertThat(eClassSrc.getEStructuralFeatures())
			.contains(feature);
		assertThrowsIAE(() -> lib.copyAllTo(List.of(feature), eClassDest),
			"Attr",
			"Dest already contains EAttribute Dest.Attr");
	}

	@Test
	public void test_moveTo() {
		var eClassSrc = ecoreFactory.createEClass();
		var eClassDest = ecoreFactory.createEClass();
		EStructuralFeature feature = ecoreFactory.createEAttribute();
		eClassSrc.getEStructuralFeatures().add(feature);
		// before
		assertThat(eClassDest.getEStructuralFeatures()).isEmpty();
		assertThat(eClassSrc.getEStructuralFeatures())
			.contains(feature);
		lib.moveTo(feature, eClassDest);
		// after
		assertThat(eClassSrc.getEStructuralFeatures()).isEmpty();
		assertThat(eClassDest.getEStructuralFeatures())
			.contains(feature);
	}

	@Test
	public void test_moveAllTo() {
		var eClassSrc = ecoreFactory.createEClass();
		var eClassDest = ecoreFactory.createEClass();
		EStructuralFeature feature1 = ecoreFactory.createEAttribute();
		eClassSrc.getEStructuralFeatures().add(feature1);
		EStructuralFeature feature2 = ecoreFactory.createEReference();
		eClassSrc.getEStructuralFeatures().add(feature2);
		// before
		assertThat(eClassDest.getEStructuralFeatures()).isEmpty();
		assertThat(eClassSrc.getEStructuralFeatures())
			.contains(feature1, feature2);
		lib.moveAllTo(Arrays.asList(feature1, feature2), eClassDest);
		// after
		assertThat(eClassSrc.getEStructuralFeatures()).isEmpty();
		assertThat(eClassDest.getEStructuralFeatures())
			.contains(feature1, feature2);
	}

	@Test
	public void test_createOpposite() {
		var c1 = ecoreFactory.createEClass();
		var c2 = ecoreFactory.createEClass();
		var c1Ref = ecoreFactory.createEReference();
		c1.getEStructuralFeatures().add(c1Ref);
		var c2Ref = lib.createOpposite(c1Ref, "c2Ref", c2);
		assertThat(c1Ref.getEOpposite()).isNotNull();
		assertThat(c2Ref.getEOpposite()).isNotNull();
		assertThat(c1Ref.getEOpposite()).isSameAs(c2Ref);
		assertThat(c2Ref.getEOpposite()).isSameAs(c1Ref);
		assertThat(c1Ref.getEReferenceType()).isSameAs(c2);
		assertThat(c2Ref.getEReferenceType()).isSameAs(c1);
		assertThat(c1Ref.getEOpposite().getEReferenceType()).isSameAs(c1);
		assertThat(c2Ref.getEOpposite().getEReferenceType()).isSameAs(c2);
		assertThat(c1.getEReferences()).containsExactly(c1Ref);
		assertThat(c2.getEReferences()).containsExactly(c2Ref);
		// test it with an existing opposite
		var c3 = ecoreFactory.createEClass();
		var c3Ref = lib.createOpposite(c1Ref, "c3Ref", c3);
		assertThat(c1Ref.getEOpposite()).isNotNull();
		assertThat(c3Ref.getEOpposite()).isNotNull();
		assertThat(c1Ref.getEOpposite()).isSameAs(c3Ref);
		assertThat(c3Ref.getEOpposite()).isSameAs(c1Ref);
		assertThat(c1Ref.getEReferenceType()).isSameAs(c3);
		assertThat(c3Ref.getEReferenceType()).isSameAs(c1);
		assertThat(c1Ref.getEOpposite().getEReferenceType()).isSameAs(c1);
		assertThat(c3Ref.getEOpposite().getEReferenceType()).isSameAs(c3);
		assertThat(c2Ref.getEOpposite()).isNull();
		assertThat(c1.getEReferences()).containsExactly(c1Ref);
		assertThat(c2.getEReferences()).containsExactly(c2Ref);
		assertThat(c3.getEReferences()).containsExactly(c3Ref);
	}

	@Test
	public void modelMigrationUnchanged() throws Exception {
		var subdir = "simpleTestData/";
		var engine = setupEngine(
			subdir,
			of("My.ecore"),
			of("MyRoot.xmi", "MyClass.xmi"),
			EdeltaDefaultRuntime::new
		);
		copyModelsSaveAndAssertOutputs(
			engine,
			subdir,
			of("My.ecore"),
			of("MyRoot.xmi", "MyClass.xmi")
		);
	}

	@Test
	public void modelMigrationRenamed() throws Exception {
		var subdir = "simpleTestData/";
		var engine = setupEngine(
			subdir,
			of("My.ecore"),
			of("MyRoot.xmi", "MyClass.xmi"),
			other -> new EdeltaDefaultRuntime(other) {
				@Override
				protected void doExecute() {
					// refactoring of Ecore
					stdLib.getEClass("mypackage", "MyClass")
					.setName("MyClassRenamed");
					stdLib.getEClass("mypackage", "MyRoot")
					.setName("MyRootRenamed");
				}
			}
		);
		copyModelsSaveAndAssertOutputs(
			engine,
			"renamedClass/",
			of("My.ecore"),
			of("MyRoot.xmi", "MyClass.xmi")
		);
	}

	@Test
	public void modelMigrationCopyToAs() throws Exception {
		var subdir = "simpleTestData/";
		var engine = setupEngine(
			subdir,
			of("My.ecore"),
			of("MyRoot.xmi", "MyClass.xmi"),
			other -> new EdeltaDefaultRuntime(other) {
				@Override
				protected void doExecute() {
					var a = stdLib.getEAttribute("mypackage", "MyClass",
							"myClassStringAttribute");
					var myRoot = stdLib.getEClass("mypackage", "MyRoot");
					var copied = stdLib.copyToAs(a, myRoot, "myRootStringAttribute");
					// custom migration rule setting the default value for the copied feature
					modelMigration(migrator -> {
						migrator.createInstanceRule(
							migrator.isRelatedTo(myRoot),
							oldObj ->
								EdeltaEcoreUtil.createInstance(myRoot, o -> {
									o.eSet(copied, "default value");
								})
							);
					});
				}
			}
		);
		copyModelsSaveAndAssertOutputs(
			engine,
			"copyToAs/",
			of("My.ecore"),
			of("MyRoot.xmi", "MyClass.xmi")
		);
	}

	@Test
	public void changeAttributeType() throws Exception {
		var subdir = "changedAttributeType/";
		var engine = setupEngine(
			subdir,
			of("My.ecore"),
			of("MyClass.xmi", "MyClass2.xmi", "MyClass3.xmi"),
			other -> new EdeltaDefaultRuntime(other) {
				@Override
				protected void doExecute() {
					var attribute = stdLib.getEAttribute("mypackage", "MyClass", "myAttribute");
					stdLib.changeType(attribute, EINT, val -> {
						try {
							return Integer.parseInt(val.toString());
						} catch (NumberFormatException e) {
							return -1;
						}
					});
				}
			}
		);
		copyModelsSaveAndAssertOutputs(
			engine,
			subdir,
			of("My.ecore"),
			of("MyClass.xmi", "MyClass2.xmi", "MyClass3.xmi")
		);
	}

	/**
	 * Note that, since we handle multiplicity automatically, the code of the
	 * refactoring and transformer is just the same as the previous test.
	 *
	 * @throws Exception
	 */
	@Test
	public void changeAttributeTypeMultiple() throws Exception {
		var subdir = "changedMultiAttributeType/";
		var engine = setupEngine(
			subdir,
			of("My.ecore"),
			of("MyClass.xmi"),
			other -> new EdeltaDefaultRuntime(other) {
				@Override
				protected void doExecute() {
					var attribute = stdLib.getEAttribute("mypackage", "MyClass", "myAttribute");
					stdLib.changeType(attribute, EINT, val -> {
						try {
							return Integer.parseInt(val.toString());
						} catch (NumberFormatException e) {
							return -1;
						}
					});
				}
			}
		);
		copyModelsSaveAndAssertOutputs(
			engine,
			subdir,
			of("My.ecore"),
			of("MyClass.xmi")
		);
	}

	/**
	 * Since this is meant as a simulation of what a user would do in the
	 * Edelta DSL, we access {@link ENamedElement}s with strings, since
	 * that's how "ecoreref()" expressions are translated by the Edelta DSL
	 * compiler.
	 *
	 * @throws Exception
	 */
	@Test
	public void changeReferenceTypeContainment() throws Exception {
		var subdir = "changeReferenceTypeContainment/";
		var engine = setupEngine(
			subdir,
			of("PersonList.ecore"),
			of("List.xmi"),
			other -> new EdeltaDefaultRuntime(other) {
				@Override
				protected void doExecute() {
					var reference = stdLib.getEReference("PersonList", "Person", "firstName");
					// first add the new class similar to NameElement
					var nameElement = stdLib.getEClass("PersonList", "NameElement");
					var otherNameElement = stdLib.addNewEClassAsSibling(
						nameElement,
						"OtherNameElement",
						c -> {
							stdLib.addNewEAttribute(c,
								"otherNameElementValue", ESTRING);
						});
					// the attribute of the original reference type
					var nameElementFeature = stdLib.getEAttribute
						("PersonList", "NameElement", "nameElementValue");
					// the attribute we just added to the new class
					var otherNameElementFeature = stdLib.getEAttribute
						("PersonList", "OtherNameElement", "otherNameElementValue");
					// change the reference type
					stdLib.changeType(reference, otherNameElement,
						// and provide the model migration for the changed reference
						oldReferredObject ->
						// oldReferredObject is part of the model being migrated
						// so it's safe to use features retrieved above,
						// like nameElementFeature
						createInstance(otherNameElement,
							// we refer to a new object of type OtherNameElement
							newReferredObject ->
							// copying its value from the original referred
							// Object of type NameElement
							newReferredObject.eSet(otherNameElementFeature,
								oldReferredObject.eGet(nameElementFeature)
							)
						)
						// since the original reference Person.firstName was a
						// containment reference, just referring to the newly
						// created object will add it to the model
					);
				}
			}
		);
		copyModelsSaveAndAssertOutputs(
			engine,
			subdir,
			of("PersonList.ecore"),
			of("List.xmi")
		);
	}

	/**
	 * Since this is meant as a simulation of what a user would do in the
	 * Edelta DSL, we access {@link ENamedElement}s with strings, since
	 * that's how "ecoreref()" expressions are translated by the Edelta DSL
	 * compiler.
	 *
	 * @throws Exception
	 */
	@Test
	public void changeReferenceTypeNonContainment() throws Exception {
		var subdir = "changeReferenceTypeNonContainment/";
		var engine = setupEngine(
			subdir,
			of("PersonList.ecore"),
			of("List.xmi"),
			other -> new EdeltaDefaultRuntime(other) {
				@Override
				protected void doExecute() {
					var reference = stdLib.getEReference("PersonList", "Person", "firstName");
					// first add the new class similar to NameElement
					var nameElement = stdLib.getEClass("PersonList", "NameElement");
					// the attribute of the original reference type NameElement
					var nameElementFeature = stdLib.getEAttribute
						("PersonList", "NameElement", "nameElementValue");
					var otherNameElement = stdLib.addNewEClassAsSibling(
						nameElement,
						"OtherNameElement");
					// the attribute we copy and add to the new class
					// of the new type OtherNameElement
					var otherNameElementFeature = stdLib.copyToAs
						(nameElementFeature, otherNameElement, "otherNameElementValue");
					// since the references are non containment, we need to add
					// a containment reference for the objects of the new type OtherNameElement
					// somewhere, e.g., in the List class
					var otherNameElements = stdLib.copyToAs(
						stdLib.getEReference("PersonList", "List", "nameElements"),
						stdLib.getEClass("PersonList", "List"),
						"otherNameElements",
						otherNameElement);

					// change the reference type
					stdLib.changeType(reference, otherNameElement,
						// and provide the model migration for the changed reference
						oldReferredObject -> {
							// oldReferredObject is part of the model being migrated
							// so it's safe to use features retrieved above,
							// like nameElementFeature

							// retrieve the copied List object
							// remember also the oldReferredObject is part
							// of the (new) model, the one being migrated
							var listObject = oldReferredObject.eContainer();
							var otherNameElementsCollection =
								getValueAsList(listObject, otherNameElements);
							return createInstance(otherNameElement,
								// we refer to a new object of type OtherNameElement
								newReferredObject -> {
									// copying its value from the original referred
									// Object of type NameElement
									newReferredObject.eSet(otherNameElementFeature,
										oldReferredObject.eGet(nameElementFeature)
									);
									// differently from the previous test
									// it's now responsibility of the caller to store the new
									// object in a container.
									// Since the original reference Person.firstName was NOT
									// containment reference, just referring to the newly
									// created object will NOT add it to the model
									otherNameElementsCollection.add(newReferredObject);
								}
							);
						}
					);
				}
			}
		);
		copyModelsSaveAndAssertOutputs(
			engine,
			subdir,
			of("PersonList.ecore"),
			of("List.xmi")
		);
	}

	/**
	 * Since this is meant as a simulation of what a user would do in the
	 * Edelta DSL, we access {@link ENamedElement}s with strings, since
	 * that's how "ecoreref()" expressions are translated by the Edelta DSL
	 * compiler.
	 *
	 * @throws Exception
	 */
	@Test
	public void changeReferenceTypeNonContainmentShared() throws Exception {
		var subdir = "changeReferenceTypeNonContainmentShared/";
		var engine = setupEngine(
			subdir,
			of("PersonList.ecore"),
			of("List.xmi", "List1.xmi", "List2.xmi"),
			other -> new EdeltaDefaultRuntime(other) {
				@Override
				protected void doExecute() {
					var reference = stdLib.getEReference("PersonList", "Person", "firstName");
					// first add the new class similar to NameElement
					var nameElement = stdLib.getEClass("PersonList", "NameElement");
					// the attribute of the original reference type NameElement
					var nameElementFeature = stdLib.getEAttribute
						("PersonList", "NameElement", "nameElementValue");
					var otherNameElement = stdLib.addNewEClassAsSibling(
						nameElement,
						"OtherNameElement");
					// the attribute we copy and add to the new class
					// of the new type OtherNameElement
					var otherNameElementFeature = stdLib.copyToAs
						(nameElementFeature, otherNameElement, "otherNameElementValue");
					// since the references are non containment, we need to add
					// a containment reference for the objects of the new type OtherNameElement
					// somewhere, e.g., in the List class
					var otherNameElements = stdLib.copyToAs(
						stdLib.getEReference("PersonList", "List", "nameElements"),
						stdLib.getEClass("PersonList", "List"),
						"otherNameElements",
						otherNameElement);

					// this is useful in the model migration rule to avoid
					// creating two many new referred objects, since, in this
					// test, referred objects were and are meant to be share
					// this will also be useful to remove previously referred
					// objects since they will not be referred anymore
					var referredMap = new HashMap<EObject, EObject>();
					// note that we use the same maps for migrating all the models
					// and that's correct, since, like in this test, objects can
					// refer to objects of other model XMI files.

					// change the reference type
					stdLib.changeType(reference, otherNameElement,
						// and provide the model migration for the changed reference
						oldReferredObject -> {
							// oldReferredObject is part of the model being migrated
							// so it's safe to use features retrieved above,
							// like nameElementFeature

							var newReferredObject = referredMap.computeIfAbsent(oldReferredObject,
								oldReferred -> {
								// ... avoiding duplicates like in this case
								// where references are meant to be shared

								// retrieve the copied List object
								// remember also the oldReferredObject is part
								// of the (new) model, the one migrateds
								var listObject = oldReferredObject.eContainer();
								var otherNameElementsCollection =
									getValueAsList(listObject, otherNameElements);
								// differently from the first test (and like the previous one)
								// it's now responsibility of the caller to store the new
								// object in a container.
								// Since the original reference Person.firstName was NOT
								// containment reference, just referring to the newly
								// created object will NOT add it to the model
								return EdeltaEcoreUtil.createInstance(otherNameElement,
									otherNameElementsCollection::add);
								}
							);

							// we refer to a new object of type OtherNameElement
							// copying its value from the original referred
							// Object of type NameElement
							newReferredObject.eSet(otherNameElementFeature,
								oldReferredObject.eGet(nameElementFeature)
							);

							return newReferredObject;
						},
						// old shared referred objects can be removed now
						() -> EcoreUtil.removeAll(referredMap.keySet())
					);
				}
			}
		);
		copyModelsSaveAndAssertOutputs(
			engine,
			subdir,
			of("PersonList.ecore"),
			of("List.xmi", "List1.xmi", "List2.xmi")
		);
	}

	@Test
	public void changeToSingle() throws Exception {
		var subdir = "toUpperCaseStringAttributesMultiple/";
		var ecores = of("My.ecore");
		var models = of("MyClass.xmi", "MyClass2.xmi", "MyClass3.xmi");

		var engine = setupEngine(
			subdir,
			ecores,
			models,
			other -> new EdeltaDefaultRuntime(other) {
				@Override
				protected void doExecute() {
					EStructuralFeature feature = stdLib.getEAttribute(
						"mypackage", "MyClass", "myAttribute");
					stdLib.changeToSingle(feature);
				}
			}
		);
		copyModelsSaveAndAssertOutputs(
			engine,
			"makeSingle/",
			ecores,
			models
		);
	}

	@Test
	public void changeToMultipleTo2() throws Exception {
		var subdir = "toUpperCaseStringAttributesMultiple/";
		var ecores = of("My.ecore");
		var models = of("MyClass.xmi", "MyClass2.xmi", "MyClass3.xmi");

		var engine = setupEngine(
			subdir,
			ecores,
			models,
			other -> new EdeltaDefaultRuntime(other) {
				@Override
				protected void doExecute() {
					EStructuralFeature feature = stdLib.getEAttribute(
						"mypackage", "MyClass", "myAttribute");
					stdLib.changeToMultiple(feature, 2);
				}
			}
		);
		copyModelsSaveAndAssertOutputs(
			engine,
			"makeMultipleTo2/",
			ecores,
			models
		);
	}

	@Test
	public void changeToMultiple() throws Exception {
		var subdir = "toUpperCaseStringAttributes/";
		var ecores = of("My.ecore");
		var models = of("MyClass.xmi", "MyClass2.xmi", "MyClass3.xmi");

		var engine = setupEngine(
			subdir,
			ecores,
			models,
			other -> new EdeltaDefaultRuntime(other) {
				@Override
				protected void doExecute() {
					EStructuralFeature feature = stdLib.getEAttribute(
						"mypackage", "MyClass", "myAttribute");
					stdLib.changeToMultiple(feature);
				}
			}
		);
		copyModelsSaveAndAssertOutputs(
			engine,
			"makeMultiple/",
			ecores,
			models
		);
	}

	private Resource loadTestEcore(String ecoreFile) {
		return modelManager.loadEcoreFile(TESTECORES+ecoreFile);
	}

	private IllegalArgumentException assertThrowsIAE(ThrowingRunnable executable,
			ENamedElement element,
			String expectedError) {
		return assertThrowsIAE(executable, element.getName(), expectedError);
	}

	private IllegalArgumentException assertThrowsIAE(ThrowingRunnable executable,
			String elementName,
			String expectedError) {
		var assertThrows = assertThrows(IllegalArgumentException.class,
				executable);
		assertEquals(expectedError, assertThrows.getMessage());
		verify(issuePresenter).showError(
			argThat(a -> a.getName().equals(elementName)),
			argThat(a -> a.equals(expectedError)));
		return assertThrows;
	}

	private void copyModelsSaveAndAssertOutputs(
			EdeltaEngine engine,
			String outputdir,
			Collection<String> ecoreFiles,
			Collection<String> modelFiles
		) throws Exception {
		engine.execute();
		var output = OUTPUT + outputdir;
		engine.save(output);
		ecoreFiles.forEach
			(fileName ->
				assertGeneratedFiles(fileName, outputdir, output, fileName));
		modelFiles.forEach
			(fileName ->
				assertGeneratedFiles(fileName, outputdir, output, fileName));
	}

	private void assertGeneratedFiles(String message, String subdir, String outputDir, String fileName) {
		try {
			assertFilesAreEquals(
				message,
				EXPECTATIONS + subdir + fileName,
				outputDir + fileName);
		} catch (IOException e) {
			e.printStackTrace();
			fail(e.getClass().getName() + ": " + e.getMessage());
		}
	}
}
