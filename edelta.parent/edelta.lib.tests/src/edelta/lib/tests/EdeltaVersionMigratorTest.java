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
import edelta.lib.EdeltaEngine.EdeltaRuntimeProvider;
import edelta.lib.EdeltaModelManager;
import edelta.lib.EdeltaResourceUtils;
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
 *
 * The previous versions of Ecores are loaded from the classpath and will not correspond to real
 * filesystem paths, while the current latest versions of Ecores are effective and are to be referred by
 * the migrated models when in their final state.
 *
 * During the intermediate migrations phases, instead, references to Ecores would not be valid.
 * We only care about the migrated models final state.
 */
class EdeltaVersionMigratorTest {

	private static final String TESTDATA = "../edelta.testdata/testdata/version-migration/";
	private static final String OUTPUT = "output/version-migration/";
	private static final String EXPECTATIONS = "../edelta.testdata/expectations/version-migration/";
	private static final String METAMODELS = "metamodels/";
	private static final String MODELS = "models/";

	private static final String PERSON_LIST_ECORE = "PersonList.ecore";
	private static final String MY_ECORE = "My.ecore";

	private static final String CLASS_TESTDATA = "/version-migration/";

	private EdeltaVersionMigrator versionMigrator;

	@BeforeAll
	static void clearOutput() throws IOException {
		cleanDirectoryRecursive(OUTPUT);
	}

	@BeforeEach
	void setup() {
		versionMigrator = new EdeltaVersionMigrator();
	}

