package edelta.lib.tests;

import static edelta.testutils.EdeltaTestUtils.assertFilesAreEquals;
import static edelta.testutils.EdeltaTestUtils.cleanDirectoryRecursive;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import edelta.lib.EdeltaDefaultRuntime;
import edelta.lib.EdeltaEngine;
import edelta.lib.EdeltaVersionMigrator;
import edelta.testutils.EdeltaTestUtils;

/**
 * The trick in these tests is to have a nesting level in output and expectations directory
 * so that the <code>xsi:schemaLocation</code> relative path points to a real Ecore file.
 * 
 * For example, in the "expectations" and in the "output", with the current subdirectory levels,
 * this works (and can be opened with the EMF reflective editor):
 * 
 * <pre>
 * xsi:schemaLocation="http://cs.gssi.it/PersonMM/v2 ../../../../edelta.testdata/testdata/version-migration/rename/metamodels/v2/PersonList.ecore">
 * </pre>
 * 
 * This also assumes that the correct versions of Ecore are found in the subdirectories.
 * 
 * These tests NEVER save Ecore files: only model files.
 * So, the Ecore files must be manually and correctly modified, according to the
 * evolutions written in these tests.
 */
class EdeltaVersionMigratorTest {

	private static final String TESTDATA = "../edelta.testdata/testdata/version-migration/";
	private static final String OUTPUT = "output/version-migration/";
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
				ensureEPackageIsLoadedByNsURI("PersonList", "http://cs.gssi.it/PersonMM/v1");
			}
			@Override
			public void doExecute() throws Exception {
				// simulate the renaming of the URI
				getEPackage("PersonList").setNsURI("http://cs.gssi.it/PersonMM/v2");
				// simulate the renaming to get to version 2
				getEAttribute("PersonList", "Person", "firstname").setName("firstName");
				getEAttribute("PersonList", "Person", "lastname").setName("lastName");
			}
		}
	);

	private EdeltaEngine renamePersonList = new EdeltaEngine(runtime ->
		new EdeltaDefaultRuntime(runtime) {
			@Override
			protected void performSanityChecks() throws Exception {
				ensureEPackageIsLoadedByNsURI("PersonList", "http://cs.gssi.it/PersonMM/v2");
			}
			@Override
			public void doExecute() throws Exception {
				// simulate the renaming of the URI
				getEPackage("PersonList").setNsURI("http://cs.gssi.it/PersonMM/v3");
				// simulate the renaming to get to version 3
				getEClass("PersonList", "List").setName("PersonList");
			}
		}
	);

	@Test
	void personListFromVersion1ToVersion2() throws Exception {
		var subdir = "rename/";
		var outputSubdir = "rename-v1-to-v2/";
		EdeltaTestUtils.copyDirectory(TESTDATA + subdir + MODELS + "/v1",
				OUTPUT + outputSubdir);
		versionMigrator.mapVersionMigration(List.of("http://cs.gssi.it/PersonMM/v1"),
				renamePersonFirstAndLastName);
		versionMigrator.loadEcoresFrom(TESTDATA + subdir + METAMODELS);
		versionMigrator.loadModelsFrom(OUTPUT + outputSubdir);
		versionMigrator.execute();
		executeAndAssertOutputs(outputSubdir, List.of("List.xmi", "List2.xmi"));
	}

	@Test
	void personListFromVersion2ToVersion3() throws Exception {
		var subdir = "rename/";
		var outputSubdir = "rename-v2-to-v3/";
		EdeltaTestUtils.copyDirectory(TESTDATA + subdir + MODELS + "/v2",
				OUTPUT + outputSubdir);
		versionMigrator.mapVersionMigration(List.of("http://cs.gssi.it/PersonMM/v1"),
				renamePersonFirstAndLastName);
		versionMigrator.mapVersionMigration(List.of("http://cs.gssi.it/PersonMM/v2"),
				renamePersonList);
		versionMigrator.loadEcoresFrom(TESTDATA + subdir + METAMODELS);
		versionMigrator.loadModelsFrom(OUTPUT + outputSubdir);
		versionMigrator.execute();
		executeAndAssertOutputs(outputSubdir, List.of("List.xmi", "List2.xmi"));
	}

	@Test
	void personListFromVersion1ToVersion3() throws Exception {
		var subdir = "rename/";
		var outputSubdir = "rename-v1-to-v3/";
		EdeltaTestUtils.copyDirectory(TESTDATA + subdir + MODELS + "/v1",
				OUTPUT + outputSubdir);
		versionMigrator.mapVersionMigration(List.of("http://cs.gssi.it/PersonMM/v1"),
				renamePersonFirstAndLastName);
		versionMigrator.mapVersionMigration(List.of("http://cs.gssi.it/PersonMM/v2"),
				renamePersonList);
		versionMigrator.loadEcoresFrom(TESTDATA + subdir + METAMODELS);
		versionMigrator.loadModelsFrom(OUTPUT + outputSubdir);
		versionMigrator.execute();
		executeAndAssertOutputs(outputSubdir, List.of("List.xmi", "List2.xmi"));
	}

	private EdeltaEngine renameMyPackage = new EdeltaEngine(runtime ->
		new EdeltaDefaultRuntime(runtime) {
			@Override
			protected void performSanityChecks() throws Exception {
				ensureEPackageIsLoadedByNsURI("mypackage", "http://my.package.org");
			}
			@Override
			public void doExecute() throws Exception {
				// simulate the renaming of the URI
				getEPackage("mypackage").setNsURI("http://my.package.org/v2");
				// simulate the renaming to get to version 2
				getEClass("mypackage", "MyClass").setName("MyRenamedClass");
				getEAttribute("mypackage", "MyRenamedClass", "myClassStringAttribute")
					.setName("myClassRenamedStringAttribute");
				getEClass("mypackage", "MyRoot").setName("MyRenamedRoot");
			}
		}
	);

	@Test
	void unrelatedEcoresAndModels() throws Exception {
		var subdir = "rename/";
		var outputSubdir = "rename-unrelated/";
		EdeltaTestUtils.copyDirectory(TESTDATA + subdir + MODELS + "/v1",
				OUTPUT + outputSubdir);
		versionMigrator.mapVersionMigration(List.of("http://cs.gssi.it/PersonMM/v1"),
				renamePersonFirstAndLastName);
		versionMigrator.mapVersionMigration(List.of("http://cs.gssi.it/PersonMM/v2"),
				renamePersonList);
		versionMigrator.mapVersionMigration(List.of("http://my.package.org"),
				renameMyPackage);
		versionMigrator.loadEcoresFrom(TESTDATA + subdir + METAMODELS);
		versionMigrator.loadModelsFrom(OUTPUT + outputSubdir);
		versionMigrator.execute();
		executeAndAssertOutputs(outputSubdir, List.of("List.xmi", "List2.xmi", "MyClass.xmi", "MyRoot.xmi"));
	}

	@Test
	void orderOfMappingsDoesNotMatter() throws Exception {
		var subdir = "rename/";
		var outputSubdir = "rename-unrelated/";
		EdeltaTestUtils.copyDirectory(TESTDATA + subdir + MODELS + "/v1",
				OUTPUT + outputSubdir);
		versionMigrator.mapVersionMigration(List.of("http://cs.gssi.it/PersonMM/v2"),
				renamePersonList);
		versionMigrator.mapVersionMigration(List.of("http://my.package.org"),
				renameMyPackage);
		versionMigrator.mapVersionMigration(List.of("http://cs.gssi.it/PersonMM/v1"),
				renamePersonFirstAndLastName);
		versionMigrator.loadEcoresFrom(TESTDATA + subdir + METAMODELS);
		versionMigrator.loadModelsFrom(OUTPUT + outputSubdir);
		versionMigrator.execute();
		executeAndAssertOutputs(outputSubdir, List.of("List.xmi", "List2.xmi", "MyClass.xmi", "MyRoot.xmi"));
	}

	/**
	 * This aims at simulating the real situation (e.g., when running in Eclipse on a real client's project
	 * or RCP): the previous versions of Ecores are loaded from the classpath and will not correspond to real
	 * filesystem paths, while the current latest versions of Ecores are effective and are to be referred by
	 * the migrated models when in their final state.
	 * 
	 * During the intermediate migrations phases, instead, references to Ecores would not be valid.
	 * We only care about the migrated models final state.
	 * 
	 * @throws Exception
	 */
	@Test
	void loadingWithInputStreams() throws Exception {
		var subdir = "rename/";
		var outputSubdir = "rename-unrelated/";
		EdeltaTestUtils.copyDirectory(TESTDATA + subdir + MODELS + "/v1",
				OUTPUT + outputSubdir);
		versionMigrator.mapVersionMigration(List.of("http://cs.gssi.it/PersonMM/v2"),
				renamePersonList);
		versionMigrator.mapVersionMigration(List.of("http://my.package.org"),
				renameMyPackage);
		versionMigrator.mapVersionMigration(List.of("http://cs.gssi.it/PersonMM/v1"),
				renamePersonFirstAndLastName);

		// note that during this scan we also load with this strategy the final versions of Ecores
		// but then (*) we load the final Ecores by their effective path
		try (var stream = Files.walk(Paths.get(TESTDATA + subdir + METAMODELS))) {
			stream
				.filter(file -> !Files.isDirectory(file))
				.filter(file -> file.toString().endsWith(".ecore"))
				.forEach(file -> {
					try {
						/*
						 * use only the file name (not the full path), to simulate the situation where
						 * previous metamodels are temporarily in memory; this means that, until the
						 * models are in their final migrated versions the references to the ecores will
						 * not be correct
						 */
						versionMigrator.loadEcore(file.getFileName().toString(), new FileInputStream(file.toFile()));
					} catch (IOException e) {
						fail(e);
					}
				});
		}

		// (*)
		// the final Ecores are instead loaded as real files, so that their references in models will be effective
		// and refer to real ecores
		versionMigrator.loadEcoresFrom(TESTDATA + subdir + METAMODELS + "v3/PersonList.ecore");
		versionMigrator.loadEcoresFrom(TESTDATA + subdir + METAMODELS + "v2/My.ecore");

		versionMigrator.loadModelsFrom(OUTPUT + outputSubdir);
		versionMigrator.execute();
		executeAndAssertOutputs(outputSubdir, List.of("List.xmi", "List2.xmi", "MyClass.xmi", "MyRoot.xmi"));
	}

	@Test
	void unrelatedEcoresAndModelsWithCustomExtensions() throws Exception {
		var subdir = "rename/";
		var outputSubdir = "rename-unrelated-custom-extensions/";
		EdeltaTestUtils.copyDirectory(TESTDATA + subdir + MODELS + "/v1-custom-extension",
				OUTPUT + outputSubdir);
		versionMigrator.addModelFileExtension(".customextension");
		versionMigrator.addModelFileExtension(".anothercustomextension");
		versionMigrator.mapVersionMigration(List.of("http://cs.gssi.it/PersonMM/v1"),
				renamePersonFirstAndLastName);
		versionMigrator.mapVersionMigration(List.of("http://cs.gssi.it/PersonMM/v2"),
				renamePersonList);
		versionMigrator.mapVersionMigration(List.of("http://my.package.org"),
				renameMyPackage);
		versionMigrator.loadEcoresFrom(TESTDATA + subdir + METAMODELS);
		versionMigrator.loadModelsFrom(OUTPUT + outputSubdir);
		versionMigrator.execute();
		executeAndAssertOutputs(outputSubdir, List.of("List.customextension", "List2.customextension",
				"MyClass.anothercustomextension", "MyRoot.anothercustomextension"));
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
