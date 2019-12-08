/**
 * 
 */
package edelta.lib.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

import static org.eclipse.emf.ecore.EcorePackage.Literals.*;

import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EEnum;
import org.eclipse.emf.ecore.EEnumLiteral;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EcoreFactory;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.impl.BasicEObjectImpl;
import org.junit.Before;
import org.junit.Test;

import edelta.lib.EdeltaLibrary;

/**
 * Library functions for manipulating an Ecore model.
 * 
 * @author Lorenzo Bettini
 *
 */
public class EdeltaLibraryTest {

	private EdeltaLibrary lib;

	private EcoreFactory ecoreFactory = EcoreFactory.eINSTANCE;

	@Before
	public void initLib() {
		lib = new EdeltaLibrary();
	}

	@Test
	public void testNewEClass() {
		EClass c = lib.newEClass("test");
		assertEquals("test", c.getName());
	}

	@Test
	public void testNewEClassWithInitializer() {
		EClass c = lib.newEClass("test", cl -> {
			cl.setName("changed");
		});
		assertEquals("changed", c.getName());
	}

	@Test
	public void testNewEAttribute() {
		EAttribute e = lib.newEAttribute("test");
		assertEquals("test", e.getName());
	}

	@Test
	public void testNewEAttributeWithInitializer() {
		EAttribute e = lib.newEAttribute("test", ee -> {
			ee.setName("changed");
		});
		assertEquals("changed", e.getName());
	}

	@Test
	public void testNewEAttributeWithTypeAndInitialize() {
		EAttribute e = lib.newEAttribute("test", ESTRING, ee -> {
			ee.setName("changed");
		});
		assertEquals("changed", e.getName());
		assertEquals(ESTRING, e.getEAttributeType());
	}

	@Test
	public void testNewEReference() {
		EReference e = lib.newEReference("test");
		assertEquals("test", e.getName());
	}

	@Test
	public void testNewEReferenceWithInitializer() {
		EReference e = lib.newEReference("test", ee -> {
			ee.setName("changed");
		});
		assertEquals("changed", e.getName());
	}

	@Test
	public void testNewEReferenceWithTypeInitializer() {
		EReference e = lib.newEReference("test", EOBJECT, ee -> {
			ee.setName("changed");
		});
		assertEquals("changed", e.getName());
		assertEquals(EOBJECT, e.getEReferenceType());
	}

	@Test
	public void testNewEEnum() {
		EEnum e = lib.newEEnum("test");
		assertEquals("test", e.getName());
	}

	@Test
	public void testNewEEnumWithInitializer() {
		EEnum e = lib.newEEnum("test", ee -> {
			ee.setName("changed");
		});
		assertEquals("changed", e.getName());
	}

	@Test
	public void testNewEEnumLiteral() {
		EEnumLiteral e = lib.newEEnumLiteral("test");
		assertEquals("test", e.getName());
	}

	@Test
	public void testNewEEnumLiteralWithInitializer() {
		EEnumLiteral e = lib.newEEnumLiteral("test", ee -> {
			ee.setName("changed");
		});
		assertEquals("changed", e.getName());
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
	public void test_addEAttribute() {
		EClass eClass = ecoreFactory.createEClass();
		EAttribute eAttribute = ecoreFactory.createEAttribute();
		lib.addEAttribute(eClass, eAttribute);
		assertSame(eAttribute,
				eClass.getEStructuralFeatures().get(0));
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
	public void testGetEObjectRepr() {
		assertEquals("ecore:EClass:eSuperTypes", lib.getEObjectRepr(EcorePackage.eINSTANCE.getEClass_ESuperTypes()));
		BasicEObjectImpl o = new BasicEObjectImpl() {
			@Override
			public String toString() {
				return "test";
			}
			
			@Override
			public EObject eContainer() {
				return null;
			}
		};
		assertEquals("test",
				lib.getEObjectRepr(o));
	}
}
