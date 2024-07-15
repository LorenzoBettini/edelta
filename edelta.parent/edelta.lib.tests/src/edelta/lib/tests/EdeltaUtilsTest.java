/**
 * 
 */
package edelta.lib.tests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.emf.ecore.EcorePackage.Literals.EOBJECT;
import static org.eclipse.emf.ecore.EcorePackage.Literals.ESTRING;
import static org.junit.Assert.assertEquals;

import java.util.List;

import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EEnum;
import org.eclipse.emf.ecore.EEnumLiteral;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.ETypedElement;
import org.eclipse.emf.ecore.EcoreFactory;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.impl.BasicEObjectImpl;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.impl.ResourceImpl;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.junit.Test;

import edelta.lib.EdeltaDefaultRuntime;
import edelta.lib.EdeltaModelManager;
import edelta.lib.EdeltaUtils;

/**
 * Library functions for manipulating an Ecore model.
 * 
 * @author Lorenzo Bettini
 *
 */
public class EdeltaUtilsTest {

	private static EcoreFactory ecoreFactory = EcoreFactory.eINSTANCE;

	@Test
	public void testNewEClass() {
		EClass c = EdeltaUtils.newEClass("test");
		assertEquals("test", c.getName());
	}

	@Test
	public void testNewEClassWithInitializer() {
		EClass c = EdeltaUtils.newEClass("test", cl -> {
			cl.setName("changed");
		});
		assertEquals("changed", c.getName());
	}

	@Test
	public void testNewEAttribute() {
		EAttribute e = EdeltaUtils.newEAttribute("test", ESTRING);
		assertEquals("test", e.getName());
		assertEquals(ESTRING, e.getEAttributeType());
	}

	@Test
	public void testNewEAttributeWithInitializer() {
		EAttribute e = EdeltaUtils.newEAttribute("test", ESTRING, ee -> {
			ee.setName("changed");
		});
		assertEquals("changed", e.getName());
		assertEquals(ESTRING, e.getEAttributeType());
	}

	@Test
	public void testNewEReference() {
		EReference e = EdeltaUtils.newEReference("test", EOBJECT);
		assertEquals("test", e.getName());
		assertEquals(EOBJECT, e.getEReferenceType());
	}

	@Test
	public void testNewEReferenceWithInitializer() {
		EReference e = EdeltaUtils.newEReference("test", EOBJECT, ee -> {
			ee.setName("changed");
		});
		assertEquals("changed", e.getName());
		assertEquals(EOBJECT, e.getEReferenceType());
	}

	@Test
	public void testNewEDataType() {
		EDataType e = EdeltaUtils.newEDataType("test", "java.lang.String");
		assertEquals("test", e.getName());
		assertEquals("java.lang.String", e.getInstanceTypeName());
		assertEquals("java.lang.String", e.getInstanceClassName());
		assertEquals(String.class, e.getInstanceClass());
	}

	@Test
	public void testNewEDataTypeWithInitializer() {
		EDataType e = EdeltaUtils.newEDataType("test", "java.lang.String", ee -> {
			ee.setName("changed");
		});
		assertEquals("changed", e.getName());
		assertEquals("java.lang.String", e.getInstanceTypeName());
		assertEquals("java.lang.String", e.getInstanceClassName());
		assertEquals(String.class, e.getInstanceClass());
	}

	@Test
	public void testNewEEnum() {
		EEnum e = EdeltaUtils.newEEnum("test");
		assertEquals("test", e.getName());
	}

	@Test
	public void testNewEEnumWithInitializer() {
		EEnum e = EdeltaUtils.newEEnum("test", ee -> {
			ee.setName("changed");
		});
		assertEquals("changed", e.getName());
	}

	@Test
	public void testNewEEnumLiteral() {
		EEnumLiteral e = EdeltaUtils.newEEnumLiteral("test");
		assertEquals("test", e.getName());
	}

