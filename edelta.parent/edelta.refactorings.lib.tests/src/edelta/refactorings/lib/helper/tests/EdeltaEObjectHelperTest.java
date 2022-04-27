package edelta.refactorings.lib.helper.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.eclipse.emf.ecore.EcoreFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import edelta.refactorings.lib.helper.EdeltaEObjectHelper;

class EdeltaEObjectHelperTest {

	private EdeltaEObjectHelper edeltaEObjectHelper;

	@BeforeEach
	void setup() {
		edeltaEObjectHelper = new EdeltaEObjectHelper();
	}

	@Test
	void testRepresentationWithNull() {
		assertNull(edeltaEObjectHelper.represent(null));
	}

	@Test
	void testRepresentation() {
		var o = EcoreFactory.eINSTANCE.createEClass();
		o.setName("AClass");
		o.setAbstract(true);
		String repr = edeltaEObjectHelper.represent(o);
		assertEquals("name = AClass, abstract = true, interface = false", repr);
	}

}
