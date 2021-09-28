package edelta.ui;

import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.eclipse.xtext.ui.editor.hyperlinking.IHyperlinkHelper;
import org.eclipse.xtext.ui.editor.outline.actions.OutlineWithEditorLinker;

import edelta.interpreter.EdeltaInterpreterFactory;
import edelta.ui.interpreter.EdeltaJavaProjectAwareInterpreterFactory;
import edelta.ui.navigation.EdeltaHyperlinkinHelper;
import edelta.ui.outline.actions.EdeltaOutlineWithEditorLinker;

/**
 * Use this class to register components to be used within the Eclipse IDE.
 */
public class EdeltaUiModule extends AbstractEdeltaUiModule {
	public EdeltaUiModule(final AbstractUIPlugin plugin) {
		super(plugin);
	}

	public Class<? extends EdeltaInterpreterFactory> bindEdeltaInterpreterFactory() {
		return EdeltaJavaProjectAwareInterpreterFactory.class;
	}

	@Override
	public Class<? extends IHyperlinkHelper> bindIHyperlinkHelper() {
		return EdeltaHyperlinkinHelper.class;
	}

	public Class<? extends OutlineWithEditorLinker> bindOutlineWithEditorLinker() {
		return EdeltaOutlineWithEditorLinker.class;
	}

}
