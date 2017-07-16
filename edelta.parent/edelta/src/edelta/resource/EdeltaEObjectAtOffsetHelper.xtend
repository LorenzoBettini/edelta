package edelta.resource

import edelta.edelta.EdeltaEcoreReference
import org.eclipse.xtext.nodemodel.INode
import org.eclipse.xtext.nodemodel.util.NodeModelUtils
import org.eclipse.xtext.xbase.linking.BrokenConstructorCallAwareEObjectAtOffsetHelper

/**
 * Customization for ecoreref references, using the original enamed element.
 * 
 * @author Lorenzo Bettini
 */
class EdeltaEObjectAtOffsetHelper extends BrokenConstructorCallAwareEObjectAtOffsetHelper {

	override protected resolveCrossReferencedElement(INode node) {
		val referenceOwner = NodeModelUtils.findActualSemanticObjectFor(node);
		if (referenceOwner instanceof EdeltaEcoreReference) {
			val original = referenceOwner.originalEnamedelement
			if (original !== null)
				return original
		}
		super.resolveCrossReferencedElement(node)
	}

}
