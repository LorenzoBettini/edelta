package edelta.statechartsenum.tests;

import static java.util.List.of;

import org.junit.Test;

import edelta.examples.tests.AbstractEdeltaExamplesTest;
import edelta.statechartsenum.example.StateChartsEnumExample;

public class StateChartsEnumExampleTest extends AbstractEdeltaExamplesTest {

	@Test
	public void testStateChartsEnum() throws Exception {
		var subdir = "";
		var ecores = of("StateChartsEnum.ecore");
		var models = of("StateChartsEnumModel.xmi");

		var engine = setupEngine(
			ecores,
			models,
			StateChartsEnumExample::new);

		assertOutputs(engine,
			subdir,
			ecores,
			models
		);
	}

}
