package edelta.interpreter;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.ENamedElement;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource.Diagnostic;
import org.eclipse.xtext.diagnostics.Severity;
import org.eclipse.xtext.validation.EObjectDiagnosticImpl;
import org.eclipse.xtext.xbase.XExpression;

import edelta.lib.AbstractEdelta;
import edelta.lib.EdeltaEPackageManager;
import edelta.resource.derivedstate.EdeltaDerivedStateHelper;
import edelta.validation.EdeltaValidator;

/**
 * Used by the {@link EdeltaInterpreter} to return {@link EPackage} instances.
 * 
 * @author Lorenzo Bettini
 *
 */
public class EdeltaInterpreterEdeltaImpl extends AbstractEdelta {

	private XExpression currentExpression;
	private EdeltaDerivedStateHelper derivedStateHelper;

	public EdeltaInterpreterEdeltaImpl(List<EPackage> ePackages,
			EdeltaDerivedStateHelper derivedStateHelper) {
		super(new EdeltaEPackageManager() {
			private Map<String, EPackage> packageMap = ePackages.stream().collect(
					Collectors.toMap(
							EPackage::getName,
							Function.identity(),
							(existingValue, newValue) -> existingValue));

			@Override
			public EPackage getEPackage(String packageName) {
				return packageMap.get(packageName);
			}
		});
		this.derivedStateHelper = derivedStateHelper;
	}

	public void setCurrentExpression(XExpression currentExpression) {
		this.currentExpression = currentExpression;
	}

	@Override
	public void showError(ENamedElement problematicObject, String message) {
		if (currentExpression == null)
			super.showError(problematicObject, message);
		else {
			addDiagnostic(problematicObject, message, Severity.ERROR);
		}
	}

	@Override
	public void showWarning(ENamedElement problematicObject, String message) {
		if (currentExpression == null)
			super.showWarning(problematicObject, message);
		else {
			addDiagnostic(problematicObject, message, Severity.WARNING);
		}
	}

	private void addDiagnostic(ENamedElement problematicObject, String message, Severity severity) {
		XExpression correspondingExpression = derivedStateHelper
			.getEnamedElementXExpressionMap(currentExpression.eResource())
			.get(problematicObject);
		final EList<Diagnostic> issues = 
			severity == Severity.WARNING ?
				currentExpression.eResource().getWarnings() :
				currentExpression.eResource().getErrors();
		issues.add(
			new EObjectDiagnosticImpl(severity,
				severity == Severity.WARNING ?
					EdeltaValidator.LIVE_VALIDATION_WARNING :
					EdeltaValidator.LIVE_VALIDATION_ERROR,
				message,
				correspondingExpression != null ?
						correspondingExpression : currentExpression,
				null,
				-1,
				new String[] {}));
	}
}
