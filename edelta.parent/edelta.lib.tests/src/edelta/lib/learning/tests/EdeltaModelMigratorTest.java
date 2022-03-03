package edelta.lib.learning.tests;

import static edelta.testutils.EdeltaTestUtils.assertFilesAreEquals;
import static edelta.testutils.EdeltaTestUtils.cleanDirectoryRecursive;
import static java.util.Arrays.asList;
import static java.util.List.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.assertj.core.api.Assertions;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EEnum;
import org.eclipse.emf.ecore.EEnumLiteral;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.xtext.xbase.lib.StringExtensions;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import edelta.lib.EdeltaEcoreUtil;
import edelta.lib.EdeltaModelManager;
import edelta.lib.EdeltaModelMigrator;
import edelta.lib.EdeltaUtils;

public class EdeltaModelMigratorTest {

	private static final String TESTDATA = "testdata/";
	private static final String OUTPUT = "output/";
	private static final String EXPECTATIONS = "expectations/";

	/**
	 * This stores the original ecores and models, and it's initially shared with
	 * the model migrator; howver, during the evolution the migrator will update its
	 * version of the original model manager, so in the tests, after the first model
	 * migration, this model manager should NOT be used anymore in the tests, since
	 * it refers to stale elements.
	 */
	EdeltaModelManager originalModelManager;

	/**
	 * This is always the most recent model manager, which is meant to be used for
	 * simulating refactorings and model evolutions
	 */
	EdeltaModelManager evolvingModelManager;

	@BeforeClass
	public static void clearOutput() throws IOException {
		cleanDirectoryRecursive(OUTPUT);
	}

	@Before
	public void setup() {
		originalModelManager = new EdeltaModelManager();
		evolvingModelManager = new EdeltaModelManager();
	}

	private EdeltaModelMigrator setupMigrator(
			String subdir,
			Collection<String> ecoreFiles,
			Collection<String> modelFiles
		) {
		var basedir = TESTDATA + subdir;
		ecoreFiles
			.forEach(fileName -> originalModelManager.loadEcoreFile(basedir + fileName));
		modelFiles
			.forEach(fileName -> originalModelManager.loadModelFile(basedir + fileName));
		var modelMigrator = new EdeltaModelMigrator(basedir, originalModelManager, evolvingModelManager);
		return modelMigrator;
	}

	@Test
	public void testCopyUnchanged() throws IOException {
		var subdir = "simpleTestData/";
		var modelMigrator = setupMigrator(
			subdir,
			of("My.ecore"),
			of("MyRoot.xmi", "MyClass.xmi")
		);

		copyModelsSaveAndAssertOutputs(
			modelMigrator,
			subdir,
			subdir,
			of("My.ecore"),
			of("MyRoot.xmi", "MyClass.xmi")
		);
	}

	@Test
	public void testCopyUnchangedWithEnums() throws IOException {
		var subdir = "simpleTestData/";
		var modelMigrator = setupMigrator(
			subdir,
			of("PersonList.ecore"),
			of("List.xmi")
		);

		copyModelsSaveAndAssertOutputs(
			modelMigrator,
			subdir,
			subdir,
			of("PersonList.ecore"),
			of("List.xmi")
		);
	}

	@Test
	public void testCopyUnchangedClassesWithTheSameNameInDifferentPackages() throws IOException {
		var subdir = "classesWithTheSameName/";

		var modelMigrator = setupMigrator(
			subdir,
			of("My1.ecore", "My2.ecore"),
			of("MyRoot1.xmi", "MyClass1.xmi", "MyRoot2.xmi", "MyClass2.xmi")
		);

		copyModelsSaveAndAssertOutputs(
			modelMigrator,
			subdir,
			subdir,
			of("My1.ecore", "My2.ecore"),
			of("MyRoot1.xmi", "MyClass1.xmi", "MyRoot2.xmi", "MyClass2.xmi")
		);
	}

	@Test
	public void testCopyMutualReferencesUnchanged() throws IOException {
		var subdir = "mutualReferencesUnchanged/";

		var modelMigrator = setupMigrator(
			subdir,
			of("PersonForReferences.ecore", "WorkPlaceForReferences.ecore"),
			of("Person1.xmi", "Person2.xmi", "WorkPlace1.xmi")
		);

		copyModelsSaveAndAssertOutputs(
			modelMigrator,
			subdir,
			subdir,
			of("PersonForReferences.ecore", "WorkPlaceForReferences.ecore"),
			of("Person1.xmi", "Person2.xmi", "WorkPlace1.xmi")
		);
	}

	@Test
	public void testRenamedClass() throws IOException {
		var subdir = "simpleTestData/";

		var modelMigrator = setupMigrator(
			subdir,
			of("My.ecore"),
			of("MyRoot.xmi", "MyClass.xmi")
		);

		// refactoring of Ecore
		evolvingModelManager.getEPackage("mypackage").getEClassifier("MyClass")
			.setName("MyClassRenamed");
		evolvingModelManager.getEPackage("mypackage").getEClassifier("MyRoot")
			.setName("MyRootRenamed");

		copyModelsSaveAndAssertOutputs(
			modelMigrator,
			subdir,
			"renamedClass/",
			of("My.ecore"),
			of("MyRoot.xmi", "MyClass.xmi")
		);
	}

	@Test
	public void testRenamedFeature() throws IOException {
		var subdir = "simpleTestData/";

		var modelMigrator = setupMigrator(
			subdir,
			of("My.ecore"),
			of("MyRoot.xmi", "MyClass.xmi")
		);

		// refactoring of Ecore
		getFeature(evolvingModelManager, 
				"mypackage", "MyRoot", "myReferences")
			.setName("myReferencesRenamed");
		getFeature(evolvingModelManager, 
				"mypackage", "MyRoot", "myContents")
			.setName("myContentsRenamed");

		copyModelsSaveAndAssertOutputs(
			modelMigrator,
			subdir,
			"renamedFeature/",
			of("My.ecore"),
			of("MyRoot.xmi", "MyClass.xmi")
		);
	}

	@Test
	public void testCopyMutualReferencesRenamed() throws IOException {
		var subdir = "mutualReferencesUnchanged/";

		var modelMigrator = setupMigrator(
			subdir,
			of("PersonForReferences.ecore", "WorkPlaceForReferences.ecore"),
			of("Person1.xmi", "Person2.xmi", "WorkPlace1.xmi")
		);

		// refactoring of Ecore
		getEClass(evolvingModelManager, "personforreferences", "Person")
			.setName("PersonRenamed");

		copyModelsSaveAndAssertOutputs(
			modelMigrator,
			subdir,
			"mutualReferencesRenamed/",
			of("PersonForReferences.ecore", "WorkPlaceForReferences.ecore"),
			of("Person1.xmi", "Person2.xmi", "WorkPlace1.xmi")
		);
	}

	@Test
	public void testCopyMutualReferencesRenamed2() throws IOException {
		var subdir = "mutualReferencesUnchanged/";

		var modelMigrator = setupMigrator(
			subdir,
			of("PersonForReferences.ecore", "WorkPlaceForReferences.ecore"),
			of("Person1.xmi", "Person2.xmi", "WorkPlace1.xmi")
		);

		// refactoring of Ecore
		// rename the feature before...
		getFeature(evolvingModelManager, "personforreferences", "Person", "works")
			.setName("workplace");
		// ...renaming the class
		getEClass(evolvingModelManager, "personforreferences", "Person")
			.setName("PersonRenamed");
		getFeature(evolvingModelManager, "WorkPlaceForReferences", "WorkPlace", "persons")
			.setName("employees");

		copyModelsSaveAndAssertOutputs(
			modelMigrator,
			subdir,
			"mutualReferencesRenamed2/",
			of("PersonForReferences.ecore", "WorkPlaceForReferences.ecore"),
			of("Person1.xmi", "Person2.xmi", "WorkPlace1.xmi")
		);
	}

	@Test
	public void testRemovedContainmentFeature() throws IOException {
		var subdir = "simpleTestData/";

		var modelMigrator = setupMigrator(
			subdir,
			of("My.ecore"),
			of("MyRoot.xmi", "MyClass.xmi")
		);

		// refactoring of Ecore
		EcoreUtil.remove(getFeature(evolvingModelManager,
				"mypackage", "MyRoot", "myContents"));

		copyModelsSaveAndAssertOutputs(
			modelMigrator,
			subdir,
			"removedContainmentFeature/",
			of("My.ecore"),
			of("MyRoot.xmi", "MyClass.xmi")
		);
	}

	@Test
	public void testRemovedNonContainmentFeature() throws IOException {
		var subdir = "simpleTestData/";

		var modelMigrator = setupMigrator(
			subdir,
			of("My.ecore"),
			of("MyRoot.xmi", "MyClass.xmi")
		);

		// refactoring of Ecore
		EdeltaUtils.removeElement(getFeature(evolvingModelManager,
				"mypackage", "MyRoot", "myReferences"));

		copyModelsSaveAndAssertOutputs(
			modelMigrator,
			subdir,
			"removedNonContainmentFeature/",
			of("My.ecore"),
			of("MyRoot.xmi", "MyClass.xmi")
		);
	}

	@Test
	public void testRemovedNonReferredClass() throws IOException {
		var subdir = "simpleTestData/";

		var modelMigrator = setupMigrator(
			subdir,
			of("My.ecore"),
			of("MyRoot.xmi", "MyClass.xmi")
		);

		// refactoring of Ecore
		EdeltaUtils.removeElement(getEClass(evolvingModelManager,
				"mypackage", "MyRoot"));

		copyModelsSaveAndAssertOutputs(
			modelMigrator,
			subdir,
			"removedNonReferredClass/",
			of("My.ecore"),
			of("MyRoot.xmi", "MyClass.xmi")
		);
	}

	@Test
	public void testRemovedReferredClass() throws IOException {
		var subdir = "simpleTestData/";

		var modelMigrator = setupMigrator(
			subdir,
			of("My.ecore"),
			of("MyRoot.xmi", "MyClass.xmi")
		);

		// refactoring of Ecore
		EdeltaUtils.removeElement(getEClass(evolvingModelManager,
				"mypackage", "MyClass"));

		copyModelsSaveAndAssertOutputs(
			modelMigrator,
			subdir,
			"removedReferredClass/",
			of("My.ecore"),
			of("MyRoot.xmi", "MyClass.xmi")
		);
	}

	@Test
	public void testToUpperCaseStringAttributes() throws IOException {
		var subdir = "toUpperCaseStringAttributes/";

		var modelMigrator = setupMigrator(
			subdir,
			of("My.ecore"),
			of("MyClass.xmi", "MyClass2.xmi", "MyClass3.xmi")
		);

		modelMigrator.transformAttributeValueRule(
			a ->
				a.getEAttributeType() == EcorePackage.eINSTANCE.getEString(),
			oldValue ->
				oldValue.toString().toUpperCase()
		);

		copyModelsSaveAndAssertOutputs(
			modelMigrator,
			subdir,
			subdir,
			of("My.ecore"),
			of("MyClass.xmi", "MyClass2.xmi", "MyClass3.xmi")
		);
	}

	@Test
	public void testToUpperCaseSingleAttribute() throws IOException {
		var subdir = "toUpperCaseStringAttributes/";

		var modelMigrator = setupMigrator(
			subdir,
			of("My.ecore"),
			of("MyClass.xmi", "MyClass2.xmi", "MyClass3.xmi")
		);

		var attribute = getAttribute(evolvingModelManager,
				"mypackage", "MyClass", "myAttribute");
		modelMigrator.transformAttributeValueRule(
			a ->
				modelMigrator.isRelatedTo(a, attribute),
			oldValue ->
				oldValue.toString().toUpperCase()
		);

		copyModelsSaveAndAssertOutputs(
			modelMigrator,
			subdir,
			"toUpperCaseSingleAttribute/",
			of("My.ecore"),
			of("MyClass.xmi", "MyClass2.xmi", "MyClass3.xmi")
		);
	}

	@Test
	public void testToUpperCaseSingleAttributeAndRenamedBefore() throws IOException {
		var subdir = "toUpperCaseStringAttributes/";

		var modelMigrator = setupMigrator(
			subdir,
			of("My.ecore"),
			of("MyClass.xmi", "MyClass2.xmi", "MyClass3.xmi")
		);

		var attribute = getAttribute(evolvingModelManager,
				"mypackage", "MyClass", "myAttribute");
		attribute.setName("myAttributeRenamed");
		modelMigrator.transformAttributeValueRule(
			a ->
				modelMigrator.isRelatedTo(a, attribute),
			oldValue ->
				oldValue.toString().toUpperCase()
		);

		copyModelsSaveAndAssertOutputs(
			modelMigrator,
			subdir,
			"toUpperCaseSingleAttributeAndRenamedBefore/",
			of("My.ecore"),
			of("MyClass.xmi", "MyClass2.xmi", "MyClass3.xmi")
		);
	}

	@Test
	public void testToUpperCaseSingleAttributeAndRenamedAfter() throws IOException {
		var subdir = "toUpperCaseStringAttributes/";

		var modelMigrator = setupMigrator(
			subdir,
			of("My.ecore"),
			of("MyClass.xmi", "MyClass2.xmi", "MyClass3.xmi")
		);

		var attribute = getAttribute(evolvingModelManager,
				"mypackage", "MyClass", "myAttribute");
		modelMigrator.transformAttributeValueRule(
			a ->
				modelMigrator.isRelatedTo(a, attribute),
			oldValue ->
				oldValue.toString().toUpperCase()
		);
		attribute.setName("myAttributeRenamed");

		copyModelsSaveAndAssertOutputs(
			modelMigrator,
			subdir,
			"toUpperCaseSingleAttributeAndRenamedBefore/",
			of("My.ecore"),
			of("MyClass.xmi", "MyClass2.xmi", "MyClass3.xmi")
		);
	}

	@Test
	public void testToUpperCaseSingleAttributeAndMakeMultipleBefore() throws IOException {
		var subdir = "toUpperCaseStringAttributes/";

		var modelMigrator = setupMigrator(
			subdir,
			of("My.ecore"),
			of("MyClass.xmi", "MyClass2.xmi", "MyClass3.xmi")
		);

		var attribute = getAttribute(evolvingModelManager,
				"mypackage", "MyClass", "myAttribute");

		makeMultiple(modelMigrator, attribute);

		modelMigrator.transformAttributeValueRule(
			modelMigrator.isRelatedTo(attribute),
			modelMigrator.multiplicityAwareTranformer(attribute,
				o -> o.toString().toUpperCase())
		);

		copyModelsSaveAndAssertOutputs(
			modelMigrator,
			subdir,
			"toUpperCaseSingleAttributeAndMakeMultiple/",
			of("My.ecore"),
			of("MyClass.xmi", "MyClass2.xmi", "MyClass3.xmi")
		);
	}

	@Test
	public void testToUpperCaseSingleAttributeAndMakeMultipleAfter() throws IOException {
		var subdir = "toUpperCaseStringAttributes/";

		var modelMigrator = setupMigrator(
			subdir,
			of("My.ecore"),
			of("MyClass.xmi", "MyClass2.xmi", "MyClass3.xmi")
		);

		var attribute = getAttribute(evolvingModelManager,
				"mypackage", "MyClass", "myAttribute");

		modelMigrator.transformAttributeValueRule(
			a ->
				modelMigrator.isRelatedTo(a, attribute),
			oldValue ->
				oldValue.toString().toUpperCase()
		);

		makeMultiple(modelMigrator, attribute);

		copyModelsSaveAndAssertOutputs(
			modelMigrator,
			subdir,
			"toUpperCaseSingleAttributeAndMakeMultiple/",
			of("My.ecore"),
			of("MyClass.xmi", "MyClass2.xmi", "MyClass3.xmi")
		);
	}

	@Test
	public void testToUpperCaseSingleAttributeMultiple() throws IOException {
		var subdir = "toUpperCaseStringAttributesMultiple/";

		var modelMigrator = setupMigrator(
			subdir,
			of("My.ecore"),
			of("MyClass.xmi", "MyClass2.xmi", "MyClass3.xmi")
		);

		var attribute = getAttribute(evolvingModelManager,
				"mypackage", "MyClass", "myAttribute");

		modelMigrator.transformAttributeValueRule(
			modelMigrator.isRelatedTo(attribute),
			modelMigrator.multiplicityAwareTranformer(attribute,
				o -> o.toString().toUpperCase())
		);

		copyModelsSaveAndAssertOutputs(
			modelMigrator,
			subdir,
			subdir,
			of("My.ecore"),
			of("MyClass.xmi", "MyClass2.xmi", "MyClass3.xmi")
		);
	}

	@Test
	public void testToUpperCaseSingleAttributeMultipleAndMakeSingleBefore() throws IOException {
		var subdir = "toUpperCaseStringAttributesMultiple/";

		var modelMigrator = setupMigrator(
			subdir,
			of("My.ecore"),
			of("MyClass.xmi", "MyClass2.xmi", "MyClass3.xmi")
		);

		var attribute = getAttribute(evolvingModelManager,
				"mypackage", "MyClass", "myAttribute");

		makeSingle(modelMigrator, attribute);

		modelMigrator.transformAttributeValueRule(
			modelMigrator.isRelatedTo(attribute),
			modelMigrator.multiplicityAwareTranformer(attribute,
				o -> o.toString().toUpperCase())
		);

		copyModelsSaveAndAssertOutputs(
			modelMigrator,
			subdir,
			"toUpperCaseSingleAttributeMultipleAndMakeSingle/",
			of("My.ecore"),
			of("MyClass.xmi", "MyClass2.xmi", "MyClass3.xmi")
		);
	}

	@Test
	public void testToUpperCaseSingleAttributeMultipleAndMakeSingleAfter() throws IOException {
		var subdir = "toUpperCaseStringAttributesMultiple/";

		var modelMigrator = setupMigrator(
			subdir,
			of("My.ecore"),
			of("MyClass.xmi", "MyClass2.xmi", "MyClass3.xmi")
		);

		var attribute = getAttribute(evolvingModelManager,
				"mypackage", "MyClass", "myAttribute");

		modelMigrator.transformAttributeValueRule(
			modelMigrator.isRelatedTo(attribute),
			modelMigrator.multiplicityAwareTranformer(attribute,
				o -> o.toString().toUpperCase())
		);

		makeSingle(modelMigrator, attribute);

		copyModelsSaveAndAssertOutputs(
			modelMigrator,
			subdir,
			"toUpperCaseSingleAttributeMultipleAndMakeSingle/",
			of("My.ecore"),
			of("MyClass.xmi", "MyClass2.xmi", "MyClass3.xmi")
		);
	}

	@Test
	public void testMakeSingleAttribute() throws IOException {
		var subdir = "toUpperCaseStringAttributesMultiple/";

		var modelMigrator = setupMigrator(
			subdir,
			of("My.ecore"),
			of("MyClass.xmi", "MyClass2.xmi", "MyClass3.xmi")
		);

		var attribute = getAttribute(evolvingModelManager,
				"mypackage", "MyClass", "myAttribute");

		makeSingle(modelMigrator, attribute);

		copyModelsSaveAndAssertOutputs(
			modelMigrator,
			subdir,
			"makeSingle/",
			of("My.ecore"),
			of("MyClass.xmi", "MyClass2.xmi", "MyClass3.xmi")
		);
	}

	@Test
	public void testMakeMultipleAttribute() throws IOException {
		var subdir = "toUpperCaseStringAttributes/";

		var modelMigrator = setupMigrator(
			subdir,
			of("My.ecore"),
			of("MyClass.xmi", "MyClass2.xmi", "MyClass3.xmi")
		);

		var attribute = getAttribute(evolvingModelManager,
				"mypackage", "MyClass", "myAttribute");

		makeMultiple(modelMigrator, attribute);

		copyModelsSaveAndAssertOutputs(
			modelMigrator,
			subdir,
			"makeMultiple/",
			of("My.ecore"),
			of("MyClass.xmi", "MyClass2.xmi", "MyClass3.xmi")
		);
	}

