package edelta.petrinet.tests;

import static java.util.List.of;

import java.util.Collections;

import org.junit.Test;

import edelta.examples.tests.AbstractEdeltaExamplesTest;
import edelta.petrinet.example.PetrinetExample;

public class PetrinetExampleTest extends AbstractEdeltaExamplesTest {

	@Test
	public void testPetrinet() throws Exception {
		var subdir = "";
		var ecores = of("Petrinet.ecore");
//		var models = of("Net.xmi");

		var engine = setupEngine(
			ecores,
			Collections.emptyList(),
			PetrinetExample::new);

		executeSaveAndAssert(engine,
			subdir,
			ecores,
			Collections.emptyList()
		);
	}

}
