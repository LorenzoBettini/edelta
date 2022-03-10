package edelta.refactorings.lib.tests;

import static edelta.lib.EdeltaUtils.getEObjectRepr;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.xtext.xbase.lib.IterableExtensions.head;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.assertj.core.api.Assertions;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import edelta.lib.EdeltaDefaultRuntime;
import edelta.lib.EdeltaModelManager;
import edelta.refactorings.lib.EdeltaRefactorings;
import edelta.refactorings.lib.tests.utils.InMemoryLoggerAppender;
import edelta.testutils.EdeltaTestUtils;

class EdeltaRefactoringsTest extends AbstractTest {
	private EdeltaRefactorings refactorings;

	private InMemoryLoggerAppender appender;

	private String testModelDirectory;

	private List<String> testModelFiles;

	private EdeltaModelManager modelManager;

	@BeforeEach
	void setup() throws Exception {
		modelManager = new EdeltaModelManager();
		refactorings = new EdeltaRefactorings(new EdeltaDefaultRuntime(modelManager));
		appender = new InMemoryLoggerAppender();
		appender.setLineSeparator("\n");
		refactorings.getLogger().addAppender(appender);
		refactorings.performSanityChecks();
	}

	/**
	 * This should be commented out when we want to copy the generated modified
	 * Ecore into the test-output-expectations directory, typically, the first time
	 * we write a new test.
	 * 
	 * We need to clean the modified directory when tests are stable, so that
	 * modified Ecore with validation errors do not fill the project with error
	 * markers.
	 * 
	 * @throws IOException
	 */
	@AfterEach
	void cleanModifiedOutputDirectory() throws IOException {
		EdeltaTestUtils.cleanDirectory(AbstractTest.MODIFIED);
	}

	private void withInputModels(String testModelDirectory, String... testModelFiles) {
		this.testModelDirectory = testModelDirectory;
		this.testModelFiles = Arrays.asList(testModelFiles);
	}

	private void loadModelFiles() {
		checkInputModelSettings();
		for (String testModelFile : testModelFiles) {
			modelManager
				.loadEcoreFile(AbstractTest.TESTECORES +
					testModelDirectory + "/" + testModelFile);
		}
	}

	private void assertModifiedFiles() throws IOException {
		checkInputModelSettings();
		// no need to explicitly validate:
		// if the ecore file is saved then it is valid
		for (String testModelFile : testModelFiles) {
			EdeltaTestUtils.assertFilesAreEquals(
					AbstractTest.EXPECTATIONS + testModelDirectory + "/" + testModelFile,
					AbstractTest.MODIFIED + testModelFile);
		}
		assertLogIsEmpty();
	}

	private void assertLogIsEmpty() {
		assertThat(appender.getResult()).isEmpty();
	}

	private void assertModifiedFilesAreSameAsOriginal() throws IOException {
		checkInputModelSettings();
		for (String testModelFile : testModelFiles) {
			EdeltaTestUtils.assertFilesAreEquals(
				AbstractTest.TESTECORES +
				testModelDirectory +
				"/" +
				testModelFile,
				AbstractTest.MODIFIED + testModelFile);
		}
	}

	private void assertOppositeRefactorings(final Runnable first, final Runnable second) throws IOException {
		loadModelFiles();
		first.run();
		modelManager.saveEcores(AbstractTest.MODIFIED);
		second.run();
		modelManager.saveEcores(AbstractTest.MODIFIED);
		assertModifiedFilesAreSameAsOriginal();
	}

	private void checkInputModelSettings() {
		assertThat(this.testModelDirectory).isNotNull();
		assertThat(this.testModelFiles).isNotNull();
	}

	@Test
	void test_addMandatoryAttribute() {
		final EClass c = this.createEClassWithoutPackage("C1");
		refactorings.addMandatoryAttribute(c, "test", this.stringDataType);
		final EAttribute attr = head(c.getEAttributes());
		Assertions.<EAttribute>assertThat(attr)
			.returns("test", EAttribute::getName)
			.returns(this.stringDataType, EAttribute::getEAttributeType)
			.returns(true, EAttribute::isRequired);
	}

	@Test
	void test_addMandatoryReference() {
		final EClass c = this.createEClassWithoutPackage("C1");
		refactorings.addMandatoryReference(c, "test", this.eClassReference);
		final EReference attr = head(c.getEReferences());
		Assertions.<EReference>assertThat(attr)
			.returns("test", EReference::getName)
			.returns(this.eClassReference, EReference::getEReferenceType)
			.returns(true, EReference::isRequired);
	}

	@Test
	void test_mergeFeatures() throws IOException {
		withInputModels("mergeFeatures", "PersonList.ecore");
		loadModelFiles();
		final EClass person = refactorings.getEClass("PersonList", "Person");
		refactorings.mergeFeatures("name",
			asList(
				person.getEStructuralFeature("firstName"),
				person.getEStructuralFeature("lastName")));
		modelManager.saveEcores(AbstractTest.MODIFIED);
		assertModifiedFiles();
	}

