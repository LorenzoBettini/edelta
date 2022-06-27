package edelta.examxml.tests;

import static java.util.List.of;

import org.junit.Test;

import edelta.examples.tests.AbstractEdeltaExamplesTest;
import edelta.examxml.example.ExamXMLExample;

public class ExamXMLExampleTest extends AbstractEdeltaExamplesTest {

	@Test
	public void testExamXML() throws Exception {
		var subdir = "";
		var ecores = of("ExamXML.ecore");
		var models = of("Exam.xmi");

		var engine = setupEngine(
			ecores,
			models,
			ExamXMLExample::new);

		assertOutputs(engine,
			subdir,
			ecores,
			models
		);
	}

}
