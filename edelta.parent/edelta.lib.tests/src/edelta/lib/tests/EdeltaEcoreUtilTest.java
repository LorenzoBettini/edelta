package edelta.lib.tests;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.eclipse.emf.ecore.EClassifier;
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
				pack, EcorePackage.Literals.ENAMED_ELEMENT__NAME);

		assertThat(collection)
			.isEmpty();

		pack.setName("A name");

		collection = EdeltaEcoreUtil.wrapAsCollection(
				pack, EcorePackage.Literals.ENAMED_ELEMENT__NAME);

		assertThat(collection)
			.containsExactly("A name");

		collection = EdeltaEcoreUtil.wrapAsCollection(
				pack, EcorePackage.Literals.EPACKAGE__ECLASSIFIERS);

		assertThat(collection)
			.isSameAs(pack.getEClassifiers())
			.isEmpty();

		var c1 = EcoreFactory.eINSTANCE.createEClass();
		var c2 = EcoreFactory.eINSTANCE.createEDataType();
		pack.getEClassifiers().addAll(List.of(c1, c2));
		collection = EdeltaEcoreUtil.wrapAsCollection(
				pack, EcorePackage.Literals.EPACKAGE__ECLASSIFIERS);
		assertThat(collection)
			.isSameAs(pack.getEClassifiers())
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
				EcorePackage.Literals.ENAMED_ELEMENT__NAME);

		assertThat(collection)
			.isEmpty();
	
		pack.setName("A name");
	
		collection = EdeltaEcoreUtil.getValueForFeature(
				pack, EcorePackage.Literals.ENAMED_ELEMENT__NAME);
	
		assertThat(collection)
			.containsExactly("A name");
	
		collection = EdeltaEcoreUtil.getValueForFeature(
				pack, EcorePackage.Literals.EPACKAGE__ECLASSIFIERS);
	
		assertThat(collection)
			.isSameAs(pack.getEClassifiers())
			.isEmpty();
	
		var c1 = EcoreFactory.eINSTANCE.createEClass();
		var c2 = EcoreFactory.eINSTANCE.createEDataType();
		pack.getEClassifiers().addAll(List.of(c1, c2));
		collection = EdeltaEcoreUtil.getValueForFeature(
				pack, EcorePackage.Literals.EPACKAGE__ECLASSIFIERS);
		assertThat(collection)
			.isSameAs(pack.getEClassifiers())
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
	public void testGetExtraValues() {
		Collection<Object> extraValues;

		var values = List.of(1, 2, 3);

		extraValues = EdeltaEcoreUtil.getExtraValues(
				values,
				EcorePackage.Literals.EPACKAGE__ECLASSIFIERS);

		assertThat(extraValues)
			.containsExactly(1, 2, 3);

		extraValues = EdeltaEcoreUtil.getExtraValues(
				values,
				EcorePackage.Literals.ENAMED_ELEMENT__NAME);

		assertThat(extraValues)
			.containsExactly(2, 3);

		extraValues = EdeltaEcoreUtil.getExtraValues(
				List.of(1),
				EcorePackage.Literals.ECLASSIFIER__EPACKAGE);

		assertThat(extraValues)
			.isEmpty();;
	}
}