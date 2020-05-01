package edelta.scoping

import com.google.inject.Inject
import edelta.edelta.EdeltaEcoreQualifiedReference
import edelta.edelta.EdeltaEcoreReference
import edelta.util.EdeltaEcoreHelper
import edelta.util.EdeltaModelUtil
import org.eclipse.emf.ecore.EClassifier
import org.eclipse.emf.ecore.EEnumLiteral
import org.eclipse.emf.ecore.ENamedElement
import org.eclipse.emf.ecore.EObject
import org.eclipse.emf.ecore.EPackage
import org.eclipse.emf.ecore.EStructuralFeature
import org.eclipse.xtext.util.IResourceScopeCache

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
		if (edeltaEcoreReference === null)
			return
		val enamedElement = edeltaEcoreReference.enamedelement
		edeltaEcoreReference.originalEnamedelement = retrieveOriginalElement(enamedElement, edeltaEcoreReference)
		if (edeltaEcoreReference instanceof EdeltaEcoreQualifiedReference) {
			recordOriginalENamedElement(edeltaEcoreReference.qualification)
		}
	}

	def private ENamedElement retrieveOriginalElement(ENamedElement e, EObject context) {
		switch (e) {
			EPackage: getEPackages(context).findEPackageByNameInRootEPackages(e)
			EClassifier: e.EPackage.getENamedElementByName(context, e.name)
			EStructuralFeature: e.EContainingClass.getENamedElementByName(context, e.name)
			EEnumLiteral: e.EEnum.getENamedElementByName(context, e.name)
		}
	}

	def private getEPackages(EObject context) {
		cache.get("getProgramMetamodels", context.eResource) [
			getProgram(context).metamodels
		]
	}

	def private getENamedElementByName(ENamedElement container, EObject context, String name) {
		container.
			retrieveOriginalElement(context).
			getENamedElementsWithoutCopiedEPackages(context).
			getByName(name)
	}
}
