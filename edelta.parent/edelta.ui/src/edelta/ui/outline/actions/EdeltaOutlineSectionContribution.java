package edelta.ui.outline.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ITreeViewerListener;
import org.eclipse.jface.viewers.TreeExpansionEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.xtext.ui.PluginImageHelper;
import org.eclipse.xtext.ui.editor.outline.IOutlineNode;
import org.eclipse.xtext.ui.editor.outline.actions.IOutlineContribution;
import org.eclipse.xtext.ui.editor.outline.impl.OutlinePage;
import org.eclipse.xtext.ui.editor.preferences.IPreferenceStoreAccess;

import com.google.inject.Inject;

import edelta.ui.outline.EdeltaOutlineTreeProvider.MetamodelGroupOutlineNode;
import edelta.ui.outline.EdeltaOutlineTreeProvider.ModelGroupOutlineNode;
import edelta.ui.outline.EdeltaOutlinePage;

/** Two page-local toolbar toggles for the evolved outline sections.
 * @author Lorenzo Bettini
 */
public class EdeltaOutlineSectionContribution implements IOutlineContribution {
	@Inject
	private PluginImageHelper imageHelper;

	private TreeViewer viewer;

	private final Action metamodels = new Action("Evolved metamodels", Action.AS_CHECK_BOX) {
		@Override
		public void run() {
			setExpanded(MetamodelGroupOutlineNode.class, this);
		}
	};

	private final Action models = new Action("Evolved test models", Action.AS_CHECK_BOX) {
		@Override
		public void run() {
			setExpanded(ModelGroupOutlineNode.class, this);
		}
	};

	private final ITreeViewerListener treeListener = new ITreeViewerListener() {
		@Override
		public void treeExpanded(TreeExpansionEvent event) {
			scheduleSync();
		}

		@Override
		public void treeCollapsed(TreeExpansionEvent event) {
			scheduleSync();
		}
	};

	@Override
	public void initialize(IPreferenceStoreAccess access) {
		// Expansion belongs to the current viewer, not to a workspace preference.
	}

	@Override
	public void register(OutlinePage outlinePage) {
		viewer = outlinePage.getTreeViewer();
		metamodels.setToolTipText("Expand or collapse all evolved metamodels");
		metamodels.setImageDescriptor(imageHelper.getImageDescriptor("EPackage.gif"));
		models.setToolTipText("Expand or collapse all evolved test models");
		models.setImageDescriptor(imageHelper.getImageDescriptor("ModelFile.gif"));
		var toolbar = outlinePage.getSite().getActionBars().getToolBarManager();
		toolbar.add(metamodels);
		toolbar.add(models);
		toolbar.update(true);
		viewer.addTreeListener(treeListener);
		((EdeltaOutlinePage) outlinePage).setTreeUpdatedListener(this::sync);
		sync();
	}

	@Override
	public void deregister(OutlinePage outlinePage) {
		((EdeltaOutlinePage) outlinePage).setTreeUpdatedListener(null);
		viewer.removeTreeListener(treeListener);
		viewer = null;
	}

	private void setExpanded(Class<? extends IOutlineNode> type, Action action) {
		if (viewer == null) return;
		var group = findGroup(type);
		if (group == null) {
			sync();
			return;
		}
		viewer.getControl().setRedraw(false);
		try {
			if (action.isChecked()) {
				viewer.expandToLevel(group, TreeViewer.ALL_LEVELS);
			} else {
				viewer.collapseToLevel(group, TreeViewer.ALL_LEVELS);
			}
		} finally {
			viewer.getControl().setRedraw(true);
			sync();
		}
	}

	private IOutlineNode findGroup(Class<? extends IOutlineNode> type) {
		if (!(viewer.getInput() instanceof IOutlineNode root)) return null;
		return root.getChildren().stream().flatMap(program -> program.getChildren().stream())
			.filter(type::isInstance).findFirst().orElse(null);
	}

	private void scheduleSync() {
		viewer.getControl().getDisplay().asyncExec(this::sync);
	}

	private void sync() {
		if (viewer == null) return;
		sync(metamodels, MetamodelGroupOutlineNode.class);
		sync(models, ModelGroupOutlineNode.class);
	}

	private void sync(Action action, Class<? extends IOutlineNode> type) {
		var group = findGroup(type);
		var item = group == null ? null : findItem(group);
		action.setEnabled(group != null);
		action.setChecked(item != null && fullyExpanded(item));
	}

	private TreeItem findItem(IOutlineNode group) {
		return findItem(viewer.getTree().getItems(), group);
	}

	private TreeItem findItem(TreeItem[] items, IOutlineNode group) {
		for (var item : items) {
			if (item.getData() == group) return item;
			var descendant = findItem(item.getItems(), group);
			if (descendant != null) return descendant;
		}
		return null;
	}

	private boolean fullyExpanded(TreeItem item) {
		var node = (IOutlineNode) item.getData();
		if (node.hasChildren() && !item.getExpanded()) {
			return false;
		}
		for (var child : item.getItems()) {
			if (!fullyExpanded(child)) return false;
		}
		return true;
	}
}
