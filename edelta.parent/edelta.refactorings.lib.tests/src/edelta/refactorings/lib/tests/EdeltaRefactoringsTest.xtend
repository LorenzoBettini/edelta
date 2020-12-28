package edelta.refactorings.lib.tests

import edelta.lib.AbstractEdelta
import edelta.refactorings.lib.EdeltaRefactorings
import edelta.refactorings.lib.tests.utils.InMemoryLoggerAppender
import java.util.stream.Collectors
import org.eclipse.emf.ecore.EAttribute
import org.eclipse.emf.ecore.EClass
import org.eclipse.emf.ecore.EReference
import org.junit.Before
import org.junit.Test

import static edelta.testutils.EdeltaTestUtils.assertFilesAreEquals
import static org.assertj.core.api.Assertions.*
import static org.junit.Assert.assertFalse
import static org.junit.Assert.assertTrue

class EdeltaRefactoringsTest extends AbstractTest {
	var EdeltaRefactorings refactorings
	var InMemoryLoggerAppender appender

	var String testModelDirectory
	var String testModelFile

	@Before
	def void setup() {
		refactorings = new EdeltaRefactorings
		appender = new InMemoryLoggerAppender
		refactorings.logger.addAppender(appender)
		refactorings.performSanityChecks
	}

	def private withInputModel(String testModelDirectory, String testModelFile) {
		this.testModelDirectory = testModelDirectory
		this.testModelFile = testModelFile
	}

	def private loadModelFile() {
		checkInputModelSettings
		refactorings
			.loadEcoreFile(
				TESTECORES + testModelDirectory + "/" + testModelFile
			)
	}

	def private assertModifiedFile() {
		checkInputModelSettings
		assertFilesAreEquals(
				EXPECTATIONS + testModelDirectory + "/" + testModelFile,
				MODIFIED + testModelFile)
		assertLogIsEmpty()
		// no need to explicitly validate:
		// if the ecore file is saved then it is valid
		/*
		val packageManager = new EdeltaEPackageManager
		packageManager.loadEcoreFile(MODIFIED + testModelFile)
		assertThat(packageManager.getEPackage("ecore")).isNotNull // ensure Ecore is loaded
		val diagnostician = Diagnostician.INSTANCE
		packageManager.resourceMapEntrySet.map[value].forEach[
			contents.forEach[
				val diagnostic = diagnostician.validate(it)
				assertThat(diagnostic.severity)
					.isEqualTo(Diagnostic.OK)
			]
		]
		*/
	}

	private def void assertLogIsEmpty() {
		assertThat(appender.result).isEmpty
	}

	def private assertModifiedFileIsSameAsOriginal() {
		checkInputModelSettings
		assertFilesAreEquals(
				TESTECORES + testModelDirectory + "/" + testModelFile,
				MODIFIED + testModelFile)
	}

	def private assertOppositeRefactorings(Runnable first, Runnable second) {
		loadModelFile
		first.run()
		refactorings.saveModifiedEcores(MODIFIED)
		second.run()
		refactorings.saveModifiedEcores(MODIFIED)
		assertModifiedFileIsSameAsOriginal
	}

	def private checkInputModelSettings() {
		assertThat(testModelDirectory).isNotNull
		assertThat(testModelFile).isNotNull
	}

	@Test
	def void test_ConstructorArgument() {
		refactorings = new EdeltaRefactorings(new AbstractEdelta() {})
		val c = createEClassWithoutPackage("C1")
		refactorings.addMandatoryAttribute(c, "test", stringDataType)
		val attr = c.EStructuralFeatures.filter(EAttribute).head
		assertThat(attr)
			.returns("test", [name])
	}

	@Test
	def void test_addMandatoryAttribute() {
		val c = createEClassWithoutPackage("C1")
		refactorings.addMandatoryAttribute(c, "test", stringDataType)
		val attr = c.EStructuralFeatures.filter(EAttribute).head
		assertThat(attr)
			.returns("test", [name])
			.returns(stringDataType, [EAttributeType])
			.returns(true, [isRequired])
	}

