package edelta.personlist.example;

import edelta.lib.AbstractEdelta;

public class MainSyntax1 {

	public static void main(String[] args) throws Exception {
		// Create an instance of the generated Java class
		AbstractEdelta edelta = new ExampleSyntax1();
		// Make sure you load all the used Ecores
		edelta.loadEcoreFile("model/PersonList.ecore");
		// Execute the actual transformations defined in the DSL
		edelta.execute();
		// Save the modified Ecore model into a new path
		edelta.saveModifiedEcores("modified-syntax1");
	}
}