	@Test
	void test_mergeFeaturesDifferent() throws IOException {
		withInputModels("mergeFeaturesDifferent", "PersonList.ecore");
		loadModelFiles();
		final EClass person = refactorings.getEClass("PersonList", "Person");
		assertThrowsIAE(() -> refactorings.mergeFeatures("name",
			asList(
				person.getEStructuralFeature("firstName"),
				person.getEStructuralFeature("lastName"))));
		modelManager.saveEcores(AbstractTest.MODIFIED);
		assertModifiedFilesAreSameAsOriginal();
		final EClass student = refactorings.getEClass("PersonList", "Student");
		assertThrowsIAE(() -> refactorings.mergeFeatures("name",
			asList(
				person.getEStructuralFeature("lastName"),
				student.getEStructuralFeature("lastName"))));
		modelManager.saveEcores(AbstractTest.MODIFIED);
		assertModifiedFilesAreSameAsOriginal();
		assertThrowsIAE(() -> refactorings.mergeFeatures("name",
			asList(
				person.getEStructuralFeature("list"),
				person.getEStructuralFeature("lastName"))));
		modelManager.saveEcores(AbstractTest.MODIFIED);
		assertModifiedFilesAreSameAsOriginal();
		assertThat(appender.getResult())
			.isEqualTo(
			"ERROR: PersonList.Person.lastName: The two features cannot be merged:\n"
			+ "ecore.ETypedElement.lowerBound:\n"
			+ "  PersonList.Person.firstName: 0\n"
			+ "  PersonList.Person.lastName: 1\n"
			+ "\n"
			+ "ERROR: PersonList.Student.lastName: The two features cannot be merged:\n"
			+ "ecore.ENamedElement.name:\n"
			+ "  PersonList.Person: Person\n"
			+ "  PersonList.Student: Student\n"
			+ "ecore.EStructuralFeature.eContainingClass:\n"
			+ "  PersonList.Person.lastName: PersonList.Person\n"
			+ "  PersonList.Student.lastName: PersonList.Student\n"
			+ "\n"
			+ "ERROR: PersonList.Person.lastName: The two features cannot be merged:\n"
			+ "different kinds:\n"
			+ "  PersonList.Person.list: ecore.EReference\n"
			+ "  PersonList.Person.lastName: ecore.EAttribute\n"
			+ "\n"
			+ "");
	}

	@Test
	void test_mergeFeatures2() throws IOException {
		withInputModels("mergeFeatures2", "PersonList.ecore");
		loadModelFiles();
		final EClass list = refactorings.getEClass("PersonList", "List");
		final EClass person = refactorings.getEClass("PersonList", "Person");
		refactorings.mergeFeatures(list.getEStructuralFeature("places"),
			asList(
				list.getEStructuralFeature("wplaces"),
				list.getEStructuralFeature("lplaces")));
		refactorings.mergeFeatures(person.getEStructuralFeature("name"),
			asList(
				person.getEStructuralFeature("firstName"),
				person.getEStructuralFeature("lastName")));
		modelManager.saveEcores(AbstractTest.MODIFIED);
		assertModifiedFiles();
	}

	@Test
	void test_mergeFeatures2NonCompliant() {
		withInputModels("mergeFeatures2", "PersonList.ecore");
		loadModelFiles();
		final EClass list = refactorings.getEClass("PersonList", "List");
		final EClass person = refactorings.getEClass("PersonList", "Person");
		final EStructuralFeature wplaces = list.getEStructuralFeature("wplaces");
		// Place is not subtype of WorkPlace
		assertThrowsIAE(() -> refactorings.mergeFeatures(wplaces,
			asList(
				list.getEStructuralFeature("places"),
				list.getEStructuralFeature("lplaces"))));
		// different lowerbound
		wplaces.setLowerBound(1);
		assertThrowsIAE(() -> refactorings.mergeFeatures(list.getEStructuralFeature("places"),
			asList(
				list.getEStructuralFeature("wplaces"),
				list.getEStructuralFeature("lplaces"))));
		// merge attributes with reference
		assertThrowsIAE(() -> refactorings.mergeFeatures(list.getEStructuralFeature("places"),
			asList(
				person.getEStructuralFeature("firstName"),
				person.getEStructuralFeature("lastName"))));
		// merge attributes with different types
		assertThrowsIAE(() -> refactorings.mergeFeatures(person.getEStructuralFeature("age"),
			asList(
				person.getEStructuralFeature("firstName"),
				person.getEStructuralFeature("lastName"))));
		assertThat(appender.getResult())
			.isEqualTo(
			"ERROR: PersonList.List.wplaces: features not compliant with type PersonList.WorkPlace:\n"
			+ "  PersonList.List.places: PersonList.Place\n"
			+ "  PersonList.List.lplaces: PersonList.LivingPlace\n"
			+ "ERROR: PersonList.List.wplaces: The two features cannot be merged:\n"
			+ "ecore.ETypedElement.lowerBound:\n"
			+ "  PersonList.List.places: 0\n"
			+ "  PersonList.List.wplaces: 1\n"
			+ "\n"
			+ "ERROR: PersonList.List.places: features not compliant with type PersonList.Place:\n"
			+ "  PersonList.Person.firstName: ecore.EString\n"
			+ "  PersonList.Person.lastName: ecore.EString\n"
			+ "ERROR: PersonList.Person.age: features not compliant with type ecore.EInt:\n"
			+ "  PersonList.Person.firstName: ecore.EString\n"
			+ "  PersonList.Person.lastName: ecore.EString\n"
			+ "");
	}

