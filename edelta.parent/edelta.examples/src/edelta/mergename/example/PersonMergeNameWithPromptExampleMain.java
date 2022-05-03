package edelta.mergename.example;

import edelta.lib.EdeltaEngine;

public class PersonMergeNameWithPromptExampleMain {

	public static void main(String[] args) throws Exception {
		// create the engine specifying the generated Java class
		EdeltaEngine engine = new EdeltaEngine(PersonMargeNameWithPromptExample::new);
		// Make sure you load all the used Ecores (Ecore.ecore is always loaded)
		engine.loadEcoreFile("model/AddressBook.ecore");
		engine.loadModelFile("model/AddressBook.xmi");
		// Execute the actual transformations defined in the DSL
		engine.execute();
		// Save the modified Ecores and models into a new path
		engine.save("modified");
	}
}
