package edelta.scoping;

import org.eclipse.emf.ecore.ENamedElement;

import com.google.inject.Inject;

import edelta.edelta.EdeltaEcoreQualifiedReference;
import edelta.edelta.EdeltaEcoreReference;
import edelta.resource.derivedstate.EdeltaDerivedStateHelper;

/**
 * Records the original referred {@link ENamedElement} in an
 * {@link EdeltaEcoreReference} expression, that is elements in the original
 * imported metamodels.
 * 
 * @author Lorenzo Bettini
 */
public class EdeltaOriginalENamedElementRecorder {
	@Inject
	private EdeltaDerivedStateHelper derivedStateHelper;

	public void recordOriginalENamedElement(final EdeltaEcoreReference edeltaEcoreReference) {
		if (edeltaEcoreReference == null) {
			return;
		}
		final var enamedElement = edeltaEcoreReference.getEnamedelement();
		var copiedEPackagesMap =
			derivedStateHelper.getCopiedEPackagesMap(edeltaEcoreReference.eResource());
		var original = copiedEPackagesMap.getOriginal(enamedElement);
		derivedStateHelper
			.getEcoreReferenceState(edeltaEcoreReference)
			.setOriginalEnamedelement
				(original);
		if (edeltaEcoreReference instanceof EdeltaEcoreQualifiedReference ecoreQualifiedReference) {
			recordOriginalENamedElement(ecoreQualifiedReference.getQualification());
		}
	}

}
