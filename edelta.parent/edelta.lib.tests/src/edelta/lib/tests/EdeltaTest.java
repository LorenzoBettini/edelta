/**
 * 
 */
package edelta.lib.tests;

import static edelta.testutils.EdeltaTestUtils.cleanDirectory;
import static edelta.testutils.EdeltaTestUtils.compareFileContents;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.List;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.WrappedException;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EGenericType;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.EcoreFactory;
import org.eclipse.emf.ecore.impl.BasicEObjectImpl;
import org.eclipse.emf.ecore.impl.EGenericTypeImpl;
import org.eclipse.emf.ecore.util.EcoreUtil;
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
	private static final String TEST_ECORE_FOR_REFERENCES = "TestEcoreForReferences.ecore";
	private static final String TEST_PACKAGE_FOR_REFERENCES = "testecoreforreferences";
	private static final String TESTECORES = "testecores/";

	protected static final class TestableEdelta extends AbstractEdelta {

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

	{
		new EdeltaEcoreUtil() {
			// just to have code coverage of protected constructor
		};
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
	public void testChangeEClass() throws IOException {
		loadTestEcore(MY_ECORE);
		EPackage ePackage = edelta.getEPackage(MYPACKAGE);
		assertNotNull(ePackage.getEClassifier(MY_CLASS));
		EClass cl = edelta.changeEClass(MYPACKAGE, MY_CLASS, null);
		assertSame(cl, ePackage.getEClassifier(MY_CLASS));
	}

	@Test
	public void testChangeEClassLaterInitialization() throws IOException {
		loadTestEcore(MY_ECORE);
		// refers to an EClass that is created later
		EClass cl1 = edelta.changeEClass(MYPACKAGE, MY_CLASS,
			edelta.createList(
				c -> c.getESuperTypes().add(edelta.getEClass(MYPACKAGE, "NewClass2")),
				c -> c.getESuperTypes().add(edelta.getEClass(MYPACKAGE, "NewClass3"))
			)
		);
		EClass newClass2 = edelta.createEClass(MYPACKAGE, "NewClass2",
			edelta.createList(edelta::fooConsumer));
		EClass newClass3 = edelta.createEClass(MYPACKAGE, "NewClass3", null);
		edelta.runInitializers();
		assertSame(newClass2, cl1.getESuperTypes().get(0));
		assertSame(newClass3, cl1.getESuperTypes().get(1));
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

	@Test
	public void testRenameEClassifier() throws IOException {
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
	public void testCopyEClassifier() throws IOException {
		loadTestEcore(MY_ECORE);
		// modify the ecore model by copying MyBaseClass
		EClass original = edelta.getEClass(MYPACKAGE, "MyDerivedClass");
		EClass copy = (EClass) edelta.copyEClassifier(MYPACKAGE, "MyDerivedClass");
		// check that the copy has the same attributes as the original one (in turn, copied)
		EList<EStructuralFeature> originalFeatures = original.getEAllStructuralFeatures();
		EList<EStructuralFeature> copiedFeatures = copy.getEAllStructuralFeatures();
		assertEquals(originalFeatures.size(), copiedFeatures.size());
		assertSame(original.getESuperTypes().get(0), copy.getESuperTypes().get(0));
		// inherited features are references so they're not copied
		assertSame(originalFeatures.get(0), copiedFeatures.get(0));
		assertSame(originalFeatures.get(1), copiedFeatures.get(1));
		// declared features are instead copied
		assertNotSame(originalFeatures.get(2), copiedFeatures.get(2));
		assertNotSame(originalFeatures.get(3), copiedFeatures.get(3));
		assertEquals(originalFeatures.get(2).getName(), copiedFeatures.get(2).getName());
		assertEquals(originalFeatures.get(3).getName(), copiedFeatures.get(3).getName());
	}

	@Test
	public void testCopyEClassifierDoesNotResolveProxies() throws IOException {
		// We use EGenericType for playing with references and proxies, since using
		// EClass.superTypes for that does not seem to be easy...
		loadTestEcore(TEST_ECORE_FOR_REFERENCES);
		// modify the ecore model by copying MyBaseClass
		EClass original = edelta.getEClass(TEST_PACKAGE_FOR_REFERENCES, "MyClass");
		EClass referred = edelta.getEClass(TEST_PACKAGE_FOR_REFERENCES, "MyReferredType");
		EGenericType genericType = original.getETypeParameters().get(0).getEBounds().get(0);
		EClassifier eClassifier = genericType.getEClassifier();
		assertNull(eClassifier);
		// explicitly set proxy for the reference EGenericType.eClassifier
		eClassifier = EcoreFactory.eINSTANCE.createEClass();
		((BasicEObjectImpl) eClassifier).eSetProxyURI(EcoreUtil.getURI(referred));
		assertTrue(eClassifier.eIsProxy());
		genericType.setEClassifier(eClassifier);
		// perform copy and make sure proxy resolution is not triggered during the copy
		EClass copy = (EClass) edelta.copyEClassifier(TEST_PACKAGE_FOR_REFERENCES, "MyClass");
		EGenericType genericTypeCopied = copy.getETypeParameters().get(0).getEBounds().get(0);
		// use basicGet, otherwise we trigger resolution of proxies
		eClassifier = ((EGenericTypeImpl)genericTypeCopied).basicGetEClassifier();
		assertTrue(eClassifier.eIsProxy());
		// proxy resolution is not triggered in the original object either
		eClassifier = ((EGenericTypeImpl)genericType).basicGetEClassifier();
		assertTrue(eClassifier.eIsProxy());
	}

	@Test
	public void testSaveModifiedEcoresAfterCopyingDerivedClass() throws IOException {
		loadTestEcore(MY_ECORE);
		// modify the ecore model by copying MyDerivedClass
		// and then rename it to avoid having duplicates in the saved ecore
		// which would not be valid
		EClassifier copy = edelta.copyEClassifier(MYPACKAGE, "MyDerivedClass");
		copy.setName("COPIED");
		EPackage p = edelta.getEPackage(MYPACKAGE);
		p.getEClassifiers().add(copy);
		wipeModifiedDirectoryContents();
		edelta.saveModifiedEcores(MODIFIED);
		compareFileContents(
				EXPECTATIONS+"/"+
					"testSaveModifiedEcoresAfterCopyingDerivedClass"+"/"+
						MY_ECORE,
				MODIFIED+"/"+MY_ECORE);
	}

	@Test
	public void testCopyEClassifierIntoEPackageDoesNotResolveProxies() throws IOException {
		// We use EGenericType for playing with references and proxies, since using
		// EClass.superTypes for that does not seem to be easy...
		loadTestEcore(TEST_ECORE_FOR_REFERENCES);
		// modify the ecore model by copying MyBaseClass
		EClass original = edelta.getEClass(TEST_PACKAGE_FOR_REFERENCES, "MyClass");
		EClass referred = edelta.getEClass(TEST_PACKAGE_FOR_REFERENCES, "MyReferredType");
		EGenericType genericType = original.getETypeParameters().get(0).getEBounds().get(0);
		EClassifier eClassifier = genericType.getEClassifier();
		assertNull(eClassifier);
		// explicitly set proxy for the reference EGenericType.eClassifier
		eClassifier = EcoreFactory.eINSTANCE.createEClass();
		((BasicEObjectImpl) eClassifier).eSetProxyURI(EcoreUtil.getURI(referred));
		assertTrue(eClassifier.eIsProxy());
		genericType.setEClassifier(eClassifier);
		// perform copy and make sure proxy resolution is not triggered during the copy
		EPackage copiedEPackage = EdeltaEcoreUtil.copyENamedElement(edelta.getEPackage(TEST_PACKAGE_FOR_REFERENCES));
		EClass copy = (EClass) EdeltaEcoreUtil.copyEClassifierIntoEPackage(copiedEPackage, original);
		// copied EClassifier is the first one
		assertSame(
			copy,
			copiedEPackage.getEClassifiers().get(0)
		);
		EGenericType genericTypeCopied = copy.getETypeParameters().get(0).getEBounds().get(0);
		// use basicGet, otherwise we trigger resolution of proxies
		eClassifier = ((EGenericTypeImpl)genericTypeCopied).basicGetEClassifier();
		assertTrue(eClassifier.eIsProxy());
		// proxy resolution is not triggered in the original object either
		eClassifier = ((EGenericTypeImpl)genericType).basicGetEClassifier();
		assertTrue(eClassifier.eIsProxy());
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
