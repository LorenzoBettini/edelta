package edelta.lib.learning.tests;

import static edelta.testutils.EdeltaTestUtils.assertFilesAreEquals;
import static edelta.testutils.EdeltaTestUtils.cleanDirectoryAndFirstSubdirectories;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.util.EcoreUtil.Copier;
import org.eclipse.emf.ecore.xmi.XMIResource;
import org.eclipse.emf.ecore.xmi.XMLResource;
import org.eclipse.emf.ecore.xmi.impl.EcoreResourceFactoryImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceImpl;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import edelta.lib.EdeltaResourceUtils;

public class EdeltaModelMigratorTest {

	private static final String ORIGINAL = "original/";
	private static final String TESTDATA = "testdata/";
	private static final String OUTPUT = "output/";
	private static final String EXPECTATIONS = "expectations/";

	EdeltaModelManager originalModelManager;
	EdeltaModelManager evolvingModelManager;

	/**
	 * A candidate for the copier used for model migration.
	 * 
	 * @author Lorenzo Bettini
	 *
	 */
	static class EdeltaModelMigrator extends Copier {
		private static final long serialVersionUID = 1L;

		private Map<EObject, EObject> ecoreCopyMap;

		public EdeltaModelMigrator(Map<EObject, EObject> ecoreCopyMap) {
			this.ecoreCopyMap = ecoreCopyMap;
		}

		@Override
		protected EClass getTarget(EClass eClass) {
			return (EClass) ecoreCopyMap.get(eClass);
		}

		@Override
		protected EStructuralFeature getTarget(EStructuralFeature eStructuralFeature) {
			return (EStructuralFeature) ecoreCopyMap.get(eStructuralFeature);
		}
	}

	/**
	 * A candidate for the model manager (for Ecores and models).
	 * 
	 * @author Lorenzo Bettini
	 *
	 */
	static class EdeltaModelManager {

		private static final Logger LOG = Logger.getLogger(EdeltaModelManager.class);

		/**
		 * Here we store the association between the Ecore path and the
		 * corresponding loaded Resource.
		 */
		private Map<String, Resource> ecoreResourceMap = new LinkedHashMap<>();

		/**
		 * Here we store the association between the model path and the
		 * corresponding loaded Resource.
		 */
		private Map<String, Resource> modelResourceMap = new LinkedHashMap<>();

		/**
		 * Here we store all the Ecores and models used by the Edelta
		 */
		private ResourceSet resourceSet = new ResourceSetImpl();

		/**
		 * Performs EMF initialization (resource factories and package registry)
		 */
		public EdeltaModelManager() {
			// Register the appropriate resource factory to handle all file extensions.
			resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put
				("ecore", 
				new EcoreResourceFactoryImpl());
			resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put
				(Resource.Factory.Registry.DEFAULT_EXTENSION, 
				new XMIResourceFactoryImpl() {
					@Override
					public Resource createResource(URI uri) {
						var resource = new XMIResourceImpl(uri);
						// this simulates the behavior of the
						// "Sample Reflective Ecore Model Editor" when saving
						resource.getDefaultSaveOptions().put(XMLResource.OPTION_LINE_WIDTH, 10);
						return resource;
					}
				});

			// Register the Ecore package to ensure it is available during loading.
			resourceSet.getPackageRegistry().put
				(EcorePackage.eNS_URI, 
					 EcorePackage.eINSTANCE);
		}

		/**
		 * Loads the ecore file specified in the path
		 * @param path
		 * @return the loaded {@link Resource}
		 */
		public Resource loadEcoreFile(String path) {
			return loadResource(path, ecoreResourceMap);
		}

		private Resource loadResource(String path, Map<String, Resource> resourceMap) {
			var uri = createAbsoluteFileURI(path);
			// Demand load resource for this file.
			LOG.info("Loading " + path + " (URI: " + uri + ")");
			var resource = resourceSet.getResource(uri, true);
			resourceMap.put(path, resource);
			return resource;
		}

