package edelta.resource

import com.google.inject.Inject
import com.google.inject.Singleton
import edelta.services.IEdeltaEcoreModelAssociations
import org.eclipse.emf.ecore.EObject
import org.eclipse.xtext.xbase.jvmmodel.JvmLocationInFileProvider

/**
 * Customization for locating Edelta Ecore operations corresponding
 * to references to Ecore elements.
 * 
 * @author Lorenzo Bettini
 */
@Singleton
class EdeltaLocationInFileProvider extends JvmLocationInFileProvider {

	@Inject extension IEdeltaEcoreModelAssociations

	override protected convertToSource(EObject element) {
		return element.getPrimarySourceElement ?:
			super.convertToSource(element)
	}

}
