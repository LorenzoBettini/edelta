/**
 * 
 */
package edelta.lib.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.emf.ecore.EcorePackage.Literals.*;

import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EEnum;
import org.eclipse.emf.ecore.EEnumLiteral;
import org.eclipse.emf.ecore.ENamedElement;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.ETypedElement;
import org.eclipse.emf.ecore.EcoreFactory;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.impl.BasicEObjectImpl;
import org.eclipse.emf.ecore.util.EcoreUtil.EqualityHelper;
import org.junit.Test;

import edelta.lib.EdeltaLibrary;

/**
 * Library functions for manipulating an Ecore model.
 * 
 * @author Lorenzo Bettini
 *
 */
public class EdeltaLibraryTest {

	private EcoreFactory ecoreFactory = EcoreFactory.eINSTANCE;

	@Test
	public void testNewEClass() {
		EClass c = EdeltaLibrary.newEClass("test");
		assertEquals("test", c.getName());
	}

	@Test
	public void testNewEClassWithInitializer() {
		EClass c = EdeltaLibrary.newEClass("test", cl -> {
			cl.setName("changed");
		});
		assertEquals("changed", c.getName());
	}

	@Test
	public void testNewEAttribute() {
		EAttribute e = EdeltaLibrary.newEAttribute("test", ESTRING);
		assertEquals("test", e.getName());
		assertEquals(ESTRING, e.getEAttributeType());
	}

	@Test
	public void testNewEAttributeWithInitializer() {
		EAttribute e = EdeltaLibrary.newEAttribute("test", ESTRING, ee -> {
			ee.setName("changed");
		});
		assertEquals("changed", e.getName());
		assertEquals(ESTRING, e.getEAttributeType());
	}

	@Test
	public void testNewEReference() {
		EReference e = EdeltaLibrary.newEReference("test", EOBJECT);
		assertEquals("test", e.getName());
		assertEquals(EOBJECT, e.getEReferenceType());
	}

	@Test
	public void testNewEReferenceWithInitializer() {
		EReference e = EdeltaLibrary.newEReference("test", EOBJECT, ee -> {
			ee.setName("changed");
		});
		assertEquals("changed", e.getName());
		assertEquals(EOBJECT, e.getEReferenceType());
	}

	@Test
	public void testNewEDataType() {
		EDataType e = EdeltaLibrary.newEDataType("test", "java.lang.String");
		assertEquals("test", e.getName());
		assertEquals("java.lang.String", e.getInstanceTypeName());
		assertEquals("java.lang.String", e.getInstanceClassName());
		assertEquals(String.class, e.getInstanceClass());
	}

	@Test
	public void testNewEDataTypeWithInitializer() {
		EDataType e = EdeltaLibrary.newEDataType("test", "java.lang.String", ee -> {
			ee.setName("changed");
		});
		assertEquals("changed", e.getName());
		assertEquals("java.lang.String", e.getInstanceTypeName());
		assertEquals("java.lang.String", e.getInstanceClassName());
		assertEquals(String.class, e.getInstanceClass());
	}

	@Test
	public void testNewEEnum() {
		EEnum e = EdeltaLibrary.newEEnum("test");
		assertEquals("test", e.getName());
	}

	@Test
	public void testNewEEnumWithInitializer() {
		EEnum e = EdeltaLibrary.newEEnum("test", ee -> {
			ee.setName("changed");
		});
		assertEquals("changed", e.getName());
	}

	@Test
	public void testNewEEnumLiteral() {
		EEnumLiteral e = EdeltaLibrary.newEEnumLiteral("test");
		assertEquals("test", e.getName());
	}

	@Test
	public void testNewEEnumLiteralWithInitializer() {
		EEnumLiteral e = EdeltaLibrary.newEEnumLiteral("test", ee -> {
			ee.setName("changed");
		});
		assertEquals("changed", e.getName());
	}

	@Test
	public void test_addEClass() {
		EPackage ePackage = ecoreFactory.createEPackage();
		EClass eClass = ecoreFactory.createEClass();
		EdeltaLibrary.addEClass(ePackage, eClass);
		assertSame(eClass,
				ePackage.getEClassifiers().get(0));
	}

	@Test
	public void test_addNewEClass() {
		EPackage ePackage = ecoreFactory.createEPackage();
		EClass eClass = EdeltaLibrary.addNewEClass(ePackage, "test");
		assertEquals("test", eClass.getName());
		assertSame(eClass,
			ePackage.getEClassifiers().get(0));
	}

