package edelta.resource;

import static org.eclipse.xtext.EcoreUtil2.getAllContentsOfType;

import java.nio.file.Path;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.xtext.parser.antlr.IReferableElementsUnloader;
import org.eclipse.xtext.resource.DerivedStateAwareResource;
import org.eclipse.xtext.workspace.IProjectConfigProvider;
import org.eclipse.xtext.xbase.jvmmodel.JvmModelAssociator;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import edelta.edelta.EdeltaEcoreReferenceExpression;
import edelta.edelta.EdeltaProgram;
import edelta.interpreter.EdeltaInterpreterFactory;
import edelta.interpreter.EdeltaInterpreterHelper;
import edelta.interpreter.EdeltaModelPreviewInterpreter;
import edelta.resource.derivedstate.EdeltaCopiedEPackagesMap;
import edelta.resource.derivedstate.EdeltaDerivedStateHelper;
import edelta.resource.derivedstate.EdeltaModelPreview;
import edelta.scoping.EdeltaOriginalENamedElementRecorder;

@Singleton
public class EdeltaDerivedStateComputer extends JvmModelAssociator {
	@Inject
	private EdeltaDerivedStateHelper derivedStateHelper;

	@Inject
	private IReferableElementsUnloader.GenericUnloader unloader;

	@Inject
	private EdeltaInterpreterFactory interpreterFactory;

	@Inject
	private EdeltaInterpreterHelper interpreterHelper;

	@Inject
	private EdeltaOriginalENamedElementRecorder originalENamedElementRecorder;

	@Inject
	private EdeltaModelSourceSelector modelSourceSelector;

	@Inject
	private IProjectConfigProvider projectConfigProvider;

	@Override
	public void installDerivedState(final DerivedStateAwareResource resource, final boolean preIndexingPhase) {
		super.installDerivedState(resource, preIndexingPhase);
		final var program = (EdeltaProgram) resource.getContents().get(0);
		if ((!preIndexingPhase)) {
			final var modifyEcoreOperations = interpreterHelper
					.filterOperations(program.getModifyEcoreOperations());
			if (modifyEcoreOperations.isEmpty()) {
				return;
			}
			// make sure packages of the program are copied
			copyEPackages(program);
			// record original ecore references before running the interpreter
			recordEcoreReferenceOriginalENamedElement(resource);
			// run the interpreter
			runInterpreter(program);
		}
	}

	protected void copyEPackages(EdeltaProgram program) {
		derivedStateHelper.copyEPackages(program);
	}

	protected void runInterpreter(final EdeltaProgram program) {
		final var interpreter = interpreterFactory.create(program.eResource());
		if (program.getTestModels() != null) {
			final var preview = derivedStateHelper.getModelPreview(program.eResource());
			final var projectRoot = projectRoot(program.eResource());
			if (projectRoot == null) {
				preview.addFailure(new EdeltaModelPreview.Failure("PROJECT_ROOT_UNAVAILABLE",
					program.getTestModels(), null, null));
			} else {
				final var selection = modelSourceSelector.select(program, projectRoot);
				for (var failure : selection.failures()) {
					preview.addFailure(new EdeltaModelPreview.Failure(failure.status().name(),
						failure.entry(), failure.path(), failure.cause()));
				}
				if (preview.hasFailures() || selection.selected().isEmpty()) {
					disposeSelection(selection);
				} else if (interpreter instanceof EdeltaModelPreviewInterpreter previewInterpreter) {
					previewInterpreter.configurePreview(selection, preview, program.getTestModels());
				} else {
					preview.addFailure(new EdeltaModelPreview.Failure("INTERPRETER_UNAVAILABLE",
						program.getTestModels(), null, null));
					disposeSelection(selection);
				}
			}
		}
		interpreter.evaluateModifyEcoreOperations(program);
	}

	/**
	 * Resolves the containing project's filesystem root from Xtext configuration.
	 * A source folder or the Edelta file's parent is never used as a fallback.
	 *
	 * @param resource the Edelta program resource
	 * @return the project root, or {@code null} when no usable root is configured
	 */
	protected Path projectRoot(final Resource resource) {
		final var config = projectConfigProvider.getProjectConfig(resource.getResourceSet());
		if (config == null) {
			return null;
		}
		final var uri = config.getPath();
		if (uri.isFile()) {
			return Path.of(uri.toFileString());
		}
		if (uri.isPlatformResource()) {
			final var project = ResourcesPlugin.getWorkspace().getRoot().getProject(config.getName());
			final var location = project.getLocation();
			if (location != null) {
				return location.toFile().toPath();
			}
		}
		return null;
	}

	private void disposeSelection(final EdeltaModelSourceSelector.Result selection) {
		for (var resource : selection.resourceSet().getResources()) {
			resource.unload();
		}
		selection.resourceSet().getResources().clear();
	}

	protected void recordEcoreReferenceOriginalENamedElement(final Resource resource) {
		final var references =
			getAllContentsOfType(resource.getContents().get(0),
				EdeltaEcoreReferenceExpression.class);
		for (var r : references) {
			originalENamedElementRecorder.recordOriginalENamedElement(r.getArgument());
		}
	}

	@Override
	public void discardDerivedState(final DerivedStateAwareResource resource) {
		final var copiedEPackagesMap = derivedStateHelper.getCopiedEPackagesMap(resource);
		final var derivedState = derivedStateHelper.getOrInstallAdapter(resource);
		// Preview models still reference copied EClasses; release them first.
		derivedState.getModelPreview().clear();
		unloadDerivedPackages(copiedEPackagesMap);
		super.discardDerivedState(resource);
		derivedState.clear();
	}

	/**
	 * Unload (turn them into proxies) all derived Ecore elements
	 */
	protected void unloadDerivedPackages(final EdeltaCopiedEPackagesMap copiedEPackagesMap) {
		for (final var p : copiedEPackagesMap.values()) {
			if (p != null)
				unloader.unloadRoot(p);
		}
	}
}
