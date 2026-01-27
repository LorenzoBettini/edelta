package edelta.scoping;

import org.eclipse.emf.ecore.ENamedElement;

import com.google.inject.Inject;

import edelta.edelta.EdeltaEcoreQualifiedArgument;
import edelta.edelta.EdeltaEcoreArgument;
import edelta.resource.derivedstate.EdeltaDerivedStateHelper;

/**
 * Records the original referred {@link ENamedElement} in an
 * {@link EdeltaEcoreArgument} expression, that is elements in the original
 * imported metamodels.
 * 
 * @author Lorenzo Bettini
 */
public class EdeltaOriginalENamedElementRecorder {
	@Inject
	private EdeltaDerivedStateHelper derivedStateHelper;

	public void recordOriginalENamedElement(final EdeltaEcoreArgument edeltaEcoreReference) {
		if (edeltaEcoreReference == null) {
			return;
		}
		final var enamedElement = edeltaEcoreReference.getElement();
		var copiedEPackagesMap =
			derivedStateHelper.getCopiedEPackagesMap(edeltaEcoreReference.eResource());
		var original = copiedEPackagesMap.getOriginal(enamedElement);
		derivedStateHelper
			.getEcoreReferenceState(edeltaEcoreReference)
			.setOriginalEnamedelement
				(original);
		if (edeltaEcoreReference instanceof EdeltaEcoreQualifiedArgument ecoreQualifiedReference) {
			recordOriginalENamedElement(ecoreQualifiedReference.getQualification());
		}
	}

}
