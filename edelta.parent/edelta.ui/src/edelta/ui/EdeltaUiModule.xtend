package edelta.ui

import edelta.ui.wizard.EdeltaProjectCreatorCustom
import org.eclipse.xtend.lib.annotations.FinalFieldsConstructor

/**
 * Use this class to register components to be used within the Eclipse IDE.
 */
@FinalFieldsConstructor
class EdeltaUiModule extends AbstractEdeltaUiModule {

	override bindIProjectCreator() {
		EdeltaProjectCreatorCustom
	}

}
