package edelta.resource

import com.google.inject.Inject
import com.google.inject.Singleton
import edelta.edelta.EdeltaEcoreCreateEClassExpression
import edelta.lib.EdeltaLibrary
import org.eclipse.xtext.resource.DerivedStateAwareResource
import org.eclipse.xtext.xbase.jvmmodel.JvmModelAssociator

@Singleton
class EdeltaDerivedStateComputer extends JvmModelAssociator {

	@Inject extension EdeltaLibrary

	override installDerivedState(DerivedStateAwareResource resource, boolean preIndexingPhase) {
		super.installDerivedState(resource, preIndexingPhase)
		if (!preIndexingPhase) {
			val createdEClasses = resource.
				allContents.
				filter(EdeltaEcoreCreateEClassExpression).map[
					cr |
					newEClass(cr.name)
				].toList
			resource.contents += createdEClasses
		}
	}
}
