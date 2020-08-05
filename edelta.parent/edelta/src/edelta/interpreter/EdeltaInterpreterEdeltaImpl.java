package edelta.interpreter;

import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.eclipse.emf.ecore.ENamedElement;
import org.eclipse.emf.ecore.EPackage;

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

	private EdeltaInterpreterDiagnosticHelper diagnosticHelper;

	/**
	 * Uses the passed {@link EPackage}s to create an {@link EdeltaEPackageManager};
	 * if an {@link EPackage} appears several times in the list, only the first
	 * occurrence will be taken into consideration.
	 * 
	 * @param copiedEPackages
	 * @param diagnosticHelper
	 */
	public EdeltaInterpreterEdeltaImpl(Collection<EPackage> copiedEPackages,
			EdeltaInterpreterDiagnosticHelper diagnosticHelper) {
		super(new EdeltaEPackageManager() {
			private Map<String, EPackage> packageMap = copiedEPackages.stream().collect(
					Collectors.toMap(
							EPackage::getName,
							Function.identity(),
							(existingValue, newValue) -> existingValue));

			@Override
			public EPackage getEPackage(String packageName) {
				return packageMap.get(packageName);
			}
		});
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
