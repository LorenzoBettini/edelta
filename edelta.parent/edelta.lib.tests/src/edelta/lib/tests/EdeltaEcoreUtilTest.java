package edelta.lib.tests;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

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
}
