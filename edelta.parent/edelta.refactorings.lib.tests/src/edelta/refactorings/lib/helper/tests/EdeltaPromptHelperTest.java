package edelta.refactorings.lib.helper.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import edelta.refactorings.lib.helper.EdeltaPromptHelper;
import edelta.testutils.EdeltaTestUtils;

class EdeltaPromptHelperTest {

	private ByteArrayOutputStream outContent;
	private ByteArrayOutputStream errContent;
	private ByteArrayInputStream testIn;
	private final PrintStream originalOut = System.out;
	private final PrintStream originalErr = System.err;
	private final InputStream originalIn = System.in;

	@BeforeEach
	public void setUpStreams() {
		outContent = new ByteArrayOutputStream();
		errContent = new ByteArrayOutputStream();
		System.setOut(new PrintStream(outContent));
		System.setErr(new PrintStream(errContent));
	}

	@AfterEach
	public void restoreStreams() {
		EdeltaPromptHelper.close();
		System.setOut(originalOut);
		System.setErr(originalErr);
		System.setIn(originalIn);
	}

	@Test
	void testShow() {
		EdeltaPromptHelper.show("A test");
		assertEquals("A test\n", getOutContent());
	}

	@Test
	void testValidChoice() {
		enterInput("2\n3\n");
		var result = EdeltaPromptHelper.choice(List.of("First", "Second", "Third"));
		assertEquals("""
				  1 First
				  2 Second
				  3 Third
				Choice?\s""", getOutContent());
		assertEquals("Second", result);
		result = EdeltaPromptHelper.choice(List.of("First", "Second", "Third"));
		assertEquals("Third", result);
	}

	@Test
	void testInvalidChoices() {
		enterInput("0\n4\nBANG\n2");
		String result = EdeltaPromptHelper.choice(List.of("First", "Second", "Third"));
		assertEquals("""
				  1 First
				  2 Second
				  3 Third
				Choice? Choice? Choice? Choice?\s""", getOutContent());
		assertEquals("""
				Not a valid choice: 0
				Not a valid choice: 4
				Not a valid number: BANG
				""", getErrContent());
		assertEquals("Second", result);
	}

	private void enterInput(String data) {
		testIn = new ByteArrayInputStream(data.getBytes());
		System.setIn(testIn);
	}

	private String getOutContent() {
		return EdeltaTestUtils.removeCR(outContent.toString());
	}

	private String getErrContent() {
		return EdeltaTestUtils.removeCR(errContent.toString());
	}
}
