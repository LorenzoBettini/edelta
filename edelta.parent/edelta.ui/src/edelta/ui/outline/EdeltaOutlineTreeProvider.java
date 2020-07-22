/**
 * generated by Xtext 2.10.0
 */
package edelta.ui.outline;

import org.eclipse.emf.ecore.EPackage;
import org.eclipse.xtext.ui.editor.outline.IOutlineNode;
import org.eclipse.xtext.ui.editor.outline.impl.DefaultOutlineTreeProvider;

import com.google.inject.Inject;

import edelta.edelta.EdeltaModifyEcoreOperation;
import edelta.edelta.EdeltaOperation;
import edelta.edelta.EdeltaProgram;
import edelta.resource.derivedstate.EdeltaDerivedStateHelper;

/**
 * Customization of the default outline structure.
 * 
 * See https://www.eclipse.org/Xtext/documentation/304_ide_concepts.html#outline
 */
public class EdeltaOutlineTreeProvider extends DefaultOutlineTreeProvider {
	@Inject
	private EdeltaDerivedStateHelper derivedStateHelper;

	protected void _createChildren(final IOutlineNode parentNode, final EdeltaProgram p) {
		for (final EdeltaOperation o : p.getOperations()) {
			this.createNode(parentNode, o);
		}
		for (final EdeltaModifyEcoreOperation o : p.getModifyEcoreOperations()) {
			this.createNode(parentNode, o);
		}
		for (final EPackage copiedEPackage : this.derivedStateHelper.getCopiedEPackagesMap(p.eResource()).values()) {
			// the cool thing is that we don't need to provide
			// customization in the label provider for EPackage and EClass
			// since Xtext defaults to the .edit plugin :)
			this.createNode(parentNode, copiedEPackage);
		}
	}

	public boolean _isLeaf(final EdeltaOperation m) {
		return true;
	}

	public boolean _isLeaf(final EdeltaModifyEcoreOperation m) {
		return true;
	}
}
