package edelta.mergename.tests;

import static java.util.List.of;

import org.junit.Test;

import edelta.examples.tests.AbstractEdeltaExamplesWithPromptTest;
import edelta.mergename.example.PersonMargeNameWithPromptExample;

public class PersonMergeNameWithPromptExampleTest extends AbstractEdeltaExamplesWithPromptTest {
	@Test
	public void testMergeNameWithPrompt() throws Exception {
		var subdir = "";
		var ecores = of("AddressBook.ecore");
		var models = of("AddressBook.xmi");

		enterInput(" \n \n \n");

		var engine = setupEngine(
			ecores,
			models,
			PersonMargeNameWithPromptExample::new);

		assertOutputs(engine,
			subdir,
			ecores,
			models
		);
	}
}
