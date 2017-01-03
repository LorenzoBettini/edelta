package edelta.resource

import com.google.inject.Inject
import com.google.inject.Singleton
import edelta.edelta.EdeltaEcoreCreateEAttributeExpression
import edelta.edelta.EdeltaEcoreCreateEClassExpression
import edelta.lib.EdeltaLibrary
import java.util.Map
import org.eclipse.emf.ecore.EObject
import org.eclipse.xtext.EcoreUtil2
import org.eclipse.xtext.resource.DerivedStateAwareResource
import org.eclipse.xtext.xbase.jvmmodel.JvmModelAssociator
import org.eclipse.emf.ecore.EClass

@Singleton
class EdeltaDerivedStateComputer extends JvmModelAssociator {

	@Inject extension EdeltaLibrary

	var Map<EObject, EObject> targetToSourceMap = newHashMap()

	override installDerivedState(DerivedStateAwareResource resource, boolean preIndexingPhase) {
		super.installDerivedState(resource, preIndexingPhase)
		if (!preIndexingPhase) {
			targetToSourceMap.clear
			for (exp :resource.allContents.toIterable.filter(EdeltaEcoreCreateEClassExpression)) {
				val expPackage = exp.epackage
				val derivedEClass = newEClass(exp.name) => [
					// could be null in an incomplete expression
					if (expPackage !== null)
						expPackage.EClassifiers += it
				]
				targetToSourceMap.put(derivedEClass, exp)
				for (e : EcoreUtil2.getAllContentsOfType(exp, EdeltaEcoreCreateEAttributeExpression)) {
					val derivedEAttribute = newEAttribute(e.name)
					derivedEClass.EStructuralFeatures += derivedEAttribute
					targetToSourceMap.put(derivedEAttribute, e)
				}
			}
			// we must add only EClasses: if we add to the resource also contained
			// elements like EAttributes then they'll be removed from the
			// features of the containing class, due to their containment property
			// derived features will still be visible
			resource.contents += targetToSourceMap.keySet.filter(EClass)
		}
	}

	override discardDerivedState(DerivedStateAwareResource resource) {
		super.discardDerivedState(resource)
		targetToSourceMap.clear
	}

	override getPrimarySourceElement(EObject jvmElement) {
		if (jvmElement !== null) {
			val sourceElement = targetToSourceMap.get(jvmElement)
			if (sourceElement !== null)
				return sourceElement
		}
		return super.getPrimarySourceElement(jvmElement)
	}

}
