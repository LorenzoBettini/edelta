package edelta.refactorings.lib.helper.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.eclipse.emf.ecore.EcoreFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import edelta.lib.EdeltaModelManager;
import edelta.refactorings.lib.helper.EdeltaEObjectHelper;

class EdeltaEObjectHelperTest {

	private static final String TESTECORES = "../edelta.testdata/testdata/";

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

	@Test
	void testPositionInContainer() {
		var o = EcoreFactory.eINSTANCE.createEClass();
		assertEquals("", edeltaEObjectHelper.positionInContainter(o));
		var p = EcoreFactory.eINSTANCE.createEPackage();
		p.getEClassifiers().add(o);
		assertEquals("1 / 1", edeltaEObjectHelper.positionInContainter(o));
	}

	@Test
	void testPositionInContainerNotList() {
		EdeltaModelManager modelManager = new EdeltaModelManager();
		var subdir = "simpleTestData/";
		modelManager.loadEcoreFile(TESTECORES + subdir + "SingleContainment.ecore");
		var resource = modelManager.loadModelFile(TESTECORES + subdir + "SingleContainmentModel.xmi");
		var contained = resource.getContents().get(0).eContents().get(0);
		assertEquals("", edeltaEObjectHelper.positionInContainter(contained));
	}

}