	@Test
	public void testMakeMultipleTo2Attribute() throws IOException {
		var subdir = "toUpperCaseStringAttributesMultiple/";

		var modelMigrator = setupMigrator(
			subdir,
			of("My.ecore"),
			of("MyClass.xmi", "MyClass2.xmi", "MyClass3.xmi")
		);

		var attribute = getAttribute(evolvingModelManager,
				"mypackage", "MyClass", "myAttribute");

		makeMultiple(modelMigrator, attribute, 2);

		copyModelsSaveAndAssertOutputs(
			modelMigrator,
			subdir,
			"makeMultipleTo2/",
			of("My.ecore"),
			of("MyClass.xmi", "MyClass2.xmi", "MyClass3.xmi")
		);
	}

	@Test
	public void testMakeMultipleAndMakeSingleAttribute() throws IOException {
		var subdir = "toUpperCaseStringAttributes/";

		var modelMigrator = setupMigrator(
			subdir,
			of("My.ecore"),
			of("MyClass.xmi", "MyClass2.xmi", "MyClass3.xmi")
		);

		var attribute = getAttribute(evolvingModelManager,
				"mypackage", "MyClass", "myAttribute");

		makeMultiple(modelMigrator, attribute);

		makeSingle(modelMigrator, attribute);

		copyModelsSaveAndAssertOutputs(
			modelMigrator,
			subdir,
			"makeMultipleAndMakeSingle/",
			of("My.ecore"),
			of("MyClass.xmi", "MyClass2.xmi", "MyClass3.xmi")
		);
	}

	/**
	 * From the metamodel point of view we get the same Ecore,
	 * but of course from the model point of view, during the first migration,
	 * we lose some elements (the ones after the first one).
	 * 
	 * @throws IOException
	 */
	@Test
	public void testMakeSingleAndMakeMultipleAttribute() throws IOException {
		var subdir = "toUpperCaseStringAttributesMultiple/";

		var modelMigrator = setupMigrator(
			subdir,
			of("My.ecore"),
			of("MyClass.xmi", "MyClass2.xmi", "MyClass3.xmi")
		);

		var attribute = getAttribute(evolvingModelManager,
				"mypackage", "MyClass", "myAttribute");

		makeSingle(modelMigrator, attribute);

		makeMultiple(modelMigrator, attribute);

		copyModelsSaveAndAssertOutputs(
			modelMigrator,
			subdir,
			"makeSingleAndMakeMultiple/",
			of("My.ecore"),
			of("MyClass.xmi", "MyClass2.xmi", "MyClass3.xmi")
		);
	}

	@Test
	public void testMakeSingleNonContainmentReference() throws IOException {
		var subdir = "referencesMultiple/";
	
		var modelMigrator = setupMigrator(
			subdir,
			of("My.ecore"),
			of("MyRoot.xmi")
		);
	
		var reference = getReference(evolvingModelManager,
				"mypackage", "MyRoot", "myReferences");
	
		makeSingle(modelMigrator, reference);
	
		copyModelsSaveAndAssertOutputs(
			modelMigrator,
			subdir,
			"makeSingleNonContainmentReference/",
			of("My.ecore"),
			of("MyRoot.xmi")
		);
	}

	/**
	 * Since the list is turned into a single element and the second element used
	 * to be the only referred class, the non containment references will be empty
	 * in the migrated model.
	 * 
	 * @throws IOException
	 */
	@Test
	public void testMakeSingleContainmentReference() throws IOException {
		var subdir = "referencesMultiple/";
	
		var modelMigrator = setupMigrator(
			subdir,
			of("My.ecore"),
			of("MyRoot.xmi")
		);
	
		var reference = getReference(evolvingModelManager,
				"mypackage", "MyRoot", "myContents");
	
		makeSingle(modelMigrator, reference);
	
		copyModelsSaveAndAssertOutputs(
			modelMigrator,
			subdir,
			"makeSingleContainmentReference/",
			of("My.ecore"),
			of("MyRoot.xmi")
		);
	}

	@Test
	public void testMakeMultipleNonContainmentReference() throws IOException {
		var subdir = "referencesSingle/";
	
		var modelMigrator = setupMigrator(
			subdir,
			of("My.ecore"),
			of("MyRoot.xmi", "MyClass.xmi")
		);
	
		var reference = getReference(evolvingModelManager,
				"mypackage", "MyRoot", "myReferences");
	
		makeMultiple(modelMigrator, reference);
	
		copyModelsSaveAndAssertOutputs(
			modelMigrator,
			subdir,
			"makeMultipleNonContainmentReference/",
			of("My.ecore"),
			of("MyRoot.xmi", "MyClass.xmi")
		);
	}

	@Test
	public void testMakeMultipleContainmentReference() throws IOException {
		var subdir = "referencesSingle/";
	
		var modelMigrator = setupMigrator(
			subdir,
			of("My.ecore"),
			of("MyRoot.xmi", "MyClass.xmi")
		);
	
		var reference = getReference(evolvingModelManager,
				"mypackage", "MyRoot", "myContents");
	
		makeMultiple(modelMigrator, reference);
	
		copyModelsSaveAndAssertOutputs(
			modelMigrator,
			subdir,
			"makeMultipleContainmentReference/",
			of("My.ecore"),
			of("MyRoot.xmi", "MyClass.xmi")
		);
	}

	@Test
	public void testMakeMultipleAndMakeSingleNonContainmentReference() throws IOException {
		var subdir = "referencesSingle/";

		var modelMigrator = setupMigrator(
			subdir,
			of("My.ecore"),
			of("MyRoot.xmi", "MyClass.xmi")
		);

		var reference = getReference(evolvingModelManager,
				"mypackage", "MyRoot", "myReferences");

		makeMultiple(modelMigrator, reference);

		makeSingle(modelMigrator, reference);

		copyModelsSaveAndAssertOutputs(
			modelMigrator,
			subdir,
			"makeMultipleAndMakeSingleNonContainmentReference/",
			of("My.ecore"),
			of("MyRoot.xmi", "MyClass.xmi")
		);
	}

	/**
	 * From the metamodel point of view we get the same Ecore,
	 * but of course from the model point of view, during the first migration,
	 * we lose some elements (the ones after the first one).
	 * 
	 * @throws IOException
	 */
	@Test
	public void testMakeSingleAndMakeMultipleNonContainmentReference() throws IOException {
		var subdir = "referencesMultiple/";

		var modelMigrator = setupMigrator(
			subdir,
			of("My.ecore"),
			of("MyRoot.xmi")
		);

		var reference = getReference(evolvingModelManager,
				"mypackage", "MyRoot", "myReferences");

		makeSingle(modelMigrator, reference);

		makeMultiple(modelMigrator, reference);

		copyModelsSaveAndAssertOutputs(
			modelMigrator,
			subdir,
			"makeSingleAndMakeMultipleNonContainmentReference/",
			of("My.ecore"),
			of("MyRoot.xmi")
		);
	}

	@Test
	public void testMakeMultipleAndMakeSingleContainmentReference() throws IOException {
		var subdir = "referencesSingle/";

		var modelMigrator = setupMigrator(
			subdir,
			of("My.ecore"),
			of("MyRoot.xmi", "MyClass.xmi")
		);

		var reference = getReference(evolvingModelManager,
				"mypackage", "MyRoot", "myContents");

		makeMultiple(modelMigrator, reference);

		makeSingle(modelMigrator, reference);

		copyModelsSaveAndAssertOutputs(
			modelMigrator,
			subdir,
			"makeMultipleAndMakeSingleContainmentReference/",
			of("My.ecore"),
			of("MyRoot.xmi", "MyClass.xmi")
		);
	}

	/**
	 * From the metamodel point of view we get the same Ecore,
	 * but of course from the model point of view, during the first migration,
	 * we lose some elements (the ones after the first one).
	 * 
	 * Since the list is turned into a single element and the second element used
	 * to be the only referred class, the non containment references will be empty
	 * in the migrated model.
	 * 
	 * @throws IOException
	 */
	@Test
	public void testMakeSingleAndMakeMultipleContainmentReference() throws IOException {
		var subdir = "referencesMultiple/";

		var modelMigrator = setupMigrator(
			subdir,
			of("My.ecore"),
			of("MyRoot.xmi")
		);

		var reference = getReference(evolvingModelManager,
				"mypackage", "MyRoot", "myContents");

		makeSingle(modelMigrator, reference);

		makeMultiple(modelMigrator, reference);

		copyModelsSaveAndAssertOutputs(
			modelMigrator,
			subdir,
			"makeSingleAndMakeMultipleContainmentReference/",
			of("My.ecore"),
			of("MyRoot.xmi")
		);
	}

	@Test
	public void testChangedAttributeTypeWithoutProperMigration() {
		var subdir = "changedAttributeType/";

		var modelMigrator = setupMigrator(
			subdir,
			of("My.ecore"),
			of("MyClass.xmi")
		);

		// actual refactoring
		getAttribute(evolvingModelManager, "mypackage", "MyClass", "myAttribute")
			.setEType(EcorePackage.eINSTANCE.getEInt());

		Assertions.assertThatThrownBy(() -> // NOSONAR
		copyModelsSaveAndAssertOutputs(
			modelMigrator,
			subdir,
			"should not get here/",
			of("My.ecore"),
			of("MyClass.xmi")
		))
		.isInstanceOf(ClassCastException.class)
		.hasMessageContaining(
			"The value of type 'class java.lang.String' must be of type 'class java.lang.Integer'");
	}

	@Test
	public void testChangedAttributeTypeWithAttributeMigrator() throws IOException {
		var subdir = "changedAttributeType/";

		var modelMigrator = setupMigrator(
			subdir,
			of("My.ecore"),
			of("MyClass.xmi", "MyClass2.xmi", "MyClass3.xmi")
		);

		// actual refactoring
		var attributeName = "myAttribute";
		var attribute = getAttribute(evolvingModelManager, "mypackage", "MyClass", attributeName);
		attribute.setEType(EcorePackage.eINSTANCE.getEInt());

		// custom migration rule
		modelMigrator.transformAttributeValueRule(
			a ->
				modelMigrator.isRelatedTo(a, attribute),
			(feature, o, oldValue) -> {
				// if we come here the old attribute was set
				return EdeltaEcoreUtil.unwrapCollection(
					EdeltaEcoreUtil.wrapAsCollection(oldValue, -1)
						.stream()
						.map(val -> {
							try {
								return Integer.parseInt(val.toString());
							} catch (NumberFormatException e) {
								return -1;
							}
						})
						.collect(Collectors.toList()),
					feature);
			}
		);

		copyModelsSaveAndAssertOutputs(
			modelMigrator,
			subdir,
			subdir,
			of("My.ecore"),
			of("MyClass.xmi", "MyClass2.xmi", "MyClass3.xmi")
		);
	}

	@Test
	public void testChangeAttributeType() throws IOException {
		var subdir = "changedAttributeType/";

		var modelMigrator = setupMigrator(
			subdir,
			of("My.ecore"),
			of("MyClass.xmi", "MyClass2.xmi", "MyClass3.xmi")
		);

		// actual refactoring
		var attributeName = "myAttribute";
		var attribute = getAttribute(evolvingModelManager, "mypackage", "MyClass", attributeName);

		changeAttributeType(modelMigrator, attribute,
			EcorePackage.eINSTANCE.getEInt(),
			val -> {
				try {
					return Integer.parseInt(val.toString());
				} catch (NumberFormatException e) {
					return -1;
				}
			}
		);

		copyModelsSaveAndAssertOutputs(
			modelMigrator,
			subdir,
			subdir,
			of("My.ecore"),
			of("MyClass.xmi", "MyClass2.xmi", "MyClass3.xmi")
		);
	}

	@Test
	public void testChangeAttributeTypeAlternative() throws IOException {
		var subdir = "changedAttributeType/";

		var modelMigrator = setupMigrator(
			subdir,
			of("My.ecore"),
			of("MyClass.xmi", "MyClass2.xmi", "MyClass3.xmi")
		);

		// actual refactoring
		var attributeName = "myAttribute";
		var attribute = getAttribute(evolvingModelManager, "mypackage", "MyClass", attributeName);

		changeAttributeTypeAlternative(modelMigrator, attribute,
			EcorePackage.eINSTANCE.getEInt(),
			val -> {
				try {
					return Integer.parseInt(val.toString());
				} catch (NumberFormatException e) {
					return -1;
				}
			}
		);

		copyModelsSaveAndAssertOutputs(
			modelMigrator,
			subdir,
			subdir,
			of("My.ecore"),
			of("MyClass.xmi", "MyClass2.xmi", "MyClass3.xmi")
		);
	}

	/**
	 * Since the type (from String to int) is changed before changing it to
	 * multiple, string values that were null would become 0 int values, so they would
	 * become a singleton list with 0 (MyClass.xmi). Instead, changing the
	 * multiplicity before, will lead to an empty list for original null string
	 * values. These two behaviors must be the same, that's why we have
	 * the check eObject.eIsSet(feature) in {@link EdeltaModelMigrator#copyRule(Predicate, EdeltaModelMigrator.CopyProcedure)}.
	 * 
	 * @throws IOException
	 */
	@Test
	public void testChangeAttributeTypeAndMutiplicityAfter() throws IOException {
		var subdir = "changedAttributeType/";

		var modelMigrator = setupMigrator(
			subdir,
			of("My.ecore"),
			of("MyClass.xmi", "MyClass2.xmi", "MyClass3.xmi")
		);

		// actual refactoring
		var attributeName = "myAttribute";
		var attribute = getAttribute(evolvingModelManager, "mypackage", "MyClass", attributeName);

		changeAttributeType(modelMigrator, attribute,
			EcorePackage.eINSTANCE.getEInt(),
			val -> {
				try {
					return Integer.parseInt(val.toString());
				} catch (NumberFormatException e) {
					return -1;
				}
			}
		);

		makeMultiple(modelMigrator, attribute);

		copyModelsSaveAndAssertOutputs(
			modelMigrator,
			subdir,
			"changedAttributeTypeAndMultiplicity/",
			of("My.ecore"),
			of("MyClass.xmi", "MyClass2.xmi", "MyClass3.xmi")
		);
	}

	@Test
	public void testChangeAttributeTypeAndMutiplicityAfterAlternative() throws IOException {
		var subdir = "changedAttributeType/";

		var modelMigrator = setupMigrator(
			subdir,
			of("My.ecore"),
			of("MyClass.xmi", "MyClass2.xmi", "MyClass3.xmi")
		);

		// actual refactoring
		var attributeName = "myAttribute";
		var attribute = getAttribute(evolvingModelManager, "mypackage", "MyClass", attributeName);

		changeAttributeTypeAlternative(modelMigrator, attribute,
			EcorePackage.eINSTANCE.getEInt(),
			val -> {
				try {
					return Integer.parseInt(val.toString());
				} catch (NumberFormatException e) {
					return -1;
				}
			}
		);

		makeMultiple(modelMigrator, attribute);

		copyModelsSaveAndAssertOutputs(
			modelMigrator,
			subdir,
			"changedAttributeTypeAndMultiplicity/",
			of("My.ecore"),
			of("MyClass.xmi", "MyClass2.xmi", "MyClass3.xmi")
		);
	}

	@Test
	public void testChangeAttributeTypeAndMutiplicityBefore() throws IOException {
		var subdir = "changedAttributeType/";

		var modelMigrator = setupMigrator(
			subdir,
			of("My.ecore"),
			of("MyClass.xmi", "MyClass2.xmi", "MyClass3.xmi")
		);

		// actual refactoring
		var attributeName = "myAttribute";
		var attribute = getAttribute(evolvingModelManager, "mypackage", "MyClass", attributeName);

		makeMultiple(modelMigrator, attribute);

		changeAttributeType(modelMigrator, attribute,
			EcorePackage.eINSTANCE.getEInt(),
			val -> {
				try {
					return Integer.parseInt(val.toString());
				} catch (NumberFormatException e) {
					return -1;
				}
			}
		);

		copyModelsSaveAndAssertOutputs(
			modelMigrator,
			subdir,
			"changedAttributeTypeAndMultiplicity/",
			of("My.ecore"),
			of("MyClass.xmi", "MyClass2.xmi", "MyClass3.xmi")
		);
	}

	@Test
	public void testChangeAttributeTypeAndMutiplicityBeforeAlternative() throws IOException {
		var subdir = "changedAttributeType/";

		var modelMigrator = setupMigrator(
			subdir,
			of("My.ecore"),
			of("MyClass.xmi", "MyClass2.xmi", "MyClass3.xmi")
		);

		// actual refactoring
		var attributeName = "myAttribute";
		var attribute = getAttribute(evolvingModelManager, "mypackage", "MyClass", attributeName);

		makeMultiple(modelMigrator, attribute);

		changeAttributeTypeAlternative(modelMigrator, attribute,
			EcorePackage.eINSTANCE.getEInt(),
			val -> {
				try {
					return Integer.parseInt(val.toString());
				} catch (NumberFormatException e) {
					return -1;
				}
			}
		);

		copyModelsSaveAndAssertOutputs(
			modelMigrator,
			subdir,
			"changedAttributeTypeAndMultiplicity/",
			of("My.ecore"),
			of("MyClass.xmi", "MyClass2.xmi", "MyClass3.xmi")
		);
	}

	@Test
	public void testChangedAttributeTypeWithCopyRule() throws IOException {
		var subdir = "changedAttributeType/";

		var modelMigrator = setupMigrator(
			subdir,
			of("My.ecore"),
			of("MyClass.xmi")
		);

		// actual refactoring
		var attributeName = "myAttribute";
		var attribute = getAttribute(evolvingModelManager, "mypackage", "MyClass", attributeName);
		attribute.setEType(EcorePackage.eINSTANCE.getEInt());

		// custom migration rule
		modelMigrator.copyRule(
			a ->
				modelMigrator.isRelatedTo(a, attribute),
			(feature, oldObj, newObj) -> {
				// feature is the feature of the original ecore
				// oldObj is the original model's object,
				// newObj is the copy, so it's the new model's object
				// feature must be used to access th eold model's object's value
				// attribute is from the evolved ecore
				newObj.eSet(attribute,
					Integer.parseInt(
						oldObj.eGet(feature).toString()));
			}
		);

		copyModelsSaveAndAssertOutputs(
			modelMigrator,
			subdir,
			subdir,
			of("My.ecore"),
			of("MyClass.xmi")
		);
	}

	@Test
	public void testChangedMultiAttributeType() throws IOException {
		var subdir = "changedMultiAttributeType/";

		var modelMigrator = setupMigrator(
			subdir,
			of("My.ecore"),
			of("MyClass.xmi")
		);

		// actual refactoring
		var attributeName = "myAttribute";
		var attribute = getAttribute(evolvingModelManager, "mypackage", "MyClass", attributeName);

		changeAttributeType(modelMigrator, attribute,
			EcorePackage.eINSTANCE.getEInt(),
			val -> {
				try {
					return Integer.parseInt(val.toString());
				} catch (NumberFormatException e) {
					return -1;
				}
			}
		);

		copyModelsSaveAndAssertOutputs(
			modelMigrator,
			subdir,
			subdir,
			of("My.ecore"),
			of("MyClass.xmi")
		);
	}

	@Test
	public void testChangedMultiAttributeTypeAlternative() throws IOException {
		var subdir = "changedMultiAttributeType/";

		var modelMigrator = setupMigrator(
			subdir,
			of("My.ecore"),
			of("MyClass.xmi")
		);

		// actual refactoring
		var attributeName = "myAttribute";
		var attribute = getAttribute(evolvingModelManager, "mypackage", "MyClass", attributeName);

		changeAttributeTypeAlternative(modelMigrator, attribute,
			EcorePackage.eINSTANCE.getEInt(),
			val -> {
				try {
					return Integer.parseInt(val.toString());
				} catch (NumberFormatException e) {
					return -1;
				}
			}
		);

		copyModelsSaveAndAssertOutputs(
			modelMigrator,
			subdir,
			subdir,
			of("My.ecore"),
			of("MyClass.xmi")
		);
	}

