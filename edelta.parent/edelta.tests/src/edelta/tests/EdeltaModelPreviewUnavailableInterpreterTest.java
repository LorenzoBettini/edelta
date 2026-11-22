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

import edelta.EdeltaRuntimeModule;
import edelta.interpreter.EdeltaDefaultInterpreter;
import edelta.interpreter.EdeltaInterpreter;
import edelta.lib.EdeltaModelManager;
import edelta.resource.derivedstate.EdeltaModelPreview;

/**
 * A custom interpreter without preview support still evolves Ecore safely.
 *
 * @author Lorenzo Bettini
 */
@RunWith(XtextRunner.class)
@InjectWith(EdeltaModelPreviewUnavailableInterpreterTest.NoPreviewProvider.class)
public class EdeltaModelPreviewUnavailableInterpreterTest extends EdeltaAbstractTest {
	/**
	 * Binds the original interpreter, which has no preview storage hooks.
	 *
	 * @author Lorenzo Bettini
	 */
	public static class NoPreviewProvider extends EdeltaInjectorProvider {
		@Override
		protected EdeltaRuntimeModule createRuntimeModule() {
			return new EdeltaRuntimeModule() {
				@Override
				public Class<? extends EdeltaInterpreter> bindEdeltaInterpreter() {
					return EdeltaDefaultInterpreter.class;
				}
			};
		}
	}

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	@Inject
	private Provider<XtextResourceSet> resourceSetProvider;

	@Test
	public void unavailablePreviewInterpreterKeepsEcoreInterpretation() throws Exception {
		var root = folder.getRoot().toPath();
		Files.createDirectories(root.resolve("src"));
		Files.createDirectories(root.resolve("models"));
		Files.copy(Path.of("../edelta.testdata/testdata/simpleTestData/MyClass.xmi"),
			root.resolve("models/MyClass.xmi"));
		var resourceSet = resourceSetProvider.get();
		ProjectConfigAdapter.install(resourceSet,
			new FileProjectConfig(root.toFile(), "preview"));
		var ecore = new EdeltaModelManager().loadEcoreFile(
			"../edelta.testdata/testdata/simpleTestData/My.ecore");
		resourceSet.getResources().add(ecore);
		var program = parseHelper.parse("""
			metamodel "mypackage"
			testmodels { "models/MyClass.xmi" }
			modifyEcore evolve epackage mypackage {
				ecoreref(MyClass).name = "Renamed"
			}
			""", URI.createFileURI(root.resolve("src/Preview.edelta").toString()),
			resourceSet);
		validationTestHelper.assertNoErrors(program);
		var preview = derivedStateHelper.getModelPreview(program.eResource());
		assertThat(preview.getModels()).isEmpty();
		assertThat(preview.getFailures()).extracting(EdeltaModelPreview.Failure::code)
			.containsExactly("INTERPRETER_UNAVAILABLE");
		assertThat(derivedStateHelper.getCopiedEPackagesMap(program.eResource())
			.get("mypackage").getEClassifier("Renamed")).isNotNull();
	}
}
