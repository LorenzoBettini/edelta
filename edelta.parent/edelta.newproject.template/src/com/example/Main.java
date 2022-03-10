package com.example;

import edelta.lib.EdeltaEngine;

public class Main {

	public static void main(String[] args) throws Exception {
		// create the engine specifying the generated Java class
		EdeltaEngine engine = new EdeltaEngine(Example::new);
		// Make sure you load all the used Ecores (Ecore.ecore is always loaded)
		engine.loadEcoreFile("model/My.ecore");
		// Execute the actual transformations defined in the DSL
		engine.execute();
		// Save the modified Ecore model into a new path
		engine.save("modified");
	}
}
