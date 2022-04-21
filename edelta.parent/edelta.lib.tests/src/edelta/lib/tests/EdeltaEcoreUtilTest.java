package edelta.lib.tests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EcoreFactory;
import org.eclipse.emf.ecore.EcorePackage;
import org.junit.Test;

import edelta.lib.EdeltaEcoreUtil;

public class EdeltaEcoreUtilTest {

	@Test
	public void testWrapAsCollection() {
		var pack = EcoreFactory.eINSTANCE.createEPackage();
		Collection<Object> collection;

		collection = EdeltaEcoreUtil.wrapAsCollection(
				pack, EcorePackage.Literals.ENAMED_ELEMENT__NAME, -1);

		assertThat(collection)
			.isEmpty();

		pack.setName("A name");

		collection = EdeltaEcoreUtil.wrapAsCollection(
				pack, EcorePackage.Literals.ENAMED_ELEMENT__NAME, -1);

		assertThat(collection)
			.containsExactly("A name");

		collection = EdeltaEcoreUtil.wrapAsCollection(
				pack, EcorePackage.Literals.EPACKAGE__ECLASSIFIERS, -1);

		assertThat(collection)
			.isSameAs(pack.getEClassifiers())
			.isEmpty();

		var c1 = EcoreFactory.eINSTANCE.createEClass();
		var c2 = EcoreFactory.eINSTANCE.createEDataType();
		var c3 = EcoreFactory.eINSTANCE.createEEnum();
		pack.getEClassifiers().addAll(List.of(c1, c2, c3));

		collection = EdeltaEcoreUtil.wrapAsCollection(
				pack, EcorePackage.Literals.EPACKAGE__ECLASSIFIERS, -1);
		assertThat(collection)
			.isSameAs(pack.getEClassifiers())
			.containsExactlyInAnyOrder(c1, c2, c3);

		collection = EdeltaEcoreUtil.wrapAsCollection(
				pack, EcorePackage.Literals.EPACKAGE__ECLASSIFIERS, 1);
		assertThat(collection)
			.isNotSameAs(pack.getEClassifiers())
			.containsExactly(c1);

		collection = EdeltaEcoreUtil.wrapAsCollection(
				pack, EcorePackage.Literals.EPACKAGE__ECLASSIFIERS, 2);
		assertThat(collection)
			.isNotSameAs(pack.getEClassifiers())
			.containsExactlyInAnyOrder(c1, c2);
	}

	@Test
	public void testUnwrapCollection() {
		var pack = EcoreFactory.eINSTANCE.createEPackage();
		Object result;

		result = EdeltaEcoreUtil.unwrapCollection(
				pack.getName(),
				EcorePackage.Literals.ENAMED_ELEMENT__NAME);

		assertThat(result)
			.isNull();

		result = EdeltaEcoreUtil.unwrapCollection(
				pack.getEClassifiers(),
				EcorePackage.Literals.EPACKAGE__ECLASSIFIERS);

		assertThat(result)
			.asList()
			.isEmpty();

		pack.setName("A name");

		result = EdeltaEcoreUtil.unwrapCollection(
				pack.getName(),
				EcorePackage.Literals.ENAMED_ELEMENT__NAME);

		assertThat(result)
			.isEqualTo("A name");

		result = EdeltaEcoreUtil.unwrapCollection(
				List.of(pack.getName()),
				EcorePackage.Literals.ENAMED_ELEMENT__NAME);

		assertThat(result)
			.isEqualTo("A name");

		result = EdeltaEcoreUtil.unwrapCollection(
				List.of(pack.getName(), "another name"),
				EcorePackage.Literals.ENAMED_ELEMENT__NAME);

		assertThat(result)
			.isEqualTo("A name");

		result = EdeltaEcoreUtil.unwrapCollection(
				Collections.emptyList(),
				EcorePackage.Literals.ENAMED_ELEMENT__NAME);

		assertThat(result)
			.isNull();
	}

