package edelta.introducingdep.example;

import edelta.lib.AbstractEdelta;

public class IntroducingDepExampleMain {

	public static void main(String[] args) throws Exception {
		// Create an instance of the generated Java class
		AbstractEdelta edelta = new IntroducingDepModifExample();
		// Make sure you load all the used Ecores
		edelta.loadEcoreFile("model/AnotherSimple.ecore");
		edelta.loadEcoreFile("model/Simple.ecore");
		// Execute the actual transformations defined in the DSL
		edelta.execute();
		// Save the modified Ecore model into a new path
		edelta.saveModifiedEcores("modified");
	}
}
