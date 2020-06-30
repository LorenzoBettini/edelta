package edelta.twopackages.example;

import edelta.lib.AbstractEdelta;

public class TwoPackagesExampleMain {

	public static void main(String[] args) throws Exception {
		// Create an instance of the generated Java class
		AbstractEdelta edelta = new TwoPackagesExample();
		// Make sure you load all the used Ecores
		edelta.loadEcoreFile("model/Person.ecore");
		edelta.loadEcoreFile("model/WorkPlace.ecore");
		// Execute the actual transformations defined in the DSL
		edelta.execute();
		// Save the modified Ecore model into a new path
		edelta.saveModifiedEcores("modified");
	}
}
