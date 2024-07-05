package edelta.ui.launching;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.xtext.xbase.ui.launching.JavaApplicationLaunchShortcut;
import org.eclipse.xtext.xbase.ui.launching.LaunchShortcutUtil;

public class EdeltaJavaApplicationLaunchShortcut extends JavaApplicationLaunchShortcut {
	@Override
	public void launch(ISelection selection, String mode) {
		if (selection instanceof IStructuredSelection structuredSelection) {
			final IStructuredSelection newSelection = LaunchShortcutUtil
					.replaceWithJavaElementDelegates(structuredSelection, EdeltaJavaElementDelegateMainLaunch.class);
			super.launch(newSelection, mode);
		}
	}
	@Override
	public void launch(IEditorPart editor, String mode) {
		final EdeltaJavaElementDelegateMainLaunch javaElementDelegate = editor.getAdapter(EdeltaJavaElementDelegateMainLaunch.class);
		if (javaElementDelegate != null) {
			super.launch(new StructuredSelection(javaElementDelegate), mode);
		} else {
			super.launch(editor, mode);
		}
	}
}