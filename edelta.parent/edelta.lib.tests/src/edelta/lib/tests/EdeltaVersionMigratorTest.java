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

	private EdeltaEngine renamePersonFirstAndLastName = new EdeltaEngine(runtime ->
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
	);

	private EdeltaEngine renamePersonList = new EdeltaEngine(runtime ->
		new EdeltaDefaultRuntime(runtime) {
			@Override
			protected void performSanityChecks() throws Exception {
				ensureEPackageIsLoaded("PersonList");
			};
			@Override
			public void doExecute() throws Exception {
				// simulate the renaming of the URI
				getEPackage("PersonList").setNsURI("http://cs.gssi.it/PersonMM/v3");
				// simulate the renaming to get to version 3
				getEClass("PersonList", "List").setName("PersonList");
			};
		}
	);

	@Test
	void personListFromVersion1ToVersion2() throws Exception {
		var subdir = "rename/";
		var outputSubdir = "rename-v1-to-v2/";
		versionMigrator.mapVersionMigration(List.of("http://cs.gssi.it/PersonMM/v1"), renamePersonFirstAndLastName);
		versionMigrator.loadEcoresFrom(TESTDATA + subdir + METAMODELS);
		versionMigrator.loadModelsFrom(TESTDATA + subdir + MODELS + "/v1");
		versionMigrator.execute(OUTPUT + outputSubdir);
		executeAndAssertOutputs(outputSubdir, List.of("List.xmi", "List2.xmi"));
	}

	@Test
	void personListFromVersion2ToVersion3() throws Exception {
		var subdir = "rename/";
		var outputSubdir = "rename-v2-to-v3/";
		versionMigrator.mapVersionMigration(List.of("http://cs.gssi.it/PersonMM/v1"), renamePersonFirstAndLastName);
		versionMigrator.mapVersionMigration(List.of("http://cs.gssi.it/PersonMM/v2"), renamePersonList);
		versionMigrator.loadEcoresFrom(TESTDATA + subdir + METAMODELS);
		versionMigrator.loadModelsFrom(TESTDATA + subdir + MODELS + "/v2");
		versionMigrator.execute(OUTPUT + outputSubdir);
		executeAndAssertOutputs(outputSubdir, List.of("List.xmi", "List2.xmi"));
	}

	@Test
	void personListFromVersion1ToVersion3() throws Exception {
		var subdir = "rename/";
		var outputSubdir = "rename-v1-to-v3/";
		versionMigrator.mapVersionMigration(List.of("http://cs.gssi.it/PersonMM/v1"), renamePersonFirstAndLastName);
		versionMigrator.mapVersionMigration(List.of("http://cs.gssi.it/PersonMM/v2"), renamePersonList);
		versionMigrator.loadEcoresFrom(TESTDATA + subdir + METAMODELS);
		versionMigrator.loadModelsFrom(TESTDATA + subdir + MODELS + "/v1");
		versionMigrator.execute(OUTPUT + outputSubdir);
		executeAndAssertOutputs(outputSubdir, List.of("List.xmi", "List2.xmi"));
	}

	private EdeltaEngine renameMyPackage = new EdeltaEngine(runtime ->
		new EdeltaDefaultRuntime(runtime) {
			@Override
			protected void performSanityChecks() throws Exception {
				ensureEPackageIsLoaded("mypackage");
			};
			@Override
			public void doExecute() throws Exception {
				// simulate the renaming of the URI
				getEPackage("mypackage").setNsURI("http://my.package.org/v2");
				// simulate the renaming to get to version 2
				getEClass("mypackage", "MyClass").setName("MyRenamedClass");
				getEAttribute("mypackage", "MyRenamedClass", "myClassStringAttribute")
					.setName("myClassRenamedStringAttribute");
				getEClass("mypackage", "MyRoot").setName("MyRenamedRoot");
			};
		}
	);

	@Test
	void unrelatedEcoresAndModels() throws Exception {
		var subdir = "rename/";
		var outputSubdir = "rename-unrelated/";
		versionMigrator.mapVersionMigration(List.of("http://cs.gssi.it/PersonMM/v1"), renamePersonFirstAndLastName);
		versionMigrator.mapVersionMigration(List.of("http://cs.gssi.it/PersonMM/v2"), renamePersonList);
		versionMigrator.mapVersionMigration(List.of("http://my.package.org"), renameMyPackage);
		versionMigrator.loadEcoresFrom(TESTDATA + subdir + METAMODELS);
		versionMigrator.loadModelsFrom(TESTDATA + subdir + MODELS + "/v1");
		versionMigrator.execute(OUTPUT + outputSubdir);
		executeAndAssertOutputs(outputSubdir, List.of("List.xmi", "List2.xmi", "MyClass.xmi", "MyRoot.xmi"));
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
