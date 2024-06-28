package edelta.lib;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.xtext.xbase.lib.Pair;

/**
 * @author Lorenzo Bettini
 */
public class EdeltaVersionMigrator {

	private EdeltaModelManager modelManager = new EdeltaModelManager();

	private List<Pair<Collection<String>, EdeltaEngine>> versionMigrations = new ArrayList<>();

	/**
	 * The loaded ecores are assumed to be different versions of the same ecores.
	 * 
	 * @param path
	 * @throws IOException
	 */
	public void loadEcoresFrom(String path) throws IOException {
		try (var stream = Files.walk(Paths.get(path))) {
			stream
				.filter(file -> !Files.isDirectory(file))
				.filter(file -> file.toString().endsWith(".ecore"))
				.forEach(file -> modelManager.loadEcoreFile(file.toString()));
		}
	}

	/**
	 * The loaded models are assumed to belong to the same version of ecores.
	 * 
	 * @param path
	 * @throws IOException
	 */
	public void loadModelsFrom(String path) throws IOException {
		try (var stream = Files.walk(Paths.get(path))) {
			stream
				.filter(file -> !Files.isDirectory(file))
				// TODO: allow for custom model file extensions
				.filter(file -> file.toString().endsWith(".xmi"))
				.forEach(file -> modelManager.loadModelFile(file.toString()));
		}
	}

	public void mapVersionMigration(Collection<String> uris, EdeltaEngine edeltaEngine) {
		versionMigrations.add(Pair.of(uris, edeltaEngine));
	}

	public void execute(String outputPath) throws Exception {
		var modelResources = modelManager.getModelResources();
		var currentVersionsOfEcores = new HashSet<Resource>();
		var modelsToMigrate = new ArrayList<Resource>();
		// TODO consider clusters of unrelated ecores
		EdeltaEngine toApply = null;
		for (var resource : modelResources) {
			var contents = resource.getContents();
			if (contents.isEmpty())
				continue;
			var ePackage = contents.get(0).eClass().getEPackage();
			for (var versionMigration : versionMigrations) {
				if (versionMigration.getKey().contains(ePackage.getNsURI())) {
					currentVersionsOfEcores.add(ePackage.eResource());
					modelsToMigrate.add(resource);
					toApply = versionMigration.getValue();
				}
			}
		}
		if (toApply != null) {
			for (var ecore : currentVersionsOfEcores)
				toApply.loadEcoreFile(ecore.getURI().path());
			for (var model : modelsToMigrate)
				toApply.loadModelFile(model.getURI().path());
			toApply.execute();
			toApply.saveModels(outputPath);
		}
	}

}