	private EdeltaRuntimeProvider renamePersonFirstAndLastNameProvider = runtime ->
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
			@Override
			public List<String> getMigratedNsURIs() {
				return List.of("http://cs.gssi.it/PersonMM/v1");
			}
			@Override
			public List<String> getMigratedEcorePaths() {
				return List.of(CLASS_TESTDATA + "rename/" + METAMODELS + "v1/" + PERSON_LIST_ECORE);
			}
		};

	private EdeltaRuntimeProvider renamePersonListProvider = runtime ->
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
			@Override
			public List<String> getMigratedNsURIs() {
				return List.of("http://cs.gssi.it/PersonMM/v2");
			}
			@Override
			public List<String> getMigratedEcorePaths() {
				return List.of(CLASS_TESTDATA + "rename/" + METAMODELS + "v2/" + PERSON_LIST_ECORE);
			}
		};

	@Test
	void personListFromVersion1ToVersion2() throws Exception {
		var subdir = "rename/";
		var outputSubdir = "rename-v1-to-v2/";
		EdeltaTestUtils.copyDirectory(TESTDATA + subdir + MODELS + "/v1",
				OUTPUT + outputSubdir);

		// initialize with migrations
		versionMigrator.registerMigration(renamePersonFirstAndLastNameProvider);
		// load the latest version of the Ecore
		versionMigrator.loadEcore(TESTDATA + subdir + METAMODELS + "v2/" + PERSON_LIST_ECORE);
		// load the models to check for migration
		versionMigrator.loadModel(OUTPUT + outputSubdir + "List.xmi");
		versionMigrator.loadModel(OUTPUT + outputSubdir + "List2.xmi");

		versionMigrator.execute();
		executeAndAssertOutputs(outputSubdir, List.of("List.xmi", "List2.xmi"));
	}

	@Test
	void personListFromVersion2ToVersion3() throws Exception {
		var subdir = "rename/";
		var outputSubdir = "rename-v2-to-v3/";
		EdeltaTestUtils.copyDirectory(TESTDATA + subdir + MODELS + "/v2",
				OUTPUT + outputSubdir);

		// initialize with migrations
		versionMigrator.registerMigration(renamePersonFirstAndLastNameProvider);
		versionMigrator.registerMigration(renamePersonListProvider);
		// load the latest version of the Ecore
		versionMigrator.loadEcore(TESTDATA + subdir + METAMODELS + "v3/" + PERSON_LIST_ECORE);
		// load the models to check for migration
		versionMigrator.loadModel(OUTPUT + outputSubdir + "List.xmi");
		versionMigrator.loadModel(OUTPUT + outputSubdir + "List2.xmi");

		versionMigrator.execute();
		executeAndAssertOutputs(outputSubdir, List.of("List.xmi", "List2.xmi"));
	}

	@Test
	void personListFromVersion1ToVersion3() throws Exception {
		var subdir = "rename/";
		var outputSubdir = "rename-v1-to-v3/";
		EdeltaTestUtils.copyDirectory(TESTDATA + subdir + MODELS + "/v1",
				OUTPUT + outputSubdir);

		// initialize with migrations
		versionMigrator.registerMigration(renamePersonFirstAndLastNameProvider);
		versionMigrator.registerMigration(renamePersonListProvider);
		// load the latest version of the Ecore
		versionMigrator.loadEcore(TESTDATA + subdir + METAMODELS + "v3/" + PERSON_LIST_ECORE);
		// load the models to check for migration
		versionMigrator.loadModel(OUTPUT + outputSubdir + "List.xmi");
		versionMigrator.loadModel(OUTPUT + outputSubdir + "List2.xmi");

		versionMigrator.execute();
		executeAndAssertOutputs(outputSubdir, List.of("List.xmi", "List2.xmi"));
	}

	@Test
	void alreadyAtTheLatestVersion() throws Exception {
		var subdir = "rename/";
		var outputSubdir = "rename-already-latest-version/";
		EdeltaTestUtils.copyDirectory(TESTDATA + subdir + MODELS + "/v3",
				OUTPUT + outputSubdir);

		// initialize with migrations
		versionMigrator.registerMigration(renamePersonFirstAndLastNameProvider);
		versionMigrator.registerMigration(renamePersonListProvider);
		// load the latest version of the Ecore
		versionMigrator.loadEcore(TESTDATA + subdir + METAMODELS + "v3/" + PERSON_LIST_ECORE);
		// load the models to check for migration
		versionMigrator.loadModel(OUTPUT + outputSubdir + "List.xmi");
		versionMigrator.loadModel(OUTPUT + outputSubdir + "List2.xmi");

		versionMigrator.execute();
		executeAndAssertOutputs(outputSubdir, List.of("List.xmi", "List2.xmi"));
	}

	private EdeltaRuntimeProvider renameMyPackageProvider = runtime ->
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
			@Override
			public List<String> getMigratedNsURIs() {
				return List.of("http://my.package.org");
			}
			@Override
			public List<String> getMigratedEcorePaths() {
				return List.of(CLASS_TESTDATA + "rename/" + METAMODELS + "v1/" + MY_ECORE);
			}
		};

	@Test
	void unrelatedEcoresAndModels() throws Exception {
		var subdir = "rename/";
		var outputSubdir = "rename-unrelated/";
		EdeltaTestUtils.copyDirectory(TESTDATA + subdir + MODELS + "/v1",
				OUTPUT + outputSubdir);

		// initialize with migrations
		versionMigrator.registerMigration(renamePersonFirstAndLastNameProvider);
		versionMigrator.registerMigration(renameMyPackageProvider);
		versionMigrator.registerMigration(renamePersonListProvider);
		// load the latest version of the Ecore
		versionMigrator.loadEcore(TESTDATA + subdir + METAMODELS + "v3/" + PERSON_LIST_ECORE);
		versionMigrator.loadEcore(TESTDATA + subdir + METAMODELS + "v2/" + MY_ECORE);
		// load the models to check for migration
		versionMigrator.loadModelsFrom(OUTPUT + outputSubdir);

		versionMigrator.execute();
		executeAndAssertOutputs(outputSubdir, List.of("List.xmi", "List2.xmi", "MyClass.xmi", "MyRoot.xmi"));
	}

	/**
	 * One of the latest version of an Ecore is specified directly with an EPackage loaded instance.
	 *
	 * @throws Exception
	 */
	@Test
	void loadEPackageDirectly() throws Exception {
		var subdir = "rename/";
		var outputSubdir = "rename-unrelated/";
		EdeltaTestUtils.copyDirectory(TESTDATA + subdir + MODELS + "/v1",
				OUTPUT + outputSubdir);

		versionMigrator.registerMigration(renamePersonListProvider);
		versionMigrator.registerMigration(renameMyPackageProvider);
		versionMigrator.registerMigration(renamePersonFirstAndLastNameProvider);

		versionMigrator.loadEcore(TESTDATA + subdir + METAMODELS + "v3/" + PERSON_LIST_ECORE);
		// simulate the loading of an EPackage (e.g., by the direct access to its instance through EMF API)
		var modelManager = new EdeltaModelManager();
		var resource = modelManager.loadEcoreFile(TESTDATA + subdir + METAMODELS + "v2/" + MY_ECORE);
		var ePackage = EdeltaResourceUtils.getEPackage(resource);
		versionMigrator.loadCurrentEPackage(ePackage);

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

		// initialize with migrations
		versionMigrator.registerMigration(renamePersonFirstAndLastNameProvider);
		versionMigrator.registerMigration(renameMyPackageProvider);
		versionMigrator.registerMigration(renamePersonListProvider);
		// load the latest version of the Ecore
		versionMigrator.loadEcore(TESTDATA + subdir + METAMODELS + "v3/" + PERSON_LIST_ECORE);
		versionMigrator.loadEcore(TESTDATA + subdir + METAMODELS + "v2/" + MY_ECORE);
		// load the models to check for migration
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
