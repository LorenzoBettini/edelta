package edelta.navigation;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.xbase.XExpression;

import com.google.inject.Inject;

import edelta.edelta.EdeltaEcoreReference;
import edelta.resource.derivedstate.EdeltaDerivedStateHelper;

/**
 * Navigation helper for navigating to the target of an
 * {@link EdeltaEcoreReference}, for example, to the original element, or to the
 * {@link XExpression} that created/renamed it (possibly handling forward
 * references, which are errors).
 * 
 * @author Lorenzo Bettini
 *
 */
public class EdeltaNavigationTargetHelper {

	@Inject
	private EdeltaDerivedStateHelper derivedStateHelper;

	public EObject getTarget(EObject obj) {
		if (obj instanceof EdeltaEcoreReference) {
			final var ecoreReference = (EdeltaEcoreReference) obj;
			final var original = derivedStateHelper
				.getOriginalEnamedelement(ecoreReference);
			if (original != null) {
				return original;
			}
			final var exp = derivedStateHelper
				.getResponsibleExpression(ecoreReference);
			if (exp != null) {
				return exp;
			}
			// last resort, in case of a forward reference
			return derivedStateHelper
				.getLastResponsibleExpression(ecoreReference.getEnamedelement());
		}
		return null;
	}
}
