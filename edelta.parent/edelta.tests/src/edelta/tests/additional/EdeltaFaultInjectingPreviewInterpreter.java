package edelta.tests.additional;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.impl.DynamicEObjectImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceImpl;

import edelta.edelta.EdeltaTestModels;
import edelta.interpreter.EdeltaSafeInterpreter;
import edelta.lib.EdeltaRuntime;
import edelta.resource.EdeltaModelSourceSelector;
import edelta.resource.derivedstate.EdeltaCopiedEPackagesMap;
import edelta.resource.derivedstate.EdeltaModelPreview;

/**
 * Injects private-resource failures at the two preview finalization boundaries.
 *
 * @author Lorenzo Bettini
 */
public class EdeltaFaultInjectingPreviewInterpreter extends EdeltaSafeInterpreter {
	private EdeltaModelSourceSelector.Result selection;

	@Override
	public void configurePreview(EdeltaModelSourceSelector.Result selection,
			EdeltaModelPreview preview, EdeltaTestModels declaration) {
		this.selection = failsBeforeRoot(selection) ? replaceSelectedResource(selection) : selection;
		if (this.selection.selected().getFirst().path().getFileName().toString().equals("final.xmi")) {
			// Install the faulty source before the migrator starts guarding it.
			var resource = this.selection.selected().getFirst().resource();
			var root = resource.getContents().getFirst();
			resource.getContents().set(0, new DynamicEObjectImpl(root.eClass()) {
				@Override
				public boolean eIsSet(EStructuralFeature feature) {
					throw new IllegalStateException("final copy failure");
				}
			});
		}
		super.configurePreview(this.selection, preview, declaration);
	}

	private boolean failsBeforeRoot(EdeltaModelSourceSelector.Result selection) {
		return selection.selected().getFirst().path().getFileName().toString()
			.equals("before-root.xmi");
	}

	private EdeltaModelSourceSelector.Result replaceSelectedResource(
			EdeltaModelSourceSelector.Result selection) {
		var selected = selection.selected().getFirst();
		var original = selected.resource();
		var replacement = new XMIResourceImpl(original.getURI()) {
			@Override
			public String getEncoding() {
				throw new IllegalStateException("copy setup failure");
			}
		};
		var roots = new ArrayList<>(original.getContents());
		original.getContents().clear();
		selection.resourceSet().getResources().remove(original);
		replacement.getContents().addAll(roots);
		selection.resourceSet().getResources().add(replacement);
		var replacedSelection = new EdeltaModelSourceSelector.Selected(
			selected.path(), selected.entry(), replacement);
		return new EdeltaModelSourceSelector.Result(selection.resourceSet(),
			List.of(replacedSelection), selection.ignored(), selection.failures(),
			selection.sourceEntries());
	}

	@Override
	protected EdeltaRuntime createRuntime(EdeltaCopiedEPackagesMap copies) {
		var name = selection.selected().getFirst().path().getFileName().toString();
		if (name.equals("pre-runtime.xmi")) {
			throw new IllegalStateException("runtime setup before migrator assignment");
		}
		if (name.equals("setup.xmi")) {
			var copied = copies.values().iterator().next();
			var sandbox = copied.eResource();
			sandbox.getContents().clear();
			try {
				return super.createRuntime(copies);
			} finally {
				sandbox.getContents().add(copied);
			}
		}
		var runtime = super.createRuntime(copies);
		if (name.equals("callback.xmi")) {
			runtime.modelMigration(migrator -> {
				throw new IllegalStateException("callback without source expression");
			});
		}
		return runtime;
	}
}
