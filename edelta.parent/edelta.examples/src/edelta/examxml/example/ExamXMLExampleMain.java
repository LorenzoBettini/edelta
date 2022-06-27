package edelta.examxml.example;

import edelta.lib.EdeltaEngine;

public class ExamXMLExampleMain {

	public static void main(String[] args) throws Exception {
		// create the engine specifying the generated Java class
		EdeltaEngine engine = new EdeltaEngine(ExamXMLExample::new);
		// Make sure you load all the used Ecores (Ecore.ecore is always loaded)
		engine.loadEcoreFile("model/ExamXML.ecore");
		engine.loadModelFile("model/Exam.xmi");
		// Execute the actual transformations defined in the DSL
		engine.execute();
		// Save the modified Ecores and models into a new path
		engine.save("modified");
	}
}