	@Test
	void test_mergeFeatures3() throws IOException {
		withInputModels("mergeFeatures3", "PersonList.ecore");
		loadModelFiles();
		final EClass list = refactorings.getEClass("PersonList", "List");
		final EClass place = refactorings.getEClass("PersonList", "Place");
		final EClass person = refactorings.getEClass("PersonList", "Person");
		refactorings.mergeFeatures("places", place, Collections.<EStructuralFeature>unmodifiableList(
			asList(
				list.getEStructuralFeature("wplaces"),
				list.getEStructuralFeature("lplaces"))));
		refactorings.mergeFeatures("name", this.stringDataType,
			asList(
				person.getEStructuralFeature("firstName"),
				person.getEStructuralFeature("lastName")));
		modelManager.saveEcores(AbstractTest.MODIFIED);
		assertModifiedFiles();
	}

	@Test
	void test_enumToSubclasses() throws IOException {
		withInputModels("enumToSubclasses", "PersonList.ecore");
		loadModelFiles();
		final EClass person = refactorings.getEClass("PersonList", "Person");
		EStructuralFeature _eStructuralFeature = person.getEStructuralFeature("gender");
		final Collection<EClass> result = refactorings.enumToSubclasses(((EAttribute) _eStructuralFeature));
		modelManager.saveEcores(AbstractTest.MODIFIED);
		assertModifiedFiles();
		assertThat(result)
			.extracting(EClass::getName)
			.containsExactlyInAnyOrder("Male", "Female");
	}

	@Test
	void test_enumToSubclassesNotAnEEnum() throws IOException {
		withInputModels("enumToSubclasses", "PersonList.ecore");
		loadModelFiles();
		final EClass person = refactorings.getEClass("PersonList", "Person");
		final Collection<EClass> result = 
			refactorings.enumToSubclasses(
				((EAttribute) person.getEStructuralFeature("firstname")));
		assertThat(result).isNull();
		modelManager.saveEcores(AbstractTest.MODIFIED);
		assertModifiedFilesAreSameAsOriginal();
		assertThat(appender.getResult().trim())
			.isEqualTo("ERROR: PersonList.Person.firstname: Not an EEnum: ecore.EString");
	}

	@Test
	void test_subclassesToEnum() throws IOException {
		withInputModels("subclassesToEnum", "PersonList.ecore");
		loadModelFiles();
		final EPackage personList = refactorings.getEPackage("PersonList");
		final EAttribute result = refactorings.subclassesToEnum("Gender",
			asList(
				(EClass) personList.getEClassifier("Male"),
				(EClass) personList.getEClassifier("Female")));
		modelManager.saveEcores(AbstractTest.MODIFIED);
		assertModifiedFiles();
		assertThat(result)
			.returns("gender", EAttribute::getName);
	}

	@Test
	void test_subclassesToEnumSubclassesNotEmpty() throws IOException {
		withInputModels("subclassesToEnumSubclassesNotEmpty", "PersonList.ecore");
		loadModelFiles();
		final EPackage personList = refactorings.getEPackage("PersonList");
		assertThrowsIAE(() -> refactorings.subclassesToEnum("Gender",
			asList(
				(EClass) personList.getEClassifier("Male"),
				(EClass) personList.getEClassifier("NotSpecified"),
				(EClass) personList.getEClassifier("Female"))));
		modelManager.saveEcores(AbstractTest.MODIFIED);
		assertModifiedFilesAreSameAsOriginal();
		assertThat(appender.getResult().trim())
			.isEqualTo(
			"ERROR: PersonList.Male: Not an empty class: PersonList.Male:\n"
			+ "  PersonList.Male.maleName\n"
			+ "ERROR: PersonList.Female: Not an empty class: PersonList.Female:\n"
			+ "  PersonList.Female.femaleName");
	}

