package edelta.resource

import org.eclipse.xtext.xbase.jvmmodel.JvmLocationInFileProvider
import org.eclipse.emf.ecore.EObject

/**
 * Customization for locating Edelta Ecore operations corresponding
 * to references to Ecore elements.
 * 
 * @author Lorenzo Bettini
 */
class EdeltaLocationInFileProvider extends JvmLocationInFileProvider {

	override protected convertToSource(EObject element) {
		super.convertToSource(element)
	}

}
