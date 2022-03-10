package edelta.twopackages.tests;

import org.junit.Test;

import edelta.lib.EdeltaEngine;
import edelta.testutils.EdeltaTestUtils;
import edelta.twopackages.example.TwoPackagesExample;

public class TwoPackagesExampleTest {
	@Test
	public void testTwoPackages() throws Exception {
		// create the engine specifying the generated Java class
		EdeltaEngine engine = new EdeltaEngine(TwoPackagesExample::new);
		// Make sure you load all the used Ecores (Ecore.ecore is always loaded)
		engine.loadEcoreFile("model/Person.ecore");
		engine.loadEcoreFile("model/WorkPlace.ecore");
		// Execute the actual transformations defined in the DSL
		engine.execute();
		// Save the modified Ecores and models into a new path
		engine.save("modified");

		EdeltaTestUtils.assertFilesAreEquals(
			"expectations/Person.ecore",
			"modified/Person.ecore");
		EdeltaTestUtils.assertFilesAreEquals(
			"expectations/WorkPlace.ecore",
			"modified/WorkPlace.ecore");
	}
}
