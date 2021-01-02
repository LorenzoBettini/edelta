package edelta.twopackages.example.tests;

import org.junit.Test;

import edelta.testutils.EdeltaTestUtils;
import edelta.twopackages.example.TwoPackagesExample;

public class TwoPackagesExampleTest {
	@Test
	public void testTwoPackages() throws Exception {
		// Create an instance of the generated Java class
		var edelta = new TwoPackagesExample();
		// Make sure you load all the used Ecores
		edelta.loadEcoreFile("model/Person.ecore");
		edelta.loadEcoreFile("model/WorkPlace.ecore");
		// Execute the actual transformations defined in the DSL
		edelta.execute();
		// Save the modified Ecore model into a new path
		edelta.saveModifiedEcores("modified");

		EdeltaTestUtils.assertFilesAreEquals(
			"expectations/Person.ecore",
			"modified/Person.ecore");
		EdeltaTestUtils.assertFilesAreEquals(
			"expectations/WorkPlace.ecore",
			"modified/WorkPlace.ecore");
	}
}
