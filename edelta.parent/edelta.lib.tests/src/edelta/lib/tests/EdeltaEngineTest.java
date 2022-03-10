package edelta.lib.tests;

import static edelta.lib.EdeltaEcoreUtil.createInstance;
import static edelta.testutils.EdeltaTestUtils.assertFilesAreEquals;
import static edelta.testutils.EdeltaTestUtils.cleanDirectoryRecursive;
import static java.util.Collections.emptyList;
import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.util.List;

import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.junit.BeforeClass;
import org.junit.Test;

import edelta.lib.AbstractEdelta;
import edelta.lib.EdeltaEngine;
import edelta.lib.EdeltaModelMigrator;
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
		var engine = new EdeltaEngine(other -> 
			new AbstractEdelta(other) {
				/**
				 * The implementation doesn't have to make sense:
				 * it's just to verify that Ecore and models are
				 * evolved as expected.
				 */
				@Override
				protected void doExecute() {
					var myClass = getEClass(MYPACKAGE, "MyClass");
					var myRoot = getEClass(MYPACKAGE, "MyRoot");
					myClass.setName("Renamed");
					var firstAttribute =
						(EAttribute) myClass.getEStructuralFeatures().get(0);
					modelMigration(migrator -> {
						turnMyClassAttributeValueToUpperCase(firstAttribute, migrator);
						createCustomInstanceOfMyRoot(myClass, myRoot, migrator);
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
		var myClassModel = engine.loadModelFile(TESTDATA+SIMPLE_TEST_DATA+MY_CLASS);
		assertEquals("MyClass",
			myClassModel.getContents().get(0).eClass().getName());
		engine.loadModelFile(TESTDATA+SIMPLE_TEST_DATA+MY_ROOT);
		engine.execute();
		// make sure the original Ecore is not changed
		assertEquals("MyClass", eClass.getName());

		var subdir = "engineModification/";
		engine.save(OUTPUT + subdir);
		assertGeneratedFiles(subdir, MY_ECORE);
		assertGeneratedFiles(subdir, MY_CLASS);
		assertGeneratedFiles(subdir, MY_ROOT);
	}

	/**
	 * An Edelta calling a Library
	 * 
	 * @throws Exception
	 */
	@Test
	public void testCreationAndExecutionSimulatingLibraries() throws Exception {
		class TestLib extends AbstractEdelta {
			public TestLib(AbstractEdelta other) {
				super(other);
			}

			/**
			 * Simulates a library method.
			 */
			public void testLibMethod() {
				var myClass = getEClass(MYPACKAGE, "MyClass");
				myClass.setName("Renamed");
				var firstAttribute =
					(EAttribute) myClass.getEStructuralFeatures().get(0);
				modelMigration(migrator -> {
					turnMyClassAttributeValueToUpperCase(firstAttribute, migrator);
				});
			}
		};
		class MyEDelta extends AbstractEdelta {
			TestLib testLib;

			public MyEDelta(AbstractEdelta other) {
				super(other);
				testLib = new TestLib(other);
			}

			/**
			 * The implementation doesn't have to make sense:
			 * it's just to verify that Ecore and models are
			 * evolved as expected.
			 */
			@Override
			public void doExecute() throws Exception {
				var myClass = getEClass(MYPACKAGE, "MyClass");
				var myRoot = getEClass(MYPACKAGE, "MyRoot");
				modelMigration(migrator -> {
					createCustomInstanceOfMyRoot(myClass, myRoot, migrator);
				});
				testLib.testLibMethod();
			}
		}
		var engine = new EdeltaEngine(other -> 
			new MyEDelta(other)
		);
		engine.loadEcoreFile(
				TESTDATA+SIMPLE_TEST_DATA+MY_ECORE);
		engine.loadModelFile(TESTDATA+SIMPLE_TEST_DATA+MY_CLASS);
		engine.loadModelFile(TESTDATA+SIMPLE_TEST_DATA+MY_ROOT);
		engine.execute();

		var subdir = "engineModificationWithLib/";
		engine.save(OUTPUT + subdir);
		assertGeneratedFiles(subdir, MY_ECORE);
		assertGeneratedFiles(subdir, MY_CLASS);
		assertGeneratedFiles(subdir, MY_ROOT);
	}

	/**
	 * An Edelta calling two Libraries
	 * 
	 * @throws Exception
	 */
	@Test
	public void testCreationAndExecutionSimulatingLibraries2() throws Exception {
		class TestLib1 extends AbstractEdelta {
			public TestLib1(AbstractEdelta other) {
				super(other);
			}

			/**
			 * Simulates a library method.
			 */
			public void testLib1Method() {
				var myClass = getEClass(MYPACKAGE, "MyClass");
				myClass.setName("Renamed");
				var firstAttribute =
					(EAttribute) myClass.getEStructuralFeatures().get(0);
				modelMigration(migrator -> {
					turnMyClassAttributeValueToUpperCase(firstAttribute, migrator);
				});
			}
		};
		class TestLib2 extends AbstractEdelta {
			public TestLib2(AbstractEdelta other) {
				super(other);
			}

			/**
			 * Simulates a library method.
			 */
			public void testLib2Method() {
				var myClass = getEClass(MYPACKAGE, "MyClass");
				var myRoot = getEClass(MYPACKAGE, "MyRoot");
				modelMigration(migrator -> {
					createCustomInstanceOfMyRoot(myClass, myRoot, migrator);
				});
			}
		};
		class MyEDelta extends AbstractEdelta {
			TestLib1 testLib1;
			TestLib2 testLib2;

			public MyEDelta(AbstractEdelta other) {
				super(other);
				testLib1 = new TestLib1(other);
				testLib2 = new TestLib2(other);
			}

			/**
			 * The implementation doesn't have to make sense:
			 * it's just to verify that Ecore and models are
			 * evolved as expected.
			 */
			@Override
			public void doExecute() throws Exception {
				testLib2.testLib2Method();
				testLib1.testLib1Method();
			}
		}
		var engine = new EdeltaEngine(other -> 
			new MyEDelta(other)
		);
		engine.loadEcoreFile(
				TESTDATA+SIMPLE_TEST_DATA+MY_ECORE);
		engine.loadModelFile(TESTDATA+SIMPLE_TEST_DATA+MY_CLASS);
		engine.loadModelFile(TESTDATA+SIMPLE_TEST_DATA+MY_ROOT);
		engine.execute();

		var subdir = "engineModificationWithLib/";
		engine.save(OUTPUT + subdir);
		assertGeneratedFiles(subdir, MY_ECORE);
		assertGeneratedFiles(subdir, MY_CLASS);
		assertGeneratedFiles(subdir, MY_ROOT);
	}

	/**
	 * An Edelta calling a Library calling a Library
	 * 
	 * @throws Exception
	 */
	@Test
	public void testCreationAndExecutionSimulatingLibraries3() throws Exception {
		class TestLib1 extends AbstractEdelta {
			public TestLib1(AbstractEdelta other) {
				super(other);
			}

			/**
			 * Simulates a library method.
			 */
			public void testLib1Method() {
				var myClass = getEClass(MYPACKAGE, "MyClass");
				myClass.setName("Renamed");
				var firstAttribute =
					(EAttribute) myClass.getEStructuralFeatures().get(0);
				modelMigration(migrator -> {
					turnMyClassAttributeValueToUpperCase(firstAttribute, migrator);
				});
			}
		};
		class TestLib2 extends AbstractEdelta {
			TestLib1 testLib1;

			public TestLib2(AbstractEdelta other) {
				super(other);
				testLib1 = new TestLib1(other);
			}

			/**
			 * Simulates a library method.
			 */
			public void testLib2Method() {
				var myClass = getEClass(MYPACKAGE, "MyClass");
				var myRoot = getEClass(MYPACKAGE, "MyRoot");
				modelMigration(migrator -> {
					createCustomInstanceOfMyRoot(myClass, myRoot, migrator);
				});
				testLib1.testLib1Method();
			}
		};
		class MyEDelta extends AbstractEdelta {
			TestLib2 testLib2;

			public MyEDelta(AbstractEdelta other) {
				super(other);
				testLib2 = new TestLib2(other);
			}

			/**
			 * The implementation doesn't have to make sense:
			 * it's just to verify that Ecore and models are
			 * evolved as expected.
			 */
			@Override
			public void doExecute() throws Exception {
				testLib2.testLib2Method();
			}
		}
		var engine = new EdeltaEngine(other -> 
			new MyEDelta(other)
		);
		engine.loadEcoreFile(
				TESTDATA+SIMPLE_TEST_DATA+MY_ECORE);
		engine.loadModelFile(TESTDATA+SIMPLE_TEST_DATA+MY_CLASS);
		engine.loadModelFile(TESTDATA+SIMPLE_TEST_DATA+MY_ROOT);
		engine.execute();

		var subdir = "engineModificationWithLib/";
		engine.save(OUTPUT + subdir);
		assertGeneratedFiles(subdir, MY_ECORE);
		assertGeneratedFiles(subdir, MY_CLASS);
		assertGeneratedFiles(subdir, MY_ROOT);
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

	/**
	 * turn the MyClass objects values to uppercase
	 * 
	 * @param firstAttribute
	 * @param migrator
	 */
	private void turnMyClassAttributeValueToUpperCase(EAttribute firstAttribute, EdeltaModelMigrator migrator) {
		migrator.transformAttributeValueRule(
			migrator.isRelatedTo(firstAttribute),
			(feature, oldVal, newVal) -> {
				return newVal.toString().toUpperCase();
			}
		);
	}

	/**
	 * completely creates new contents and references for MyRoot objects
	 * 
	 * @param myClass
	 * @param myRoot
	 * @param migrator
	 */
	private void createCustomInstanceOfMyRoot(EClass myClass, EClass myRoot, EdeltaModelMigrator migrator) {
		migrator.createInstanceRule(
			migrator.isRelatedTo(myRoot),
			oldObj -> {
				var oldClass = oldObj.eClass();
				oldObj.eSet(oldClass.getEStructuralFeatures().get(0), emptyList());
				oldObj.eSet(oldClass.getEStructuralFeatures().get(1), emptyList());
				return createInstance(
					myRoot,
					newObj -> {
						// clear the old values or they will be copied by the
						// default implementation of EcoreUtil.copy
						var myContents = myRoot.getEStructuralFeature("myContents");
						var myReferences = myRoot.getEStructuralFeature("myReferences");
						var myClassAttribute = myClass.getEStructuralFeatures().get(0);
						// set the new values completely
						var contents = List.of(
							createInstance(myClass, o -> {
								o.eSet(myClassAttribute, "Created");
							}),
							createInstance(myClass, o -> {
								o.eSet(myClassAttribute, "Created and referred");
							})
						);
						var references = List.of(
							contents.get(1)
						);
						newObj.eSet(myContents, contents);
						newObj.eSet(myReferences, references);
					}
				);
			}
		);
	}

}
