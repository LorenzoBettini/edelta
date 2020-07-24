package edelta.ui

import edelta.ui.navigation.EdeltaHyperlinkinHelper
import org.eclipse.xtend.lib.annotations.FinalFieldsConstructor
import org.eclipse.xtext.ui.editor.outline.actions.OutlineWithEditorLinker
import edelta.ui.outline.actions.EdeltaOutlineWithEditorLinker

/**
 * Use this class to register components to be used within the Eclipse IDE.
 */
@FinalFieldsConstructor
class EdeltaUiModule extends AbstractEdeltaUiModule {

	override bindIHyperlinkHelper() {
		EdeltaHyperlinkinHelper
	}

	def Class<? extends OutlineWithEditorLinker> bindOutlineWithEditorLinker() {
		EdeltaOutlineWithEditorLinker
	}
}
