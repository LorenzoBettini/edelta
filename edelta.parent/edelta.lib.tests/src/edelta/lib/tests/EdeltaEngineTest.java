package edelta.lib.tests;

import static edelta.testutils.EdeltaTestUtils.assertFilesAreEquals;
import static edelta.testutils.EdeltaTestUtils.cleanDirectoryRecursive;
import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;

import org.eclipse.emf.ecore.EAttribute;
import org.junit.BeforeClass;
import org.junit.Test;

import edelta.lib.AbstractEdelta;
import edelta.lib.EdeltaEngine;
import edelta.lib.EdeltaResourceUtils;

public class EdeltaEngineTest {

	private static final String OUTPUT = "output/";
	private static final String EXPECTATIONS = "expectations/";

	private static final String MYPACKAGE = "mypackage";
	private static final String MY_ECORE = "My.ecore";
	private static final String TESTDATA = "testdata/";
	private static final String SIMPLE_TEST_DATA = "simpleTestData/";
	private static final String MY_CLASS = "MyClass.xmi";
	private static final String MY_ROOT = "MyRoot.xmi";

	@BeforeClass
	static public void clearOutput() throws IOException {
		cleanDirectoryRecursive(OUTPUT);
	}

	@Test
	public void testCreationAndExecution() throws Exception {
		var engine = new EdeltaEngine(modelManager -> 
			new AbstractEdelta(modelManager) {
				@Override
				protected void doExecute() {
					var myClass = getEClass(MYPACKAGE, "MyClass");
					myClass.setName("Renamed");
					var firstAttribute =
						(EAttribute) myClass.getEStructuralFeatures().get(0);
					modelMigration(migrator -> {
						migrator.transformAttributeValueRule(
							migrator.isRelatedTo(firstAttribute),
							(feature, oldVal, newVal) -> {
								return newVal.toString().toUpperCase();
							}
						);
					});
				}
			}
		);
		var ecoreResource = engine.loadEcoreFile(
				TESTDATA+SIMPLE_TEST_DATA+MY_ECORE);
		var ePackage = EdeltaResourceUtils.getEPackage(ecoreResource);
		assertNotNull(ePackage);
		var eClass = ePackage.getEClassifier("MyClass");
		assertNotNull(eClass);
		var model = engine.loadModelFile(TESTDATA+SIMPLE_TEST_DATA+MY_CLASS);
		assertEquals("MyClass",
			model.getContents().get(0).eClass().getName());
		engine.execute();
		// make sure the original Ecore is not changed
		assertEquals("MyClass", eClass.getName());

		var subdir = "engineModification/";
		engine.save(OUTPUT + subdir);
		assertGeneratedFiles(subdir, MY_ECORE);
		assertGeneratedFiles(subdir, MY_CLASS);
	}

	private void assertGeneratedFiles(String subdir, String fileName) {
		try {
			assertFilesAreEquals(
				fileName,
				EXPECTATIONS + subdir + fileName,
				OUTPUT + subdir + fileName);
		} catch (IOException e) {
			e.printStackTrace();
			fail(e.getClass().getName() + ": " + e.getMessage());
		}
	}

}
