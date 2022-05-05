package edelta.extractclass.example;

import edelta.lib.EdeltaEngine;

public class PersonExtractAddressExampleMain {

	public static void main(String[] args) throws Exception {
		// create the engine specifying the generated Java class
		EdeltaEngine engine = new EdeltaEngine(PersonExtractAddressExample::new);
		// Make sure you load all the used Ecores (Ecore.ecore is always loaded)
		engine.loadEcoreFile("model/AddressBook2.ecore");
		engine.loadModelFile("model/AddressBook2.xmi");
		// Execute the actual transformations defined in the DSL
		engine.execute();
		// Save the modified Ecores and models into a new path
		engine.save("modified");
	}
}
