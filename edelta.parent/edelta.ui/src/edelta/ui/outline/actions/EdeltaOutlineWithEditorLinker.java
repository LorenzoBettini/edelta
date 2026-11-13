/**
 * 
 */
package edelta.ui.outline.actions;

import static edelta.edelta.EdeltaPackage.Literals.EDELTA_MODIFY_ECORE_OPERATION;
import static org.eclipse.emf.ecore.EcorePackage.Literals.EPACKAGE;

import java.util.Objects;

import org.eclipse.emf.ecore.ENamedElement;
import org.eclipse.xtext.ui.editor.outline.IOutlineNode;
import org.eclipse.xtext.ui.editor.outline.actions.OutlineWithEditorLinker;
import org.eclipse.xtext.ui.editor.outline.impl.EObjectNode;
import org.eclipse.xtext.util.ITextRegion;
import org.eclipse.xtext.xbase.XExpression;

import edelta.ui.outline.EdeltaOutlineTreeProvider.EcoreOutlineNode;
import edelta.ui.outline.EdeltaOutlineTreeProvider.EcorePropertyOutlineNode;

/**
 * From an {@link XExpression} responsible of the modification of an {@link ENamedElement}
 * in the editor, navigate to the corresponding element in the outline.
 * 
 * @author Lorenzo Bettini
 *
 */
public class EdeltaOutlineWithEditorLinker extends OutlineWithEditorLinker {

	@Override
	protected IOutlineNode findBestNode(IOutlineNode input, ITextRegion selectedTextRegion) {
		final var findBestNode = super.findBestNode(input, selectedTextRegion);
		if (findBestNode instanceof EObjectNode eObjectNode && eObjectNode.getEClass() == EDELTA_MODIFY_ECORE_OPERATION) {
			/* since XExpressions are not shown in the outline, when we select
			 * such an expression, by default the containing modifyEcore node is
			 * selected. We try and find a node representing an Ecore element
			 * that is modified by the selected XExpression */
			var ePackages = eObjectNode.getParent().getChildren().stream()
				.filter(node -> EPACKAGE == ((EObjectNode) node).getEClass())
				.toList();
			var property = ePackages.stream()
				.map(node -> findPropertyNode(node, selectedTextRegion))
				.filter(Objects::nonNull)
				.findFirst()
				.orElse(null);
			if (property != null) {
				return property;
			}
			return ePackages.stream()
				.map(node -> findENamedElementNode(node, selectedTextRegion))
				.filter(Objects::nonNull)
				.findFirst()
				.orElse(findBestNode);
		}
		return findBestNode;
	}

	private IOutlineNode findPropertyNode(IOutlineNode node, ITextRegion selectedTextRegion) {
		for (var child : node.getChildren()) {
			if (child instanceof EcorePropertyOutlineNode property && property.isDirectChange() &&
					contains(property.getSignificantTextRegion(), selectedTextRegion)) {
				return property;
			}
			if (child instanceof EcoreOutlineNode) {
				var match = findPropertyNode(child, selectedTextRegion);
				if (match != null) {
					return match;
				}
			}
		}
		return null;
	}

	private IOutlineNode findENamedElementNode(IOutlineNode node, ITextRegion selectedTextRegion) {
		for (var child : node.getChildren()) {
			if (!(child instanceof EcoreOutlineNode ecoreNode)) {
				continue;
			}
			// our Outline nodes for Ecore elements are already associated with the
			// text region of the corresponding responsible XExpression
			final var recursiveFind = findENamedElementNode(child, selectedTextRegion);
			// first check whether there's a child that matches, since,
			// as said before, an outer expression region might match an inner one
			if (recursiveFind != null) {
				return recursiveFind;
			} else if (contains(ecoreNode.getSignificantTextRegion(), selectedTextRegion)) {
				return ecoreNode;
			}
		}
		return null;
	}

	private boolean contains(ITextRegion region, ITextRegion selection) {
		return region != null && region.contains(selection);
	}
}
