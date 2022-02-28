/**
 * 
 */
package edelta.lib.tests;

import static edelta.testutils.EdeltaTestUtils.assertFilesAreEquals;
import static edelta.testutils.EdeltaTestUtils.cleanDirectoryAndFirstSubdirectories;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

import java.io.IOException;

import org.assertj.core.api.Assertions;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.ecore.xmi.ClassNotFoundException;
import org.eclipse.emf.ecore.xmi.XMIResource;
import org.junit.Before;
import org.junit.Test;

import edelta.lib.EdeltaModelManager;
import edelta.lib.EdeltaUtils;

/**
 * Tests for the {@link EdeltaModelManager}.
 * 
 * @author Lorenzo Bettini
 *
 */
public class EdeltaModelManagerTest {

	private static final String MYPACKAGE = "mypackage";
	private static final String MODIFIED = "modified";
	private static final String EXPECTATIONS = "expectations";
	private static final String MY_ECORE = "My.ecore";
	private static final String TESTECORES = "testecores/";
	private static final String TESTDATA = "testdata/";
	private static final String SIMPLE_TEST_DATA = "simpleTestData/";
	private static final String MY_CLASS = "MyClass.xmi";
	private static final String MY_ROOT = "MyRoot.xmi";

	private EdeltaModelManager modelManager;

	@Before
	public void init() throws IOException {
		cleanDirectoryAndFirstSubdirectories(MODIFIED);
		modelManager = new EdeltaModelManager();
	}

	@Test
	public void testGetEPackage() {
		modelManager.loadEcoreFile(TESTECORES+MY_ECORE);
		var ePackage = modelManager.getEPackage(MYPACKAGE);
		assertNotNull(ePackage);
		var ecorePackage = modelManager.getEPackage("ecore");
		assertSame(EcorePackage.eINSTANCE, ecorePackage);
	}

	@Test
	public void testSaveModifiedEcoresAfterRemovingBaseClass() throws IOException {
		modelManager.loadEcoreFile(TESTECORES+MY_ECORE);
		// modify the ecore model by removing MyBaseClass
		var ePackage = modelManager.getEPackage(MYPACKAGE);
		// modify the ecore model by removing MyBaseClass
		// this will also remove existing references, so the model
		// is still valid
		EdeltaUtils.removeElement(
			ePackage.getEClassifier("MyBaseClass"));
		modelManager.saveEcores(MODIFIED);
		assertFilesAreEquals(
				EXPECTATIONS+"/"+
					"testSaveModifiedEcoresAfterRemovingBaseClass"+"/"+
						MY_ECORE,
				MODIFIED+"/"+MY_ECORE);
	}

	@Test
	public void testSaveModels() throws IOException {
		modelManager.loadEcoreFile(TESTDATA+SIMPLE_TEST_DATA+MY_ECORE);
		modelManager.loadModelFile(TESTDATA+SIMPLE_TEST_DATA+MY_CLASS);
		modelManager.loadModelFile(TESTDATA+SIMPLE_TEST_DATA+MY_ROOT);
		modelManager.saveEcores(MODIFIED);
		modelManager.saveModels(MODIFIED);
		assertFilesAreEquals(
				EXPECTATIONS+"/"+SIMPLE_TEST_DATA+"/"+	MY_ECORE,
				MODIFIED+"/"+MY_ECORE);
		assertFilesAreEquals(
				EXPECTATIONS+"/"+SIMPLE_TEST_DATA+"/"+	MY_CLASS,
				MODIFIED+"/"+MY_CLASS);
		assertFilesAreEquals(
				EXPECTATIONS+"/"+SIMPLE_TEST_DATA+"/"+	MY_ROOT,
				MODIFIED+"/"+MY_ROOT);
	}

