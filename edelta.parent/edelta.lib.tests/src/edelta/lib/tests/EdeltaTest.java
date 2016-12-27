/**
 * 
 */
package edelta.lib.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

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
		loadTestEcore("My.ecore");
	}

	@Test(expected=WrappedException.class)
	public void testLoadNonExistantEcoreFile() {
		edelta.loadEcoreFile("foo.ecore");
	}

	@Test
	public void testGetEPackage() {
		loadTestEcore("My.ecore");
		loadTestEcore("My2.ecore");
		EPackage ePackage = edelta.getEPackage("mypackage");
		assertEquals("mypackage", ePackage.getName());
		assertNotNull(ePackage);
		assertNotNull(edelta.getEPackage("myotherpackage"));
		assertNull(edelta.getEPackage("foo"));
	}

	@Test
	public void testGetEClassifier() {
		loadTestEcore("My.ecore");
		loadTestEcore("My2.ecore");
		assertNotNull(edelta.getEClassifier("mypackage", "MyClass"));
		assertNotNull(edelta.getEClassifier("mypackage", "MyDataType"));
		// wrong package
		assertNull(edelta.getEClassifier("myotherpackage", "MyDataType"));
		// package does not exist
		assertNull(edelta.getEClassifier("foo", "MyDataType"));
	}

	@Test
	public void testGetEClass() {
		loadTestEcore("My.ecore");
		assertNotNull(edelta.getEClass("mypackage", "MyClass"));
		assertNull(edelta.getEClass("mypackage", "MyDataType"));
	}

	@Test
	public void testGetEDataType() {
		loadTestEcore("My.ecore");
		assertNull(edelta.getEDataType("mypackage", "MyClass"));
		assertNotNull(edelta.getEDataType("mypackage", "MyDataType"));
	}

	@Test
	public void testEnsureEPackageIsLoaded() throws EdeltaPackageNotLoadedException {
		loadTestEcore("My.ecore");
		edelta.ensureEPackageIsLoaded("mypackage");
	}

	@Test(expected=EdeltaPackageNotLoadedException.class)
	public void testEnsureEPackageIsLoadedWhenNotLoaded() throws EdeltaPackageNotLoadedException {
		edelta.ensureEPackageIsLoaded("mypackage");
	}

	@Test
	public void testGetEStructuralFeature() {
		loadTestEcore("My.ecore");
		assertEStructuralFeature(
			edelta.getEStructuralFeature("mypackage", "MyDerivedClass", "myBaseAttribute"),
				"myBaseAttribute");
		assertEStructuralFeature(
			edelta.getEStructuralFeature("mypackage", "MyDerivedClass", "myDerivedAttribute"),
				"myDerivedAttribute");
	}

	@Test
	public void testGetEAttribute() {
		loadTestEcore("My.ecore");
		assertEAttribute(
			edelta.getEAttribute("mypackage", "MyDerivedClass", "myBaseAttribute"),
				"myBaseAttribute");
		assertEAttribute(
			edelta.getEAttribute("mypackage", "MyDerivedClass", "myDerivedAttribute"),
				"myDerivedAttribute");
	}

	@Test
	public void testGetEReference() {
		loadTestEcore("My.ecore");
		assertEReference(
			edelta.getEReference("mypackage", "MyDerivedClass", "myBaseReference"),
				"myBaseReference");
		assertEReference(
			edelta.getEReference("mypackage", "MyDerivedClass", "myDerivedReference"),
				"myDerivedReference");
	}

	@Test
	public void testGetEStructuralFeatureWithNonExistantClass() {
		loadTestEcore("My.ecore");
		assertNull(
			edelta.getEStructuralFeature("mypackage", "foo", "foo"));
	}

	@Test
	public void testGetEStructuralFeatureWithNonExistantFeature() {
		loadTestEcore("My.ecore");
		assertNull(
			edelta.getEStructuralFeature("mypackage", "MyDerivedClass", "foo"));
	}

	@Test
	public void testGetEAttributeWithEReference() {
		loadTestEcore("My.ecore");
		assertNull(
			edelta.getEAttribute("mypackage", "MyDerivedClass", "myDerivedReference"));
	}

	@Test
	public void testGetEReferenceWithEAttribute() {
		loadTestEcore("My.ecore");
		assertNull(
			edelta.getEReference("mypackage", "MyDerivedClass", "myDerivedAttribute"));
	}

	@Test
	public void testSaveModifiedEcores() throws IOException {
		loadTestEcore("My.ecore");
		loadTestEcore("My2.ecore");
		edelta.saveModifiedEcores("modified");
	}

	private void loadTestEcore(String ecoreFile) {
		edelta.loadEcoreFile("testecores/"+ecoreFile);
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
