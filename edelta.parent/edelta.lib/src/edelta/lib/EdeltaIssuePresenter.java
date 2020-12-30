package edelta.lib;

import org.eclipse.emf.ecore.ENamedElement;

/**
 * Responsible of showing issues (errors and warnings).
 * 
 * @author Lorenzo Bettini
 *
 */
public interface EdeltaIssuePresenter {

	void showError(ENamedElement problematicObject, String message);

	void showWarning(ENamedElement problematicObject, String message);

}
