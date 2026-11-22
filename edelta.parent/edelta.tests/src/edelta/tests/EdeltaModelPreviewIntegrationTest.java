package edelta.tests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.impl.ResourceImpl;
import org.eclipse.xtext.resource.DerivedStateAwareResource;
import org.eclipse.xtext.resource.XtextResourceSet;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.XtextRunner;
import org.eclipse.xtext.workspace.FileProjectConfig;
import org.eclipse.xtext.workspace.ProjectConfigAdapter;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;

import com.google.inject.Inject;
import com.google.inject.Provider;

import edelta.edelta.EdeltaProgram;
import edelta.edelta.EdeltaFactory;
import edelta.lib.EdeltaModelManager;
import edelta.resource.EdeltaModelSourceSelector;
import edelta.resource.derivedstate.EdeltaModelPreview;
import edelta.validation.EdeltaValidator;

/**
 * Exercises private model previews through the real derived-state lifecycle.
 *
 * @author Lorenzo Bettini
 */
@RunWith(XtextRunner.class)
@InjectWith(EdeltaInjectorProvider.class)
public class EdeltaModelPreviewIntegrationTest extends EdeltaAbstractTest {
	private static final Path DATA = Path.of("../edelta.testdata/testdata/simpleTestData");

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	@Inject
	private Provider<XtextResourceSet> resourceSetProvider;

	@Test
	public void directRuleUsesTheEditorsCopiedEClass() throws Exception {
		var root = projectWithModels();
		var input = root.resolve("models/MyClass.xmi");
		var sourceBytes = Files.readAllBytes(input);
		var program = parse(root, true, """
			metamodel "mypackage"
			testmodels { "models/MyClass.xmi" }
			modifyEcore evolve epackage mypackage {
				val attribute = ecoreref(MyClass.myClassStringAttribute)
				modelMigration[
					transformAttributeValueRule(isRelatedTo(attribute), [value |
						value.toString.toUpperCase
					])
				]
			}
			""");
		var preview = derivedStateHelper.getModelPreview(program.eResource());
		assertThat(preview.getFailures()).isEmpty();
		assertThat(preview.getModels()).hasSize(1);
		var model = preview.getModels().getFirst();
		assertThat(model.sourcePath()).isEqualTo(input);
		assertThat(model.declaration()).isSameAs(program.getTestModels().getPaths().getFirst());
		var migrated = model.resource().getContents().getFirst();
		var copied = derivedStateHelper.getCopiedEPackagesMap(program.eResource()).get("mypackage");
		assertThat(migrated.eClass()).isSameAs(copied.getEClassifier("MyClass"));
		assertThat(copied.eResource()).isSameAs(program.eResource());
		assertThat(model.resource().getResourceSet())
			.isNotSameAs(program.eResource().getResourceSet());
		assertThat(model.resource().getResourceSet().getResources())
			.containsExactly(model.resource());
		assertThat(migrated.eGet(migrated.eClass().getEStructuralFeature(
			"myClassStringAttribute"))).isEqualTo("MY ATTRIBUTE VALUE");
		assertThat(Files.readAllBytes(input)).isEqualTo(sourceBytes);
	}

	@Test
	public void stagedRulesKeepResourceAssociationsAndCrossReferences() throws Exception {
		var root = projectWithModels();
		var rootFile = root.resolve("models/MyRoot.xmi");
		Files.writeString(rootFile, Files.readString(rootFile)
			.replace("myReferences=\"//@myContents.1\"",
				"myReferences=\"MyClass.xmi#/\""));
		var program = parse(root, true, """
			metamodel "mypackage"
			testmodels { "models/MyRoot.xmi" "models/MyClass.xmi" }
			modifyEcore evolve epackage mypackage {
				val attribute = ecoreref(MyClass.myClassStringAttribute)
				modelMigration[
					transformAttributeValueRule(isRelatedTo(attribute), [value |
						value.toString.toUpperCase
					])
				]
				modelMigration[
					transformAttributeValueRule(isRelatedTo(attribute), [value |
						value.toString + "!"
					])
				]
			}
			""");
		var preview = derivedStateHelper.getModelPreview(program.eResource());
		assertThat(preview.getFailures()).isEmpty();
		assertThat(preview.getModels()).extracting(m -> m.sourcePath().getFileName().toString())
			.containsExactly("MyRoot.xmi", "MyClass.xmi");
		assertThat(preview.getModels()).extracting(EdeltaModelPreview.Model::declaration)
			.containsExactlyElementsOf(program.getTestModels().getPaths());
		var rootModel = preview.getModels().get(0).resource().getContents().getFirst();
		var classModel = preview.getModels().get(1).resource().getContents().getFirst();
		assertThat(value(classModel, "myClassStringAttribute")).isEqualTo("MY ATTRIBUTE VALUE!");
		assertThat(references(rootModel, "myReferences")).containsExactly(classModel);
		var copied = derivedStateHelper.getCopiedEPackagesMap(program.eResource()).get("mypackage");
		assertThat(rootModel.eClass()).isSameAs(copied.getEClassifier("MyRoot"));
		assertThat(classModel.eClass()).isSameAs(copied.getEClassifier("MyClass"));
	}

