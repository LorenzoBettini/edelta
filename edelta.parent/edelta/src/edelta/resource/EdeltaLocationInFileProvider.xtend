package edelta.resource

import org.eclipse.xtext.xbase.jvmmodel.JvmLocationInFileProvider
import org.eclipse.emf.ecore.EObject
import com.google.inject.Inject
import com.google.inject.Singleton

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