	@Test
	public void testChangedMultiAttributeTypeAndMultiplicity() throws IOException {
		var subdir = "changedMultiAttributeType/";

		var modelMigrator = setupMigrator(
			subdir,
			of("My.ecore"),
			of("MyClass.xmi")
		);

		// actual refactoring
		var attributeName = "myAttribute";
		var attribute = getAttribute(evolvingModelManager, "mypackage", "MyClass", attributeName);

		changeAttributeType(modelMigrator, attribute,
			EcorePackage.eINSTANCE.getEInt(),
			val -> {
				try {
					return Integer.parseInt(val.toString());
				} catch (NumberFormatException e) {
					return -1;
				}
			}
		);

		makeSingle(modelMigrator, attribute);

		copyModelsSaveAndAssertOutputs(
			modelMigrator,
			subdir,
			"changedMultiAttributeTypeAndMultiplicity/",
			of("My.ecore"),
			of("MyClass.xmi")
		);
	}

	@Test
	public void testChangedMultiAttributeTypeAndMultiplicityAlternative() throws IOException {
		var subdir = "changedMultiAttributeType/";

		var modelMigrator = setupMigrator(
			subdir,
			of("My.ecore"),
			of("MyClass.xmi")
		);

		// actual refactoring
		var attributeName = "myAttribute";
		var attribute = getAttribute(evolvingModelManager, "mypackage", "MyClass", attributeName);

		changeAttributeTypeAlternative(modelMigrator, attribute,
			EcorePackage.eINSTANCE.getEInt(),
			val -> {
				try {
					return Integer.parseInt(val.toString());
				} catch (NumberFormatException e) {
					return -1;
				}
			}
		);

		makeSingle(modelMigrator, attribute);

		copyModelsSaveAndAssertOutputs(
			modelMigrator,
			subdir,
			"changedMultiAttributeTypeAndMultiplicity/",
			of("My.ecore"),
			of("MyClass.xmi")
		);
	}

	@Test
	public void testChangedMultiAttributeTypeAndMultiplicityTo2() throws IOException {
		var subdir = "changedMultiAttributeType/";

		var modelMigrator = setupMigrator(
			subdir,
			of("My.ecore"),
			of("MyClass.xmi")
		);

		// actual refactoring
		var attributeName = "myAttribute";
		var attribute = getAttribute(evolvingModelManager, "mypackage", "MyClass", attributeName);

		makeMultiple(modelMigrator, attribute, 2);

		changeAttributeType(modelMigrator, attribute,
			EcorePackage.eINSTANCE.getEInt(),
			val -> {
				try {
					return Integer.parseInt(val.toString());
				} catch (NumberFormatException e) {
					return -1;
				}
			}
		);

		copyModelsSaveAndAssertOutputs(
			modelMigrator,
			subdir,
			"changedMultiAttributeTypeAndMultiplicityTo2/",
			of("My.ecore"),
			of("MyClass.xmi")
		);
	}

	@Test
	public void testChangedMultiAttributeTypeAndMultiplicityTo2Alternative() throws IOException {
		var subdir = "changedMultiAttributeType/";

		var modelMigrator = setupMigrator(
			subdir,
			of("My.ecore"),
			of("MyClass.xmi")
		);

		// actual refactoring
		var attributeName = "myAttribute";
		var attribute = getAttribute(evolvingModelManager, "mypackage", "MyClass", attributeName);

		makeMultiple(modelMigrator, attribute, 2);

		changeAttributeTypeAlternative(modelMigrator, attribute,
			EcorePackage.eINSTANCE.getEInt(),
			val -> {
				try {
					return Integer.parseInt(val.toString());
				} catch (NumberFormatException e) {
					return -1;
				}
			}
		);

		copyModelsSaveAndAssertOutputs(
			modelMigrator,
			subdir,
			"changedMultiAttributeTypeAndMultiplicityTo2/",
			of("My.ecore"),
			of("MyClass.xmi")
		);
	}

	@Test
	public void testChangedAttributeNameAndType() throws IOException {
		var subdir = "changedAttributeType/";

		var modelMigrator = setupMigrator(
			subdir,
			of("My.ecore"),
			of("MyClass.xmi")
		);

		// actual refactoring
		var attributeName = "myAttribute";
		var attribute = getAttribute(evolvingModelManager, "mypackage", "MyClass", attributeName);
		attribute.setName("newName");
		attribute.setEType(EcorePackage.eINSTANCE.getEInt());

		// custom migration rule
		modelMigrator.transformAttributeValueRule(
			modelMigrator.isRelatedTo(attribute),
			(feature, oldObj, oldValue) -> {
				var eClass = oldObj.eClass();
				return Integer.parseInt(
					oldObj.eGet(eClass.getEStructuralFeature(attributeName)).toString());
			}
		);

		copyModelsSaveAndAssertOutputs(
			modelMigrator,
			subdir,
			"changedAttributeNameAndType/",
			of("My.ecore"),
			of("MyClass.xmi")
		);
	}

	@Test
	public void testChangedAttributeTypeAndName() throws IOException {
		var subdir = "changedAttributeType/";

		var modelMigrator = setupMigrator(
			subdir,
			of("My.ecore"),
			of("MyClass.xmi")
		);

		// actual refactoring
		var attributeName = "myAttribute";
		var attribute = getAttribute(evolvingModelManager, "mypackage", "MyClass", attributeName);
		attribute.setEType(EcorePackage.eINSTANCE.getEInt());

		// custom migration rule
		modelMigrator.transformAttributeValueRule(
				modelMigrator.isRelatedTo(attribute),
			(feature, o, oldValue) -> {
				// o is the old object,
				// so we must use the original feature to retrieve the value to copy
				// that is, don't use attribute, which is the one of the new package
				var eClass = o.eClass();
				return Integer.parseInt(
					o.eGet(eClass.getEStructuralFeature(attributeName)).toString());
			}
		);
		attribute.setName("newName");

		copyModelsSaveAndAssertOutputs(
			modelMigrator,
			subdir,
			"changedAttributeTypeAndName/",
			of("My.ecore"),
			of("MyClass.xmi")
		);
	}

	@Test
	public void testElementAssociations() {
		var subdir = "simpleTestData/";

		var modelMigrator = setupMigrator(
			subdir,
			of("My.ecore"),
			of("MyClass.xmi")
		);

		var origfeature1 = getAttribute(originalModelManager,
				"mypackage", "MyClass", "myClassStringAttribute");
		var origfeature2 = getFeature(originalModelManager,
				"mypackage", "MyRoot", "myReferences");

		var feature1 = getAttribute(evolvingModelManager,
				"mypackage", "MyClass", "myClassStringAttribute");
		var feature2 = getFeature(evolvingModelManager,
				"mypackage", "MyRoot", "myReferences");

		assertTrue(modelMigrator.isRelatedTo(origfeature1, feature1));
		assertTrue(modelMigrator.isRelatedTo(origfeature2, feature2));
		assertFalse(modelMigrator.isRelatedTo(origfeature2, feature1));
		assertFalse(modelMigrator.isRelatedTo(origfeature1, feature2));

		assertSame(origfeature1,
				modelMigrator.getOriginal(feature1));
		assertSame(origfeature2,
				modelMigrator.getOriginal(feature2));

		// remove a feature
		EdeltaUtils.removeElement(feature1);
		assertFalse(modelMigrator.isRelatedTo(origfeature1, feature1));
		assertTrue(modelMigrator.isRelatedTo(origfeature2, feature2));
		assertFalse(modelMigrator.isRelatedTo(origfeature2, feature1));
		assertFalse(modelMigrator.isRelatedTo(origfeature1, feature2));

		// getOriginal does not check whether the second argument is still there
		assertSame(origfeature1,
				modelMigrator.getOriginal(feature1));
		assertSame(origfeature2,
				modelMigrator.getOriginal(feature2));

		// wasRelatedTo does not check whether the second argument is still there
		assertTrue(modelMigrator.wasRelatedTo(origfeature1, feature1));
		assertTrue(modelMigrator.wasRelatedTo(origfeature2, feature2));
		assertFalse(modelMigrator.wasRelatedTo(origfeature2, feature1));
		assertFalse(modelMigrator.wasRelatedTo(origfeature1, feature2));

	}

	@Test
	public void testReplaceWithCopy() throws IOException {
		var subdir = "simpleTestData/";

		var modelMigrator = setupMigrator(
			subdir,
			of("My.ecore"),
			of("MyClass.xmi")
		);

		// actual refactoring
		var attribute = getAttribute(evolvingModelManager,
				"mypackage", "MyClass", "myClassStringAttribute");

		replaceWithCopy(modelMigrator, attribute, "myAttributeRenamed");

		copyModelsSaveAndAssertOutputs(
			modelMigrator,
			subdir,
			"replaceWithCopy/",
			of("My.ecore"),
			of("MyClass.xmi")
		);
	}

	@Test
	public void testReplaceWithCopyTwice() throws IOException {
		var subdir = "simpleTestData/";

		var modelMigrator = setupMigrator(
			subdir,
			of("My.ecore"),
			of("MyClass.xmi")
		);

		// actual refactoring
		var attribute = getAttribute(evolvingModelManager,
				"mypackage", "MyClass", "myClassStringAttribute");
		var copied = replaceWithCopy(modelMigrator, attribute, "myAttributeRenamed");
		replaceWithCopy(modelMigrator, copied, "myAttributeRenamedTwice");

		copyModelsSaveAndAssertOutputs(
			modelMigrator,
			subdir,
			"replaceWithCopyTwice/",
			of("My.ecore"),
			of("MyClass.xmi")
		);
	}

	/**
	 * Note that with pull up the migrated model is actually the same as the
	 * original one, but we have to adjust some mappings to make the copy work,
	 * because the value from the original object has to be put in an inherited
	 * attribute in the copied object.
	 * 
	 * @throws IOException
	 */
	@Test
	public void testPullUpFeatures() throws IOException {
		var subdir = "pullUpFeatures/";

		var modelMigrator = setupMigrator(
			subdir,
			of("PersonList.ecore"),
			of("List.xmi")
		);

		var personClass = getEClass(evolvingModelManager,
				"PersonList", "Person");
		var studentName = getFeature(evolvingModelManager,
				"PersonList", "Student", "name");
		var employeeName = getFeature(evolvingModelManager,
				"PersonList", "Employee", "name");
		// refactoring
		pullUp(modelMigrator,
				personClass,
				List.of(studentName, employeeName));

		copyModelsSaveAndAssertOutputs(
			modelMigrator,
			subdir,
			subdir,
			of("PersonList.ecore"),
			of("List.xmi")
		);
	}

	@Test
	public void testPullUpReferences() throws IOException {
		var subdir = "pullUpReferences/";

		var modelMigrator = setupMigrator(
			subdir,
			of("PersonList.ecore"),
			of("List.xmi")
		);

		var personClass = getEClass(evolvingModelManager,
				"PersonList", "Person");
		var studentAddress = getFeature(evolvingModelManager,
				"PersonList", "Student", "address");
		var employeeAddress = getFeature(evolvingModelManager,
				"PersonList", "Employee", "address");
		// refactoring
		pullUp(modelMigrator,
				personClass,
				List.of(studentAddress, employeeAddress));

		copyModelsSaveAndAssertOutputs(
			modelMigrator,
			subdir,
			subdir,
			of("PersonList.ecore"),
			of("List.xmi")
		);
	}

	@Test
	public void testPullUpContainmentReferences() throws IOException {
		var subdir = "pullUpContainmentReferences/";

		var modelMigrator = setupMigrator(
			subdir,
			of("PersonList.ecore"),
			of("List.xmi")
		);

		var personClass = getEClass(evolvingModelManager,
				"PersonList", "Person");
		var studentAddress = getFeature(evolvingModelManager,
				"PersonList", "Student", "address");
		var employeeAddress = getFeature(evolvingModelManager,
				"PersonList", "Employee", "address");
		// refactoring
		pullUp(modelMigrator,
				personClass,
				List.of(studentAddress, employeeAddress));

		copyModelsSaveAndAssertOutputs(
			modelMigrator,
			subdir,
			subdir,
			of("PersonList.ecore"),
			of("List.xmi")
		);
	}

	@Test
	public void testPushDownFeatures() throws IOException {
		var subdir = "pushDownFeatures/";

		var modelMigrator = setupMigrator(
			subdir,
			of("PersonList.ecore"),
			of("List.xmi")
		);

		var personClass = getEClass(evolvingModelManager,
				"PersonList", "Person");
		var personName = personClass.getEStructuralFeature("name");
		var studentClass = getEClass(evolvingModelManager,
				"PersonList", "Student");
		var employeeClass = getEClass(evolvingModelManager,
				"PersonList", "Employee");
		// refactoring
		pushDown(modelMigrator,
				personName,
				List.of(studentClass, employeeClass));

		copyModelsSaveAndAssertOutputs(
			modelMigrator,
			subdir,
			subdir,
			of("PersonList.ecore"),
			of("List.xmi")
		);
	}

	@Test
	public void testPullUpAndPushDown() throws IOException {
		var subdir = "pullUpFeatures/";

		var modelMigrator = setupMigrator(
			subdir,
			of("PersonList.ecore"),
			of("List.xmi")
		);

		var personClass = getEClass(evolvingModelManager,
				"PersonList", "Person");
		var studentClass = getEClass(evolvingModelManager,
				"PersonList", "Student");
		var employeeClass = getEClass(evolvingModelManager,
				"PersonList", "Employee");
		var studentName = studentClass.getEStructuralFeature("name");
		var employeeName = employeeClass.getEStructuralFeature("name");
		// refactoring
		var personName = pullUp(modelMigrator,
				personClass,
				List.of(studentName, employeeName));
		pushDown(modelMigrator,
				personName,
				List.of(studentClass, employeeClass));

		copyModelsSaveAndAssertOutputs(
			modelMigrator,
			subdir,
			"pullUpAndPushDown/",
			of("PersonList.ecore"),
			of("List.xmi")
		);
	}

	@Test
	public void testPushDownAndPullUp() throws IOException {
		var subdir = "pushDownFeatures/";

		var modelMigrator = setupMigrator(
			subdir,
			of("PersonList.ecore"),
			of("List.xmi")
		);

		var personClass = getEClass(evolvingModelManager,
				"PersonList", "Person");
		var personName = personClass.getEStructuralFeature("name");
		var studentClass = getEClass(evolvingModelManager,
				"PersonList", "Student");
		var employeeClass = getEClass(evolvingModelManager,
				"PersonList", "Employee");
		// refactoring
		var features = pushDown(modelMigrator,
				personName,
				List.of(studentClass, employeeClass));
		pullUp(modelMigrator,
				personClass,
				features);

		copyModelsSaveAndAssertOutputs(
			modelMigrator,
			subdir,
			"pushDownAndPullUp/",
			of("PersonList.ecore"),
			of("List.xmi")
		);
	}

	@Test
	public void testMakeBidirectional() throws IOException {
		var subdir = "makeBidirectional/";

		var modelMigrator = setupMigrator(
			subdir,
			of("PersonList.ecore"),
			of("List.xmi")
		);

		var personWorks = getReference(evolvingModelManager,
				"PersonList", "Person", "works");
		var workPlace = getEClass(evolvingModelManager,
				"PersonList", "WorkPlace");
		// refactoring
		var workPlacePerson = EdeltaUtils.newEReference("person", null);
		workPlace.getEStructuralFeatures().add(workPlacePerson);
		EdeltaUtils.makeBidirectional(personWorks, workPlacePerson);

		copyModelsSaveAndAssertOutputs(
			modelMigrator,
			subdir,
			subdir,
			of("PersonList.ecore"),
			of("List.xmi")
		);
	}

	@Test
	public void testMakeBidirectionalExisting() throws IOException {
		var subdir = "makeBidirectionalExisting/";
	
		var modelMigrator = setupMigrator(
			subdir,
			of("PersonList.ecore"),
			of("List.xmi")
		);
	
		var personWorks = getReference(evolvingModelManager,
				"PersonList", "Person", "works");
		// refactoring
		var workPlacePerson = getReference(evolvingModelManager,
				"PersonList", "WorkPlace", "person");
		assertNotNull(workPlacePerson);
		// this should not change anything
		EdeltaUtils.makeBidirectional(personWorks, workPlacePerson);
	
		copyModelsSaveAndAssertOutputs(
			modelMigrator,
			subdir,
			subdir,
			of("PersonList.ecore"),
			of("List.xmi")
		);
	}

	/**
	 * Makes sure that when copying a model, getting the migrated version of an
	 * object will use the current copier (and don't copy the same object twice).
	 * 
	 * IMPORTANT: this strongly relies on the contents of the test model MyRoot.xmi,
	 * which contains two MyClass objects.
	 * 
	 * @throws IOException
	 */
	@Test
	public void testGetMigratedInTransformAttributeValueRule() throws IOException {
		var subdir = "getMigratedTestData/";

		var modelMigrator = setupMigrator(
			subdir,
			of("My.ecore"),
			of("MyRoot.xmi")
		);

		var myAttribute = getAttribute(evolvingModelManager,
				"mypackage", "MyClass", "myAttribute");
		var myOtherAttribute = getAttribute(evolvingModelManager,
				"mypackage", "MyClass", "myOtherAttribute");

		var myAttributeOriginal = getAttribute(originalModelManager,
				"mypackage", "MyClass", "myAttribute");
		var root = originalModelManager
				.getModelResourceMap().values().iterator().next().getContents().get(0);
		assertEquals("MyRoot", root.eClass().getName());
		var myClassO1 = root.eContents().get(0);
		var myClassO2 = root.eContents().get(1);
		assertNotNull(myClassO1);
		assertNotNull(myClassO2);
		modelMigrator.transformAttributeValueRule(
			a ->
				a.getEAttributeType() == EcorePackage.eINSTANCE.getEString(),
			oldValue -> {
				// we make sure we also turn the other object value uppercase
				// this way we know that this copier is being used.
				var value = myClassO1.eGet(myAttributeOriginal);
				if (value.equals(oldValue)) {
					// get the migrated version of the other object
					var migrated = modelMigrator.getMigrated(myClassO2);
					// its attribute must be changed to upper case as well
					var migratedValue = migrated.eGet(myAttribute);
					// since that means that we are using the cupper copier
					// to get or to create the migrated version of objects
					assertThat(migratedValue.toString())
						.isUpperCase();
					// the same for the other attribute
					migratedValue = migrated.eGet(myOtherAttribute);
					// since that means that we are using the cupper copier
					// to get or to create the migrated version of objects
					assertThat(migratedValue.toString())
						.isUpperCase();
				}
				
				return oldValue.toString().toUpperCase();
			}
		);

		copyModelsSaveAndAssertOutputs(
			modelMigrator,
			subdir,
			subdir,
			of("My.ecore"),
			of("MyRoot.xmi")
		);
	}

	/**
	 * Makes sure that when copying a model, getting the migrated version of an
	 * object will use the current copier (and don't copy the same object twice).
	 * 
	 * IMPORTANT: this strongly relies on the contents of the test model MyRoot.xmi,
	 * which contains two MyClass objects.
	 * 
	 * @throws IOException
	 */
	@Test
	public void testGetMigratedInTransformAttributeValueRule2() throws IOException {
		var subdir = "getMigratedTestData/";

		var modelMigrator = setupMigrator(
			subdir,
			of("My.ecore"),
			of("MyRoot.xmi")
		);

		var myAttribute = getAttribute(evolvingModelManager,
				"mypackage", "MyClass", "myAttribute");
		var myOtherAttribute = getAttribute(evolvingModelManager,
				"mypackage", "MyClass", "myOtherAttribute");

		var root = originalModelManager
				.getModelResourceMap().values().iterator().next().getContents().get(0);
		assertEquals("MyRoot", root.eClass().getName());
		var myClassO1 = root.eContents().get(0);
		var myClassO2 = root.eContents().get(1);
		assertNotNull(myClassO1);
		assertNotNull(myClassO2);
		modelMigrator.transformAttributeValueRule(
			a ->
				a.getEAttributeType() == EcorePackage.eINSTANCE.getEString(),
			(feature, oldObj, oldValue) -> {
				// we make sure we also turn the other object value uppercase
				// this way we know that this copier is being used.
				var value = myClassO1.eGet(feature);
				if (value.equals(oldValue)) {
					// get the migrated version of the other object
					var migrated = modelMigrator.getMigrated(myClassO2);
					// its attribute must be changed to upper case as well
					var migratedValue = migrated.eGet(myAttribute);
					// since that means that we are using the cupper copier
					// to get or to create the migrated version of objects
					assertThat(migratedValue.toString())
						.isUpperCase();
					// the same for the other attribute
					migratedValue = migrated.eGet(myOtherAttribute);
					// since that means that we are using the cupper copier
					// to get or to create the migrated version of objects
					assertThat(migratedValue.toString())
						.isUpperCase();
				}
				
				return oldValue.toString().toUpperCase();
			}
		);

		copyModelsSaveAndAssertOutputs(
			modelMigrator,
			subdir,
			subdir,
			of("My.ecore"),
			of("MyRoot.xmi")
		);
	}