	@Test
	public void libraryRefactoringMigratesTheSelectedModel() throws Exception {
		var root = projectWithModels();
		var rootFile = root.resolve("models/MyRoot.xmi");
		Files.writeString(rootFile, Files.readString(rootFile)
			.replace("myReferences=\"//@myContents.1\"",
				"myReferences=\"//@myContents.0 //@myContents.1\""));
		var program = parse(root, true, """
			import edelta.refactorings.lib.EdeltaRefactorings
			metamodel "mypackage"
			metamodel "ecore"
			testmodels { "models/MyRoot.xmi" }
			use EdeltaRefactorings as extension refactorings
			modifyEcore evolve epackage mypackage {
				refactorings.changeToSingle(ecoreref(MyRoot.myReferences))
			}
			""");
		var preview = derivedStateHelper.getModelPreview(program.eResource());
		assertThat(preview.getFailures()).isEmpty();
		var model = preview.getModels().getFirst().resource().getContents().getFirst();
		var contents = references(model, "myContents");
		assertThat(value(model, "myReferences")).isSameAs(contents.getFirst());
		assertThat(model.eClass().getEStructuralFeature("myReferences").isMany()).isFalse();
	}

	@Test
	public void recoveredMigrationFailureCannotPublishModels() throws Exception {
		var root = projectWithModels();
		var program = parse(root, true, """
			metamodel "mypackage"
			testmodels { "models/MyClass.xmi" }
			modifyEcore evolve epackage mypackage {
				ecoreref(MyClass).name = "Renamed"
				modelMigration[throw new IllegalStateException("preview failed")]
			}
			""");
		var preview = derivedStateHelper.getModelPreview(program.eResource());
		assertThat(preview.getModels()).isEmpty();
		assertThat(preview.getFailures()).extracting(EdeltaModelPreview.Failure::code)
			.contains("MIGRATION");
		assertThat(derivedStateHelper.getCopiedEPackagesMap(program.eResource())
			.get("mypackage").getEClassifier("Renamed")).isNotNull();
	}

	@Test
	public void interpretationFailureRetainsEcoreButNoPreview() throws Exception {
		var root = projectWithModels();
		var program = parse(root, true, """
			metamodel "mypackage"
			testmodels { "models/MyClass.xmi" }
			modifyEcore evolve epackage mypackage {
				ecoreref(MyClass).name = "Renamed"
				throw new NullPointerException("interpretation failed")
			}
			""");
		var preview = derivedStateHelper.getModelPreview(program.eResource());
		assertThat(preview.getModels()).isEmpty();
		assertThat(preview.getFailures()).extracting(EdeltaModelPreview.Failure::code)
			.containsExactly("INTERPRETATION");
		assertThat(validationTestHelper.validate(program)).anyMatch(issue ->
			EdeltaValidator.MODEL_PREVIEW_FAILURE.equals(issue.getCode()) &&
			issue.getMessage().contains("interpretation failed"));
		assertThat(derivedStateHelper.getCopiedEPackagesMap(program.eResource())
			.get("mypackage").getEClassifier("Renamed")).isNotNull();
	}

	@Test
	public void nestedCallbackFailureIsRecordedOnce() throws Exception {
		var root = projectWithModels();
		var program = parse(root, true, """
			metamodel "mypackage"
			testmodels { "models/MyClass.xmi" }
			modifyEcore evolve epackage mypackage {
				modelMigration[
					modelMigration[throw new IllegalStateException("nested failure")]
				]
			}
			""");
		var preview = derivedStateHelper.getModelPreview(program.eResource());
		assertThat(preview.getModels()).isEmpty();
		assertThat(preview.getFailures()).extracting(EdeltaModelPreview.Failure::code)
			.containsExactly("MIGRATION");
	}