	@Test
	void test_subclassesToEnumSubclassesWrongSubclasses() throws IOException {
		withInputModels("subclassesToEnumSubclassesWrongSubclasses", "PersonList.ecore");
		loadModelFiles();
		var personList = refactorings.getEPackage("PersonList");
		var female = (EClass) personList.getEClassifier("Female");
		var anotherFemale = (EClass) personList.getEClassifier("AnotherFemale");
		var male = (EClass) personList.getEClassifier("Male");
		var femaleEmployee = (EClass) personList.getEClassifier("FemaleEmployee");
		var employee = (EClass) personList.getEClassifier("Employee");
		assertThrowsIAE(() -> refactorings.subclassesToEnum("Gender",
				asList(male, female, femaleEmployee, employee)));
		assertThrowsIAE(() -> refactorings.subclassesToEnum("Gender",
				asList(female, anotherFemale)));
		assertThrowsIAE(() -> refactorings.subclassesToEnum("Gender",
				asList(female)));
		modelManager.saveEcores(AbstractTest.MODIFIED);
		assertModifiedFilesAreSameAsOriginal();
		assertThat(appender.getResult().trim())
			.isEqualTo(
			"ERROR: PersonList.FemaleEmployee: Expected one superclass: PersonList.FemaleEmployee instead of:\n"
			+ "  PersonList.Person\n"
			+ "  PersonList.Employee\n"
			+ "ERROR: PersonList.Employee: Expected one superclass: PersonList.Employee instead of:\n"
			+ "  empty\n"
			+ "ERROR: PersonList.AnotherFemale: Wrong superclass of PersonList.AnotherFemale:\n"
			+ "  Expected: PersonList.Person\n"
			+ "  Actual  : PersonList.AnotherPerson\n"
			+ "ERROR: PersonList.Person: The class has additional subclasses:\n"
			+ "  PersonList.Male\n"
			+ "  PersonList.FemaleEmployee");
	}

	@Test
	void test_subclassesToEnumSubclassesWrongSubclassesParallel() throws IOException {
		withInputModels("subclassesToEnumSubclassesWrongSubclassesParallel",
				"PersonList.ecore", "PersonListReferring.ecore");
		loadModelFiles();
		var personList = refactorings.getEPackage("PersonList");
		var female = (EClass) personList.getEClassifier("Female");
		assertThrowsIAE(() -> refactorings.subclassesToEnum("Gender",
				asList(female)));
		modelManager.saveEcores(AbstractTest.MODIFIED);
		assertModifiedFilesAreSameAsOriginal();
		assertThat(appender.getResult().trim())
			.isEqualTo(
			"ERROR: PersonList.Person: The class has additional subclasses:\n"
			+ "  PersonList.Male\n"
			+ "  PersonListReferring.FemaleEmployee");
	}

	@Test
	void test_enumToSubclasses_IsOppositeOf_subclassesToEnum() throws IOException {
		withInputModels("enumToSubclasses", "PersonList.ecore");
		assertOppositeRefactorings(
			() -> refactorings.enumToSubclasses(
					refactorings.getEAttribute("PersonList", "Person", "gender")),
			() -> refactorings.subclassesToEnum("Gender",
				asList(
					refactorings.getEClass("PersonList", "Male"),
					refactorings.getEClass("PersonList", "Female"))));
		assertLogIsEmpty();
	}

	@Test
	void test_subclassesToEnum_IsOppositeOf_enumToSubclasses() throws IOException {
		withInputModels("subclassesToEnum", "PersonList.ecore");
		assertOppositeRefactorings(
			() -> refactorings.subclassesToEnum("Gender",
				asList(
					refactorings.getEClass("PersonList", "Male"),
					refactorings.getEClass("PersonList", "Female"))),
			() -> refactorings.enumToSubclasses(
					refactorings.getEAttribute("PersonList", "Person", "gender")));
		assertLogIsEmpty();
	}

	@Test
	void test_extractClassWithAttributes() throws IOException {
		withInputModels("extractClassWithAttributes", "PersonList.ecore");
		loadModelFiles();
		refactorings.extractClass("Address",
			asList(
				refactorings.getEAttribute("PersonList", "Person", "street"),
				refactorings.getEAttribute("PersonList", "Person", "houseNumber"))
			);
		modelManager.saveEcores(AbstractTest.MODIFIED);
		assertModifiedFiles();
	}

	@Test
	void test_extractClassWithAttributesContainedInDifferentClasses() throws IOException {
		withInputModels("extractClassWithAttributesContainedInDifferentClasses", "PersonList.ecore");
		loadModelFiles();
		var thrown = assertThrowsIAE(() -> refactorings.extractClass("Address",
			asList(
				refactorings.getEAttribute("PersonList", "Person", "street"),
				refactorings.getEAttribute("PersonList", "Person2", "street"))
			));
		assertThat(thrown.getMessage())
			.isEqualTo(
			"Multiple containing classes:\n"
			+ "  PersonList.Person:\n"
			+ "    PersonList.Person.street\n"
			+ "  PersonList.Person2:\n"
			+ "    PersonList.Person2.street");
		modelManager.saveEcores(AbstractTest.MODIFIED);
		assertModifiedFilesAreSameAsOriginal();
		assertThat(appender.getResult().trim())
			.isEqualTo(
			"ERROR: PersonList.Person: Extracted features must belong to the same class: PersonList.Person\n"
			+ "ERROR: PersonList.Person2: Extracted features must belong to the same class: PersonList.Person2");
	}

