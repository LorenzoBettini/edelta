package edelta.ui;

import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;
import org.eclipse.xtext.ui.editor.outline.actions.IOutlineContribution;
import org.eclipse.xtext.ui.editor.hyperlinking.IHyperlinkHelper;
import org.eclipse.xtext.ui.editor.outline.actions.OutlineWithEditorLinker;

import com.google.inject.Binder;

import edelta.interpreter.EdeltaInterpreterFactory;
import edelta.ui.interpreter.EdeltaJavaProjectAwareInterpreterFactory;
import edelta.ui.navigation.EdeltaHyperlinkinHelper;
import edelta.ui.outline.actions.EdeltaOutlineWithEditorLinker;
import edelta.ui.outline.actions.EdeltaOutlineSectionContribution;
import edelta.ui.outline.EdeltaOutlinePage;

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

	@Override
	public Class<? extends IContentOutlinePage> bindIContentOutlinePage() {
		return EdeltaOutlinePage.class;
	}

	public void configureEdeltaOutlineSectionContribution(Binder binder) {
		binder.bind(IOutlineContribution.class).to(EdeltaOutlineSectionContribution.class);
	}

}
