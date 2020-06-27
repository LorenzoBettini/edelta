/**
 * 
 */
package edelta.validation;

import org.eclipse.emf.common.util.Diagnostic;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.util.IAcceptor;
import org.eclipse.xtext.validation.DiagnosticConverterImpl;
import org.eclipse.xtext.validation.Issue;

import edelta.edelta.EdeltaEcoreReference;

/**
 * Customization to avoid error messages from EMF about dangling references
 * that are due to interpreting operations (these might remove references
 * in the Edelta program, but we only simulate the refactoring so we don't want
 * errors about dangling references).
 * 
 * @author Lorenzo Bettini
 *
 */
public class EdeltaDiagnosticConverter extends DiagnosticConverterImpl {

	@Override
	public void convertValidatorDiagnostic(Diagnostic diagnostic, IAcceptor<Issue> acceptor) {
		EObject causer = getCauser(diagnostic);
		if (causer instanceof EdeltaEcoreReference &&
				!EdeltaValidator.INTERPRETER_ACCESS_NOT_YET_EXISTING_ELEMENT
					.equals(getIssueCode(diagnostic))) {
			return;
		}
		super.convertValidatorDiagnostic(diagnostic, acceptor);
	}
}
