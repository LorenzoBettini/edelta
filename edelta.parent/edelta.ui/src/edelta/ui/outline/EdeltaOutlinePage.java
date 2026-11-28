package edelta.ui.outline;

import org.eclipse.xtext.ui.editor.outline.impl.OutlinePage;

/** Notifies page-local controls after Xtext has restored the outline tree.
 * @author Lorenzo Bettini
 */
public class EdeltaOutlinePage extends OutlinePage {
	private Runnable treeUpdatedListener;

	/** Registers the current toolbar contribution's refresh callback. */
	public void setTreeUpdatedListener(Runnable listener) {
		treeUpdatedListener = listener;
	}

	@Override
	protected void treeUpdated() {
		if (treeUpdatedListener != null) treeUpdatedListener.run();
	}
}
