/**
 * 
 */
package edelta.lib.tests;

import static edelta.testutils.EdeltaTestUtils.cleanDirectory;
import static edelta.testutils.EdeltaTestUtils.compareFileContents;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.eclipse.emf.common.util.WrappedException;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.ENamedElement;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.EcoreFactory;
import org.eclipse.xtext.util.Wrapper;
import org.junit.Before;
import org.junit.Test;

import edelta.lib.AbstractEdelta;
import edelta.lib.EdeltaEcoreUtil;
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
	private static final String MY_SUBPACKAGES_ECORE = "MySubPackages.ecore";
	private static final String MY_MAINPACKAGE = "mainpackage";
	private static final String MY_SUBPACKAGE = "subpackage";
	private static final String MY_SUBSUBPACKAGE = "subsubpackage";
	private static final String TEST_ECORE_FOR_REFERENCES1 = "TestEcoreForReferences1.ecore";
	private static final String TEST_PACKAGE_FOR_REFERENCES1 = "testecoreforreferences1";
	private static final String TEST_ECORE_FOR_REFERENCES2 = "TestEcoreForReferences2.ecore";
	private static final String TEST_PACKAGE_FOR_REFERENCES2 = "testecoreforreferences2";
	private static final String TESTECORES = "testecores/";

	public static class TestableEdelta extends AbstractEdelta {

		public TestableEdelta() {
			super();
		}

		public TestableEdelta(AbstractEdelta other) {
			super(other);
		}

		@Override
		public void ensureEPackageIsLoaded(String packageName) throws EdeltaPackageNotLoadedException {
			super.ensureEPackageIsLoaded(packageName);
		}

		@Override
		public void runInitializers() {
			super.runInitializers();
		}

	}

	protected TestableEdelta edelta;

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
		tryToRetrieveSomeEPackages();
	}

	@Test
	public void testGetEPackageWithEmptyString() {
		assertNull(edelta.getEPackage(""));
	}

	@Test
	public void testGetEPackageWithOtherEdelta() {
		TestableEdelta other = edelta;
		edelta = new TestableEdelta(other);
		tryToRetrieveSomeEPackages();
	}

	private void tryToRetrieveSomeEPackages() {
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
	public void testGetEEnum() {
		loadTestEcore(MY_ECORE);
		assertNull(edelta.getEEnum(MYPACKAGE, MY_CLASS));
		assertNotNull(edelta.getEEnum(MYPACKAGE, "MyEnum"));
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
	public void testGetEEnumLiteral() {
		loadTestEcore(MY_ECORE);
		assertNull(
			edelta.getEEnumLiteral(MYPACKAGE, "MyDerivedClass", "myDerivedAttribute"));
		assertNotNull(
				edelta.getEEnumLiteral(MYPACKAGE, "MyEnum", "MyEnumLiteral"));
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
	public void testRemoveEClassifier() {
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

	@Test
	public void testRenameEClassifier() {
		loadTestEcore(MY_ECORE);
		// check that the superclass is set
		assertSame(
			edelta.getEClassifier(MYPACKAGE, "MyBaseClass"),
			edelta.getEClass(MYPACKAGE, "MyDerivedClass").getESuperTypes().get(0));
		// modify the ecore model by renaming MyBaseClass
		edelta.getEClassifier(MYPACKAGE, "MyBaseClass").setName("RENAMED");
		// check that MyDerivedClass has the renamed superclass
		assertEquals("RENAMED", edelta.getEClass(MYPACKAGE, "MyDerivedClass").getESuperTypes().get(0).getName());
	}

	@Test
	public void testSaveModifiedEcoresAfterRenamingBaseClass() throws IOException {
		loadTestEcore(MY_ECORE);
		// modify the ecore model by renaming MyBaseClass
		// this will also renaming existing references, so the model
		// is still valid
		edelta.getEClassifier(MYPACKAGE, "MyBaseClass").setName("RENAMED");
		wipeModifiedDirectoryContents();
		edelta.saveModifiedEcores(MODIFIED);
		compareFileContents(
				EXPECTATIONS+"/"+
					"testSaveModifiedEcoresAfterRenamingBaseClass"+"/"+
						MY_ECORE,
				MODIFIED+"/"+MY_ECORE);
	}

	@Test
	public void testCopyENamedElementEOppositeReferenceWorksAcrossEPackages() {
		loadTestEcore(TEST_ECORE_FOR_REFERENCES1);
		loadTestEcore(TEST_ECORE_FOR_REFERENCES2);
		EPackage original1 = edelta.getEPackage(TEST_PACKAGE_FOR_REFERENCES1);
		EPackage original2 = edelta.getEPackage(TEST_PACKAGE_FOR_REFERENCES2);
		EClass person = getEClassByName(original1.getEClassifiers(), "Person");
		EClass workplace = getEClassByName(original2.getEClassifiers(), "WorkPlace");
		EReference works = getEReferenceByName(person.getEStructuralFeatures(), "works");
		EReference persons = getEReferenceByName(workplace.getEStructuralFeatures(), "persons");
		assertSame(works.getEOpposite(), persons);
		assertSame(persons.getEOpposite(), works);
		// perform copy and EOpposite refers to the copied opposite
		// and that is good for us!
		Collection<EPackage> copyEPackages =
			EdeltaEcoreUtil.copyEPackages(Arrays.asList(original1, original2));
		Iterator<EPackage> iterator = copyEPackages.iterator();
		EPackage copied1 = iterator.next();
		EPackage copied2 = iterator.next();
		// the following is not true anymore, since we resolve proxies while copying:
		// everything must be in a resource set, and the resources
		// for the copied EPackages must have the same URIs of the
		// original resources for the references to be resolved.
		person = getEClassByName(copied1.getEClassifiers(), "Person");
		workplace = getEClassByName(copied2.getEClassifiers(), "WorkPlace");
		works = getEReferenceByName(person.getEStructuralFeatures(), "works");
		persons = getEReferenceByName(workplace.getEStructuralFeatures(), "persons");
		assertSame(works.getEOpposite(), persons);
		assertSame(persons.getEOpposite(), works);
	}

	@Test
	public void testGetLogger() {
		edelta.getLogger().info("test message");
	}

	@Test
	public void testLoggers() {
		Wrapper<Boolean> errorSupplierCalled = Wrapper.wrap(false);
		Wrapper<Boolean> warnSupplierCalled = Wrapper.wrap(false);
		Wrapper<Boolean> infoSupplierCalled = Wrapper.wrap(false);
		Wrapper<Boolean> debugSupplierCalled = Wrapper.wrap(false);
		edelta.logError(() -> {
			errorSupplierCalled.set(true);
			return "test logError";
		});
		edelta.logWarn(() -> {
			warnSupplierCalled.set(true);
			return "test logWarn";
		});
		edelta.logInfo(() -> {
			infoSupplierCalled.set(true);
			return "test logInfo";
		});
		edelta.logDebug(() -> {
			debugSupplierCalled.set(true);
			return "test logDebug";
		});
		assertTrue(errorSupplierCalled.get());
		assertTrue(warnSupplierCalled.get());
		assertTrue(infoSupplierCalled.get());
		assertFalse(debugSupplierCalled.get());
	}

	@Test
	public void testShowMethods() {
		Logger logger = spy(edelta.getLogger());
		edelta.setLogger(logger);
		EPackage problematicObject = EcoreFactory.eINSTANCE.createEPackage();
		problematicObject.setName("anEPackage");
		edelta.showError(problematicObject, "an error");
		edelta.showWarning(problematicObject, "a warning");
		verify(logger).log(Level.ERROR, "anEPackage: an error");
		verify(logger).log(Level.WARN, "anEPackage: a warning");
	}

	@Test
	public void testGetSubPackage() {
		loadTestEcore(MY_SUBPACKAGES_ECORE);
		assertNull(edelta.getEPackage(MY_SUBPACKAGE));
		assertNotNull(
			edelta.getEPackage(MY_MAINPACKAGE + "." + MY_SUBPACKAGE));
		assertNotNull(
			edelta.getEPackage(MY_MAINPACKAGE + "." + MY_SUBPACKAGE + "." + MY_SUBSUBPACKAGE));
	}

	@Test
	public void testGetSubPackageEAttribute() {
		loadTestEcore(MY_SUBPACKAGES_ECORE);
		EAttribute eAttribute = edelta.getEAttribute(
			MY_MAINPACKAGE + "." + MY_SUBPACKAGE + "." + MY_SUBSUBPACKAGE,
			MY_CLASS,
			"myAttribute");
		assertNotNull(eAttribute);
		assertEquals(MY_SUBSUBPACKAGE,
			((EClass) eAttribute.eContainer()).getEPackage().getName());
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

	private EClass getEClassByName(List<EClassifier> classifiers, String nameToSearch) {
		return getByName(
				classifiers.stream().
				filter(e -> e instanceof EClass).
				map(e -> (EClass)e).
				collect(Collectors.toList()),
			nameToSearch);
	}

	private EReference getEReferenceByName(List<EStructuralFeature> features, String nameToSearch) {
		return getByName(
				features.stream().
				filter(e -> e instanceof EReference).
				map(e -> (EReference)e).
				collect(Collectors.toList()),
			nameToSearch);
	}

	private <T extends ENamedElement> T getByName(List<T> namedElements, String nameToSearch) {
		return namedElements.
				stream().
				filter(e -> e.getName().contentEquals(nameToSearch)).
				findFirst().orElse(null);
	}
}
