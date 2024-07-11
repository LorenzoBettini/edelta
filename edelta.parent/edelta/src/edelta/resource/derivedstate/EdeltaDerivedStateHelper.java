package edelta.resource.derivedstate;

import static org.eclipse.xtext.EcoreUtil2.getContainerOfType;

import java.util.Collection;
import java.util.Objects;

import org.eclipse.emf.ecore.ENamedElement;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.ecore.util.EcoreUtil.Copier;
import org.eclipse.xtext.Constants;
import org.eclipse.xtext.resource.XtextResource;
import org.eclipse.xtext.xbase.XExpression;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import edelta.edelta.EdeltaEcoreReference;
import edelta.edelta.EdeltaEcoreReferenceExpression;
import edelta.edelta.EdeltaProgram;
import edelta.util.EdeltaModelUtil;

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
		if (resource instanceof XtextResource xtextResource) {
			final var resourceLanguageName = xtextResource.getLanguageName();
			if (Objects.equals(resourceLanguageName, languageName)) {
				var adapter = 
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

	/**
	 * Copies all imported {@link EPackage}s of the passed {@link EdeltaProgram} in
	 * the {@link EdeltaCopiedEPackagesMap} in the derived state of the program's
	 * {@link Resource}; the copied {@link EPackage}s are also inserted in the
	 * resource.
	 * 
	 * @param program
	 */
	public void copyEPackages(EdeltaProgram program) {
		copyEPackages(program, program.eResource());
	}

	/**
	 * Copies all imported {@link EPackage}s of the passed {@link EdeltaProgram} in
	 * the {@link EdeltaCopiedEPackagesMap} in the derived state of the specified
	 * {@link Resource}; the copied {@link EPackage}s are also inserted in the
	 * specified resource.
	 * 
	 * @param program
	 * @param resource
	 */
	public void copyEPackages(EdeltaProgram program, final Resource resource) {
		final var packages = EdeltaModelUtil.getMetamodels(program).stream()
			.distinct()
			.toList();
		final var copiedEPackagesMap = getCopiedEPackagesMap(resource);
		Copier copier = new Copier();
		Collection<EPackage> copies = copier.copyAll(packages);
		copier.copyReferences();
		copiedEPackagesMap.setCopies(copies, copier);
		// we must add the copied EPackages to the resource
		addToProgramResource(resource, copiedEPackagesMap);
	}

	/**
	 * Adds the copied {@link EPackage}s to the {@link Resource} of the program
	 * 
	 * @param resource
	 * @param copiedEPackagesMap
	 */
	public void addToProgramResource(final Resource resource, final EdeltaCopiedEPackagesMap copiedEPackagesMap) {
		resource.getContents().addAll(copiedEPackagesMap.values());
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
	 * {@link ENamedElement}.
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
	 * @param programContext A part of the Edelta program, needed to retrieve
	 * the Resource where the derived state is stored.
	 * @param enamedElement
	 * @return
	 */
	public XExpression getLastResponsibleExpression(EObject programContext, ENamedElement enamedElement) {
		return getEnamedElementXExpressionMap(programContext.eResource())
				.get(enamedElement);
	}

	public EdeltaUnresolvedEcoreReferences getUnresolvedEcoreReferences(final Resource resource) {
		return getOrInstallAdapter(resource).getUnresolvedEcoreReferences();
	}

	public EdeltaAccessibleElements getAccessibleElements(EdeltaEcoreReferenceExpression ecoreRefExp) {
		return getEcoreReferenceExpressionState(ecoreRefExp)
				.getAccessibleElements();
	}

	public void setAccessibleElements(EdeltaEcoreReferenceExpression ecoreRefExp,
			EdeltaAccessibleElements accessibleElements) {
		getEcoreReferenceExpressionState(ecoreRefExp)
			.setAccessibleElements(accessibleElements);
	}

	public EdeltaModifiedElements getModifiedElements(Resource resource) {
		return getOrInstallAdapter(resource).getModifiedElements();
	}

}
