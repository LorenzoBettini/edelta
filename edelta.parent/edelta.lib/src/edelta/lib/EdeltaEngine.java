package edelta.lib;

import java.io.IOException;
import java.io.InputStream;
import java.util.function.UnaryOperator;

import org.eclipse.emf.ecore.resource.Resource;

/**
 * The main engine to load Ecore and model files and migrate them, using an
 * {@link EdeltaRuntime}.
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
	public static interface EdeltaRuntimeProvider extends UnaryOperator<EdeltaRuntime> {
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

	/**
	 * See {@link EdeltaModelManager#loadEcoreFile(String, InputStream)}.
	 * 
	 * @param ecoreFile
	 * @param inputStream
	 * @return 
	 * @throws IOException 
	 */
	public Resource loadEcoreFile(String ecoreFile, InputStream inputStream) throws IOException {
		return originalModelManager.loadEcoreFile(ecoreFile, inputStream);
	}

	public Resource loadModelFile(String path) {
		return originalModelManager.loadModelFile(path);
	}

	public void setOriginalModelManager(EdeltaModelManager manager) {
		originalModelManager = manager;
	}

	public EdeltaModelManager getEvolvingModelManager() {
		return evolvingModelManager;
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
		saveEcores(outputPath);
		saveModels(outputPath);
	}

	/**
	 * Saves the evolved Ecore files to the specified outputPath.
	 * 
	 * @param outputPath
	 * @throws IOException
	 */
	public void saveEcores(String outputPath) throws IOException {
		evolvingModelManager.saveEcores(outputPath);
	}

	/**
	 * Saves the evolved model files to the specified outputPath.
	 * 
	 * @param outputPath
	 * @throws IOException
	 */
	public void saveModels(String outputPath) throws IOException {
		evolvingModelManager.saveModels(outputPath);
	}

}
