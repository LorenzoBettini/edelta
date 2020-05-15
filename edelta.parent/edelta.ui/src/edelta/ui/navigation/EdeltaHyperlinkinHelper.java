/**
 * 
 */
package edelta.ui.navigation;

import org.eclipse.emf.ecore.ENamedElement;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.nodemodel.INode;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;
import org.eclipse.xtext.resource.XtextResource;
import org.eclipse.xtext.ui.editor.hyperlinking.IHyperlinkAcceptor;
import org.eclipse.xtext.xbase.XExpression;
import org.eclipse.xtext.xbase.ui.navigation.XbaseHyperLinkHelper;

import com.google.inject.Inject;

import edelta.edelta.EdeltaEcoreReference;
import edelta.edelta.EdeltaEcoreReferenceExpression;
import edelta.resource.derivedstate.EdeltaDerivedStateHelper;

/**
 * Customizations for {@link EdeltaEcoreReference}: jump to the original element
 * (in an ecore file) or to the {@link XExpression} that created the element.
 * 
 * @author Lorenzo Bettini
 *
 */
public class EdeltaHyperlinkinHelper extends XbaseHyperLinkHelper {

	@Inject
	private EdeltaDerivedStateHelper edeltaDerivedStateHelper;

	@Override
	protected void createHyperlinksTo(XtextResource resource, INode node, EObject target, IHyperlinkAcceptor acceptor) {
		EObject semanticObj = NodeModelUtils.findActualSemanticObjectFor(node);
		if (semanticObj instanceof EdeltaEcoreReference) {
			final EdeltaEcoreReference ecoreReference = (EdeltaEcoreReference) semanticObj;
			final ENamedElement original = edeltaDerivedStateHelper
					.getEcoreReferenceState(ecoreReference)
					.getOriginalEnamedelement();
			if (original != null) {
				super.createHyperlinksTo(resource, node, original, acceptor);
				return;
			}
			XExpression expression = edeltaDerivedStateHelper
				.getEcoreReferenceExpressionState(
						EcoreUtil2.getContainerOfType(ecoreReference, EdeltaEcoreReferenceExpression.class))
				.getEnamedElementXExpressionMap()
				.get(ecoreReference.getEnamedelement());
			super.createHyperlinksTo(resource, node, expression, acceptor);
			return;
		}
		super.createHyperlinksTo(resource, node, target, acceptor);
	}
}
