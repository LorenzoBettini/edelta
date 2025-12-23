/**
 * 
 */
package edelta.ui.navigation;

import static org.eclipse.xtext.nodemodel.util.NodeModelUtils.findActualSemanticObjectFor;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.nodemodel.INode;
import org.eclipse.xtext.resource.XtextResource;
import org.eclipse.xtext.ui.editor.hyperlinking.IHyperlinkAcceptor;
import org.eclipse.xtext.xbase.XExpression;
import org.eclipse.xtext.xbase.ui.navigation.XbaseHyperLinkHelper;

import com.google.inject.Inject;

import edelta.edelta.EdeltaEcoreArgument;
import edelta.navigation.EdeltaNavigationTargetHelper;

/**
 * Customizations for {@link EdeltaEcoreArgument}: jump to the original element
 * (in an ecore file) or to the {@link XExpression} that created the element.
 * 
 * @author Lorenzo Bettini
 *
 */
public class EdeltaHyperlinkinHelper extends XbaseHyperLinkHelper {

	@Inject
	private EdeltaNavigationTargetHelper navigationTargetHelper;

	@Override
	protected void createHyperlinksTo(XtextResource resource, INode node, EObject target, IHyperlinkAcceptor acceptor) {
		var semanticObj = findActualSemanticObjectFor(node);
		final var effectiveTarget = navigationTargetHelper.getTarget(semanticObj);
		if (effectiveTarget != null) {
			super.createHyperlinksTo(resource, node, effectiveTarget, acceptor);
		} else {
			super.createHyperlinksTo(resource, node, target, acceptor);
		}
	}
}
