package edelta.stdlib.examples;

import edelta.lib.EdeltaEngine;

public class ChangeReferenceTypeExampleMain {

	public static void main(String[] args) throws Exception {
		// create the engine specifying the generated Java class
		EdeltaEngine engine = new EdeltaEngine(ChangeReferenceTypeExample::new);
		// Make sure you load all the used Ecores (Ecore.ecore is always loaded)
		engine.loadEcoreFile("model/PersonListForChangeType.ecore");
		// and the model files you want to migrate
		engine.loadModelFile("model/ListForChangeType.xmi");
		// Execute the actual transformations defined in the DSL
		engine.execute();
		// Save the modified Ecores and models into a new path
		engine.save("modified");
	}
}
