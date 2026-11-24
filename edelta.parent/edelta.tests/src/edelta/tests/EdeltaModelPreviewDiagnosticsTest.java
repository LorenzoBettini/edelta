package edelta.tests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import org.eclipse.emf.common.util.URI;
import org.eclipse.xtext.diagnostics.Severity;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;
import org.eclipse.xtext.resource.DerivedStateAwareResource;
import org.eclipse.xtext.resource.XtextResourceSet;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.XtextRunner;
import org.eclipse.xtext.validation.Issue;
import org.eclipse.xtext.workspace.FileProjectConfig;
import org.eclipse.xtext.workspace.ProjectConfigAdapter;
import org.junit.Assume;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;

import com.google.inject.Inject;
import com.google.inject.Provider;

import edelta.edelta.EdeltaProgram;
import edelta.lib.EdeltaModelManager;
import edelta.resource.derivedstate.EdeltaModelPreview;
import edelta.validation.EdeltaValidator;

/**
 * Checks source-located preview issues and their derived-state lifetime.
 *
 * @author Lorenzo Bettini
 */
@RunWith(XtextRunner.class)
@InjectWith(EdeltaInjectorProvider.class)
public class EdeltaModelPreviewDiagnosticsTest extends EdeltaAbstractTest {
	private static final Path DATA = Path.of("../edelta.testdata/testdata/simpleTestData");

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	@Inject
	private Provider<XtextResourceSet> resourceSetProvider;

	@Test
	public void failingMigrationLambdaPointsToTheFailingCall() throws Exception {
		var root = project();
		var source = """
			metamodel "mypackage"
			testmodels { "models/MyClass.xmi" }
			modifyEcore evolve epackage mypackage {
				val attribute = ecoreref(MyClass.myClassStringAttribute)
				modelMigration[
					transformAttributeValueRule(isRelatedTo(attribute), [value |
						Integer.parseInt(value.toString).toString
					])
				]
			}
			""";
		var program = parse(root, source);
		var issue = onlyPreviewIssue(program);
		assertIssue(issue, EdeltaValidator.MODEL_PREVIEW_FAILURE, source,
			"Integer.parseInt(value.toString)");
		assertThat(issue.getMessage())
			.startsWith("Model preview migration failed for '")
			.contains("MyClass.xmi' (resource root MyClass",
				"NumberFormatException: For input string:")
			.doesNotContain("\n  at java.", "\n  at org.eclipse.");
		assertThat(issue.getMessage().lines().filter(line -> line.startsWith("  at ")).toList())
			.hasSizeLessThanOrEqualTo(3);
		assertFailedPreview(program, "MIGRATION");
		var modelPath = root.resolve("models/MyClass.xmi");
		Files.writeString(modelPath, Files.readString(modelPath)
			.replace("My Attribute Value", "42"));
		var resource = (DerivedStateAwareResource) program.eResource();
		resource.discardDerivedState();
		resource.installDerivedState(false);
		assertThat(previewIssues(program)).isEmpty();
		assertThat(derivedStateHelper.getModelPreview(resource).getModels()).hasSize(1);
	}

	@Test
	public void failureInSecondModelNamesTheSecondXmiFile() throws Exception {
		var root = project();
		Files.writeString(root.resolve("models/MyRoot.xmi"), Files.readString(
			DATA.resolve("MyRoot.xmi"))
			.replace("contained not referred", "1")
			.replace("contained referred", "2"));
		var source = """
			metamodel "mypackage"
			testmodels { "models/MyRoot.xmi" "models/MyClass.xmi" }
			modifyEcore evolve epackage mypackage {
				val attribute = ecoreref(MyClass.myClassStringAttribute)
				modelMigration[
					transformAttributeValueRule(isRelatedTo(attribute), [value |
						Integer.parseInt(value.toString).toString
					])
				]
			}
			""";
		var program = parse(root, source);
		var issue = onlyPreviewIssue(program);
		assertThat(issue.getMessage()).contains("MyClass.xmi", "MyClass");
		assertThat(issue.getMessage()).doesNotContain("MyRoot.xmi");
		assertFailedPreview(program, "MIGRATION");
	}

	@Test
	public void interpretationFailureUsesTheOperationThatActuallyRan() throws Exception {
		var root = project();
		var source = """
			metamodel "mypackage"
			testmodels { "models/MyClass.xmi" }
			modifyEcore first epackage mypackage {
				throw new NullPointerException("first operation failed")
			}
			modifyEcore second epackage mypackage {
				val unrelated = 42
			}
			""";
		var program = parse(root, source);
		var issue = onlyPreviewIssue(program);
		var firstBody = program.getModifyEcoreOperations().getFirst().getBody();
		var firstNode = NodeModelUtils.getNode(firstBody);
		assertThat(issue.getOffset()).isEqualTo(firstNode.getOffset());
		assertThat(issue.getLength()).isEqualTo(firstNode.getLength());
		assertThat(issue.getMessage())
			.startsWith("Model preview interpretation failed for '")
			.contains("MyClass.xmi'", "NullPointerException: first operation failed");
		assertFailedPreview(program, "INTERPRETATION");
	}