	@Test
	void test_extractClassWithAttributesEmpty() {
		var result = refactorings.extractClass("Address", emptyList());
		assertThat(result).isNull();
	}

	@Test
	void test_extractClassWithReferences() throws IOException {
		withInputModels("extractClassWithReferences", "PersonList.ecore");
		loadModelFiles();
		refactorings.extractClass("WorkAddress",
			asList(
				refactorings.getEAttribute("PersonList", "Person", "street"),
				refactorings.getEReference("PersonList", "Person", "workplace"),
				refactorings.getEAttribute("PersonList", "Person", "houseNumber"))
			);
		modelManager.saveEcores(AbstractTest.MODIFIED);
		assertModifiedFiles();
	}

	@Test
	void test_extractClassWithReferencesBidirectional() throws IOException {
		withInputModels("extractClassWithReferencesBidirectional", "PersonList.ecore");
		loadModelFiles();
		assertThrowsIAE(() -> refactorings.extractClass("WorkAddress",
			asList(
				refactorings.getEAttribute("PersonList", "Person", "street"),
				refactorings.getEReference("PersonList", "Person", "workplace"),
				refactorings.getEAttribute("PersonList", "Person", "houseNumber"))
			));
		assertThat(appender.getResult().trim())
			.isEqualTo(
			"ERROR: PersonList.Person.workplace: Cannot extract bidirectional references:\n"
			+ "  PersonList.Person.workplace");
	}

	@Test
	void test_inlineClassWithAttributes() throws IOException {
		withInputModels("inlineClassWithAttributes", "PersonList.ecore");
		loadModelFiles();
		refactorings.inlineClass(refactorings.getEClass("PersonList", "Address"));
		modelManager.saveEcores(AbstractTest.MODIFIED);
		assertModifiedFiles();
	}

	@Test
	void test_inlineClassPrefix() throws IOException {
		withInputModels("inlineClassPrefix", "PersonList.ecore");
		loadModelFiles();
		refactorings.inlineClass(
			refactorings.getEClass("PersonList", "Address"),
			"address_");
		modelManager.saveEcores(AbstractTest.MODIFIED);
		assertModifiedFiles();
	}

	@Test
	void test_inlineClassWithReferences() throws IOException {
		withInputModels("inlineClassWithReferences", "PersonList.ecore");
		loadModelFiles();
		refactorings.inlineClass(refactorings.getEClass("PersonList", "WorkAddress"));
		modelManager.saveEcores(AbstractTest.MODIFIED);
		assertModifiedFiles();
	}

	@Test
	void test_inlineClassWithAttributesWronglyUsedByOthersParallel() throws IOException {
		withInputModels("inlineClassWithAttributesWronglyUsedByOthersParallel",
				"PersonList.ecore", "PersonListReferring.ecore");
		loadModelFiles();
		assertThrowsIAE(() ->
			refactorings.inlineClass(refactorings.getEClass("PersonList", "Address")));
		modelManager.saveEcores(AbstractTest.MODIFIED);
		assertModifiedFilesAreSameAsOriginal();
		assertThat(appender.getResult().trim())
			.isEqualTo(
			"ERROR: PersonList.Address: The EClass is used by more than one element:\n"
			+ "  PersonList.Person.address\n"
			+ "    ecore.ETypedElement.eType\n"
			+ "  PersonListReferring.UsesAddress.address\n"
			+ "    ecore.ETypedElement.eType\n"
			+ "  PersonListReferring.ExtendsAddress\n"
			+ "    ecore.EClass.eSuperTypes");
	}

	@Test
	void test_inlineClassWithAttributesWronglyMulti() throws IOException {
		withInputModels("inlineClassWithAttributesWronglyMulti",
				"PersonList.ecore");
		loadModelFiles();
		assertThrowsIAE(() ->
			refactorings.inlineClass(refactorings.getEClass("PersonList", "Address")));
		modelManager.saveEcores(AbstractTest.MODIFIED);
		assertModifiedFilesAreSameAsOriginal();
		assertThat(appender.getResult().trim())
			.isEqualTo(
			"ERROR: PersonList.Person.addresses: Cannot inline in a 'many' reference: PersonList.Person.addresses");
	}

	@Test
	void test_inlineClassWithReferencesBidirectional() throws IOException {
		withInputModels("inlineClassWithReferencesBidirectional", "PersonList.ecore");
		loadModelFiles();
		assertThrowsIAE(() ->
			refactorings.inlineClass(refactorings.getEClass("PersonList", "WorkAddress")));
		assertThat(appender.getResult().trim())
			.isEqualTo(
			"ERROR: PersonList.WorkAddress: The EClass is used by more than one element:\n"
			+ "  PersonList.Person.workAddress\n"
			+ "    ecore.ETypedElement.eType\n"
			+ "  PersonList.WorkPlace.address\n"
			+ "    ecore.ETypedElement.eType");
	}

