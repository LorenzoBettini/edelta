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

	@Inject extension EdeltaDerivedStateComputer

	override protected convertToSource(EObject element) {
		if (element === null)
			return null
		return element.getPrimarySourceElement ?:
			super.convertToSource(element)
	}

}
