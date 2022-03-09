package edelta.lib;

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

	public static interface EdeltaRuntimeProvider extends Function<EdeltaModelManager, AbstractEdelta> {
	}

	private EdeltaModelManager originalModelManager = new EdeltaModelManager();
	private EdeltaRuntimeProvider provider;

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
		var evolvingModelManager = new EdeltaModelManager();
		evolvingModelManager.copyEcores(originalModelManager);
		var edelta = provider.apply(evolvingModelManager);
		edelta.execute();
	}

}
