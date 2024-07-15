package edelta.examples.tests;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;

import org.apache.log4j.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import edelta.refactorings.lib.helper.EdeltaPromptHelper;

public abstract class AbstractEdeltaExamplesWithPromptTest extends AbstractEdeltaExamplesTest {

	private static final PrintStream ORIGINAL_OUT = System.out; // NOSONAR: we need this
	private static final PrintStream ORIGINAL_ERR = System.err; // NOSONAR: we need this
	private static final InputStream ORIGINAL_IN = System.in;

	private Logger logger = Logger.getLogger(getClass());

	@BeforeEach
	void setup() {
		ByteArrayOutputStream errContent;
		ByteArrayOutputStream outContent;
		EdeltaPromptHelper.close();
		outContent = new ByteArrayOutputStream();
		errContent = new ByteArrayOutputStream();
		System.setOut(new PrintStream(outContent));
		System.setErr(new PrintStream(errContent));
	}

	@AfterEach
	void resetStreams() {
		logger.info("Resetting streams");
		EdeltaPromptHelper.close();
		System.setOut(ORIGINAL_OUT);
		System.setErr(ORIGINAL_ERR);
		System.setIn(ORIGINAL_IN);
	}

	protected void enterInput(String data) {
		ByteArrayInputStream testIn;
		testIn = new ByteArrayInputStream(data.getBytes());
		System.setIn(testIn);
	}

}