	@Test
	public void missingProjectRootDoesNotUseSourceFolder() throws Exception {
		var root = projectWithModels();
		var program = parse(root, false, """
			metamodel "mypackage"
			testmodels { "models/MyClass.xmi" }
			modifyEcore evolve epackage mypackage {
				ecoreref(MyClass).name = "Renamed"
			}
			""");
		var preview = derivedStateHelper.getModelPreview(program.eResource());
		assertThat(preview.getModels()).isEmpty();
		assertThat(preview.getFailures()).extracting(EdeltaModelPreview.Failure::code)
			.containsExactly("PROJECT_ROOT_UNAVAILABLE");
		assertThat(validationTestHelper.validate(program)).anyMatch(issue ->
			EdeltaValidator.MODEL_PREVIEW_INPUT_FAILURE.equals(issue.getCode()) &&
			issue.getMessage().contains("project root"));
		assertThat(derivedStateHelper.getCopiedEPackagesMap(program.eResource())
			.get("mypackage").getEClassifier("Renamed")).isNotNull();
	}

	@Test
	public void oneMissingInputAbortsTheWholePreview() throws Exception {
		var root = projectWithModels();
		var program = parse(root, true, """
			metamodel "mypackage"
			testmodels { "models/MyClass.xmi" "models/missing.xmi" }
			modifyEcore evolve epackage mypackage {
				ecoreref(MyClass).name = "Renamed"
			}
			""");
		var preview = derivedStateHelper.getModelPreview(program.eResource());
		assertThat(preview.getModels()).isEmpty();
		assertThat(preview.getFailures()).extracting(EdeltaModelPreview.Failure::code)
			.containsExactly("MISSING");
		assertThat(preview.getFailures().getFirst().source())
			.isSameAs(program.getTestModels().getPaths().get(1));
		assertThat(derivedStateHelper.getCopiedEPackagesMap(program.eResource())
			.get("mypackage").getEClassifier("Renamed")).isNotNull();
	}

	@Test
	public void unrelatedInputProducesAnEmptyPreview() throws Exception {
		var root = projectWithModels();
		Files.writeString(root.resolve("models/unrelated.xmi"),
			"<other:Thing xmlns:other=\"http://other.example\"/>");
		var program = parse(root, true, """
			metamodel "mypackage"
			testmodels { "models/unrelated.xmi" }
			modifyEcore evolve epackage mypackage {
				ecoreref(MyClass).name = "Renamed"
			}
			""");
		var preview = derivedStateHelper.getModelPreview(program.eResource());
		assertThat(preview.getModels()).isEmpty();
		assertThat(preview.getFailures()).isEmpty();
		assertThat(derivedStateHelper.getCopiedEPackagesMap(program.eResource())
			.get("mypackage").getEClassifier("Renamed")).isNotNull();
	}

	@Test
	public void resourceAssociationRejectsMissingOrReorderedOutputs() {
		var entry1 = EdeltaFactory.eINSTANCE.createEdeltaTestModelPath();
		var entry2 = EdeltaFactory.eINSTANCE.createEdeltaTestModelPath();
		var first = new ResourceImpl(URI.createURI("memory:/first.xmi"));
		var second = new ResourceImpl(URI.createURI("memory:/second.xmi"));
		var selected = List.of(
			new EdeltaModelSourceSelector.Selected(Path.of("first.xmi"), entry1, first),
			new EdeltaModelSourceSelector.Selected(Path.of("second.xmi"), entry2, second));
		assertThatThrownBy(() -> EdeltaModelPreview.associate(selected, List.of(first)))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("count");
		assertThatThrownBy(() -> EdeltaModelPreview.associate(selected, List.of(second, first)))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("order");
	}

	@Test
	public void failedPreviewCannotPublishModels() {
		var preview = new EdeltaModelPreview();
		preview.addFailure(new EdeltaModelPreview.Failure("MIGRATION", null, null, null));
		assertThatThrownBy(() -> preview.setModels(List.of()))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("failed model preview");
	}

	@Test
	public void platformProjectUsesWorkspaceLocation() throws Exception {
		var root = projectWithModels();
		var name = "edeltaPreview" + UUID.randomUUID().toString().replace("-", "");
		var workspace = ResourcesPlugin.getWorkspace();
		var project = workspace.getRoot().getProject(name);
		var description = workspace.newProjectDescription(name);
		description.setLocationURI(root.toUri());
		project.create(description, null);
		project.open(null);
		try {
			var config = new FileProjectConfig(URI.createPlatformResourceURI(name, true), name);
			var program = parse(root, config, """
				metamodel "mypackage"
				testmodels { "models/MyClass.xmi" }
				modifyEcore evolve epackage mypackage {}
				""");
			var preview = derivedStateHelper.getModelPreview(program.eResource());
			assertThat(preview.getFailures()).isEmpty();
			assertThat(preview.getModels()).hasSize(1);
			assertThat(preview.getModels().getFirst().sourcePath().toRealPath())
				.isEqualTo(root.resolve("models/MyClass.xmi").toRealPath());
		} finally {
			project.delete(false, true, null);
		}
	}