		/**
		 * Tries to load the package name from the previously loaded ecores.
		 * 
		 * @param packageName
		 * @return the found {@link EPackage} or null
		 */
		public EPackage getEPackage(String packageName) {
			// Ecore package is implicitly available
			if (EcorePackage.eNAME.equals(packageName)) {
				return EcorePackage.eINSTANCE;
			}
			return EdeltaResourceUtils.getEPackages(ecoreResourceMap.values())
				.stream()
				.filter(p -> p.getName().equals(packageName))
				.findFirst()
				.orElse(null);
		}

		/**
		 * Saves the modified EPackages as Ecore files in the specified
		 * output path.
		 * 
		 * The final path of the modified Ecore files is made of the
		 * specified outputPath and the original loaded Ecore
		 * file names.
		 * 
		 * @param outputPath
		 * @throws IOException 
		 */
		public void saveEcores(String outputPath) throws IOException {
			saveResources(outputPath, ecoreResourceMap);
		}

		private void saveResources(String outputPath, Map<String, Resource> resourceMap) throws IOException {
			for (Entry<String, Resource> entry : resourceMap.entrySet()) {
				var p = Paths.get(entry.getKey());
				final var fileName = p.getFileName().toString();
				LOG.info("Saving " + outputPath + "/" + fileName);
				var newFile = new File(outputPath, fileName);
				newFile.getParentFile().mkdirs();
				var fos = new FileOutputStream(newFile);
				entry.getValue().save(fos, null);
				fos.flush();
				fos.close();
			}
		}

		/**
		 * Loads the XMI model file specified in the path
		 * @param path
		 * @return the loaded {@link Resource}
		 */
		public Resource loadModelFile(String path) {
			return loadResource(path, modelResourceMap);
		}

		/**
		 * Saves the modified models into files in the specified
		 * output path.
		 * 
		 * The final path of the modified files is made of the
		 * specified outputPath and the original loaded model
		 * file names.
		 * 
		 * @param outputPath
		 * @throws IOException 
		 */
		public void saveModels(String outputPath) throws IOException {
			saveResources(outputPath, modelResourceMap);
		}

		/**
		 * Create a new {@link XMIResource} for an Ecore in the {@link ResourceSet},
		 * using the passed resource as a "prototype": it takes all its options and
		 * encoding.
		 * 
		 * @param path
		 * @param prototypeResource
		 * @return
		 */
		public Resource createEcoreResource(String path, XMIResource prototypeResource) {
			return createResource(path, prototypeResource, ecoreResourceMap);
		}

		/**
		 * Create a new {@link XMIResource} in the {@link ResourceSet},
		 * using the passed resource as a "prototype": it takes all its options and
		 * encoding.
		 * 
		 * @param path
		 * @param prototypeResource
		 * @return
		 */
		public Resource createModelResource(String path, XMIResource prototypeResource) {
			return createResource(path, prototypeResource, modelResourceMap);
		}

		/**
		 * Create a new {@link XMIResource} in the {@link ResourceSet}, using the passed
		 * resource as a "prototype": it takes all its options and encoding.
		 * 
		 * @param path
		 * @param prototypeResource
		 * @param resourceMap 
		 * @return
		 */
		private Resource createResource(String path, XMIResource prototypeResource, Map<String, Resource> resourceMap) {
			var uri = createAbsoluteFileURI(path);
			LOG.info("Creating " + path + " (URI: " + uri + ")");
			var resource = (XMIResource) resourceSet.createResource(uri);
			resource.getDefaultLoadOptions().putAll(prototypeResource.getDefaultLoadOptions());
			resource.getDefaultSaveOptions().putAll(prototypeResource.getDefaultSaveOptions());
			resource.setEncoding(prototypeResource.getEncoding());
			resourceMap.put(path, resource);
			return resource;
		}

		/**
		 * make sure we have a complete file URI, otherwise the saved modified files
		 * will contain wrong references (i.e., with the prefixed relative path)
		 * 
		 * @param path
		 * @return
		 */
		private URI createAbsoluteFileURI(String path) {
			return URI.createFileURI(Paths.get(path).toAbsolutePath().toString());
		}

		public Map<String, Resource> getModelResourceMap() {
			return modelResourceMap;
		}

		public Map<String, Resource> getEcoreResourceMap() {
			return ecoreResourceMap;
		}

