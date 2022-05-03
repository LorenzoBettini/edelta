package edelta.statecharts.tests;

import static java.util.List.of;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import edelta.examples.tests.AbstractEdeltaExamplesWithPromptTest;
import edelta.statecharts.example.StateChartsWithPromptExample;

public class StateChartsWithPromptExampleTest extends AbstractEdeltaExamplesWithPromptTest {

	@Test
	void testStateChartsWithPrompt() throws Exception {
		var subdir = "";
		var ecores = of("StateCharts.ecore");
		var models = of("StateChartsModel.xmi");

		enterInput("1\n1\n2\n3\n2\n1\n2\n3\n3");

		// must complete within 5 seconds
		assertTimeoutPreemptively(Duration.ofSeconds(5), () -> {
			var engine = setupEngine(
				ecores,
				models,
				StateChartsWithPromptExample::new);
	
			assertOutputs(engine,
				subdir,
				ecores,
				models
			);
		});
	}
}
