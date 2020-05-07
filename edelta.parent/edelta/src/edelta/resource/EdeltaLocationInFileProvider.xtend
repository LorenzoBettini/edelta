package edelta.resource

import com.google.inject.Inject
import com.google.inject.Singleton
import org.eclipse.emf.ecore.EObject
import org.eclipse.xtext.xbase.jvmmodel.IJvmModelAssociations
import org.eclipse.xtext.xbase.jvmmodel.JvmLocationInFileProvider

/**
 * Customization for locating Edelta Ecore operations corresponding
 * to references to Ecore elements.
 * 
 * @author Lorenzo Bettini
 */
@Singleton
class EdeltaLocationInFileProvider extends JvmLocationInFileProvider {

	@Inject extension IJvmModelAssociations

	override protected convertToSource(EObject element) {
		return element.getPrimarySourceElement ?:
			super.convertToSource(element)
	}

}
