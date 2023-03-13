package edelta.statecharts.example;

import edelta.lib.EdeltaEngine;

public class StateChartsWithPromptExampleMain {

	public static void main(String[] args) throws Exception {
		// create the engine specifying the generated Java class
		EdeltaEngine engine = new EdeltaEngine(StateChartsWithPromptExample::new);
		// Make sure you load all the used Ecores (Ecore.ecore is always loaded)
		engine.loadEcoreFile("model/StateCharts.ecore");
		engine.loadModelFile("model/StateChartsModel.xmi");
		// Execute the actual transformations defined in the DSL
		engine.execute();
		// Save the modified Ecores and models into a new path
		engine.save("modified/interactive");
	}
}
