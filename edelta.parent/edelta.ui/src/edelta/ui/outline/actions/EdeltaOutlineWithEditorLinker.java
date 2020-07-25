/**
 * 
 */
package edelta.ui.outline.actions;

import org.eclipse.emf.ecore.ENamedElement;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.xtext.ui.editor.outline.IOutlineNode;
import org.eclipse.xtext.ui.editor.outline.actions.OutlineWithEditorLinker;
import org.eclipse.xtext.ui.editor.outline.impl.EObjectNode;
import org.eclipse.xtext.util.ITextRegion;
import org.eclipse.xtext.xbase.XExpression;

import edelta.edelta.EdeltaPackage;

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
		if (findBestNode instanceof EObjectNode) {
			EObjectNode eObjectNode = (EObjectNode) findBestNode;
			if (eObjectNode.getEClass() == EdeltaPackage.Literals.EDELTA_MODIFY_ECORE_OPERATION) {
				final var findENamedElementNode = findENamedElementNode(eObjectNode.getParent(), selectedTextRegion);
				if (findENamedElementNode != null)
					return findENamedElementNode;
			}
		}
		return findBestNode;
	}

	private IOutlineNode findENamedElementNode(IOutlineNode node, ITextRegion selectedTextRegion) {
		for (var child : node.getChildren()) {
			// at this point we are sure it's an EObjectNode
			EObjectNode eObjectNode = (EObjectNode) child;
			if (EcorePackage.Literals.ENAMED_ELEMENT.isSuperTypeOf(eObjectNode.getEClass())) {
				if (eObjectNode.getSignificantTextRegion().contains(selectedTextRegion)) {
					return eObjectNode;
				}
				final var recursiveFind = findENamedElementNode(child, selectedTextRegion);
				if (recursiveFind != null)
					return recursiveFind;
			}
		}
		return null;
	}
}
