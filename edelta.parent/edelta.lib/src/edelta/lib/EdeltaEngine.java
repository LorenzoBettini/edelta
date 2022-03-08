package edelta.lib;

import org.eclipse.emf.ecore.resource.Resource;

/**
 * The main engine to load Ecore and model files and migrate them,
 * using an {@link AbstractEdelta}.
 * 
 * @author Lorenzo Bettini
 *
 */
public class EdeltaEngine {

	private Class<? extends AbstractEdelta> edeltaClass;
	private EdeltaModelManager modelManager = new EdeltaModelManager();

	public EdeltaEngine(Class<? extends AbstractEdelta> edeltaClass) {
		this.edeltaClass = edeltaClass;
	}

	public Resource loadEcoreFile(String path) {
		return modelManager.loadEcoreFile(path);
	}

	public Resource loadModelFile(String path) {
		return modelManager.loadModelFile(path);
	}

	public void execute() {
		try {
			var constructor = edeltaClass.getConstructor(EdeltaModelManager.class);
			var edelta = constructor.newInstance(modelManager);
			edelta.execute();
		} catch (Exception e) {
			throw new IllegalArgumentException("Cannot execute " +
				edeltaClass.getName(), e);
		}
	}

}
