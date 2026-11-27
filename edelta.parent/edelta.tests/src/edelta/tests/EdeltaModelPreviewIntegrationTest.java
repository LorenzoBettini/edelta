package edelta.tests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EcoreFactory;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.impl.ResourceImpl;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.eclipse.xtext.resource.DerivedStateAwareResource;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;
import org.eclipse.xtext.nodemodel.impl.NodeModelBuilder;
import org.eclipse.xtext.resource.XtextResourceSet;
import org.eclipse.xtext.xbase.XbaseFactory;
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
import edelta.lib.EdeltaDefaultRuntime;
import edelta.lib.EdeltaEngine;
import edelta.lib.EdeltaModelManager;
import edelta.lib.EdeltaResourceUtils;
import edelta.interpreter.EdeltaModelPreviewInterpreter;
import edelta.refactorings.lib.EdeltaRefactorings;
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
		var sourceText = Files.readString(input);
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
		var attribute = (org.eclipse.emf.ecore.EAttribute) migrated.eClass()
			.getEStructuralFeature("myClassStringAttribute");
		assertThat(NodeModelUtils.findActualNodeFor(
			preview.getMigrationExpression(migrated, attribute)).getText())
			.contains("modelMigration", "toUpperCase");
		assertThat(EdeltaModelPreviewInterpreter.userMigrationExpression(program,
			preview.getMigrationExpression(migrated, attribute)))
			.isSameAs(preview.getMigrationExpression(migrated, attribute));
		assertThat(Files.readString(input)).isEqualToNormalizingNewlines(sourceText);
	}

	@Test
	public void equalCopyRulePreservesLastValueChangingExpression() throws Exception {
		var root = projectWithModels();
		var program = parse(root, true, """
			metamodel "mypackage"
			testmodels { "models/MyClass.xmi" }
			modifyEcore evolve epackage mypackage {
				val attribute = ecoreref(MyClass.myClassStringAttribute)
				modelMigration[
					transformAttributeValueRule(isRelatedTo(attribute),
						[value | value.toString.toUpperCase])
				]
				modelMigration[
					copyRule([feature | false], [feature, source, target | ])
				]
			}
			""");
		var preview = derivedStateHelper.getModelPreview(program.eResource());
		assertThat(preview.getFailures()).isEmpty();
		var object = preview.getModels().getFirst().resource().getContents().getFirst();
		var feature = (EAttribute) object.eClass()
			.getEStructuralFeature("myClassStringAttribute");
		var effective = preview.getMigrationExpression(object, feature);
		assertThat(effective.eResource()).isSameAs(program.eResource());
		assertThat(NodeModelUtils.findActualNodeFor(effective).getText())
			.contains("toUpperCase");
	}

	@Test
	public void stagedRulesKeepResourceAssociationsAndCrossReferences() throws Exception {
		var root = projectWithModels();
		var rootFile = root.resolve("models/MyRoot.xmi");
		Files.writeString(rootFile, Files.readString(rootFile)
			.replace("myReferences=\"//@myContents.1\"",
				"myReferences=\"MyClass.xmi#/\""));
		var sourceRootText = Files.readString(rootFile);
		var sourceClassText = Files.readString(root.resolve("models/MyClass.xmi"));
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
		var attribute = (org.eclipse.emf.ecore.EAttribute) classModel.eClass()
			.getEStructuralFeature("myClassStringAttribute");
		assertThat(NodeModelUtils.findActualNodeFor(
			preview.getMigrationExpression(classModel, attribute)).getText())
			.contains("modelMigration", "value.toString + \"!\"");
		assertThat(references(rootModel, "myReferences")).containsExactly(classModel);
		var copied = derivedStateHelper.getCopiedEPackagesMap(program.eResource()).get("mypackage");
		assertThat(rootModel.eClass()).isSameAs(copied.getEClassifier("MyRoot"));
		assertThat(classModel.eClass()).isSameAs(copied.getEClassifier("MyClass"));

		var engineManager = executeWithSuccessiveEngineMigrations(root);
		assertThat(semanticSnapshot(rootModel))
			.isEqualTo(semanticSnapshot(modelRoot(engineManager, "MyRoot.xmi")));
		assertThat(semanticSnapshot(classModel))
			.isEqualTo(semanticSnapshot(modelRoot(engineManager, "MyClass.xmi")));
		assertThat(Files.readString(rootFile)).isEqualToNormalizingNewlines(sourceRootText);
		assertThat(Files.readString(root.resolve("models/MyClass.xmi")))
			.isEqualToNormalizingNewlines(sourceClassText);
	}

	@Test
	public void sameAttributeInstancesKeepDifferentLastChangingExpressions() throws Exception {
		// Both instances share one EAttribute and receive uppercase. Only the
		// first predicate visit receives suffix, so the final tokens must differ.
		var root = projectWithModels();
		var program = parse(root, true, """
			import java.util.concurrent.atomic.AtomicInteger
			metamodel "mypackage"
			testmodels { "models/MyClass.xmi" "models/MyRoot.xmi" }
			modifyEcore evolve epackage mypackage {
				val attribute = ecoreref(MyClass.myClassStringAttribute)
				val visits = new AtomicInteger(0)
				modelMigration[
					transformAttributeValueRule(isRelatedTo(attribute), [value |
						value.toString.toUpperCase
					])
				]
				modelMigration[
					transformAttributeValueRule([candidate |
						isRelatedTo(candidate, attribute) && visits.getAndIncrement() == 0
					], [value | value.toString + "!"])
				]
			}
			""");
		var preview = derivedStateHelper.getModelPreview(program.eResource());
		assertThat(preview.getFailures()).isEmpty();
		var first = preview.getModels().get(0).resource().getContents().getFirst();
		var rootObject = preview.getModels().get(1).resource().getContents().getFirst();
		var second = references(rootObject, "myContents").getFirst();
		var attribute = (org.eclipse.emf.ecore.EAttribute) first.eClass()
			.getEStructuralFeature("myClassStringAttribute");
		var firstExpression = preview.getMigrationExpression(first, attribute);
		var secondExpression = preview.getMigrationExpression(second, attribute);
		assertThat(firstExpression).isNotSameAs(secondExpression);
		assertThat(NodeModelUtils.findActualNodeFor(firstExpression).getText())
			.contains("value.toString + \"!\"");
		assertThat(NodeModelUtils.findActualNodeFor(secondExpression).getText())
			.contains("toUpperCase");
	}

	@Test
	public void migrationExpressionStorageRequiresFinalOwnedModels() {
		// A trace must be published after final models and must never expose
		// objects from another ResourceSet or a discarded preview.
		var failedPreview = new EdeltaModelPreview();
		assertThatThrownBy(() -> failedPreview.setMigrationExpressions(Map.of()))
			.isInstanceOf(IllegalStateException.class);
		failedPreview.addFailure(new EdeltaModelPreview.Failure("MIGRATION", null, null, null));
		assertThatThrownBy(() -> failedPreview.setMigrationExpressions(Map.of()))
			.isInstanceOf(IllegalStateException.class);
		var preview = new EdeltaModelPreview();
		var resourceSet = new ResourceSetImpl();
		var resource = new ResourceImpl(URI.createURI("memory:/final.xmi"));
		resourceSet.getResources().add(resource);
		var object = EcoreFactory.eINSTANCE.createEClass();
		resource.getContents().add(object);
		var unrecorded = EcoreFactory.eINSTANCE.createEClass();
		resource.getContents().add(unrecorded);
		var foreign = EcoreFactory.eINSTANCE.createEClass();
		var foreignResource = new ResourceImpl(URI.createURI("memory:/foreign.xmi"));
		new ResourceSetImpl().getResources().add(foreignResource);
		foreignResource.getContents().add(foreign);
		var detached = EcoreFactory.eINSTANCE.createEClass();
		EAttribute attribute = EcorePackage.Literals.ENAMED_ELEMENT__NAME;
		var expression = XbaseFactory.eINSTANCE.createXStringLiteral();
		preview.setModels(List.of(new EdeltaModelPreview.Model(Path.of("input.xmi"),
			null, resource)));
		preview.setMigrationExpressions(Map.of(object, Map.of(attribute, expression),
			foreign, Map.of(attribute, expression), detached, Map.of(attribute, expression)));
		assertThat(preview.getMigrationExpression(object, attribute)).isSameAs(expression);
		assertThat(preview.getMigrationExpression(unrecorded, attribute)).isNull();
		assertThat(preview.getMigrationExpression(object,
			EcorePackage.Literals.ETYPED_ELEMENT__LOWER_BOUND)).isNull();
		assertThat(preview.getMigrationExpression(null, attribute)).isNull();
		assertThat(preview.getMigrationExpression(foreign, attribute)).isNull();
		assertThat(preview.getMigrationExpression(detached, attribute)).isNull();
		preview.clear();
		assertThat(preview.getMigrationExpression(object, attribute)).isNull();
	}

	@Test
	public void migrationSourceTokenRejectsDetachedExternalAndUnparsedExpressions() {
		// Matching offsets alone cannot establish an editor target: the node
		// needs the current program, its resource, and real parsed source text.
		var program = EdeltaFactory.eINSTANCE.createEdeltaProgram();
		var candidate = XbaseFactory.eINSTANCE.createXStringLiteral();
		assertThat(EdeltaModelPreviewInterpreter.userMigrationExpression(null, candidate)).isNull();
		assertThat(EdeltaModelPreviewInterpreter.userMigrationExpression(program, null)).isNull();
		assertThat(EdeltaModelPreviewInterpreter.userMigrationExpression(program, candidate))
			.isNull();
		var operation = EdeltaFactory.eINSTANCE.createEdeltaModifyEcoreOperation();
		var body = XbaseFactory.eINSTANCE.createXBlockExpression();
		body.getExpressions().add(candidate);
		operation.setBody(body);
		program.getModifyEcoreOperations().add(operation);
		var resource = new ResourceImpl();
		resource.getContents().add(program);
		assertThat(EdeltaModelPreviewInterpreter.userMigrationExpression(program, candidate))
			.isNull();
		var emptyNode = new NodeModelBuilder();
		emptyNode.associateWithSemanticElement(emptyNode.newRootNode(""), candidate);
		assertThat(EdeltaModelPreviewInterpreter.userMigrationExpression(program, candidate))
			.isNull();
		var unrelated = XbaseFactory.eINSTANCE.createXStringLiteral();
		resource.getContents().add(unrelated);
		assertThat(EdeltaModelPreviewInterpreter.userMigrationExpression(program, unrelated))
			.isNull();
		var external = XbaseFactory.eINSTANCE.createXStringLiteral();
		new ResourceImpl().getContents().add(external);
		assertThat(EdeltaModelPreviewInterpreter.userMigrationExpression(program, external))
			.isNull();
	}

	@Test
	public void customExtensionsKeepPreviewCrossResourceAssociations() throws Exception {
		var root = projectWithModels();
		var models = root.resolve("models");
		var rootFile = models.resolve("MyRoot.library");
		var classFile = models.resolve("MyClass.persons");
		Files.copy(models.resolve("MyRoot.xmi"), rootFile);
		Files.copy(models.resolve("MyClass.xmi"), classFile);
		Files.writeString(rootFile, Files.readString(rootFile)
			.replace("myReferences=\"//@myContents.1\"",
				"myReferences=\"MyClass.persons#/\""));
		var rootText = Files.readString(rootFile);
		var classText = Files.readString(classFile);
		var program = parse(root, true, """
			metamodel "mypackage"
			testmodels { "models/MyRoot.library" "models/MyClass.persons" }
			modifyEcore evolve epackage mypackage {}
			""");
		var preview = derivedStateHelper.getModelPreview(program.eResource());
		assertThat(preview.getFailures()).isEmpty();
		assertThat(preview.getModels()).extracting(
			m -> m.sourcePath().getFileName().toString())
			.containsExactly("MyRoot.library", "MyClass.persons");
		assertThat(preview.getModels()).extracting(EdeltaModelPreview.Model::declaration)
			.containsExactlyElementsOf(program.getTestModels().getPaths());
		var rootModel = preview.getModels().get(0).resource().getContents().getFirst();
		var classModel = preview.getModels().get(1).resource().getContents().getFirst();
		assertThat(references(rootModel, "myReferences")).containsExactly(classModel);
		assertThat(Files.readString(rootFile)).isEqualToNormalizingNewlines(rootText);
		assertThat(Files.readString(classFile)).isEqualToNormalizingNewlines(classText);
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
		var sourceText = Files.readString(rootFile);
		var preview = derivedStateHelper.getModelPreview(program.eResource());
		assertThat(preview.getFailures()).isEmpty();
		var model = preview.getModels().getFirst().resource().getContents().getFirst();
		var contents = references(model, "myContents");
		assertThat(value(model, "myReferences")).isSameAs(contents.getFirst());
		assertThat(model.eClass().getEStructuralFeature("myReferences").isMany()).isFalse();
		var engineManager = executeWithEngineChangeToSingle(root);
		assertThat(semanticSnapshot(model))
			.isEqualTo(semanticSnapshot(modelRoot(engineManager, "MyRoot.xmi")));
		assertThat(Files.readString(rootFile)).isEqualToNormalizingNewlines(sourceText);
	}

	@Test
	public void relativeSchemaLocationUsesTheImportedEcoreCopy() throws Exception {
		var root = projectWithModels();
		var input = root.resolve("models/MyRoot.library");
		Files.move(root.resolve("models/MyRoot.xmi"), input);
		var sourceText = Files.readString(input);
		assertThat(Files.readString(input)).contains("xsi:schemaLocation",
			"http://my.package.org My.ecore");
		var program = parse(root, true, """
			metamodel "mypackage"
			testmodels { "models/MyRoot.library" }
			modifyEcore evolve epackage mypackage {}
			""");
		var preview = derivedStateHelper.getModelPreview(program.eResource());
		assertThat(preview.getFailures()).isEmpty();
		assertThat(preview.getModels()).hasSize(1);
		var model = preview.getModels().getFirst().resource().getContents().getFirst();
		var copied = derivedStateHelper.getCopiedEPackagesMap(program.eResource()).get("mypackage");
		assertThat(model.eClass()).isSameAs(copied.getEClassifier("MyRoot"));
		assertThat(Files.readString(input)).isEqualToNormalizingNewlines(sourceText);
	}

	@Test
	public void directoryPreviewDiscoversCustomExtensionsAndIgnoresUnrelatedFiles()
			throws Exception {
		var root = projectWithModels();
		var models = root.resolve("models");
		Files.move(models.resolve("MyRoot.xmi"), models.resolve("MyRoot.library"));
		Files.move(models.resolve("MyClass.xmi"), models.resolve("MyClass.persons"));
		Files.writeString(models.resolve("notes.txt"), "not a model");
		Files.writeString(models.resolve("unrelated.xml"),
			"<other:Thing xmlns:other=\"http://other.example\"/>");
		Files.writeString(models.resolve("broken.xml"), "<not recognizable");
		var sourceText = Files.readString(models.resolve("MyRoot.library"));
		var program = parse(root, true, """
			metamodel "mypackage"
			testmodels { "models" }
			modifyEcore evolve epackage mypackage {}
			""");
		var preview = derivedStateHelper.getModelPreview(program.eResource());
		assertThat(preview.getFailures()).isEmpty();
		assertThat(preview.getModels()).extracting(
			m -> m.sourcePath().getFileName().toString())
			.containsExactly("MyClass.persons", "MyRoot.library");
		assertThat(preview.getModels()).extracting(EdeltaModelPreview.Model::declaration)
			.containsOnly(program.getTestModels().getPaths().getFirst());
		assertThat(Files.readString(models.resolve("MyRoot.library")))
			.isEqualToNormalizingNewlines(sourceText);
	}

	@Test
	public void emptyAndMultiRootInputsKeepResourceAndReferenceStructure() throws Exception {
		var root = projectWithModels();
		var firstPath = root.resolve("models/MultiRoot.xmi");
		var secondPath = root.resolve("models/Reference.xmi");
		writeMultipleRootModels(firstPath, secondPath);
		var sourceFirstText = Files.readString(firstPath);
		var sourceSecondText = Files.readString(secondPath);
		var emptyPath = root.resolve("models/Empty.xmi");
		Files.writeString(emptyPath, """
			<?xml version="1.0" encoding="UTF-8"?>
			<xmi:XMI xmlns:xmi="http://www.omg.org/XMI"/>
			""");

		var program = parse(root, true, """
			metamodel "mypackage"
			testmodels { "models/MultiRoot.xmi" "models/Reference.xmi" "models/Empty.xmi" }
			modifyEcore evolve epackage mypackage {}
			""");
		var preview = derivedStateHelper.getModelPreview(program.eResource());
		assertThat(preview.getFailures()).isEmpty();
		assertThat(preview.getModels()).hasSize(2);
		assertThat(preview.getModels()).extracting(m -> m.sourcePath().getFileName().toString())
			.containsExactly("MultiRoot.xmi", "Reference.xmi");
		assertThat(preview.getModels()).extracting(EdeltaModelPreview.Model::declaration)
			.containsExactly(program.getTestModels().getPaths().get(0),
				program.getTestModels().getPaths().get(1));

		var firstResource = preview.getModels().get(0).resource();
		var secondResource = preview.getModels().get(1).resource();
		assertThat(firstResource.getContents()).hasSize(2);
		var firstRoot = firstResource.getContents().get(0);
		var nonFirstRoot = firstResource.getContents().get(1);
		var secondResourceRoot = secondResource.getContents().getFirst();
		assertThat(firstRoot.eClass().getName()).isEqualTo("MyRoot");
		assertThat(nonFirstRoot.eClass().getName()).isEqualTo("MyClass");
		assertThat(value(nonFirstRoot, "myClassStringAttribute"))
			.isEqualTo("target in the second root");
		assertThat(references(firstRoot, "myReferences")).containsExactly(nonFirstRoot);
		assertThat(references(secondResourceRoot, "myReferences")).containsExactly(nonFirstRoot);
		assertThat(secondResourceRoot.eClass()).isSameAs(firstRoot.eClass());
		assertThat(Files.readString(firstPath)).isEqualToNormalizingNewlines(sourceFirstText);
		assertThat(Files.readString(secondPath)).isEqualToNormalizingNewlines(sourceSecondText);
	}

	@Test
	public void invalidSelectedPathProducesFailureWithoutPartialPreview() throws Exception {
		var root = projectWithModels();
		var program = parse(root, true, """
			metamodel "mypackage"
			testmodels { "models/MyClass.xmi" "../outside.xmi" }
			modifyEcore evolve epackage mypackage {}
			""");
		var preview = derivedStateHelper.getModelPreview(program.eResource());
		assertThat(preview.getModels()).isEmpty();
		assertThat(preview.getFailures()).extracting(EdeltaModelPreview.Failure::code)
			.containsExactly("INVALID_PATH");
		assertThat(preview.getFailures().getFirst().source())
			.isSameAs(program.getTestModels().getPaths().get(1));
	}

	@Test
	public void emptyXmiWithoutAnImportedNamespaceIsIgnored() throws Exception {
		var root = projectWithModels();
		Files.writeString(root.resolve("models/Empty.xmi"), """
			<?xml version="1.0" encoding="UTF-8"?>
			<xmi:XMI xmlns:xmi="http://www.omg.org/XMI"/>
			""");
		var program = parse(root, true, """
			metamodel "mypackage"
			testmodels { "models/Empty.xmi" }
			modifyEcore evolve epackage mypackage {}
			""");
		var preview = derivedStateHelper.getModelPreview(program.eResource());
		assertThat(preview.getModels()).isEmpty();
		assertThat(preview.getFailures()).isEmpty();
	}

	/** Reproduces a source-interpreted library migration with two borrowed EPackages. */
	@Test
	public void interpretedLibraryKeepsBothBorrowedPackagesAndTheirMappings() throws Exception {
		var root = projectWithModels();
		var rootFile = root.resolve("models/MyRoot.xmi");
		Files.writeString(rootFile, Files.readString(rootFile)
			.replace("myReferences=\"//@myContents.1\"",
				"myReferences=\"//@myContents.0 //@myContents.1\""));
		var inputText = Files.readString(rootFile);
		var name = "edeltaPreviewLibrary" + UUID.randomUUID().toString().replace("-", "");
		var workspace = ResourcesPlugin.getWorkspace();
		var project = workspace.getRoot().getProject(name);
		var description = workspace.newProjectDescription(name);
		description.setLocationURI(root.toUri());
		project.create(description, null);
		project.open(null);
		try {
			var resourceSet = resourceSetProvider.get();
			ProjectConfigAdapter.install(resourceSet, new FileProjectConfig(
				URI.createPlatformResourceURI(name, true), name));
			resourceSet.getResources().add(new EdeltaModelManager().loadEcoreFile(
				DATA.resolve("My.ecore").toString()));
			resourceSet.getResource(createFileURIFromPath(METAMODEL_PATH + ECORE_ECORE), true);
			// Loading source forces the cross-source interpreter path used by the editor.
			parseHelper.parse("""
				import org.eclipse.emf.ecore.EAttribute
				import org.eclipse.emf.ecore.EStructuralFeature
				package previewrefactorings
				metamodel "ecore"
				def changeToSingle(EStructuralFeature feature) {
					feature.upperBound = 1
					modelMigration[
						copyRule(isRelatedTo(feature), multiplicityAwareCopy(feature))
					]
				}
				def uppercase(EAttribute feature) {
					modelMigration[
						transformAttributeValueRule(isRelatedTo(feature), [value |
							value.toString.toUpperCase
						])
					]
				}
				""", URI.createFileURI(root.resolve("src/EdeltaRefactorings.edelta").toString()),
				resourceSet);
			var program = parseHelper.parse("""
				metamodel "mypackage"
				metamodel "ecore"
				testmodels { "models/MyRoot.xmi" }
				use previewrefactorings.EdeltaRefactorings as extension refactorings
				modifyEcore evolve epackage mypackage {
					refactorings.changeToSingle(ecoreref(MyRoot.myReferences))
					refactorings.uppercase(ecoreref(MyClass.myClassStringAttribute))
				}
				""", URI.createFileURI(root.resolve("src/LibraryRefactoring.edelta").toString()),
				resourceSet);
			var preview = derivedStateHelper.getModelPreview(program.eResource());
			assertThat(preview.getFailures()).isEmpty();
			assertThat(preview.getModels()).hasSize(1);
			var copies = derivedStateHelper.getCopiedEPackagesMap(program.eResource());
			var originalMy = program.getEPackages().get(0);
			var originalEcore = program.getEPackages().get(1);
			var evolvedMy = copies.get("mypackage");
			var evolvedEcore = copies.get("ecore");
			assertThat(copies.getOriginalToCopyMap().get(originalMy)).isSameAs(evolvedMy);
			assertThat(copies.getOriginalToCopyMap().get(originalEcore)).isSameAs(evolvedEcore);
			var originalFeature = ((EClass)
				originalMy.getEClassifier("MyRoot")).getEStructuralFeature("myReferences");
			var evolvedFeature = ((EClass)
				evolvedMy.getEClassifier("MyRoot")).getEStructuralFeature("myReferences");
			assertThat(copies.getOriginalToCopyMap().get(originalFeature)).isSameAs(evolvedFeature);
			assertThat(copies.getOriginalToCopyMap().get(
				originalEcore.getEClassifier("EAttribute")))
				.isSameAs(evolvedEcore.getEClassifier("EAttribute"));
			assertThat(copies.getOriginal(evolvedFeature)).isSameAs(originalFeature);
			assertThat(evolvedMy.eResource()).isSameAs(program.eResource());
			assertThat(evolvedEcore.eResource()).isSameAs(program.eResource());
			assertThat(evolvedFeature.isMany()).isFalse();
			var migrated = preview.getModels().getFirst().resource().getContents().getFirst();
			var contents = references(migrated, "myContents");
			var attribute = (EAttribute) contents.getFirst().eClass()
				.getEStructuralFeature("myClassStringAttribute");
			var sourceExpression = preview.getMigrationExpression(contents.getFirst(), attribute);
			assertThat(sourceExpression.eResource()).isSameAs(program.eResource());
			assertThat(NodeModelUtils.findActualNodeFor(sourceExpression).getText())
				.contains("refactorings.uppercase");
			assertThat(value(migrated, "myReferences")).isSameAs(contents.getFirst());
			assertThat(migrated.eClass()).isSameAs(evolvedMy.getEClassifier("MyRoot"));
			assertThat(Files.readString(rootFile)).isEqualToNormalizingNewlines(inputText);
		} finally {
			project.delete(false, true, null);
		}
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
		List<Resource> missing = List.of(first);
		List<Resource> reordered = List.of(second, first);
		assertThatThrownBy(() -> EdeltaModelPreview.associate(selected, missing))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("count");
		assertThatThrownBy(() -> EdeltaModelPreview.associate(selected, reordered))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("order");
	}

	@Test
	public void failedPreviewCannotPublishModels() {
		var preview = new EdeltaModelPreview();
		preview.addFailure(new EdeltaModelPreview.Failure("MIGRATION", null, null, null));
		List<EdeltaModelPreview.Model> models = List.of();
		assertThatThrownBy(() -> preview.setModels(models))
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
		assertThat(newPreview.getModels().getFirst().sourcePath().getFileName())
			.hasToString("MyRoot.xmi");
		assertThat(newPreview.getModels().getFirst().resource().getContents().getFirst())
			.isNotSameAs(oldObject);
	}

	private Path projectWithModels() throws Exception {
		var root = folder.getRoot().toPath();
		Files.createDirectories(root.resolve("src"));
		Files.createDirectories(root.resolve("models"));
		Files.copy(DATA.resolve("My.ecore"), root.resolve("models/My.ecore"));
		Files.copy(DATA.resolve("MyClass.xmi"), root.resolve("models/MyClass.xmi"));
		Files.copy(DATA.resolve("MyRoot.xmi"), root.resolve("models/MyRoot.xmi"));
		return root;
	}

	private void writeMultipleRootModels(Path firstPath, Path secondPath) throws Exception {
		var ecore = new EdeltaModelManager().loadEcoreFile(DATA.resolve("My.ecore").toString());
		var ePackage = EdeltaResourceUtils.getEPackage(ecore);
		var resourceSet = new ResourceSetImpl();
		resourceSet.getPackageRegistry().put(ePackage.getNsURI(), ePackage);
		resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap()
			.put("xmi", new XMIResourceFactoryImpl());
		var firstResource = resourceSet.createResource(URI.createFileURI(firstPath.toString()));
		var secondResource = resourceSet.createResource(URI.createFileURI(secondPath.toString()));
		var rootClass = (EClass) ePackage.getEClassifier("MyRoot");
		var classClass = (EClass) ePackage.getEClassifier("MyClass");
		var firstRoot = ePackage.getEFactoryInstance().create(rootClass);
		var nonFirstRoot = ePackage.getEFactoryInstance().create(classClass);
		nonFirstRoot.eSet(classClass.getEStructuralFeature("myClassStringAttribute"),
			"target in the second root");
		var secondResourceRoot = ePackage.getEFactoryInstance().create(rootClass);
		firstRoot.eSet(rootClass.getEStructuralFeature("myReferences"), List.of(nonFirstRoot));
		secondResourceRoot.eSet(rootClass.getEStructuralFeature("myReferences"),
			List.of(nonFirstRoot));
		firstResource.getContents().add(firstRoot);
		firstResource.getContents().add(nonFirstRoot);
		secondResource.getContents().add(secondResourceRoot);
		firstResource.save(null);
		secondResource.save(null);
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
		return parseHelper.parse(source, uri, resourceSet);
	}

	private Object value(EObject object, String feature) {
		return object.eGet(object.eClass().getEStructuralFeature(feature));
	}

	@SuppressWarnings("unchecked")
	private List<EObject> references(EObject object, String feature) {
		return (List<EObject>) value(object, feature);
	}

	private EdeltaModelManager executeWithSuccessiveEngineMigrations(Path root) throws Exception {
		var engine = new EdeltaEngine(other -> new EdeltaDefaultRuntime(other) {
			@Override
			protected void doExecute() {
				var attribute = getEAttribute("mypackage", "MyClass",
					"myClassStringAttribute");
				modelMigration(migrator -> migrator.transformAttributeValueRule(
					migrator.isRelatedTo(attribute), value -> value.toString().toUpperCase()));
				modelMigration(migrator -> migrator.transformAttributeValueRule(
					migrator.isRelatedTo(attribute), value -> value + "!"));
			}
		});
		loadEngineInputs(engine, root, "MyRoot.xmi", "MyClass.xmi");
		engine.execute();
		return engine.getEvolvingModelManager();
	}

	private EdeltaModelManager executeWithEngineChangeToSingle(Path root) throws Exception {
		var engine = new EdeltaEngine(other -> new EdeltaRefactorings(other) {
			@Override
			protected void doExecute() {
				changeToSingle(getEClass("mypackage", "MyRoot")
					.getEStructuralFeature("myReferences"));
			}
		});
		loadEngineInputs(engine, root, "MyRoot.xmi");
		engine.execute();
		return engine.getEvolvingModelManager();
	}

	private void loadEngineInputs(EdeltaEngine engine, Path root, String... models) {
		engine.loadEcoreFile(root.resolve("models/My.ecore").toString());
		for (var model : models) {
			engine.loadModelFile(root.resolve("models").resolve(model).toString());
		}
	}

	private EObject modelRoot(EdeltaModelManager manager, String fileName) {
		return manager.getModelResources().stream()
			.filter(resource -> resource.getURI().lastSegment().equals(fileName))
			.findFirst().orElseThrow().getContents().getFirst();
	}

	private Map<String, Object> semanticSnapshot(EObject object) {
		var result = new LinkedHashMap<String, Object>();
		result.put("class", object.eClass().getName());
		var features = new LinkedHashMap<String, Object>();
		for (var feature : object.eClass().getEAllStructuralFeatures()) {
			var featureValue = object.eGet(feature);
			if (feature instanceof EReference reference) {
				featureValue = reference.isContainment()
					? mapReferences(featureValue, this::semanticSnapshot)
					: mapReferences(featureValue, this::semanticLabel);
			} else if (featureValue instanceof List<?> values) {
				featureValue = List.copyOf(values);
			}
			features.put(feature.getName(), featureValue);
		}
		result.put("features", features);
		return result;
	}

	private Object mapReferences(Object value,
			java.util.function.Function<EObject, Object> mapper) {
		if (value instanceof List<?> values) {
			return values.stream().map(EObject.class::cast).map(mapper).toList();
		}
		return value instanceof EObject target ? mapper.apply(target) : value;
	}

	private String semanticLabel(EObject object) {
		var attributes = new LinkedHashMap<String, Object>();
		for (var attribute : object.eClass().getEAllAttributes()) {
			attributes.put(attribute.getName(), object.eGet(attribute));
		}
		return object.eClass().getName() + attributes;
	}
}
