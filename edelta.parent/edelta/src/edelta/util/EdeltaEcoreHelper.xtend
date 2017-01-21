/*
 * generated by Xtext 2.10.0
 */
package edelta.util

import com.google.inject.Inject
import edelta.resource.EdeltaDerivedStateComputer
import java.util.Collections
import org.eclipse.emf.ecore.EClass
import org.eclipse.emf.ecore.EEnum
import org.eclipse.emf.ecore.ENamedElement
import org.eclipse.emf.ecore.EObject
import org.eclipse.emf.ecore.EPackage
import org.eclipse.xtext.EcoreUtil2
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
	@Inject extension EdeltaDerivedStateComputer

	def Iterable<? extends ENamedElement> getProgramENamedElements(EObject context) {
		cache.get("getENamedElements", context.eResource) [
			val prog = getProgram(context)
			// we also must explicitly consider the derived EPackages
			// created by our derived state computer, containing EClasses
			// created in the program
			(
				prog.eResource.derivedEPackages.
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

	def private getAllENamedElements(EPackage e) {
		EcoreUtil2.getAllContentsOfType(e, ENamedElement)
	}

	def dispatch Iterable<? extends ENamedElement> getENamedElements(ENamedElement e, EObject context) {
		Collections.emptyList
	}

	def dispatch Iterable<? extends ENamedElement> getENamedElements(EPackage e, EObject context) {
		val derived = context.eResource.derivedEPackages.findFirst[name == e.name]
		(derived?.getEClassifiers ?: emptyList) + e.getEClassifiers
	}

	def dispatch Iterable<? extends ENamedElement> getENamedElements(EClass e, EObject context) {
		e.EAllStructuralFeatures
	}

	def dispatch Iterable<? extends ENamedElement> getENamedElements(EEnum e, EObject context) {
		e.ELiterals
	}
}
