package edelta.tests;

import static org.assertj.core.api.Assertions.assertThat;

import org.eclipse.xtext.common.types.TypesFactory;
import org.junit.jupiter.api.Test;

import edelta.jvmmodel.EdeltaJvmModelHelper;

class EdeltaJvmModelHelperTest {

	private final EdeltaJvmModelHelper helper = new EdeltaJvmModelHelper();

	private final TypesFactory typesFactory = TypesFactory.eINSTANCE;

	@Test
	void testMissingOperation() {
		var type = typesFactory.createJvmGenericType();
		assertThat(helper.findJvmOperation(type, "missing")).isNull();

		var field = typesFactory.createJvmField();
		field.setSimpleName("missing");
		type.getMembers().add(field);
		assertThat(helper.findJvmOperation(type, "missing")).isNull();
	}

	@Test
	void testFirstMatchingOperationSkipsFieldsAndOtherNames() {
		var type = typesFactory.createJvmGenericType();
		var field = typesFactory.createJvmField();
		field.setSimpleName("target");
		var other = typesFactory.createJvmOperation();
		other.setSimpleName("other");
		var first = typesFactory.createJvmOperation();
		first.setSimpleName("target");
		var second = typesFactory.createJvmOperation();
		second.setSimpleName("target");
		type.getMembers().add(field);
		type.getMembers().add(other);
		type.getMembers().add(first);
		type.getMembers().add(second);

		assertThat(helper.findJvmOperation(type, "target")).isSameAs(first);
	}

	@Test
	void testInheritedOperation() {
		var parent = typesFactory.createJvmGenericType();
		var operation = typesFactory.createJvmOperation();
		operation.setSimpleName("inherited");
		parent.getMembers().add(operation);
		var type = typesFactory.createJvmGenericType();
		var superType = typesFactory.createJvmParameterizedTypeReference();
		superType.setType(parent);
		type.getSuperTypes().add(superType);

		assertThat(helper.findJvmOperation(type, "inherited")).isSameAs(operation);
	}
}