	@Test
	public void testNewEEnumLiteralWithInitializer() {
		EEnumLiteral e = EdeltaUtils.newEEnumLiteral("test", ee -> {
			ee.setName("changed");
		});
		assertEquals("changed", e.getName());
	}

	@Test
	public void test_removeESuperType() {
		EClass superClass = ecoreFactory.createEClass();
		EClass subClass = ecoreFactory.createEClass();
		subClass.getESuperTypes().add(superClass);
		assertThat(subClass.getESuperTypes()).containsExactly(superClass);
		EdeltaUtils.removeESuperType(subClass, superClass);
		assertThat(subClass.getESuperTypes()).isEmpty();
	}

	@Test
	public void testGetEObjectRepr() {
		assertEquals("ecore.EClass.eSuperTypes",
			EdeltaUtils.getEObjectRepr(EcorePackage.eINSTANCE.getEClass_ESuperTypes()));
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
				EdeltaUtils.getEObjectRepr(o));
		assertEquals("",
				EdeltaUtils.getEObjectRepr(null));
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
				EdeltaUtils.getEObjectRepr(p1));
	}

	@Test
	public void test_getFullyQualifiedName() {
		assertEquals("ecore.EClass.eSuperTypes",
			EdeltaUtils.getFullyQualifiedName(EcorePackage.eINSTANCE.getEClass_ESuperTypes()));
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
		EdeltaUtils.removeElement(superClass);
		// references to the removed class should be removed as well
		assertThat(subClass.getESuperTypes()).isEmpty();
		assertThat(subClass.getEStructuralFeatures())
			.containsOnly(referenceToSubClass, opposite);
		// the opposite reference should be set to null as well
		EdeltaUtils.removeElement(referenceToSubClass);
		assertThat(subClass.getEStructuralFeatures())
			.containsOnly(opposite);
		assertThat(opposite.getEOpposite()).isNull();
		// try to remove something simpler
		EAttribute attribute = ecoreFactory.createEAttribute();
		subClass.getEStructuralFeatures().add(attribute);
		EdeltaUtils.removeElement(attribute);
		assertThat(subClass.getEStructuralFeatures())
			.containsOnly(opposite);
		// try to remove an EClass and its contents
		attribute = ecoreFactory.createEAttribute();
		subClass.getEStructuralFeatures().add(attribute);
		EdeltaUtils.removeElement(subClass);
		assertThat(subClass.getEStructuralFeatures()).isEmpty();
	}

	@Test
	public void test_removeElementInResourceSet() {
		// the references to be removed are in another package
		// in the same resource set.
		var resourceSet = new ResourceSetImpl();

		var p1 = ecoreFactory.createEPackage();
		var resource1 = new ResourceImpl();
		resource1.getContents().add(p1);
		resourceSet.getResources().add(resource1);
		var superClass = ecoreFactory.createEClass();
		p1.getEClassifiers().add(superClass);

		var p2 = ecoreFactory.createEPackage();
		var resource2 = new ResourceImpl();
		resource2.getContents().add(p2);
		resourceSet.getResources().add(resource2);
		var subClass = ecoreFactory.createEClass();
		p2.getEClassifiers().add(subClass);
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
		EdeltaUtils.removeElement(superClass);
		// references to the removed class should be removed as well
		assertThat(subClass.getESuperTypes()).isEmpty();
		assertThat(subClass.getEStructuralFeatures())
			.containsOnly(referenceToSubClass, opposite);
		// the opposite reference should be set to null as well
		EdeltaUtils.removeElement(referenceToSubClass);
		assertThat(subClass.getEStructuralFeatures())
			.containsOnly(opposite);
		assertThat(opposite.getEOpposite()).isNull();
		// try to remove something simpler
		EAttribute attribute = ecoreFactory.createEAttribute();
		subClass.getEStructuralFeatures().add(attribute);
		EdeltaUtils.removeElement(attribute);
		assertThat(subClass.getEStructuralFeatures())
			.containsOnly(opposite);
		// try to remove an EClass and its contents
		attribute = ecoreFactory.createEAttribute();
		subClass.getEStructuralFeatures().add(attribute);
		EdeltaUtils.removeElement(subClass);
		assertThat(subClass.getEStructuralFeatures()).isEmpty();
	}

	@Test
	public void test_allEClasses() {
		assertThat(EdeltaUtils.allEClasses(null)).isEmpty();
		EPackage ePackage = ecoreFactory.createEPackage();
		EClass eClass = ecoreFactory.createEClass();
		EDataType dataType = ecoreFactory.createEDataType();
		ePackage.getEClassifiers().add(eClass);
		ePackage.getEClassifiers().add(dataType);
		assertThat(EdeltaUtils.allEClasses(ePackage))
			.containsOnly(eClass);
	}

	@Test
	public void test_allEClassesInResourceSet() {
		var p1 = ecoreFactory.createEPackage();
		var eClass1 = ecoreFactory.createEClass();
		var dataType = ecoreFactory.createEDataType();
		p1.getEClassifiers().add(eClass1);
		p1.getEClassifiers().add(dataType);

		var p2 = ecoreFactory.createEPackage();
		var eClass2 = ecoreFactory.createEClass();
		p2.getEClassifiers().add(eClass2);

		var resource1 = new ResourceImpl();
		resource1.getContents().add(p1);

		var resource2 = new ResourceImpl();
		resource2.getContents().add(p2);
		
		var resourceSet = new ResourceSetImpl();
		resourceSet.getResources().add(resource1);
		resourceSet.getResources().add(resource2);

		assertThat(EdeltaUtils.allEClasses(p1))
			.containsExactlyInAnyOrder(eClass1, eClass2);
	}

	@Test
	public void test_getEClasses() {
		var p1 = ecoreFactory.createEPackage();
		var eClass1 = ecoreFactory.createEClass();
		var dataType = ecoreFactory.createEDataType();
		p1.getEClassifiers().add(eClass1);
		p1.getEClassifiers().add(dataType);

		var p2 = ecoreFactory.createEPackage();
		var eClass2 = ecoreFactory.createEClass();
		p2.getEClassifiers().add(eClass2);

		var resource1 = new ResourceImpl();
		resource1.getContents().add(p1);

		var resource2 = new ResourceImpl();
		resource2.getContents().add(p2);
		
		var resourceSet = new ResourceSetImpl();
		resourceSet.getResources().add(resource1);
		resourceSet.getResources().add(resource2);

		// differently from allEClasses, it only returns
		// the EClasses of the single package
		assertThat(EdeltaUtils.getEClasses(p1))
			.containsOnly(eClass1);
		assertThat(EdeltaUtils.getEClasses(p2))
			.containsOnly(eClass2);
		assertThat(EdeltaUtils.getEClasses(null))
			.isEmpty();
	}

	@Test
	public void test_allEStructuralFeatures() {
		var p1 = ecoreFactory.createEPackage();
		var eClass1 = ecoreFactory.createEClass();
		var dataType = ecoreFactory.createEDataType();
		p1.getEClassifiers().add(eClass1);
		p1.getEClassifiers().add(dataType);

		var p2 = ecoreFactory.createEPackage();
		var eClass2 = ecoreFactory.createEClass();
		p2.getEClassifiers().add(eClass2);

		var resource1 = new ResourceImpl();
		resource1.getContents().add(p1);

		var resource2 = new ResourceImpl();
		resource2.getContents().add(p2);
		
		var resourceSet = new ResourceSetImpl();
		resourceSet.getResources().add(resource1);
		resourceSet.getResources().add(resource2);

		var f1 = ecoreFactory.createEAttribute();
		var f2 = ecoreFactory.createEReference();
		eClass1.getEStructuralFeatures().addAll(List.of(f1, f2));

		var f3 = ecoreFactory.createEAttribute();
		var f4 = ecoreFactory.createEReference();
		eClass2.getEStructuralFeatures().addAll(List.of(f3, f4));

		// differently from allEStructuralFeatures, it only returns
		// the EStructuralFeatures of the single package
		assertThat(EdeltaUtils.getEStructuralFeatures(p1))
			.containsOnly(f1, f2);
		assertThat(EdeltaUtils.getEStructuralFeatures(p2))
			.containsOnly(f3, f4);
		assertThat(EdeltaUtils.getEStructuralFeatures(null))
			.isEmpty();
	}

	@Test
	public void test_getEStructuralFeatures() {
		var p1 = ecoreFactory.createEPackage();
		var eClass1 = ecoreFactory.createEClass();
		var dataType = ecoreFactory.createEDataType();
		p1.getEClassifiers().add(eClass1);
		p1.getEClassifiers().add(dataType);

		var p2 = ecoreFactory.createEPackage();
		var eClass2 = ecoreFactory.createEClass();
		p2.getEClassifiers().add(eClass2);

		var resource1 = new ResourceImpl();
		resource1.getContents().add(p1);

		var resource2 = new ResourceImpl();
		resource2.getContents().add(p2);
		
		var resourceSet = new ResourceSetImpl();
		resourceSet.getResources().add(resource1);
		resourceSet.getResources().add(resource2);

		var f1 = ecoreFactory.createEAttribute();
		var f2 = ecoreFactory.createEReference();
		eClass1.getEStructuralFeatures().addAll(List.of(f1, f2));

		var f3 = ecoreFactory.createEAttribute();
		var f4 = ecoreFactory.createEReference();
		eClass2.getEStructuralFeatures().addAll(List.of(f3, f4));

		
		assertThat(EdeltaUtils.allEStructuralFeatures(p1))
			.containsExactlyInAnyOrder(f1, f2, f3, f4);
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
		EdeltaUtils.makeBidirectional(c1Ref, c2Ref);
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
		EdeltaUtils.makeBidirectional(c1Ref, c3Ref);
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
		EdeltaUtils.dropOpposite(c1Ref);
		assertThat(c1Ref.getEOpposite()).isNull();
		// test it with an existing opposite
		c1Ref.setEOpposite(c2Ref);
		c2Ref.setEOpposite(c1Ref);
		assertThat(c1Ref.getEOpposite()).isSameAs(c2Ref);
		assertThat(c2Ref.getEOpposite()).isSameAs(c1Ref);
		EdeltaUtils.dropOpposite(c1Ref);
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
		EdeltaUtils.removeOpposite(c1Ref);
		assertThat(c1Ref.getEOpposite()).isNull();
		assertThat(c1.getEReferences()).containsExactly(c1Ref);
		assertThat(c2.getEReferences()).containsExactly(c2Ref);
		// test it with an existing opposite
		c1Ref.setEOpposite(c2Ref);
		c2Ref.setEOpposite(c1Ref);
		assertThat(c1Ref.getEOpposite()).isSameAs(c2Ref);
		assertThat(c2Ref.getEOpposite()).isSameAs(c1Ref);
		EdeltaUtils.removeOpposite(c1Ref);
		assertThat(c1Ref.getEOpposite()).isNull();
		assertThat(c2Ref.getEOpposite()).isNull();
		assertThat(c1.getEReferences()).containsExactly(c1Ref);
		assertThat(c2.getEReferences()).isEmpty();
	}

	@Test
	public void test_makeSingle() {
		EStructuralFeature feature = ecoreFactory.createEReference();
		feature.setLowerBound(0);
		feature.setUpperBound(2);
		EdeltaUtils.makeSingle(feature);
		assertThat(feature)
			.returns(0, ETypedElement::getLowerBound)
			.returns(1, ETypedElement::getUpperBound);
	}

	@Test
	public void test_makeMultiple() {
		EStructuralFeature feature = ecoreFactory.createEReference();
		feature.setLowerBound(1);
		feature.setUpperBound(1);
		EdeltaUtils.makeMultiple(feature);
		assertThat(feature)
			.returns(1, ETypedElement::getLowerBound)
			.returns(-1, ETypedElement::getUpperBound);
	}

	@Test
	public void test_makeRequired() {
		EStructuralFeature feature = ecoreFactory.createEReference();
		feature.setLowerBound(0);
		feature.setUpperBound(2);
		EdeltaUtils.makeRequired(feature);
		assertThat(feature)
			.returns(1, ETypedElement::getLowerBound)
			.returns(2, ETypedElement::getUpperBound);
	}

	@Test
	public void test_makeSingleRequired() {
		EStructuralFeature feature = ecoreFactory.createEReference();
		feature.setLowerBound(0);
		feature.setUpperBound(-1);
		EdeltaUtils.makeSingleRequired(feature);
		assertThat(feature)
			.returns(1, ETypedElement::getLowerBound)
			.returns(1, ETypedElement::getUpperBound);
	}

	@Test
	public void test_makeContainment() {
		var reference = ecoreFactory.createEReference();
		reference.setContainment(false);
		EdeltaUtils.makeContainment(reference);
		assertThat(reference.isContainment()).isTrue();
	}

	@Test
	public void test_dropContainment() {
		var reference = ecoreFactory.createEReference();
		reference.setContainment(true);
		EdeltaUtils.dropContainment(reference);
		assertThat(reference.isContainment()).isFalse();
	}

	@Test
	public void test_makeAbstract() {
		var c = ecoreFactory.createEClass();
		c.setAbstract(false);
		EdeltaUtils.makeAbstract(c);
		assertThat(c.isAbstract()).isTrue();
	}

	@Test
	public void test_makeConcrete() {
		var c = ecoreFactory.createEClass();
		c.setAbstract(true);
		EdeltaUtils.makeConcrete(c);
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

	@Test
	public void test_packagesToInspectWhenNoContainingPackage() {
		var c = ecoreFactory.createEClass();
		var packages = EdeltaUtils.packagesToInspect(c);
		assertThat(packages).isEmpty();
	}

	@Test
	public void test_packagesToInspectWhenPackageNotInResource() {
		var c = ecoreFactory.createEClass();
		var p = ecoreFactory.createEPackage();
		p.getEClassifiers().add(c);
		var packages = EdeltaUtils.packagesToInspect(c);
		assertThat(packages).containsExactly(p);
	}

	@Test
	public void test_packagesToInspectWhenPackageNotInResourceSet() {
		var c = ecoreFactory.createEClass();
		var p = ecoreFactory.createEPackage();
		var resource = new ResourceImpl();
		resource.getContents().add(p);
		p.getEClassifiers().add(c);
		var packages = EdeltaUtils.packagesToInspect(c);
		assertThat(packages).containsExactly(p);
	}

	@Test
	public void test_packagesToInspectWhenPackageInResourceSet() {
		var c = ecoreFactory.createEClass();
		var p = ecoreFactory.createEPackage();
		var resource = new ResourceImpl();
		resource.getContents().add(p);
		p.getEClassifiers().add(c);
		var resourceSet = new ResourceSetImpl();
		resourceSet.getResources().add(resource);
		var anotherResource = new ResourceImpl();
		resourceSet.getResources().add(anotherResource);
		var anotherPackage = ecoreFactory.createEPackage();
		anotherResource.getContents().add(anotherPackage);
		var packages = EdeltaUtils.packagesToInspect(c);
		assertThat(packages).containsExactlyInAnyOrder(p, anotherPackage);
	}

	@Test
	public void test_packagesToInspectSkipsEcorePackage() {
		var c = ecoreFactory.createEClass();
		var p = ecoreFactory.createEPackage();
		var resource = new ResourceImpl();
		resource.getContents().add(p);
		p.getEClassifiers().add(c);
		var resourceSet = new ResourceSetImpl();
		resourceSet.getResources().add(resource);
		var anotherResource = new ResourceImpl();
		resourceSet.getResources().add(anotherResource);
		var anotherPackage = ecoreFactory.createEPackage();
		anotherResource.getContents().add(anotherPackage);
		Resource ecorePackageResource = EcorePackage.eINSTANCE.eResource();
		resourceSet.getResources().add(ecorePackageResource);
		var packages = EdeltaUtils.packagesToInspect(c);
		assertThat(packages).containsExactlyInAnyOrder(p, anotherPackage);
	}

	@Test
	public void test_usedPackages() {
		var modelManager = new EdeltaModelManager();
		var edelta = new EdeltaDefaultRuntime(modelManager);
		modelManager.loadEcoreFile("testecores/TestEcoreForUsages1.ecore");
		modelManager.loadEcoreFile("testecores/TestEcoreForUsages2.ecore");
		modelManager.loadEcoreFile("testecores/TestEcoreForUsages3.ecore");
		modelManager.loadEcoreFile("testecores/TestEcoreForUsages4.ecore");
		var package1 = edelta.getEPackage("testecoreforusages1");
		var package2 = edelta.getEPackage("testecoreforusages2");
		var package3 = edelta.getEPackage("testecoreforusages3");
		var package4 = edelta.getEPackage("testecoreforusages4");
		assertThat(package1).isNotNull();
		assertThat(package2).isNotNull();
		assertThat(package3).isNotNull();
		assertThat(package4).isNotNull();
		assertThat(EdeltaUtils.usedPackages(package1))
			.containsExactlyInAnyOrder(package2);
		assertThat(EdeltaUtils.usedPackages(package2))
			.containsExactlyInAnyOrder(package1);
		assertThat(EdeltaUtils.usedPackages(package3))
			.containsExactlyInAnyOrder(package2);
		assertThat(EdeltaUtils.usedPackages(package4))
			.containsExactlyInAnyOrder(package2, package1);
	}

	@Test
	public void test_getEContainingPackage() {
		var ePackage = ecoreFactory.createEPackage();
		assertThat(EdeltaUtils.getEContainingPackage(ePackage))
			.isSameAs(ePackage);
		var eClass = ecoreFactory.createEClass();
		assertThat(EdeltaUtils.getEContainingPackage(eClass))
			.isNull();
		ePackage.getEClassifiers().add(eClass);
		assertThat(EdeltaUtils.getEContainingPackage(eClass))
			.isSameAs(ePackage);
		var feature = ecoreFactory.createEAttribute();
		assertThat(EdeltaUtils.getEContainingPackage(feature))
			.isNull();
		eClass.getEStructuralFeatures().add(feature);
		assertThat(EdeltaUtils.getEContainingPackage(feature))
			.isSameAs(ePackage);
	}

	@Test
	public void testFindSiblingByName() {
		var pack = EcoreFactory.eINSTANCE.createEPackage();

		var c1 = EcoreFactory.eINSTANCE.createEClass();
		c1.setName("C1");
		var c2 = EcoreFactory.eINSTANCE.createEClass();
		c2.setName("C2");
		var c3 = EcoreFactory.eINSTANCE.createEClass();
		c3.setName("C3");
		pack.getEClassifiers().addAll(List.of(c1, c2, c3));

		assertThat(EdeltaUtils.findSiblingByName(c1, "C1"))
			.isSameAs(c1);
		assertThat(EdeltaUtils.findSiblingByName(c1, "C2"))
			.isSameAs(c2);
		assertThat(EdeltaUtils.findSiblingByName(c1, "C3"))
			.isSameAs(c3);
		assertThat(EdeltaUtils.findSiblingByName(c1, "NonExisting"))
			.isNull();
	}
}
