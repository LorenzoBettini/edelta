package edelta.resource;

import org.eclipse.emf.ecore.ENamedElement;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.xbase.XExpression;
import org.eclipse.xtext.xbase.jvmmodel.JvmLocationInFileProvider;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import edelta.resource.derivedstate.EdeltaDerivedStateHelper;
import edelta.resource.derivedstate.EdeltaENamedElementXExpressionMap;

/**
 * Customization for locating elements corresponding to references to Ecore
 * elements.
 * 
 * @author Lorenzo Bettini
 */
@Singleton
public class EdeltaLocationInFileProvider extends JvmLocationInFileProvider {
	@Inject
	private EdeltaDerivedStateHelper edeltaDerivedStateHelper;

	@Override
	protected EObject convertToSource(final EObject element) {
		if (element instanceof ENamedElement) {
			final ENamedElement enamedElement = (ENamedElement) element;
			final EdeltaENamedElementXExpressionMap enamedElementXExpressionMap =
				edeltaDerivedStateHelper.getEnamedElementXExpressionMap(enamedElement.eResource());
			XExpression expression = enamedElementXExpressionMap
				.get(enamedElement);
			if (expression != null)
				return super.convertToSource(expression);
		}
		return super.convertToSource(element);
	}
}
