package edelta.interpreter;

import static edelta.edelta.EdeltaPackage.Literals.EDELTA_TEST_MODEL_PATH__PATH;

import java.util.Locale;
import java.util.stream.Collectors;

import org.eclipse.emf.ecore.ENamedElement;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.xtext.diagnostics.Severity;
import org.eclipse.xtext.validation.EObjectDiagnosticImpl;
import org.eclipse.xtext.xbase.XExpression;

import com.google.inject.Inject;

import edelta.edelta.EdeltaProgram;
import edelta.edelta.EdeltaTestModelPath;
import edelta.edelta.EdeltaTestModels;
import edelta.resource.derivedstate.EdeltaDerivedStateHelper;
import edelta.resource.derivedstate.EdeltaModelPreview;
import edelta.validation.EdeltaValidator;

/**
 * For creating {@link EObjectDiagnosticImpl} on resources during interpretation.
 * 
 * @author Lorenzo Bettini
 *
 */
public class EdeltaInterpreterDiagnosticHelper {

	@Inject
	private EdeltaDerivedStateHelper derivedStateHelper;

	private XExpression currentExpression;

	private EObject fallbackSource;

	/** Sets the user program used when interpretation has no current expression. */
	public void setFallbackSource(EObject fallbackSource) {
		this.fallbackSource = fallbackSource;
		currentExpression = null;
	}

	public void setCurrentExpression(XExpression currentExpression) {
		if (fallbackSource == null || currentExpression == null ||
				currentExpression.eResource() == fallbackSource.eResource()) {
			this.currentExpression = currentExpression;
		}
	}

	public void addError(EObject problematicObject, String problemCode, String message) {
		addDiagnostic(problematicObject, problemCode, message, Severity.ERROR);
	}

	public void addWarning(EObject problematicObject, String problemCode, String message) {
		addDiagnostic(problematicObject, problemCode, message, Severity.WARNING);
	}

	private void addDiagnostic(EObject problematicObject, String problemCode, String message, Severity severity) {
		XExpression correspondingExpression = null;
		var source = currentExpression != null ? currentExpression : fallbackSource;
		if (source == null || source.eResource() == null) {
			return;
		}
		final var eResource = source.eResource();
		if (source instanceof XExpression expression &&
				problematicObject instanceof ENamedElement namedElement) {
			correspondingExpression = derivedStateHelper
				.getLastResponsibleExpression(expression, namedElement);
		}
		final var issues = 
			severity == Severity.WARNING ? eResource.getWarnings() : eResource.getErrors();
		issues.add(
			new EObjectDiagnosticImpl(severity,
				problemCode,
				message,
				correspondingExpression != null ?
						correspondingExpression : source,
				null,
				-1,
				new String[] {}));
	}

	/** Removes diagnostics from an earlier preview before derived state is reinstalled. */
	public void clearPreviewDiagnostics(Resource resource) {
		resource.getErrors().removeIf(issue -> issue instanceof EObjectDiagnosticImpl diagnostic &&
				(EdeltaValidator.MODEL_PREVIEW_INPUT_FAILURE.equals(diagnostic.getCode()) ||
				 EdeltaValidator.MODEL_PREVIEW_FAILURE.equals(diagnostic.getCode())));
	}

	/** Publishes one retained preview failure on a source element in the active program. */
	public void addPreviewFailure(EdeltaProgram program, EdeltaModelPreview.Failure failure) {
		var source = failure.source();
		if (source == null || source.eResource() != program.eResource()) {
			source = program;
		}
		var inputFailure = source instanceof EdeltaTestModelPath || source instanceof EdeltaTestModels;
		var code = inputFailure ? EdeltaValidator.MODEL_PREVIEW_INPUT_FAILURE :
			EdeltaValidator.MODEL_PREVIEW_FAILURE;
		var feature = source instanceof EdeltaTestModelPath ? EDELTA_TEST_MODEL_PATH__PATH : null;
		program.eResource().getErrors().add(new EdeltaInterpreterDiagnostic(Severity.ERROR,
			code, previewMessage(program, failure), source, feature, -1, new String[] {}));
	}

	private String previewMessage(EdeltaProgram program, EdeltaModelPreview.Failure failure) {
		var path = failure.path() != null ? failure.path().toString() :
			failure.source() instanceof EdeltaTestModelPath entry ? entry.getPath() : null;
		var detail = causeDetail(failure.cause());
		var namedPath = path == null ? "selected models" : "'" + path + "'";
		return switch (failure.code()) {
		case "MISSING" -> "Model preview input " + namedPath + " does not exist.";
		case "INVALID_PATH" -> "Invalid model preview input " + namedPath +
			": expected a project-relative .xmi file or directory.";
		case "UNREADABLE" -> "Cannot read model preview input " + namedPath + detail;
		case "MALFORMED" -> "Malformed XMI model " + namedPath + detail;
		case "MIXED_NAMESPACE" -> "Model preview input " + namedPath +
			" contains an unknown namespace.";
		case "LOAD_FAILED" -> "Cannot load model preview input " + namedPath + detail;
		case "PROJECT_ROOT_UNAVAILABLE" ->
			"Cannot resolve the project root for model preview.";
		case "INTERPRETER_UNAVAILABLE" -> "Model preview interpreter is unavailable.";
		case "SETUP" -> "Cannot initialize model preview" + detail;
		default -> {
				namedPath = previewPath(program, path, namedPath);
				var context = failure.objectContext() == null ? "" :
					" (resource root " + failure.objectContext() + ")";
				yield "Model preview " + failure.code().toLowerCase(Locale.ROOT).replace('_', ' ') +
					" failed for " + namedPath + context + detail;
			}
		};
	}

	private String previewPath(EdeltaProgram program, String path, String namedPath) {
		if (path == null && program.getTestModels() != null) {
			return program.getTestModels().getPaths().stream()
				.map(EdeltaTestModelPath::getPath).collect(Collectors.joining(", "));
		}
		return namedPath;
	}

	private String causeDetail(Throwable cause) {
		if (cause == null) {
			return ".";
		}
		while (cause.getCause() != null) {
			cause = cause.getCause();
		}
		return ": " + cause.getClass().getSimpleName() +
			(cause.getMessage() == null ? "" : ": " + cause.getMessage());
	}
}
