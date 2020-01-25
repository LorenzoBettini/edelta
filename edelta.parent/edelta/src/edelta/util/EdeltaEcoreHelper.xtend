package edelta.util

import com.google.inject.Inject
import edelta.services.IEdeltaEcoreModelAssociations
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
			getProgramENamedElementsInternal(context)
		]
	}

	def private Iterable<? extends ENamedElement> getProgramENamedElementsInternal(EObject context) {
		val prog = getProgram(context)
		val epackages = getProgramEPackages(context)
		(
			epackages.map[getAllENamedElements].flatten
		+
			prog.metamodels
		).toList
	}

	def private Iterable<? extends EPackage> getProgramEPackages(EObject context) {
		val prog = getProgram(context)
		// we also must explicitly consider the derived EPackages
		// created by our derived state computer, containing EClasses
		// created in the program
		// and also copied elements for interpreting without
		// breaking the original EMF package registries classes
		(
			prog.eResource.derivedEPackages
		+
			prog.eResource.copiedEPackages
		+
			prog.metamodels
		)
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

	def Iterable<? extends ENamedElement> getENamedElements(ENamedElement e, EObject context) {
		getENamedElementsInternal(e, context, true)
	}

	def Iterable<? extends ENamedElement> getENamedElementsWithoutCopiedEPackages(ENamedElement e, EObject context) {
		getENamedElementsInternal(e, context, false)
	}

	def private Iterable<? extends ENamedElement> getENamedElementsInternal(ENamedElement e,
		EObject context, boolean includeCopiedEPackages
	) {
		switch (e) {
			EPackage:
				cache.get("getEPackageENamedElements" + includeCopiedEPackages -> e.name, context.eResource) [
					return getEPackageENamedElementsInternal(e, context, includeCopiedEPackages)
				]
			EClass:
				cache.get("getEClassENamedElements" + includeCopiedEPackages -> e.name, context.eResource) [
					e.EPackage.getENamedElementsInternal(context, includeCopiedEPackages).
						filter(EClass).
						filter[name == e.name].
						map[EAllStructuralFeatures].flatten
				]
			EEnum:
				cache.get("getEEnumENamedElements" + includeCopiedEPackages -> e.name, context.eResource) [
					e.EPackage.getENamedElementsInternal(context, includeCopiedEPackages).
						filter(EEnum).
						filter[name == e.name].
						map[ELiterals].flatten
				]
			default:
				emptyList
		}
	}

	def private getEPackageENamedElementsInternal(EPackage ePackage, EObject context, boolean includeCopiedEPackages) {
		val ePackageName = ePackage.name
		val imported = getProgram(context).metamodels.getByName(ePackageName)
		val derived = context.eResource.derivedEPackages.getByName(ePackageName)
		if (derived !== null) {
			if (includeCopiedEPackages) {
				// there'll also be copied epackages
				val copiedEClassifiers = context.eResource.
					copiedEPackages.getByName(ePackageName).
					getEClassifiers
				return (
					derived.getEClassifiers +
					copiedEClassifiers +
					imported.getEClassifiers
				).toList
			} else {
				return (
					imported.getEClassifiers +
					derived.getEClassifiers
				).toList
			}
		}
		return imported.getEClassifiers
	}

	def <T extends ENamedElement> getByName(Iterable<T> namedElements, String nameToSearch) {
		return namedElements.findFirst[name == nameToSearch]
	}
}
