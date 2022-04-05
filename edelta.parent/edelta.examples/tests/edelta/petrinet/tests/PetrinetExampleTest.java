package edelta.petrinet.tests;

import org.junit.Test;

import edelta.lib.EdeltaEngine;
import edelta.petrinet.example.PetrinetExample;
import edelta.testutils.EdeltaTestUtils;

public class PetrinetExampleTest {
	@Test
	public void testPetrinet() throws Exception {
		// create the engine specifying the generated Java class
		EdeltaEngine engine = new EdeltaEngine(PetrinetExample::new);
		// Make sure you load all the used Ecores (Ecore.ecore is always loaded)
		engine.loadEcoreFile("model/Petrinet.ecore");
		// Execute the actual transformations defined in the DSL
		engine.execute();
		// Save the modified Ecores and models into a new path
		engine.save("modified");

		EdeltaTestUtils.assertFilesAreEquals(
			"expectations/Petrinet.ecore",
			"modified/Petrinet.ecore");
	}

}