	@Test
	public void importedRefactoringFailurePointsToUserInvocation() throws Exception {
		var root = project();
		Files.copy(DATA.resolve("MyRoot.xmi"), root.resolve("models/MyRoot.xmi"));
		var resourceSet = resourceSet(root);
		var library = parseHelper.parse("""
			import org.eclipse.emf.ecore.EPackage
			package example
			def fail(EPackage it) {
				modelMigration[throw new IllegalStateException("library failure")]
			}
			""", URI.createFileURI(root.resolve("src/FailingRefactorings.edelta").toString()),
			resourceSet);
		var source = """
			metamodel "mypackage"
			testmodels { "models/MyClass.xmi" "models/MyRoot.xmi" }
			use example.FailingRefactorings as extension refactorings
			modifyEcore evolve epackage mypackage {
				val unrelated = 42
				refactorings.fail(it)
			}
			""";
		var program = parse(root, source, resourceSet);
		var issue = onlyPreviewIssue(program);
		assertIssue(issue, EdeltaValidator.MODEL_PREVIEW_FAILURE, source,
			"refactorings.fail(it)");
		assertThat(issue.getMessage())
			.startsWith("Model preview migration failed for ")
			.contains("MyClass.xmi", "MyRoot.xmi",
				"IllegalStateException: library failure");
		assertThat(library.eResource().getErrors()).noneMatch(d ->
			d instanceof org.eclipse.xtext.validation.EObjectDiagnosticImpl diagnostic &&
			EdeltaValidator.MODEL_PREVIEW_FAILURE.equals(diagnostic.getCode()));
		assertFailedPreview(program, "MIGRATION");
	}

	@Test
	public void missingAndMalformedInputsPointToTheirOwnQuotedEntries() throws Exception {
		var root = project();
		Files.writeString(root.resolve("models/broken.xmi"), "<broken");
		var source = """
			metamodel "mypackage"
			testmodels { "models/missing.xmi" "models/broken.xmi" }
			modifyEcore evolve epackage mypackage {}
			""";
		var program = parse(root, source);
		var issues = previewIssues(program);
		assertThat(issues).hasSize(2);
		assertIssue(issues.get(0), EdeltaValidator.MODEL_PREVIEW_INPUT_FAILURE, source,
			"\"models/missing.xmi\"");
		assertThat(issues.get(0).getMessage()).contains("missing.xmi", "does not exist");
		assertIssue(issues.get(1), EdeltaValidator.MODEL_PREVIEW_INPUT_FAILURE, source,
			"\"models/broken.xmi\"");
		assertThat(issues.get(1).getMessage()).contains("broken.xmi", "Malformed XMI");
		assertThat(derivedStateHelper.getModelPreview(program.eResource()).getModels()).isEmpty();
	}

	@Test
	public void invalidPathPointsToItsQuotedEntry() throws Exception {
		var root = project();
		var source = """
			metamodel "mypackage"
			testmodels { "../outside.xmi" }
			modifyEcore evolve epackage mypackage {}
			""";
		var issue = onlyPreviewIssue(parse(root, source));
		assertIssue(issue, EdeltaValidator.MODEL_PREVIEW_INPUT_FAILURE, source,
			"\"../outside.xmi\"");
		assertThat(issue.getMessage()).contains("Invalid", "outside.xmi");
	}

	@Test
	public void blankPathStillProducesALocatedPreviewIssue() throws Exception {
		var root = project();
		var source = """
			metamodel "mypackage"
			testmodels { " " }
			modifyEcore evolve epackage mypackage {}
			""";
		var issue = onlyPreviewIssue(parse(root, source));
		assertIssue(issue, EdeltaValidator.MODEL_PREVIEW_INPUT_FAILURE, source, "\" \"");
		assertThat(issue.getMessage()).contains("Invalid model preview input");
	}

	@Test
	public void mixedNamespaceAndFailedLoadAreInputIssues() throws Exception {
		var root = project();
		Files.writeString(root.resolve("models/mixed.xmi"), """
			<xmi:XMI xmlns:xmi="http://www.omg.org/XMI"
			 xmlns:mypackage="http://my.package.org" xmlns:other="http://other.example">
			 <mypackage:MyClass/><other:Unknown/>
			</xmi:XMI>
			""");
		Files.writeString(root.resolve("models/unknown.xmi"), """
			<mypackage:Unknown xmlns:mypackage="http://my.package.org"/>
			""");
		var source = """
			metamodel "mypackage"
			testmodels { "models/mixed.xmi" "models/unknown.xmi" }
			modifyEcore evolve epackage mypackage {}
			""";
		var issues = previewIssues(parse(root, source));
		assertThat(issues).hasSize(2);
		assertIssue(issues.get(0), EdeltaValidator.MODEL_PREVIEW_INPUT_FAILURE, source,
			"\"models/mixed.xmi\"");
		assertThat(issues.get(0).getMessage()).contains("unknown namespace");
		assertIssue(issues.get(1), EdeltaValidator.MODEL_PREVIEW_INPUT_FAILURE, source,
			"\"models/unknown.xmi\"");
		assertThat(issues.get(1).getMessage()).contains("Cannot load");
	}

