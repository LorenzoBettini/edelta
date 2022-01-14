/**
 * 
 */
package edelta.lib.tests;

import static edelta.testutils.EdeltaTestUtils.assertFilesAreEquals;
import static edelta.testutils.EdeltaTestUtils.cleanDirectory;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

import java.io.IOException;

import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EcorePackage;
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

	private EdeltaEPackageManager packageManager;

	@Before
	public void init() throws IOException {
		wipeModifiedDirectoryContents();
		packageManager = new EdeltaEPackageManager();
	}

	@Test
	public void testGetEPackage() {
		loadTestEcore(MY_ECORE);
		var ePackage = packageManager.getEPackage(MYPACKAGE);
		assertNotNull(ePackage);
		var ecorePackage = packageManager.getEPackage("ecore");
		assertSame(EcorePackage.eINSTANCE, ecorePackage);
	}

	@Test
	public void testSaveModifiedEcoresAfterRemovingBaseClass() throws IOException {
		loadTestEcore(MY_ECORE);
		// modify the ecore model by removing MyBaseClass
		EPackage ePackage = packageManager.getEPackage(MYPACKAGE);
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

	private void wipeModifiedDirectoryContents() throws IOException {
		cleanDirectory(MODIFIED);
	}

	private void loadTestEcore(String ecoreFile) {
		packageManager.loadEcoreFile(TESTECORES+ecoreFile);
	}

}