	/**
	 * Makes sure that when copying a model, getting the migrated version of an
	 * object will use the current copier (and don't copy the same object twice).
	 * 
	 * IMPORTANT: this strongly relies on the contents of the test model MyRoot.xmi,
	 * which contains two MyClass objects.
	 * 
	 * @throws IOException
	 */
	@Test
	public void testGetMigratedInCopyRule() throws IOException {
		var subdir = "getMigratedTestData/";

		var modelMigrator = setupMigrator(
			subdir,
			of("My.ecore"),
			of("MyRoot.xmi")
		);

		var myAttribute = getAttribute(evolvingModelManager,
				"mypackage", "MyClass", "myAttribute");
		var myOtherAttribute = getAttribute(evolvingModelManager,
				"mypackage", "MyClass", "myOtherAttribute");

		var myAttributeOriginal = getAttribute(originalModelManager,
				"mypackage", "MyClass", "myAttribute");
		var myOtherAttributeOriginal = getAttribute(originalModelManager,
				"mypackage", "MyClass", "myOtherAttribute");
		var root = originalModelManager
				.getModelResourceMap().values().iterator().next().getContents().get(0);
		assertEquals("MyRoot", root.eClass().getName());
		var myClassO1 = root.eContents().get(0);
		var myClassO2 = root.eContents().get(1);
		assertNotNull(myClassO1);
		assertNotNull(myClassO2);
		modelMigrator.copyRule(
			a ->
				a.getEType() == EcorePackage.eINSTANCE.getEString(),
			(feature, oldObj, newObj) -> {
				// we make sure we also turn the other object value uppercase
				// this way we know that this copier is being used.
				var value = myClassO1.eGet(feature);
				var oldValue = oldObj.eGet(feature);
				if (value.equals(oldValue)) {
					// get the migrated version of the other object
					var migrated = modelMigrator.getMigrated(myClassO2);
					// its attribute must be changed to upper case as well
					var migratedValue = migrated.eGet(myAttribute);
					// since that means that we are using the cupper copier
					// to get or to create the migrated version of objects
					assertThat(migratedValue.toString())
						.isUpperCase();
					// the same for the other attribute
					migratedValue = migrated.eGet(myOtherAttribute);
					// since that means that we are using the cupper copier
					// to get or to create the migrated version of objects
					assertThat(migratedValue.toString())
						.isUpperCase();
				}
				newObj.eSet(myAttribute, oldObj.eGet(myAttributeOriginal).toString().toUpperCase());
				newObj.eSet(myOtherAttribute, oldObj.eGet(myOtherAttributeOriginal).toString().toUpperCase());
			}
		);

		copyModelsSaveAndAssertOutputs(
			modelMigrator,
			subdir,
			subdir,
			of("My.ecore"),
			of("MyRoot.xmi")
		);
	}

	/**
	 * Makes sure that when copying a model, getting the migrated version of an
	 * object will use the current copier (and don't copy the same object twice).
	 * 
	 * IMPORTANT: this strongly relies on the contents of the test model MyRoot.xmi,
	 * which contains two MyClass objects.
	 * 
	 * @throws IOException
	 */
	@Test
	public void testGetMigratedInFeatureMigratorRule() throws IOException {
		var subdir = "simpleTestData/";

		var modelMigrator = setupMigrator(
			subdir,
			of("My.ecore"),
			of("MyClass.xmi")
		);

		// actual refactoring
		var attribute = getAttribute(evolvingModelManager,
				"mypackage", "MyClass", "myClassStringAttribute");

//		replaceWithCopy(modelMigrator, attribute, "myAttributeRenamed");
		var copy = createCopy(modelMigrator, attribute);
		copy.setName("myAttributeRenamed");
		var containingClass = attribute.getEContainingClass();
		EdeltaUtils.removeElement(attribute);
		containingClass.getEStructuralFeatures().add(copy);
		modelMigrator.featureMigratorRule(
			f -> // the feature must be originally associated with the
				// attribute we've just removed
				modelMigrator.wasRelatedTo(f, attribute),
			(feature, oldObj, newObj) -> {
				var migrated = modelMigrator.getMigrated(oldObj);
				// if we don't use the same copier the objects will be different
				assertSame(newObj, migrated);
				return copy;
			});

		copyModelsSaveAndAssertOutputs(
			modelMigrator,
			subdir,
			"replaceWithCopy/",
			of("My.ecore"),
			of("MyClass.xmi")
		);
	}

	/**
	 * Makes sure that when copying a model, getting the migrated version of an
	 * object will use the current copier (and don't copy the same object twice).
	 * 
	 * IMPORTANT: this strongly relies on the contents of the test model MyRoot.xmi,
	 * which contains two MyClass objects.
	 * 
	 * @throws IOException
	 */
	@Test
	public void testGetMigratedMultipleInFeatureMigratorRule() throws IOException {
		var subdir = "simpleTestData/";

		var modelMigrator = setupMigrator(
			subdir,
			of("My.ecore"),
			of("MyRoot.xmi")
		);

		// actual refactoring
		var attribute = getAttribute(evolvingModelManager,
				"mypackage", "MyClass", "myClassStringAttribute");
		attribute.setName("myAttributeRenamed");
		// the above renaming is not relevant for this test, it's used only
		// to have the final output just as the same as the previous test
		// thus avoiding having to create another expectations folder

		var reference = getReference(evolvingModelManager,
				"mypackage", "MyRoot", "myReferences");

		var copy = createCopy(modelMigrator, reference);
		var containingClass = reference.getEContainingClass();
		EdeltaUtils.removeElement(reference);
		// put it in first position to have the same order as the original one
		containingClass.getEStructuralFeatures().add(0, copy);
		modelMigrator.featureMigratorRule(
			f -> // the feature must be originally associated with the
				// attribute we've just removed
				modelMigrator.wasRelatedTo(f, reference),
			(feature, oldObj, newObj) -> {
				// make sure the copy is correctly propagated when copying a collection
				var oldValue = (Collection<?>) oldObj.eGet(feature);
				var migrated = modelMigrator.getMigrated(oldValue);
				assertSame(
					modelMigrator.getMigrated(oldValue).iterator().next(),
					migrated.iterator().next()
				);
				return copy;
			});

		copyModelsSaveAndAssertOutputs(
			modelMigrator,
			subdir,
			"replaceWithCopy/",
			of("My.ecore"),
			of("MyRoot.xmi")
		);
	}

	@Test
	public void testReferenceToClassUnidirectional() throws IOException {
		var subdir = "referenceToClassUnidirectional/";

		var modelMigrator = setupMigrator(
			subdir,
			of("PersonList.ecore"),
			of("List.xmi")
		);

		var personWorks = getReference(evolvingModelManager,
				"PersonList", "Person", "works");
		// refactoring
		referenceToClass(modelMigrator, personWorks, "WorkingPosition");

		copyModelsSaveAndAssertOutputs(
			modelMigrator,
			subdir,
			subdir,
			of("PersonList.ecore"),
			of("List.xmi")
		);
	}

	@Test
	public void testReferenceToClassMultipleUnidirectional() throws IOException {
		var subdir = "referenceToClassMultipleUnidirectional/";

		var modelMigrator = setupMigrator(
			subdir,
			of("PersonList.ecore"),
			of("List.xmi")
		);

		var personWorks = getReference(evolvingModelManager,
				"PersonList", "Person", "works");
		// refactoring
		referenceToClass(modelMigrator, personWorks, "WorkingPosition");

		copyModelsSaveAndAssertOutputs(
			modelMigrator,
			subdir,
			subdir,
			of("PersonList.ecore"),
			of("List.xmi")
		);
	}

	@Test
	public void testReferenceToClassBidirectional() throws IOException {
		var subdir = "referenceToClassBidirectional/";

		var modelMigrator = setupMigrator(
			subdir,
			of("PersonList.ecore"),
			of("List.xmi")
		);

		var personWorks = getReference(evolvingModelManager,
				"PersonList", "Person", "works");
		// refactoring
		referenceToClass(modelMigrator, personWorks, "WorkingPosition");

		copyModelsSaveAndAssertOutputs(
			modelMigrator,
			subdir,
			subdir,
			of("PersonList.ecore"),
			of("List.xmi")
		);
	}

	@Test
	public void testReferenceToClassBidirectionalAlternative() throws IOException {
		var subdir = "referenceToClassBidirectional/";

		var modelMigrator = setupMigrator(
			subdir,
			of("PersonList.ecore"),
			of("List.xmi")
		);

		var personWorks = getReference(evolvingModelManager,
				"PersonList", "Person", "works");
		// refactoring
		referenceToClassAlternative(modelMigrator, personWorks, "WorkingPosition");

		copyModelsSaveAndAssertOutputs(
			modelMigrator,
			subdir,
			subdir,
			of("PersonList.ecore"),
			of("List.xmi")
		);
	}

	@Test
	public void testReferenceToClassBidirectionalDifferentOrder() throws IOException {
		var subdir = "referenceToClassBidirectionalDifferentOrder/";

		var modelMigrator = setupMigrator(
			subdir,
			of("PersonList.ecore"),
			of("List.xmi")
		);

		var personWorks = getReference(evolvingModelManager,
				"PersonList", "Person", "works");
		// refactoring
		referenceToClass(modelMigrator, personWorks, "WorkingPosition");

		copyModelsSaveAndAssertOutputs(
			modelMigrator,
			subdir,
			subdir,
			of("PersonList.ecore"),
			of("List.xmi")
		);
	}

	@Test
	public void testReferenceToClassBidirectionalOppositeMultiple() throws IOException {
		var subdir = "referenceToClassBidirectionalOppositeMultiple/";

		var modelMigrator = setupMigrator(
			subdir,
			of("PersonList.ecore"),
			of("List.xmi")
		);

		var personWorks = getReference(evolvingModelManager,
				"PersonList", "Person", "works");
		// refactoring
		referenceToClass(modelMigrator, personWorks, "WorkingPosition");

		copyModelsSaveAndAssertOutputs(
			modelMigrator,
			subdir,
			subdir,
			of("PersonList.ecore"),
			of("List.xmi")
		);
	}

	@Test
	public void testReferenceToClassMultipleBidirectional() throws IOException {
		var subdir = "referenceToClassMultipleBidirectional/";

		var modelMigrator = setupMigrator(
			subdir,
			of("PersonList.ecore"),
			of("List.xmi")
		);

		var personWorks = getReference(evolvingModelManager,
				"PersonList", "Person", "works");
		// refactoring
		referenceToClass(modelMigrator, personWorks, "WorkingPosition");

		copyModelsSaveAndAssertOutputs(
			modelMigrator,
			subdir,
			subdir,
			of("PersonList.ecore"),
			of("List.xmi")
		);
	}

	@Test
	public void testReferenceToClassMultipleBidirectionalAlternative() throws IOException {
		var subdir = "referenceToClassMultipleBidirectional/";

		var modelMigrator = setupMigrator(
			subdir,
			of("PersonList.ecore"),
			of("List.xmi")
		);

		var personWorks = getReference(evolvingModelManager,
				"PersonList", "Person", "works");
		// refactoring
		referenceToClassAlternative(modelMigrator, personWorks, "WorkingPosition");

		copyModelsSaveAndAssertOutputs(
			modelMigrator,
			subdir,
			subdir,
			of("PersonList.ecore"),
			of("List.xmi")
		);
	}

	/**
	 * Changing from multi to single only the main reference after performing
	 * referenceToClass does not make much sense, since in the evolved model we lose
	 * some associations. This is just to make sure that nothing else bad happens
	 * 
	 * @throws IOException
	 */
	@Test
	public void testReferenceToClassMultipleBidirectionalChangedIntoSingleMain() throws IOException {
		var subdir = "referenceToClassMultipleBidirectional/";

		var modelMigrator = setupMigrator(
			subdir,
			of("PersonList.ecore"),
			of("List.xmi")
		);

		var personWorks = getReference(evolvingModelManager,
				"PersonList", "Person", "works");
		// refactoring
		referenceToClass(modelMigrator, personWorks, "WorkingPosition");
		makeSingle(modelMigrator, personWorks);

		copyModelsSaveAndAssertOutputs(
			modelMigrator,
			subdir,
			"referenceToClassMultipleBidirectionalChangedIntoSingleMain/",
			of("PersonList.ecore"),
			of("List.xmi")
		);
	}

	/**
	 * Changing from multi to single only the opposite reference after performing
	 * referenceToClass does not make much sense, since in the evolved model we lose
	 * some associations. This is just to make sure that nothing else bad happens
	 * 
	 * @throws IOException
	 */
	@Test
	public void testReferenceToClassMultipleBidirectionalChangedIntoSingleOpposite() throws IOException {
		var subdir = "referenceToClassMultipleBidirectional/";

		var modelMigrator = setupMigrator(
			subdir,
			of("PersonList.ecore"),
			of("List.xmi")
		);

		var personWorks = getReference(evolvingModelManager,
				"PersonList", "Person", "works");
		// refactoring
		var extractedClass = referenceToClass(modelMigrator, personWorks, "WorkingPosition");
		// in the evolved model, the original personWorks.getEOpposite
		// now is extractedClass.getEStructuralFeature(0).getEOpposite
		makeSingle(modelMigrator,
			((EReference) extractedClass.getEStructuralFeature(0))
				.getEOpposite());
		// changing the opposite multi to single of course makes the model
		// lose associations (the last Person that refers to a WorkingPosition wins)

		copyModelsSaveAndAssertOutputs(
			modelMigrator,
			subdir,
			"referenceToClassMultipleBidirectionalChangedIntoSingleOpposite/",
			of("PersonList.ecore"),
			of("List.xmi")
		);
	}

	/**
	 * Changing from multi to single only the opposite reference after performing
	 * referenceToClass does not make much sense, since in the evolved model we lose
	 * some associations. This is just to make sure that nothing else bad happens
	 * 
	 * @throws IOException
	 */
	@Test
	public void testReferenceToClassMultipleBidirectionalChangedIntoSingleOppositeAlternative() throws IOException {
		var subdir = "referenceToClassMultipleBidirectional/";

		var modelMigrator = setupMigrator(
			subdir,
			of("PersonList.ecore"),
			of("List.xmi")
		);

		var personWorks = getReference(evolvingModelManager,
				"PersonList", "Person", "works");
		// refactoring
		var extractedClass = referenceToClassAlternative(modelMigrator, personWorks, "WorkingPosition");
		// in the evolved model, the original personWorks.getEOpposite
		// now is extractedClass.getEStructuralFeature(0).getEOpposite
		makeSingle(modelMigrator,
			((EReference) extractedClass.getEStructuralFeature(0))
				.getEOpposite());
		// changing the opposite multi to single of course makes the model
		// lose associations (the last Person that refers to a WorkingPosition wins)

		copyModelsSaveAndAssertOutputs(
			modelMigrator,
			subdir,
			"referenceToClassMultipleBidirectionalChangedIntoSingleOpposite/",
			of("PersonList.ecore"),
			of("List.xmi")
		);
	}

	/**
	 * Changing from multi to single the two bidirectional references after performing
	 * referenceToClass does not make much sense, since in the evolved model we lose
	 * some associations. This is just to make sure that nothing else bad happens
	 * 
	 * @throws IOException
	 */
	@Test
	public void testReferenceToClassMultipleBidirectionalChangedIntoSingleBoth() throws IOException {
		var subdir = "referenceToClassMultipleBidirectional/";

		var modelMigrator = setupMigrator(
			subdir,
			of("PersonList.ecore"),
			of("List.xmi")
		);

		var personWorks = getReference(evolvingModelManager,
				"PersonList", "Person", "works");
		// refactoring
		var extractedClass = referenceToClass(modelMigrator, personWorks, "WorkingPosition");
		makeSingle(modelMigrator, personWorks);
		// in the evolved model, the original personWorks.getEOpposite
		// now is extractedClass.getEStructuralFeature(0).getEOpposite
		makeSingle(modelMigrator,
			((EReference) extractedClass.getEStructuralFeature(0))
				.getEOpposite());

		copyModelsSaveAndAssertOutputs(
			modelMigrator,
			subdir,
			"referenceToClassMultipleBidirectionalChangedIntoSingleBoth/",
			of("PersonList.ecore"),
			of("List.xmi")
		);
	}

	/**
	 * Changing from multi to single the two bidirectional references after performing
	 * referenceToClass does not make much sense, since in the evolved model we lose
	 * some associations. This is just to make sure that nothing else bad happens
	 * 
	 * @throws IOException
	 */
	@Test
	public void testReferenceToClassMultipleBidirectionalChangedIntoSingleBothAlternative() throws IOException {
		var subdir = "referenceToClassMultipleBidirectional/";

		var modelMigrator = setupMigrator(
			subdir,
			of("PersonList.ecore"),
			of("List.xmi")
		);

		var personWorks = getReference(evolvingModelManager,
				"PersonList", "Person", "works");
		// refactoring
		var extractedClass = referenceToClassAlternative(modelMigrator, personWorks, "WorkingPosition");
		makeSingle(modelMigrator, personWorks);
		// in the evolved model, the original personWorks.getEOpposite
		// now is extractedClass.getEStructuralFeature(0).getEOpposite
		makeSingle(modelMigrator,
			((EReference) extractedClass.getEStructuralFeature(0))
				.getEOpposite());

		copyModelsSaveAndAssertOutputs(
			modelMigrator,
			subdir,
			"referenceToClassMultipleBidirectionalChangedIntoSingleBoth/",
			of("PersonList.ecore"),
			of("List.xmi")
		);
	}

	@Test
	public void testClassToReferenceUnidirectional() throws IOException {
		var subdir = "classToReferenceUnidirectional/";

		var modelMigrator = setupMigrator(
			subdir,
			of("PersonList.ecore"),
			of("List.xmi")
		);

		var personWorks = getReference(evolvingModelManager,
				"PersonList", "Person", "works");
		// refactoring
		classToReference(modelMigrator, personWorks);

		copyModelsSaveAndAssertOutputs(
			modelMigrator,
			subdir,
			subdir,
			of("PersonList.ecore"),
			of("List.xmi")
		);
	}

	@Test
	public void testClassToReferenceMultipleUnidirectional() throws IOException {
		var subdir = "classToReferenceMultipleUnidirectional/";

		var modelMigrator = setupMigrator(
			subdir,
			of("PersonList.ecore"),
			of("List.xmi")
		);

		var personWorks = getReference(evolvingModelManager,
				"PersonList", "Person", "works");
		// refactoring
		classToReference(modelMigrator, personWorks);

		copyModelsSaveAndAssertOutputs(
			modelMigrator,
			subdir,
			subdir,
			of("PersonList.ecore"),
			of("List.xmi")
		);
	}

