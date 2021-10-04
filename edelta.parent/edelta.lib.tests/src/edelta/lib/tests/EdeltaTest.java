/**
 * 
 */
package edelta.lib.tests;

import static edelta.testutils.EdeltaTestUtils.cleanDirectory;
import static edelta.testutils.EdeltaTestUtils.assertFilesAreEquals;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.eclipse.emf.common.util.WrappedException;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.EcoreFactory;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.xtext.util.Wrapper;
import org.junit.Before;
import org.junit.Test;

import edelta.lib.AbstractEdelta;
import edelta.lib.EdeltaEmptyRuntime;
import edelta.lib.EdeltaIssuePresenter;
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
	private static final String NEW_MODIFIED = "new_modified";
	private static final String MY2_ECORE = "My2.ecore";
	private static final String MY_ECORE = "My.ecore";
	private static final String MY_SUBPACKAGES_ECORE = "MySubPackages.ecore";
	private static final String MY_MAINPACKAGE = "mainpackage";
	private static final String MY_SUBPACKAGE = "subpackage";
	private static final String MY_SUBSUBPACKAGE = "subsubpackage";
	private static final String TESTECORES = "testecores/";

	public static class TestableEdelta extends EdeltaEmptyRuntime {

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
	}

	protected TestableEdelta edelta;

	@Before
	public void init() {
		edelta = new TestableEdelta();
	}

	@Test
	public void testDefaultExecuteDoesNotThrow() throws Exception { // NOSONAR just make sure it runs
		edelta.execute();
	}

	@Test
	public void testLoadEcoreFile() {
		var loadTestEcore = loadTestEcore(MY_ECORE);
		assertThat(((EPackage) loadTestEcore.getContents().get(0)).getName())
			.isEqualTo(MYPACKAGE);
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
	public void testEnsureEPackageIsLoaded() throws EdeltaPackageNotLoadedException { // NOSONAR just make sure it runs
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
		assertFilesAreEquals(
				TESTECORES+"/"+MY_ECORE, MODIFIED+"/"+MY_ECORE);
		assertFilesAreEquals(
				TESTECORES+"/"+MY2_ECORE, MODIFIED+"/"+MY2_ECORE);
	}

	@Test
	public void testSaveModifiedEcoresInNonExistingDirectory() throws IOException {
		var nested = NEW_MODIFIED + "/nested";
		deleteDirectory(nested);
		deleteDirectory(NEW_MODIFIED);
		try {
			loadTestEcore(MY_ECORE);
			loadTestEcore(MY2_ECORE);
			wipeModifiedDirectoryContents();
			edelta.saveModifiedEcores(nested);
			// we did not modify anything so the generated files and the
			// original ones must be the same
			assertFilesAreEquals(
				TESTECORES + "/" + MY_ECORE, nested + "/" + MY_ECORE);
			assertFilesAreEquals(
				TESTECORES + "/" + MY2_ECORE, nested + "/" + MY2_ECORE);
		} finally {
			deleteDirectory(nested);
			deleteDirectory(NEW_MODIFIED);
		}
	}

	@Test
	public void testGetLogger() { // NOSONAR just make sure it runs
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
	public void testSetIssuePresenter() {
		var issuePresenter = mock(EdeltaIssuePresenter.class);
		edelta.setIssuePresenter(issuePresenter);
		EPackage problematicObject = EcoreFactory.eINSTANCE.createEPackage();
		problematicObject.setName("anEPackage");
		edelta.showError(problematicObject, "an error");
		edelta.showWarning(problematicObject, "a warning");
		verify(issuePresenter).showError(problematicObject, "an error");
		verify(issuePresenter).showWarning(problematicObject, "a warning");
	}

	@Test
	public void testSetIssuePresenterPropagatesToChildren() {
		var issuePresenter = mock(EdeltaIssuePresenter.class);
		AbstractEdelta child = new EdeltaEmptyRuntime(edelta);
		AbstractEdelta grandchild = new EdeltaEmptyRuntime(child);
		edelta.setIssuePresenter(issuePresenter);
		EPackage problematicObject = EcoreFactory.eINSTANCE.createEPackage();
		problematicObject.setName("anEPackage");
		child.showError(problematicObject, "an error");
		child.showWarning(problematicObject, "a warning");
		verify(issuePresenter).showError(problematicObject, "an error");
		verify(issuePresenter).showWarning(problematicObject, "a warning");
		grandchild.showError(problematicObject, "an error");
		grandchild.showWarning(problematicObject, "a warning");
		verify(issuePresenter, times(2)).showError(problematicObject, "an error");
		verify(issuePresenter, times(2)).showWarning(problematicObject, "a warning");
	}

	@Test
	public void testIssuePresenterIsPropagatedToChildrenByConstructor() {
		var issuePresenter = mock(EdeltaIssuePresenter.class);
		edelta.setIssuePresenter(issuePresenter);
		AbstractEdelta child = new EdeltaEmptyRuntime(edelta);
		AbstractEdelta grandchild = new EdeltaEmptyRuntime(child);
		EPackage problematicObject = EcoreFactory.eINSTANCE.createEPackage();
		problematicObject.setName("anEPackage");
		child.showError(problematicObject, "an error");
		child.showWarning(problematicObject, "a warning");
		verify(issuePresenter).showError(problematicObject, "an error");
		verify(issuePresenter).showWarning(problematicObject, "a warning");
		grandchild.showError(problematicObject, "an error");
		grandchild.showWarning(problematicObject, "a warning");
		verify(issuePresenter, times(2)).showError(problematicObject, "an error");
		verify(issuePresenter, times(2)).showWarning(problematicObject, "a warning");
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

	private void wipeModifiedDirectoryContents() throws IOException {
		cleanDirectory(MODIFIED);
	}

	private void deleteDirectory(String directory) throws IOException {
		File dir = new File(directory);
		if (dir.exists()) {
			cleanDirectory(directory);
			Files.delete(dir.toPath());
		}
	}

	private Resource loadTestEcore(String ecoreFile) {
		return edelta.loadEcoreFile(TESTECORES+ecoreFile);
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