	@Test
	public void unreadableInputPointsToItsQuotedEntry() throws Exception {
		var root = project();
		var path = root.resolve("models/unreadable.xmi");
		Files.copy(DATA.resolve("MyClass.xmi"), path);
		Assume.assumeTrue(Files.getFileStore(path).supportsFileAttributeView("posix"));
		var original = Files.getPosixFilePermissions(path);
		try {
			Files.setPosixFilePermissions(path, Set.of());
			var source = """
				metamodel "mypackage"
				testmodels { "models/unreadable.xmi" }
				modifyEcore evolve epackage mypackage {}
				""";
			var issue = onlyPreviewIssue(parse(root, source));
			assertIssue(issue, EdeltaValidator.MODEL_PREVIEW_INPUT_FAILURE, source,
				"\"models/unreadable.xmi\"");
			assertThat(issue.getMessage()).contains("Cannot read", "unreadable.xmi");
		} finally {
			Files.setPosixFilePermissions(path, original);
		}
	}

	@Test
	public void correctedInputClearsIssueAndReinstallationDoesNotDuplicateIt() throws Exception {
		var root = project();
		var source = """
			metamodel "mypackage"
			testmodels { "models/fixed.xmi" }
			modifyEcore evolve epackage mypackage {}
			""";
		var program = parse(root, source);
		assertThat(previewIssues(program)).hasSize(1);
		var resource = (DerivedStateAwareResource) program.eResource();
		resource.discardDerivedState();
		resource.installDerivedState(false);
		assertThat(previewIssues(program)).hasSize(1);
		Files.copy(DATA.resolve("MyClass.xmi"), root.resolve("models/fixed.xmi"));
		resource.discardDerivedState();
		resource.installDerivedState(false);
		assertThat(previewIssues(program)).isEmpty();
		assertThat(derivedStateHelper.getModelPreview(resource).getModels()).hasSize(1);
		resource.discardDerivedState();
		resource.installDerivedState(false);
		assertThat(previewIssues(program)).isEmpty();
	}

	@Test
	public void programWithoutPreviewHasNoPreviewDiagnostics() throws Exception {
		var root = project();
		var source = """
			metamodel "mypackage"
			modifyEcore evolve epackage mypackage {
				modelMigration[throw new IllegalStateException("must not run")]
			}
			""";
		var program = parse(root, source);
		assertThatCode(() -> validationTestHelper.validate(program)).doesNotThrowAnyException();
		assertThat(previewIssues(program)).isEmpty();
		assertThat(derivedStateHelper.getModelPreview(program.eResource()).getFailures()).isEmpty();
	}

	private Path project() throws Exception {
		var root = folder.getRoot().toPath();
		Files.createDirectories(root.resolve("src"));
		Files.createDirectories(root.resolve("models"));
		Files.copy(DATA.resolve("MyClass.xmi"), root.resolve("models/MyClass.xmi"));
		return root;
	}

	private XtextResourceSet resourceSet(Path root) {
		var resourceSet = resourceSetProvider.get();
		ProjectConfigAdapter.install(resourceSet,
			new FileProjectConfig(root.toFile(), "preview"));
		var ecore = new EdeltaModelManager().loadEcoreFile(DATA.resolve("My.ecore").toString());
		resourceSet.getResources().add(ecore);
		return resourceSet;
	}

	private EdeltaProgram parse(Path root, String source) throws Exception {
		return parse(root, source, resourceSet(root));
	}

	private EdeltaProgram parse(Path root, String source, XtextResourceSet resourceSet)
			throws Exception {
		return parseHelper.parse(source,
			URI.createFileURI(root.resolve("src/Preview.edelta").toString()), resourceSet);
	}

	private List<Issue> previewIssues(EdeltaProgram program) {
		return validationTestHelper.validate(program).stream()
			.filter(issue -> EdeltaValidator.MODEL_PREVIEW_INPUT_FAILURE.equals(issue.getCode()) ||
				EdeltaValidator.MODEL_PREVIEW_FAILURE.equals(issue.getCode()))
			.toList();
	}

	private Issue onlyPreviewIssue(EdeltaProgram program) {
		var issues = previewIssues(program);
		assertThat(issues).hasSize(1);
		return issues.getFirst();
	}

	private void assertIssue(Issue issue, String code, String source, String text) {
		assertThat(issue.getCode()).isEqualTo(code);
		assertThat(issue.getSeverity()).isEqualTo(Severity.ERROR);
		assertThat(issue.getOffset()).isEqualTo(source.indexOf(text));
		assertThat(issue.getLength()).isEqualTo(text.length());
	}

	private void assertFailedPreview(EdeltaProgram program, String code) {
		var preview = derivedStateHelper.getModelPreview(program.eResource());
		assertThat(preview.getModels()).isEmpty();
		assertThat(preview.getFailures()).extracting(EdeltaModelPreview.Failure::code)
			.containsExactly(code);
	}
}
