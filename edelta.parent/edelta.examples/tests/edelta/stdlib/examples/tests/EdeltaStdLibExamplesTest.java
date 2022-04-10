package edelta.stdlib.examples.tests;

import static java.util.List.of;

import org.junit.Test;

import edelta.examples.tests.AbstractEdeltaExamplesTest;
import edelta.stdlib.examples.ChangeReferenceTypeExample;
import edelta.stdlib.examples.ChangeReferenceTypeManualExample;
import edelta.stdlib.examples.ChangeReferenceTypeMultipleExample;
import edelta.stdlib.examples.ChangeToAbstractExample;

public class EdeltaStdLibExamplesTest extends AbstractEdeltaExamplesTest {

	@Test
	public void testChangeReferenceType() throws Exception {
		var subdir = "ChangeReferenceType/";
		var ecores = of("PersonListForChangeType.ecore");
		var models = of("ListForChangeType.xmi");

		var engine = setupEngine(
			ecores,
			models,
			ChangeReferenceTypeExample::new);

		assertOutputs(engine,
			subdir,
			ecores,
			models
		);
	}

	@Test
	public void testChangeReferenceTypeMultiple() throws Exception {
		var subdir = "ChangeReferenceTypeMultiple/";
		var ecores = of("PersonListForChangeType.ecore");
		var models = of("ListForChangeType.xmi");

		var engine = setupEngine(
			ecores,
			models,
			ChangeReferenceTypeMultipleExample::new);

		assertOutputs(engine,
			subdir,
			ecores,
			models
		);
	}

	@Test
	public void testChangeReferenceTypeManual() throws Exception {
		var subdir = "ChangeReferenceType/";
		var ecores = of("PersonListForChangeType.ecore");
		var models = of("ListForChangeType.xmi");

		var engine = setupEngine(
			ecores,
			models,
			ChangeReferenceTypeManualExample::new);

		assertOutputs(engine,
			subdir,
			ecores,
			models
		);
	}

	@Test
	public void testChangeToAbstract() throws Exception {
		var subdir = "ChangeToAbstract/";
		var ecores = of("PersonListForChangeToAbstract.ecore");
		var models = of("ListForChangeToAbstract.xmi");

		var engine = setupEngine(
			ecores,
			models,
			ChangeToAbstractExample::new);

		assertOutputs(engine,
			subdir,
			ecores,
			models
		);
	}
}
