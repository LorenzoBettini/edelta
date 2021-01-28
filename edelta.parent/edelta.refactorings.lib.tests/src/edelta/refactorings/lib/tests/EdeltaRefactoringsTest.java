package edelta.refactorings.lib.tests;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.xtext.xbase.lib.IterableExtensions.head;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;

import org.assertj.core.api.Assertions;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import edelta.lib.AbstractEdelta;
import edelta.refactorings.lib.EdeltaRefactorings;
import edelta.refactorings.lib.tests.utils.InMemoryLoggerAppender;
import edelta.testutils.EdeltaTestUtils;

class EdeltaRefactoringsTest extends AbstractTest {
	private EdeltaRefactorings refactorings;

	private InMemoryLoggerAppender appender;

	private String testModelDirectory;

	private String testModelFile;

	@BeforeEach
	void setup() throws Exception {
		refactorings = new EdeltaRefactorings();
		appender = new InMemoryLoggerAppender();
		appender.setLineSeparator("\n");
		refactorings.getLogger().addAppender(appender);
		refactorings.performSanityChecks();
	}

	private void withInputModel(final String testModelDirectory, final String testModelFile) {
		this.testModelDirectory = testModelDirectory;
		this.testModelFile = testModelFile;
	}

	private void loadModelFile() {
		checkInputModelSettings();
		refactorings
				.loadEcoreFile(AbstractTest.TESTECORES +
						testModelDirectory +
						"/" +
						testModelFile);
	}

	private void assertModifiedFile() throws IOException {
		checkInputModelSettings();
		// no need to explicitly validate:
		// if the ecore file is saved then it is valid
		EdeltaTestUtils.assertFilesAreEquals(
				AbstractTest.EXPECTATIONS +
					testModelDirectory +
					"/" +
					testModelFile,
				AbstractTest.MODIFIED + testModelFile);
		assertLogIsEmpty();
	}

	private void assertLogIsEmpty() {
		assertThat(appender.getResult()).isEmpty();
	}

	private void assertModifiedFileIsSameAsOriginal() throws IOException {
		checkInputModelSettings();
		EdeltaTestUtils.assertFilesAreEquals(
				AbstractTest.TESTECORES +
				testModelDirectory +
				"/" +
				testModelFile,
				AbstractTest.MODIFIED + testModelFile);
	}

	private void assertOppositeRefactorings(final Runnable first, final Runnable second) throws IOException {
		loadModelFile();
		first.run();
		refactorings.saveModifiedEcores(AbstractTest.MODIFIED);
		second.run();
		refactorings.saveModifiedEcores(AbstractTest.MODIFIED);
		assertModifiedFileIsSameAsOriginal();
	}

	private void checkInputModelSettings() {
		assertThat(this.testModelDirectory).isNotNull();
		assertThat(this.testModelFile).isNotNull();
	}

	@Test
	void test_ConstructorArgument() {
		refactorings = new EdeltaRefactorings(new AbstractEdelta() {
		});
		final EClass c = this.createEClassWithoutPackage("C1");
		refactorings.addMandatoryAttribute(c, "test", this.stringDataType);
		final EAttribute attr = head(c.getEAttributes());
		assertThat(attr)
			.returns("test", EAttribute::getName);
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
		withInputModel("mergeFeatures", "PersonList.ecore");
		loadModelFile();
		final EClass person = refactorings.getEClass("PersonList", "Person");
		refactorings.mergeFeatures("name",
			asList(
				person.getEStructuralFeature("firstName"),
				person.getEStructuralFeature("lastName")));
		refactorings.saveModifiedEcores(AbstractTest.MODIFIED);
		assertModifiedFile();
	}

	@Test
	void test_mergeFeaturesDifferent() throws IOException {
		withInputModel("mergeFeaturesDifferent", "PersonList.ecore");
		loadModelFile();
		final EClass person = refactorings.getEClass("PersonList", "Person");
		assertThrowsIAE(() -> refactorings.mergeFeatures("name",
			asList(
				person.getEStructuralFeature("firstName"),
				person.getEStructuralFeature("lastName"))));
		refactorings.saveModifiedEcores(AbstractTest.MODIFIED);
		assertModifiedFileIsSameAsOriginal();
		final EClass student = refactorings.getEClass("PersonList", "Student");
		assertThrowsIAE(() -> refactorings.mergeFeatures("name",
			asList(
				person.getEStructuralFeature("lastName"),
				student.getEStructuralFeature("lastName"))));
		refactorings.saveModifiedEcores(AbstractTest.MODIFIED);
		assertModifiedFileIsSameAsOriginal();
		assertThrowsIAE(() -> refactorings.mergeFeatures("name",
			asList(
				person.getEStructuralFeature("list"),
				person.getEStructuralFeature("lastName"))));
		refactorings.saveModifiedEcores(AbstractTest.MODIFIED);
		assertModifiedFileIsSameAsOriginal();
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
		withInputModel("mergeFeatures2", "PersonList.ecore");
		loadModelFile();
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
		refactorings.saveModifiedEcores(AbstractTest.MODIFIED);
		assertModifiedFile();
	}