	@Test
	void test_inlineClass_IsOppositeOf_extractClass() throws IOException {
		withInputModels("extractClassWithAttributes", "PersonList.ecore");
		assertOppositeRefactorings(
			() -> refactorings.extractClass("Address",
					asList(
							refactorings.getEAttribute("PersonList", "Person", "street"),
							refactorings.getEAttribute("PersonList", "Person", "houseNumber"))
						),
			() -> refactorings.inlineClass(refactorings.getEClass("PersonList", "Address"))
		);
		assertLogIsEmpty();
	}

	@Test
	void test_inlineClass_IsOppositeOf_extractClass2() throws IOException {
		withInputModels("inlineClassWithAttributes", "PersonList.ecore");
		assertOppositeRefactorings(
			() -> refactorings.inlineClass(refactorings.getEClass("PersonList", "Address")),
			() -> refactorings.extractClass("Address",
					asList(
							refactorings.getEAttribute("PersonList", "Person", "street"),
							refactorings.getEAttribute("PersonList", "Person", "houseNumber"))
					)
		);
		assertLogIsEmpty();
	}

	@ParameterizedTest
	@ValueSource(strings = {
		"referenceToClassBidirectional",
		"referenceToClassWithCardinality",
		"referenceToClassUnidirectional"
	})
	void test_referenceToClass(String directory) throws IOException {
		withInputModels(directory, "PersonList.ecore");
		loadModelFiles();
		final EReference ref = refactorings.getEReference("PersonList", "Person", "works");
		refactorings.referenceToClass("WorkingPosition", ref);
		modelManager.saveEcores(AbstractTest.MODIFIED);
		assertModifiedFiles();
	}

	@Test
	void test_referenceToClassWithContainmentReference() throws IOException {
		withInputModels("referenceToClassWithContainmentReference", "PersonList.ecore");
		loadModelFiles();
		final EReference ref = refactorings.getEReference("PersonList", "Person", "works");
		assertThrowsIAE(() -> refactorings.referenceToClass("WorkingPosition", ref));
		modelManager.saveEcores(AbstractTest.MODIFIED);
		assertModifiedFilesAreSameAsOriginal();
		assertThat(appender.getResult().trim()).isEqualTo(
			"ERROR: PersonList.Person.works: Cannot apply referenceToClass on containment reference: PersonList.Person.works");
	}

	@ParameterizedTest
	@ValueSource(strings = {
		"classToReferenceUnidirectional",
		"classToReferenceBidirectional",
		"classToReferenceWithCardinality"
	})
	void test_classToReferenceUnidirectional(String directory) throws IOException {
		withInputModels(directory, "PersonList.ecore");
		loadModelFiles();
		final EClass cl = refactorings.getEClass("PersonList", "WorkingPosition");
		var result = refactorings.classToReference(cl);
		assertThat(result)
			.isEqualTo(refactorings.getEReference("PersonList", "Person", "works"));
		modelManager.saveEcores(AbstractTest.MODIFIED);
		assertModifiedFiles();
	}

	@Test
	void test_classToReferenceWhenClassIsNotReferred() {
		withInputModels("classToReferenceWronglyReferred", "TestEcore.ecore");
		loadModelFiles();
		assertThrowsIAE(() -> refactorings.classToReference(
				refactorings.getEClass("p", "CNotReferred")));
		assertThat(appender.getResult().trim())
			.isEqualTo("ERROR: p.CNotReferred: The EClass is not referred: p.CNotReferred");
	}

	@Test
	void test_classToReferenceWhenClassIsReferredMoreThanOnce() {
		withInputModels("classToReferenceWronglyReferred", "TestEcore.ecore");
		loadModelFiles();
		assertThrowsIAE(() -> refactorings.classToReference(
				refactorings.getEClass("p", "C")));
		assertThat(appender.getResult())
			.isEqualTo(
			"ERROR: p.C: The EClass is referred by more than one container:\n"
			+ "  p.C1.r1\n"
			+ "  p.C2.r2\n"
			+ "");
	}

	@Test
	void test_classToReferenceWhenClassIsReferredMoreThanOnceParallel() {
		withInputModels("classToReferenceWronglyReferredParallel",
				"TestEcore.ecore", "TestEcoreReferring.ecore");
		loadModelFiles();
		assertThrowsIAE(() -> refactorings.classToReference(
				refactorings.getEClass("p", "C")));
		assertThat(appender.getResult())
			.isEqualTo(
			"ERROR: p.C: The EClass is referred by more than one container:\n"
			+ "  p.C1.r1\n"
			+ "  p2.C2.r2\n"
			+ "");
	}

	@Test
	void test_classToReferenceWithMissingTarget() {
		withInputModels("classToReferenceUnidirectional", "PersonList.ecore");
		loadModelFiles();
		final EClass cl = refactorings.getEClass("PersonList", "WorkingPosition");
		// manually remove reference to target class WorkPlace
		cl.getEStructuralFeatures().remove(cl.getEStructuralFeature("workPlace"));
		assertThrowsIAE(() -> refactorings.classToReference(cl));
		assertThat(appender.getResult().trim()).isEqualTo(
			"ERROR: PersonList.WorkingPosition: No references not of type PersonList.Person");
	}