	@Test
	public void testClassToReferenceBidirectional() throws IOException {
		var subdir = "classToReferenceBidirectional/";

		var modelMigrator = setupMigrator(
			subdir,
			of("PersonList.ecore"),
			of("List.xmi")
		);

		var personWorks = getReference(evolvingModelManager,
				"PersonList", "Person", "works");
		// refactoring
		classToReference(modelMigrator, personWorks);

		copyModelsSaveAndAssertOutputs(
			modelMigrator,
			subdir,
			subdir,
			of("PersonList.ecore"),
			of("List.xmi")
		);
	}

	@Test
	public void testClassToReferenceBidirectionalDifferentOrder() throws IOException {
		var subdir = "classToReferenceBidirectionalDifferentOrder/";

		var modelMigrator = setupMigrator(
			subdir,
			of("PersonList.ecore"),
			of("List.xmi")
		);

		var personWorks = getReference(evolvingModelManager,
				"PersonList", "Person", "works");
		// refactoring
		classToReference(modelMigrator, personWorks);

		copyModelsSaveAndAssertOutputs(
			modelMigrator,
			subdir,
			subdir,
			of("PersonList.ecore"),
			of("List.xmi")
		);
	}

	@Test
	public void testClassToReferenceMultipleBidirectional() throws IOException {
		var subdir = "classToReferenceMultipleBidirectional/";

		var modelMigrator = setupMigrator(
			subdir,
			of("PersonList.ecore"),
			of("List.xmi")
		);

		var personWorks = getReference(evolvingModelManager,
				"PersonList", "Person", "works");
		// refactoring
		classToReference(modelMigrator, personWorks);

		copyModelsSaveAndAssertOutputs(
			modelMigrator,
			subdir,
			subdir,
			of("PersonList.ecore"),
			of("List.xmi")
		);
	}

	/**
	 * The inversion works both for the metamodel and for the model.
	 * 
	 * @throws IOException
	 */
	@Test
	public void testClassToReferenceAndReferenceToClassUnidirectional() throws IOException {
		var subdir = "classToReferenceUnidirectional/";

		var modelMigrator = setupMigrator(
			subdir,
			of("PersonList.ecore"),
			of("List.xmi")
		);

		var personWorks = getReference(evolvingModelManager,
				"PersonList", "Person", "works");
		// refactoring
		classToReference(modelMigrator, personWorks);
		referenceToClass(modelMigrator, personWorks, "WorkingPosition");

		copyModelsSaveAndAssertOutputs(
			modelMigrator,
			subdir,
			"referenceToClassUnidirectional/",
			of("PersonList.ecore"),
			of("List.xmi")
		);
	}

	@Test
	public void testReferenceToClassAndClassToReferenceUnidirectional() throws IOException {
		var subdir = "referenceToClassUnidirectional/";

		var modelMigrator = setupMigrator(
			subdir,
			of("PersonList.ecore"),
			of("List.xmi")
		);

		var personWorks = getReference(evolvingModelManager,
				"PersonList", "Person", "works");
		// refactoring
		referenceToClass(modelMigrator, personWorks, "WorkingPosition");
		classToReference(modelMigrator, personWorks);

		copyModelsSaveAndAssertOutputs(
			modelMigrator,
			subdir,
			"classToReferenceUnidirectional/",
			of("PersonList.ecore"),
			of("List.xmi")
		);
	}

	@Test
	public void testClassToReferenceAndReferenceToClassBidirectional() throws IOException {
		var subdir = "classToReferenceBidirectional/";

		var modelMigrator = setupMigrator(
			subdir,
			of("PersonList.ecore"),
			of("List.xmi")
		);

		var personWorks = getReference(evolvingModelManager,
				"PersonList", "Person", "works");
		// refactoring
		classToReference(modelMigrator, personWorks);
		referenceToClass(modelMigrator, personWorks, "WorkingPosition");

		copyModelsSaveAndAssertOutputs(
			modelMigrator,
			subdir,
			"referenceToClassBidirectional/",
			of("PersonList.ecore"),
			of("List.xmi")
		);
	}

	@Test
	public void testReferenceToClassAndClassToReferenceBidirectional() throws IOException {
		var subdir = "referenceToClassBidirectional/";

		var modelMigrator = setupMigrator(
			subdir,
			of("PersonList.ecore"),
			of("List.xmi")
		);

		var personWorks = getReference(evolvingModelManager,
				"PersonList", "Person", "works");
		// refactoring
		referenceToClass(modelMigrator, personWorks, "WorkingPosition");
		classToReference(modelMigrator, personWorks);

		copyModelsSaveAndAssertOutputs(
			modelMigrator,
			subdir,
			"classToReferenceBidirectional/",
			of("PersonList.ecore"),
			of("List.xmi")
		);
	}

	@Test
	public void testClassToReferenceAndReferenceToClassBidirectionalAlternative() throws IOException {
		var subdir = "classToReferenceBidirectional/";

		var modelMigrator = setupMigrator(
			subdir,
			of("PersonList.ecore"),
			of("List.xmi")
		);

		var personWorks = getReference(evolvingModelManager,
				"PersonList", "Person", "works");
		// refactoring
		classToReference(modelMigrator, personWorks);
		referenceToClassAlternative(modelMigrator, personWorks, "WorkingPosition");

		copyModelsSaveAndAssertOutputs(
			modelMigrator,
			subdir,
			"referenceToClassBidirectional/",
			of("PersonList.ecore"),
			of("List.xmi")
		);
	}

	@Test
	public void testReferenceToClassAndClassToReferenceBidirectionalAlternative() throws IOException {
		var subdir = "referenceToClassBidirectional/";

		var modelMigrator = setupMigrator(
			subdir,
			of("PersonList.ecore"),
			of("List.xmi")
		);

		var personWorks = getReference(evolvingModelManager,
				"PersonList", "Person", "works");
		// refactoring
		referenceToClassAlternative(modelMigrator, personWorks, "WorkingPosition");
		classToReference(modelMigrator, personWorks);

		copyModelsSaveAndAssertOutputs(
			modelMigrator,
			subdir,
			"classToReferenceBidirectional/",
			of("PersonList.ecore"),
			of("List.xmi")
		);
	}

	@Test
	public void testMergeAttributesManual() throws IOException {
		var subdir = "mergeAttributesManual/";
	
		var modelMigrator = setupMigrator(
			subdir,
			of("Person.ecore"),
			of("Person.xmi")
		);
	
		var firstName = getAttribute(evolvingModelManager,
				"person", "Person", "firstname");
		var lastName = getAttribute(evolvingModelManager,
				"person", "Person", "lastname");
		// refactoring
		EcoreUtil.remove(lastName);
		// rename the first attribute among the ones to merge
		firstName.setName("fullName");
		// specify the converter using firstname and lastname original values
		modelMigrator.transformAttributeValueRule(
			modelMigrator.isRelatedTo(firstName),
			(feature, o, oldValue) -> {
				// o is the old object,
				// so we must use the original feature to retrieve the value to copy
				// that is, don't use attribute, which is the one of the new package
				var eClass = o.eClass();
				return 
					o.eGet(feature) +
					" " +
					o.eGet(eClass.getEStructuralFeature("lastname"));
			}
		);
	
		copyModelsSaveAndAssertOutputs(
			modelMigrator,
			subdir,
			subdir,
			of("Person.ecore"),
			of("Person.xmi")
		);
	}

	@Test
	public void testMergeAttributesWithoutValueMerger() throws IOException {
		var subdir = "mergeAttributes/";

		var modelMigrator = setupMigrator(
			subdir,
			of("PersonList.ecore"),
			of("List.xmi")
		);

		final EClass person = getEClass(evolvingModelManager, "PersonList", "Person");
		var personFirstName = person.getEStructuralFeature("firstName");
		var personLastName = person.getEStructuralFeature("lastName");
		mergeFeatures(
			modelMigrator,
			"name",
			asList(
				personFirstName,
				personLastName),
			null, null);

		copyModelsSaveAndAssertOutputs(
			modelMigrator,
			subdir,
			"mergeAttributesWithoutValueMerger/",
			of("PersonList.ecore"),
			of("List.xmi")
		);
	}

	@Test
	public void testMergeAttributes() throws IOException {
		var subdir = "mergeAttributes/";

		var modelMigrator = setupMigrator(
			subdir,
			of("PersonList.ecore"),
			of("List.xmi")
		);

		final EClass person = getEClass(evolvingModelManager, "PersonList", "Person");
		mergeFeatures(
			modelMigrator,
			"name",
			asList(
				person.getEStructuralFeature("firstName"),
				person.getEStructuralFeature("lastName")),
			values -> {
				var merged = values.stream()
					.filter(Objects::nonNull)
					.map(Object::toString)
					.collect(Collectors.joining(" "));
				return merged.isEmpty() ? null : merged;
			}, null
		);

		copyModelsSaveAndAssertOutputs(
			modelMigrator,
			subdir,
			subdir,
			of("PersonList.ecore"),
			of("List.xmi")
		);
	}

	@Test
	public void testMergeFeaturesContainment() throws IOException {
		var subdir = "mergeFeaturesContainment/";

		var modelMigrator = setupMigrator(
			subdir,
			of("PersonList.ecore"),
			of("List.xmi")
		);

		EClass person = getEClass(evolvingModelManager, "PersonList", "Person");
		EClass nameElement = getEClass(evolvingModelManager, "PersonList", "NameElement");
		EAttribute nameElementAttribute =
				getAttribute(evolvingModelManager, "PersonList", "NameElement", "nameElementValue");
		assertNotNull(nameElementAttribute);
		mergeFeatures(
			modelMigrator,
			"name",
			asList(
				person.getEStructuralFeature("firstName"),
				person.getEStructuralFeature("lastName")),
			values -> {
				// it is responsibility of the merger to create an instance
				// of the (now single) referred object with the result
				// of merging the original objects' values
				var mergedValue = values.stream()
					.map(EObject.class::cast)
					.map(o -> 
						"" + o.eGet(nameElementAttribute))
					.collect(Collectors.joining(" "));
				if (mergedValue.isEmpty())
					return null;
				return EdeltaEcoreUtil.createInstance(nameElement,
					// since it's a containment feature, setting it will also
					// add it to the resource
					o -> o.eSet(nameElementAttribute, mergedValue)
				);
			}, null
		);

		copyModelsSaveAndAssertOutputs(
			modelMigrator,
			subdir,
			subdir,
			of("PersonList.ecore"),
			of("List.xmi")
		);
	}

	/**
	 * It might not make much sense to merge features concerning non containment
	 * references, but we just check that we can do it.
	 * 
	 * We assume that referred NameElements are not shared among Person, so that we
	 * remove them while performing the copy (and split and merging), otherwise we
	 * end up with a few additional objects in the final model.
	 * 
	 * In a more realistic scenario, the modeler will have to take care of that,
	 * e.g., by later removing NameElements that are not referred anymore.
	 * 
	 * @throws IOException
	 */
	@Test
	public void testMergeFeaturesNonContainment() throws IOException {
		var subdir = "mergeFeaturesNonContainment/";

		var modelMigrator = setupMigrator(
			subdir,
			of("PersonList.ecore"),
			of("List.xmi")
		);

		EClass person = getEClass(evolvingModelManager, "PersonList", "Person");
		EClass nameElement = getEClass(evolvingModelManager, "PersonList", "NameElement");
		EAttribute nameElementAttribute =
				getAttribute(evolvingModelManager, "PersonList", "NameElement", "nameElementValue");
		assertNotNull(nameElementAttribute);
		mergeFeatures(
			modelMigrator,
			"name",
			asList(
				person.getEStructuralFeature("firstName"),
				person.getEStructuralFeature("lastName")),
			values -> {
				// it is responsibility of the merger to create an instance
				// of the (now single) referred object with the result
				// of merging the original objects' values
				if (values.isEmpty())
					return null;
				@SuppressWarnings("unchecked")
				var objectValues =
					(List<EObject>) values;

				EObject firstObject = objectValues.iterator().next();
				var containingFeature = firstObject.eContainingFeature();
				@SuppressWarnings("unchecked")
				List<EObject> containerCollection = (List<EObject>) firstObject.eContainer().eGet(containingFeature);

				// assume that a referred NameElement object is not shared
				EcoreUtil.removeAll(objectValues);

				var mergedValue = objectValues.stream()
					.map(o -> 
						"" + o.eGet(nameElementAttribute))
					.collect(Collectors.joining(" "));
				return EdeltaEcoreUtil.createInstance(nameElement,
					o -> {
						o.eSet(nameElementAttribute, mergedValue);
						// since it's a NON containment feature, we have to manually
						// add it to the resource
						containerCollection.add(o);
					}
				);
			}, null
		);

		copyModelsSaveAndAssertOutputs(
			modelMigrator,
			subdir,
			subdir,
			of("PersonList.ecore"),
			of("List.xmi")
		);
	}

	/**
	 * Since the referred NameElements are shared among Person objects, we cannot
	 * simply remove the merged objects because they will have to be processed again
	 * during the model migration. So we need to keep the associations between
	 * objects that have been merged into a single one, so that the sharing
	 * semantics is kept in the evolved models, and we can then remove the old
	 * objects that have been merged (indeed they don't make sense anymore in the
	 * evolved model).
	 * 
	 * This requires some additional effort, but it shows that we can do it!
	 * 
	 * @throws IOException
	 */
	@Test
	public void testMergeFeaturesNonContainmentShared() throws IOException {
		var subdir = "mergeFeaturesNonContainmentShared/";

		var modelMigrator = setupMigrator(
			subdir,
			of("PersonList.ecore"),
			of("List.xmi")
		);

		EClass person = getEClass(evolvingModelManager, "PersonList", "Person");
		EClass nameElement = getEClass(evolvingModelManager, "PersonList", "NameElement");
		EAttribute nameElementAttribute =
				getAttribute(evolvingModelManager, "PersonList", "NameElement", "nameElementValue");
		assertNotNull(nameElementAttribute);

		// keep track of objects that are merged into a single one
		var merged = new HashMap<Collection<EObject>, EObject>();

		mergeFeatures(
			modelMigrator,
			"name",
			asList(
				person.getEStructuralFeature("firstName"),
				person.getEStructuralFeature("lastName")),
			values -> {
				// it is responsibility of the merger to create an instance
				// of the (now single) referred object with the result
				// of merging the original objects' values
				if (values.isEmpty())
					return null;
				@SuppressWarnings("unchecked")
				var objectValues =
					(List<EObject>) values;

				var alreadyMerged = merged.get(objectValues);
				if (alreadyMerged != null)
					return alreadyMerged;
				// we have already processed the object collection
				// and created a merged one so we reuse it

				EObject firstObject = objectValues.iterator().next();
				var containingFeature = firstObject.eContainingFeature();
				@SuppressWarnings("unchecked")
				List<EObject> containerCollection = (List<EObject>) firstObject.eContainer().eGet(containingFeature);

				var mergedValue = objectValues.stream()
					.map(o -> 
						"" + o.eGet(nameElementAttribute))
					.collect(Collectors.joining(" "));
				return EdeltaEcoreUtil.createInstance(nameElement,
					o -> {
						o.eSet(nameElementAttribute, mergedValue);
						// since it's a NON containment feature, we have to manually
						// add it to the resource
						containerCollection.add(o);

						// record that we associated the single object o
						// to the original ones, which are now merged
						merged.put(objectValues, o);
					}
				);
			},
			// now we can remove the stale objects that have been merged
			() -> EcoreUtil.removeAll(
					merged.keySet().stream()
						.flatMap(Collection<EObject>::stream)
						.collect(Collectors.toList()))
		);

		copyModelsSaveAndAssertOutputs(
			modelMigrator,
			subdir,
			subdir,
			of("PersonList.ecore"),
			of("List.xmi")
		);
	}

	@Test
	public void testSplitAttributes() throws IOException {
		var subdir = "splitAttributes/";

		var modelMigrator = setupMigrator(
			subdir,
			of("PersonList.ecore"),
			of("List.xmi")
		);

		final EClass person = getEClass(evolvingModelManager, "PersonList", "Person");
		EStructuralFeature personName = person.getEStructuralFeature("name");
		splitFeature(
			modelMigrator,
			personName,
			asList(
				"firstName",
				"lastName"),
			value -> {
				// a few more checks should be performed in a realistic context
				if (value == null)
					return Collections.emptyList();
				String[] split = value.toString().split("\\s+");
				return Arrays.asList(split);
			}, null
		);

		copyModelsSaveAndAssertOutputs(
			modelMigrator,
			subdir,
			subdir,
			of("PersonList.ecore"),
			of("List.xmi")
		);
	}

	@Test
	public void testSplitFeatureContainment() throws IOException {
		var subdir = "splitFeatureContainment/";

		var modelMigrator = setupMigrator(
			subdir,
			of("PersonList.ecore"),
			of("List.xmi")
		);

		final EClass person = getEClass(evolvingModelManager, "PersonList", "Person");
		EStructuralFeature personName = person.getEStructuralFeature("name");
		EClass nameElement = getEClass(evolvingModelManager, "PersonList", "NameElement");
		EAttribute nameElementAttribute =
				getAttribute(evolvingModelManager, "PersonList", "NameElement", "nameElementValue");
		assertNotNull(nameElementAttribute);
		splitFeature(
			modelMigrator,
			personName,
			asList(
				"firstName",
				"lastName"),
			value -> {
				// a few more checks should be performed in a realistic context
				if (value == null)
					return Collections.emptyList();
				var obj = (EObject) value;
				// of course if there's no space and only one element in the array
				// it will assigned to the first feature value
				// that is, in case of a single element, the lastName will be empty
				String[] split = obj.eGet(nameElementAttribute).toString().split("\\s+");
				return Stream.of(split)
					.map(val -> 
						EdeltaEcoreUtil.createInstance(nameElement,
							o -> o.eSet(nameElementAttribute, val)
						)
					)
					.collect(Collectors.toList());
			}, null
		);

		copyModelsSaveAndAssertOutputs(
			modelMigrator,
			subdir,
			subdir,
			of("PersonList.ecore"),
			of("List.xmi")
		);
	}

	/**
	 * We assume that referred NameElements are not shared among Person, so that we
	 * remove them while performing the copy (and split and merging), otherwise we
	 * end up with a few additional objects in the final model.
	 * 
	 * In a more realistic scenario, the modeler will have to take care of that,
	 * e.g., by later removing NameElements that are not referred anymore.
	 * 
	 * @throws IOException
	 */
	@Test
	public void testSplitFeatureNonContainment() throws IOException {
		var subdir = "splitFeatureNonContainment/";

		var modelMigrator = setupMigrator(
			subdir,
			of("PersonList.ecore"),
			of("List.xmi")
		);

		final EClass person = getEClass(evolvingModelManager, "PersonList", "Person");
		EStructuralFeature personName = person.getEStructuralFeature("name");
		EClass nameElement = getEClass(evolvingModelManager, "PersonList", "NameElement");
		EAttribute nameElementAttribute =
				getAttribute(evolvingModelManager, "PersonList", "NameElement", "nameElementValue");
		assertNotNull(nameElementAttribute);
		splitFeature(
			modelMigrator,
			personName,
			asList(
				"firstName",
				"lastName"),
			value -> {
				// a few more checks should be performed in a realistic context
				if (value == null)
					return Collections.emptyList();
				var obj = (EObject) value;

				var containingFeature = obj.eContainingFeature();
				@SuppressWarnings("unchecked")
				var containerCollection =
					(List<EObject>) obj.eContainer().eGet(containingFeature);

				// assume that a referred NameElement object is not shared
				EcoreUtil.remove(obj);

				// of course if there's no space and only one element in the array
				// it will assigned to the first feature value
				// that is, in case of a single element, the lastName will be empty
				String[] split = obj.eGet(nameElementAttribute).toString().split("\\s+");
				return Stream.of(split)
					.map(val -> EdeltaEcoreUtil.createInstance(nameElement,
						o -> {
							o.eSet(nameElementAttribute, val);
							// since it's a NON containment feature, we have to manually
							// add it to the resource
							containerCollection.add(o);
						}
					))
					.collect(Collectors.toList());
			}, null
		);

		copyModelsSaveAndAssertOutputs(
			modelMigrator,
			subdir,
			subdir,
			of("PersonList.ecore"),
			of("List.xmi")
		);
	}

