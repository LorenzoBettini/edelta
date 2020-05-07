package edelta.util;

import org.eclipse.emf.ecore.ENamedElement;

import edelta.edelta.EdeltaEcoreReference;

/**
 * Additional information on {@link EdeltaEcoreReference}
 * 
 * @author Lorenzo Bettini
 *
 */
public class EdeltaEcoreReferenceState {

	private ENamedElement originalEnamedelement;

	public ENamedElement getOriginalEnamedelement() {
		return originalEnamedelement;
	}

	public void setOriginalEnamedelement(ENamedElement originalEnamedelement) {
		this.originalEnamedelement = originalEnamedelement;
	}
}