	@Test
	void test_classToReferenceWithTooManyTargets() {
		withInputModels("classToReferenceUnidirectional", "PersonList.ecore");
		loadModelFiles();
		final EClass cl = refactorings.getEClass("PersonList", "WorkingPosition");
		// manually add another reference to target class
		EReference ref = this.createEReference(cl, "another");
		ref.setEType(refactorings.getEClass("PersonList", "List"));
		assertThrowsIAE(() -> refactorings.classToReference(cl));
		assertThat(appender.getResult())
			.isEqualTo(
			"ERROR: PersonList.WorkingPosition: Too many references not of type PersonList.Person:\n"
			+ "  PersonList.WorkingPosition.workPlace\n"
			+ "  PersonList.WorkingPosition.another\n"
			+ "");
	}

	@Test
	void test_classToReferenceUnidirectionalWithoutOppositeIsOk() throws IOException {
		withInputModels("classToReferenceUnidirectional", "PersonList.ecore");
		loadModelFiles();
		final EClass cl = refactorings.getEClass("PersonList", "WorkingPosition");
		// manually remove the opposite reference
		EReference personFeature = (EReference) cl.getEStructuralFeature("person");
		// also appropriately update the opposite, otherwise we have a dangling reference
		personFeature.getEOpposite().setEOpposite(null);
		cl.getEStructuralFeatures().remove(personFeature);
		refactorings.classToReference(cl);
		modelManager.saveEcores(AbstractTest.MODIFIED);
		assertModifiedFiles();
	}

	@Test
	void test_referenceToClass_IsOppositeOf_classToReferenceUnidirectional() throws IOException {
		withInputModels("referenceToClassUnidirectional", "PersonList.ecore");
		assertOppositeRefactorings(
			() -> refactorings.referenceToClass("WorkingPosition",
					refactorings.getEReference("PersonList", "Person", "works")),
			() -> refactorings.classToReference(
					refactorings.getEClass("PersonList", "WorkingPosition")));
		assertLogIsEmpty();
	}

	@Test
	void test_referenceToClass_IsOppositeOf_classToReferenceUnidirectional2() throws IOException {
		withInputModels("classToReferenceUnidirectional", "PersonList.ecore");
		assertOppositeRefactorings(
			() -> refactorings.classToReference(
					refactorings.getEClass("PersonList", "WorkingPosition")),
			() -> refactorings.referenceToClass("WorkingPosition",
					refactorings.getEReference("PersonList", "Person", "works")));
		assertLogIsEmpty();
	}

	@Test
	void test_referenceToClass_IsOppositeOf_classToReferenceBidirectional() throws IOException {
		withInputModels("referenceToClassBidirectional", "PersonList.ecore");
		assertOppositeRefactorings(
			() -> refactorings.referenceToClass("WorkingPosition",
					refactorings.getEReference("PersonList", "Person", "works")),
			() -> refactorings.classToReference(
					refactorings.getEClass("PersonList", "WorkingPosition")));
		assertLogIsEmpty();
	}

	@Test
	void test_referenceToClass_IsOppositeOf_classToReferenceBidirectional2() throws IOException {
		withInputModels("classToReferenceBidirectional", "PersonList.ecore");
		assertOppositeRefactorings(
			() -> refactorings.classToReference(
					refactorings.getEClass("PersonList", "WorkingPosition")),
			() -> refactorings.referenceToClass("WorkingPosition",
					refactorings.getEReference("PersonList", "Person", "works")));
		assertLogIsEmpty();
	}

	@Test
	void test_extractSuperclass() throws IOException {
		withInputModels("extractSuperclass", "TestEcore.ecore");
		loadModelFiles();
		refactorings.extractSuperclass(
			asList(
				refactorings.getEAttribute("p", "C1", "a1"),
				refactorings.getEAttribute("p", "C2", "a1")));
		refactorings.extractSuperclass(
			asList(
				refactorings.getEAttribute("p", "C3", "a1"),
				refactorings.getEAttribute("p", "C4", "a1")));
		modelManager.saveEcores(AbstractTest.MODIFIED);
		assertModifiedFiles();
		assertLogIsEmpty();
	}

	@Test
	void test_pullUpFeatures() throws IOException {
		withInputModels("pullUpFeatures", "PersonList.ecore");
		loadModelFiles();
		final EClass person = refactorings.getEClass("PersonList", "Person");
		final EClass student = refactorings.getEClass("PersonList", "Student");
		final EClass employee = refactorings.getEClass("PersonList", "Employee");
		refactorings.pullUpFeatures(person,
			asList(
				student.getEStructuralFeature("name"),
				employee.getEStructuralFeature("name")));
		modelManager.saveEcores(AbstractTest.MODIFIED);
		assertModifiedFiles();
	}

