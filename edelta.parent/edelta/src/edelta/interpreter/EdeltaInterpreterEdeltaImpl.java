package edelta.interpreter;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.xtext.diagnostics.Severity;
import org.eclipse.xtext.validation.EObjectDiagnosticImpl;
import org.eclipse.xtext.xbase.XExpression;

import edelta.lib.AbstractEdelta;
import edelta.lib.EdeltaEPackageManager;
import edelta.validation.EdeltaValidator;

/**
 * Used by the {@link EdeltaInterpreter} to return {@link EPackage} instances.
 * 
 * @author Lorenzo Bettini
 *
 */
public class EdeltaInterpreterEdeltaImpl extends AbstractEdelta {

	private XExpression currentExpression;

	public EdeltaInterpreterEdeltaImpl(List<EPackage> ePackages) {
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
	}

	public void setCurrentExpression(XExpression currentExpression) {
		this.currentExpression = currentExpression;
	}

	@Override
	public void showError(EObject problematicObject, String message) {
		if (currentExpression == null)
			super.showError(problematicObject, message);
		else {
			currentExpression.eResource().getErrors().add(
				new EObjectDiagnosticImpl(Severity.ERROR,
					EdeltaValidator.LIVE_VALIDATION_ERROR,
					message,
					currentExpression,
					null,
					-1,
					new String[] {}));
		}
	}

	@Override
	public void showWarning(EObject problematicObject, String message) {
		if (currentExpression == null)
			super.showWarning(problematicObject, message);
		else {
			currentExpression.eResource().getWarnings().add(
				new EObjectDiagnosticImpl(Severity.WARNING,
					EdeltaValidator.LIVE_VALIDATION_WARNING,
					message,
					currentExpression,
					null,
					-1,
					new String[] {}));
		}
	}
}
