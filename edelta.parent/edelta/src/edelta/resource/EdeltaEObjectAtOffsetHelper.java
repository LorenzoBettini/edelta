package edelta.resource;

import org.eclipse.emf.ecore.ENamedElement;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.nodemodel.INode;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;
import org.eclipse.xtext.xbase.linking.BrokenConstructorCallAwareEObjectAtOffsetHelper;

import com.google.inject.Inject;

import edelta.edelta.EdeltaEcoreReference;
import edelta.resource.derivedstate.EdeltaDerivedStateHelper;

/**
 * Customization for ecoreref references, using the original enamed element.
 * 
 * @author Lorenzo Bettini
 */
public class EdeltaEObjectAtOffsetHelper extends BrokenConstructorCallAwareEObjectAtOffsetHelper {
	@Inject
	private EdeltaDerivedStateHelper edeltaDerivedStateHelper;

	@Override
	protected EObject resolveCrossReferencedElement(final INode node) {
		final EObject referenceOwner = NodeModelUtils.findActualSemanticObjectFor(node);
		if (referenceOwner instanceof EdeltaEcoreReference) {
			final ENamedElement original = this.edeltaDerivedStateHelper
					.getEcoreReferenceState((EdeltaEcoreReference) referenceOwner)
					.getOriginalEnamedelement();
			if (original != null) {
				return original;
			}
		}
		return super.resolveCrossReferencedElement(node);
	}
}
