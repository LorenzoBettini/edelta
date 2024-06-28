package edelta.lib;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;

/**
 * @author Lorenzo Bettini
 */
public class EdeltaVersionMigrator {

	private static final Logger LOG = Logger.getLogger(EdeltaVersionMigrator.class);

	private EdeltaModelManager modelManager = new EdeltaModelManager();

	private static record VersionMigrationEntry(Collection<String> uris, EdeltaEngine engine) {
		
	}

	private List<VersionMigrationEntry> versionMigrations = new ArrayList<>();

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
		versionMigrations.add(new VersionMigrationEntry(uris, edeltaEngine));
	}

	public void execute(String outputPath) throws Exception {
		record MigrationData(Set<EPackage> ecores, Collection<Resource> models) {
		}

		var modelResources = modelManager.getModelResources();
		var migrationDatas = new HashMap<VersionMigrationEntry, MigrationData>();
		do {
			// at each iteration we must always use the already in-memory loaded resources
			// to make sure that each migration from versionx to versiony always use the
			// most up-to-date version of ecores and models.
			migrationDatas.clear();
			for (var resource : modelResources) {
				var contents = resource.getContents();
				var ePackage = contents.get(0).eClass().getEPackage();
				for (var versionMigration : versionMigrations) {
					if (versionMigration.uris.contains(ePackage.getNsURI())) {
						var data = migrationDatas
							.computeIfAbsent(versionMigration,
								x -> new MigrationData(new HashSet<>(), new ArrayList<>()));
						data.ecores.add(ePackage);
						data.models.add(resource);
					}
				}
			}
			modelResources.clear();
			for (var entry : migrationDatas.entrySet()) {
				var toApply = entry.getKey().engine;
				var ecoresToMigrate = entry.getValue().ecores;
				var modelsToMigrate = entry.getValue().models;
				var migrationModelManager = new EdeltaModelManager();
				
				var nsURIs = ecoresToMigrate.stream().map(e -> e.getNsURI()).toList();
				var modelPaths = modelsToMigrate.stream().map(e -> e.getURI().path()).toList();
				nsURIs.forEach(e -> LOG.info("Stale Ecore nsURI: " + e));
				modelPaths.forEach(e -> LOG.info("Model requiring migration: " + e));
				for (var ecore : ecoresToMigrate) {
					migrationModelManager.addEcoreResource(ecore.eResource());
				}
				for (var model : modelsToMigrate) {
					migrationModelManager.addModelResource(model);
				}
				toApply.setOriginalModelManager(migrationModelManager);
				toApply.execute();
				// note that we never save the evolved ecores: only the models
				// the ecores are meant to be part of the application code, not of the client project
				toApply.saveModels(outputPath);
				modelResources.addAll(toApply.getEvolvingModelManager().getModelResources());
			}
		} while (!migrationDatas.isEmpty());
	}

}