	@Test
	public void testSaveModelsAfterRemovingClass() throws IOException {
		modelManager.loadEcoreFile(TESTDATA+SIMPLE_TEST_DATA+MY_ECORE);
		modelManager.loadModelFile(TESTDATA+SIMPLE_TEST_DATA+MY_ROOT);

		var ePackage = modelManager.getEPackage(MYPACKAGE);
		// modify the ecore model by removing MyBaseClass
		// this will also remove existing references, so the ecore model
		// is still valid
		EdeltaUtils.removeElement(
			ePackage.getEClassifier("MyClass"));

		var subdir = "manuallyRemovedClass";
		var output = MODIFIED+"/"+subdir;
		modelManager.saveEcores(output);
		assertFilesAreEquals(
				EXPECTATIONS+"/"+subdir+"/"+MY_ECORE,
				output+"/"+MY_ECORE);
		modelManager.saveModels(output);
		assertFilesAreEquals(
				EXPECTATIONS+"/"+subdir+"/"+MY_ROOT,
				output+"/"+MY_ROOT);

		Assertions.assertThatThrownBy(
			() -> modelManager.loadModelFile(TESTDATA+SIMPLE_TEST_DATA+MY_CLASS))
			.hasCauseInstanceOf(ClassNotFoundException.class)
			.hasMessageContaining(MY_CLASS);
	}

	@Test
	public void testSaveModelAfterCreatingResource() throws IOException {
		var additionalModelManager = new EdeltaModelManager();
		additionalModelManager.loadEcoreFile(TESTDATA+SIMPLE_TEST_DATA+MY_ECORE);
		var prototypeResource = (XMIResource) additionalModelManager.loadModelFile(TESTDATA+SIMPLE_TEST_DATA+MY_CLASS);

		// note that we use the same prototypeResource to create the
		// two new resources, since we're only interested in the prototype's
		// options and encoding. In this test this should be enough,
		// since the models we create are based on the same ecore
		modelManager.loadEcoreFile(TESTDATA+SIMPLE_TEST_DATA+MY_ECORE);
		var myClassModelResource = modelManager
			.createModelResource(TESTDATA+SIMPLE_TEST_DATA+MY_CLASS, prototypeResource);
		var myRootModelResource = modelManager
			.createModelResource(TESTDATA+SIMPLE_TEST_DATA+MY_ROOT, prototypeResource);
		var myClassEClass = (EClass)
			modelManager.getEPackage(MYPACKAGE).getEClassifier("MyClass");
		myClassModelResource.getContents().add(EcoreUtil.create(myClassEClass));
		var myRootEClass = (EClass)
			modelManager.getEPackage(MYPACKAGE).getEClassifier("MyRoot");
		myRootModelResource.getContents().add(EcoreUtil.create(myRootEClass));

		var subdir = "manuallyCreatedResource";
		var output = MODIFIED+"/"+subdir;
		modelManager.saveEcores(output);
		modelManager.saveModels(output);
		assertFilesAreEquals(
				EXPECTATIONS+"/"+subdir+"/"+MY_ECORE,
				output+"/"+MY_ECORE);
		assertFilesAreEquals(
				EXPECTATIONS+"/"+subdir+"/"+MY_CLASS,
				output+"/"+MY_CLASS);
		assertFilesAreEquals(
				EXPECTATIONS+"/"+subdir+"/"+MY_ROOT,
				output+"/"+MY_ROOT);
	}

	@Test
	public void testGetModelResourceMap() {
		modelManager.loadEcoreFile(TESTDATA+SIMPLE_TEST_DATA+MY_ECORE);
		modelManager.loadModelFile(TESTDATA+SIMPLE_TEST_DATA+MY_CLASS);
		modelManager.loadModelFile(TESTDATA+SIMPLE_TEST_DATA+MY_ROOT);
		var map = modelManager.getModelResourceMap();
		assertThat(map.keySet())
			.containsExactlyInAnyOrder(
					TESTDATA+SIMPLE_TEST_DATA+MY_CLASS,
					TESTDATA+SIMPLE_TEST_DATA+MY_ROOT);
	}
}