	@Test
	def void test_addMandatoryReference() {
		val c = createEClassWithoutPackage("C1")
		refactorings.addMandatoryReference(c, "test", eClassReference)
		val attr = c.EStructuralFeatures.filter(EReference).head
		assertThat(attr)
			.returns("test", [name])
			.returns(eClassReference, [EReferenceType])
			.returns(true, [isRequired])
	}

	@Test
	def void test_mergeFeatures() {
		withInputModel("mergeFeatures", "PersonList.ecore")
		loadModelFile
		val person = refactorings.getEClass("PersonList", "Person")
		refactorings.mergeFeatures("name",
			#[person.getEStructuralFeature("firstName"),person.getEStructuralFeature("lastName")]
		)
		refactorings.saveModifiedEcores(MODIFIED)
		assertModifiedFile
	}

	@Test
	def void test_mergeFeaturesDifferent() {
		withInputModel("mergeFeaturesDifferent", "PersonList.ecore")
		loadModelFile
		val person = refactorings.getEClass("PersonList", "Person")
		refactorings.mergeFeatures("name",
			#[person.getEStructuralFeature("firstName"),person.getEStructuralFeature("lastName")]
		)
		refactorings.saveModifiedEcores(MODIFIED)
		assertModifiedFileIsSameAsOriginal
		val student = refactorings.getEClass("PersonList", "Student")
		refactorings.mergeFeatures("name",
			#[person.getEStructuralFeature("lastName"),student.getEStructuralFeature("lastName")]
		)
		refactorings.saveModifiedEcores(MODIFIED)
		assertModifiedFileIsSameAsOriginal
		refactorings.mergeFeatures("name",
			#[person.getEStructuralFeature("list"),person.getEStructuralFeature("lastName")]
		)
		refactorings.saveModifiedEcores(MODIFIED)
		assertModifiedFileIsSameAsOriginal
		assertThat(appender.result)
			.isEqualTo(
			'''
			ERROR: PersonList.Person.lastName: The two features cannot be merged:
			ecore.ETypedElement.lowerBound:
			  PersonList.Person.firstName: 0
			  PersonList.Person.lastName: 1
			
			ERROR: PersonList.Student.lastName: The two features cannot be merged:
			ecore.ENamedElement.name:
			  PersonList.Person: Person
			  PersonList.Student: Student
			ecore.EStructuralFeature.eContainingClass:
			  PersonList.Person.lastName: PersonList.Person
			  PersonList.Student.lastName: PersonList.Student
			
			ERROR: PersonList.Person.lastName: The two features cannot be merged:
			different kinds:
			  PersonList.Person.list: ecore.EReference
			  PersonList.Person.lastName: ecore.EAttribute
			
			'''.toString)
	}

	@Test
	def void test_mergeFeatures2() {
		withInputModel("mergeFeatures2", "PersonList.ecore")
		loadModelFile
		val list = refactorings.getEClass("PersonList", "List")
		val person = refactorings.getEClass("PersonList", "Person")
		refactorings.mergeFeatures(list.getEStructuralFeature("places"),
			#[list.getEStructuralFeature("wplaces"),list.getEStructuralFeature("lplaces")]
		)
		refactorings.mergeFeatures(person.getEStructuralFeature("name"),
			#[person.getEStructuralFeature("firstName"),person.getEStructuralFeature("lastName")]
		)
		refactorings.saveModifiedEcores(MODIFIED)
		assertModifiedFile
	}

