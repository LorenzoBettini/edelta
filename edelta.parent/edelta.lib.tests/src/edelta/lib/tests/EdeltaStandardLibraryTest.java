/**
 * 
 */
package edelta.lib.tests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.emf.ecore.EcorePackage.Literals.EOBJECT;
import static org.eclipse.emf.ecore.EcorePackage.Literals.ESTRING;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.Arrays;
import java.util.List;

import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EEnum;
import org.eclipse.emf.ecore.EEnumLiteral;
import org.eclipse.emf.ecore.ENamedElement;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.ETypedElement;
import org.eclipse.emf.ecore.EcoreFactory;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.util.EcoreUtil.EqualityHelper;
import org.junit.Before;
import org.junit.Test;
import org.junit.function.ThrowingRunnable;

import edelta.lib.EdeltaRuntime;
import edelta.lib.EdeltaIssuePresenter;
import edelta.lib.EdeltaModelManager;
import edelta.lib.EdeltaStandardLibrary;

/**
 * Library functions for manipulating an Ecore model.
 * 
 * @author Lorenzo Bettini
 *
 */
public class EdeltaStandardLibraryTest {

	private static final String MYPACKAGE = "mypackage";
	private static final String MYOTHERPACKAGE = "myotherpackage";
	private static final String MY2_ECORE = "My2.ecore";
	private static final String MY_ECORE = "My.ecore";
	private static final String TESTECORES = "testecores/";

	private static EcoreFactory ecoreFactory = EcoreFactory.eINSTANCE;

	private EdeltaStandardLibrary lib;

	private EdeltaIssuePresenter issuePresenter;

	private EdeltaModelManager modelManager;

	@Before
	public void setup() {
		issuePresenter = mock(EdeltaIssuePresenter.class);
		modelManager = new EdeltaModelManager();
		lib = new EdeltaStandardLibrary(modelManager);
		lib.setIssuePresenter(issuePresenter);
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
		EPackage ePackage = other.getEPackage(MYPACKAGE);
		assertEquals(MYPACKAGE, ePackage.getName());
		assertNotNull(ePackage);
		assertNotNull(other.getEPackage(MYOTHERPACKAGE));
		assertNull(other.getEPackage("foo"));
	}

	@Test
	public void test_addEClass() {
		EPackage ePackage = ecoreFactory.createEPackage();
		EClass eClass = ecoreFactory.createEClass();
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
		EPackage ePackage = ecoreFactory.createEPackage();
		EClass eClass = lib.addNewEClass(ePackage, "test");
		assertEquals("test", eClass.getName());
		assertSame(eClass,
			ePackage.getEClassifiers().get(0));
	}

