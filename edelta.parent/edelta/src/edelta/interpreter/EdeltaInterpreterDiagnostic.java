/**
 * 
 */
package edelta.interpreter;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.xtext.diagnostics.Severity;
import org.eclipse.xtext.validation.EObjectDiagnosticImpl;

/**
 * Represents an {@link EObjectDiagnosticImpl} generated during the
 * interpretation.
 * 
 * @author Lorenzo Bettini
 *
 */
public class EdeltaInterpreterDiagnostic extends EObjectDiagnosticImpl {

	/**
	 * @param severity
	 * @param problemCode
	 * @param message
	 * @param problematicObject
	 * @param problematicFeature
	 * @param indexOfProblematicValueInFeature
	 * @param data
	 */
	public EdeltaInterpreterDiagnostic(Severity severity, String problemCode, String message, EObject problematicObject,
			EStructuralFeature problematicFeature, int indexOfProblematicValueInFeature, String[] data) {
		super(severity, problemCode, message, problematicObject, problematicFeature, indexOfProblematicValueInFeature,
				data);
	}

}
