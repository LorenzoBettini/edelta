package edelta.tests;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;

import org.eclipse.emf.common.util.URI;
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
import edelta.lib.EdeltaModelManager;
import edelta.resource.derivedstate.EdeltaModelPreview;
import edelta.tests.injectors.EdeltaInjectorProviderFaultyPreviewInterpreter;

/**
 * Verifies preview failures retain normal evolved Ecore and release private models.
 *
 * @author Lorenzo Bettini
 */
@RunWith(XtextRunner.class)
@InjectWith(EdeltaInjectorProviderFaultyPreviewInterpreter.class)
public class EdeltaModelPreviewFailureTest extends EdeltaAbstractTest {
	private static final Path DATA = Path.of("../edelta.testdata/testdata/simpleTestData");

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	@Inject
	private Provider<XtextResourceSet> resourceSetProvider;

	@Test
	public void setupFailureFallsBackToEcoreInterpretation() throws Exception {
		var program = parse("setup.xmi");
		var preview = derivedStateHelper.getModelPreview(program.eResource());
		assertThat(preview.getModels()).isEmpty();
		assertThat(preview.getFailures()).extracting(EdeltaModelPreview.Failure::code)
			.containsExactly("SETUP");
		assertThat(derivedStateHelper.getCopiedEPackagesMap(program.eResource())
			.get("mypackage").getEClassifier("Renamed")).isNotNull();
	}

	@Test
	public void finalCopyFailurePublishesNoPartialModels() throws Exception {
		var program = parse("final.xmi");
		var preview = derivedStateHelper.getModelPreview(program.eResource());
		assertThat(preview.getModels()).isEmpty();
		assertThat(preview.getFailures()).extracting(EdeltaModelPreview.Failure::code)
			.containsExactly("FINAL_COPY");
		assertThat(derivedStateHelper.getCopiedEPackagesMap(program.eResource())
			.get("mypackage").getEClassifier("Renamed")).isNotNull();
	}

	private EdeltaProgram parse(String fileName) throws Exception {
		var root = folder.getRoot().toPath();
		Files.createDirectories(root.resolve("src"));
		Files.createDirectories(root.resolve("models"));
		Files.copy(DATA.resolve("MyClass.xmi"), root.resolve("models/" + fileName));
		var resourceSet = resourceSetProvider.get();
		ProjectConfigAdapter.install(resourceSet,
			new FileProjectConfig(root.toFile(), "preview"));
		var ecore = new EdeltaModelManager().loadEcoreFile(DATA.resolve("My.ecore").toString());
		resourceSet.getResources().add(ecore);
		var program = parseHelper.parse("""
			metamodel "mypackage"
			testmodels { "models/%s" }
			modifyEcore evolve epackage mypackage {
				ecoreref(MyClass).name = "Renamed"
			}
			""".formatted(fileName),
			URI.createFileURI(root.resolve("src/Preview.edelta").toString()), resourceSet);
		validationTestHelper.assertNoErrors(program);
		return program;
	}
}