	@Test
	void test_mergeFeatures2NonCompliant() {
		withInputModel("mergeFeatures2", "PersonList.ecore");
		loadModelFile();
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
		withInputModel("mergeFeatures3", "PersonList.ecore");
		loadModelFile();
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
		refactorings.saveModifiedEcores(AbstractTest.MODIFIED);
		assertModifiedFile();
	}

	@Test
	void test_enumToSubclasses() throws IOException {
		withInputModel("enumToSubclasses", "PersonList.ecore");
		loadModelFile();
		final EClass person = refactorings.getEClass("PersonList", "Person");
		EStructuralFeature _eStructuralFeature = person.getEStructuralFeature("gender");
		final Collection<EClass> result = refactorings.enumToSubclasses(((EAttribute) _eStructuralFeature));
		refactorings.saveModifiedEcores(AbstractTest.MODIFIED);
		assertModifiedFile();
		assertThat(result)
			.extracting(EClass::getName)
			.containsExactlyInAnyOrder("Male", "Female");
	}

	@Test
	void test_enumToSubclassesNotAnEEnum() throws IOException {
		withInputModel("enumToSubclasses", "PersonList.ecore");
		loadModelFile();
		final EClass person = refactorings.getEClass("PersonList", "Person");
		final Collection<EClass> result = 
			refactorings.enumToSubclasses(
				((EAttribute) person.getEStructuralFeature("firstname")));
		assertThat(result).isNull();
		refactorings.saveModifiedEcores(AbstractTest.MODIFIED);
		assertModifiedFileIsSameAsOriginal();
		assertThat(appender.getResult().trim())
			.isEqualTo("ERROR: PersonList.Person.firstname: Not an EEnum: ecore.EString");
	}

	@Test
	void test_subclassesToEnum() throws IOException {
		withInputModel("subclassesToEnum", "PersonList.ecore");
		loadModelFile();
		final EPackage personList = refactorings.getEPackage("PersonList");
		final EAttribute result = refactorings.subclassesToEnum("Gender",
			asList(
				(EClass) personList.getEClassifier("Male"),
				(EClass) personList.getEClassifier("Female")));
		refactorings.saveModifiedEcores(AbstractTest.MODIFIED);
		assertModifiedFile();
		assertThat(result)
			.returns("gender", EAttribute::getName);
	}

	@Test
	void test_subclassesToEnumSubclassesNotEmpty() throws IOException {
		withInputModel("subclassesToEnumSubclassesNotEmpty", "PersonList.ecore");
		loadModelFile();
		final EPackage personList = refactorings.getEPackage("PersonList");
		assertThrowsIAE(() -> refactorings.subclassesToEnum("Gender",
			asList(
				(EClass) personList.getEClassifier("Male"),
				(EClass) personList.getEClassifier("NotSpecified"),
				(EClass) personList.getEClassifier("Female"))));
		refactorings.saveModifiedEcores(AbstractTest.MODIFIED);
		assertModifiedFileIsSameAsOriginal();
		assertThat(appender.getResult().trim())
			.isEqualTo(
			"ERROR: PersonList.Male: Not an empty class: PersonList.Male:\n"
			+ "  PersonList.Male.maleName\n"
			+ "ERROR: PersonList.Female: Not an empty class: PersonList.Female:\n"
			+ "  PersonList.Female.femaleName");
	}

