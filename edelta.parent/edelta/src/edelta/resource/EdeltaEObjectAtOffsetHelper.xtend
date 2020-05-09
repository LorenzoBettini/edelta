package edelta.resource

import com.google.inject.Inject
import edelta.edelta.EdeltaEcoreReference
import edelta.resource.derivedstate.EdeltaDerivedStateHelper
import org.eclipse.xtext.nodemodel.INode
import org.eclipse.xtext.nodemodel.util.NodeModelUtils
import org.eclipse.xtext.xbase.linking.BrokenConstructorCallAwareEObjectAtOffsetHelper

/**
 * Customization for ecoreref references, using the original enamed element.
 * 
 * @author Lorenzo Bettini
 */
class EdeltaEObjectAtOffsetHelper extends BrokenConstructorCallAwareEObjectAtOffsetHelper {

	@Inject extension EdeltaDerivedStateHelper

	override protected resolveCrossReferencedElement(INode node) {
		val referenceOwner = NodeModelUtils.findActualSemanticObjectFor(node);
		if (referenceOwner instanceof EdeltaEcoreReference) {
			val original = referenceOwner.ecoreReferenceState.originalEnamedelement
			if (original !== null)
				return original
		}
		super.resolveCrossReferencedElement(node)
	}

}
