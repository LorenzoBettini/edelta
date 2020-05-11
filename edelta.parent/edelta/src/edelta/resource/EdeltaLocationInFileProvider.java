package edelta.resource;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.xbase.jvmmodel.IJvmModelAssociations;
import org.eclipse.xtext.xbase.jvmmodel.JvmLocationInFileProvider;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * Customization for locating Edelta Ecore operations corresponding to
 * references to Ecore elements.
 * 
 * @author Lorenzo Bettini
 */
@Singleton
public class EdeltaLocationInFileProvider extends JvmLocationInFileProvider {
	@Inject
	private IJvmModelAssociations associations;

	@Override
	protected EObject convertToSource(final EObject element) {
		final EObject primarySourceElement = associations.getPrimarySourceElement(element);
		if (primarySourceElement != null) {
			return associations.getPrimarySourceElement(element);
		} else {
			return super.convertToSource(element);
		}
	}
}
