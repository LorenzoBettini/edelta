package edelta.petrinet.tests;

import org.junit.Test;

import edelta.petrinet.example.PetrinetExample;
import edelta.testutils.EdeltaTestUtils;

public class PetrinetExampleTest {
	@Test
	public void testPetrinet() throws Exception {
		// Create an instance of the generated Java class
		var edelta = new PetrinetExample();
		// Make sure you load all the used Ecores
		edelta.loadEcoreFile("model/Petrinet.ecore");
		// Execute the actual transformations defined in the DSL
		edelta.execute();
		// Save the modified Ecore model into a new path
		edelta.saveModifiedEcores("modified");

		EdeltaTestUtils.assertFilesAreEquals(
			"expectations/Petrinet.ecore",
			"modified/Petrinet.ecore");
	}
}
