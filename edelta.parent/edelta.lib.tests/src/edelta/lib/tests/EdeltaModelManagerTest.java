/**
 * 
 */
package edelta.lib.tests;

import static edelta.testutils.EdeltaTestUtils.assertFilesAreEquals;
import static edelta.testutils.EdeltaTestUtils.cleanDirectoryAndFirstSubdirectories;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.assertj.core.api.Assertions;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.ecore.xmi.ClassNotFoundException;
import org.eclipse.emf.ecore.xmi.XMIResource;
import org.junit.Before;
import org.junit.Test;

import edelta.lib.EdeltaModelManager;
import edelta.lib.EdeltaUtils;
import edelta.lib.exception.EdeltaPackageNotLoadedException;

/**
 * Tests for the {@link EdeltaModelManager}.
 * 
 * @author Lorenzo Bettini
 *
 */
public class EdeltaModelManagerTest {

	private static final String EXPECTATIONS = "../edelta.testdata/expectations";
	private static final String TESTDATA = "../edelta.testdata/testdata/";

	private static final String MYPACKAGE = "mypackage";
	private static final String MODIFIED = "modified";
	private static final String MY_ECORE = "My.ecore";
	private static final String TESTECORES = "testecores/";
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
		var prototypeEcoreResource =
			(XMIResource) additionalModelManager.loadEcoreFile(TESTDATA+SIMPLE_TEST_DATA+MY_ECORE);
		var prototypeMyClassModelResource =
			(XMIResource) additionalModelManager.loadModelFile(TESTDATA+SIMPLE_TEST_DATA+MY_CLASS);
		var prototypeMyRootModelResource =
			(XMIResource) additionalModelManager.loadModelFile(TESTDATA+SIMPLE_TEST_DATA+MY_ROOT);

		modelManager.createEcoreResource(prototypeEcoreResource);
		modelManager.loadEcoreFile(TESTDATA+SIMPLE_TEST_DATA+MY_ECORE);
		var myClassModelResource = modelManager
			.createModelResource(prototypeMyClassModelResource);
		var myRootModelResource = modelManager
			.createModelResource(prototypeMyRootModelResource);
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
	public void testClearModel() {
		modelManager.loadEcoreFile(TESTDATA+SIMPLE_TEST_DATA+MY_ECORE);
		modelManager.loadModelFile(TESTDATA+SIMPLE_TEST_DATA+MY_CLASS);
		modelManager.loadModelFile(TESTDATA+SIMPLE_TEST_DATA+MY_ROOT);
		assertThat(modelManager.getModelResources())
			.hasSize(2);
		assertThat(modelManager.getEcoreResources())
			.hasSize(1);
		modelManager.clearModels();
		assertThat(modelManager.getModelResources())
			.isEmpty();
		assertThat(modelManager.getEcoreResources())
			.hasSize(1);
	}

	@Test
	public void testCopyEcores() {
		var otherModelManager = new EdeltaModelManager();
		var ecoreResource =
			(XMIResource) otherModelManager.loadEcoreFile(TESTDATA+SIMPLE_TEST_DATA+MY_ECORE);

		var map = modelManager.copyEcores(otherModelManager);

		Iterable<EObject> originalContents = () -> 
			EcoreUtil.getAllContents(ecoreResource, true);

		// NOT exactly, because we don't care about GenericType elements
		assertThat(map.keySet())
			.containsAnyElementsOf(originalContents);

		Iterable<EObject> copiedContents = () -> 
			EcoreUtil.getAllContents(
				modelManager.getEcoreResources().iterator().next(),
				true);

		assertThat(map.values())
			.containsAnyElementsOf(copiedContents);
	}

	@Test
	public void testRegisterNsURI() throws IOException, EdeltaPackageNotLoadedException {
		var ecoreSubdir = TESTDATA+"version-migration/rename/metamodels";
		try (var stream = Files.walk(Paths.get(ecoreSubdir))) {
			stream
				.filter(file -> !Files.isDirectory(file))
				.filter(file -> file.toString().endsWith(".ecore"))
				.forEach(file -> modelManager.loadEcoreFile(file.toString()));
		}
		var thrown = assertThrows(EdeltaPackageNotLoadedException.class,
			() -> modelManager.registerEPackageByNsURI("PersonList", "foo"));
		assertEquals("EPackage with name 'PersonList' and nsURI 'foo' not loaded.", thrown.getMessage());
		modelManager.registerEPackageByNsURI("PersonList", "http://cs.gssi.it/PersonMM/v2");
		var registered = modelManager.getEPackage("PersonList");
		assertEquals("http://cs.gssi.it/PersonMM/v2", registered.getNsURI());
		modelManager.registerEPackageByNsURI("mypackage", "http://my.package.org/v2");
		registered = modelManager.getEPackage("mypackage");
		assertEquals("http://my.package.org/v2", registered.getNsURI());
		modelManager.registerEPackageByNsURI("mypackage", "http://my.package.org");
		registered = modelManager.getEPackage("mypackage");
		assertEquals("http://my.package.org", registered.getNsURI());
	}

	@Test
	public void testLoadFromInputStream() throws IOException, EdeltaPackageNotLoadedException {
		var ecoreFile = TESTDATA+"version-migration/rename/metamodels/v1/PersonList.ecore";
		InputStream inputStream = new FileInputStream(ecoreFile);
		modelManager.loadEcoreFile("PersonList file", inputStream);
		modelManager.registerEPackageByNsURI("PersonList", "http://cs.gssi.it/PersonMM/v1");
		var registered = modelManager.getEPackage("PersonList");
		assertEquals("http://cs.gssi.it/PersonMM/v1", registered.getNsURI());
	}
}