	@Test
	void test_subclassesToEnumSubclassesWrongSubclasses() throws IOException {
		withInputModel("subclassesToEnumSubclassesWrongSubclasses", "PersonList.ecore");
		loadModelFile();
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
		refactorings.saveModifiedEcores(AbstractTest.MODIFIED);
		assertModifiedFileIsSameAsOriginal();
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
	void test_enumToSubclasses_IsOppositeOf_subclassesToEnum() throws IOException {
		withInputModel("enumToSubclasses", "PersonList.ecore");
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
		withInputModel("subclassesToEnum", "PersonList.ecore");
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
		withInputModel("extractClassWithAttributes", "PersonList.ecore");
		loadModelFile();
		refactorings.extractClass("Address",
			asList(
				refactorings.getEAttribute("PersonList", "Person", "street"),
				refactorings.getEAttribute("PersonList", "Person", "houseNumber"))
			);
		refactorings.saveModifiedEcores(AbstractTest.MODIFIED);
		assertModifiedFile();
	}

	@Test
	void test_extractClassWithAttributesContainedInDifferentClasses() throws IOException {
		withInputModel("extractClassWithAttributesContainedInDifferentClasses", "PersonList.ecore");
		loadModelFile();
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
		refactorings.saveModifiedEcores(AbstractTest.MODIFIED);
		assertModifiedFileIsSameAsOriginal();
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

	@ParameterizedTest
	@ValueSource(strings = {
		"referenceToClassBidirectional",
		"referenceToClassWithCardinality",
		"referenceToClassUnidirectional"
	})
	void test_referenceToClass(String directory) throws IOException {
		withInputModel(directory, "PersonList.ecore");
		loadModelFile();
		final EReference ref = refactorings.getEReference("PersonList", "Person", "works");
		refactorings.referenceToClass("WorkingPosition", ref);
		refactorings.saveModifiedEcores(AbstractTest.MODIFIED);
		assertModifiedFile();
	}

	@Test
	void test_referenceToClassWithContainmentReference() throws IOException {
		withInputModel("referenceToClassWithContainmentReference", "PersonList.ecore");
		loadModelFile();
		final EReference ref = refactorings.getEReference("PersonList", "Person", "works");
		assertThrowsIAE(() -> refactorings.referenceToClass("WorkingPosition", ref));
		refactorings.saveModifiedEcores(AbstractTest.MODIFIED);
		assertModifiedFileIsSameAsOriginal();
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
		withInputModel(directory, "PersonList.ecore");
		loadModelFile();
		final EClass cl = refactorings.getEClass("PersonList", "WorkingPosition");
		var result = refactorings.classToReference(cl);
		assertThat(result)
			.isEqualTo(refactorings.getEReference("PersonList", "Person", "works"));
		refactorings.saveModifiedEcores(AbstractTest.MODIFIED);
		assertModifiedFile();
	}

	@Test
	void test_classToReferenceWhenClassIsNotReferred() {
		withInputModel("classToReferenceWronglyReferred", "TestEcore.ecore");
		loadModelFile();
		assertThrowsIAE(() -> refactorings.classToReference(
				refactorings.getEClass("p", "CNotReferred")));
		assertThat(appender.getResult().trim())
			.isEqualTo("ERROR: p.CNotReferred: The EClass is not referred: p.CNotReferred");
	}

	@Test
	void test_classToReferenceWhenClassIsReferredMoreThanOnce() {
		withInputModel("classToReferenceWronglyReferred", "TestEcore.ecore");
		loadModelFile();
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
	void test_classToReferenceWithMissingTarget() {
		withInputModel("classToReferenceUnidirectional", "PersonList.ecore");
		loadModelFile();
		final EClass cl = refactorings.getEClass("PersonList", "WorkingPosition");
		// manually remove reference to target class WorkPlace
		cl.getEStructuralFeatures().remove(cl.getEStructuralFeature("workPlace"));
		assertThrowsIAE(() -> refactorings.classToReference(cl));
		assertThat(appender.getResult().trim()).isEqualTo(
			"ERROR: PersonList.WorkingPosition: No references not of type PersonList.Person");
	}

	@Test
	void test_classToReferenceWithTooManyTargets() {
		withInputModel("classToReferenceUnidirectional", "PersonList.ecore");
		loadModelFile();
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
		withInputModel("classToReferenceUnidirectional", "PersonList.ecore");
		loadModelFile();
		final EClass cl = refactorings.getEClass("PersonList", "WorkingPosition");
		// manually remove the opposite reference
		EReference personFeature = (EReference) cl.getEStructuralFeature("person");
		// also appropriately update the opposite, otherwise we have a dangling reference
		personFeature.getEOpposite().setEOpposite(null);
		cl.getEStructuralFeatures().remove(personFeature);
		refactorings.classToReference(cl);
		refactorings.saveModifiedEcores(AbstractTest.MODIFIED);
		assertModifiedFile();
	}

	@Test
	void test_referenceToClass_IsOppositeOf_classToReferenceUnidirectional() throws IOException {
		withInputModel("referenceToClassUnidirectional", "PersonList.ecore");
		assertOppositeRefactorings(
			() -> refactorings.referenceToClass("WorkingPosition",
					refactorings.getEReference("PersonList", "Person", "works")),
			() -> refactorings.classToReference(
					refactorings.getEClass("PersonList", "WorkingPosition")));
		assertLogIsEmpty();
	}

	@Test
	void test_referenceToClass_IsOppositeOf_classToReferenceUnidirectional2() throws IOException {
		withInputModel("classToReferenceUnidirectional", "PersonList.ecore");
		assertOppositeRefactorings(
			() -> refactorings.classToReference(
					refactorings.getEClass("PersonList", "WorkingPosition")),
			() -> refactorings.referenceToClass("WorkingPosition",
					refactorings.getEReference("PersonList", "Person", "works")));
		assertLogIsEmpty();
	}

	@Test
	void test_referenceToClass_IsOppositeOf_classToReferenceBidirectional() throws IOException {
		withInputModel("referenceToClassBidirectional", "PersonList.ecore");
		assertOppositeRefactorings(
			() -> refactorings.referenceToClass("WorkingPosition",
					refactorings.getEReference("PersonList", "Person", "works")),
			() -> refactorings.classToReference(
					refactorings.getEClass("PersonList", "WorkingPosition")));
		assertLogIsEmpty();
	}

	@Test
	void test_referenceToClass_IsOppositeOf_classToReferenceBidirectional2() throws IOException {
		withInputModel("classToReferenceBidirectional", "PersonList.ecore");
		assertOppositeRefactorings(
			() -> refactorings.classToReference(
					refactorings.getEClass("PersonList", "WorkingPosition")),
			() -> refactorings.referenceToClass("WorkingPosition",
					refactorings.getEReference("PersonList", "Person", "works")));
		assertLogIsEmpty();
	}

	@Test
	void test_extractSuperclass() throws IOException {
		withInputModel("extractSuperclass", "TestEcore.ecore");
		loadModelFile();
		refactorings.extractSuperclass(
			asList(
				refactorings.getEAttribute("p", "C1", "a1"),
				refactorings.getEAttribute("p", "C2", "a1")));
		refactorings.extractSuperclass(
			asList(
				refactorings.getEAttribute("p", "C3", "a1"),
				refactorings.getEAttribute("p", "C4", "a1")));
		refactorings.saveModifiedEcores(AbstractTest.MODIFIED);
		assertModifiedFile();
		assertLogIsEmpty();
	}

	@Test
	void test_pullUpFeatures() throws IOException {
		withInputModel("pullUpFeatures", "PersonList.ecore");
		loadModelFile();
		final EClass person = refactorings.getEClass("PersonList", "Person");
		final EClass student = refactorings.getEClass("PersonList", "Student");
		final EClass employee = refactorings.getEClass("PersonList", "Employee");
		refactorings.pullUpFeatures(person,
			asList(
				student.getEStructuralFeature("name"),
				employee.getEStructuralFeature("name")));
		refactorings.saveModifiedEcores(AbstractTest.MODIFIED);
		assertModifiedFile();
	}

	@Test
	void test_pullUpFeaturesDifferent() throws IOException {
		withInputModel("pullUpFeaturesDifferent", "PersonList.ecore");
		loadModelFile();
		final EClass person = refactorings.getEClass("PersonList", "Person");
		final EClass student = refactorings.getEClass("PersonList", "Student");
		final EClass employee = refactorings.getEClass("PersonList", "Employee");
		assertThrowsIAE(() -> refactorings.pullUpFeatures(person,
			asList(
				student.getEStructuralFeature("name"),
				employee.getEStructuralFeature("name"))));
		refactorings.saveModifiedEcores(AbstractTest.MODIFIED);
		assertModifiedFileIsSameAsOriginal();
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
		withInputModel("pullUpFeaturesNotSubclass", "PersonList.ecore");
		loadModelFile();
		final EClass person = refactorings.getEClass("PersonList", "Person");
		final EClass student = refactorings.getEClass("PersonList", "Student");
		final EClass employee = refactorings.getEClass("PersonList", "Employee");
		refactorings.pullUpFeatures(person,
			asList(
				student.getEStructuralFeature("name"),
				employee.getEStructuralFeature("name")));
		refactorings.saveModifiedEcores(AbstractTest.MODIFIED);
		assertModifiedFileIsSameAsOriginal();
		assertThat(appender.getResult())
			.isEqualTo(
			"ERROR: PersonList.Student.name: Not a direct subclass of destination: PersonList.Student\n"
			+ "ERROR: PersonList.Employee.name: Not a direct subclass of destination: PersonList.Employee\n"
			+ "");
	}

	private static IllegalArgumentException assertThrowsIAE(Executable executable) {
		return assertThrows(IllegalArgumentException.class,
			executable);
	}
}