	@Test
	def void test_mergeFeatures2NonCompliant() {
		withInputModel("mergeFeatures2", "PersonList.ecore")
		loadModelFile
		val list = refactorings.getEClass("PersonList", "List")
		val person = refactorings.getEClass("PersonList", "Person")
		// Place is not subtype of WorkPlace
		val wplaces = list.getEStructuralFeature("wplaces")
		refactorings.mergeFeatures(wplaces,
			#[list.getEStructuralFeature("places"),list.getEStructuralFeature("lplaces")]
		)
		// different lowerbound
		wplaces.lowerBound = 1
		refactorings.mergeFeatures(list.getEStructuralFeature("places"),
			#[list.getEStructuralFeature("wplaces"),list.getEStructuralFeature("lplaces")]
		)
		// merge attributes with reference
		refactorings.mergeFeatures(list.getEStructuralFeature("places"),
			#[person.getEStructuralFeature("firstName"),person.getEStructuralFeature("lastName")]
		)
		// merge attributes with different types
		refactorings.mergeFeatures(person.getEStructuralFeature("age"),
			#[person.getEStructuralFeature("firstName"),person.getEStructuralFeature("lastName")]
		)
		assertThat(appender.result)
			.isEqualTo(
			'''
			ERROR: PersonList.List.wplaces: features not compliant with type PersonList.WorkPlace:
			  PersonList.List.places: PersonList.Place
			  PersonList.List.lplaces: PersonList.LivingPlace
			ERROR: PersonList.List.wplaces: The two features cannot be merged:
			ecore.ETypedElement.lowerBound:
			  PersonList.List.places: 0
			  PersonList.List.wplaces: 1
			
			ERROR: PersonList.List.places: features not compliant with type PersonList.Place:
			  PersonList.Person.firstName: ecore.EString
			  PersonList.Person.lastName: ecore.EString
			ERROR: PersonList.Person.age: features not compliant with type ecore.EInt:
			  PersonList.Person.firstName: ecore.EString
			  PersonList.Person.lastName: ecore.EString
			'''.toString)
	}

	@Test
	def void test_mergeFeatures3() {
		withInputModel("mergeFeatures3", "PersonList.ecore")
		loadModelFile
		val list = refactorings.getEClass("PersonList", "List")
		val place = refactorings.getEClass("PersonList", "Place")
		val person = refactorings.getEClass("PersonList", "Person")
		refactorings.mergeFeatures("places", place,
			#[list.getEStructuralFeature("wplaces"),list.getEStructuralFeature("lplaces")]
		)
		refactorings.mergeFeatures("name", stringDataType,
			#[person.getEStructuralFeature("firstName"),person.getEStructuralFeature("lastName")]
		)
		refactorings.saveModifiedEcores(MODIFIED)
		assertModifiedFile
	}

	@Test
	def void test_enumToSubclasses() {
		withInputModel("enumToSubclasses", "PersonList.ecore")
		loadModelFile
		val person = refactorings.getEClass("PersonList", "Person")
		val result = refactorings.enumToSubclasses(
			person.getEStructuralFeature("gender") as EAttribute
		)
		refactorings.saveModifiedEcores(MODIFIED)
		assertModifiedFile
		assertThat(result)
			.extracting([name])
			.containsExactlyInAnyOrder("Male", "Female")
	}

	@Test
	def void test_enumToSubclassesNotAnEEnum() {
		withInputModel("enumToSubclasses", "PersonList.ecore")
		loadModelFile
		val person = refactorings.getEClass("PersonList", "Person")
		val result = refactorings.enumToSubclasses(
			person.getEStructuralFeature("firstname") as EAttribute
		)
		assertThat(result).isNull
		refactorings.saveModifiedEcores(MODIFIED)
		assertModifiedFileIsSameAsOriginal
		assertThat(appender.result.trim)
			.isEqualTo("ERROR: PersonList.Person.firstname: Not an EEnum: ecore.EString")
	}

