package com.example2;

import edelta.lib.AbstractEdelta;

public class Main {

	public static void main(String[] args) throws Exception {
		// Create an instance of the generated Java class
		AbstractEdelta edelta = new Example2();
		// Make sure you load all the used Ecores (Ecore.ecore is always loaded)
		edelta.loadEcoreFile("model/My2.ecore");
		edelta.loadEcoreFile("../edelta.testprojects.first/model/My1.ecore");
		// Execute the actual transformations defined in the DSL
		edelta.execute();
		// Save the modified Ecore model into a new path
		edelta.saveModifiedEcores("modified");
	}
}
