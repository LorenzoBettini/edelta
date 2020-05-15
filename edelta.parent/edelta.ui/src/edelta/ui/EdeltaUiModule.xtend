package edelta.ui

import org.eclipse.xtend.lib.annotations.FinalFieldsConstructor
import edelta.ui.navigation.EdeltaHyperlinkinHelper

/**
 * Use this class to register components to be used within the Eclipse IDE.
 */
@FinalFieldsConstructor
class EdeltaUiModule extends AbstractEdeltaUiModule {

	override bindIHyperlinkHelper() {
		EdeltaHyperlinkinHelper
	}

}