	@Test
	def void test_subclassesToEnum() {
		withInputModel("subclassesToEnum", "PersonList.ecore")
		loadModelFile
		val personList = refactorings.getEPackage("PersonList")
		val result = refactorings.subclassesToEnum("Gender",
			#[
				personList.getEClassifier("Male") as EClass,
				personList.getEClassifier("Female") as EClass
			]
		)
		refactorings.saveModifiedEcores(MODIFIED)
		assertModifiedFile
		assertThat(result)
			.returns("gender", [name])
	}

	@Test
	def void test_subclassesToEnumSubclassesNotEmpty() {
		withInputModel("subclassesToEnumSubclassesNotEmpty", "PersonList.ecore")
		loadModelFile
		val personList = refactorings.getEPackage("PersonList")
		val result = refactorings.subclassesToEnum("Gender",
			#[
				personList.getEClassifier("Male") as EClass,
				personList.getEClassifier("NotSpecified") as EClass,
				personList.getEClassifier("Female") as EClass
			]
		)
		assertThat(result).isNull
		refactorings.saveModifiedEcores(MODIFIED)
		assertModifiedFileIsSameAsOriginal
		assertThat(appender.result.trim)
			.isEqualTo(
				'''
				ERROR: PersonList.Male: Not an empty class: PersonList.Male:
				  PersonList.Male.maleName
				ERROR: PersonList.Female: Not an empty class: PersonList.Female:
				  PersonList.Female.femaleName'''.toString
			)
	}

	@Test
	def void test_subclassesToEnumSubclassesWrongSubclasses() {
		withInputModel("subclassesToEnumSubclassesWrongSubclasses", "PersonList.ecore")
		loadModelFile
		val personList = refactorings.getEPackage("PersonList")
		var result = refactorings.subclassesToEnum("Gender",
			#[
				personList.getEClassifier("Male") as EClass,
				personList.getEClassifier("Female") as EClass,
				personList.getEClassifier("FemaleEmployee") as EClass,
				personList.getEClassifier("Employee") as EClass
			]
		)
		assertThat(result).isNull
		result = refactorings.subclassesToEnum("Gender",
			#[
				personList.getEClassifier("Female") as EClass,
				personList.getEClassifier("AnotherFemale") as EClass
			]
		)
		assertThat(result).isNull
		result = refactorings.subclassesToEnum("Gender",
			#[
				personList.getEClassifier("Female") as EClass
			]
		)
		assertThat(result).isNull
		refactorings.saveModifiedEcores(MODIFIED)
		assertModifiedFileIsSameAsOriginal
		assertThat(appender.result.trim)
			.isEqualTo(
				'''
				ERROR: PersonList.FemaleEmployee: Expected one superclass: PersonList.FemaleEmployee instead of:
				  PersonList.Person
				  PersonList.Employee
				ERROR: PersonList.Employee: Expected one superclass: PersonList.Employee instead of:
				  empty
				ERROR: PersonList.AnotherFemale: Wrong superclass of PersonList.AnotherFemale:
				  Expected: PersonList.Person
				  Actual  : PersonList.AnotherPerson
				ERROR: PersonList.Person: The class has additional subclasses:
				  PersonList.Male
				  PersonList.FemaleEmployee'''.toString
			)
	}

	@Test
	def void test_enumToSubclasses_IsOppositeOf_subclassesToEnum() {
		withInputModel("enumToSubclasses", "PersonList.ecore")
		assertOppositeRefactorings(
			[refactorings.enumToSubclasses(
				refactorings.getEAttribute("PersonList", "Person", "gender"))],
			[refactorings.subclassesToEnum("Gender",
				#[
					refactorings.getEClass("PersonList", "Male"),
					refactorings.getEClass("PersonList", "Female")
				])]
		)
		assertLogIsEmpty
	}

	@Test
	def void test_subclassesToEnum_IsOppositeOf_enumToSubclasses() {
		withInputModel("subclassesToEnum", "PersonList.ecore")
		assertOppositeRefactorings(
			[refactorings.subclassesToEnum("Gender",
				#[
					refactorings.getEClass("PersonList", "Male"),
					refactorings.getEClass("PersonList", "Female")
				])],
			[refactorings.enumToSubclasses(
				refactorings.getEAttribute("PersonList", "Person", "gender"))]
		)
		assertLogIsEmpty
	}

	@Test
	def void test_extractClassWithAttributes() {
		withInputModel("extractClassWithAttributes", "PersonList.ecore")
		loadModelFile
		refactorings
			.extractClass("Address",
				#[
					refactorings.getEAttribute("PersonList", "Person", "street"),
					refactorings.getEAttribute("PersonList", "Person", "houseNumber")
				],
				"address"
			)
		refactorings.saveModifiedEcores(MODIFIED)
		assertModifiedFile
	}

	@Test
	def void test_extractClassWithAttributesContainedInDifferentClasses() {
		withInputModel("extractClassWithAttributesContainedInDifferentClasses", "PersonList.ecore")
		loadModelFile
		refactorings
			.extractClass("Address",
				#[
					refactorings.getEAttribute("PersonList", "Person", "street"),
					refactorings.getEAttribute("PersonList", "Person2", "street")
				],
				"address"
			)
		refactorings.saveModifiedEcores(MODIFIED)
		assertModifiedFileIsSameAsOriginal
		assertThat(appender.result.trim)
			.isEqualTo(
				'''
				ERROR: PersonList.Person: Extracted features must belong to the same class: PersonList.Person
				ERROR: PersonList.Person2: Extracted features must belong to the same class: PersonList.Person2'''
				.toString
			)
	}

	@Test
	def void test_extractClassWithAttributesEmpty() {
		val result = refactorings
			.extractClass("Address",
				#[
				],
				"address"
			)
		assertThat(result).isNull
	}

	@Test
	def void test_referenceToClassBidirectional() {
		withInputModel("referenceToClassBidirectional", "PersonList.ecore")
		loadModelFile
		val ref = refactorings.getEReference("PersonList", "Person", "works")
		refactorings.referenceToClass("WorkingPosition", ref)
		refactorings.saveModifiedEcores(MODIFIED)
		assertModifiedFile
	}

	@Test
	def void test_referenceToClassWithCardinality() {
		withInputModel("referenceToClassWithCardinality", "PersonList.ecore")
		loadModelFile
		val ref = refactorings.getEReference("PersonList", "Person", "works")
		refactorings.referenceToClass("WorkingPosition", ref)
		refactorings.saveModifiedEcores(MODIFIED)
		assertModifiedFile
	}

	@Test
	def void test_referenceToClassUnidirectional() {
		withInputModel("referenceToClassUnidirectional", "PersonList.ecore")
		loadModelFile
		val ref = refactorings.getEReference("PersonList", "Person", "works")
		refactorings.referenceToClass("WorkingPosition", ref)
		refactorings.saveModifiedEcores(MODIFIED)
		assertModifiedFile
	}

	@Test
	def void test_referenceToClassWithContainmentReference() {
		withInputModel("referenceToClassWithContainmentReference", "PersonList.ecore")
		loadModelFile
		val ref = refactorings.getEReference("PersonList", "Person", "works")
		refactorings.referenceToClass("WorkingPosition", ref)
		refactorings.saveModifiedEcores(MODIFIED)
		assertModifiedFileIsSameAsOriginal
		assertThat(appender.result.trim)
			.isEqualTo("ERROR: PersonList.Person.works: Cannot apply referenceToClass on containment reference: PersonList.Person.works")
	}

	@Test
	def void test_classToReferenceWhenClassIsNotReferred() {
		val ePackage = factory.createEPackage => [
			name = "p"
		]
		val c = ePackage.createEClass("C")
		refactorings.classToReference(c)
		assertThat(appender.result.trim)
			.isEqualTo("ERROR: p.C: The EClass is not referred: p.C")
	}

	@Test
	def void test_classToReferenceWhenClassIsReferredMoreThanOnce() {
		val ePackage = factory.createEPackage => [
			name = "p"
		]
		val c = ePackage.createEClass("C")
		ePackage.createEClass("C1") => [
			createEReference("r1") => [
				containment = true
				EType = c
			]
		]
		ePackage.createEClass("C2") => [
			createEReference("r2") => [
				containment = true
				EType = c
			]
		]
		refactorings.classToReference(c)
		assertThat(appender.result)
			.isEqualTo('''
			ERROR: p.C: The EClass is referred by more than one container:
			  p.C1.r1
			  p.C2.r2
			'''.toString)
	}

	@Test
	def void test_classToReferenceUnidirectional() {
		withInputModel("classToReferenceUnidirectional", "PersonList.ecore")
		loadModelFile
		val cl = refactorings.getEClass("PersonList", "WorkingPosition")
		refactorings.classToReference(cl)
		refactorings.saveModifiedEcores(MODIFIED)
		assertModifiedFile
	}

	@Test
	def void test_classToReferenceWithMissingTarget() {
		withInputModel("classToReferenceUnidirectional", "PersonList.ecore")
		loadModelFile
		val cl = refactorings.getEClass("PersonList", "WorkingPosition")
		// manually remove reference to target class WorkPlace
		cl.EStructuralFeatures -= cl.getEStructuralFeature("workPlace")
		refactorings.classToReference(cl)
		assertThat(appender.result.trim)
			.isEqualTo("ERROR: PersonList.WorkingPosition: Missing reference to target type: PersonList.WorkingPosition")
	}

	@Test
	def void test_classToReferenceWithTooManyTargets() {
		withInputModel("classToReferenceUnidirectional", "PersonList.ecore")
		loadModelFile
		val cl = refactorings.getEClass("PersonList", "WorkingPosition")
		// manually add another reference to target class
		cl.createEReference("another") => [
			EType = refactorings.getEClass("PersonList", "List")
		]
		refactorings.classToReference(cl)
		assertThat(appender.result)
			.isEqualTo('''
			ERROR: PersonList.WorkingPosition: Too many references to target type:
			  PersonList.WorkingPosition.workPlace
			  PersonList.WorkingPosition.another
		  '''.toString)
	}

	@Test
	def void test_classToReferenceUnidirectionalWithoutOppositeIsOk() {
		withInputModel("classToReferenceUnidirectional", "PersonList.ecore")
		loadModelFile
		val cl = refactorings.getEClass("PersonList", "WorkingPosition")
		// manually remove the opposite reference
		cl.EStructuralFeatures -= cl.getEStructuralFeature("person") => [
			// also appropriately update the opposite, otherwise we have a dangling reference
			(it as EReference).EOpposite.EOpposite = null
		]
		refactorings.classToReference(cl)
		refactorings.saveModifiedEcores(MODIFIED)
		assertModifiedFile
	}

	@Test
	def void test_classToReferenceBidirectional() {
		withInputModel("classToReferenceBidirectional", "PersonList.ecore")
		loadModelFile
		val cl = refactorings.getEClass("PersonList", "WorkingPosition")
		refactorings.classToReference(cl)
		refactorings.saveModifiedEcores(MODIFIED)
		assertModifiedFile
	}

	@Test
	def void test_classToReferenceWithCardinality() {
		withInputModel("classToReferenceWithCardinality", "PersonList.ecore")
		loadModelFile
		val cl = refactorings.getEClass("PersonList", "WorkingPosition")
		refactorings.classToReference(cl)
		refactorings.saveModifiedEcores(MODIFIED)
		assertModifiedFile
	}

	@Test
	def void test_referenceToClass_IsOppositeOf_classToReferenceUnidirectional() {
		withInputModel("referenceToClassUnidirectional", "PersonList.ecore")
		assertOppositeRefactorings(
			[refactorings.referenceToClass("WorkingPosition",
				refactorings.getEReference("PersonList", "Person", "works"))],
			[refactorings.classToReference(
				refactorings.getEClass("PersonList", "WorkingPosition"))]
		)
		assertLogIsEmpty
	}

	@Test
	def void test_referenceToClass_IsOppositeOf_classToReferenceUnidirectional2() {
		withInputModel("classToReferenceUnidirectional", "PersonList.ecore")
		assertOppositeRefactorings(
			[refactorings.classToReference(
				refactorings.getEClass("PersonList", "WorkingPosition"))],
			[refactorings.referenceToClass("WorkingPosition",
				refactorings.getEReference("PersonList", "Person", "works"))]
		)
		assertLogIsEmpty
	}

	@Test
	def void test_referenceToClass_IsOppositeOf_classToReferenceBidirectional() {
		withInputModel("referenceToClassBidirectional", "PersonList.ecore")
		assertOppositeRefactorings(
			[refactorings.referenceToClass("WorkingPosition",
				refactorings.getEReference("PersonList", "Person", "works"))],
			[refactorings.classToReference(
				refactorings.getEClass("PersonList", "WorkingPosition"))]
		)
		assertLogIsEmpty
	}

	@Test
	def void test_referenceToClass_IsOppositeOf_classToReferenceBidirectional2() {
		withInputModel("classToReferenceBidirectional", "PersonList.ecore")
		assertOppositeRefactorings(
			[refactorings.classToReference(
				refactorings.getEClass("PersonList", "WorkingPosition"))],
			[refactorings.referenceToClass("WorkingPosition",
				refactorings.getEReference("PersonList", "Person", "works"))]
		)
		assertLogIsEmpty
	}

	@Test def void test_extractSuperClass() {
		val p = factory.createEPackage => [
			createEClass("C1") => [
				createEAttribute("A1") => [
					EType = stringDataType
				]
			]
			createEClass("C2") => [
				createEAttribute("A1") => [
					EType = stringDataType
				]
			]
		]
		// we know the duplicates, manually
		val duplicates = p.EClasses.map[EAttributes].flatten.toList
		refactorings.extractSuperclass(duplicates)
		val classifiersNames = p.EClassifiers.map[name]
		assertThat(classifiersNames)
			.hasSize(3)
			.containsExactly("C1", "C2", "A1Element")
		val classes = p.EClasses
		assertThat(classes.get(0).EAttributes).isEmpty
		assertThat(classes.get(1).EAttributes).isEmpty
		assertThat(classes.get(2).EAttributes).hasSize(1)
		val extracted = classes.get(2).EAttributes.head
		assertThat(extracted.name).isEqualTo("A1")
		assertThat(extracted.EAttributeType).isEqualTo(stringDataType)
	}

	@Test def void test_extractSuperClassUnique() {
		val p = factory.createEPackage => [
			createEClass("C1") => [
				createEAttribute("A1") => [
					EType = stringDataType
				]
			]
			createEClass("C2") => [
				createEAttribute("A1") => [
					EType = stringDataType
				]
			]
			createEClass("C3") => [
				createEAttribute("A1") => [
					EType = stringDataType
					lowerBound = 2
				]
			]
			createEClass("C4") => [
				createEAttribute("A1") => [
					EType = stringDataType
					lowerBound = 2
				]
			]
		]
		// we know the duplicates, manually
		val attributes = p.EClasses.map[EAttributes].flatten.toList
		refactorings.extractSuperclass(
			attributes.take(2).toList // A1 without lowerbound
		)
		refactorings.extractSuperclass(
			attributes.stream.skip(2).collect(Collectors.toList) // A1 with lowerbound
		)
		val classifiersNames = p.EClassifiers.map[name]
		assertThat(classifiersNames)
			.containsExactly("C1", "C2", "C3", "C4", "A1Element", "A1Element1")
		val classes = p.EClasses
		assertThat(classes.get(0).EAttributes).isEmpty // C1
		assertThat(classes.get(1).EAttributes).isEmpty // C2
		assertThat(classes.get(2).EAttributes).isEmpty // C3
		assertThat(classes.get(3).EAttributes).isEmpty // C4
		assertThat(classes.get(4).EAttributes).hasSize(1) // WithA1EString
		assertThat(classes.get(5).EAttributes).hasSize(1) // WithA1EString1
		val extractedA1NoLowerBound = classes.get(4).EAttributes.head
		assertThat(extractedA1NoLowerBound.name).isEqualTo("A1")
		assertThat(extractedA1NoLowerBound.EAttributeType).isEqualTo(stringDataType)
		assertThat(extractedA1NoLowerBound.lowerBound).isZero
		val extractedA1WithLowerBound = classes.get(5).EAttributes.head
		assertThat(extractedA1WithLowerBound.name).isEqualTo("A1")
		assertThat(extractedA1WithLowerBound.EAttributeType).isEqualTo(stringDataType)
		assertThat(extractedA1WithLowerBound.lowerBound).isEqualTo(2)
	}

	@Test
	def void test_pullUpFeatures() {
		withInputModel("pullUpFeatures", "PersonList.ecore")
		loadModelFile
		val person = refactorings.getEClass("PersonList", "Person")
		val student = refactorings.getEClass("PersonList", "Student")
		val employee = refactorings.getEClass("PersonList", "Employee")
		refactorings.pullUpFeatures(person,
			#[student.getEStructuralFeature("name"),employee.getEStructuralFeature("name")]
		)
		refactorings.saveModifiedEcores(MODIFIED)
		assertModifiedFile
	}

	@Test
	def void test_pullUpFeaturesDifferent() {
		withInputModel("pullUpFeaturesDifferent", "PersonList.ecore")
		loadModelFile
		val person = refactorings.getEClass("PersonList", "Person")
		val student = refactorings.getEClass("PersonList", "Student")
		val employee = refactorings.getEClass("PersonList", "Employee")
		refactorings.pullUpFeatures(person,
			#[student.getEStructuralFeature("name"),employee.getEStructuralFeature("name")]
		)
		refactorings.saveModifiedEcores(MODIFIED)
		assertModifiedFileIsSameAsOriginal
		assertThat(appender.result)
			.isEqualTo(
			'''
			ERROR: PersonList.Employee.name: The two features are not equal:
			ecore.ETypedElement.lowerBound:
			  PersonList.Student.name: 0
			  PersonList.Employee.name: 1
			
			'''.toString)
	}

	@Test
	def void test_pullUpFeaturesNotSubclass() {
		withInputModel("pullUpFeaturesNotSubclass", "PersonList.ecore")
		loadModelFile
		val person = refactorings.getEClass("PersonList", "Person")
		val student = refactorings.getEClass("PersonList", "Student")
		val employee = refactorings.getEClass("PersonList", "Employee")
		refactorings.pullUpFeatures(person,
			#[student.getEStructuralFeature("name"),employee.getEStructuralFeature("name")]
		)
		refactorings.saveModifiedEcores(MODIFIED)
		assertModifiedFileIsSameAsOriginal
		assertThat(appender.result)
			.isEqualTo(
			'''
			ERROR: PersonList.Student.name: Not a direct subclass of destination: PersonList.Student
			ERROR: PersonList.Employee.name: Not a direct subclass of destination: PersonList.Employee
			'''.toString)
	}

	@Test def void test_makeAbstract() {
		val p = factory.createEPackage => [
			val base = createEClass("ConcreteAbstractMetaclass")
			createEClass("Derived1") => [
				ESuperTypes += base
			]
		]
		val c = p.EClasses.head
		assertFalse(c.abstract)
		refactorings.makeAbstract(#[c])
		assertTrue(c.abstract)
	}

	@Test def void test_makeConcrete() {
		val p = factory.createEPackage => [
			createEClass("AbstractConcreteMetaclass") => [
				abstract = true
			]
		]
		val c = p.EClasses.head
		assertTrue(c.abstract)
		refactorings.makeConcrete(#[c])
		assertFalse(c.abstract)
	}
}
