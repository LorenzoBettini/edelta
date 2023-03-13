package edelta.interpreter;

import org.eclipse.emf.ecore.ENamedElement;

import edelta.lib.EdeltaRuntime;
import edelta.lib.EdeltaIssuePresenter;
import edelta.validation.EdeltaValidator;

/**
 * Used by the {@link EdeltaInterpreter} to catch issues reported by
 * {@link EdeltaRuntime#showError(ENamedElement, String)},
 * {@link EdeltaRuntime#showWarning(ENamedElement, String)}, etc, using
 * {@link EdeltaInterpreterDiagnosticHelper}.
 * 
 * @author Lorenzo Bettini
 *
 */
public class EdeltaInterpreterIssuePresenter implements EdeltaIssuePresenter {

	private EdeltaInterpreterDiagnosticHelper diagnosticHelper;

	public EdeltaInterpreterIssuePresenter(EdeltaInterpreterDiagnosticHelper diagnosticHelper) {
		this.diagnosticHelper = diagnosticHelper;
	}

	@Override
	public void showError(ENamedElement problematicObject, String message) {
		diagnosticHelper
			.addError(problematicObject, EdeltaValidator.LIVE_VALIDATION_ERROR, message);
	}

	@Override
	public void showWarning(ENamedElement problematicObject, String message) {
		diagnosticHelper
			.addWarning(problematicObject, EdeltaValidator.LIVE_VALIDATION_WARNING, message);
	}

}
