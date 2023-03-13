package edelta.statecharts.tests;

import static java.util.List.of;

import org.junit.Test;

import edelta.examples.tests.AbstractEdeltaExamplesTest;
import edelta.statecharts.example.StateChartsExample;

public class StateChartsExampleTest extends AbstractEdeltaExamplesTest {

	@Test
	public void testStateCharts() throws Exception {
		var subdir = "";
		var ecores = of("StateCharts.ecore");
		var models = of("StateChartsModel.xmi");

		var engine = setupEngine(
			ecores,
			models,
			StateChartsExample::new);

		assertOutputs(engine,
			subdir,
			ecores,
			models
		);
	}

}
