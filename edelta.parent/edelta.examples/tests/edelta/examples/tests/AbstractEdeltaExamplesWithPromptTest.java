package edelta.examples.tests;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;

import org.apache.log4j.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import edelta.refactorings.lib.helper.EdeltaPromptHelper;

public abstract class AbstractEdeltaExamplesWithPromptTest extends AbstractEdeltaExamplesTest {

	private ByteArrayOutputStream outContent;
	private ByteArrayOutputStream errContent;
	private ByteArrayInputStream testIn;
	private final PrintStream originalOut = System.out;
	private final PrintStream originalErr = System.err;
	private final InputStream originalIn = System.in;

	private Logger logger = Logger.getLogger(getClass());

	@BeforeEach
	void setup() throws Exception {
		EdeltaPromptHelper.close();
		outContent = new ByteArrayOutputStream();
		errContent = new ByteArrayOutputStream();
		System.setOut(new PrintStream(outContent));
		System.setErr(new PrintStream(errContent));
	}

	@AfterEach
	void resetStreams() throws IOException {
		logger.info("Resetting streams");
		EdeltaPromptHelper.close();
		System.setOut(originalOut);
		System.setErr(originalErr);
		System.setIn(originalIn);
	}

	protected void enterInput(String data) {
		testIn = new ByteArrayInputStream(data.getBytes());
		System.setIn(testIn);
	}

}
