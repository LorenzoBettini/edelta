package edelta.resource.derivedstate;

import static org.eclipse.xtext.EcoreUtil2.getContainerOfType;

import java.util.Objects;

import org.eclipse.emf.ecore.ENamedElement;
import org.eclipse.emf.ecore.EPackage;
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

	/**
	 * Returns the original {@link ENamedElement} referred by the given
	 * {@link EdeltaEcoreReference}, that is, the one in the original imported
	 * {@link EPackage}, if the element is not one created in this program.
	 * 
	 * @param ecoreReference
	 * @return
	 */
	public ENamedElement getOriginalEnamedelement(EdeltaEcoreReference ecoreReference) {
		return getEcoreReferenceState(ecoreReference)
				.getOriginalEnamedelement();
	}

	/**
	 * Returns the {@link XExpression} that created/changed the name of the
	 * {@link ENamedElement} of the passed {@link EdeltaEcoreReference}, keeping
	 * into consideration the current location of the 'ecoreref' expression
	 * ({@link EdeltaEcoreReferenceExpression}).
	 * 
	 * For example, given
	 * 
	 * <pre>
	 * modifyEcore aTest epackage foo {
	 * 	addNewEClass("NewClass")
	 * 	ecoreref(NewClass).name = "Renamed"
	 * 	ecoreref(Renamed)
	 * }
	 * </pre>
	 * 
	 * For <tt>ecoreref(NewClass)</tt> returns <tt>addNewEClass("NewClass")</tt>,
	 * for <tt>ecoreref(Renamed)</tt> returns
	 * <tt>ecoreref(NewClass).name = "Renamed"</tt> although, in the end, the
	 * {@link ENamedElement} is always the same.
	 * 
	 * @param ecoreReference
	 * @return
	 */
	public XExpression getResponsibleExpression(EdeltaEcoreReference ecoreReference) {
		return getEcoreReferenceExpressionState(
					getContainerOfType(ecoreReference,
						EdeltaEcoreReferenceExpression.class))
				.getEnamedElementXExpressionMap()
				.get(ecoreReference.getEnamedelement());
	}

	/**
	 * Returns the last {@link XExpression} that created/changed the name of the
	 * {@link ENamedElement}, assuming the element is part of the resource of the
	 * current program, that is, it has been created/modified in the program.
	 * 
	 * For example, given
	 * 
	 * <pre>
	 * modifyEcore aTest epackage foo {
	 * 	addNewEClass("NewClass")
	 * 	ecoreref(NewClass).name = "Renamed"
	 * 	ecoreref(Renamed)
	 * }
	 * </pre>
	 * 
	 * For <tt>NewClass</tt> and <tt>Renamed</tt> returns
	 * <tt>ecoreref(NewClass).name = "Renamed"</tt>.
	 * 
	 * @param enamedElement
	 * @return
	 */
	public XExpression getLastResponsibleExpression(ENamedElement enamedElement) {
		return getEnamedElementXExpressionMap(enamedElement.eResource())
				.get(enamedElement);
	}

	public EdeltaUnresolvedEcoreReferences getUnresolvedEcoreReferences(final Resource resource) {
		return getOrInstallAdapter(resource).getUnresolvedEcoreReferences();
	}

}
