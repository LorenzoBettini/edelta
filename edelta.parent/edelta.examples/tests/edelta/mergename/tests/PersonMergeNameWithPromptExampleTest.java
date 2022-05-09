package edelta.mergename.tests;

import static java.util.List.of;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import edelta.examples.tests.AbstractEdeltaExamplesWithPromptTest;
import edelta.mergename.example.PersonMargeNameWithPromptExample;

class PersonMergeNameWithPromptExampleTest extends AbstractEdeltaExamplesWithPromptTest {
	@Test
	void testMergeNameWithPrompt() throws Exception {
		var subdir = "";
		var ecores = of("AddressBook.ecore");
		var models = of("AddressBook.xmi");

		enterInput(" \n \n \n");

		// must complete within 5 seconds
		assertTimeoutPreemptively(Duration.ofSeconds(5), () -> {
			var engine = setupEngine(
				ecores,
				models,
				PersonMargeNameWithPromptExample::new);
	
			assertOutputs(engine,
				subdir,
				ecores,
				models
			);
		});
	}
}
