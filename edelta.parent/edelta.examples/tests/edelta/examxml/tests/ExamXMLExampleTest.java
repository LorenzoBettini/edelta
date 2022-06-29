package edelta.examxml.tests;

import static java.util.List.of;

import org.junit.Test;

import edelta.examples.tests.AbstractEdeltaExamplesTest;
import edelta.examxml.example.ExamXMLExample1;
import edelta.examxml.example.ExamXMLExample2;

public class ExamXMLExampleTest extends AbstractEdeltaExamplesTest {

	@Test
	public void testExamXML1() throws Exception {
		var ecores = of("ExamXML.ecore");
		var models = of("Exam.xmi");

		var engine = setupEngine(
			ecores,
			models,
			ExamXMLExample1::new);

		assertOutputs(engine,
			"ExamXMLExample1/",
			ecores,
			models
		);
	}

	@Test
	public void testExamXML2() throws Exception {
		var ecores = of("ExamXML.ecore");
		var models = of("Exam.xmi");

		var engine = setupEngine(
			ecores,
			models,
			ExamXMLExample2::new);

		assertOutputs(engine,
			"ExamXMLExample2/",
			ecores,
			models
			);
	}

}
