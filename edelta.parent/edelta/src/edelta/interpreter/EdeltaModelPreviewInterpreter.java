package edelta.interpreter;

import java.util.ArrayList;
import java.util.Objects;
import java.util.function.Consumer;

import org.eclipse.emf.ecore.EPackage;

import edelta.edelta.EdeltaProgram;
import edelta.edelta.EdeltaTestModels;
import edelta.lib.EdeltaDefaultRuntime;
import edelta.lib.EdeltaModelManager;
import edelta.lib.EdeltaModelMigrator;
import edelta.lib.EdeltaRuntime;
import edelta.resource.EdeltaModelSourceSelector;
import edelta.resource.derivedstate.EdeltaCopiedEPackagesMap;
import edelta.resource.derivedstate.EdeltaModelPreview;

/**
 * Interprets an optional model preview with the derived state's exact Ecore copies.
 * The input and final model resource sets are private; the sandbox still owns the
 * copied EPackages during interpretation and returns them to the program afterward.
 *
 * @author Lorenzo Bettini
 */
public class EdeltaModelPreviewInterpreter extends EdeltaDefaultInterpreter {
	private EdeltaModelSourceSelector.Result selection;
	private EdeltaModelPreview preview;
	private EdeltaTestModels declaration;
	private PreviewMigrator migrator;

	/**
	 * Configures one interpretation with selected originals. The interpreter
	 * releases their private resource set after migration, even on failure.
	 *
	 * @param selection selected original models and their source declarations
	 * @param preview derived-state storage for final models and failures
	 * @param declaration the program's test-model declaration
	 */
	public void configurePreview(EdeltaModelSourceSelector.Result selection,
			EdeltaModelPreview preview, EdeltaTestModels declaration) {
		this.selection = selection;
		this.preview = preview;
		this.declaration = declaration;
	}

	@Override
	protected EdeltaRuntime createRuntime(EdeltaCopiedEPackagesMap copies) {
		if (selection == null) {
			return super.createRuntime(copies);
		}
		var originals = new EdeltaModelManager();
		var evolving = new EdeltaModelManager();
		copies.values().stream()
			.map(copies::getOriginal)
			.filter(EPackage.class::isInstance)
			.map(EPackage.class::cast)
			.map(EPackage::eResource)
			.filter(Objects::nonNull)
			.distinct()
			.forEach(originals.getEcoreResources()::add);
		for (var copied : copies.values()) {
			evolving.loadEPackage(copied);
		}
		for (var selected : selection.selected()) {
			originals.getModelResources().add(selected.resource());
		}
		try {
			migrator = new PreviewMigrator(originals, evolving, copies, preview, declaration);
			return new EdeltaDefaultRuntime(migrator);
		} catch (RuntimeException exception) {
			preview.addFailure(new EdeltaModelPreview.Failure("SETUP", declaration,
				null, exception));
			return super.createRuntime(copies);
		}
	}

	@Override
	public void evaluateModifyEcoreOperations(EdeltaProgram program) {
		if (selection == null) {
			super.evaluateModifyEcoreOperations(program);
			return;
		}
		var finalCopy = false;
		try {
			super.evaluateModifyEcoreOperations(program);
			if (!preview.hasFailures()) {
				finalCopy = true;
				migrator.copyModels();
				publishFinalModels();
			}
		} catch (RuntimeException | Error exception) {
			if (!preview.hasFailures()) {
				preview.addFailure(new EdeltaModelPreview.Failure(
					finalCopy ? "FINAL_COPY" : "INTERPRETATION", declaration, null, exception));
			}
			throw exception;
		} finally {
			if (preview.hasFailures() && migrator != null) {
				for (var resource : migrator.getEvolvingModelManager().getModelResources()) {
					resource.unload();
				}
				migrator.getEvolvingModelManager().clearModels();
			}
			for (var resource : selection.resourceSet().getResources()) {
				resource.unload();
			}
			selection.resourceSet().getResources().clear();
			selection = null;
			preview = null;
			declaration = null;
			migrator = null;
		}
	}

	private void publishFinalModels() {
		var resources = new ArrayList<>(migrator.getEvolvingModelManager().getModelResources());
		preview.setModels(EdeltaModelPreview.associate(selection.selected(), resources));
	}

	/** Records callback failures before Xbase or the safe interpreter can recover. */
	private static class PreviewMigrator extends EdeltaModelMigrator {
		private final EdeltaModelPreview preview;
		private final EdeltaTestModels declaration;

		private PreviewMigrator(EdeltaModelManager originals, EdeltaModelManager evolving,
				EdeltaCopiedEPackagesMap copies, EdeltaModelPreview preview,
				EdeltaTestModels declaration) {
			super(originals, evolving, copies.getOriginalToCopyMap());
			this.preview = preview;
			this.declaration = declaration;
		}

		@Override
		public void runMigrationCallback(Consumer<EdeltaModelMigrator> callback) {
			try {
				super.runMigrationCallback(callback);
			} catch (RuntimeException | Error exception) {
				if (!preview.hasFailures()) {
					preview.addFailure(new EdeltaModelPreview.Failure("MIGRATION",
						declaration, null, exception));
				}
				throw exception;
			}
		}
	}
}
