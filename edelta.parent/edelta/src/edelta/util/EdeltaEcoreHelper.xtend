package edelta.util

import com.google.inject.Inject
import edelta.services.IEdeltaEcoreModelAssociations
import java.util.Collection
import java.util.Collections
import org.eclipse.emf.ecore.EClass
import org.eclipse.emf.ecore.EEnum
import org.eclipse.emf.ecore.ENamedElement
import org.eclipse.emf.ecore.EObject
import org.eclipse.emf.ecore.EPackage
import org.eclipse.xtext.util.IResourceScopeCache

/**
 * Helper methods for accessing Ecore elements.
 * 
 * Computations that require filtering or mapping are cached in the scope
 * of the resource.
 * 
 * @author Lorenzo Bettini
 */
class EdeltaEcoreHelper {

	@Inject IResourceScopeCache cache
	@Inject extension EdeltaModelUtil
	@Inject extension IEdeltaEcoreModelAssociations

	def Iterable<? extends ENamedElement> getProgramENamedElements(EObject context) {
		cache.get("getProgramENamedElements", context.eResource) [
			val prog = getProgram(context)
			// we also must explicitly consider the derived EPackages
			// created by our derived state computer, containing EClasses
			// created in the program
			// and also copied elements for interpreting without
			// breaking the original EMF package registries classes
			(
				prog.eResource.derivedEPackages.
					getAllENamedElements
			+
				prog.eResource.copiedEPackages.
					getAllENamedElements
			+
				prog.metamodels.getAllENamedElements
			+
				prog.metamodels
			).toList
		]
	}

	def private getAllENamedElements(Iterable<EPackage> e) {
		e.map[getAllENamedElements].flatten
	}

	def private Iterable<ENamedElement> getAllENamedElements(EPackage e) {
		val classifiers = e.EClassifiers
		val inner = classifiers.map[
			switch (it) {
				// important: don't use EAllStructuralFeatures
				// or we can get into
				// Cyclic linking detected : EdeltaEcoreReference.enamedelement->EdeltaEcoreReference.enamedelement
				// when we resolve ecore reference to supertypes
				EClass: EStructuralFeatures
				EEnum: ELiterals
				default: <ENamedElement>emptyList
			}
		].flatten
		classifiers + inner
	}

	def getAllEClasses(EPackage e) {
		e.EClassifiers.filter(EClass)
	}

	def dispatch Iterable<? extends ENamedElement> getENamedElements(ENamedElement e, EObject context) {
		Collections.emptyList
	}

	def dispatch Iterable<? extends ENamedElement> getENamedElements(EPackage e, EObject context) {
		cache.get("getEPackageENamedElements" -> e.name, context.eResource) [
			val derived = context.eResource.derivedEPackages.getEPackageByName(e.name)
			if (derived !== null) {
				// there'll also be copied epackages
				val copied = context.eResource.copiedEPackages.getEPackageByName(e.name)
				return (
					derived.getEClassifiers +
					copied.getEClassifiers +
					e.getEClassifiers
				).toList
			}
			return e.getEClassifiers
		]
	}

	def dispatch Iterable<? extends ENamedElement> getENamedElements(EClass e, EObject context) {
		e.EAllStructuralFeatures
	}

	def dispatch Iterable<? extends ENamedElement> getENamedElements(EEnum e, EObject context) {
		e.ELiterals
	}

	def private getEPackageByName(Collection<EPackage> epackages, String packageName) {
		return epackages.findFirst[name == packageName]
	}
}
