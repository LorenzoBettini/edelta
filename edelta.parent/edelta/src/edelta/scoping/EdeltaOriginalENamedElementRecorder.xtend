package edelta.scoping

import com.google.inject.Inject
import edelta.edelta.EdeltaEcoreReference
import edelta.util.EdeltaEcoreHelper
import edelta.util.EdeltaModelUtil
import org.eclipse.emf.ecore.EClassifier
import org.eclipse.emf.ecore.ENamedElement
import org.eclipse.emf.ecore.EObject
import org.eclipse.xtext.util.IResourceScopeCache
import edelta.edelta.EdeltaEcoreQualifiedReference
import org.eclipse.emf.ecore.EPackage
import org.eclipse.emf.ecore.EStructuralFeature

/**
 * Records the original referred ENamedElement in an EdeltaEcoreReference expression,
 * before the interpreter runs potentially changing containments.
 * 
 * @author Lorenzo Bettini
 */
class EdeltaOriginalENamedElementRecorder {

	@Inject extension EdeltaEcoreHelper
	@Inject extension EdeltaModelUtil

	@Inject IResourceScopeCache cache

	def void recordOriginalENamedElement(EdeltaEcoreReference edeltaEcoreReference) {
		val enamedElement = edeltaEcoreReference.enamedelement
		edeltaEcoreReference.originalEnamedelement = retrieveOriginalElement(enamedElement, edeltaEcoreReference)
		if (edeltaEcoreReference instanceof EdeltaEcoreQualifiedReference) {
			recordOriginalENamedElement(edeltaEcoreReference.qualification)
		}
	}

	def private ENamedElement retrieveOriginalElement(ENamedElement e, EObject context) {
		switch (e) {
			EPackage:
				getProgramEPackage(e, context)
			EClassifier: retrieveOriginalElement(e.EPackage, context).
				getENamedElementsWithoutCopiedEPackages(context).getByName(e.name)
			EStructuralFeature:
				retrieveOriginalElement(e.EContainingClass, context).
				getENamedElementsWithoutCopiedEPackages(context).getByName(e.name)
		}
	}

	def private getProgramEPackage(EPackage e, EObject context) {
		getEPackages(context).
				getByName(e.name)
	}

	def private getEPackages(EObject context) {
		cache.get("getProgramMetamodels", context.eResource) [
			getProgram(context).metamodels
		]
	}
}
