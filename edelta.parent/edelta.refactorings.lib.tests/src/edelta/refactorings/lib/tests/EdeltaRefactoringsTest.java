package edelta.refactorings.lib.tests;

import static edelta.lib.EdeltaEcoreUtil.getValueAsList;
import static edelta.lib.EdeltaUtils.getEObjectRepr;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.List.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.xtext.xbase.lib.IterableExtensions.head;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.assertj.core.api.Assertions;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.ENamedElement;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import edelta.lib.EdeltaDefaultRuntime;
import edelta.lib.EdeltaEcoreUtil;
import edelta.lib.EdeltaModelManager;
import edelta.refactorings.lib.EdeltaRefactorings;
import edelta.refactorings.lib.tests.utils.InMemoryLoggerAppender;
import edelta.testutils.EdeltaTestUtils;

class EdeltaRefactoringsTest extends AbstractEdeltaRefactoringsLibTest {
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
		EdeltaTestUtils.cleanDirectory(AbstractEdeltaRefactoringsLibTest.MODIFIED);
	}

	private void withInputModels(String testModelDirectory, String... testModelFiles) {
		this.testModelDirectory = testModelDirectory;
		this.testModelFiles = Arrays.asList(testModelFiles);
	}

	private void loadEcoreFiles() {
		checkInputModelSettings();
		for (String testModelFile : testModelFiles) {
			modelManager
				.loadEcoreFile(AbstractEdeltaRefactoringsLibTest.TESTECORES +
					testModelDirectory + "/" + testModelFile);
		}
	}

	private void assertModifiedFiles() throws IOException {
		checkInputModelSettings();
		// no need to explicitly validate:
		// if the ecore file is saved then it is valid
		for (String testModelFile : testModelFiles) {
			EdeltaTestUtils.assertFilesAreEquals(
					AbstractEdeltaRefactoringsLibTest.EXPECTATIONS + testModelDirectory + "/" + testModelFile,
					AbstractEdeltaRefactoringsLibTest.MODIFIED + testModelFile);
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
				AbstractEdeltaRefactoringsLibTest.TESTECORES +
				testModelDirectory +
				"/" +
				testModelFile,
				AbstractEdeltaRefactoringsLibTest.MODIFIED + testModelFile);
		}
	}

	private void assertOppositeRefactorings(final Runnable first, final Runnable second) throws IOException {
		loadEcoreFiles();
		first.run();
		modelManager.saveEcores(AbstractEdeltaRefactoringsLibTest.MODIFIED);
		second.run();
		modelManager.saveEcores(AbstractEdeltaRefactoringsLibTest.MODIFIED);
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
	void test_changeToSingle() throws Exception {
		var subdir = "toUpperCaseStringAttributesMultiple/";
		var ecores = of("My.ecore");
		var models = of("MyClass.xmi", "MyClass2.xmi", "MyClass3.xmi");

		var engine = setupEngine(
			subdir,
			ecores,
			models,
			other -> new EdeltaRefactorings(other) {
				@Override
				protected void doExecute() {
					var attribute = getEAttribute(
							"mypackage", "MyClass", "myAttribute");

					changeToSingle(attribute);
				}
			}
		);

		assertOutputs(
			engine,
			"makeSingle/",
			ecores,
			models
		);
	}

	@Test
	void test_changeToMultiple() throws Exception {
		var subdir = "toUpperCaseStringAttributes/";
		var ecores = of("My.ecore");
		var models = of("MyClass.xmi", "MyClass2.xmi", "MyClass3.xmi");

		var engine = setupEngine(
			subdir,
			ecores,
			models,
			other -> new EdeltaRefactorings(other) {
				@Override
				protected void doExecute() {
					var attribute = getEAttribute(
							"mypackage", "MyClass", "myAttribute");

					changeToMultiple(attribute);
				}
			}
		);

		assertOutputs(
			engine,
			"makeMultiple/",
			ecores,
			models
		);
	}

	@Test
	void test_changeUpperBound() throws Exception {
		var subdir = "toUpperCaseStringAttributesMultiple/";
		var ecores = of("My.ecore");
		var models = of("MyClass.xmi", "MyClass2.xmi", "MyClass3.xmi");

		var engine = setupEngine(
			subdir,
			ecores,
			models,
			other -> new EdeltaRefactorings(other) {
				@Override
				protected void doExecute() {
					var attribute = getEAttribute(
							"mypackage", "MyClass", "myAttribute");

					changeUpperBound(attribute, 2);
				}
			}
		);

		assertOutputs(
			engine,
			"makeMultipleTo2/",
			ecores,
			models
		);
	}

	@Test
	void test_mergeAttributes() throws Exception {
		var subdir = "mergeAttributes/";
		var ecores = of("PersonList.ecore");
		var models = of("List.xmi");

		var engine = setupEngine(
			subdir,
			ecores,
			models,
			other -> new EdeltaRefactorings(other) {
				@Override
				protected void doExecute() {
					var person = getEClass("PersonList", "Person");
					mergeAttributes(
						"name",
						asList(
							(EAttribute) person.getEStructuralFeature("firstName"),
							(EAttribute) person.getEStructuralFeature("lastName")),
						values -> {
							var merged = values.stream()
								.filter(Objects::nonNull)
								.map(Object::toString)
								.collect(Collectors.joining(" "));
							return merged.isEmpty() ? null : merged;
						}
					);
				}
			}
		);

		assertOutputs(
			engine,
			subdir,
			ecores,
			models
		);
	}

	@Test
	void test_mergeReferences() throws Exception {
		var subdir = "mergeFeaturesContainment/";
		var ecores = of("PersonList.ecore");
		var models = of("List.xmi");

		var engine = setupEngine(
			subdir,
			ecores,
			models,
			other -> new EdeltaRefactorings(other) {
				@Override
				protected void doExecute() {
					EClass person = getEClass("PersonList", "Person");
					EClass nameElement = getEClass("PersonList", "NameElement");
					EAttribute nameElementAttribute =
							getEAttribute("PersonList", "NameElement", "nameElementValue");
					assertNotNull(nameElementAttribute);

					mergeReferences(
						"name",
						asList(
							(EReference) person.getEStructuralFeature("firstName"),
							(EReference) person.getEStructuralFeature("lastName")),
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
				}
			}
		);

		assertOutputs(
			engine,
			subdir,
			ecores,
			models
		);
	}

	@Test
	void test_mergeReferencesWithPostCopy() throws Exception {
		var subdir = "mergeFeaturesNonContainmentShared/";
		var ecores = of("PersonList.ecore");
		var models = of("List.xmi");

		var engine = setupEngine(
			subdir,
			ecores,
			models,
			other -> new EdeltaRefactorings(other) {
				@Override
				protected void doExecute() {
					EClass person = getEClass("PersonList", "Person");
					EClass nameElement = getEClass("PersonList", "NameElement");
					EAttribute nameElementAttribute =
							getEAttribute("PersonList", "NameElement", "nameElementValue");
					assertNotNull(nameElementAttribute);

					// keep track of objects that are merged into a single one
					var merged = new HashMap<Collection<EObject>, EObject>();

					mergeReferences(
						"name",
						asList(
							(EReference) person.getEStructuralFeature("firstName"),
							(EReference) person.getEStructuralFeature("lastName")),
						values -> {
							// it is responsibility of the merger to create an instance
							// of the (now single) referred object with the result
							// of merging the original objects' values
							if (values.isEmpty())
								return null;

							var alreadyMerged = merged.get(values);
							if (alreadyMerged != null)
								return alreadyMerged;
							// we have already processed the object collection
							// and created a merged one so we reuse it

							EObject firstObject = values.iterator().next();
							var containingFeature = firstObject.eContainingFeature();
							List<EObject> containerCollection =
								getValueAsList(firstObject.eContainer(), containingFeature);

							var mergedValue = values.stream()
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
									merged.put(values, o);
								}
							);
						},
						// now we can remove the stale objects that have been merged
						() -> EcoreUtil.removeAll(
								merged.keySet().stream()
									.flatMap(Collection<EObject>::stream)
									.collect(Collectors.toList()))
					);
				}
			}
		);

		assertOutputs(
			engine,
			subdir,
			ecores,
			models
		);
	}

	@Test
	void test_mergeFeaturesDifferent() throws IOException {
		withInputModels("mergeFeaturesDifferent", "PersonList.ecore");
		loadEcoreFiles();
		final EClass person = refactorings.getEClass("PersonList", "Person");
		assertThrowsIAE(() -> refactorings.mergeFeatures("name",
			asList(
				person.getEStructuralFeature("firstName"),
				person.getEStructuralFeature("lastName"))));
		modelManager.saveEcores(AbstractEdeltaRefactoringsLibTest.MODIFIED);
		assertModifiedFilesAreSameAsOriginal();
		final EClass student = refactorings.getEClass("PersonList", "Student");
		assertThrowsIAE(() -> refactorings.mergeFeatures("name",
			asList(
				person.getEStructuralFeature("lastName"),
				student.getEStructuralFeature("lastName"))));
		modelManager.saveEcores(AbstractEdeltaRefactoringsLibTest.MODIFIED);
		assertModifiedFilesAreSameAsOriginal();
		assertThrowsIAE(() -> refactorings.mergeFeatures("name",
			asList(
				person.getEStructuralFeature("list"),
				person.getEStructuralFeature("lastName"))));
		modelManager.saveEcores(AbstractEdeltaRefactoringsLibTest.MODIFIED);
		assertModifiedFilesAreSameAsOriginal();
		assertEquals("""
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
		
		""", appender.getResult());
	}

	@Test
	void test_mergeFeatures2() throws IOException {
		withInputModels("mergeFeatures2", "PersonList.ecore");
		loadEcoreFiles();
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
		modelManager.saveEcores(AbstractEdeltaRefactoringsLibTest.MODIFIED);
		assertModifiedFiles();
	}

	@Test
	void test_mergeFeatures2NonCompliant() {
		withInputModels("mergeFeatures2", "PersonList.ecore");
		loadEcoreFiles();
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
		assertEquals("""
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
			""",
			appender.getResult());
	}

	@Test
	void test_mergeFeatures3() throws IOException {
		withInputModels("mergeFeatures3", "PersonList.ecore");
		loadEcoreFiles();
		final EClass list = refactorings.getEClass("PersonList", "List");
		final EClass place = refactorings.getEClass("PersonList", "Place");
		final EClass person = refactorings.getEClass("PersonList", "Person");
		refactorings.mergeFeatures("places", place,
			asList(
				list.getEStructuralFeature("wplaces"),
				list.getEStructuralFeature("lplaces")));
		refactorings.mergeFeatures("name", this.stringDataType,
			asList(
				person.getEStructuralFeature("firstName"),
				person.getEStructuralFeature("lastName")));
		modelManager.saveEcores(AbstractEdeltaRefactoringsLibTest.MODIFIED);
		assertModifiedFiles();
	}

	@Test
	void test_splitAttribute() throws Exception {
		var subdir = "splitAttributes/";
		var ecores = of("PersonList.ecore");
		var models = of("List.xmi");

		var engine = setupEngine(
			subdir,
			ecores,
			models,
			other -> new EdeltaRefactorings(other) {
				@Override
				protected void doExecute() {
					final EClass person = getEClass("PersonList", "Person");
					var personName = (EAttribute) person.getEStructuralFeature("name");
					splitAttribute(
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
						}
					);
				}
			}
		);

		assertOutputs(
			engine,
			subdir,
			ecores,
			models
		);
	}

	@Test
	void test_splitFeature() throws Exception {
		var subdir = "splitFeatureNonContainmentShared/";
		var ecores = of("PersonList.ecore");
		var models = of("List.xmi");

		var engine = setupEngine(
			subdir,
			ecores,
			models,
			other -> new EdeltaRefactorings(other) {
				@Override
				protected void doExecute() {
					final EClass person = getEClass("PersonList", "Person");
					var personName = (EReference) person.getEStructuralFeature("name");
					EClass nameElement = getEClass("PersonList", "NameElement");
					EAttribute nameElementAttribute =
							getEAttribute("PersonList", "NameElement", "nameElementValue");
					assertNotNull(nameElementAttribute);

					// keep track of objects that are splitted into several ones
					var splitted = new HashMap<EObject, Collection<EObject>>();

					splitReference(
						personName,
						asList(
							"firstName",
							"lastName"),
						obj -> {
							// a few more checks should be performed in a realistic context
							if (obj == null)
								return Collections.emptyList();

							var alreadySplitted = splitted.get(obj);
							if (alreadySplitted != null)
								return alreadySplitted;
							// we have already processed the object collection
							// and created a merged one so we reuse it

							var containingFeature = obj.eContainingFeature();
							List<EObject> containerCollection =
								getValueAsList(obj.eContainer(), containingFeature);

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
				}
			}
		);

		assertOutputs(
			engine,
			subdir,
			ecores,
			models
		);
	}

	@Test
	void test_mergeAttributes_IsOppositeOf_splitAttribute() throws Exception {
		var subdir = "mergeAndSplitAttributes/";
		var ecores = of("PersonList.ecore");
		var models = of("List.xmi");

		var engine = setupEngine(
			subdir,
			ecores,
			models,
			other -> new EdeltaRefactorings(other) {
				@Override
				protected void doExecute() {
					final EClass person = getEClass("PersonList", "Person");
					var personFirstName = (EAttribute) person.getEStructuralFeature("firstName");
					var personLastName = (EAttribute) person.getEStructuralFeature("lastName");
					var mergedFeature = mergeAttributes(
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
						});
					splitAttribute(
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
						}
					);
				}
			}
		);

		assertOutputs(
			engine,
			subdir,
			ecores,
			models
		);
	}

	/**
	 * The evolved metamodel and model are just the same as the original ones, as
	 * long the merge and split can be inversed. For example, in this test we have
	 * "firstname lastname" or no string at all. If you had "lastname" then the
	 * model wouldn't be reversable.
	 * 
	 * The input directory and the output one will contain the same data.
	 */
	@Test
	void test_splitAttribute_IsOppositeOf_mergeAttributes() throws Exception {
		var subdir = "splitAndMergeAttributes/";
		var ecores = of("PersonList.ecore");
		var models = of("List.xmi");

		var engine = setupEngine(
			subdir,
			ecores,
			models,
			other -> new EdeltaRefactorings(other) {
				@Override
				protected void doExecute() {
					final EClass person = getEClass("PersonList", "Person");
					var personName = (EAttribute) person.getEStructuralFeature("name");
					var splitFeatures = splitAttribute(
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
						}
					);
					mergeAttributes(
						"name",
						splitFeatures,
						values -> {
							var merged = values.stream()
								.filter(Objects::nonNull)
								.map(Object::toString)
								.collect(Collectors.joining(" "));
							return merged.isEmpty() ? null : merged;
						}
					);
				}
			}
		);

		assertOutputs(
			engine,
			subdir,
			ecores,
			models
		);
	}

	@Test
	void test_mergeReferences_IsOppositeOf_splitReference() throws Exception {
		var subdir = "mergeFeaturesNonContainmentShared/";
		var ecores = of("PersonList.ecore");
		var models = of("List.xmi");

		var engine = setupEngine(
			subdir,
			ecores,
			models,
			other -> new EdeltaRefactorings(other) {
				@Override
				protected void doExecute() {
					EClass person = getEClass("PersonList", "Person");
					EClass nameElement = getEClass("PersonList", "NameElement");
					EAttribute nameElementAttribute =
							getEAttribute("PersonList", "NameElement", "nameElementValue");
					var personFirstName = (EReference) person.getEStructuralFeature("firstName");
					var personLastName = (EReference) person.getEStructuralFeature("lastName");
					assertNotNull(nameElementAttribute);

					// keep track of objects that are merged into a single one
					var merged = new HashMap<Collection<EObject>, EObject>();

					var mergedFeature =  mergeReferences(
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

							var alreadyMerged = merged.get(values);
							if (alreadyMerged != null)
								return alreadyMerged;
							// we have already processed the object collection
							// and created a merged one so we reuse it

							EObject firstObject = values.iterator().next();
							var containingFeature = firstObject.eContainingFeature();
							List<EObject> containerCollection =
								getValueAsList(firstObject.eContainer(), containingFeature);

							var mergedValue = values.stream()
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
									merged.put(values, o);
								}
							);
						},
						// now we can remove the stale objects that have been merged
						() -> EcoreUtil.removeAll(
								merged.keySet().stream()
									.flatMap(Collection<EObject>::stream)
									.collect(Collectors.toList()))
					);

					// keep track of objects that are split into several ones
					var splitted = new HashMap<EObject, Collection<EObject>>();

					splitReference(
						mergedFeature,
						asList(
							personFirstName.getName(),
							personLastName.getName()),
						obj -> {
							// a few more checks should be performed in a realistic context
							if (obj == null)
								return Collections.emptyList();

							var alreadySplitted = splitted.get(obj);
							if (alreadySplitted != null)
								return alreadySplitted;
							// we have already processed the object collection
							// and created a merged one so we reuse it

							var containingFeature = obj.eContainingFeature();
							List<EObject> containerCollection =
								getValueAsList(obj.eContainer(), containingFeature);

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
				}
			}
		);

		assertOutputs(
			engine,
			"splitFeatureNonContainmentShared/",
			ecores,
			models
		);
	}

	@Test
	void test_splitReference_IsOppositeOf_mergeReferences() throws Exception {
		var subdir = "splitFeatureNonContainmentShared/";
		var ecores = of("PersonList.ecore");
		var models = of("List.xmi");

		var engine = setupEngine(
			subdir,
			ecores,
			models,
			other -> new EdeltaRefactorings(other) {
				@Override
				protected void doExecute() {
					final EClass person = getEClass("PersonList", "Person");
					var personName = (EReference) person.getEStructuralFeature("name");
					EClass nameElement = getEClass("PersonList", "NameElement");
					EAttribute nameElementAttribute =
							getEAttribute("PersonList", "NameElement", "nameElementValue");
					assertNotNull(nameElementAttribute);

					// keep track of objects that are splitted into several ones
					var splitted = new HashMap<EObject, Collection<EObject>>();

					var splitFeatures = splitReference(
						personName,
						asList(
							"firstName",
							"lastName"),
						obj -> {
							// a few more checks should be performed in a realistic context
							if (obj == null)
								return Collections.emptyList();

							var alreadySplitted = splitted.get(obj);
							if (alreadySplitted != null)
								return alreadySplitted;
							// we have already processed the object collection
							// and created a merged one so we reuse it

							var containingFeature = obj.eContainingFeature();
							List<EObject> containerCollection =
								getValueAsList(obj.eContainer(), containingFeature);

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

					mergeReferences(
						"name",
						splitFeatures.stream().map(EReference.class::cast).collect(Collectors.toList()),
						values -> {
							// it is responsibility of the merger to create an instance
							// of the (now single) referred object with the result
							// of merging the original objects' values
							if (values.isEmpty())
								return null;

							var alreadyMerged = merged.get(values);
							if (alreadyMerged != null)
								return alreadyMerged;
							// we have already processed the object collection
							// and created a merged one so we reuse it

							EObject firstObject = values.iterator().next();
							var containingFeature = firstObject.eContainingFeature();
							List<EObject> containerCollection =
								getValueAsList(firstObject.eContainer(), containingFeature);

							var mergedValue = values.stream()
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
									merged.put(values, o);
								}
							);
						},
						// now we can remove the stale objects that have been merged
						() -> EcoreUtil.removeAll(
								merged.keySet().stream()
									.flatMap(Collection<EObject>::stream)
									.collect(Collectors.toList()))
					);
				}
			}
		);

		assertOutputs(
			engine,
			"mergeFeaturesNonContainmentShared/",
			ecores,
			models
		);
	}

	/**
	 * This also implicitly tests introduceSubclasses
	 * 
	 * @throws Exception
	 */
	@Test
	void test_enumToSubclasses() throws Exception {
		var subdir = "enumToSubclasses/";
		var ecores = of("PersonList.ecore");
		var models = of("List.xmi");

		var engine = setupEngine(
			subdir,
			ecores,
			models,
			other -> new EdeltaRefactorings(other) {
				@Override
				protected void doExecute() {
					var person = getEClass("PersonList", "Person");
					var genreAttribute = (EAttribute) person.getEStructuralFeature("gender");
					var subclasses = enumToSubclasses(genreAttribute);
					assertThat(subclasses)
						.extracting(EClass::getName)
						.containsExactlyInAnyOrder("Male", "Female");
				}
			}
		);

		assertOutputs(
			engine,
			subdir,
			ecores,
			models
		);
	}

	@Test
	void test_enumToSubclassesNotAnEEnum() throws IOException {
		withInputModels("enumToSubclasses", "PersonList.ecore");
		loadEcoreFiles();
		final EClass person = refactorings.getEClass("PersonList", "Person");
		final Collection<EClass> result = 
			refactorings.enumToSubclasses(
				((EAttribute) person.getEStructuralFeature("firstname")));
		assertThat(result).isNull();
		modelManager.saveEcores(AbstractEdeltaRefactoringsLibTest.MODIFIED);
		assertModifiedFilesAreSameAsOriginal();
		assertThat(appender.getResult().trim())
			.isEqualTo("ERROR: PersonList.Person.firstname: Not an EEnum: ecore.EString");
	}

	@Test
	void test_subclassesToEnum() throws Exception {
		var subdir = "subclassesToEnum/";
		var ecores = of("PersonList.ecore");
		var models = of("List.xmi");

		var engine = setupEngine(
			subdir,
			ecores,
			models,
			other -> new EdeltaRefactorings(other) {
				@Override
				protected void doExecute() {
					var personList = getEPackage("PersonList");
					var result = subclassesToEnum("Gender",
						asList(
							(EClass) personList.getEClassifier("Male"),
							(EClass) personList.getEClassifier("Female")));
					assertThat(result)
						.returns("gender", EAttribute::getName);
				}
			}
		);

		assertOutputs(
			engine,
			subdir,
			ecores,
			models
		);
	}

	@Test
	void test_subclassesToEnumSubclassesNotEmpty() throws IOException {
		withInputModels("subclassesToEnumSubclassesNotEmpty", "PersonList.ecore");
		loadEcoreFiles();
		final EPackage personList = refactorings.getEPackage("PersonList");
		assertThrowsIAE(() -> refactorings.subclassesToEnum("Gender",
			asList(
				(EClass) personList.getEClassifier("Male"),
				(EClass) personList.getEClassifier("NotSpecified"),
				(EClass) personList.getEClassifier("Female"))));
		modelManager.saveEcores(AbstractEdeltaRefactoringsLibTest.MODIFIED);
		assertModifiedFilesAreSameAsOriginal();
		assertThat(appender.getResult().trim())
			.isEqualTo(
			"""
				ERROR: PersonList.Male: Not an empty class: PersonList.Male:
				  PersonList.Male.maleName
				ERROR: PersonList.Female: Not an empty class: PersonList.Female:
				  PersonList.Female.femaleName""");
	}

	@Test
	void test_subclassesToEnumSubclassesWrongSubclasses() throws IOException {
		withInputModels("subclassesToEnumSubclassesWrongSubclasses", "PersonList.ecore");
		loadEcoreFiles();
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
		modelManager.saveEcores(AbstractEdeltaRefactoringsLibTest.MODIFIED);
		assertModifiedFilesAreSameAsOriginal();
		assertThat(appender.getResult().trim())
			.isEqualTo(
			"""
				ERROR: PersonList.FemaleEmployee: Expected one superclass: PersonList.FemaleEmployee instead of:
				  PersonList.Person
				  PersonList.Employee
				ERROR: PersonList.Employee: Expected one superclass: PersonList.Employee instead of:
				  empty
				ERROR: PersonList.AnotherFemale: Wrong superclass of PersonList.AnotherFemale:
				  Expected: PersonList.Person
				  Actual  : PersonList.AnotherPerson
				ERROR: PersonList.Person: The class PersonList.Person has additional subclasses:
				  PersonList.Male
				  PersonList.FemaleEmployee""");
	}

	@Test
	void test_subclassesToEnumSubclassesWrongSubclassesParallel() throws IOException {
		withInputModels("subclassesToEnumSubclassesWrongSubclassesParallel",
				"PersonList.ecore", "PersonListReferring.ecore");
		loadEcoreFiles();
		var personList = refactorings.getEPackage("PersonList");
		var female = (EClass) personList.getEClassifier("Female");
		assertThrowsIAE(() -> refactorings.subclassesToEnum("Gender",
				asList(female)));
		modelManager.saveEcores(AbstractEdeltaRefactoringsLibTest.MODIFIED);
		assertModifiedFilesAreSameAsOriginal();
		assertThat(appender.getResult().trim())
			.isEqualTo(
			"""
				ERROR: PersonList.Person: The class PersonList.Person has additional subclasses:
				  PersonList.Male
				  PersonListReferring.FemaleEmployee""");
	}

	@Test
	void test_enumToSubclasses_IsOppositeOf_subclassesToEnum() throws Exception {
		var subdir = "enumToSubclasses/";
		var ecores = of("PersonList.ecore");
		var models = of("List.xmi");

		var engine = setupEngine(
			subdir,
			ecores,
			models,
			other -> new EdeltaRefactorings(other) {
				@Override
				protected void doExecute() {
					var person = getEClass("PersonList", "Person");
					var genreAttribute = (EAttribute) person.getEStructuralFeature("gender");
					var subclasses = enumToSubclasses(genreAttribute);
					assertThat(subclasses)
						.extracting(EClass::getName)
						.containsExactlyInAnyOrder("Male", "Female");
					// inverse
					var result = subclassesToEnum("Gender",
						subclasses);
					assertThat(result)
						.returns("gender", EAttribute::getName);
				}
			}
		);

		assertOutputs(
			engine,
			"subclassesToEnum/",
			ecores,
			models
		);
	}

	@Test
	void test_subclassesToEnum_IsOppositeOf_enumToSubclasses() throws Exception {
		var subdir = "subclassesToEnum/";
		var ecores = of("PersonList.ecore");
		var models = of("List.xmi");

		var engine = setupEngine(
			subdir,
			ecores,
			models,
			other -> new EdeltaRefactorings(other) {
				@Override
				protected void doExecute() {
					var personList = getEPackage("PersonList");
					var result = subclassesToEnum("Gender",
						asList(
							(EClass) personList.getEClassifier("Male"),
							(EClass) personList.getEClassifier("Female")));
					assertThat(result)
						.returns("gender", EAttribute::getName);
					// inverse
					var subclasses = enumToSubclasses(result);
					assertThat(subclasses)
						.extracting(EClass::getName)
						.containsExactlyInAnyOrder("Male", "Female");
				}
			}
		);

		assertOutputs(
			engine,
			"enumToSubclasses/",
			ecores,
			models
		);
	}

	@Test
	void test_extractClassWithAttributes() throws IOException {
		withInputModels("extractClassWithAttributes", "PersonList.ecore");
		loadEcoreFiles();
		refactorings.extractClass("Address",
			asList(
				refactorings.getEAttribute("PersonList", "Person", "street"),
				refactorings.getEAttribute("PersonList", "Person", "houseNumber"))
			);
		modelManager.saveEcores(AbstractEdeltaRefactoringsLibTest.MODIFIED);
		assertModifiedFiles();
	}

	@Test
	void test_extractClassWithAttributesContainedInDifferentClasses() throws IOException {
		withInputModels("extractClassWithAttributesContainedInDifferentClasses", "PersonList.ecore");
		loadEcoreFiles();
		var thrown = assertThrowsIAE(() -> refactorings.extractClass("Address",
			asList(
				refactorings.getEAttribute("PersonList", "Person", "street"),
				refactorings.getEAttribute("PersonList", "Person2", "street"))
			));
		assertThat(thrown.getMessage())
			.isEqualTo(
			"""
				Multiple containing classes:
				  PersonList.Person:
				    PersonList.Person.street
				  PersonList.Person2:
				    PersonList.Person2.street""");
		modelManager.saveEcores(AbstractEdeltaRefactoringsLibTest.MODIFIED);
		assertModifiedFilesAreSameAsOriginal();
		assertThat(appender.getResult().trim())
			.isEqualTo(
			"""
			ERROR: PersonList.Person: Extracted features must belong to the same class: PersonList.Person
			ERROR: PersonList.Person2: Extracted features must belong to the same class: PersonList.Person2"""
			);
	}

	@Test
	void test_extractClassWithAttributesEmpty() {
		var result = refactorings.extractClass("Address", emptyList());
		assertThat(result).isNull();
	}

	@Test
	void test_extractClassWithReferences() throws Exception {
		var subdir = "extractClassWithReferences/";
		var ecores = of("PersonList.ecore");
		var models = of("List.xmi");

		var engine = setupEngine(
			subdir,
			ecores,
			models,
			other -> new EdeltaRefactorings(other) {
				@Override
				protected void doExecute() {
					extractClass("WorkAddress",
						asList(
							getEAttribute("PersonList", "Person", "street"),
							getEAttribute("PersonList", "Person", "houseNumber"),
							getEReference("PersonList", "Person", "workplace")
						)
					);
				}
			}
		);

		assertOutputs(
			engine,
			subdir,
			ecores,
			models
		);
	}

	@Test
	void test_extractClassWithReferencesBidirectional() throws IOException {
		withInputModels("extractClassWithReferencesBidirectional", "PersonList.ecore");
		loadEcoreFiles();
		assertThrowsIAE(() -> refactorings.extractClass("WorkAddress",
			asList(
				refactorings.getEAttribute("PersonList", "Person", "street"),
				refactorings.getEReference("PersonList", "Person", "workplace"),
				refactorings.getEAttribute("PersonList", "Person", "houseNumber"))
			));
		assertThat(appender.getResult().trim())
			.isEqualTo(
			"""
			ERROR: PersonList.Person.workplace: Cannot extract bidirectional references:
			  PersonList.Person.workplace"""
			);
	}

	@Test
	void test_inlineClassWithAttributes() throws IOException {
		withInputModels("inlineClassWithAttributes", "PersonList.ecore");
		loadEcoreFiles();
		refactorings.inlineClass(refactorings.getEClass("PersonList", "Address"));
		modelManager.saveEcores(AbstractEdeltaRefactoringsLibTest.MODIFIED);
		assertModifiedFiles();
	}

	@Test
	void test_inlineClassPrefix() throws IOException {
		withInputModels("inlineClassPrefix", "PersonList.ecore");
		loadEcoreFiles();
		refactorings.inlineClass(
			refactorings.getEClass("PersonList", "Address"),
			"address_");
		modelManager.saveEcores(AbstractEdeltaRefactoringsLibTest.MODIFIED);
		assertModifiedFiles();
	}

	@Test
	void test_inlineClassWithReferences() throws Exception {
		var subdir = "inlineClassWithReferences/";
		var ecores = of("PersonList.ecore");
		var models = of("List.xmi");

		var engine = setupEngine(
			subdir,
			ecores,
			models,
			other -> new EdeltaRefactorings(other) {
				@Override
				protected void doExecute() {
					inlineClass(getEClass("PersonList", "WorkAddress"));
				}
			}
		);

		assertOutputs(
			engine,
			subdir,
			ecores,
			models
		);
	}

	@Test
	void test_inlineClassWithAttributesWronglyUsedByOthersParallel() throws IOException {
		withInputModels("inlineClassWithAttributesWronglyUsedByOthersParallel",
				"PersonList.ecore", "PersonListReferring.ecore");
		loadEcoreFiles();
		assertThrowsIAE(() ->
			refactorings.inlineClass(refactorings.getEClass("PersonList", "Address")));
		modelManager.saveEcores(AbstractEdeltaRefactoringsLibTest.MODIFIED);
		assertModifiedFilesAreSameAsOriginal();
		assertThat(appender.getResult().trim())
			.isEqualTo(
			"""
				ERROR: PersonList.Address: The EClass is used by more than one element:
				  PersonList.Person.address
				    ecore.ETypedElement.eType
				  PersonListReferring.UsesAddress.address
				    ecore.ETypedElement.eType
				  PersonListReferring.ExtendsAddress
				    ecore.EClass.eSuperTypes""");
	}

	@Test
	void test_inlineClassWithAttributesWronglyMulti() throws IOException {
		withInputModels("inlineClassWithAttributesWronglyMulti",
				"PersonList.ecore");
		loadEcoreFiles();
		assertThrowsIAE(() ->
			refactorings.inlineClass(refactorings.getEClass("PersonList", "Address")));
		modelManager.saveEcores(AbstractEdeltaRefactoringsLibTest.MODIFIED);
		assertModifiedFilesAreSameAsOriginal();
		assertThat(appender.getResult().trim())
			.isEqualTo(
			"ERROR: PersonList.Person.addresses: Cannot inline in a 'many' reference: PersonList.Person.addresses");
	}

	@Test
	void test_inlineClassWithReferencesBidirectional() throws IOException {
		withInputModels("inlineClassWithReferencesBidirectional", "PersonList.ecore");
		loadEcoreFiles();
		assertThrowsIAE(() ->
			refactorings.inlineClass(refactorings.getEClass("PersonList", "WorkAddress")));
		assertThat(appender.getResult().trim())
			.isEqualTo(
			"""
				ERROR: PersonList.WorkAddress: The EClass is used by more than one element:
				  PersonList.Person.workAddress
				    ecore.ETypedElement.eType
				  PersonList.WorkPlace.address
				    ecore.ETypedElement.eType""");
	}

	/**
	 * To have complete reversibility (since we use text comparison), we
	 * have to extract features in a specific order. In fact, during model migration,
	 * attributes are processed before references and this might change the resulting
	 * XMI, though the models are effectively the same).
	 * 
	 * @throws Exception
	 */
	@Test
	void test_extractClass_IsOppositeOf_inlineClass() throws Exception {
		var subdir = "extractClassWithReferences/";
		var ecores = of("PersonList.ecore");
		var models = of("List.xmi");

		var engine = setupEngine(
			subdir,
			ecores,
			models,
			other -> new EdeltaRefactorings(other) {
				@Override
				protected void doExecute() {
					var ref = extractClass("WorkAddress",
						asList(
							getEAttribute("PersonList", "Person", "street"),
							getEReference("PersonList", "Person", "workplace"),
							getEAttribute("PersonList", "Person", "houseNumber")
						)
					);
					inlineClass(ref.getEReferenceType());
				}
			}
		);

		assertOutputs(
			engine,
			"inlineClassWithReferences/",
			ecores,
			models
		);
	}

	/**
	 * see {@link #test_extractClass_IsOppositeOf_inlineClass()}
	 * 
	 * @throws Exception
	 */
	@Test
	void test_inlineClass_IsOppositeOf_extractClass() throws Exception {
		var subdir = "inlineClassWithReferences/";
		var ecores = of("PersonList.ecore");
		var models = of("List.xmi");

		var engine = setupEngine(
			subdir,
			ecores,
			models,
			other -> new EdeltaRefactorings(other) {
				@Override
				protected void doExecute() {
					inlineClass(getEClass("PersonList", "WorkAddress"));
					extractClass("WorkAddress",
						asList(
							getEAttribute("PersonList", "Person", "street"),
							getEAttribute("PersonList", "Person", "houseNumber"),
							getEReference("PersonList", "Person", "workplace")
						)
					);
				}
			}
		);

		assertOutputs(
			engine,
			"extractClassWithReferences/",
			ecores,
			models
		);
	}

	@ParameterizedTest
	@ValueSource(strings = {
		"referenceToClassUnidirectional/",
		"referenceToClassMultipleUnidirectional/",
		"referenceToClassBidirectional/",
		"referenceToClassBidirectionalDifferentOrder/",
		"referenceToClassBidirectionalOppositeMultiple/",
		"referenceToClassMultipleBidirectional/",
	})
	void test_referenceToClass(String directory) throws Exception {
		var subdir = directory;
		var ecores = of("PersonList.ecore");
		var models = of("List.xmi");

		var engine = setupEngine(
			subdir,
			ecores,
			models,
			other -> new EdeltaRefactorings(other) {
				@Override
				protected void doExecute() {
					final EReference ref = getEReference("PersonList", "Person", "works");
					referenceToClass("WorkingPosition", ref);
				}
			}
		);

		assertOutputs(
			engine,
			subdir,
			ecores,
			models
		);
	}

	@Test
	void test_referenceToClassWithContainmentReference() throws IOException {
		withInputModels("referenceToClassWithContainmentReference", "PersonList.ecore");
		loadEcoreFiles();
		final EReference ref = refactorings.getEReference("PersonList", "Person", "works");
		assertThrowsIAE(() -> refactorings.referenceToClass("WorkingPosition", ref));
		modelManager.saveEcores(AbstractEdeltaRefactoringsLibTest.MODIFIED);
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
		loadEcoreFiles();
		final EClass cl = refactorings.getEClass("PersonList", "WorkingPosition");
		var result = refactorings.classToReference(cl);
		assertThat(result)
			.isEqualTo(refactorings.getEReference("PersonList", "Person", "works"));
		modelManager.saveEcores(AbstractEdeltaRefactoringsLibTest.MODIFIED);
		assertModifiedFiles();
	}

	@Test
	void test_classToReferenceWhenClassIsNotReferred() {
		withInputModels("classToReferenceWronglyReferred", "TestEcore.ecore");
		loadEcoreFiles();
		assertThrowsIAE(() -> refactorings.classToReference(
				refactorings.getEClass("p", "CNotReferred")));
		assertThat(appender.getResult().trim())
			.isEqualTo("ERROR: p.CNotReferred: The EClass is not referred: p.CNotReferred");
	}

	@Test
	void test_classToReferenceWhenClassIsReferredMoreThanOnce() {
		withInputModels("classToReferenceWronglyReferred", "TestEcore.ecore");
		loadEcoreFiles();
		assertThrowsIAE(() -> refactorings.classToReference(
				refactorings.getEClass("p", "C")));
		assertThat(appender.getResult())
			.isEqualTo(
			"""
			ERROR: p.C: The EClass is referred by more than one container:
			  p.C1.r1
			  p.C2.r2
			"""
			);
	}

	@Test
	void test_classToReferenceWhenClassIsReferredMoreThanOnceParallel() {
		withInputModels("classToReferenceWronglyReferredParallel",
				"TestEcore.ecore", "TestEcoreReferring.ecore");
		loadEcoreFiles();
		assertThrowsIAE(() -> refactorings.classToReference(
				refactorings.getEClass("p", "C")));
		assertThat(appender.getResult())
			.isEqualTo(
			"""
			ERROR: p.C: The EClass is referred by more than one container:
			  p.C1.r1
			  p2.C2.r2
			"""
			);
	}

	@Test
	void test_classToReferenceWithMissingTarget() {
		withInputModels("classToReferenceUnidirectional", "PersonList.ecore");
		loadEcoreFiles();
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
		loadEcoreFiles();
		final EClass cl = refactorings.getEClass("PersonList", "WorkingPosition");
		// manually add another reference to target class
		EReference ref = this.createEReference(cl, "another");
		ref.setEType(refactorings.getEClass("PersonList", "List"));
		assertThrowsIAE(() -> refactorings.classToReference(cl));
		assertThat(appender.getResult())
			.isEqualTo(
			"""
			ERROR: PersonList.WorkingPosition: Too many references not of type PersonList.Person:
			  PersonList.WorkingPosition.workPlace
			  PersonList.WorkingPosition.another
			"""
			);
	}

	@Test
	void test_classToReferenceUnidirectionalWithoutOppositeIsOk() throws IOException {
		withInputModels("classToReferenceUnidirectional", "PersonList.ecore");
		loadEcoreFiles();
		final EClass cl = refactorings.getEClass("PersonList", "WorkingPosition");
		// manually remove the opposite reference
		EReference personFeature = (EReference) cl.getEStructuralFeature("person");
		// also appropriately update the opposite, otherwise we have a dangling reference
		personFeature.getEOpposite().setEOpposite(null);
		cl.getEStructuralFeatures().remove(personFeature);
		refactorings.classToReference(cl);
		modelManager.saveEcores(AbstractEdeltaRefactoringsLibTest.MODIFIED);
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
		loadEcoreFiles();
		refactorings.extractSuperclass(
			asList(
				refactorings.getEAttribute("p", "C1", "a1"),
				refactorings.getEAttribute("p", "C2", "a1")));
		refactorings.extractSuperclass(
			asList(
				refactorings.getEAttribute("p", "C3", "a1"),
				refactorings.getEAttribute("p", "C4", "a1")));
		modelManager.saveEcores(AbstractEdeltaRefactoringsLibTest.MODIFIED);
		assertModifiedFiles();
		assertLogIsEmpty();
	}

	@ParameterizedTest
	@ValueSource(strings = {
		"pullUpReferences/",
		"pullUpContainmentReferences/",
	})
	void test_pullUpReferences(String directory) throws Exception {
		var subdir = directory;
		var ecores = of("PersonList.ecore");
		var models = of("List.xmi");

		var engine = setupEngine(
			subdir,
			ecores,
			models,
			other -> new EdeltaRefactorings(other) {
				@Override
				protected void doExecute() {
					var personClass = getEClass("PersonList", "Person");
					var studentAddress = getEReference("PersonList", "Student", "address");
					var employeeAddress = getEReference("PersonList", "Employee", "address");
					pullUpFeatures(personClass,
						asList(
							studentAddress, employeeAddress));
				}
			}
		);

		assertOutputs(
			engine,
			subdir,
			ecores,
			models
		);
	}

	@Test
	void test_pullUpAttributes() throws Exception {
		var subdir = "pullUpAttributes/";
		var ecores = of("PersonList.ecore");
		var models = of("List.xmi");

		var engine = setupEngine(
			subdir,
			ecores,
			models,
			other -> new EdeltaRefactorings(other) {
				@Override
				protected void doExecute() {
					var personClass = getEClass("PersonList", "Person");
					var studentAddress = getEAttribute("PersonList", "Student", "name");
					var employeeAddress = getEAttribute("PersonList", "Employee", "name");
					pullUpFeatures(personClass,
						asList(
							studentAddress, employeeAddress));
				}
			}
		);

		assertOutputs(
			engine,
			subdir,
			ecores,
			models
		);
	}

	@Test
	void test_pullUpFeaturesDifferent() throws IOException {
		withInputModels("pullUpFeaturesDifferent", "PersonList.ecore");
		loadEcoreFiles();
		final EClass person = refactorings.getEClass("PersonList", "Person");
		final EClass student = refactorings.getEClass("PersonList", "Student");
		final EClass employee = refactorings.getEClass("PersonList", "Employee");
		assertThrowsIAE(() -> refactorings.pullUpFeatures(person,
			asList(
				student.getEStructuralFeature("name"),
				employee.getEStructuralFeature("name"))));
		modelManager.saveEcores(AbstractEdeltaRefactoringsLibTest.MODIFIED);
		assertModifiedFilesAreSameAsOriginal();
		assertEquals(
		"""
		ERROR: PersonList.Employee.name: The two features are not equal:
		ecore.ETypedElement.lowerBound:
		  PersonList.Student.name: 0
		  PersonList.Employee.name: 1
		
		"""
		, appender.getResult());
	}

	@Test
	void test_pullUpFeaturesNotSubclass() throws IOException {
		withInputModels("pullUpFeaturesNotSubclass", "PersonList.ecore");
		loadEcoreFiles();
		final EClass person = refactorings.getEClass("PersonList", "Person");
		final EClass student = refactorings.getEClass("PersonList", "Student");
		final EClass employee = refactorings.getEClass("PersonList", "Employee");
		assertThrowsIAE(() -> refactorings.pullUpFeatures(person,
			asList(
				student.getEStructuralFeature("name"),
				employee.getEStructuralFeature("name"))));
		modelManager.saveEcores(AbstractEdeltaRefactoringsLibTest.MODIFIED);
		assertModifiedFilesAreSameAsOriginal();
		assertThat(appender.getResult())
			.isEqualTo(
			"""
			ERROR: PersonList.Student: Not a direct subclass of: PersonList.Person
			ERROR: PersonList.Employee: Not a direct subclass of: PersonList.Person
			"""
			);
	}

	@Test
	void test_pushDownFeature() throws Exception {
		var subdir = "pushDownFeatures/";
		var ecores = of("PersonList.ecore");
		var models = of("List.xmi");

		var engine = setupEngine(
			subdir,
			ecores,
			models,
			other -> new EdeltaRefactorings(other) {
				@Override
				protected void doExecute() {
					var personClass = getEClass("PersonList", "Person");
					var personName = personClass.getEStructuralFeature("name");
					var studentClass = getEClass("PersonList", "Student");
					var employeeClass = getEClass("PersonList", "Employee");
					// refactoring
					pushDownFeature(
							personName,
							List.of(studentClass, employeeClass));
				}
			}
		);

		assertOutputs(
			engine,
			subdir,
			ecores,
			models
		);
	}

	@Test
	void test_pushDown_IsOppositeOf_pullUp() throws Exception {
		var subdir = "pushDownFeatures/";
		var ecores = of("PersonList.ecore");
		var models = of("List.xmi");

		var engine = setupEngine(
			subdir,
			ecores,
			models,
			other -> new EdeltaRefactorings(other) {
				@Override
				protected void doExecute() {
					var personClass = getEClass("PersonList", "Person");
					var personName = personClass.getEStructuralFeature("name");
					var studentClass = getEClass("PersonList", "Student");
					var employeeClass = getEClass("PersonList", "Employee");
					// refactoring
					var features = pushDownFeature(
							personName,
							List.of(studentClass, employeeClass));
					// opposite
					pullUpFeatures(personClass,
							features);
				}
			}
		);

		assertOutputs(
			engine,
			"pushDownAndPullUp/",
			ecores,
			models
		);
	}

	@Test
	void test_pullUp_IsOppositeOf_pushDown() throws Exception {
		var subdir = "pullUpAttributes/";
		var ecores = of("PersonList.ecore");
		var models = of("List.xmi");

		var engine = setupEngine(
			subdir,
			ecores,
			models,
			other -> new EdeltaRefactorings(other) {
				@Override
				protected void doExecute() {
					var personClass = getEClass("PersonList", "Person");
					var studentClass = getEClass("PersonList", "Student");
					var employeeClass = getEClass("PersonList", "Employee");
					var studentName = studentClass.getEStructuralFeature("name");
					var employeeName = employeeClass.getEStructuralFeature("name");
					// refactoring
					var personName = pullUpFeatures(
							personClass,
							List.of(studentName, employeeName));
					// opposite
					pushDownFeature(
							personName,
							List.of(studentClass, employeeClass));
				}
			}
		);

		assertOutputs(
			engine,
			"pullUpAndPushDown/",
			ecores,
			models
		);
	}

	@Test
	void test_mergeClasses() throws Exception {
		var subdir = "mergeClasses/";
		var ecores = of("TestEcore.ecore");
		var models = of("Container.xmi");

		var engine = setupEngine(
			subdir,
			ecores,
			models,
			other -> new EdeltaRefactorings(other) {
				@Override
				protected void doExecute() {
					var toMerge = of(
						getEClass("testecore", "SubElement1"),
						getEClass("testecore", "SubElement2"),
						getEClass("testecore", "SubElement3")
					);
					var merged = mergeClasses("SubElement", toMerge);
					var superClass = getEClass("testecore", "Element");
					assertThat(merged.getESuperTypes())
						.containsOnly(superClass);
					assertThat(merged.getEPackage())
						.isSameAs(superClass.getEPackage());
				}
			}
		);

		assertOutputs(
			engine,
			subdir,
			ecores,
			models
		);
	}

	@Test
	void test_mergeClassesFeaturesDifferInSize() throws Exception {
		withInputModels("mergeClasses", "TestEcore.ecore");
		loadEcoreFiles();
		var thirdSubClass = refactorings.getEClass("testecore", "SubElement3");
		thirdSubClass.getEStructuralFeatures().remove(2);
		var toMerge = of(
			refactorings.getEClass("testecore", "SubElement1"),
			refactorings.getEClass("testecore", "SubElement2"),
			thirdSubClass
		);
		assertThrowsIAE(() -> refactorings.mergeClasses("SubElement", toMerge));
		modelManager.saveEcores(AbstractEdeltaRefactoringsLibTest.MODIFIED);
		assertEquals(
			"""
			ERROR: testecore.SubElement3: Different features size: expected 3 but was 2
			  in classes testecore.SubElement1, testecore.SubElement3
			""",
			appender.getResult());
	}

	@Test
	void test_mergeClassesFeaturesDifferInADetail() throws Exception {
		withInputModels("mergeClasses", "TestEcore.ecore");
		loadEcoreFiles();
		var thirdSubClass = refactorings.getEClass("testecore", "SubElement3");
		thirdSubClass.getEStructuralFeature("containments").setLowerBound(2);
		var toMerge = of(
			refactorings.getEClass("testecore", "SubElement1"),
			refactorings.getEClass("testecore", "SubElement2"),
			thirdSubClass
		);
		assertThrowsIAE(() -> refactorings.mergeClasses("SubElement", toMerge));
		modelManager.saveEcores(AbstractEdeltaRefactoringsLibTest.MODIFIED);
		assertEquals(
			"""
			ERROR: testecore.SubElement3: ecore.ETypedElement.lowerBound:
			  testecore.SubElement1.containments: 0
			  testecore.SubElement3.containments: 2
			
			""",
			appender.getResult());
	}

	@Test
	void test_splitClassDefault() throws Exception {
		var subdir = "splitClass/";
		var ecores = of("TestEcore.ecore");
		var models = of("Container.xmi");

		var engine = setupEngine(
			subdir,
			ecores,
			models,
			other -> new EdeltaRefactorings(other) {
				@Override
				protected void doExecute() {
					var toSplit = getEClass("testecore", "SubElement");
					var split = splitClass(toSplit,
						of(
							"SubElement1",
							"SubElement2",
							"SubElement3"
						)
					);
					var superClass = getEClass("testecore", "Element");
					assertThat(split.stream()
						.flatMap(c -> c.getESuperTypes().stream()))
						.containsOnly(superClass);
					assertThat(split.stream()
						.map(ENamedElement::getName))
						.containsExactly(
							"SubElement1",
							"SubElement2",
							"SubElement3"
						);
				}
			}
		);

		assertOutputs(
			engine,
			"splitClassDefault/",
			ecores,
			models
		);
	}

	/**
	 * Assumes that the name of the SubElement ends with a number
	 * that is used to create the instance of one of the splitted classes.
	 * 
	 * @throws Exception
	 */
	@Test
	void test_splitClass() throws Exception {
		var subdir = "splitClass/";
		var ecores = of("TestEcore.ecore");
		var models = of("Container.xmi");

		var engine = setupEngine(
			subdir,
			ecores,
			models,
			other -> new EdeltaRefactorings(other) {
				@Override
				protected void doExecute() {
					var ePackage = getEPackage("testecore");
					var toSplit = getEClass("testecore", "SubElement");
					var split = splitClass(toSplit,
						of(
							"SubElement1",
							"SubElement2",
							"SubElement3"
						),
						(origObj -> {
							var origClass = origObj.eClass();
							var nameFeature = origClass.getEStructuralFeature("name");
							var nameValue = origObj.eGet(nameFeature).toString();
							var newObjClassName = "SubElement" +
									nameValue.charAt(nameValue.length() - 1);
							return EdeltaEcoreUtil.createInstance(
									getEClass(ePackage, newObjClassName));
						})
					);
					var superClass = getEClass("testecore", "Element");
					assertThat(split.stream()
						.flatMap(c -> c.getESuperTypes().stream()))
						.containsOnly(superClass);
					assertThat(split.stream()
						.map(ENamedElement::getName))
						.containsExactly(
							"SubElement1",
							"SubElement2",
							"SubElement3"
						);
				}
			}
		);

		assertOutputs(
			engine,
			subdir,
			ecores,
			models
		);
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
			"""
				p.SubClass
				  ecore.EClass.eSuperTypes
				p.UsesC.refToC
				  ecore.ETypedElement.eType""");
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
		assertEquals(
			"""
			ERROR: p.C: The EClass is not used: p.C
			ERROR: p.C: The EClass is used by more than one element:
			  p.SubClass
			    ecore.EClass.eSuperTypes
			  p.UsesC.refToC
			    ecore.ETypedElement.eType
			"""
			, appender.getResult());
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
			"""
			ERROR: p.C: Not a reference: p.C
			ERROR: p.C.refToC: Not a containment reference: p.C.refToC
			"""
			);
	}

	@Test
	void test_checkType() {
		refactorings.checkType(EcorePackage.Literals.ENAMED_ELEMENT__NAME, stringDataType);
		assertThrowsIAE(() -> refactorings.checkType(EcorePackage.Literals.ENAMED_ELEMENT__NAME, intDataType));
		assertEquals(
			"ERROR: ecore.ENamedElement.name: expecting ecore.EInt but was ecore.EString\n"
			, appender.getResult());
	}

	private static IllegalArgumentException assertThrowsIAE(Executable executable) {
		return assertThrows(IllegalArgumentException.class,
			executable);
	}
}
