package edelta.tests.additional;

import org.eclipse.emf.ecore.EClass;

import edelta.lib.EdeltaRuntime;
import edelta.tests.input.Inputs;

/**
 * This is used in {@link Inputs} for "use ... as ..." expressions and calls
 * {@link #showError(org.eclipse.emf.ecore.ENamedElement, String)}.
 * 
 * @author Lorenzo Bettini
 *
 */
public class MyCustomEdeltaShowingError extends EdeltaRuntime {

	public MyCustomEdeltaShowingError() {
	}

	public MyCustomEdeltaShowingError(EdeltaRuntime other) {
		super(other);
	}

	public void checkClassName(EClass c) {
		if (Character.isLowerCase(c.getName().charAt(0))) {
			showError(c, "Name should start with a capital: " + c.getName());
		}
	}
}
