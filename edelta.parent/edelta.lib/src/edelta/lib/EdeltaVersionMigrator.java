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
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.xmi.XMIResource;

/**
 * @author Lorenzo Bettini
 */
public class EdeltaVersionMigrator {

	private static final String XMI_EXTENSION = "." + XMIResource.XMI_NS;

	private static final Logger LOG = Logger.getLogger(EdeltaVersionMigrator.class);

	private EdeltaModelManager modelManager = new EdeltaModelManager();

	private static record VersionMigrationEntry(Collection<String> uris, EdeltaEngine engine) {
		
	}

	private List<VersionMigrationEntry> versionMigrations = new ArrayList<>();

	private Set<String> ecorePaths = new HashSet<>();

	private Set<String> modelExtensions = new HashSet<>();

	public EdeltaVersionMigrator() {
		modelExtensions.add(XMI_EXTENSION);
	}

	/**
	 * By default, it loads only ".xmi" files as models.
	 * 
	 * @param modelFileExtension
	 */
	public void addModelFileExtension(String modelFileExtension) {
		modelExtensions.add(modelFileExtension);
	}

	/**
	 * The loaded ecores are assumed to be different versions of the same ecores.
	 * 
	 * @param path
	 * @throws IOException
	 */
	public void loadEcoresFrom(String path) throws IOException {
		ecorePaths.add(path);
		loadEcoreFilesInternal(path);
	}

	private void loadEcoreFilesInternal(String path) throws IOException {
		try (var stream = Files.walk(Paths.get(path))) {
			stream
				.filter(file -> !Files.isDirectory(file))
				.filter(file -> file.toString().endsWith(".ecore"))
				.forEach(file -> {
					var resource = modelManager.loadEcoreFile(file.toString());
					var ePackage = EdeltaResourceUtils.getEPackage(resource);
					resource.getResourceSet().getPackageRegistry()
						.put(ePackage.getNsURI(), ePackage);
				});
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
				.filter(file -> {
					var fileToString = file.toString();
					return modelExtensions.stream().anyMatch(ext -> fileToString.endsWith(ext));
				})
				.forEach(file -> modelManager.loadModelFile(file.toString()));
		}
	}

	public void mapVersionMigration(Collection<String> uris, EdeltaEngine edeltaEngine) {
		versionMigrations.add(new VersionMigrationEntry(uris, edeltaEngine));
	}

	public void execute(String outputPath) throws Exception {
		record MigrationData(Set<EPackage> ecores, Collection<Resource> models) {
		}

		var migrationDatas = new HashMap<VersionMigrationEntry, MigrationData>();
		do {
			migrationDatas.clear();
			for (var resource : modelManager.getModelResources()) {
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
			Collection<Resource> migratedModelResources = new ArrayList<>();
			for (var entry : migrationDatas.entrySet()) {
				var toApply = entry.getKey().engine;
				var ecoresToMigrate = entry.getValue().ecores;
				var modelsToMigrate = entry.getValue().models;

				var nsURIs = ecoresToMigrate.stream().map(e -> e.getNsURI()).toList();
				var modelPaths = modelsToMigrate.stream()
						.map(e -> e.getURI().path())
						.collect(Collectors.toSet());
				nsURIs.forEach(e -> LOG.info("Stale Ecore nsURI: " + e));
				modelPaths.forEach(e -> LOG.info("Model requiring migration: " + e));

				// We assume EPackages have been loaded and registered by name and nsURI
				toApply.setOriginalModelManager(modelManager);
				toApply.execute();

				// note that we never save the evolved ecores: only the models
				// the ecores are meant to be part of the application code, not of the client project
				var evolvedModels = toApply.getEvolvingModelManager().getModelResources().stream()
						.filter(r -> modelPaths.contains(r.getURI().path()))
						.toList();
				for (var resource : evolvedModels) {
					LOG.info("Saving: " + resource.getURI());
					resource.save(null);
				}
				migratedModelResources.addAll(evolvedModels);
			}
			if (!migratedModelResources.isEmpty()) {
				// reload all Ecore files...
				modelManager = new EdeltaModelManager();
				for (var ecorePath : ecorePaths) {
					loadEcoreFilesInternal(ecorePath);
				}
				// ... and only migrated models
				for (var model : migratedModelResources) {
					loadModelsFrom(model.getURI().path());
				}
				// it is important to save after all the migrations took place
				// this way, the models refer to the most up-to-date Ecore loaded
				// from the file system, and possible relative xsi:schemaLocation
				// point to the most up-to-date Ecore
				// REMEMBER: in this method, evolved Ecore files are NEVER saved to disk:
				// they are used in memory only to migrate models
				// Ecore files are meant to be in the Application code, not in the client
				for (var resource : modelManager.getModelResources()) {
					LOG.info("Saving: " + resource.getURI());
					resource.save(null);
				}
			}
		} while (!migrationDatas.isEmpty());
	}

}
