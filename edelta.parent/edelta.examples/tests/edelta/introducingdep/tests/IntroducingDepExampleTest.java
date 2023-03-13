package edelta.introducingdep.tests;

import org.junit.Test;

import edelta.introducingdep.example.IntroducingDepModifExample;
import edelta.lib.EdeltaEngine;
import edelta.testutils.EdeltaTestUtils;

public class IntroducingDepExampleTest {
	@Test
	public void testIntroducingDep() throws Exception {
		// create the engine specifying the generated Java class
		EdeltaEngine engine = new EdeltaEngine(IntroducingDepModifExample::new);
		// Make sure you load all the used Ecores (Ecore.ecore is always loaded)
		engine.loadEcoreFile("model/AnotherSimple.ecore");
		engine.loadEcoreFile("model/Simple.ecore");
		// Execute the actual transformations defined in the DSL
		engine.execute();
		// Save the modified Ecores and models into a new path
		engine.save("modified");

		EdeltaTestUtils.assertFilesAreEquals(
			"expectations/Simple.ecore",
			"modified/Simple.ecore");
		EdeltaTestUtils.assertFilesAreEquals(
			"expectations/AnotherSimple.ecore",
			"modified/AnotherSimple.ecore");
	}
}
