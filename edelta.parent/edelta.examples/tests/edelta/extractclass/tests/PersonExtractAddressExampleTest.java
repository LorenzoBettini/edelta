package edelta.extractclass.tests;

import static java.util.List.of;

import org.junit.Test;

import edelta.examples.tests.AbstractEdeltaExamplesTest;
import edelta.extractclass.example.PersonExtractAddressExample;

public class PersonExtractAddressExampleTest extends AbstractEdeltaExamplesTest {
	@Test
	public void testExtractAddress() throws Exception {
		var subdir = "";
		var ecores = of("AddressBook2.ecore");
		var models = of("AddressBook2.xmi");

		var engine = setupEngine(
			ecores,
			models,
			PersonExtractAddressExample::new);

		assertOutputs(engine,
			subdir,
			ecores,
			models
		);
	}
}
