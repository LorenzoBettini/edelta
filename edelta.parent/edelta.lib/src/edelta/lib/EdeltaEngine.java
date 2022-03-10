package edelta.lib;

import java.io.IOException;
import java.util.function.UnaryOperator;

import org.eclipse.emf.ecore.resource.Resource;

/**
 * The main engine to load Ecore and model files and migrate them, using an
 * {@link AbstractEdelta}.
 * 
 * @author Lorenzo Bettini
 *
 */
public class EdeltaEngine {

	/**
	 * The functional interface representing the constructor argument.
	 * 
	 * @author Lorenzo Bettini
	 *
	 */
	@FunctionalInterface
	public static interface EdeltaRuntimeProvider extends UnaryOperator<AbstractEdelta> {
	}

	private EdeltaRuntimeProvider provider;

	private EdeltaModelManager originalModelManager = new EdeltaModelManager();
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

	/**
	 * Executes the actual evolution of the loaded Ecore files and of the (possibly)
	 * loaded model files.
	 * 
	 * Such an evolution is NOT performed on the loaded Ecore and model files: it is
	 * executed on copies.
	 * 
	 * @throws Exception
	 */
	public void execute() throws Exception {
		var migrator = new EdeltaModelMigrator(originalModelManager);
		evolvingModelManager = migrator.getEvolvingModelManager();
		var edelta = provider.apply(new EdeltaDefaultRuntime(migrator));
		edelta.execute();
		migrator.copyModels();
	}

	/**
	 * Saves the evolved Ecore and model files to the specified outputPath.
	 * 
	 * @param outputPath
	 * @throws IOException
	 */
	public void save(String outputPath) throws IOException {
		evolvingModelManager.saveEcores(outputPath);
		evolvingModelManager.saveModels(outputPath);
	}

}