	/**
	 * Since the referred NameElements are shared among Person objects, we cannot
	 * simply remove the already splitted objects because they will have to be processed again
	 * during the model migration. So we need to keep the associations between
	 * objects that have been splitted into several ones (2 in this example), so that the sharing
	 * semantics is kept in the evolved models, and we can then remove the old
	 * objects that have been splitted (indeed they don't make sense anymore in the
	 * evolved model).
	 * 
	 * This requires some additional effort, but it shows that we can do it!
	 * 
	 * @throws IOException
	 */
	@Test
	public void testSplitFeatureNonContainmentShared() throws IOException {
		var subdir = "splitFeatureNonContainmentShared/";

		var modelMigrator = setupMigrator(
			subdir,
			of("PersonList.ecore"),
			of("List.xmi")
		);

		final EClass person = getEClass(evolvingModelManager, "PersonList", "Person");
		EStructuralFeature personName = person.getEStructuralFeature("name");
		EClass nameElement = getEClass(evolvingModelManager, "PersonList", "NameElement");
		EAttribute nameElementAttribute =
				getAttribute(evolvingModelManager, "PersonList", "NameElement", "nameElementValue");
		assertNotNull(nameElementAttribute);

		// keep track of objects that are splitted into several ones
		var splitted = new HashMap<EObject, Collection<EObject>>();

		splitFeature(
			modelMigrator,
			personName,
			asList(
				"firstName",
				"lastName"),
			value -> {
				// a few more checks should be performed in a realistic context
				if (value == null)
					return Collections.emptyList();
				var obj = (EObject) value;

				var alreadySplitted = splitted.get(obj);
				if (alreadySplitted != null)
					return alreadySplitted;
				// we have already processed the object collection
				// and created a merged one so we reuse it

				var containingFeature = obj.eContainingFeature();
				@SuppressWarnings("unchecked")
				var containerCollection =
					(List<EObject>) obj.eContainer().eGet(containingFeature);

				// of course if there's no space and only one element in the array
				// it will assigned to the first feature value
				// that is, in case of a single element, the lastName will be empty
				String[] split = obj.eGet(nameElementAttribute).toString().split("\\s+");
				var result = Stream.of(split)
					.map(val -> EdeltaEcoreUtil.createInstance(nameElement,
						o -> {
							o.eSet(nameElementAttribute, val);

							// since it's a NON containment feature, we have to manually
							// add it to the resource
							containerCollection.add(o);
						}
					))
					.collect(Collectors.toList());

				// record that we associated the several objects (2 in this example)
				// to the original one, which is now splitted
				splitted.put(obj, result);

				return result;
			},
			// now we can remove the stale objects that have been splitted
			() -> EcoreUtil.removeAll(splitted.keySet())
		);

		copyModelsSaveAndAssertOutputs(
			modelMigrator,
			subdir,
			subdir,
			of("PersonList.ecore"),
			of("List.xmi")
		);
	}

	/**
	 * The evolved metamodel and model are just the same as the original ones, as
	 * long the merge and split can be inversed. For example, in this test we have
	 * "firstname lastname" or no string at all. If you had "lastname" then the
	 * model wouldn't be reversable.
	 * 
	 * The input directory and the output one will contain the same data.
	 * 
	 * @throws IOException
	 */
	@Test
	public void testSplitAndMergeAttributes() throws IOException {
		var subdir = "splitAndMergeAttributes/";

		var modelMigrator = setupMigrator(
			subdir,
			of("PersonList.ecore"),
			of("List.xmi")
		);

		final EClass person = getEClass(evolvingModelManager, "PersonList", "Person");
		EStructuralFeature personName = person.getEStructuralFeature("name");
		Collection<EStructuralFeature> splitFeatures = splitFeature(
			modelMigrator,
			personName,
			asList(
				"firstName",
				"lastName"),
			value -> {
				// a few more checks should be performed in a realistic context
				if (value == null)
					return Collections.emptyList();
				String[] split = value.toString().split("\\s+");
				return Arrays.asList(split);
			}, null
		);
		mergeFeatures(
			modelMigrator,
			"name",
			splitFeatures,
			values -> {
				var merged = values.stream()
					.filter(Objects::nonNull)
					.map(Object::toString)
					.collect(Collectors.joining(" "));
				return merged.isEmpty() ? null : merged;
			}, null
		);

		copyModelsSaveAndAssertOutputs(
			modelMigrator,
			subdir,
			subdir,
			of("PersonList.ecore"),
			of("List.xmi")
		);
	}

	/**
	 * The evolved metamodel and model are just the same as the original ones.
	 * 
	 * The input directory and the output one will contain the same data.
	 * 
	 * @throws IOException
	 */
	@Test
	public void testMergeAndSplitAttributes() throws IOException {
		var subdir = "mergeAndSplitAttributes/";

		var modelMigrator = setupMigrator(
			subdir,
			of("PersonList.ecore"),
			of("List.xmi")
		);

		final EClass person = getEClass(evolvingModelManager, "PersonList", "Person");
		var personFirstName = person.getEStructuralFeature("firstName");
		var personLastName = person.getEStructuralFeature("lastName");
		EStructuralFeature mergedFeature = mergeFeatures(
			modelMigrator,
			"name",
			asList(
				personFirstName,
				personLastName),
			values -> {
				var merged = values.stream()
					.filter(Objects::nonNull)
					.map(Object::toString)
					.collect(Collectors.joining(" "));
				return merged.isEmpty() ? null : merged;
			}, null);
		splitFeature(
			modelMigrator,
			mergedFeature,
			asList(
				personFirstName.getName(),
				personLastName.getName()),
			value -> {
				// a few more checks should be performed in a realistic context
				if (value == null)
					return Collections.emptyList();
				String[] split = value.toString().split("\\s+");
				return Arrays.asList(split);
			}, null
		);

		copyModelsSaveAndAssertOutputs(
			modelMigrator,
			subdir,
			subdir,
			of("PersonList.ecore"),
			of("List.xmi")
		);
	}

	@Test
	public void testMergeAndSplitFeaturesContainment() throws IOException {
		var subdir = "mergeAndSplitFeaturesContainment/";

		var modelMigrator = setupMigrator(
			subdir,
			of("PersonList.ecore"),
			of("List.xmi")
		);

		EClass person = getEClass(evolvingModelManager, "PersonList", "Person");
		EClass nameElement = getEClass(evolvingModelManager, "PersonList", "NameElement");
		EAttribute nameElementAttribute =
				getAttribute(evolvingModelManager, "PersonList", "NameElement", "nameElementValue");
		var personFirstName = person.getEStructuralFeature("firstName");
		var personLastName = person.getEStructuralFeature("lastName");
		assertNotNull(nameElementAttribute);
		EStructuralFeature mergedFeature =  mergeFeatures(
			modelMigrator,
			"name",
			asList(
				personFirstName,
				personLastName),
			values -> {
				// it is responsibility of the merger to create an instance
				// of the (now single) referred object with the result
				// of merging the original objects' values
				var mergedValue = values.stream()
					.map(EObject.class::cast)
					.map(o -> 
						"" + o.eGet(nameElementAttribute))
					.collect(Collectors.joining(" "));
				if (mergedValue.isEmpty())
					return null;
				return EdeltaEcoreUtil.createInstance(nameElement,
					// since it's a containment feature, setting it will also
					// add it to the resource
					o -> o.eSet(nameElementAttribute, mergedValue)
				);
			}, null
		);
		splitFeature(
			modelMigrator,
			mergedFeature,
			asList(
				personFirstName.getName(),
				personLastName.getName()),
			value -> {
				// a few more checks should be performed in a realistic context
				if (value == null)
					return Collections.emptyList();
				var obj = (EObject) value;
				// of course if there's no space and only one element in the array
				// it will assigned to the first feature value
				// that is, in case of a single element, the lastName will be empty
				String[] split = obj.eGet(nameElementAttribute).toString().split("\\s+");
				return Stream.of(split)
					.map(val -> 
						EdeltaEcoreUtil.createInstance(nameElement,
							o -> o.eSet(nameElementAttribute, val)
						)
					)
					.collect(Collectors.toList());
			}, null
		);

		copyModelsSaveAndAssertOutputs(
			modelMigrator,
			subdir,
			subdir,
			of("PersonList.ecore"),
			of("List.xmi")
		);
	}

	/**
	 * See the Javadoc of
	 * {@link #testMergeFeaturesNonContainment()}
	 * and
	 * {@link #testSplitFeatureNonContainment()}
	 * 
	 * @throws IOException
	 */
	@Test
	public void testMergeAndSplitFeaturesNonContainment() throws IOException {
		var subdir = "mergeFeaturesNonContainment/";

		var modelMigrator = setupMigrator(
			subdir,
			of("PersonList.ecore"),
			of("List.xmi")
		);

		EClass person = getEClass(evolvingModelManager, "PersonList", "Person");
		EClass nameElement = getEClass(evolvingModelManager, "PersonList", "NameElement");
		EAttribute nameElementAttribute =
				getAttribute(evolvingModelManager, "PersonList", "NameElement", "nameElementValue");
		var personFirstName = person.getEStructuralFeature("firstName");
		var personLastName = person.getEStructuralFeature("lastName");
		assertNotNull(nameElementAttribute);
		EStructuralFeature mergedFeature =  mergeFeatures(
			modelMigrator,
			"name",
			asList(
				personFirstName,
				personLastName),
			values -> {
				// it is responsibility of the merger to create an instance
				// of the (now single) referred object with the result
				// of merging the original objects' values
				if (values.isEmpty())
					return null;
				@SuppressWarnings("unchecked")
				var objectValues =
					(List<EObject>) values;

				EObject firstObject = objectValues.iterator().next();
				var containingFeature = firstObject.eContainingFeature();
				@SuppressWarnings("unchecked")
				List<EObject> containerCollection = (List<EObject>) firstObject.eContainer().eGet(containingFeature);

				// assume that a referred NameElement object is not shared
				EcoreUtil.removeAll(objectValues);

				var mergedValue = objectValues.stream()
					.map(o -> 
						"" + o.eGet(nameElementAttribute))
					.collect(Collectors.joining(" "));
				return EdeltaEcoreUtil.createInstance(nameElement,
					o -> {
						o.eSet(nameElementAttribute, mergedValue);
						// since it's a NON containment feature, we have to manually
						// add it to the resource
						containerCollection.add(o);
					}
				);
			}, null
		);
		splitFeature(
			modelMigrator,
			mergedFeature,
			asList(
				personFirstName.getName(),
				personLastName.getName()),
			value -> {
				// a few more checks should be performed in a realistic context
				if (value == null)
					return Collections.emptyList();
				var obj = (EObject) value;

				var containingFeature = obj.eContainingFeature();
				@SuppressWarnings("unchecked")
				var containerCollection =
					(List<EObject>) obj.eContainer().eGet(containingFeature);

				// assume that a referred NameElement object is not shared
				EcoreUtil.remove(obj);

				// of course if there's no space and only one element in the array
				// it will assigned to the first feature value
				// that is, in case of a single element, the lastName will be empty
				String[] split = obj.eGet(nameElementAttribute).toString().split("\\s+");
				return Stream.of(split)
					.map(val -> EdeltaEcoreUtil.createInstance(nameElement,
						o -> {
							o.eSet(nameElementAttribute, val);
							// since it's a NON containment feature, we have to manually
							// add it to the resource
							containerCollection.add(o);
						}
					))
					.collect(Collectors.toList());
			}, null
		);

		copyModelsSaveAndAssertOutputs(
			modelMigrator,
			subdir,
			"splitFeatureNonContainment/",
			of("PersonList.ecore"),
			of("List.xmi")
		);
	}

	/**
	 * See the Javadoc of
	 * {@link #testMergeFeaturesNonContainmentShared()}
	 * and
	 * {@link #testSplitFeatureNonContainmentShared()}
	 * 
	 * @throws IOException
	 */
	@Test
	public void testMergeAndSplitFeaturesNonContainmentShared() throws IOException {
		var subdir = "mergeFeaturesNonContainmentShared/";

		var modelMigrator = setupMigrator(
			subdir,
			of("PersonList.ecore"),
			of("List.xmi")
		);

		EClass person = getEClass(evolvingModelManager, "PersonList", "Person");
		EClass nameElement = getEClass(evolvingModelManager, "PersonList", "NameElement");
		EAttribute nameElementAttribute =
				getAttribute(evolvingModelManager, "PersonList", "NameElement", "nameElementValue");
		var personFirstName = person.getEStructuralFeature("firstName");
		var personLastName = person.getEStructuralFeature("lastName");
		assertNotNull(nameElementAttribute);

		// keep track of objects that are merged into a single one
		var merged = new HashMap<Collection<EObject>, EObject>();

		EStructuralFeature mergedFeature =  mergeFeatures(
			modelMigrator,
			"name",
			asList(
				personFirstName,
				personLastName),
			values -> {
				// it is responsibility of the merger to create an instance
				// of the (now single) referred object with the result
				// of merging the original objects' values
				if (values.isEmpty())
					return null;
				@SuppressWarnings("unchecked")
				var objectValues =
					(List<EObject>) values;

				var alreadyMerged = merged.get(objectValues);
				if (alreadyMerged != null)
					return alreadyMerged;
				// we have already processed the object collection
				// and created a merged one so we reuse it

				EObject firstObject = objectValues.iterator().next();
				var containingFeature = firstObject.eContainingFeature();
				@SuppressWarnings("unchecked")
				List<EObject> containerCollection = (List<EObject>) firstObject.eContainer().eGet(containingFeature);

				var mergedValue = objectValues.stream()
					.map(o -> 
						"" + o.eGet(nameElementAttribute))
					.collect(Collectors.joining(" "));
				return EdeltaEcoreUtil.createInstance(nameElement,
					o -> {
						o.eSet(nameElementAttribute, mergedValue);
						// since it's a NON containment feature, we have to manually
						// add it to the resource
						containerCollection.add(o);

						// record that we associated the single object o
						// to the original ones, which are now merged
						merged.put(objectValues, o);
					}
				);
			},
			// now we can remove the stale objects that have been merged
			() -> EcoreUtil.removeAll(
					merged.keySet().stream()
						.flatMap(Collection<EObject>::stream)
						.collect(Collectors.toList()))
		);

		// keep track of objects that are splitted into several ones
		var splitted = new HashMap<EObject, Collection<EObject>>();

		splitFeature(
			modelMigrator,
			mergedFeature,
			asList(
				personFirstName.getName(),
				personLastName.getName()),
			value -> {
				// a few more checks should be performed in a realistic context
				if (value == null)
					return Collections.emptyList();
				var obj = (EObject) value;

				var alreadySplitted = splitted.get(obj);
				if (alreadySplitted != null)
					return alreadySplitted;
				// we have already processed the object collection
				// and created a merged one so we reuse it

				var containingFeature = obj.eContainingFeature();
				@SuppressWarnings("unchecked")
				var containerCollection =
					(List<EObject>) obj.eContainer().eGet(containingFeature);

				// of course if there's no space and only one element in the array
				// it will assigned to the first feature value
				// that is, in case of a single element, the lastName will be empty
				String[] split = obj.eGet(nameElementAttribute).toString().split("\\s+");
				var result = Stream.of(split)
					.map(val -> EdeltaEcoreUtil.createInstance(nameElement,
						o -> {
							o.eSet(nameElementAttribute, val);

							// since it's a NON containment feature, we have to manually
							// add it to the resource
							containerCollection.add(o);
						}
					))
					.collect(Collectors.toList());

				// record that we associated the several objects (2 in this example)
				// to the original one, which is now splitted
				splitted.put(obj, result);

				return result;
			},
			// now we can remove the stale objects that have been splitted
			() -> EcoreUtil.removeAll(splitted.keySet())
		);

		copyModelsSaveAndAssertOutputs(
			modelMigrator,
			subdir,
			"splitFeatureNonContainmentShared/",
			of("PersonList.ecore"),
			of("List.xmi")
		);
	}

	/**
	 * The evolved metamodel and model are just the same as the original ones, as
	 * long the merge and split can be inversed. For example, in this test we have
	 * "firstname lastname" or no string at all. If you had "lastname" then the
	 * model wouldn't be reversable.
	 * 
	 * The input directory and the output one will contain the same data.
	 * 
	 * @throws IOException
	 */
	@Test
	public void testSplitAndMergeFeatureContainment() throws IOException {
		var subdir = "splitAndMergeFeatureContainment/";
	
		var modelMigrator = setupMigrator(
			subdir,
			of("PersonList.ecore"),
			of("List.xmi")
		);
	
		final EClass person = getEClass(evolvingModelManager, "PersonList", "Person");
		EStructuralFeature personName = person.getEStructuralFeature("name");
		EClass nameElement = getEClass(evolvingModelManager, "PersonList", "NameElement");
		EAttribute nameElementAttribute =
				getAttribute(evolvingModelManager, "PersonList", "NameElement", "nameElementValue");
		assertNotNull(nameElementAttribute);
		Collection<EStructuralFeature> splitFeatures = splitFeature(
			modelMigrator,
			personName,
			asList(
				"firstName",
				"lastName"),
			value -> {
				// a few more checks should be performed in a realistic context
				if (value == null)
					return Collections.emptyList();
				var obj = (EObject) value;
				// of course if there's no space and only one element in the array
				// it will assigned to the first feature value
				// that is, in case of a single element, the lastName will be empty
				String[] split = obj.eGet(nameElementAttribute).toString().split("\\s+");
				return Stream.of(split)
					.map(val -> 
						EdeltaEcoreUtil.createInstance(nameElement,
							o -> o.eSet(nameElementAttribute, val)
						)
					)
					.collect(Collectors.toList());
			}, null
		);
		mergeFeatures(
			modelMigrator,
			"name",
			splitFeatures,
			values -> {
				// it is responsibility of the merger to create an instance
				// of the (now single) referred object with the result
				// of merging the original objects' values
				var mergedValue = values.stream()
					.map(EObject.class::cast)
					.map(o -> 
						"" + o.eGet(nameElementAttribute))
					.collect(Collectors.joining(" "));
				if (mergedValue.isEmpty())
					return null;
				return EdeltaEcoreUtil.createInstance(nameElement,
					// since it's a containment feature, setting it will also
					// add it to the resource
					o -> o.eSet(nameElementAttribute, mergedValue)
				);
			}, null
		);
	
		copyModelsSaveAndAssertOutputs(
			modelMigrator,
			subdir,
			subdir,
			of("PersonList.ecore"),
			of("List.xmi")
		);
	}

