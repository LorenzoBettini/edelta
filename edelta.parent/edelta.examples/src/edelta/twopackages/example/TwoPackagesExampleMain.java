package edelta.twopackages.example;

import edelta.lib.EdeltaEngine;

public class TwoPackagesExampleMain {

	public static void main(String[] args) throws Exception {
		// create the engine specifying the generated Java class
		EdeltaEngine engine = new EdeltaEngine(TwoPackagesExample::new);
		// Make sure you load all the used Ecores (Ecore.ecore is always loaded)
		engine.loadEcoreFile("model/Person.ecore");
		engine.loadEcoreFile("model/WorkPlace.ecore");
		// Execute the actual transformations defined in the DSL
		engine.execute();
		// Save the modified Ecores and models into a new path
		engine.save("modified");
	}
}
