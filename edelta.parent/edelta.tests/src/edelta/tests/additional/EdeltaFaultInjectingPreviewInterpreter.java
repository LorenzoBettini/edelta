package edelta.tests.additional;

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
		super.configurePreview(selection, preview, declaration);
		this.selection = selection;
	}

	@Override
	protected EdeltaRuntime createRuntime(EdeltaCopiedEPackagesMap copies) {
		var name = selection.selected().getFirst().path().getFileName().toString();
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
		if (name.equals("final.xmi")) {
			selection.selected().getFirst().resource().getContents().clear();
		}
		return runtime;
	}
}