	/**
	 * To make the two refactorings completely inverse, we must assume that referred
	 * NameElements are not shared among Person, so that we remove them while
	 * performing the copy (and split and merging), otherwise we end up with a few
	 * additional objects in the final model, which will not be exactly the same as
	 * the initial one.
	 * 
	 * In a more realistic scenario, the modeler will have to take care of that,
	 * e.g., by later removing NameElements that are not referred anymore.
	 * 
	 * @throws IOException
	 */
	@Test
	public void testSplitAndMergeFeatureNonContainment() throws IOException {
		var subdir = "splitAndMergeFeatureNonContainment/";

		var modelMigrator = setupMigrator(
			subdir,
			of("PersonList.ecore"),
			of("List.xmi")
		);

		final EClass person = getEClass(evolvingModelManager, "PersonList", "Person");
		EStructuralFeature personName = person.getEStructuralFeature("name");
		EClass nameElement = getEClass(evolvingModelManager, "PersonList", "NameElement");
		EAttribute nameElementAttribute =
				getAttribute(evolvingModelManager, "PersonList", "NameElement", "nameElementValue");
		assertNotNull(nameElementAttribute);
		Collection<EStructuralFeature> splitFeatures = splitFeature(
			modelMigrator,
			personName,
			asList(
				"firstName",
				"lastName"),
			value -> {
				// a few more checks should be performed in a realistic context
				if (value == null)
					return Collections.emptyList();
				var obj = (EObject) value;

				var containingFeature = obj.eContainingFeature();
				@SuppressWarnings("unchecked")
				var containerCollection =
					(List<EObject>) obj.eContainer().eGet(containingFeature);

				// assume that a referred NameElement object is not shared
				EcoreUtil.remove(obj);

				// of course if there's no space and only one element in the array
				// it will assigned to the first feature value
				// that is, in case of a single element, the lastName will be empty
				String[] split = obj.eGet(nameElementAttribute).toString().split("\\s+");
				return Stream.of(split)
					.map(val -> EdeltaEcoreUtil.createInstance(nameElement,
						o -> {
							o.eSet(nameElementAttribute, val);
							// since it's a NON containment feature, we have to manually
							// add it to the resource
							containerCollection.add(o);
						}
					))
					.collect(Collectors.toList());
			}, null
		);
		mergeFeatures(
			modelMigrator,
			"name",
			splitFeatures,
			values -> {
				// it is responsibility of the merger to create an instance
				// of the (now single) referred object with the result
				// of merging the original objects' values
				if (values.isEmpty())
					return null;
				@SuppressWarnings("unchecked")
				var objectValues =
					(List<EObject>) values;

				EObject firstObject = objectValues.iterator().next();
				var containingFeature = firstObject.eContainingFeature();
				@SuppressWarnings("unchecked")
				List<EObject> containerCollection = (List<EObject>) firstObject.eContainer().eGet(containingFeature);

				// assume that a referred NameElement object is not shared
				EcoreUtil.removeAll(objectValues);

				var mergedValue = objectValues.stream()
					.map(o -> 
						"" + o.eGet(nameElementAttribute))
					.collect(Collectors.joining(" "));
				return EdeltaEcoreUtil.createInstance(nameElement,
					o -> {
						o.eSet(nameElementAttribute, mergedValue);
						// since it's a NON containment feature, we have to manually
						// add it to the resource
						containerCollection.add(o);
					}
				);
			}, null
		);

		copyModelsSaveAndAssertOutputs(
			modelMigrator,
			subdir,
			subdir,
			of("PersonList.ecore"),
			of("List.xmi")
		);
	}

	/**
	 * See the Javadoc of
	 * {@link #testSplitFeatureNonContainmentShared()}
	 * and
	 * {@link #testMergeFeaturesNonContainmentShared()}
	 * 
	 * @throws IOException
	 */
	@Test
	public void testSplitAndMergeFeaturesNonContainmentShared() throws IOException {
		var subdir = "splitFeatureNonContainmentShared/";

		var modelMigrator = setupMigrator(
			subdir,
			of("PersonList.ecore"),
			of("List.xmi")
		);

		final EClass person = getEClass(evolvingModelManager, "PersonList", "Person");
		EStructuralFeature personName = person.getEStructuralFeature("name");
		EClass nameElement = getEClass(evolvingModelManager, "PersonList", "NameElement");
		EAttribute nameElementAttribute =
				getAttribute(evolvingModelManager, "PersonList", "NameElement", "nameElementValue");
		assertNotNull(nameElementAttribute);

		// keep track of objects that are splitted into several ones
		var splitted = new HashMap<EObject, Collection<EObject>>();

		Collection<EStructuralFeature> splitFeatures = splitFeature(
			modelMigrator,
			personName,
			asList(
				"firstName",
				"lastName"),
			value -> {
				// a few more checks should be performed in a realistic context
				if (value == null)
					return Collections.emptyList();
				var obj = (EObject) value;

				var alreadySplitted = splitted.get(obj);
				if (alreadySplitted != null)
					return alreadySplitted;
				// we have already processed the object collection
				// and created a merged one so we reuse it

				var containingFeature = obj.eContainingFeature();
				@SuppressWarnings("unchecked")
				var containerCollection =
					(List<EObject>) obj.eContainer().eGet(containingFeature);

				// of course if there's no space and only one element in the array
				// it will assigned to the first feature value
				// that is, in case of a single element, the lastName will be empty
				String[] split = obj.eGet(nameElementAttribute).toString().split("\\s+");
				var result = Stream.of(split)
					.map(val -> EdeltaEcoreUtil.createInstance(nameElement,
						o -> {
							o.eSet(nameElementAttribute, val);

							// since it's a NON containment feature, we have to manually
							// add it to the resource
							containerCollection.add(o);
						}
					))
					.collect(Collectors.toList());

				// record that we associated the several objects (2 in this example)
				// to the original one, which is now splitted
				splitted.put(obj, result);

				return result;
			},
			// now we can remove the stale objects that have been splitted
			() -> EcoreUtil.removeAll(splitted.keySet())
		);

		// keep track of objects that are merged into a single one
		var merged = new HashMap<Collection<EObject>, EObject>();

		mergeFeatures(
			modelMigrator,
			"name",
			splitFeatures,
			values -> {
				// it is responsibility of the merger to create an instance
				// of the (now single) referred object with the result
				// of merging the original objects' values
				if (values.isEmpty())
					return null;
				@SuppressWarnings("unchecked")
				var objectValues =
					(List<EObject>) values;

				var alreadyMerged = merged.get(objectValues);
				if (alreadyMerged != null)
					return alreadyMerged;
				// we have already processed the object collection
				// and created a merged one so we reuse it

				EObject firstObject = objectValues.iterator().next();
				var containingFeature = firstObject.eContainingFeature();
				@SuppressWarnings("unchecked")
				List<EObject> containerCollection = (List<EObject>) firstObject.eContainer().eGet(containingFeature);

				var mergedValue = objectValues.stream()
					.map(o -> 
						"" + o.eGet(nameElementAttribute))
					.collect(Collectors.joining(" "));
				return EdeltaEcoreUtil.createInstance(nameElement,
					o -> {
						o.eSet(nameElementAttribute, mergedValue);
						// since it's a NON containment feature, we have to manually
						// add it to the resource
						containerCollection.add(o);

						// record that we associated the single object o
						// to the original ones, which are now merged
						merged.put(objectValues, o);
					}
				);
			},
			// now we can remove the stale objects that have been merged
			() -> EcoreUtil.removeAll(
					merged.keySet().stream()
						.flatMap(Collection<EObject>::stream)
						.collect(Collectors.toList()))
		);

		copyModelsSaveAndAssertOutputs(
			modelMigrator,
			subdir,
			"mergeFeaturesNonContainmentShared/",
			of("PersonList.ecore"),
			of("List.xmi")
		);
	}

	@Test
	public void testEnumToSubclasses() throws IOException {
		var subdir = "enumToSubclasses/";

		var modelMigrator = setupMigrator(
			subdir,
			of("PersonList.ecore"),
			of("List.xmi")
		);

		var person = getEClass(evolvingModelManager, "PersonList", "Person");
		var genreAttribute = (EAttribute) person.getEStructuralFeature("gender");
		enumToSubclasses(modelMigrator, genreAttribute);

		copyModelsSaveAndAssertOutputs(
			modelMigrator,
			subdir,
			subdir,
			of("PersonList.ecore"),
			of("List.xmi")
		);
	}

	@Test
	public void testSubclassesToEnum() throws IOException {
		var subdir = "subclassesToEnum/";
		
		var modelMigrator = setupMigrator(
			subdir,
			of("PersonList.ecore"),
			of("List.xmi")
		);

		var personList = evolvingModelManager.getEPackage("PersonList");
		subclassesToEnum(modelMigrator,
			"Gender",
			asList(
				(EClass) personList.getEClassifier("Male"),
				(EClass) personList.getEClassifier("Female")));

		copyModelsSaveAndAssertOutputs(
			modelMigrator,
			subdir,
			subdir,
			of("PersonList.ecore"),
			of("List.xmi")
		);
	}

	@Test
	public void testEnumToSubclassesAndSubclassesToEnum() throws IOException {
		var subdir = "enumToSubclasses/";

		var modelMigrator = setupMigrator(
			subdir,
			of("PersonList.ecore"),
			of("List.xmi")
		);

		var person = getEClass(evolvingModelManager, "PersonList", "Person");
		var genreAttribute = (EAttribute) person.getEStructuralFeature("gender");
		var genreName = genreAttribute.getEAttributeType().getName();

		Collection<EClass> subclasses =
			enumToSubclasses(modelMigrator, genreAttribute);

		subclassesToEnum(modelMigrator,
			genreName, // "Genre"
			subclasses);

		copyModelsSaveAndAssertOutputs(
			modelMigrator,
			subdir,
			"subclassesToEnum/",
			of("PersonList.ecore"),
			of("List.xmi")
		);
	}

	@Test
	public void testSubclassesToEnumAndEnumToSubclasses() throws IOException {
		var subdir = "subclassesToEnum/";
		
		var modelMigrator = setupMigrator(
			subdir,
			of("PersonList.ecore"),
			of("List.xmi")
		);

		var personList = evolvingModelManager.getEPackage("PersonList");

		EAttribute genreAttribute = subclassesToEnum(modelMigrator,
			"Gender",
			asList(
				(EClass) personList.getEClassifier("Male"),
				(EClass) personList.getEClassifier("Female")));

		enumToSubclasses(modelMigrator, genreAttribute);

		copyModelsSaveAndAssertOutputs(
			modelMigrator,
			subdir,
			"enumToSubclasses/",
			of("PersonList.ecore"),
			of("List.xmi")
		);
	}

	/**
	 * enumToSubclasses, pushDown twice, merge attributes twice, pull up the merged
	 * attributes.
	 * 
	 * Just for testing, the refactorings might not make sense.
	 * 
	 * @throws IOException
	 */
	@Test
	public void testComposeOperations1() throws IOException {
		var subdir = "enumToSubclasses/";

		var modelMigrator = setupMigrator(
			subdir,
			of("PersonList.ecore"),
			of("List.xmi")
		);

		var person = getEClass(evolvingModelManager, "PersonList", "Person");
		var genreAttribute = (EAttribute) person.getEStructuralFeature("gender");

		Collection<EClass> subclasses =
			enumToSubclasses(modelMigrator, genreAttribute);

		var personFirstName = person.getEStructuralFeature("firstname");
		var personLastName = person.getEStructuralFeature("lastname");

		// push down in both subclasses
		pushDown(modelMigrator,
			personFirstName,
			subclasses);
		pushDown(modelMigrator,
			personLastName,
			subclasses);

		// merge for both subclasses
		subclasses.forEach(
			subclass ->
				mergeFeatures(
					modelMigrator,
					"name",
					asList(
						subclass.getEStructuralFeature("firstname"),
						subclass.getEStructuralFeature("lastname")),
					values -> {
						var merged = values.stream()
							.filter(Objects::nonNull)
							.map(Object::toString)
							.collect(Collectors.joining(" "));
						return merged.isEmpty() ? null : merged;
					}, null
				)
		);

		// pull up the merged features (name) in the superclass
		pullUp(modelMigrator, person,
			subclasses.stream()
				.map(c -> c.getEStructuralFeature("name"))
				.collect(Collectors.toList())
		);

		copyModelsSaveAndAssertOutputs(
			modelMigrator,
			subdir,
			"composeOperations1/",
			of("PersonList.ecore"),
			of("List.xmi")
		);
	}

	private void copyModelsSaveAndAssertOutputs(
			EdeltaModelMigrator modelMigrator,
			String origdir,
			String outputdir,
			Collection<String> ecoreFiles,
			Collection<String> modelFiles
		) throws IOException {
		var basedir = TESTDATA + origdir;
		copyModels(modelMigrator, basedir);
		var output = OUTPUT + outputdir;
		evolvingModelManager.saveEcores(output);
		evolvingModelManager.saveModels(output);
		ecoreFiles.forEach
			(fileName ->
				assertGeneratedFiles(fileName, outputdir, output, fileName));
		modelFiles.forEach
			(fileName ->
				assertGeneratedFiles(fileName, outputdir, output, fileName));
	}

	private EAttribute getAttribute(EdeltaModelManager modelManager, String packageName, String className, String attributeName) {
		return (EAttribute) getFeature(modelManager, packageName, className, attributeName);
	}

	private EReference getReference(EdeltaModelManager modelManager, String packageName, String className, String attributeName) {
		return (EReference) getFeature(modelManager, packageName, className, attributeName);
	}

	private EStructuralFeature getFeature(EdeltaModelManager modelManager, String packageName, String className, String featureName) {
		return getEClass(modelManager, packageName, className)
				.getEStructuralFeature(featureName);
	}

	private EClass getEClass(EdeltaModelManager modelManager, String packageName, String className) {
		return (EClass) modelManager.getEPackage(
				packageName).getEClassifier(className);
	}

	private void assertGeneratedFiles(String message, String subdir, String outputDir, String fileName) {
		try {
			assertFilesAreEquals(
				message,
				EXPECTATIONS + subdir + fileName,
				outputDir + fileName);
		} catch (IOException e) {
			e.printStackTrace();
			fail(e.getClass().getName() + ": " + e.getMessage());
		}
	}

	/**
	 * This simulates what the final model migration should do.
	 * 
	 * IMPORTANT: the original Ecores and models must be in a subdirectory
	 * of the directory that stores the modified Ecores.
	 * 
	 * It is crucial to strip the original path and use the baseDir
	 * to create the new {@link Resource} URI, so that, upon saving,
	 * the schema location is computed correctly.
	 * 
	 * @param baseDir
	 */
	private void copyModels(EdeltaModelMigrator modelMigrator, String baseDir) {
		modelMigrator.copyModels(baseDir);
	}

	// SIMULATION OF REFACTORINGS THAT WILL BE PART OF OUR LIBRARY LATER

	/**
	 * Makes this feature multiple (upper = -1)
	 * 
	 * @param feature
	 */
	private static void makeMultiple(EdeltaModelMigrator modelMigrator, EStructuralFeature feature) {
		makeMultiple(modelMigrator, feature, -1);
	}

	/**
	 * Makes this feature multiple with a specific upper bound
	 * 
	 * @param feature
	 * @param upperBound
	 */
	private static void makeMultiple(EdeltaModelMigrator modelMigrator, EStructuralFeature feature, int upperBound) {
		feature.setUpperBound(upperBound);
		modelMigrator.copyRule(
			modelMigrator.isRelatedTo(feature),
			modelMigrator.multiplicityAwareCopy(feature)
		);
	}

	/**
	 * Makes this feature single (upper = 1)
	 * 
	 * @param feature
	 */
	private static void makeSingle(EdeltaModelMigrator modelMigrator, EStructuralFeature feature) {
		feature.setUpperBound(1);
		modelMigrator.copyRule(
			modelMigrator.isRelatedTo(feature),
			modelMigrator.multiplicityAwareCopy(feature)
		);
	}

	/**
	 * Changes the type of the attribute and when migrating the model
	 * it applies the passed lambda to transform the value or values
	 * (transparently).
	 * 
	 * @param modelMigrator
	 * @param attribute
	 * @param type
	 * @param singleValueTransformer
	 */
	private void changeAttributeType(EdeltaModelMigrator modelMigrator, EAttribute attribute,
			EDataType type, Function<Object, Object> singleValueTransformer) {
		attribute.setEType(type);
		modelMigrator.copyRule(
			a ->
				modelMigrator.isRelatedTo(a, attribute),
			(feature, oldObj, newObj) -> {
				// if we come here the old attribute was set
				EdeltaEcoreUtil.setValueForFeature(
					newObj,
					attribute,
					// use the upper bound of the destination attribute, since it might
					// be different from the original one
					EdeltaEcoreUtil.getValueForFeature(oldObj, feature, attribute.getUpperBound())
						.stream()
						.map(singleValueTransformer)
						.collect(Collectors.toList())
				);
			}
		);
	}

	/**
	 * Changes the type of the attribute and when migrating the model
	 * it applies the passed lambda to transform the value or values
	 * (transparently).
	 * 
	 * @param modelMigrator
	 * @param attribute
	 * @param type
	 * @param singleValueTransformer
	 */
	private void changeAttributeTypeAlternative(EdeltaModelMigrator modelMigrator, EAttribute attribute,
			EDataType type, Function<Object, Object> singleValueTransformer) {
		attribute.setEType(type);
		modelMigrator.transformAttributeValueRule(
			a ->
				modelMigrator.isRelatedTo(a, attribute),
			(feature, oldObj, oldValue) ->
				// if we come here the old attribute was set
				EdeltaEcoreUtil.unwrapCollection(
					// use the upper bound of the destination attribute, since it might
					// be different from the original one
					EdeltaEcoreUtil.wrapAsCollection(oldValue, attribute.getUpperBound())
						.stream()
						.map(singleValueTransformer)
						.collect(Collectors.toList()),
					attribute
				)
		);
	}

	private EAttribute replaceWithCopy(EdeltaModelMigrator modelMigrator, EAttribute attribute, String newName) {
		var copy = createCopy(modelMigrator, attribute);
		copy.setName(newName);
		var containingClass = attribute.getEContainingClass();
		EdeltaUtils.removeElement(attribute);
		containingClass.getEStructuralFeatures().add(copy);
		modelMigrator.featureMigratorRule(
			f -> // the feature must be originally associated with the
				// attribute we've just removed
				modelMigrator.wasRelatedTo(f, attribute),
			(feature, oldObj, newObj) -> copy);
		return copy;
	}

	private <T extends EObject> T createCopy(EdeltaModelMigrator modelMigrator, T o) {
		var copy = EcoreUtil.copy(o);
		return copy;
	}

	private <T extends EObject> T createSingleCopy(EdeltaModelMigrator modelMigrator, Collection<T> elements) {
		var iterator = elements.iterator();
		var copy = createCopy(modelMigrator, iterator.next());
		return copy;
	}

	private EStructuralFeature pullUp(EdeltaModelMigrator modelMigrator,
			EClass superClass, Collection<EStructuralFeature> features) {
		var pulledUp = createSingleCopy(modelMigrator, features);
		superClass.getEStructuralFeatures().add(pulledUp);
		EdeltaUtils.removeAllElements(features);
		// remember we must map the original metamodel element to the new one
		modelMigrator.featureMigratorRule(
			f -> // the feature of the original metamodel
				modelMigrator.wasRelatedToAtLeastOneOf(f, features),
			(feature, oldObj, newObj) -> { // the object of the original model
				// the result can be safely returned
				// independently from the object's class, since the
				// predicate already matched
				return pulledUp;
			}
		);
		return pulledUp;
	}

	private Collection<EStructuralFeature> pushDown(EdeltaModelMigrator modelMigrator,
			EStructuralFeature featureToPush, Collection<EClass> subClasses) {
		var pushedDownFeatures = new HashMap<EClass, EStructuralFeature>();
		for (var subClass : subClasses) {
			var pushedDown = createCopy(modelMigrator, featureToPush);
			pushedDownFeatures.put(subClass, pushedDown);
			// we add it in the very first position just to have exactly the
			// same Ecore model as the starting one of pullUpFeatures
			// but just for testing purposes: we verify that the output is
			// exactly the same as the original model of pullUpFeatures
			subClass.getEStructuralFeatures().add(0, pushedDown);
		}
		EdeltaUtils.removeElement(featureToPush);
		// remember we must compare to the original metamodel element
		modelMigrator.featureMigratorRule(
			modelMigrator.wasRelatedTo(featureToPush),
			(feature, oldObj, newObj) -> { // the object of the original model
				// the result depends on the EClass of the original
				// object being copied, but the map was built
				// using evolved classes
				return pushedDownFeatures.get(newObj.eClass());
			}
		);
		return pushedDownFeatures.values();
	}

