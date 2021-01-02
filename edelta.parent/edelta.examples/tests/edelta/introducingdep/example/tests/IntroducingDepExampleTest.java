package edelta.introducingdep.example.tests;

import org.junit.Test;

import edelta.introducingdep.example.IntroducingDepModifExample;
import edelta.testutils.EdeltaTestUtils;

public class IntroducingDepExampleTest {
	@Test
	public void testIntroducingDep() throws Exception {
		// Create an instance of the generated Java class
		var edelta = new IntroducingDepModifExample();
		// Make sure you load all the used Ecores
		edelta.loadEcoreFile("model/AnotherSimple.ecore");
		edelta.loadEcoreFile("model/Simple.ecore");
		// Execute the actual transformations defined in the DSL
		edelta.execute();
		// Save the modified Ecore model into a new path
		edelta.saveModifiedEcores("modified");

		EdeltaTestUtils.assertFilesAreEquals(
			"expectations/Simple.ecore",
			"modified/Simple.ecore");
		EdeltaTestUtils.assertFilesAreEquals(
			"expectations/AnotherSimple.ecore",
			"modified/AnotherSimple.ecore");
	}
}
