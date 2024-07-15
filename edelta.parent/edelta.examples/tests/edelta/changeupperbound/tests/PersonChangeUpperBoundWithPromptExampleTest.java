package edelta.changeupperbound.tests;

import static java.util.List.of;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import edelta.changeupperbound.example.PersonListChangeUpperBoundExample;
import edelta.examples.tests.AbstractEdeltaExamplesWithPromptTest;

class PersonChangeUpperBoundWithPromptExampleTest extends AbstractEdeltaExamplesWithPromptTest {
	@Test
	void testChangeUpperBoundInteractive() {
		var subdir = "";
		var ecores = of("PersonListForChangeUpperBound.ecore");
		var models = of("ListForChangeUpperBound.xmi");

		enterInput("2\n3\n");

		// must complete within 5 seconds
		assertTimeoutPreemptively(Duration.ofSeconds(5), () -> {
			var engine = setupEngine(
				ecores,
				models,
				PersonListChangeUpperBoundExample::new);
	
			assertOutputs(engine,
				subdir,
				ecores,
				models
			);
		});
	}
}