	@Test
	public void testGetValueForFeature() {
		var pack = EcoreFactory.eINSTANCE.createEPackage();
		Collection<Object> collection;

		collection = EdeltaEcoreUtil.getValueForFeature(pack,
				EcorePackage.Literals.ENAMED_ELEMENT__NAME, -1);

		assertThat(collection)
			.isEmpty();
	
		pack.setName("A name");
	
		collection = EdeltaEcoreUtil.getValueForFeature(
				pack, EcorePackage.Literals.ENAMED_ELEMENT__NAME, -1);
	
		assertThat(collection)
			.containsExactly("A name");
	
		collection = EdeltaEcoreUtil.getValueForFeature(
				pack, EcorePackage.Literals.EPACKAGE__ECLASSIFIERS, -1);
	
		assertThat(collection)
			.isSameAs(pack.getEClassifiers())
			.isEmpty();
	
		var c1 = EcoreFactory.eINSTANCE.createEClass();
		var c2 = EcoreFactory.eINSTANCE.createEDataType();
		var c3 = EcoreFactory.eINSTANCE.createEEnum();
		pack.getEClassifiers().addAll(List.of(c1, c2, c3));

		collection = EdeltaEcoreUtil.getValueForFeature(
				pack, EcorePackage.Literals.EPACKAGE__ECLASSIFIERS, -1);
		assertThat(collection)
			.isSameAs(pack.getEClassifiers())
			.containsExactlyInAnyOrder(c1, c2, c3);

		collection = EdeltaEcoreUtil.getValueForFeature(
				pack, EcorePackage.Literals.EPACKAGE__ECLASSIFIERS, 1);
		assertThat(collection)
			.isNotSameAs(pack.getEClassifiers())
			.containsExactlyInAnyOrder(c1);

		collection = EdeltaEcoreUtil.getValueForFeature(
				pack, EcorePackage.Literals.EPACKAGE__ECLASSIFIERS, 2);
		assertThat(collection)
			.isNotSameAs(pack.getEClassifiers())
			.containsExactlyInAnyOrder(c1, c2);
	}

	@Test
	public void testSetValueForFeature() {
		var pack = EcoreFactory.eINSTANCE.createEPackage();

		EdeltaEcoreUtil.setValueForFeature(pack,
				EcorePackage.Literals.ENAMED_ELEMENT__NAME,
				"A name");

		assertThat(pack.getName())
			.isEqualTo("A name");

		EdeltaEcoreUtil.setValueForFeature(pack,
				EcorePackage.Literals.ENAMED_ELEMENT__NAME,
				List.of("Another name"));

		assertThat(pack.getName())
			.isEqualTo("Another name");

		var c1 = EcoreFactory.eINSTANCE.createEClass();
		var c2 = EcoreFactory.eINSTANCE.createEDataType();
		List<EClassifier> collection = List.of(c1, c2);
		EdeltaEcoreUtil.setValueForFeature(pack,
				EcorePackage.Literals.EPACKAGE__ECLASSIFIERS,
				collection);
		assertThat(pack.getEClassifiers())
			.isNotSameAs(collection)
			.containsExactlyInAnyOrder(c1, c2);

		var c3 = EcoreFactory.eINSTANCE.createEEnum();
		EdeltaEcoreUtil.setValueForFeature(pack,
				EcorePackage.Literals.EPACKAGE__ECLASSIFIERS,
				c3);
		assertThat(pack.getEClassifiers())
			.containsExactlyInAnyOrder(c3);

		EdeltaEcoreUtil.setValueForFeature(pack,
				EcorePackage.Literals.EPACKAGE__ECLASSIFIERS,
				null);
		assertThat(pack.getEClassifiers())
			.isEmpty();
	}

