package edelta.resource;

import org.eclipse.emf.ecore.ENamedElement;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.nodemodel.INode;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;
import org.eclipse.xtext.xbase.XExpression;
import org.eclipse.xtext.xbase.linking.BrokenConstructorCallAwareEObjectAtOffsetHelper;

import com.google.inject.Inject;

import edelta.edelta.EdeltaEcoreReference;
import edelta.edelta.EdeltaEcoreReferenceExpression;
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
			final EdeltaEcoreReference ecoreReference = (EdeltaEcoreReference) referenceOwner;
			final ENamedElement original = edeltaDerivedStateHelper
					.getEcoreReferenceState(ecoreReference)
					.getOriginalEnamedelement();
			if (original != null)
				return original;
			XExpression expression = edeltaDerivedStateHelper
				.getEcoreReferenceExpressionState(
						EcoreUtil2.getContainerOfType(ecoreReference, EdeltaEcoreReferenceExpression.class))
				.getEnamedElementXExpressionMap()
				.get(ecoreReference.getEnamedelement());
			if (expression != null)
				return expression;
		}
		return super.resolveCrossReferencedElement(node);
	}
}
