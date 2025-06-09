package edelta.lib;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.xmi.XMIResource;

import edelta.lib.EdeltaEngine.EdeltaRuntimeProvider;

/**
 * Handles the migration of model files according to the information configured.
 * 
 * It is meant to scan a path, loading model files, check which ones need to be migrated
 * from a version of metamodels to the next one. It repeats the scan until all model files
 * are migrated to the latest current version of metamodels.
 * 
 * @author Lorenzo Bettini
 */
public class EdeltaVersionMigrator {

	private static final String XMI_EXTENSION = "." + XMIResource.XMI_NS;

	private static final Logger LOG = Logger.getLogger(EdeltaVersionMigrator.class);

	private EdeltaModelManager modelManager = new EdeltaModelManager();

	private static class VersionMigrationEntry {
		private Collection<String> uris;
		private EdeltaEngine engine;

		VersionMigrationEntry(Collection<String> uris, EdeltaEngine engine) {
			this.uris = uris;
			this.engine = engine;
		}
	}

	private List<VersionMigrationEntry> versionMigrations = new ArrayList<>();

	private Set<String> modelExtensions = new HashSet<>();

	public EdeltaVersionMigrator() {
		modelExtensions.add(XMI_EXTENSION);
	}

	/**
	 * Adds the given model file extension to the list of model file extensions.
	 * By default, it loads only ".xmi" files as models.
	 * 
	 * @param modelFileExtension including the "."
	 */
	public void addModelFileExtension(String modelFileExtension) {
		modelExtensions.add(modelFileExtension);
	}

	/**
	 * Adds the given model file extensions to the list of model file extensions.
	 * 
	 * see {@link #addModelFileExtension(String)}
	 * 
	 * @param modelFileExtensions
	 */
	public void addModelFileExtensions(String... modelFileExtensions) {
		for (var modelFileExtension : modelFileExtensions) {
			addModelFileExtension(modelFileExtension);
		}
	}

	/**
	 * Ensure that the nsURI is mapped to the loaded {@link EPackage}, so that
	 * when loading an XMI the referenced Ecore file is found by nsURI.
	 * 
	 * @param resource
	 */
	private void updatePackageRegistry(Resource resource) {
		var ePackage = EdeltaResourceUtils.getEPackage(resource);
		var resourceSet = resource.getResourceSet();
		updatePackageRegistry(ePackage, resourceSet);
	}

	/**
	 * Ensure that the nsURI is mapped to the loaded {@link EPackage}, in the
	 * specified {@link ResourceSet} so that when loading an XMI the referenced
	 * Ecore file is found by nsURI.
	 * 
	 * @param ePackage
	 * @param resourceSet
	 */
	private void updatePackageRegistry(EPackage ePackage, ResourceSet resourceSet) {
		resourceSet.getPackageRegistry()
			.put(ePackage.getNsURI(), ePackage);
	}

	/**
	 * See {@link EdeltaModelManager#loadEcoreFile(String)}.
	 * 
	 * @param ecorePath
	 * @return the loaded {@link Resource} containing the Ecore model
	 * @throws IOException 
	 */
	public Resource loadEcore(String ecorePath) {
		var resource = modelManager.loadEcoreFile(ecorePath);
		updatePackageRegistry(resource);
		return resource;
	}

	/**
	 * See {@link EdeltaModelManager#loadEcoreFile(String, InputStream)}.
	 * 
	 * @param ecoreFile
	 * @param inputStream
	 * @return the loaded {@link Resource} containing the Ecore model
	 * @throws IOException 
	 */
	public Resource loadEcore(String ecoreFile, InputStream inputStream) throws IOException {
		var resource = modelManager.loadEcoreFile(ecoreFile, inputStream);
		updatePackageRegistry(resource);
		return resource;
	}

	/**
	 * Loads an {@link EPackage}, assumed to be properly part of a
	 * {@link ResourceSet}, which represents the current (and latest) version of a
	 * metamodel.
	 * 
	 * See {@link EdeltaModelManager#loadEPackage(EPackage)}.
	 * 
	 * @param ePackage
	 */
	public void loadCurrentEPackage(EPackage ePackage) {
		modelManager.loadEPackage(ePackage);
		// since the EPackage might be in a different ResourceSet than our modelManager's one
		// we must ensure the registration is performed in the modelManager's ResourceSet
		updatePackageRegistry(ePackage, modelManager.getResourceSet());
	}

	/**
	 * Loads all the {@link EPackage}s, assumed to be properly part of a
	 * {@link ResourceSet}, which represent the current (and latest) version of
	 * metamodels.
	 * 
	 * See {@link EdeltaModelManager#loadEPackage(EPackage)}.
	 * 
	 * @param ePackages
	 */
	public void loadCurrentEPackages(EPackage... ePackages) {
		for (var ePackage : ePackages) {
			loadCurrentEPackage(ePackage);
		}
	}

	/**
	 * Load all models in the given path (possibly recursively in subdirectories),
	 * using the configured file extensions, by default ".xmi" files (see
	 * {@link #addModelFileExtension(String)}).
	 * 
	 * @param path the path to scan for model files, e.g., "path/to/models"
	 * @throws IOException
	 */
	public void loadModelsFrom(String path) throws IOException {
		try (var stream = Files.walk(Paths.get(path))) {
			stream
				.filter(file -> !Files.isDirectory(file))
				.filter(file -> {
					var fileToString = file.toString();
					return modelExtensions.stream().anyMatch(fileToString::endsWith);
				})
				.forEach(file -> loadModel(file.toString()));
		}
	}

