package edelta.examples.tests;

import static edelta.testutils.EdeltaTestUtils.assertFilesAreEquals;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.Collection;

import org.junit.BeforeClass;

import edelta.lib.EdeltaEngine;
import edelta.lib.EdeltaEngine.EdeltaRuntimeProvider;
import edelta.testutils.EdeltaTestUtils;

public abstract class AbstractEdeltaExamplesTest {

	private static final String TESTDATA = "model/";
	private static final String OUTPUT = "modified/";
	private static final String EXPECTATIONS = "expectations/";

	@BeforeClass
	public static void clearOutput() throws IOException {
		EdeltaTestUtils.cleanDirectoryRecursive(OUTPUT);
	}

	public AbstractEdeltaExamplesTest() {
		super();
	}

	protected EdeltaEngine setupEngine(Collection<String> ecoreFiles, Collection<String> modelFiles, EdeltaRuntimeProvider runtimeProvider) {
		var basedir = TESTDATA; // subdir is not used for the moment
		var engine = new EdeltaEngine(runtimeProvider);
		ecoreFiles
			.forEach(fileName -> engine.loadEcoreFile(basedir + fileName));
		modelFiles
			.forEach(fileName -> engine.loadModelFile(basedir + fileName));
		return engine;
	}

	protected void executeSaveAndAssert(EdeltaEngine engine, String outputdir, Collection<String> ecoreFiles, Collection<String> modelFiles) throws Exception {
		engine.execute();
		var output = OUTPUT + outputdir;
		engine.save(output);
		ecoreFiles.forEach
			(fileName ->
				assertGeneratedFiles(fileName, outputdir, output, fileName));
		modelFiles.forEach
			(fileName ->
				assertGeneratedFiles(fileName, outputdir, output, fileName));
	}

	protected void assertGeneratedFiles(String message, String subdir, String outputDir, String fileName) {
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