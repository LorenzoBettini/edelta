package edelta.util

import com.google.inject.Inject
import edelta.resource.derivedstate.EdeltaDerivedStateHelper
import java.util.List
import org.eclipse.emf.ecore.EClass
import org.eclipse.emf.ecore.EEnum
import org.eclipse.emf.ecore.ENamedElement
import org.eclipse.emf.ecore.EObject
import org.eclipse.emf.ecore.EPackage
import org.eclipse.xtext.util.IResourceScopeCache

import static edelta.util.EdeltaModelUtil.*

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
	@Inject extension EdeltaDerivedStateHelper

	/**
	 * Returns all the ENamedElements in the program:
	 * it uses the copied EPackages if present, otherwise it uses the original
	 * imported metamodels, but NOT both.
	 */
	def Iterable<? extends ENamedElement> getProgramENamedElements(EObject context) {
		cache.get("getProgramENamedElements", context.eResource) [
			val prog = getProgram(context)
			val copied = prog.eResource.copiedEPackagesMap.values
			// copied EPackage are present only when there's at least one modifyEcore
			val epackages = copied.empty ? prog.metamodels : copied
			return (
				epackages.map[getAllENamedElements].flatten
			+
				epackages
			).toList
		]
	}

	/**
	 * Returns all ENamedElements of the passed EPackage, recursively,
	 * including subpackages and getAllENamedElements on each subpackage
	 */
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
		classifiers + inner + e.ESubpackages +
			e.ESubpackages.map[getAllENamedElements].flatten
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
				cache.get("getEPackageENamedElements" + includeCopiedEPackages -> e, context.eResource) [
					return getEPackageENamedElementsInternal(e)
				]
			EClass:
				cache.get("getEClassENamedElements" + includeCopiedEPackages -> e, context.eResource) [
					e.EStructuralFeatures
				]
			EEnum:
				cache.get("getEEnumENamedElements" + includeCopiedEPackages -> e, context.eResource) [
					e.ELiterals
				]
			default:
				emptyList
		}
	}

	def private List<? extends ENamedElement> getEPackageENamedElementsInternal(EPackage ePackage) {
		// could be null if we searched for a new subpackage
		// in the imported metamodels
		if (ePackage === null)
			return emptyList
		return (
			ePackage.getEClassifiers +
			ePackage.getESubpackages
		).toList
	}

	def <T extends ENamedElement> getByName(Iterable<T> namedElements, String nameToSearch) {
		return namedElements.findFirst[name == nameToSearch]
	}

	/**
	 * Try to retrieve an EPackage with the same name of the passed EPackage
	 * in the given EPackages, possibly by inspecting the super package relation.
	 * In case of loop in the super package relation, simply returns the passed
	 * EPackage.
	 */
	def EPackage findEPackageByNameInRootEPackages(Iterable<EPackage> roots, EPackage p) {
		if (hasCycleInSuperPackage(p))
			return p
		if (p.ESuperPackage === null) {
			return roots.getByName(p.name)
		} else {
			val foundSuperPackage =
				findEPackageByNameInRootEPackages(roots, p.ESuperPackage)
			// it might not be found (e.g., in copied EPackages)
			if (foundSuperPackage === null)
				return null
			return foundSuperPackage.ESubpackages.getByName(p.name)
		}
	}
}
