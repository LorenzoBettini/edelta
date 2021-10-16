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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EEnum;
import org.eclipse.emf.ecore.EEnumLiteral;
import org.eclipse.emf.ecore.ENamedElement;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EcoreFactory;
import org.eclipse.emf.ecore.resource.Resource;
import org.junit.Before;
import org.junit.Test;
import org.junit.function.ThrowingRunnable;

import edelta.lib.AbstractEdelta;
import edelta.lib.EdeltaIssuePresenter;
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

	@Before
	public void setup() {
		issuePresenter = mock(EdeltaIssuePresenter.class);
		lib = new EdeltaStandardLibrary();
		lib.setIssuePresenter(issuePresenter);
	}

	@Test
	public void testGetEPackageWithOtherEdelta() {
		AbstractEdelta other = new AbstractEdelta() {
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
	public void test_addEEnumLiteral() {
		EEnum eEnum = ecoreFactory.createEEnum();
		EEnumLiteral eEnumLiteral = ecoreFactory.createEEnumLiteral();
		lib.addEEnumLiteral(eEnum, eEnumLiteral);
		assertSame(eEnumLiteral,
				eEnum.getELiterals().get(0));
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

	private Resource loadTestEcore(String ecoreFile) {
		return lib.loadEcoreFile(TESTECORES+ecoreFile);
	}

	private IllegalArgumentException assertThrowsIAE(ThrowingRunnable executable,
			ENamedElement element,
			String expectedError) {
		var assertThrows = assertThrows(IllegalArgumentException.class,
					executable);
		assertEquals(expectedError, assertThrows.getMessage());
		verify(issuePresenter).showError(element, expectedError);
		return assertThrows;
	}
}