	@Test
	public void testCreateInstance() {
		var instance = EdeltaEcoreUtil.createInstance(
			EcorePackage.eINSTANCE.getEDataType(),
			o -> o.eSet(EcorePackage.eINSTANCE.getENamedElement_Name(), "a name")
		);
		assertEquals("a name",
			instance.eGet(EcorePackage.eINSTANCE.getENamedElement_Name()));
		instance = EdeltaEcoreUtil.createInstance(
			EcorePackage.eINSTANCE.getEDataType());
		assertNull(
			instance.eGet(EcorePackage.eINSTANCE.getENamedElement_Name()));
	}

	@Test
	public void testGetValueAsList() {
		var pack = EcoreFactory.eINSTANCE.createEPackage();

		var collection = EdeltaEcoreUtil.getValueAsList(
				pack, EcorePackage.Literals.EPACKAGE__ECLASSIFIERS);
	
		assertThat(collection)
			.isSameAs(pack.getEClassifiers())
			.isEmpty();
	
		var c1 = EcoreFactory.eINSTANCE.createEClass();
		var c2 = EcoreFactory.eINSTANCE.createEDataType();
		var c3 = EcoreFactory.eINSTANCE.createEEnum();
		pack.getEClassifiers().addAll(List.of(c1, c2, c3));

		collection = EdeltaEcoreUtil.getValueAsList(
				pack, EcorePackage.Literals.EPACKAGE__ECLASSIFIERS);
		assertThat(collection)
			.isSameAs(pack.getEClassifiers())
			.containsExactlyInAnyOrder(c1, c2, c3);
	}

	@Test
	public void testGetValueAsEObject() {
		var attr = EcoreFactory.eINSTANCE.createEAttribute();
		attr.setEType(EcorePackage.Literals.ESTRING);

		EObject o = EdeltaEcoreUtil.getValueAsEObject(attr,
			EcorePackage.Literals.ETYPED_ELEMENT__ETYPE);

		assertThat(o)
			.isSameAs(EcorePackage.Literals.ESTRING);
	}

	@Test
	public void testSetValueFrom() {
		var src = EcoreFactory.eINSTANCE.createEAttribute();
		src.setName("anAttribute");
		src.setEType(EcorePackage.Literals.ESTRING);

		var dest = EcoreFactory.eINSTANCE.createEAttribute();
		EdeltaEcoreUtil.setValueFrom(
			dest,
			EcorePackage.Literals.ETYPED_ELEMENT__ETYPE,
			src,
			EcorePackage.Literals.ETYPED_ELEMENT__ETYPE);

		assertThat(dest.getEAttributeType())
			.isSameAs(EcorePackage.Literals.ESTRING);

		assertThatThrownBy(() -> 
			EdeltaEcoreUtil.setValueFrom(
				dest,
				EcorePackage.Literals.ETYPED_ELEMENT__ETYPE,
				src,
				EcorePackage.Literals.ENAMED_ELEMENT__NAME))
			.isInstanceOf(ClassCastException.class)
			.hasMessageContaining("class java.lang.String cannot be cast to class org.eclipse.emf.ecore.EClassifier");
	}

	@Test
	public void testGetValueFromFeatureName() {
		var attr = EcoreFactory.eINSTANCE.createEAttribute();
		attr.setEType(EcorePackage.Literals.ESTRING);

		Object o = EdeltaEcoreUtil.getValueFromFeatureName(attr,
			EcorePackage.Literals.ETYPED_ELEMENT__ETYPE.getName());

		assertThat(o)
			.isSameAs(EcorePackage.Literals.ESTRING);
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

		assertThat(EdeltaEcoreUtil.findSiblingByName(c1, "C1"))
			.isSameAs(c1);
		assertThat(EdeltaEcoreUtil.findSiblingByName(c1, "C2"))
			.isSameAs(c2);
		assertThat(EdeltaEcoreUtil.findSiblingByName(c1, "C3"))
			.isSameAs(c3);
		assertThat(EdeltaEcoreUtil.findSiblingByName(c1, "NonExisting"))
			.isNull();;
	}
}
