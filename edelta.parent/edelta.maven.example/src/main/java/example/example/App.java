package example.example;

import com.example.Example;

import edelta.lib.AbstractEdelta;

/**
 * Hello world!
 *
 */
public class App {
	public static void main(String[] args) throws Exception {
		// Create an instance of the generated Java class
		AbstractEdelta edelta = new Example();
		// Make sure you load all the used Ecores
		edelta.loadEcoreFile("model/My.ecore");
		// Execute the actual transformations defined in the DSL
		edelta.execute();
		// Save the modified Ecore model into a new path
		edelta.saveModifiedEcores("modified");
	}
}
