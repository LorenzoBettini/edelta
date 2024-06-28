package edelta.lib.tests;

import static edelta.testutils.EdeltaTestUtils.assertFilesAreEquals;
import static edelta.testutils.EdeltaTestUtils.cleanDirectoryRecursive;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import edelta.lib.EdeltaDefaultRuntime;
import edelta.lib.EdeltaEngine;
import edelta.lib.EdeltaVersionMigrator;

class EdeltaVersionMigratorTest {

	private static final String TESTDATA = "../edelta.testdata/testdata/version-migration/";
	private static final String OUTPUT = "output/";
	private static final String EXPECTATIONS = "../edelta.testdata/expectations/version-migration/";
	private static final String METAMODELS = "metamodels/";
	private static final String MODELS = "models/";

	private EdeltaVersionMigrator versionMigrator;

	@BeforeAll
	static void clearOutput() throws IOException {
		cleanDirectoryRecursive(OUTPUT);
	}

	@BeforeEach
	void setup() {
		versionMigrator = new EdeltaVersionMigrator();
	}

	@Test
	void simpleCase() throws Exception {
		var subdir = "rename/";
		versionMigrator.mapVersionMigration(List.of("http://cs.gssi.it/PersonMM/v1"), new EdeltaEngine(runtime ->
			new EdeltaDefaultRuntime(runtime) {
				@Override
				protected void performSanityChecks() throws Exception {
					ensureEPackageIsLoaded("PersonList");
				};
				@Override
				public void doExecute() throws Exception {
					// simulate the renaming of the URI
					getEPackage("PersonList").setNsURI("http://cs.gssi.it/PersonMM/v2");
					// simulate the renaming to get to version 2
					getEAttribute("PersonList", "Person", "firstname").setName("firstName");
					getEAttribute("PersonList", "Person", "lastname").setName("lastName");
				};
			}
		));
		versionMigrator.loadEcoresFrom(TESTDATA + subdir + METAMODELS);
		versionMigrator.loadModelsFrom(TESTDATA + subdir + MODELS + "/v1");
		versionMigrator.execute(OUTPUT + subdir);
		executeAndAssertOutputs(subdir, List.of("List.xmi", "List2.xmi"));
	}

	private void executeAndAssertOutputs(String subdir, Collection<String> modelFiles) {
		var output = OUTPUT + subdir;
		modelFiles.forEach
			(fileName ->
				assertGeneratedFiles(fileName, subdir, output, fileName));
	}

	private void assertGeneratedFiles(String message, String subdir, String outputDir, String fileName) {
		try {
			assertFilesAreEquals(
				message,
				EXPECTATIONS + subdir + fileName,
				outputDir + fileName);
		} catch (IOException e) {
			e.printStackTrace();
			fail(e.getClass().getName() + ": " + e.getMessage());
		}
	}
}
