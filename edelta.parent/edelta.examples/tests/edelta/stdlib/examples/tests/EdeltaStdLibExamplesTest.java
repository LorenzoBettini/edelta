package edelta.stdlib.examples.tests;

import static edelta.testutils.EdeltaTestUtils.assertFilesAreEquals;
import static java.util.List.of;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.Collection;

import org.junit.BeforeClass;
import org.junit.Test;

import edelta.lib.EdeltaEngine;
import edelta.lib.EdeltaEngine.EdeltaRuntimeProvider;
import edelta.stdlib.examples.ChangeReferenceTypeExample;
import edelta.stdlib.examples.ChangeReferenceTypeManualExample;
import edelta.stdlib.examples.ChangeReferenceTypeMultipleExample;
import edelta.stdlib.examples.ChangeToAbstractExample;
import edelta.testutils.EdeltaTestUtils;

public class EdeltaStdLibExamplesTest {

	private static final String TESTDATA = "model/";
	private static final String OUTPUT = "modified/";
	private static final String EXPECTATIONS = "expectations/";

	@BeforeClass
	public static void clearOutput() throws IOException {
		EdeltaTestUtils.cleanDirectoryRecursive(OUTPUT);
	}

	private EdeltaEngine setupEngine(
			// String subdir, not used for the moment
			Collection<String> ecoreFiles,
			Collection<String> modelFiles,
			EdeltaRuntimeProvider runtimeProvider
		) {
		var basedir = TESTDATA; // subdir is not used for the moment
		var engine = new EdeltaEngine(runtimeProvider);
		ecoreFiles
			.forEach(fileName -> engine.loadEcoreFile(basedir + fileName));
		modelFiles
			.forEach(fileName -> engine.loadModelFile(basedir + fileName));
		return engine;
	}

	@Test
	public void testChangeReferenceType() throws Exception {
		var subdir = "ChangeReferenceType/";
		var ecores = of("PersonListForChangeType.ecore");
		var models = of("ListForChangeType.xmi");

		var engine = setupEngine(
			ecores,
			models,
			ChangeReferenceTypeExample::new);

		executeSaveAndAssert(engine,
			subdir,
			ecores,
			models
		);
	}

	@Test
	public void testChangeReferenceTypeMultiple() throws Exception {
		var subdir = "ChangeReferenceTypeMultiple/";
		var ecores = of("PersonListForChangeType.ecore");
		var models = of("ListForChangeType.xmi");

		var engine = setupEngine(
			ecores,
			models,
			ChangeReferenceTypeMultipleExample::new);

		executeSaveAndAssert(engine,
			subdir,
			ecores,
			models
		);
	}

	@Test
	public void testChangeReferenceTypeManual() throws Exception {
		var subdir = "ChangeReferenceType/";
		var ecores = of("PersonListForChangeType.ecore");
		var models = of("ListForChangeType.xmi");

		var engine = setupEngine(
			ecores,
			models,
			ChangeReferenceTypeManualExample::new);

		executeSaveAndAssert(engine,
			subdir,
			ecores,
			models
		);
	}

	@Test
	public void testChangeToAbstract() throws Exception {
		var subdir = "ChangeToAbstract/";
		var ecores = of("PersonListForChangeToAbstract.ecore");
		var models = of("ListForChangeToAbstract.xmi");

		var engine = setupEngine(
			ecores,
			models,
			ChangeToAbstractExample::new);

		executeSaveAndAssert(engine,
			subdir,
			ecores,
			models
		);
	}

	private void executeSaveAndAssert(
			EdeltaEngine engine,
			String outputdir,
			Collection<String> ecoreFiles,
			Collection<String> modelFiles
		) throws Exception {
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
