package edelta.lib;

import java.io.IOException;
import java.io.InputStream;
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
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.xmi.XMIResource;

import edelta.lib.EdeltaEngine.EdeltaRuntimeProvider;

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

	private Set<String> modelExtensions = new HashSet<>();

	public EdeltaVersionMigrator() {
		modelExtensions.add(XMI_EXTENSION);
	}

	/**
	 * By default, it loads only ".xmi" files as models.
	 * 
	 * @param modelFileExtension including the "."
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
		try (var stream = Files.walk(Paths.get(path))) {
			stream
				.filter(file -> !Files.isDirectory(file))
				.filter(file -> file.toString().endsWith(".ecore"))
				.forEach(file -> {
					var resource = modelManager.loadEcoreFile(file.toString());
					updatePackageRegistry(resource);
				});
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
	 * Ensure that the nsURI is mapped to the loaded {@link EPackage}, so that when
	 * loading an XMI the referenced Ecore file is found by nsURI.
	 * 
	 * @param ePackage
	 * @param resourceSet
	 */
	private void updatePackageRegistry(EPackage ePackage, ResourceSet resourceSet) {
		resourceSet.getPackageRegistry()
			.put(ePackage.getNsURI(), ePackage);
	}

	/**
	 * See {@link EdeltaModelManager#loadEcoreFile(String, InputStream)}.
	 * 
	 * @param ecoreFile
	 * @param inputStream
	 * @throws IOException 
	 */
	public void loadEcore(String ecoreFile, InputStream inputStream) throws IOException {
		var resource = modelManager.loadEcoreFile(ecoreFile, inputStream);
		updatePackageRegistry(resource);
	}

	/**
	 * See {@link EdeltaModelManager#loadEPackage(EPackage)}.
	 * 
	 * @param ePackage 
	 */
	public void loadEPackage(EPackage ePackage) {
		modelManager.loadEPackage(ePackage);
		// since the EPackage might be in a different ResourceSet than our modelManager's one
		// we must ensure the registration is performed in the modelManager's ResourceSet
		updatePackageRegistry(ePackage, modelManager.getResourceSet());
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
					return modelExtensions.stream().anyMatch(fileToString::endsWith);
				})
				.forEach(file -> loadModel(file.toString()));
		}
	}

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
	 * 
	 * @throws Exception
	 */
	public void execute() throws Exception {
		record MigrationData(Set<EPackage> ecores, Collection<Resource> models) {
		}

		var migrationDatas = new HashMap<VersionMigrationEntry, MigrationData>();
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
					loadModelsFrom(resourceToURIString(model));
				}
				// it is important to save after all the migrations took place
				// this way, the models refer to the most up-to-date Ecore loaded
				// from the file system, and possible relative xsi:schemaLocation
				// point to the most up-to-date Ecore
				// REMEMBER: in this method, evolved Ecore files are NEVER saved to disk:
				// they are used in memory only to migrate models
				// Ecore files are meant to be in the Application code, not in the client
				saveInPlace(modelManager.getModelResources());
			}
		} while (!migrationDatas.isEmpty());
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
