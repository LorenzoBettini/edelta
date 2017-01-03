package edelta.resource

import com.google.inject.Inject
import com.google.inject.Singleton
import edelta.edelta.EdeltaEcoreCreateEClassExpression
import edelta.lib.EdeltaLibrary
import org.eclipse.xtext.resource.DerivedStateAwareResource
import org.eclipse.xtext.xbase.jvmmodel.JvmModelAssociator
import java.util.Map
import org.eclipse.emf.ecore.EObject

@Singleton
class EdeltaDerivedStateComputer extends JvmModelAssociator {

	@Inject extension EdeltaLibrary

	var Map<? extends EObject, ? extends EObject> targetToSourceMap = newHashMap()

	override installDerivedState(DerivedStateAwareResource resource, boolean preIndexingPhase) {
		super.installDerivedState(resource, preIndexingPhase)
		if (!preIndexingPhase) {
			val createdEClasses = resource.
				allContents.
				filter(EdeltaEcoreCreateEClassExpression).toMap[
					exp |
					newEClass(exp.name) => [
						exp.epackage.EClassifiers += it
					]
				]
			targetToSourceMap.clear
			targetToSourceMap = createdEClasses
			resource.contents += createdEClasses.keySet
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