	@Test
	public void test_addNewEClassWithInitializer() {
		EPackage ePackage = ecoreFactory.createEPackage();
		EClass eClass = lib.addNewEClass(ePackage, "test",
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
		EPackage ePackage = ecoreFactory.createEPackage();
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
		EPackage ePackage = ecoreFactory.createEPackage();
		EClass eClass = lib.addNewAbstractEClass(ePackage, "test");
		assertEquals("test", eClass.getName());
		assertThat(eClass.isAbstract())
			.isTrue();
		assertSame(eClass,
			ePackage.getEClassifiers().get(0));
	}

	@Test
	public void test_addNewAbstractEClassWithInitializer() {
		EPackage ePackage = ecoreFactory.createEPackage();
		EClass eClass = lib.addNewAbstractEClass(ePackage, "test",
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
		EPackage ePackage = ecoreFactory.createEPackage();
		EEnum eEnum = ecoreFactory.createEEnum();
		lib.addEEnum(ePackage, eEnum);
		assertSame(eEnum,
				ePackage.getEClassifiers().get(0));
	}

	@Test
	public void test_addNewEEnum() {
		EPackage ePackage = ecoreFactory.createEPackage();
		EEnum eEnum = lib.addNewEEnum(ePackage, "test");
		assertEquals("test", eEnum.getName());
		assertSame(eEnum,
			ePackage.getEClassifiers().get(0));
	}

	@Test
	public void test_addNewEEnumWithInitializer() {
		EPackage ePackage = ecoreFactory.createEPackage();
		EEnum eEnum = lib.addNewEEnum(ePackage, "test",
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
		EPackage ePackage = ecoreFactory.createEPackage();
		var sibling = ecoreFactory.createEEnum();
		ePackage.getEClassifiers().add(sibling);
		EEnum eEnum = lib.addNewEEnumAsSibling(sibling, "test");
		assertEquals("test", eEnum.getName());
		assertSame(eEnum,
			ePackage.getEClassifiers().get(1));
	}

	@Test
	public void test_addNewEEnumAsSiblingWithInitializer() {
		EPackage ePackage = ecoreFactory.createEPackage();
		var sibling = ecoreFactory.createEEnum();
		ePackage.getEClassifiers().add(sibling);
		EEnum eEnum = lib.addNewEEnumAsSibling(sibling, "test",
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
		EEnum eEnum = ecoreFactory.createEEnum();
		EEnumLiteral eEnumLiteral = ecoreFactory.createEEnumLiteral();
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
		EEnum eEnum = ecoreFactory.createEEnum();
		EEnumLiteral eEnumLiteral =
				lib.addNewEEnumLiteral(eEnum, "test");
		assertEquals("test", eEnumLiteral.getName());
		assertSame(eEnumLiteral,
				eEnum.getELiterals().get(0));
	}

	@Test
	public void test_addNewEEnumLiteralWithInitializer() {
		EEnum eEnum = ecoreFactory.createEEnum();
		EEnumLiteral eEnumLiteral = lib.addNewEEnumLiteral(eEnum, "test",
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
		EEnum eEnum = ecoreFactory.createEEnum();
		var sibling = ecoreFactory.createEEnumLiteral();
		eEnum.getELiterals().add(sibling);
		EEnumLiteral eEnumLiteral =
				lib.addNewEEnumLiteralAsSibling(sibling, "test");
		assertEquals("test", eEnumLiteral.getName());
		assertSame(eEnumLiteral,
				eEnum.getELiterals().get(1));
	}

	@Test
	public void test_addNewEEnumLiteralAsSiblingWithInitializer() {
		EEnum eEnum = ecoreFactory.createEEnum();
		var sibling = ecoreFactory.createEEnumLiteral();
		eEnum.getELiterals().add(sibling);
		EEnumLiteral eEnumLiteral = lib.addNewEEnumLiteralAsSibling(sibling, "test",
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
		EPackage ePackage = ecoreFactory.createEPackage();
		EDataType eEnum = ecoreFactory.createEDataType();
		lib.addEDataType(ePackage, eEnum);
		assertSame(eEnum,
				ePackage.getEClassifiers().get(0));
	}

	@Test
	public void test_addNewEDataType() {
		EPackage ePackage = ecoreFactory.createEPackage();
		EDataType eDataType = lib.addNewEDataType(ePackage, "test", "java.lang.String");
		assertEquals("test", eDataType.getName());
		assertSame(eDataType,
			ePackage.getEClassifiers().get(0));
		assertEquals("java.lang.String", eDataType.getInstanceTypeName());
		assertEquals("java.lang.String", eDataType.getInstanceClassName());
		assertEquals(String.class, eDataType.getInstanceClass());
	}

	@Test
	public void test_addNewEDataTypeWithInitializer() {
		EPackage ePackage = ecoreFactory.createEPackage();
		EDataType eDataType = lib.addNewEDataType(ePackage, "test", "java.lang.String",
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
		EPackage ePackage = ecoreFactory.createEPackage();
		var sibling = ecoreFactory.createEClass();
		ePackage.getEClassifiers().add(sibling);
		EDataType eDataType = lib.addNewEDataTypeAsSibling(sibling, "test", "java.lang.String");
		assertEquals("test", eDataType.getName());
		assertSame(eDataType,
			ePackage.getEClassifiers().get(1));
		assertEquals("java.lang.String", eDataType.getInstanceTypeName());
		assertEquals("java.lang.String", eDataType.getInstanceClassName());
		assertEquals(String.class, eDataType.getInstanceClass());
	}

	@Test
	public void test_addNewEDataTypeAsSiblingWithInitializer() {
		EPackage ePackage = ecoreFactory.createEPackage();
		var sibling = ecoreFactory.createEClass();
		ePackage.getEClassifiers().add(sibling);
		EDataType eDataType = lib.addNewEDataTypeAsSibling(sibling, "test", "java.lang.String",
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
		EClass eClass = ecoreFactory.createEClass();
		EAttribute eAttribute = ecoreFactory.createEAttribute();
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
		EClass eClass = ecoreFactory.createEClass();
		EAttribute eAttribute =
				lib.addNewEAttribute(eClass, "test", ESTRING);
		assertEquals("test", eAttribute.getName());
		assertEquals(ESTRING, eAttribute.getEType());
		assertEquals(ESTRING, eAttribute.getEAttributeType());
		assertSame(eAttribute,
				eClass.getEStructuralFeatures().get(0));
	}

	@Test
	public void test_addNewEAttributeWithInitializer() {
		EClass eClass = ecoreFactory.createEClass();
		EAttribute eAttribute =
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
		EClass eClass = ecoreFactory.createEClass();
		var sibling = ecoreFactory.createEAttribute();
		eClass.getEStructuralFeatures().add(sibling);
		EAttribute eAttribute =
				lib.addNewEAttributeAsSibling(sibling, "test", ESTRING);
		assertEquals("test", eAttribute.getName());
		assertEquals(ESTRING, eAttribute.getEType());
		assertEquals(ESTRING, eAttribute.getEAttributeType());
		assertSame(eAttribute,
				eClass.getEStructuralFeatures().get(1));
	}

	@Test
	public void test_addNewEAttributeAsSiblingWithInitializer() {
		EClass eClass = ecoreFactory.createEClass();
		var sibling = ecoreFactory.createEAttribute();
		eClass.getEStructuralFeatures().add(sibling);
		EAttribute eAttribute =
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
		EClass eClass = ecoreFactory.createEClass();
		EReference eReference = ecoreFactory.createEReference();
		lib.addEReference(eClass, eReference);
		assertSame(eReference,
				eClass.getEStructuralFeatures().get(0));
	}

	@Test
	public void test_addNewEReference() {
		EClass eClass = ecoreFactory.createEClass();
		EReference eReference =
				lib.addNewEReference(eClass, "test", EOBJECT);
		assertEquals("test", eReference.getName());
		assertEquals(EOBJECT, eReference.getEType());
		assertEquals(EOBJECT, eReference.getEReferenceType());
		assertSame(eReference,
				eClass.getEStructuralFeatures().get(0));
	}

	@Test
	public void test_addNewEReferenceWithInitializer() {
		EClass eClass = ecoreFactory.createEClass();
		EReference eReference =
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
		EClass eClass = ecoreFactory.createEClass();
		var sibling = ecoreFactory.createEAttribute();
		eClass.getEStructuralFeatures().add(sibling);
		EReference eReference =
				lib.addNewEReferenceAsSibling(sibling, "test", EOBJECT);
		assertEquals("test", eReference.getName());
		assertEquals(EOBJECT, eReference.getEType());
		assertEquals(EOBJECT, eReference.getEReferenceType());
		assertSame(eReference,
				eClass.getEStructuralFeatures().get(1));
	}

	@Test
	public void test_addNewEReferenceAsSiblingWithInitializer() {
		EClass eClass = ecoreFactory.createEClass();
		var sibling = ecoreFactory.createEAttribute();
		eClass.getEStructuralFeatures().add(sibling);
		EReference eReference =
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
		EClass eClass = ecoreFactory.createEClass();
		EReference eReference =
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
		EClass eClass = ecoreFactory.createEClass();
		EReference eReference =
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
		EClass superClass = ecoreFactory.createEClass();
		EClass subClass = ecoreFactory.createEClass();
		lib.addESuperType(subClass, superClass);
		assertThat(subClass.getESuperTypes()).containsExactly(superClass);
	}

	@Test
	public void test_addNewSubclass() {
		EPackage ePackage = ecoreFactory.createEPackage();
		EClass superClass = ecoreFactory.createEClass();
		superClass.setName("Superclass");
		ePackage.getEClassifiers().add(superClass);
		EClass subClass = lib.addNewSubclass(superClass, "test");
		assertEquals("test", subClass.getName());
		assertThat(subClass.getESuperTypes())
			.containsExactly(superClass);
		assertThat(ePackage.getEClassifiers())
			.containsExactlyInAnyOrder(superClass, subClass);
	}

	@Test
	public void test_addNewSubclassWithInitializer() {
		EPackage ePackage = ecoreFactory.createEPackage();
		EClass superClass = ecoreFactory.createEClass();
		superClass.setName("Superclass");
		ePackage.getEClassifiers().add(superClass);
		EClass subClass = lib.addNewSubclass(superClass, "test",
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
		EClass superClass = ecoreFactory.createEClass();
		EClass subClass = ecoreFactory.createEClass();
		subClass.getESuperTypes().add(superClass);
		assertThat(subClass.getESuperTypes()).containsExactly(superClass);
		lib.removeESuperType(subClass, superClass);
		assertThat(subClass.getESuperTypes()).isEmpty();
	}

	@Test
	public void test_addNewESubpackage() {
		EPackage superPackage = ecoreFactory.createEPackage();
		EPackage subPackage =
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
		EPackage superPackage = ecoreFactory.createEPackage();
		EPackage subPackage =
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
		EClass eClassSrc = ecoreFactory.createEClass();
		EClass eClassDest = ecoreFactory.createEClass();
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
		EClass eClassSrc = ecoreFactory.createEClass();
		eClassSrc.setName("Src");
		EStructuralFeature feature = ecoreFactory.createEAttribute();
		feature.setName("Attr");
		eClassSrc.getEStructuralFeatures().add(feature);
		EClass eClassDest = ecoreFactory.createEClass();
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
		EClass eClassSrc = ecoreFactory.createEClass();
		EClass eClassDest = ecoreFactory.createEClass();
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
		EClass eClassSrc = ecoreFactory.createEClass();
		EClass eClassDest = ecoreFactory.createEClass();
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
	public void test_copyAllTo() {
		EClass eClassSrc = ecoreFactory.createEClass();
		EClass eClassDest = ecoreFactory.createEClass();
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
		EClass eClassSrc = ecoreFactory.createEClass();
		eClassSrc.setName("Src");
		EStructuralFeature feature = ecoreFactory.createEAttribute();
		feature.setName("Attr");
		eClassSrc.getEStructuralFeatures().add(feature);
		EClass eClassDest = ecoreFactory.createEClass();
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
		EClass eClassSrc = ecoreFactory.createEClass();
		EClass eClassDest = ecoreFactory.createEClass();
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
		EClass eClassSrc = ecoreFactory.createEClass();
		EClass eClassDest = ecoreFactory.createEClass();
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
		EClass c1 = ecoreFactory.createEClass();
		EClass c2 = ecoreFactory.createEClass();
		EReference c1Ref = ecoreFactory.createEReference();
		c1.getEStructuralFeatures().add(c1Ref);
		EReference c2Ref = lib.createOpposite(c1Ref, "c2Ref", c2);
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
		EClass c3 = ecoreFactory.createEClass();
		EReference c3Ref = lib.createOpposite(c1Ref, "c3Ref", c3);
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
}
