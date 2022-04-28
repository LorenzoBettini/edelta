package edelta.statecharts.tests;

import static java.util.List.of;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.time.Duration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import edelta.examples.tests.AbstractEdeltaExamplesTest;
import edelta.statecharts.example.StateChartsWithPromptExample;

class StateChartsWithPromptExampleTest extends AbstractEdeltaExamplesTest {

	private ByteArrayOutputStream outContent;
	private ByteArrayOutputStream errContent;
	private ByteArrayInputStream testIn;
	private final PrintStream originalOut = System.out;
	private final PrintStream originalErr = System.err;
	private final InputStream originalIn = System.in;

	@BeforeEach
	void setup() throws Exception {
		outContent = new ByteArrayOutputStream();
		errContent = new ByteArrayOutputStream();
		System.setOut(new PrintStream(outContent));
		System.setErr(new PrintStream(errContent));
	}

	@AfterEach
	void resetStreams() throws IOException {
		System.setOut(originalOut);
		System.setErr(originalErr);
		System.setIn(originalIn);
	}

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

	private void enterInput(String data) {
		testIn = new ByteArrayInputStream(data.getBytes());
		System.setIn(testIn);
	}
}