	/**
	 * Load all models in the given paths (possibly recursively in subdirectories),
	 * using the configured file extensions, by default ".xmi" files (see
	 * {@link #addModelFileExtension(String)}).
	 * 
	 * @param paths one or more paths to scan for model files
	 * @throws IOException
	 */
	public void loadModelsFromPaths(String... paths) throws IOException {
		for (var path : paths) {
			loadModelsFrom(path);
		}
	}

	/**
	 * Loads the model file at the given path, assuming it is a valid model file
	 * (i.e., differently from {@link #loadCurrentEPackage(EPackage)}, it does not
	 * check the file extension).
	 * 
	 * @param path the path to the model file, e.g., "path/to/model.xmi"
	 * @return the loaded {@link Resource} containing the model
	 */
	public Resource loadModel(String path) {
		return modelManager.loadModelFile(path);
	}

	public void mapVersionMigration(Collection<String> uris, EdeltaEngine edeltaEngine) {
		versionMigrations.add(new VersionMigrationEntry(uris, edeltaEngine));
	}

	/**
	 * Registers an {@link EdeltaRuntimeProvider} by recording the nsURIs handled by the
	 * corresponding {@link EdeltaRuntime} implementation and by loading the corresponding
	 * Ecore files from the classpath, e.g., with {@link Class#getResourceAsStream(String)}.
	 * 
	 * @param provider
	 * @throws IOException
	 */
	public void registerMigration(EdeltaRuntimeProvider provider) throws IOException {
		var tempRuntime = provider.apply(new EdeltaDefaultRuntime(modelManager));
		mapVersionMigration(tempRuntime.getMigratedNsURIs(), new EdeltaEngine(provider));
		var ecorePaths = tempRuntime.getMigratedEcorePaths();
		for (var ecorePath : ecorePaths) {
			loadEcore(ecorePath, tempRuntime.getClass().getResourceAsStream(ecorePath));
		}
	}

	/**
	 * Executes all the needed model migrations, saving the model files in-place.
	 * @return the migrated model resources
	 * 
	 * @throws Exception
	 */
	public Collection<Resource> execute() throws Exception {
		class MigrationData {
			private Set<EPackage> ecores;
			private Collection<Resource> models;

			MigrationData(Set<EPackage> ecores, Collection<Resource> models) {
				this.ecores = ecores;
				this.models = models;
			}
		}

		var migrationDatas = new HashMap<VersionMigrationEntry, MigrationData>();
		var collectedMigratedResources = new ArrayList<Resource>();

		do {
			migrationDatas.clear();
			for (var resource : modelManager.getModelResources()) {
				var contents = resource.getContents();
				var ePackage = contents.get(0).eClass().getEPackage();
				versionMigrations.stream()
					.filter(versionMigration -> versionMigration.uris.contains(ePackage.getNsURI()))
					.forEach(versionMigration -> {
						var data = migrationDatas
							.computeIfAbsent(versionMigration,
								x -> new MigrationData(new HashSet<>(), new ArrayList<>()));
						data.ecores.add(ePackage);
						data.models.add(resource);
					});
			}
			Collection<Resource> migratedModelResources = new ArrayList<>();
			for (var entry : migrationDatas.entrySet()) {
				var toApply = entry.getKey().engine;
				var ecoresToMigrate = entry.getValue().ecores;
				var modelsToMigrate = entry.getValue().models;

				var nsURIs = ecoresToMigrate.stream().map(e -> e.getNsURI()).toList();
				var modelPaths = modelsToMigrate.stream()
						.map(this::resourceToURIString)
						.collect(Collectors.toSet());
				nsURIs.forEach(e -> LOG.info("Stale Ecore nsURI: " + e));
				modelPaths.forEach(e -> LOG.info("Model requiring migration: " + e));

				// We assume EPackages have been loaded and registered by name and nsURI
				toApply.setOriginalModelManager(modelManager);
				toApply.execute();

				// note that we never save the evolved ecores: only the models
				// the ecores are meant to be part of the application code, not of the client project
				var evolvedModels = toApply.getEvolvingModelManager().getModelResources().stream()
						.filter(r -> modelPaths.contains(resourceToURIString(r)))
						.toList();
				saveInPlace(evolvedModels);
				migratedModelResources.addAll(evolvedModels);
			}
			if (!migratedModelResources.isEmpty()) {
				// reload only migrated models
				modelManager.clearModels();
				for (var model : migratedModelResources) {
					var resourceToURIString = resourceToURIString(model);
					loadModelsFrom(resourceToURIString);
				}
				// it is important to save after all the migrations took place
				// this way, the models refer to the most up-to-date Ecore loaded
				// from the file system, and possible relative xsi:schemaLocation
				// point to the most up-to-date Ecore
				// REMEMBER: in this method, evolved Ecore files are NEVER saved to disk:
				// they are used in memory only to migrate models
				// Ecore files are meant to be in the Application code, not in the client
				saveInPlace(modelManager.getModelResources());
				// collect the migrated resources taken from the model manager
				// that correspond to the saved models
				collectedMigratedResources.addAll(modelManager.getModelResources());
			}
		} while (!migrationDatas.isEmpty());

		return collectedMigratedResources.stream()
				.collect(
					Collectors.toMap(Resource::getURI, // key mapper: use URI as key
						r -> r, // value mapper: resource itself
						(r1, r2) -> r2, // merge function: keep latest in case of duplicates
						LinkedHashMap::new // preserve order if needed
				)).values();
	}

	private String resourceToURIString(Resource resource) {
		return resource.getURI().path();
	}

	private void saveInPlace(Collection<Resource> resources) throws IOException {
		for (var resource : resources) {
			LOG.info("Saving: " + resource.getURI());
			resource.save(null);
		}
	}

}
