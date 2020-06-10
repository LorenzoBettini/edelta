package edelta.resource.derivedstate;

import static org.eclipse.xtext.EcoreUtil2.getContainerOfType;

import java.util.Objects;

import org.eclipse.emf.ecore.ENamedElement;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.xtext.Constants;
import org.eclipse.xtext.resource.XtextResource;
import org.eclipse.xtext.xbase.XExpression;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import edelta.edelta.EdeltaEcoreReference;
import edelta.edelta.EdeltaEcoreReferenceExpression;

/**
 * Provides access (and possibly install) to the {@link EdeltaDerivedState}.
 * 
 * @author Lorenzo Bettini
 *
 */
@Singleton
public class EdeltaDerivedStateHelper {

	@Inject
	@Named(Constants.LANGUAGE_NAME)
	private String languageName;

	public EdeltaDerivedState getOrInstallAdapter(final Resource resource) {
		if (resource instanceof XtextResource) {
			final String resourceLanguageName = ((XtextResource) resource).getLanguageName();
			if (Objects.equals(resourceLanguageName, this.languageName)) {
				EdeltaDerivedState adapter = 
					(EdeltaDerivedState) EcoreUtil.getAdapter
						(resource.eAdapters(), EdeltaDerivedState.class);
				if (adapter == null) {
					adapter = new EdeltaDerivedState();
					resource.eAdapters().add(adapter);
				}
				return adapter;
			}
		}
		return new EdeltaDerivedState();
	}

	public EdeltaCopiedEPackagesMap getCopiedEPackagesMap(final Resource resource) {
		return getOrInstallAdapter(resource).getCopiedEPackagesMap();
	}

	public EdeltaEcoreReferenceState getEcoreReferenceState(EdeltaEcoreReference edeltaEcoreReference) {
		return getOrInstallAdapter(edeltaEcoreReference.eResource())
				.getEcoreReferenceStateMap()
				.computeIfAbsent(edeltaEcoreReference,
						e -> new EdeltaEcoreReferenceState());
	}

	public EdeltaEcoreReferenceExpressionState getEcoreReferenceExpressionState(
			EdeltaEcoreReferenceExpression edeltaEcoreReferenceExpression) {
		return getOrInstallAdapter(edeltaEcoreReferenceExpression.eResource())
				.getEcoreReferenceExpressionStateMap()
				.computeIfAbsent(edeltaEcoreReferenceExpression,
						e -> new EdeltaEcoreReferenceExpressionState());
	}

	public EdeltaENamedElementXExpressionMap getEnamedElementXExpressionMap(Resource resource) {
		return getOrInstallAdapter(resource).getEnamedElementXExpressionMap();
	}

	public ENamedElement getOriginalEnamedelement(EdeltaEcoreReference ecoreReference) {
		return getEcoreReferenceState(ecoreReference)
				.getOriginalEnamedelement();
	}

	public XExpression getResponsibleExpression(EdeltaEcoreReference ecoreReference) {
		return getEcoreReferenceExpressionState(
					getContainerOfType(ecoreReference,
						EdeltaEcoreReferenceExpression.class))
				.getEnamedElementXExpressionMap()
				.get(ecoreReference.getEnamedelement());
	}

	public EdeltaUnresolvedEcoreReferences getUnresolvedEcoreReferences(final Resource resource) {
		return getOrInstallAdapter(resource).getUnresolvedEcoreReferences();
	}

}
