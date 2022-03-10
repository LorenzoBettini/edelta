package edelta.personlist.tests;

import org.junit.Test;

import edelta.lib.EdeltaEngine;
import edelta.personlist.example.PersonListExample;
import edelta.testutils.EdeltaTestUtils;

public class PersonListExampleTest {
	@Test
	public void testPersonList() throws Exception {
		// create the engine specifying the generated Java class
		EdeltaEngine engine = new EdeltaEngine(PersonListExample::new);
		// Make sure you load all the used Ecores (Ecore.ecore is always loaded)
		engine.loadEcoreFile("model/PersonList.ecore");
		// Execute the actual transformations defined in the DSL
		engine.execute();
		// Save the modified Ecores and models into a new path
		engine.save("modified");

		EdeltaTestUtils.assertFilesAreEquals(
			"expectations/PersonList.ecore",
			"modified/PersonList.ecore");
	}
}
