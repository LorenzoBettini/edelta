package edelta.lib.tests;

import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.Test;

import edelta.lib.EdeltaDefaultRuntime;
import edelta.lib.EdeltaEngine;
import edelta.lib.EdeltaModelManager;
import edelta.lib.EdeltaResourceUtils;

public class EdeltaEngineTest {

	private static final String MYPACKAGE = "mypackage";
	private static final String MODIFIED = "modified";
	private static final String EXPECTATIONS = "expectations";
	private static final String MY_ECORE = "My.ecore";
	private static final String TESTECORES = "testecores/";
	private static final String TESTDATA = "testdata/";
	private static final String SIMPLE_TEST_DATA = "simpleTestData/";
	private static final String MY_CLASS = "MyClass.xmi";
	private static final String MY_ROOT = "MyRoot.xmi";

	static public class TestEdeltaRuntime extends EdeltaDefaultRuntime {
		public TestEdeltaRuntime(EdeltaModelManager other) {
			super(other);
		}
	};

	@Test
	public void testCreationAndExecution() {
		var engine = new EdeltaEngine(TestEdeltaRuntime.class);
		var ecoreResource = engine.loadEcoreFile(TESTDATA+SIMPLE_TEST_DATA+MY_ECORE);
		var ePackage = EdeltaResourceUtils.getEPackage(ecoreResource);
		assertNotNull(ePackage);
		var model = engine.loadModelFile(TESTDATA+SIMPLE_TEST_DATA+MY_CLASS);
		assertEquals("MyClass",
			model.getContents().get(0).eClass().getName());
		engine.execute();
	}
}