	/**
	 * Replaces an EReference with an EClass (with the given name, the same package
	 * as the package of the reference's containing class), updating possible
	 * opposite reference, so that a relation can be extended with additional
	 * features. The original reference will be made a containment reference, (its
	 * other properties will not be changed) to the added EClass (and made
	 * bidirectional).
	 * 
	 * For example, given
	 * 
	 * <pre>
	 *    b2    b1
	 * A <-------> C
	 * </pre>
	 * 
	 * (where the opposite "b2" might not be present) if we pass "b1" and the name
	 * "B", then the result will be
	 * 
	 * <pre>
	 *    a     b1    b2    c
	 * A <-------> B <------> C
	 * </pre>
	 * 
	 * where "b1" will be a containment reference. Note the names inferred for the
	 * new additional opposite references.
	 * 
	 * @param name      the name for the extracted class
	 * @param reference the reference to turn into a reference to the extracted
	 *                  class
	 * @return the extracted class
	 */
	private EClass referenceToClass(EdeltaModelMigrator modelMigrator,
			EReference reference, String name) {
		// checkNotContainment reference:
		// "Cannot apply referenceToClass on containment reference"
		var ePackage = reference.getEContainingClass().getEPackage();
		var extracted = EdeltaUtils.newEClass(name);
		ePackage.getEClassifiers().add(extracted);
		var extractedRef = addMandatoryReference(extracted, 
			fromTypeToFeatureName(reference.getEType()),
			reference.getEReferenceType());
		final EReference eOpposite = reference.getEOpposite();
		if (eOpposite != null) {
			EdeltaUtils.makeBidirectional(eOpposite, extractedRef);
		}
		reference.setEType(extracted);
		makeContainmentBidirectional(reference);

		// handle the migration of the reference that now has to refer
		// to a new object (of the extracted class), or, transparently
		// to a list of new objects in case of a multi reference
		modelMigrator.copyRule(
			feature ->
				modelMigrator.isRelatedTo(feature, reference) || modelMigrator.isRelatedTo(feature, eOpposite),
			(feature, oldObj, newObj) -> {
				// feature: the feature of the original metamodel
				// oldObj: the object of the original model
				// newObj: the object of the new model, already created

				// the opposite reference now changed its type
				// so we have to skip the copy or we'll have a ClassCastException
				// the bidirectionality will be implied in the next migrator
				if (modelMigrator.isRelatedTo(feature, eOpposite))
					return;

				// retrieve the original value, wrapped in a list
				// so this works (transparently) for both single and multi feature
				// discard possible extra values, in case the multiplicity has changed
				var oldValueOrValues =
					EdeltaEcoreUtil
						.getValueForFeature(oldObj, feature,
								reference.getUpperBound());

				// for each old value create a new object for the
				// extracted class, by setting the reference's value
				// with the copied value of that reference
				var copies = oldValueOrValues.stream()
					.map(oldValue -> {
						// since this is NOT a containment reference
						// the referred oldValue has already been copied
						var copy = modelMigrator.getMigrated((EObject) oldValue);
						var created = EdeltaEcoreUtil.createInstance(extracted,
							o -> o.eSet(extractedRef, copy)
						);
						return created;
					})
					.collect(Collectors.toList());
				// in the new object set the value or values (transparently)
				// with the created object (or objects, again, transparently)
				EdeltaEcoreUtil.setValueForFeature(
					newObj, reference, copies);
			}
		);
		return extracted;
	}

	/**
	 * Alternative implementation that copies and removes the possible
	 * opposite reference (this way, we don't have to handle it in the
	 * migration rule). On the other hand, we have to handle copy,
	 * add and remove explicitly.
	 * 
	 * @param modelMigrator
	 * @param reference
	 * @param name
	 * @return
	 */
	private EClass referenceToClassAlternative(EdeltaModelMigrator modelMigrator,
			EReference reference, String name) {
		// checkNotContainment reference:
		// "Cannot apply referenceToClass on containment reference"
		var ePackage = reference.getEContainingClass().getEPackage();
		var extracted = EdeltaUtils.newEClass(name);
		ePackage.getEClassifiers().add(extracted);
		var extractedRef = addMandatoryReference(extracted, 
			fromTypeToFeatureName(reference.getEType()),
			reference.getEReferenceType());
		final EReference eOpposite = reference.getEOpposite();
		if (eOpposite != null) {
			var newOpposite = createCopy(modelMigrator, eOpposite);
			// put it in first position to have the same order as the original one
			eOpposite.getEContainingClass().getEStructuralFeatures().
				add(0, newOpposite);
			EdeltaUtils.makeBidirectional(newOpposite, extractedRef);
			EdeltaUtils.removeElement(eOpposite);
		}
		reference.setEType(extracted);
		makeContainmentBidirectional(reference);

		// handle the migration of the reference that now has to refer
		// to a new object (of the extracted class), or, transparently
		// to a list of new objects in case of a multi reference
		modelMigrator.copyRule(
			feature ->
				modelMigrator.isRelatedTo(feature, reference),
			(feature, oldObj, newObj) -> {
				// feature: the feature of the original metamodel
				// oldObj: the object of the original model
				// newObj: the object of the new model, already created

				// retrieve the original value, wrapped in a list
				// so this works (transparently) for both single and multi feature
				// discard possible extra values, in case the multiplicity has changed
				var oldValueOrValues =
					EdeltaEcoreUtil
						.getValueForFeature(oldObj, feature,
								reference.getUpperBound());

				// for each old value create a new object for the
				// extracted class, by setting the reference's value
				// with the copied value of that reference
				var copies = oldValueOrValues.stream()
					.map(oldValue -> {
						// since this is NOT a containment reference
						// the referred oldValue has already been copied
						var copy = modelMigrator.getMigrated((EObject) oldValue);
						return EdeltaEcoreUtil.createInstance(extracted,
							// the bidirectionality is implied
							o -> o.eSet(extractedRef, copy)
						);
					})
					.collect(Collectors.toList());
				// in the new object set the value or values (transparently)
				// with the created object (or objects, again, transparently)
				EdeltaEcoreUtil.setValueForFeature(
					newObj, reference, copies);
			}
		);
		return extracted;

	}

	private String fromTypeToFeatureName(final EClassifier type) {
		return StringExtensions.toFirstLower(type.getName());
	}

	/**
	 * SIMPLIFIED VERSION OF THE ACTUAL REFACTORING: directly pass the reference b1
	 * to B instead of passing B
	 * 
	 * Given an EClass, which is meant to represent a relation, removes such a
	 * class, transforming the relation into an EReference.
	 * 
	 * For example, given
	 * 
	 * <pre>
	 *    a     b1    b2    c
	 * A <-------> B <------> C
	 * </pre>
	 * 
	 * (where the opposites "a" and "b2" might not be present) if we pass "B", then
	 * the result will be
	 * 
	 * <pre>
	 *    b2    b1
	 * A <-------> C
	 * </pre>
	 * 
	 * @param cl
	 * @return the EReference that now represents the relation, that is, the
	 *         EReference originally of type cl ("b1" above)
	 */
	private EReference classToReference(EdeltaModelMigrator modelMigrator,
			final EReference reference) {
		// "B" above
		final EClass toRemove = reference.getEReferenceType();
		// "A" above
		final EClass owner = reference.getEContainingClass();
		// search for a single EReference ("c" above) in cl that has not type owner
		// (the one with type owner, if exists, would be the EOpposite
		// of reference, which we are not interested in, "a" above).
		final EReference referenceToTarget =
				findSingleReferenceNotOfType(toRemove, owner);
		reference.setEType(referenceToTarget.getEType());
		EdeltaUtils.dropContainment(reference);
		final EReference opposite = referenceToTarget.getEOpposite();
		if (opposite != null) {
			EdeltaUtils.makeBidirectional(reference, opposite);
		}
		EdeltaUtils.removeElement(toRemove);
		modelMigrator.copyRule(
			f ->
				modelMigrator.isRelatedTo(f, reference),
			(feature, oldObj, newObj) -> {
				// feature: the feature of the original metamodel
				// oldObj: the object of the original model
				// newObj: the object of the new model, already created
	
				// retrieve the original value, wrapped in a list
				// so this works (transparently) for both single and multi feature
				// discard possible extra values, in case the multiplicity has changed
				var oldValueOrValues =
					EdeltaEcoreUtil
						.getValueForFeature(oldObj, feature,
								reference.getUpperBound());
	
				var copyOfOldReferred = oldValueOrValues.stream()
					.map(value -> {
						// the object of the class to remove
						var objOfRemovedClass = (EObject) value;
						var eClass = objOfRemovedClass.eClass();
						// the reference not of type of the containing class of the feature
						var refToTarget =
							findSingleReferenceNotOfType(eClass, oldObj.eClass());
						// the original referred object in the object to remove
						var oldReferred =
							(EObject) objOfRemovedClass.eGet(refToTarget);
						// create the copy (our modelMigrator.copy checks whether
						// an object has already been copied, so we avoid to copy
						// the same object twice). We don't even have to care whether
						// this will be part of a resource, since, as a contained
						// object, it will be possibly copied later
						return modelMigrator.getMigrated(oldReferred);
					})
					.collect(Collectors.toList());
	
				// in the new object set the value or values (transparently)
				// with the created object (or objects, again, transparently)
				EdeltaEcoreUtil.setValueForFeature(
					newObj, reference, copyOfOldReferred);
			}
		);
		return reference;
	}

	/**
	 * Makes the EReference, which is assumed to be already part of an EClass, a
	 * single required containment reference, adds to the referred type, which is
	 * assumed to be set, an opposite required single reference.
	 * 
	 * @param reference
	 */
	private EReference makeContainmentBidirectional(final EReference reference) {
		EdeltaUtils.makeContainment(reference);
		final EClass owner = reference.getEContainingClass();
		final EClass referredType = reference.getEReferenceType();
		EReference addedMandatoryReference = this.addMandatoryReference(referredType,
				this.fromTypeToFeatureName(owner), owner);
		EdeltaUtils.makeBidirectional(addedMandatoryReference, reference);
		return addedMandatoryReference;
	}

	private EReference addMandatoryReference(final EClass eClass, final String referenceName, final EClass type) {
		var reference = EdeltaUtils.newEReference(referenceName, type, it -> 
			EdeltaUtils.makeSingleRequired(it)
		);
		eClass.getEStructuralFeatures().add(reference);
		return reference;
	}

	/**
	 * SIMPLIFIED VERSION WITHOUT ERROR CHECKING
	 * 
	 * Finds the single EReference, in the EReferences of the given EClass, with a
	 * type different from the given type, performing validation (that is, no
	 * reference is found, or more than one) checks and in case show errors and
	 * throws an IllegalArgumentException
	 * 
	 * @param cl
	 * @param target
	 */
	private EReference findSingleReferenceNotOfType(final EClass cl, final EClass type) {
		return cl.getEReferences().stream()
				.filter(r -> r.getEType() != type)
				.findFirst()
				.orElse(null);
	}

	/**
	 * Merges the given features into a single new feature in the containing class.
	 * The features must be compatible (same containing class, same type, same
	 * cardinality, etc).
	 * @param newFeatureName
	 * @param features
	 * @param valueMerger if not null, it is used to merge the values of the original
	 * features in the new model
	 * @param postCopy TODO
	 * 
	 * @return the new feature added to the containing class of the features
	 */
	private EStructuralFeature mergeFeatures(EdeltaModelMigrator modelMigrator,
			final String newFeatureName,
			final Collection<EStructuralFeature> features,
			Function<Collection<?>, Object> valueMerger, Runnable postCopy) {
		// THIS SHOULD BE CHECKED IN THE FINAL IMPLEMENTATION (ALREADY DONE IN refactorings.lib)
//		this.checkNoDifferences(features, new EdeltaFeatureDifferenceFinder().ignoringName(),
//				"The two features cannot be merged");
		// ALSO MAKE SURE IT'S A SINGLE FEATURE, NOT MULTI (TO BE DONE ALSO IN refactorings.lib)
		// ALSO MAKE SURE IT'S NOT BIDIRECTIONAL (TO BE DONE ALSO IN refactorings.lib)
		var firstFeature = features.iterator().next();
		final EClass owner = firstFeature.getEContainingClass();
		final EStructuralFeature mergedFeature = createCopy(modelMigrator, firstFeature);
		mergedFeature.setName(newFeatureName);
		owner.getEStructuralFeatures().add(mergedFeature);
		EdeltaUtils.removeAllElements(features);
		if (valueMerger != null) {
			if (firstFeature instanceof EReference) {
				modelMigrator.copyRule(
					modelMigrator.wasRelatedTo(firstFeature),
					(feature, oldObj, newObj) -> {
						var originalFeatures = features.stream()
								.map(modelMigrator::getOriginal);
						// for references we must get the copied EObject
						var oldValues = originalFeatures
								.map(f -> oldObj.eGet(f))
								.collect(Collectors.toList());
						var merged = valueMerger.apply(
							modelMigrator.getMigrated(oldValues));
						newObj.eSet(mergedFeature, merged);
					},
					postCopy
				);
			} else {
				modelMigrator.copyRule(
					modelMigrator.wasRelatedTo(firstFeature),
					(feature, oldObj, newObj) -> {
						var originalFeatures = features.stream()
								.map(modelMigrator::getOriginal);
						var oldValues = originalFeatures
								.map(f -> oldObj.eGet(f))
								.collect(Collectors.toList());
						var merged = valueMerger.apply(oldValues);
						newObj.eSet(mergedFeature, merged);
					},
					postCopy
				);
			}
		}
		return mergedFeature;
	}

	private Collection<EStructuralFeature> splitFeature(EdeltaModelMigrator modelMigrator,
			final EStructuralFeature featureToSplit,
			final Collection<String> newFeatureNames,
			Function<Object, Collection<?>> valueSplitter, Runnable postCopy) {
		// THIS SHOULD BE CHECKED IN THE FINAL IMPLEMENTATION
		// ALSO MAKE SURE IT'S A SINGLE FEATURE, NOT MULTI (TO BE DONE ALSO IN refactorings.lib)
		// ALSO MAKE SURE IT'S NOT BIDIRECTIONAL (TO BE DONE ALSO IN refactorings.lib)
		var splitFeatures = newFeatureNames.stream()
			.map(newName -> {
				var newFeature = createCopy(modelMigrator, featureToSplit);
				newFeature.setName(newName);
				return newFeature;
			})
			.collect(Collectors.toList());
		featureToSplit.getEContainingClass()
			.getEStructuralFeatures().addAll(splitFeatures);
		EdeltaUtils.removeElement(featureToSplit);
		if (valueSplitter != null) {
			if (featureToSplit instanceof EReference)
				modelMigrator.copyRule(
					modelMigrator.wasRelatedTo(featureToSplit),
					(feature, oldObj, newObj) -> {
						// for references we must get the copied EObject
						var oldValue = modelMigrator.getMigrated((EObject) oldObj.eGet(feature));
						var splittedValues = valueSplitter.apply(oldValue).iterator();
						for (var splitFeature : splitFeatures) {
							if (!splittedValues.hasNext())
								break;
							newObj.eSet(splitFeature, splittedValues.next());
						}
					},
					postCopy
				);
			else
				modelMigrator.copyRule(
					modelMigrator.wasRelatedTo(featureToSplit),
					(feature, oldObj, newObj) -> {
						var oldValue = oldObj.eGet(feature);
						var splittedValues = valueSplitter.apply(oldValue).iterator();
						for (var splitFeature : splitFeatures) {
							if (!splittedValues.hasNext())
								break;
							newObj.eSet(splitFeature, splittedValues.next());
						}
					},
					postCopy
				);
		}
		return splitFeatures;
	}

	/**
	 * Given an EAttribute, expected to have an EEnum type, creates a subclass of
	 * the containing class for each value of the referred EEnum (each subclass is
	 * given a name corresponding to the the EEnumLiteral, all lowercase but the
	 * first letter, for example, given the literal "LITERAL1", the subclass is
	 * given the name "Literal1"). The attribute will then be removed and so will
	 * the EEnum. The original containing EClass is made abstract.
	 * 
	 * @param attr
	 * @return the collection of created subclasses
	 */
	private Collection<EClass> enumToSubclasses(EdeltaModelMigrator modelMigrator, EAttribute attr) {
		// CHECK THAT IT'S AN ENUM (already done in refactorings.lib)
		var type = (EEnum) attr.getEAttributeType();
		// map the literal string to the corresponding created Subclass
		Map<String, EClass> createdSubclasses = new HashMap<>();
		var owner = attr.getEContainingClass();
		EdeltaUtils.makeAbstract(owner);
		var containingPackage = owner.getEPackage();
		var literals = type.getELiterals();
		for (final EEnumLiteral literal : literals) {
			// ensureEClassifierNameIsUnique (already done in refactorings.lib)
			var literalString = literal.getLiteral();
			var subclassName = 
				StringExtensions.toFirstUpper(literalString.toLowerCase());
			createdSubclasses.put(literalString, EdeltaUtils.newEClass(subclassName,
				c -> {
					c.getESuperTypes().add(owner);
					containingPackage.getEClassifiers().add(c);
				}
			));
		}
		// will also implicitly remove the attribute of this type
		EdeltaUtils.removeElement(type);
		modelMigrator.createInstanceRule(
			modelMigrator.isRelatedTo(owner),
			oldObj -> {
				var literalValue =
					oldObj.eGet(modelMigrator.getOriginal(attr)).toString();
				var correspondingSubclass =
					createdSubclasses.get(literalValue);
				return EcoreUtil.create(correspondingSubclass);
			}
		);
		return createdSubclasses.values();
	}

	/**
	 * Given a collection of subclasses, which are expected to be direct subclasses
	 * of an EClass, say superclass, generates an EEnum (in the superclass' package)
	 * with the specified name, representing the inheritance relation, with an
	 * EEnumLiteral for each subclass (the name is the name of the subclass in
	 * uppercase); the subclasses are removed, and an attributed is added to the
	 * superclass with the created EEnum as type (the name is the name of the EEnum,
	 * first letter lowercase).
	 * 
	 * For example, given the name "BaseType" and the collection of classes
	 * {"Derived1", "Derived2"} subclasses of the superclass "Base", it creates the
	 * EEnum "BaseType" with literals "DERIVED1", "DERIVED2", (the values will be
	 * incremental numbers starting from 0, according to the order of the subclasses
	 * in the collection) it adds to "Base" the EAttribute "baseType" of type
	 * "BaseType". The EClasses "Derived1" and "Derived2" are removed from the
	 * package.
	 * 
	 * @param name       the name for the created EEnum
	 * @param subclasses
	 * @return the created EAttribute
	 */
	private EAttribute subclassesToEnum(EdeltaModelMigrator modelMigrator,
			String name, final Collection<EClass> subclasses) {
		// TO BE DONE (already implemented in refactorings.lib) this.checkNoFeatures(subclasses);
		// ORIGINAL : final EClass superclass = this.getSingleDirectSuperclass(subclasses);
		// SIMPLIFIED HERE: TAKE THE FIRST ONE
		var superclass = subclasses.iterator().next().getESuperTypes().get(0);
		var containingPackage = superclass.getEPackage();
		// map the subclass name to the EEnumLiteral
		Map<String, EEnumLiteral> createdLiterals = new HashMap<>();
		var createdEnum = EdeltaUtils.newEEnum(name, e -> {
			var i = 0;
			for (var subclass : subclasses) {
				// WAS val enumLiteralName = ensureEClassifierNameIsUnique
				// (superclass, subClass.name.toUpperCase)
				var literalName = subclass.getName().toUpperCase();
				var val = i++;
				e.getELiterals().add(
					EdeltaUtils.newEEnumLiteral(literalName,
						l -> {
							l.setValue(val);
							createdLiterals.put(subclass.getName(), l);
						}
					)
				);
			}
			containingPackage.getEClassifiers().add(e);
		});
		var attribute = EdeltaUtils.newEAttribute(
			fromTypeToFeatureName(createdEnum), createdEnum);
		superclass.getEStructuralFeatures().add(attribute);
		EdeltaUtils.makeConcrete(superclass);
		EdeltaUtils.removeAllElements(subclasses);
		modelMigrator.createInstanceRule(
			modelMigrator.wasRelatedToAtLeastOneOf(subclasses),
			oldObj -> {
				var literal =
					createdLiterals.get(oldObj.eClass().getName());
				return EdeltaEcoreUtil.createInstance(
					superclass,
					newObj ->
						newObj.eSet(attribute, literal)
				);
			}
		);
		return attribute;
	}
}
