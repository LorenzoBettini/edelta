/**
 * 
 */
package edelta.lib.tests;

import static edelta.testutils.EdeltaTestUtils.cleanDirectory;
import static edelta.testutils.EdeltaTestUtils.compareFileContents;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import java.io.IOException;
import java.util.List;

import org.eclipse.emf.common.util.WrappedException;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
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

	private static final String MY_CLASS = "MyClass";
	private static final String MYOTHERPACKAGE = "myotherpackage";
	private static final String MYPACKAGE = "mypackage";
	private static final String MODIFIED = "modified";
	private static final String EXPECTATIONS = "expectations";
	private static final String MY2_ECORE = "My2.ecore";
	private static final String MY_ECORE = "My.ecore";
	private static final String TESTECORES = "testecores/";

	private static final class TestableEdelta extends AbstractEdelta {
		@Override
		public void ensureEPackageIsLoaded(String packageName) throws EdeltaPackageNotLoadedException {
			super.ensureEPackageIsLoaded(packageName);
		}

		@Override
		public void runInitializers() {
			super.runInitializers();
		}

		@Override
		public <E> List<E> createList(E e) {
			return super.createList(e);
		}

		@Override
		public <E> List<E> createList(E e1, E e2) {
			return super.createList(e1, e2);
		}

		public void fooConsumer(EClass e) {
			
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
	public void testEcorePackageIsAlwaysAvailable() {
		EPackage ePackage = edelta.getEPackage("ecore");
		assertNotNull(ePackage);
		assertEquals("ecore", ePackage.getName());
	}

	@Test
	public void testEcoreStuffIsAlwaysAvailable() {
		assertNotNull(edelta.getEClassifier("ecore", "EClass"));
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
		assertNotNull(edelta.getEClassifier(MYPACKAGE, MY_CLASS));
		assertNotNull(edelta.getEClassifier(MYPACKAGE, "MyDataType"));
		// wrong package
		assertNull(edelta.getEClassifier(MYOTHERPACKAGE, "MyDataType"));
		// package does not exist
		assertNull(edelta.getEClassifier("foo", "MyDataType"));
	}

	@Test
	public void testGetEClass() {
		loadTestEcore(MY_ECORE);
		assertNotNull(edelta.getEClass(MYPACKAGE, MY_CLASS));
		assertNull(edelta.getEClass(MYPACKAGE, "MyDataType"));
	}

	@Test
	public void testGetEDataType() {
		loadTestEcore(MY_ECORE);
		assertNull(edelta.getEDataType(MYPACKAGE, MY_CLASS));
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
		compareFileContents(
				TESTECORES+"/"+MY_ECORE, MODIFIED+"/"+MY_ECORE);
		compareFileContents(
				TESTECORES+"/"+MY2_ECORE, MODIFIED+"/"+MY2_ECORE);
	}

	@Test
	public void testSaveModifiedEcoresAfterRemovingBaseClass() throws IOException {
		loadTestEcore(MY_ECORE);
		// modify the ecore model by removing MyBaseClass
		EPackage ePackage = edelta.getEPackage(MYPACKAGE);
		ePackage.getEClassifiers().remove(
			edelta.getEClass(MYPACKAGE, "MyBaseClass"));
		// also unset it as a superclass, or the model won't be valid
		edelta.getEClass(MYPACKAGE, "MyDerivedClass").getESuperTypes().clear();
		wipeModifiedDirectoryContents();
		edelta.saveModifiedEcores(MODIFIED);
		compareFileContents(
				EXPECTATIONS+"/"+
					"testSaveModifiedEcoresAfterRemovingBaseClass"+"/"+
						MY_ECORE,
				MODIFIED+"/"+MY_ECORE);
	}

	@Test
	public void testSaveModifiedEcoresAfterRemovingBaseClass2() throws IOException {
		loadTestEcore(MY_ECORE);
		// modify the ecore model by removing MyBaseClass
		// this will also remove existing references, so the model
		// is still valid
		edelta.removeEClassifier(MYPACKAGE, "MyBaseClass");
		wipeModifiedDirectoryContents();
		edelta.saveModifiedEcores(MODIFIED);
		compareFileContents(
				EXPECTATIONS+"/"+
					"testSaveModifiedEcoresAfterRemovingBaseClass"+"/"+
						MY_ECORE,
				MODIFIED+"/"+MY_ECORE);
	}

	@Test
	public void testCreateEClass() throws IOException {
		loadTestEcore(MY_ECORE);
		EPackage ePackage = edelta.getEPackage(MYPACKAGE);
		assertNull(ePackage.getEClassifier("NewClass"));
		EClass createEClass = edelta.createEClass(MYPACKAGE, "NewClass", null);
		assertSame(createEClass, ePackage.getEClassifier("NewClass"));
	}

	@Test
	public void testCreateEClassLaterInitialization() throws IOException {
		loadTestEcore(MY_ECORE);
		// refers to an EClass that is created later
		EClass newClass1 = edelta.createEClass(MYPACKAGE, "NewClass1",
			edelta.createList(
				c -> c.getESuperTypes().add(edelta.getEClass(MYPACKAGE, "NewClass2")),
				c -> c.getESuperTypes().add(edelta.getEClass(MYPACKAGE, "NewClass3"))
			)
		);
		EClass newClass2 = edelta.createEClass(MYPACKAGE, "NewClass2",
			edelta.createList(edelta::fooConsumer));
		EClass newClass3 = edelta.createEClass(MYPACKAGE, "NewClass3", null);
		edelta.runInitializers();
		assertSame(newClass2, newClass1.getESuperTypes().get(0));
		assertSame(newClass3, newClass1.getESuperTypes().get(1));
	}

	@Test
	public void testCreateEAttribute() throws IOException {
		loadTestEcore(MY_ECORE);
		EPackage ePackage = edelta.getEPackage(MYPACKAGE);
		EClass eClass = (EClass) ePackage.getEClassifier(MY_CLASS);
		assertNull(eClass.getEStructuralFeature("newAttribute"));
		EAttribute createEAttribute = edelta.createEAttribute(eClass, "newAttribute", null);
		assertSame(createEAttribute, eClass.getEStructuralFeature("newAttribute"));
	}

	@Test
	public void testCreateEAttributeLaterInitialization() throws IOException {
		loadTestEcore(MY_ECORE);
		EPackage ePackage = edelta.getEPackage(MYPACKAGE);
		EClass eClass = (EClass) ePackage.getEClassifier(MY_CLASS);
		EAttribute createEAttribute1 = edelta.createEAttribute(eClass, "newAttribute",
			edelta.createList(
				a -> a.setName("newAttribute1"),
				a -> edelta.getEAttribute(MYPACKAGE, MY_CLASS, "newAttribute2").setName("changed")
			)
		);
		EAttribute createEAttribute2 = edelta.createEAttribute(eClass, "newAttribute2", null);
		edelta.runInitializers();
		// make sure the initializers have been called
		// the second attribute must have a different name, changed
		assertSame(createEAttribute2, edelta.getEAttribute(MYPACKAGE, MY_CLASS, "changed"));
		// the same for the first attribute
		assertSame(createEAttribute1, edelta.getEAttribute(MYPACKAGE, MY_CLASS, "newAttribute1"));
	}

	@Test
	public void testRemoveEClassifier() throws IOException {
		loadTestEcore(MY_ECORE);
		// check that the superclass is set
		assertSame(
			edelta.getEClassifier(MYPACKAGE, "MyBaseClass"),
			edelta.getEClass(MYPACKAGE, "MyDerivedClass").getESuperTypes().get(0));
		// modify the ecore model by removing MyBaseClass
		edelta.removeEClassifier(MYPACKAGE, "MyBaseClass");
		// check that MyDerivedClass is not its subclass anymore
		assertEquals(0, edelta.getEClass(MYPACKAGE, "MyDerivedClass").getESuperTypes().size());
	}

	private void wipeModifiedDirectoryContents() {
		cleanDirectory(MODIFIED);
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
