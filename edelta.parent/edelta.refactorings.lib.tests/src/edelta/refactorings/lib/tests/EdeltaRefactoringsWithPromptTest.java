package edelta.refactorings.lib.tests;

import static java.util.List.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.time.Duration;
import java.util.List;

import org.eclipse.emf.ecore.EClass;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import edelta.lib.EdeltaDefaultRuntime;
import edelta.lib.EdeltaModelManager;
import edelta.lib.EdeltaUtils;
import edelta.refactorings.lib.EdeltaRefactorings;
import edelta.refactorings.lib.EdeltaRefactoringsWithPrompt;
import edelta.refactorings.lib.helper.EdeltaPromptHelper;
import edelta.testutils.EdeltaTestUtils;

class EdeltaRefactoringsWithPromptTest extends AbstractEdeltaRefactoringsLibTest {

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

		var modelManager = new EdeltaModelManager();
		var refactorings = new EdeltaRefactorings(new EdeltaDefaultRuntime(modelManager));
		refactorings.performSanityChecks();
	}

	/**
	 * This should be commented out when we want to copy the generated modified
	 * Ecore into the test-output-expectations directory, typically, the first time
	 * we write a new test.
	 * 
	 * We need to clean the modified directory when tests are stable, so that
	 * modified Ecore with validation errors do not fill the project with error
	 * markers.
	 * 
	 * @throws IOException
	 */
	@AfterEach
	void tearDown() throws IOException {
		EdeltaPromptHelper.close();
		System.setOut(originalOut);
		System.setErr(originalErr);
		System.setIn(originalIn);

		EdeltaTestUtils.cleanDirectory(AbstractEdeltaRefactoringsLibTest.MODIFIED);
	}

	@Test
	void test_enumToSubclasses() throws Exception {
		var subdir = "enumToSubclasses/";
		var ecores = of("PersonList.ecore");
		var models = of("List.xmi");

		enterInput("1\n2\n1\n");

		// must complete within 5 seconds
		assertTimeoutPreemptively(Duration.ofSeconds(5), () -> {
			var engine = setupEngine(
				subdir,
				ecores,
				models,
				other -> new EdeltaRefactoringsWithPrompt(other) {
					@Override
					protected void doExecute() {
						var person = getEClass("PersonList", "Person");
						// simulates enumToSubclasses
						var subclasses = introduceSubclassesInteractive(person, List.of("Male", "Female"));
						assertThat(subclasses)
							.extracting(EClass::getName)
							.containsExactlyInAnyOrder("Male", "Female");
						var genre = getEEnum("PersonList", "Gender");
						assertNotNull(genre);
						EdeltaUtils.removeElement(genre);
					}
				}
			);
	
			assertOutputs(
				engine,
				subdir,
				ecores,
				models
			);
		});
		assertEquals("""
			Migrating PersonList.Person{firstname = MaleFirstName, lastname = MaleLastName, gender = MALE}
			1 / 3
			  1 Male
			  2 Female
			Choice? Migrating PersonList.Person{firstname = FemaleFirstName, lastname = FemaleLastName, gender = FEMALE}
			2 / 3
			  1 Male
			  2 Female
			Choice? Migrating PersonList.Person{firstname = UnspecifiedFirstName, lastname = UnspecifiedLastName, gender = MALE}
			3 / 3
			  1 Male
			  2 Female
			Choice?\s""",
			getOutContent());
		assertEquals("", getErrContent());
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
