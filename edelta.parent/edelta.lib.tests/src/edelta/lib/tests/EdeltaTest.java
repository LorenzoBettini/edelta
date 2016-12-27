/**
 * 
 */
package edelta.lib.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.File;
import java.io.IOException;

import org.eclipse.emf.common.util.WrappedException;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.junit.Before;
import org.junit.Test;

import edelta.lib.AbstractEdelta;
import edelta.lib.exception.EdeltaPackageNotLoadedException;

/**
 * Tests for the base class of generated Edelta programs.
 * 
 * @author Lorenzo Bettini
 *
 */
public class EdeltaTest {

	private static final String MYOTHERPACKAGE = "myotherpackage";
	private static final String MYPACKAGE = "mypackage";
	private static final String MODIFIED = "modified";
	private static final String MY2_ECORE = "My2.ecore";
	private static final String MY_ECORE = "My.ecore";
	private static final String TESTECORES = "testecores/";

	private static final class TestableEdelta extends AbstractEdelta {
		@Override
		public void ensureEPackageIsLoaded(String packageName) throws EdeltaPackageNotLoadedException {
			super.ensureEPackageIsLoaded(packageName);
		}
	}

	private TestableEdelta edelta;

	@Before
	public void init() {
		edelta = new TestableEdelta();
	}

	@Test
	public void testDefaultExecuteDoesNotThrow() throws Exception {
		edelta.execute();
	}

	@Test
	public void testLoadEcoreFile() {
		loadTestEcore(MY_ECORE);
	}

	@Test(expected=WrappedException.class)
	public void testLoadNonExistantEcoreFile() {
		edelta.loadEcoreFile("foo.ecore");
	}

	@Test
	public void testGetEPackage() {
		loadTestEcore(MY_ECORE);
		loadTestEcore(MY2_ECORE);
		EPackage ePackage = edelta.getEPackage(MYPACKAGE);
		assertEquals(MYPACKAGE, ePackage.getName());
		assertNotNull(ePackage);
		assertNotNull(edelta.getEPackage(MYOTHERPACKAGE));
		assertNull(edelta.getEPackage("foo"));
	}

	@Test
	public void testGetEClassifier() {
		loadTestEcore(MY_ECORE);
		loadTestEcore(MY2_ECORE);
		assertNotNull(edelta.getEClassifier(MYPACKAGE, "MyClass"));
		assertNotNull(edelta.getEClassifier(MYPACKAGE, "MyDataType"));
		// wrong package
		assertNull(edelta.getEClassifier(MYOTHERPACKAGE, "MyDataType"));
		// package does not exist
		assertNull(edelta.getEClassifier("foo", "MyDataType"));
	}

	@Test
	public void testGetEClass() {
		loadTestEcore(MY_ECORE);
		assertNotNull(edelta.getEClass(MYPACKAGE, "MyClass"));
		assertNull(edelta.getEClass(MYPACKAGE, "MyDataType"));
	}

	@Test
	public void testGetEDataType() {
		loadTestEcore(MY_ECORE);
		assertNull(edelta.getEDataType(MYPACKAGE, "MyClass"));
		assertNotNull(edelta.getEDataType(MYPACKAGE, "MyDataType"));
	}

	@Test
	public void testEnsureEPackageIsLoaded() throws EdeltaPackageNotLoadedException {
		loadTestEcore(MY_ECORE);
		edelta.ensureEPackageIsLoaded(MYPACKAGE);
	}

	@Test(expected=EdeltaPackageNotLoadedException.class)
	public void testEnsureEPackageIsLoadedWhenNotLoaded() throws EdeltaPackageNotLoadedException {
		edelta.ensureEPackageIsLoaded(MYPACKAGE);
	}

	@Test
	public void testGetEStructuralFeature() {
		loadTestEcore(MY_ECORE);
		assertEStructuralFeature(
			edelta.getEStructuralFeature(MYPACKAGE, "MyDerivedClass", "myBaseAttribute"),
				"myBaseAttribute");
		assertEStructuralFeature(
			edelta.getEStructuralFeature(MYPACKAGE, "MyDerivedClass", "myDerivedAttribute"),
				"myDerivedAttribute");
	}

	@Test
	public void testGetEAttribute() {
		loadTestEcore(MY_ECORE);
		assertEAttribute(
			edelta.getEAttribute(MYPACKAGE, "MyDerivedClass", "myBaseAttribute"),
				"myBaseAttribute");
		assertEAttribute(
			edelta.getEAttribute(MYPACKAGE, "MyDerivedClass", "myDerivedAttribute"),
				"myDerivedAttribute");
	}

	@Test
	public void testGetEReference() {
		loadTestEcore(MY_ECORE);
		assertEReference(
			edelta.getEReference(MYPACKAGE, "MyDerivedClass", "myBaseReference"),
				"myBaseReference");
		assertEReference(
			edelta.getEReference(MYPACKAGE, "MyDerivedClass", "myDerivedReference"),
				"myDerivedReference");
	}

	@Test
	public void testGetEStructuralFeatureWithNonExistantClass() {
		loadTestEcore(MY_ECORE);
		assertNull(
			edelta.getEStructuralFeature(MYPACKAGE, "foo", "foo"));
	}

	@Test
	public void testGetEStructuralFeatureWithNonExistantFeature() {
		loadTestEcore(MY_ECORE);
		assertNull(
			edelta.getEStructuralFeature(MYPACKAGE, "MyDerivedClass", "foo"));
	}

	@Test
	public void testGetEAttributeWithEReference() {
		loadTestEcore(MY_ECORE);
		assertNull(
			edelta.getEAttribute(MYPACKAGE, "MyDerivedClass", "myDerivedReference"));
	}

	@Test
	public void testGetEReferenceWithEAttribute() {
		loadTestEcore(MY_ECORE);
		assertNull(
			edelta.getEReference(MYPACKAGE, "MyDerivedClass", "myDerivedAttribute"));
	}

	@Test
	public void testSaveModifiedEcores() throws IOException {
		loadTestEcore(MY_ECORE);
		loadTestEcore(MY2_ECORE);
		wipeModifiedDirectoryContents();
		edelta.saveModifiedEcores(MODIFIED);
		// we did not modify anything so the generated files and the
		// original ones must be the same
		EPackage p1 = edelta.getEPackage(MYPACKAGE);
		TestableEdelta edelta2 = new TestableEdelta();
		edelta2.loadEcoreFile(MODIFIED+"/"+MY_ECORE);
		EPackage p2 = edelta2.getEPackage(MYPACKAGE);
		assertEquals(p1.getName(), p2.getName());
	}

	private void wipeModifiedDirectoryContents() {
		File dir = new File(MODIFIED);
		for (File file : dir.listFiles())
			if (!file.isDirectory() && !file.getName().equals(".gitignore"))
				file.delete();
	}

	private void loadTestEcore(String ecoreFile) {
		edelta.loadEcoreFile(TESTECORES+ecoreFile);
	}

	private void assertEAttribute(EAttribute f, String expectedName) {
		assertEStructuralFeature(f, expectedName);
	}

	private void assertEReference(EReference f, String expectedName) {
		assertEStructuralFeature(f, expectedName);
	}

	private void assertEStructuralFeature(EStructuralFeature f, String expectedName) {
		assertEquals(expectedName, f.getName());
	}
}
