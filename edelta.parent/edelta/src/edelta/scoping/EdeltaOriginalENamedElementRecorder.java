package edelta.scoping;

import static edelta.util.EdeltaModelUtil.getProgram;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.ENamedElement;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.xtext.util.IResourceScopeCache;
import org.eclipse.xtext.xbase.lib.IterableExtensions;

import com.google.common.base.Objects;
import com.google.inject.Inject;

import edelta.edelta.EdeltaEcoreQualifiedReference;
import edelta.edelta.EdeltaEcoreReference;
import edelta.resource.derivedstate.EdeltaDerivedStateHelper;
import edelta.util.EdeltaEcoreHelper;

/**
 * Records the original referred {@link ENamedElement} in an
 * {@link EdeltaEcoreReference} expression, that is elements in the original
 * imported metamodels.
 * 
 * @author Lorenzo Bettini
 */
public class EdeltaOriginalENamedElementRecorder {
	@Inject
	private EdeltaEcoreHelper ecoreHelper;

	@Inject
	private EdeltaDerivedStateHelper derivedStateHelper;

	@Inject
	private IResourceScopeCache cache;

	public void recordOriginalENamedElement(final EdeltaEcoreReference edeltaEcoreReference) {
		if (edeltaEcoreReference == null) {
			return;
		}
		final var enamedElement = edeltaEcoreReference.getEnamedelement();
		this.derivedStateHelper
			.getEcoreReferenceState(edeltaEcoreReference)
			.setOriginalEnamedelement
				(retrieveOriginalElement(enamedElement, edeltaEcoreReference));
		if (edeltaEcoreReference instanceof EdeltaEcoreQualifiedReference) {
			recordOriginalENamedElement(
				((EdeltaEcoreQualifiedReference) edeltaEcoreReference).getQualification());
		}
	}

	private ENamedElement retrieveOriginalElement(final ENamedElement e, final EObject context) {
		if (e == null) {
			return null;
		}
		final var container = e.eContainer();
		if (container == null) {
			return getByName(getEPackages(context), e.getName());
		}
		return getENamedElementByName(
			((ENamedElement) container), context, e.getName());
	}

	private EList<EPackage> getEPackages(final EObject context) {
		return cache.get("getProgramMetamodels", context.eResource(),
				getProgram(context)::getMetamodels);
	}

	private ENamedElement getENamedElementByName(final ENamedElement container, final EObject context,
			final String name) {
		final var containerElements = ecoreHelper.getENamedElements(
					retrieveOriginalElement(container, context));
		return getByName(containerElements, name);
	}

	private <T extends ENamedElement> T getByName(final Iterable<T> namedElements, final String nameToSearch) {
		return IterableExtensions.<T>findFirst(namedElements,
				it -> Objects.equal(it.getName(), nameToSearch));
	}
}
