package edelta.lib;

import java.io.IOException;
import java.util.function.Function;

import org.eclipse.emf.ecore.resource.Resource;

/**
 * The main engine to load Ecore and model files and migrate them, using an
 * {@link AbstractEdelta}.
 * 
 * @author Lorenzo Bettini
 *
 */
public class EdeltaEngine {

	public static interface EdeltaRuntimeProvider extends Function<AbstractEdelta, AbstractEdelta> {
	}

	private EdeltaModelManager originalModelManager = new EdeltaModelManager();
	private EdeltaRuntimeProvider provider;
	private EdeltaModelManager evolvingModelManager;

	public EdeltaEngine(EdeltaRuntimeProvider provider) {
		this.provider = provider;
	}

	public Resource loadEcoreFile(String path) {
		return originalModelManager.loadEcoreFile(path);
	}

	public Resource loadModelFile(String path) {
		return originalModelManager.loadModelFile(path);
	}

	public void execute() throws Exception {
		var migrator = new EdeltaModelMigrator(originalModelManager);
		evolvingModelManager = migrator.getEvolvingModelManager();
		var edelta = provider.apply(new EdeltaDefaultRuntime(migrator));
		edelta.execute();
		migrator.copyModels();
	}

	public void save(String outputPath) throws IOException {
		evolvingModelManager.saveEcores(outputPath);
		evolvingModelManager.saveModels(outputPath);
	}

}