	@Test
	public void unavailableConfiguredLocationsProduceStructuredFailures() throws Exception {
		var root = projectWithModels();
		var source = """
			metamodel "mypackage"
			testmodels { "models/MyClass.xmi" }
			modifyEcore evolve epackage mypackage {}
			""";
		var unsupported = parse(root,
			new FileProjectConfig(URI.createURI("memory:/preview"), "preview"), source);
		assertThat(derivedStateHelper.getModelPreview(unsupported.eResource()).getFailures())
			.extracting(EdeltaModelPreview.Failure::code)
			.containsExactly("PROJECT_ROOT_UNAVAILABLE");
		var missingProject = parse(root,
			new FileProjectConfig(URI.createPlatformResourceURI("absentPreviewProject", true),
				"absentPreviewProject"), source);
		assertThat(derivedStateHelper.getModelPreview(missingProject.eResource()).getFailures())
			.extracting(EdeltaModelPreview.Failure::code)
			.containsExactly("PROJECT_ROOT_UNAVAILABLE");
	}

	@Test
	public void discardAndReinstallReleasesOldModelResources() throws Exception {
		var root = projectWithModels();
		var program = parse(root, true, """
			metamodel "mypackage"
			testmodels { "models/MyClass.xmi" }
			modifyEcore evolve epackage mypackage {}
			""");
		var resource = (DerivedStateAwareResource) program.eResource();
		var preview = derivedStateHelper.getModelPreview(resource);
		var oldResource = preview.getModels().getFirst().resource();
		var oldResourceSet = oldResource.getResourceSet();
		var oldObject = oldResource.getContents().getFirst();
		program.getTestModels().getPaths().getFirst().setPath("models/MyRoot.xmi");
		resource.discardDerivedState();
		assertThat(oldResource.isLoaded()).isFalse();
		assertThat(oldResource.getResourceSet()).isNull();
		assertThat(oldResourceSet.getResources()).isEmpty();
		assertThat(oldObject.eResource()).isNull();
		assertThat(preview.getModels()).isEmpty();
		resource.installDerivedState(false);
		var newPreview = derivedStateHelper.getModelPreview(resource);
		assertThat(newPreview.getFailures()).isEmpty();
		assertThat(newPreview.getModels()).hasSize(1);
		assertThat(newPreview.getModels().getFirst().sourcePath().getFileName().toString())
			.isEqualTo("MyRoot.xmi");
		assertThat(newPreview.getModels().getFirst().resource().getContents().getFirst())
			.isNotSameAs(oldObject);
	}

	private Path projectWithModels() throws Exception {
		var root = folder.getRoot().toPath();
		Files.createDirectories(root.resolve("src"));
		Files.createDirectories(root.resolve("models"));
		Files.copy(DATA.resolve("MyClass.xmi"), root.resolve("models/MyClass.xmi"));
		Files.copy(DATA.resolve("MyRoot.xmi"), root.resolve("models/MyRoot.xmi"));
		return root;
	}

	private EdeltaProgram parse(Path root, boolean projectConfig, String source) throws Exception {
		return parse(root, projectConfig ? new FileProjectConfig(root.toFile(), "preview") : null,
			source);
	}

	private EdeltaProgram parse(Path root, FileProjectConfig projectConfig, String source)
			throws Exception {
		var resourceSet = resourceSetProvider.get();
		if (projectConfig != null) {
			ProjectConfigAdapter.install(resourceSet, projectConfig);
		}
		var ecore = new EdeltaModelManager().loadEcoreFile(DATA.resolve("My.ecore").toString());
		resourceSet.getResources().add(ecore);
		if (source.contains("metamodel \"ecore\"")) {
			resourceSet.getResource(createFileURIFromPath(METAMODEL_PATH + ECORE_ECORE), true);
		}
		var uri = URI.createFileURI(root.resolve("src/Preview.edelta").toString());
		var program = parseHelper.parse(source, uri, resourceSet);
		return program;
	}

	private Object value(EObject object, String feature) {
		return object.eGet(object.eClass().getEStructuralFeature(feature));
	}

	@SuppressWarnings("unchecked")
	private List<EObject> references(EObject object, String feature) {
		return (List<EObject>) value(object, feature);
	}
}
