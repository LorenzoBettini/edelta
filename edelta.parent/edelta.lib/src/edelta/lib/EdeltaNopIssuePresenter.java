package edelta.lib;

import org.eclipse.emf.ecore.ENamedElement;

/**
 * Since the {@link EdeltaIssuePresenter} must not be null, this is
 * a nop presenter that is used by default.
 * 
 * @author Lorenzo Bettini
 *
 */
public final class EdeltaNopIssuePresenter implements EdeltaIssuePresenter {
	public static final EdeltaIssuePresenter INSTANCE = new EdeltaNopIssuePresenter();

	private EdeltaNopIssuePresenter() {
		// just use the singleton INSTANCE
	}

	@Override
	public void showWarning(ENamedElement problematicObject, String message) {
		// nop
	}

	@Override
	public void showError(ENamedElement problematicObject, String message) {
		// nop
	}
}