	@Test
	void test_pullUpFeaturesDifferent() throws IOException {
		withInputModels("pullUpFeaturesDifferent", "PersonList.ecore");
		loadModelFiles();
		final EClass person = refactorings.getEClass("PersonList", "Person");
		final EClass student = refactorings.getEClass("PersonList", "Student");
		final EClass employee = refactorings.getEClass("PersonList", "Employee");
		assertThrowsIAE(() -> refactorings.pullUpFeatures(person,
			asList(
				student.getEStructuralFeature("name"),
				employee.getEStructuralFeature("name"))));
		modelManager.saveEcores(AbstractTest.MODIFIED);
		assertModifiedFilesAreSameAsOriginal();
		assertThat(appender.getResult())
			.isEqualTo(
			"ERROR: PersonList.Employee.name: The two features are not equal:\n"
			+ "ecore.ETypedElement.lowerBound:\n"
			+ "  PersonList.Student.name: 0\n"
			+ "  PersonList.Employee.name: 1\n"
			+ "\n"
			+ "");
	}

	@Test
	void test_pullUpFeaturesNotSubclass() throws IOException {
		withInputModels("pullUpFeaturesNotSubclass", "PersonList.ecore");
		loadModelFiles();
		final EClass person = refactorings.getEClass("PersonList", "Person");
		final EClass student = refactorings.getEClass("PersonList", "Student");
		final EClass employee = refactorings.getEClass("PersonList", "Employee");
		assertThrowsIAE(() -> refactorings.pullUpFeatures(person,
			asList(
				student.getEStructuralFeature("name"),
				employee.getEStructuralFeature("name"))));
		modelManager.saveEcores(AbstractTest.MODIFIED);
		assertModifiedFilesAreSameAsOriginal();
		assertThat(appender.getResult())
			.isEqualTo(
			"ERROR: PersonList.Student: Not a direct subclass of: PersonList.Person\n"
			+ "ERROR: PersonList.Employee: Not a direct subclass of: PersonList.Person\n"
			+ "");
	}

	@Test
	void test_allUsagesOfThisClass() {
		var p = createEPackage("p");
		var classForUsages = stdLib.addNewEClass(p, "C");
		stdLib.addNewSubclass(classForUsages, "SubClass");
		stdLib.addNewEClass(p, "UsesC", c ->
			stdLib.addNewEReference(c, "refToC", classForUsages));
		var usages = refactorings.allUsagesOfThisClass(classForUsages);
		var repr = usages.stream().map(s ->
				getEObjectRepr(s.getEObject()) + "\n" +
				"  " +
				getEObjectRepr(s.getEStructuralFeature()))
			.collect(Collectors.joining("\n"));
		assertThat(repr)
			.isEqualTo(
			"p.SubClass\n"
			+ "  ecore.EClass.eSuperTypes\n"
			+ "p.UsesC.refToC\n"
			+ "  ecore.ETypedElement.eType");
	}

	@Test
	void test_findSingleUsageOfThisClass() {
		var p = createEPackage("p");
		var classForUsages = stdLib.addNewEClass(p, "C");

		// not used at all
		assertThrowsIAE(() -> refactorings.findSingleUsageOfThisClass(classForUsages));

		// just one usage OK
		var subClass = stdLib.addNewSubclass(classForUsages, "SubClass");
		assertThat(refactorings.findSingleUsageOfThisClass(classForUsages))
			.isSameAs(subClass);

		// too many usages
		stdLib.addNewEClass(p, "UsesC", c ->
			stdLib.addNewEReference(c, "refToC", classForUsages));
		assertThrowsIAE(() -> refactorings.findSingleUsageOfThisClass(classForUsages));
		assertThat(appender.getResult())
			.isEqualTo(
			"ERROR: p.C: The EClass is not used: p.C\n"
			+ "ERROR: p.C: The EClass is used by more than one element:\n"
			+ "  p.SubClass\n"
			+ "    ecore.EClass.eSuperTypes\n"
			+ "  p.UsesC.refToC\n"
			+ "    ecore.ETypedElement.eType\n"
			+ "");
	}

	@Test
	void test_getAsContainmentReference() {
		var p = createEPackage("p");
		var classForUsages = stdLib.addNewEClass(p, "C");

		// not an EReference
		assertThrowsIAE(() -> refactorings.getAsContainmentReference(classForUsages));

		// not a containment reference
		var ref = stdLib.addNewEReference(classForUsages, "refToC", null);
		assertThrowsIAE(() -> refactorings.getAsContainmentReference(ref));

		// OK
		var containmentRef = stdLib.addNewContainmentEReference(classForUsages, "contRefToC", null);
		assertThat(refactorings.getAsContainmentReference(containmentRef))
			.isSameAs(containmentRef);

		assertThat(appender.getResult())
			.isEqualTo(
			"ERROR: p.C: Not a reference: p.C\n"
			+ "ERROR: p.C.refToC: Not a containment reference: p.C.refToC\n"
			+ "");
	}

	private static IllegalArgumentException assertThrowsIAE(Executable executable) {
		return assertThrows(IllegalArgumentException.class,
			executable);
	}
}
