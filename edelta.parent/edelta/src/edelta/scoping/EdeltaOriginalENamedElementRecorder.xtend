package edelta.scoping

import com.google.inject.Inject
import edelta.edelta.EdeltaEcoreQualifiedReference
import edelta.edelta.EdeltaEcoreReference
import edelta.resource.derivedstate.EdeltaDerivedStateHelper
import edelta.util.EdeltaEcoreHelper
import org.eclipse.emf.ecore.ENamedElement
import org.eclipse.emf.ecore.EObject
import org.eclipse.xtext.util.IResourceScopeCache

import static edelta.util.EdeltaModelUtil.*

/**
 * Records the original referred ENamedElement in an EdeltaEcoreReference expression,
 * before the interpreter runs potentially changing containments.
 * 
 * @author Lorenzo Bettini
 */
class EdeltaOriginalENamedElementRecorder {

	@Inject extension EdeltaEcoreHelper
	@Inject extension EdeltaDerivedStateHelper
	@Inject IResourceScopeCache cache

	def void recordOriginalENamedElement(EdeltaEcoreReference edeltaEcoreReference) {
		if (edeltaEcoreReference === null)
			return
		val enamedElement = edeltaEcoreReference.enamedelement
		edeltaEcoreReference
			.ecoreReferenceState
			.originalEnamedelement = 
				retrieveOriginalElement(enamedElement, edeltaEcoreReference)
		if (edeltaEcoreReference instanceof EdeltaEcoreQualifiedReference) {
			recordOriginalENamedElement(edeltaEcoreReference.qualification)
		}
	}

	def private ENamedElement retrieveOriginalElement(ENamedElement e, EObject context) {
		if (e === null)
			return null
		val container = e.eContainer
		if (container === null)
			return getEPackages(context).getByName(e.name)
		return (container as ENamedElement)
			.getENamedElementByName(context, e.name)
	}

	def private getEPackages(EObject context) {
		cache.get("getProgramMetamodels", context.eResource) [
			getProgram(context).metamodels
		]
	}

	def private getENamedElementByName(ENamedElement container, EObject context, String name) {
		container.
			retrieveOriginalElement(context).
			getENamedElements().
			getByName(name)
	}

	def private <T extends ENamedElement> getByName(Iterable<T> namedElements, String nameToSearch) {
		return namedElements.findFirst[name == nameToSearch]
	}

}