		public Map<EObject, EObject> copyEcores(EdeltaModelManager otherModelManager, String basedir) {
			var ecoreResourceMap = otherModelManager.getEcoreResourceMap();
			var ecoreCopier = new Copier();
			for (var entry : ecoreResourceMap.entrySet()) {
				var originalResource = (XMIResource) entry.getValue();
				var p = Paths.get(entry.getKey());
				final var fileName = p.getFileName().toString();
				var newResource = this.createEcoreResource
					(basedir + fileName, originalResource);
				var root = originalResource.getContents().get(0);
				newResource.getContents().add(ecoreCopier.copy(root));
			}
			ecoreCopier.copyReferences();
			return ecoreCopier;
		}
	}

	@BeforeClass
	public static void clearOutput() throws IOException {
		cleanDirectoryAndFirstSubdirectories(OUTPUT);
	}

	@Before
	public void setup() {
		originalModelManager = new EdeltaModelManager();
		evolvingModelManager = new EdeltaModelManager();
	}

	@Test
	public void testCopyUnchanged() throws IOException {
		var subdir = "unchanged/";
		var basedir = TESTDATA + subdir;
		originalModelManager.loadEcoreFile(basedir + "My.ecore");
		originalModelManager.loadModelFile(basedir + "MyRoot.xmi");
		originalModelManager.loadModelFile(basedir + "MyClass.xmi");

		var modelMigrator = new EdeltaModelMigrator(evolvingModelManager.copyEcores(originalModelManager, basedir));
		copyModels(modelMigrator, basedir);

		var output = OUTPUT + subdir;
		evolvingModelManager.saveEcores(output);
		evolvingModelManager.saveModels(output);
		assertGeneratedFiles(subdir, output, "MyRoot.xmi");
		assertGeneratedFiles(subdir, output, "MyClass.xmi");
		assertGeneratedFiles(subdir, output, "My.ecore");
	}

	@Test
	public void testManualRenaming() throws IOException {
		var subdir = "renamedClass/";
		var basedir = TESTDATA + subdir;
		originalModelManager.loadEcoreFile(basedir + ORIGINAL + "My.ecore");
		originalModelManager.loadModelFile(basedir + ORIGINAL + "MyRoot.xmi");
		originalModelManager.loadModelFile(basedir + ORIGINAL+ "MyClass.xmi");

		var modelMigrator = new EdeltaModelMigrator(evolvingModelManager.copyEcores(originalModelManager, basedir));

		// refactoring of Ecore
		evolvingModelManager.getEPackage("mypackage").getEClassifier("MyClass")
			.setName("MyClassRenamed");
		evolvingModelManager.getEPackage("mypackage").getEClassifier("MyRoot")
			.setName("MyRootRenamed");

		// migration of models
		copyModels(modelMigrator, basedir);

		var output = OUTPUT + subdir;
		evolvingModelManager.saveEcores(output);
		evolvingModelManager.saveModels(output);
		assertGeneratedFiles(subdir, output, "MyRoot.xmi");
		assertGeneratedFiles(subdir, output, "MyClass.xmi");
		assertGeneratedFiles(subdir, output, "My.ecore");
	}

	private void assertGeneratedFiles(String subdir, String outputDir, String fileName) throws IOException {
		assertFilesAreEquals(
			EXPECTATIONS + subdir + fileName,
			outputDir + fileName);
	}

	/**
	 * This simulates what the final model migration should do.
	 * 
	 * IMPORTANT: the original Ecores and models must be in a subdirectory
	 * of the directory that stores the modified Ecores.
	 * 
	 * It is crucial to strip the original path and use the baseDir
	 * to create the new {@link Resource} URI, so that, upon saving,
	 * the schema location is computed correctly.
	 * 
	 * @param modelMigrator
	 * @param baseDir
	 */
	private void copyModels(EdeltaModelMigrator modelMigrator, String baseDir) {
		var map = originalModelManager.getModelResourceMap();
		for (var entry : map.entrySet()) {
			var originalResource = (XMIResource) entry.getValue();
			var p = Paths.get(entry.getKey());
			final var fileName = p.getFileName().toString();
			var newResource = evolvingModelManager.createModelResource
				(baseDir + fileName, originalResource);
			var root = originalResource.getContents().get(0);
			newResource.getContents().add(modelMigrator.copy(root));
		}
		modelMigrator.copyReferences();
	}
}
