/**
 * 
 */
package edelta.lib.tests;

import static edelta.testutils.EdeltaTestUtils.assertFilesAreEquals;
import static edelta.testutils.EdeltaTestUtils.cleanDirectoryAndFirstSubdirectories;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

import java.io.IOException;

import org.assertj.core.api.Assertions;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.xmi.ClassNotFoundException;
import org.junit.Before;
import org.junit.Test;

import edelta.lib.EdeltaEPackageManager;
import edelta.lib.EdeltaUtils;

/**
 * Tests for the {@link EdeltaEPackageManager}.
 * 
 * @author Lorenzo Bettini
 *
 */
public class EdeltaEPackageManagerTest {

	private static final String MYPACKAGE = "mypackage";
	private static final String MODIFIED = "modified";
	private static final String EXPECTATIONS = "expectations";
	private static final String MY_ECORE = "My.ecore";
	private static final String TESTECORES = "testecores/";
	private static final String TESTDATA = "testdata/";
	private static final String UNCHANGED = "unchanged/";
	private static final String MY_CLASS = "MyClass.xmi";
	private static final String MY_ROOT = "MyRoot.xmi";

	private EdeltaEPackageManager packageManager;

	@Before
	public void init() throws IOException {
		cleanDirectoryAndFirstSubdirectories(MODIFIED);
		packageManager = new EdeltaEPackageManager();
	}

	@Test
	public void testGetEPackage() {
		packageManager.loadEcoreFile(TESTECORES+MY_ECORE);
		var ePackage = packageManager.getEPackage(MYPACKAGE);
		assertNotNull(ePackage);
		var ecorePackage = packageManager.getEPackage("ecore");
		assertSame(EcorePackage.eINSTANCE, ecorePackage);
	}

	@Test
	public void testSaveModifiedEcoresAfterRemovingBaseClass() throws IOException {
		packageManager.loadEcoreFile(TESTECORES+MY_ECORE);
		// modify the ecore model by removing MyBaseClass
		var ePackage = packageManager.getEPackage(MYPACKAGE);
		// modify the ecore model by removing MyBaseClass
		// this will also remove existing references, so the model
		// is still valid
		EdeltaUtils.removeElement(
			ePackage.getEClassifier("MyBaseClass"));
		packageManager.saveEcores(MODIFIED);
		assertFilesAreEquals(
				EXPECTATIONS+"/"+
					"testSaveModifiedEcoresAfterRemovingBaseClass"+"/"+
						MY_ECORE,
				MODIFIED+"/"+MY_ECORE);
	}

	@Test
	public void testSaveModels() throws IOException {
		packageManager.loadEcoreFile(TESTDATA+UNCHANGED+MY_ECORE);
		packageManager.loadModelFile(TESTDATA+UNCHANGED+MY_CLASS);
		packageManager.loadModelFile(TESTDATA+UNCHANGED+MY_ROOT);
		packageManager.saveEcores(MODIFIED);
		packageManager.saveModels(MODIFIED);
		assertFilesAreEquals(
				EXPECTATIONS+"/"+UNCHANGED+"/"+	MY_ECORE,
				MODIFIED+"/"+MY_ECORE);
		assertFilesAreEquals(
				EXPECTATIONS+"/"+UNCHANGED+"/"+	MY_CLASS,
				MODIFIED+"/"+MY_CLASS);
		assertFilesAreEquals(
				EXPECTATIONS+"/"+UNCHANGED+"/"+	MY_ROOT,
				MODIFIED+"/"+MY_ROOT);
	}

	@Test
	public void testSaveModelsAfterRemovingClass() throws IOException {
		packageManager.loadEcoreFile(TESTDATA+UNCHANGED+MY_ECORE);
		packageManager.loadModelFile(TESTDATA+UNCHANGED+MY_ROOT);

		var ePackage = packageManager.getEPackage(MYPACKAGE);
		// modify the ecore model by removing MyBaseClass
		// this will also remove existing references, so the ecore model
		// is still valid
		EdeltaUtils.removeElement(
			ePackage.getEClassifier("MyClass"));

		var subdir = "manuallyRemovedClass";
		var output = MODIFIED+"/"+subdir;
		packageManager.saveEcores(output);
		assertFilesAreEquals(
				EXPECTATIONS+"/"+subdir+"/"+MY_ECORE,
				output+"/"+MY_ECORE);
		packageManager.saveModels(output);
		assertFilesAreEquals(
				EXPECTATIONS+"/"+subdir+"/"+MY_ROOT,
				output+"/"+MY_ROOT);

		Assertions.assertThatThrownBy(
			() -> packageManager.loadModelFile(TESTDATA+UNCHANGED+MY_CLASS))
			.hasCauseInstanceOf(ClassNotFoundException.class)
			.hasMessageContaining(MY_CLASS);
	}
}