	@Test
	public void test_addNewEClassWithInitializer() {
		EPackage ePackage = ecoreFactory.createEPackage();
		EClass eClass = EdeltaLibrary.addNewEClass(ePackage, "test",
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
		EClass eClass = EdeltaLibrary.addNewAbstractEClass(ePackage, "test");
		assertEquals("test", eClass.getName());
		assertThat(eClass.isAbstract())
			.isTrue();
		assertSame(eClass,
			ePackage.getEClassifiers().get(0));
	}

	@Test
	public void test_addNewAbstractEClassWithInitializer() {
		EPackage ePackage = ecoreFactory.createEPackage();
		EClass eClass = EdeltaLibrary.addNewAbstractEClass(ePackage, "test",
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
		EdeltaLibrary.addEEnum(ePackage, eEnum);
		assertSame(eEnum,
				ePackage.getEClassifiers().get(0));
	}

	@Test
	public void test_addNewEEnum() {
		EPackage ePackage = ecoreFactory.createEPackage();
		EEnum eEnum = EdeltaLibrary.addNewEEnum(ePackage, "test");
		assertEquals("test", eEnum.getName());
		assertSame(eEnum,
			ePackage.getEClassifiers().get(0));
	}

	@Test
	public void test_addNewEEnumWithInitializer() {
		EPackage ePackage = ecoreFactory.createEPackage();
		EEnum eEnum = EdeltaLibrary.addNewEEnum(ePackage, "test",
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
		EdeltaLibrary.addEEnumLiteral(eEnum, eEnumLiteral);
		assertSame(eEnumLiteral,
				eEnum.getELiterals().get(0));
	}

	@Test
	public void test_addNewEEnumLiteral() {
		EEnum eEnum = ecoreFactory.createEEnum();
		EEnumLiteral eEnumLiteral =
				EdeltaLibrary.addNewEEnumLiteral(eEnum, "test");
		assertEquals("test", eEnumLiteral.getName());
		assertSame(eEnumLiteral,
				eEnum.getELiterals().get(0));
	}

	@Test
	public void test_addNewEEnumLiteralWithInitializer() {
		EEnum eEnum = ecoreFactory.createEEnum();
		EEnumLiteral eEnumLiteral = EdeltaLibrary.addNewEEnumLiteral(eEnum, "test",
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
		EdeltaLibrary.addEDataType(ePackage, eEnum);
		assertSame(eEnum,
				ePackage.getEClassifiers().get(0));
	}

	@Test
	public void test_addNewEDataType() {
		EPackage ePackage = ecoreFactory.createEPackage();
		EDataType eDataType = EdeltaLibrary.addNewEDataType(ePackage, "test", "java.lang.String");
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
		EDataType eDataType = EdeltaLibrary.addNewEDataType(ePackage, "test", "java.lang.String",
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
		EdeltaLibrary.addEAttribute(eClass, eAttribute);
		assertSame(eAttribute,
				eClass.getEStructuralFeatures().get(0));
	}

	@Test
	public void test_addNewEAttribute() {
		EClass eClass = ecoreFactory.createEClass();
		EAttribute eAttribute =
				EdeltaLibrary.addNewEAttribute(eClass, "test", ESTRING);
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
			EdeltaLibrary.addNewEAttribute(eClass, "test", ESTRING,
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
		EdeltaLibrary.addEReference(eClass, eReference);
		assertSame(eReference,
				eClass.getEStructuralFeatures().get(0));
	}

	@Test
	public void test_addNewEReference() {
		EClass eClass = ecoreFactory.createEClass();
		EReference eReference =
				EdeltaLibrary.addNewEReference(eClass, "test", EOBJECT);
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
			EdeltaLibrary.addNewEReference(eClass, "test", EOBJECT,
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
				EdeltaLibrary.addNewContainmentEReference(eClass, "test", EOBJECT);
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
			EdeltaLibrary.addNewContainmentEReference(eClass, "test", EOBJECT,
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
		EdeltaLibrary.addESuperType(subClass, superClass);
		assertThat(subClass.getESuperTypes()).containsExactly(superClass);
	}

	@Test
	public void test_addNewSubclass() {
		EPackage ePackage = ecoreFactory.createEPackage();
		EClass superClass = ecoreFactory.createEClass();
		superClass.setName("Superclass");
		ePackage.getEClassifiers().add(superClass);
		EClass subClass = EdeltaLibrary.addNewSubclass(superClass, "test");
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
		EClass subClass = EdeltaLibrary.addNewSubclass(superClass, "test",
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
		EdeltaLibrary.removeESuperType(subClass, superClass);
		assertThat(subClass.getESuperTypes()).isEmpty();
	}

	@Test
	public void test_addNewESubpackage() {
		EPackage superPackage = ecoreFactory.createEPackage();
		EPackage subPackage =
			EdeltaLibrary.addNewESubpackage(superPackage, "newSubpackage", "prefix", "nsURI");
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
			EdeltaLibrary.addNewESubpackage(superPackage, "newSubpackage", "prefix", "nsURI",
				(EPackage p) -> p.setName("changed"));
		assertThat(subPackage)
			.returns("changed", EPackage::getName)
			.returns("prefix", EPackage::getNsPrefix)
			.returns("nsURI", EPackage::getNsURI);
		assertThat(superPackage.getESubpackages()).containsOnly(subPackage);
		assertThat(subPackage.getESuperPackage()).isSameAs(superPackage);
	}

	@Test
	public void testGetEObjectRepr() {
		assertEquals("ecore.EClass.eSuperTypes",
			EdeltaLibrary.getEObjectRepr(EcorePackage.eINSTANCE.getEClass_ESuperTypes()));
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
				EdeltaLibrary.getEObjectRepr(o));
		assertEquals("",
				EdeltaLibrary.getEObjectRepr(null));
	}

	@Test
	public void testGetEObjectReprWithCycle() {
		EPackage p1 = ecoreFactory.createEPackage();
		p1.setName("p1");
		EPackage p2 = ecoreFactory.createEPackage();
		p2.setName("p2");
		// create the cycle
		p1.getESubpackages().add(p2);
		p2.getESubpackages().add(p1);
		assertThat(p1.getESubpackages()).contains(p2);
		assertThat(p2.getESubpackages()).contains(p1);
		assertEquals("p1.p2.p1",
				EdeltaLibrary.getEObjectRepr(p1));
	}

	@Test
	public void test_removeElement() {
		EPackage ePackage = ecoreFactory.createEPackage();
		EClass superClass = ecoreFactory.createEClass();
		EClass subClass = ecoreFactory.createEClass();
		ePackage.getEClassifiers().add(superClass);
		ePackage.getEClassifiers().add(subClass);
		subClass.getESuperTypes().add(superClass);
		EReference referenceToSuperClass = ecoreFactory.createEReference();
		referenceToSuperClass.setEType(superClass);
		EReference referenceToSubClass = ecoreFactory.createEReference();
		referenceToSubClass.setEType(subClass);
		EReference opposite = ecoreFactory.createEReference();
		opposite.setEOpposite(referenceToSubClass);
		referenceToSubClass.setEOpposite(opposite);
		subClass.getEStructuralFeatures().add(referenceToSubClass);
		subClass.getEStructuralFeatures().add(opposite);
		subClass.getEStructuralFeatures().add(referenceToSuperClass);
		assertThat(subClass.getESuperTypes()).containsExactly(superClass);
		EdeltaLibrary.removeElement(superClass);
		// references to the removed class should be removed as well
		assertThat(subClass.getESuperTypes()).isEmpty();
		assertThat(subClass.getEStructuralFeatures())
			.containsOnly(referenceToSubClass, opposite);
		// the opposite reference should be set to null as well
		EdeltaLibrary.removeElement(referenceToSubClass);
		assertThat(subClass.getEStructuralFeatures())
			.containsOnly(opposite);
		assertThat(opposite.getEOpposite()).isNull();
		// try to remove something simpler
		EAttribute attribute = ecoreFactory.createEAttribute();
		subClass.getEStructuralFeatures().add(attribute);
		EdeltaLibrary.removeElement(attribute);
		assertThat(subClass.getEStructuralFeatures())
			.containsOnly(opposite);
		// try to remove an EClass and its contents
		attribute = ecoreFactory.createEAttribute();
		subClass.getEStructuralFeatures().add(attribute);
		EdeltaLibrary.removeElement(subClass);
		assertThat(subClass.getEStructuralFeatures()).isEmpty();
	}

	@Test
	public void test_allEClasses() {
		assertThat(EdeltaLibrary.allEClasses(null)).isEmpty();
		EPackage ePackage = ecoreFactory.createEPackage();
		EClass eClass = ecoreFactory.createEClass();
		EDataType dataType = ecoreFactory.createEDataType();
		ePackage.getEClassifiers().add(eClass);
		ePackage.getEClassifiers().add(dataType);
		assertThat(EdeltaLibrary.allEClasses(ePackage))
			.containsOnly(eClass);
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
		var copy = EdeltaLibrary.copyTo(feature, eClassDest);
		// after
		assertThat(eClassSrc.getEStructuralFeatures())
			.contains(feature);
		assertThat(eClassDest.getEStructuralFeatures())
			.hasSize(1)
			.containsOnly(copy);
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
		var copy = EdeltaLibrary.copyToAs(feature, eClassDest, "newName");
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
		var copy = EdeltaLibrary.copyToAs(feature, eClassDest, "newName",
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
		EdeltaLibrary.copyAllTo(Arrays.asList(feature1, feature2), eClassDest);
		// after
		assertThat(eClassSrc.getEStructuralFeatures())
			.contains(feature1, feature2);
		assertThat(eClassDest.getEStructuralFeatures())
			.hasSize(2)
			.allMatch(f -> new EqualityHelper().equals(f, feature1)
						|| new EqualityHelper().equals(f, feature2));
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
		EdeltaLibrary.moveTo(feature, eClassDest);
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
		EdeltaLibrary.moveAllTo(Arrays.asList(feature1, feature2), eClassDest);
		// after
		assertThat(eClassSrc.getEStructuralFeatures()).isEmpty();
		assertThat(eClassDest.getEStructuralFeatures())
			.contains(feature1, feature2);
	}

	@Test
	public void test_setEOppositeDoesNotMakeReferenceBidirectionalAutomatically() {
		EClass c1 = ecoreFactory.createEClass();
		EClass c2 = ecoreFactory.createEClass();
		EReference c1Ref = ecoreFactory.createEReference();
		c1.getEStructuralFeatures().add(c1Ref);
		EReference c2Ref = ecoreFactory.createEReference();
		c2.getEStructuralFeatures().add(c2Ref);

		c1Ref.setEOpposite(c2Ref);
		assertThat(c2Ref.getEOpposite()).isNull();
	}

	@Test
	public void test_makeBidirectional() {
		EClass c1 = ecoreFactory.createEClass();
		EClass c2 = ecoreFactory.createEClass();
		EReference c1Ref = ecoreFactory.createEReference();
		c1.getEStructuralFeatures().add(c1Ref);
		EReference c2Ref = ecoreFactory.createEReference();
		c2.getEStructuralFeatures().add(c2Ref);
		EdeltaLibrary.makeBidirectional(c1Ref, c2Ref);
		assertThat(c1Ref.getEOpposite()).isNotNull();
		assertThat(c2Ref.getEOpposite()).isNotNull();
		assertThat(c1Ref.getEOpposite()).isSameAs(c2Ref);
		assertThat(c2Ref.getEOpposite()).isSameAs(c1Ref);
		assertThat(c1Ref.getEReferenceType()).isSameAs(c2);
		assertThat(c2Ref.getEReferenceType()).isSameAs(c1);
		assertThat(c1Ref.getEOpposite().getEReferenceType()).isSameAs(c1);
		assertThat(c2Ref.getEOpposite().getEReferenceType()).isSameAs(c2);
		// test it with an existing opposite
		EClass c3 = ecoreFactory.createEClass();
		EReference c3Ref = ecoreFactory.createEReference();
		c3.getEStructuralFeatures().add(c3Ref);
		EdeltaLibrary.makeBidirectional(c1Ref, c3Ref);
		assertThat(c1Ref.getEOpposite()).isNotNull();
		assertThat(c3Ref.getEOpposite()).isNotNull();
		assertThat(c1Ref.getEOpposite()).isSameAs(c3Ref);
		assertThat(c3Ref.getEOpposite()).isSameAs(c1Ref);
		assertThat(c1Ref.getEReferenceType()).isSameAs(c3);
		assertThat(c3Ref.getEReferenceType()).isSameAs(c1);
		assertThat(c1Ref.getEOpposite().getEReferenceType()).isSameAs(c1);
		assertThat(c3Ref.getEOpposite().getEReferenceType()).isSameAs(c3);
		assertThat(c2Ref.getEOpposite()).isNull();
	}

	@Test
	public void test_dropOpposite() {
		EClass c1 = ecoreFactory.createEClass();
		EClass c2 = ecoreFactory.createEClass();
		EReference c1Ref = ecoreFactory.createEReference();
		c1.getEStructuralFeatures().add(c1Ref);
		EReference c2Ref = ecoreFactory.createEReference();
		c2.getEStructuralFeatures().add(c2Ref);
		// test it with no existing opposite
		EdeltaLibrary.dropOpposite(c1Ref);
		assertThat(c1Ref.getEOpposite()).isNull();
		// test it with an existing opposite
		c1Ref.setEOpposite(c2Ref);
		c2Ref.setEOpposite(c1Ref);
		assertThat(c1Ref.getEOpposite()).isSameAs(c2Ref);
		assertThat(c2Ref.getEOpposite()).isSameAs(c1Ref);
		EdeltaLibrary.dropOpposite(c1Ref);
		assertThat(c1Ref.getEOpposite()).isNull();
		assertThat(c2Ref.getEOpposite()).isNull();
	}

	@Test
	public void test_removeOpposite() {
		EClass c1 = ecoreFactory.createEClass();
		EClass c2 = ecoreFactory.createEClass();
		EReference c1Ref = ecoreFactory.createEReference();
		c1.getEStructuralFeatures().add(c1Ref);
		EReference c2Ref = ecoreFactory.createEReference();
		c2.getEStructuralFeatures().add(c2Ref);
		// test it with no existing opposite
		EdeltaLibrary.removeOpposite(c1Ref);
		assertThat(c1Ref.getEOpposite()).isNull();
		assertThat(c1.getEReferences()).containsExactly(c1Ref);
		assertThat(c2.getEReferences()).containsExactly(c2Ref);
		// test it with an existing opposite
		c1Ref.setEOpposite(c2Ref);
		c2Ref.setEOpposite(c1Ref);
		assertThat(c1Ref.getEOpposite()).isSameAs(c2Ref);
		assertThat(c2Ref.getEOpposite()).isSameAs(c1Ref);
		EdeltaLibrary.removeOpposite(c1Ref);
		assertThat(c1Ref.getEOpposite()).isNull();
		assertThat(c2Ref.getEOpposite()).isNull();
		assertThat(c1.getEReferences()).containsExactly(c1Ref);
		assertThat(c2.getEReferences()).isEmpty();
	}

	@Test
	public void test_createOpposite() {
		EClass c1 = ecoreFactory.createEClass();
		EClass c2 = ecoreFactory.createEClass();
		EReference c1Ref = ecoreFactory.createEReference();
		c1.getEStructuralFeatures().add(c1Ref);
		EReference c2Ref = EdeltaLibrary.createOpposite(c1Ref, "c2Ref", c2);
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
		EReference c3Ref = EdeltaLibrary.createOpposite(c1Ref, "c3Ref", c3);
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
	public void test_makeSingleRequired() {
		EStructuralFeature feature = ecoreFactory.createEReference();
		feature.setLowerBound(0);
		feature.setUpperBound(-1);
		EdeltaLibrary.makeSingleRequired(feature);
		assertThat(feature)
			.returns(1, ETypedElement::getLowerBound)
			.returns(1, ETypedElement::getUpperBound);
	}

	@Test
	public void test_makeAbstract() {
		var c = ecoreFactory.createEClass();
		c.setAbstract(false);
		EdeltaLibrary.makeAbstract(c);
		assertThat(c.isAbstract()).isTrue();
	}

	@Test
	public void test_makeConcrete() {
		var c = ecoreFactory.createEClass();
		c.setAbstract(true);
		EdeltaLibrary.makeConcrete(c);
		assertThat(c.isAbstract()).isFalse();
	}

	@Test
	public void test_referringANewEClassDoesNotAddItToEPackageAutomatically() {
		EPackage ePackage = ecoreFactory.createEPackage();
		EClass client = ecoreFactory.createEClass();
		ePackage.getEClassifiers().add(client);
		EClass referred = ecoreFactory.createEClass();
		client.getESuperTypes().add(referred);
		assertThat(client.getEPackage()).isSameAs(ePackage);
		assertThat(referred.getEPackage()).isNull();
	}
}
