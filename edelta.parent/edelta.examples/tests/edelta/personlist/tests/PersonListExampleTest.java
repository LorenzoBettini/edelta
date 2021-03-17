package edelta.personlist.tests;

import org.junit.Test;

import edelta.personlist.example.PersonListExample;
import edelta.testutils.EdeltaTestUtils;

public class PersonListExampleTest {
	@Test
	public void testPersonList() throws Exception {
		// Create an instance of the generated Java class
		var edelta = new PersonListExample();
		// Make sure you load all the used Ecores
		edelta.loadEcoreFile("model/PersonList.ecore");
		// Execute the actual transformations defined in the DSL
		edelta.execute();
		// Save the modified Ecore model into a new path
		edelta.saveModifiedEcores("modified");

		EdeltaTestUtils.assertFilesAreEquals(
			"expectations/PersonList.ecore",
			"modified/PersonList.ecore");
	}
}
