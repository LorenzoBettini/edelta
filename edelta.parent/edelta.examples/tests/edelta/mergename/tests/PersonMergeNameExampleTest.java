package edelta.mergename.tests;

import static java.util.List.of;

import org.junit.Test;

import edelta.examples.tests.AbstractEdeltaExamplesTest;
import edelta.mergename.example.PersonMargeNameExample;

public class PersonMergeNameExampleTest extends AbstractEdeltaExamplesTest {
	@Test
	public void testMergeName() throws Exception {
		var subdir = "";
		var ecores = of("AddressBook.ecore");
		var models = of("AddressBook.xmi");

		var engine = setupEngine(
			ecores,
			models,
			PersonMargeNameExample::new);

		assertOutputs(engine,
			subdir,
			ecores,
			models
		);
	}
}
