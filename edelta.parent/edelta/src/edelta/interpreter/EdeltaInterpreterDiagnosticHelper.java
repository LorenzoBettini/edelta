package edelta.interpreter;

import java.util.List;

import org.eclipse.emf.ecore.ENamedElement;
import org.eclipse.emf.ecore.resource.Resource.Diagnostic;
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

	public void addError(ENamedElement problematicObject, String problemCode, String message) {
		addDiagnostic(problematicObject, problemCode, message, Severity.ERROR);
	}

	public void addWarning(ENamedElement problematicObject, String problemCode, String message) {
		addDiagnostic(problematicObject, problemCode, message, Severity.WARNING);
	}

	private void addDiagnostic(ENamedElement problematicObject, String problemCode, String message, Severity severity) {
		XExpression correspondingExpression = derivedStateHelper
			.getEnamedElementXExpressionMap(currentExpression.eResource())
			.get(problematicObject);
		final List<Diagnostic> issues = 
			severity == Severity.WARNING ?
				currentExpression.eResource().getWarnings() :
				currentExpression.eResource().getErrors();
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
