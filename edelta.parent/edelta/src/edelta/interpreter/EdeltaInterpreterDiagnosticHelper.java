package edelta.interpreter;

import org.eclipse.emf.ecore.ENamedElement;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.diagnostics.Severity;
import org.eclipse.xtext.validation.EObjectDiagnosticImpl;
import org.eclipse.xtext.xbase.XExpression;

import com.google.inject.Inject;

import edelta.resource.derivedstate.EdeltaDerivedStateHelper;

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

	public void setCurrentExpression(XExpression currentExpression) {
		this.currentExpression = currentExpression;
	}

	public void addError(EObject problematicObject, String problemCode, String message) {
		addDiagnostic(problematicObject, problemCode, message, Severity.ERROR);
	}

	public void addWarning(EObject problematicObject, String problemCode, String message) {
		addDiagnostic(problematicObject, problemCode, message, Severity.WARNING);
	}

	private void addDiagnostic(EObject problematicObject, String problemCode, String message, Severity severity) {
		XExpression correspondingExpression = null;
		final var eResource = currentExpression.eResource();
		if (problematicObject instanceof ENamedElement namedElement) {
			correspondingExpression = derivedStateHelper
				.getLastResponsibleExpression(currentExpression, namedElement);
		}
		final var issues = 
			severity == Severity.WARNING ? eResource.getWarnings() : eResource.getErrors();
		issues.add(
			new EObjectDiagnosticImpl(severity,
				problemCode,
				message,
				correspondingExpression != null ?
						correspondingExpression : currentExpression,
				null,
				-1,
				new String[] {}));
	}
}
