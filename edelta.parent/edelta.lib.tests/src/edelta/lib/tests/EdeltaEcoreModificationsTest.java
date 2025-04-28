/**
 *
 */
package edelta.lib.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.ENamedElement;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.junit.Before;
import org.junit.Test;

import edelta.lib.EdeltaDefaultRuntime;
import edelta.lib.EdeltaModelManager;
import edelta.lib.EdeltaRuntime;
import edelta.lib.EdeltaUtils;

/**
 * Tests manipulations of Ecore models.
 *
 * @author Lorenzo Bettini
 *
 */
public class EdeltaEcoreModificationsTest {

	private static final String MYPACKAGE = "mypackage";
	private static final String MY_ECORE = "My.ecore";
	private static final String TEST_ECORE_FOR_REFERENCES1 = "TestEcoreForReferences1.ecore";
	private static final String TEST_PACKAGE_FOR_REFERENCES1 = "testecoreforreferences1";
	private static final String TEST_ECORE_FOR_REFERENCES2 = "TestEcoreForReferences2.ecore";
	private static final String TEST_PACKAGE_FOR_REFERENCES2 = "testecoreforreferences2";
	private static final String TESTECORES = "testecores/";

	protected EdeltaRuntime edelta;

	protected EdeltaModelManager modelManager;

	@Before
	public void init() {
		modelManager = new EdeltaModelManager();
		edelta = new EdeltaDefaultRuntime(modelManager);
	}

	@Test
	public void testRemoveEClassifier() {
		loadTestEcore(MY_ECORE);
		// check that the superclass is set
		assertSame(
			edelta.getEClassifier(MYPACKAGE, "MyBaseClass"),
			edelta.getEClass(MYPACKAGE, "MyDerivedClass").getESuperTypes().get(0));
		// modify the ecore model by removing MyBaseClass
		EdeltaUtils.removeElement(
			edelta.getEClassifier(MYPACKAGE, "MyBaseClass"));
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
	public void testCopyENamedElementEOppositeReferenceWorksAcrossEPackages() {
		loadTestEcore(TEST_ECORE_FOR_REFERENCES1);
		loadTestEcore(TEST_ECORE_FOR_REFERENCES2);
		var original1 = edelta.getEPackage(TEST_PACKAGE_FOR_REFERENCES1);
		var original2 = edelta.getEPackage(TEST_PACKAGE_FOR_REFERENCES2);
		var person = getEClassByName(original1.getEClassifiers(), "Person");
		var workplace = getEClassByName(original2.getEClassifiers(), "WorkPlace");
		var works = getEReferenceByName(person.getEStructuralFeatures(), "works");
		var persons = getEReferenceByName(workplace.getEStructuralFeatures(), "persons");
		assertSame(works.getEOpposite(), persons);
		assertSame(persons.getEOpposite(), works);
		// perform copy and EOpposite refers to the copied opposite
		// and that is good for us!
		Collection<EPackage> copyEPackages =
			EcoreUtil.copyAll(Arrays.asList(original1, original2));
		var iterator = copyEPackages.iterator();
		var copied1 = iterator.next();
		var copied2 = iterator.next();
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

	private void loadTestEcore(String ecoreFile) {
		modelManager.loadEcoreFile(TESTECORES+ecoreFile);
	}

	private EClass getEClassByName(List<EClassifier> classifiers, String nameToSearch) {
		return getByName(
				classifiers.stream().
				filter(EClass.class::isInstance).
				map(e -> (EClass)e).
				toList(),
			nameToSearch);
	}

	private EReference getEReferenceByName(List<EStructuralFeature> features, String nameToSearch) {
		return getByName(
				features.stream().
				filter(EReference.class::isInstance).
				map(e -> (EReference)e).
				toList(),
			nameToSearch);
	}

	private <T extends ENamedElement> T getByName(List<T> namedElements, String nameToSearch) {
		return namedElements.
				stream().
				filter(e -> e.getName().contentEquals(nameToSearch)).
				findFirst().orElse(null);
	}
}
