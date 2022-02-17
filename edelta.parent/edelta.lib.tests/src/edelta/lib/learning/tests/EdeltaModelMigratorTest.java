package edelta.lib.learning.tests;

import static edelta.testutils.EdeltaTestUtils.assertFilesAreEquals;
import static edelta.testutils.EdeltaTestUtils.cleanDirectoryAndFirstSubdirectories;
import static java.util.List.of;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.ecore.util.EcoreUtil.Copier;
import org.eclipse.emf.ecore.xmi.XMIResource;
import org.eclipse.emf.ecore.xmi.XMLResource;
import org.eclipse.emf.ecore.xmi.impl.EcoreResourceFactoryImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceImpl;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import edelta.lib.EdeltaResourceUtils;
import edelta.lib.EdeltaUtils;

public class EdeltaModelMigratorTest {

	private static final String TESTDATA = "testdata/";
	private static final String OUTPUT = "output/";
	private static final String EXPECTATIONS = "expectations/";

	EdeltaModelManager originalModelManager;
	EdeltaModelManager evolvingModelManager;

	/**
	 * A candidate for the model migration.
	 * 
	 * @author Lorenzo Bettini
	 *
	 */
	static class EdeltaModelMigrator {

		private EdeltaModelManager originalModelManager;
		private EdeltaModelManager evolvingModelManager;
		private EdeltaModelCopier modelCopier;

		public EdeltaModelMigrator(String basedir, EdeltaModelManager originalModelManager, EdeltaModelManager evolvingModelManager) {
			this.originalModelManager = originalModelManager;
			this.evolvingModelManager = evolvingModelManager;
			this.modelCopier = new EdeltaModelCopier(
					evolvingModelManager.copyEcores(originalModelManager, basedir));
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
		 * @param baseDir
		 */
		public void copyModels(String baseDir) {
			var map = originalModelManager.getModelResourceMap();
			for (var entry : map.entrySet()) {
				var originalResource = (XMIResource) entry.getValue();
				var p = Paths.get(entry.getKey());
				final var fileName = p.getFileName().toString();
				var newResource = evolvingModelManager.createModelResource
					(baseDir + fileName, originalResource);
				var root = originalResource.getContents().get(0);
				var copy = modelCopier.copy(root);
				if (copy != null)
					newResource.getContents().add(copy);
			}
			modelCopier.copyReferences();
		}
	}

	/**
	 * A candidate for the copier used for model migration.
	 * 
	 * @author Lorenzo Bettini
	 *
	 */
	static class EdeltaModelCopier extends Copier {
		private static final long serialVersionUID = 1L;

		private BiMap<EObject, EObject> ecoreCopyMap;

		public EdeltaModelCopier(Map<EObject, EObject> ecoreCopyMap) {
			this.ecoreCopyMap = HashBiMap.create(ecoreCopyMap);
		}

		/**
		 * An object can be explicitly copied after a containment reference
		 * became a non-containment reference, we must first check whether it
		 * has already been copied.
		 */
		@Override
		public EObject copy(EObject eObject) {
			var alreadyCopied = get(eObject);
			if (alreadyCopied != null)
				return alreadyCopied;
			return super.copy(eObject);
		}

		@Override
		protected EClass getTarget(EClass eClass) {
			return getMapped(eClass);
		}

		@Override
		protected EStructuralFeature getTarget(EStructuralFeature eStructuralFeature) {
			return getMapped(eStructuralFeature);
		}

		private <T extends EObject> T getMapped(T o) {
			var value = ecoreCopyMap.get(o);
			@SuppressWarnings("unchecked")
			var mapped = (T) value;
			if (isNotThereAnymore(mapped))
				return null;
			return mapped;
		}

		private boolean isNotThereAnymore(EObject target) {
			return target != null && target.eResource() == null;
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

	private EdeltaModelMigrator setupMigrator(
			String subdir,
			Collection<String> ecoreFiles,
			Collection<String> modelFiles
		) {
		var basedir = TESTDATA + subdir;
		ecoreFiles
			.forEach(fileName -> originalModelManager.loadEcoreFile(basedir + fileName));
		modelFiles
			.forEach(fileName -> originalModelManager.loadModelFile(basedir + fileName));
		var modelMigrator = new EdeltaModelMigrator(basedir, originalModelManager, evolvingModelManager);
		return modelMigrator;
	}

	@Test
	public void testCopyUnchanged() throws IOException {
		var subdir = "unchanged/";
		var modelMigrator = setupMigrator(
			subdir,
			of("My.ecore"),
			of("MyRoot.xmi", "MyClass.xmi")
		);

		copyModelsSaveAndAssertOutputs(
			modelMigrator,
			subdir,
			subdir,
			of("My.ecore"),
			of("MyRoot.xmi", "MyClass.xmi")
		);
	}

	@Test
	public void testCopyUnchangedClassesWithTheSameNameInDifferentPackages() throws IOException {
		var subdir = "classesWithTheSameName/";

		var modelMigrator = setupMigrator(
			subdir,
			of("My1.ecore", "My2.ecore"),
			of("MyRoot1.xmi", "MyClass1.xmi", "MyRoot2.xmi", "MyClass2.xmi")
		);

		copyModelsSaveAndAssertOutputs(
			modelMigrator,
			subdir,
			subdir,
			of("My1.ecore", "My2.ecore"),
			of("MyRoot1.xmi", "MyClass1.xmi", "MyRoot2.xmi", "MyClass2.xmi")
		);
	}

	@Test
	public void testCopyMutualReferencesUnchanged() throws IOException {
		var subdir = "mutualReferencesUnchanged/";

		var modelMigrator = setupMigrator(
			subdir,
			of("PersonForReferences.ecore", "WorkPlaceForReferences.ecore"),
			of("Person1.xmi", "Person2.xmi", "WorkPlace1.xmi")
		);

		copyModelsSaveAndAssertOutputs(
			modelMigrator,
			subdir,
			subdir,
			of("PersonForReferences.ecore", "WorkPlaceForReferences.ecore"),
			of("Person1.xmi", "Person2.xmi", "WorkPlace1.xmi")
		);
	}

	@Test
	public void testRenamedClass() throws IOException {
		var subdir = "unchanged/";

		var modelMigrator = setupMigrator(
			subdir,
			of("My.ecore"),
			of("MyRoot.xmi", "MyClass.xmi")
		);

		// refactoring of Ecore
		evolvingModelManager.getEPackage("mypackage").getEClassifier("MyClass")
			.setName("MyClassRenamed");
		evolvingModelManager.getEPackage("mypackage").getEClassifier("MyRoot")
			.setName("MyRootRenamed");

		copyModelsSaveAndAssertOutputs(
			modelMigrator,
			subdir,
			"renamedClass/",
			of("My.ecore"),
			of("MyRoot.xmi", "MyClass.xmi")
		);
	}

	@Test
	public void testRenamedFeature() throws IOException {
		var subdir = "unchanged/";

		var modelMigrator = setupMigrator(
			subdir,
			of("My.ecore"),
			of("MyRoot.xmi", "MyClass.xmi")
		);

		// refactoring of Ecore
		getFeature(evolvingModelManager, 
				"mypackage", "MyRoot", "myReferences")
			.setName("myReferencesRenamed");
		getFeature(evolvingModelManager, 
				"mypackage", "MyRoot", "myContents")
			.setName("myContentsRenamed");

		copyModelsSaveAndAssertOutputs(
			modelMigrator,
			subdir,
			"renamedFeature/",
			of("My.ecore"),
			of("MyRoot.xmi", "MyClass.xmi")
		);
	}

	@Test
	public void testCopyMutualReferencesRenamed() throws IOException {
		var subdir = "mutualReferencesUnchanged/";

		var modelMigrator = setupMigrator(
			subdir,
			of("PersonForReferences.ecore", "WorkPlaceForReferences.ecore"),
			of("Person1.xmi", "Person2.xmi", "WorkPlace1.xmi")
		);

		// refactoring of Ecore
		getEClass(evolvingModelManager, "personforreferences", "Person")
			.setName("PersonRenamed");

		copyModelsSaveAndAssertOutputs(
			modelMigrator,
			subdir,
			"mutualReferencesRenamed/",
			of("PersonForReferences.ecore", "WorkPlaceForReferences.ecore"),
			of("Person1.xmi", "Person2.xmi", "WorkPlace1.xmi")
		);
	}

	@Test
	public void testCopyMutualReferencesRenamed2() throws IOException {
		var subdir = "mutualReferencesUnchanged/";

		var modelMigrator = setupMigrator(
			subdir,
			of("PersonForReferences.ecore", "WorkPlaceForReferences.ecore"),
			of("Person1.xmi", "Person2.xmi", "WorkPlace1.xmi")
		);

		// refactoring of Ecore
		// rename the feature before...
		getFeature(evolvingModelManager, "personforreferences", "Person", "works")
			.setName("workplace");
		// ...renaming the class
		getEClass(evolvingModelManager, "personforreferences", "Person")
			.setName("PersonRenamed");
		getFeature(evolvingModelManager, "WorkPlaceForReferences", "WorkPlace", "persons")
			.setName("employees");

		copyModelsSaveAndAssertOutputs(
			modelMigrator,
			subdir,
			"mutualReferencesRenamed2/",
			of("PersonForReferences.ecore", "WorkPlaceForReferences.ecore"),
			of("Person1.xmi", "Person2.xmi", "WorkPlace1.xmi")
		);
	}

	@Test
	public void testRemovedContainmentFeature() throws IOException {
		var subdir = "unchanged/";

		var modelMigrator = setupMigrator(
			subdir,
			of("My.ecore"),
			of("MyRoot.xmi", "MyClass.xmi")
		);

		// refactoring of Ecore
		EcoreUtil.remove(getFeature(evolvingModelManager,
				"mypackage", "MyRoot", "myContents"));

		copyModelsSaveAndAssertOutputs(
			modelMigrator,
			subdir,
			"removedContainmentFeature/",
			of("My.ecore"),
			of("MyRoot.xmi", "MyClass.xmi")
		);
	}

	@Test
	public void testRemovedNonContainmentFeature() throws IOException {
		var subdir = "unchanged/";

		var modelMigrator = setupMigrator(
			subdir,
			of("My.ecore"),
			of("MyRoot.xmi", "MyClass.xmi")
		);

		// refactoring of Ecore
		EdeltaUtils.removeElement(getFeature(evolvingModelManager,
				"mypackage", "MyRoot", "myReferences"));

		copyModelsSaveAndAssertOutputs(
			modelMigrator,
			subdir,
			"removedNonContainmentFeature/",
			of("My.ecore"),
			of("MyRoot.xmi", "MyClass.xmi")
		);
	}

	@Test
	public void testRemovedNonReferredClass() throws IOException {
		var subdir = "unchanged/";

		var modelMigrator = setupMigrator(
			subdir,
			of("My.ecore"),
			of("MyRoot.xmi", "MyClass.xmi")
		);

		// refactoring of Ecore
		EdeltaUtils.removeElement(getEClass(evolvingModelManager,
				"mypackage", "MyRoot"));

		copyModelsSaveAndAssertOutputs(
			modelMigrator,
			subdir,
			"removedNonReferredClass/",
			of("My.ecore"),
			of("MyRoot.xmi", "MyClass.xmi")
		);
	}

	@Test
	public void testRemovedReferredClass() throws IOException {
		var subdir = "unchanged/";

		var modelMigrator = setupMigrator(
			subdir,
			of("My.ecore"),
			of("MyRoot.xmi", "MyClass.xmi")
		);

		// refactoring of Ecore
		EdeltaUtils.removeElement(getEClass(evolvingModelManager,
				"mypackage", "MyClass"));

		copyModelsSaveAndAssertOutputs(
			modelMigrator,
			subdir,
			"removedReferredClass/",
			of("My.ecore"),
			of("MyRoot.xmi", "MyClass.xmi")
		);
	}

	private void copyModelsSaveAndAssertOutputs(
			EdeltaModelMigrator modelMigrator,
			String origdir,
			String outputdir,
			Collection<String> ecoreFiles,
			Collection<String> modelFiles
		) throws IOException {
		var basedir = TESTDATA + origdir;
		copyModels(modelMigrator, basedir);
		var output = OUTPUT + outputdir;
		evolvingModelManager.saveEcores(output);
		evolvingModelManager.saveModels(output);
		ecoreFiles.forEach
			(fileName -> assertGeneratedFiles(outputdir, output, fileName));
		modelFiles.forEach
			(fileName -> assertGeneratedFiles(outputdir, output, fileName));
	}

	private EAttribute getAttribute(EdeltaModelManager modelManager, String packageName, String className, String attributeName) {
		return (EAttribute) getFeature(modelManager, packageName, className, attributeName);
	}

	private EReference getReference(EdeltaModelManager modelManager, String packageName, String className, String attributeName) {
		return (EReference) getFeature(modelManager, packageName, className, attributeName);
	}

	private EStructuralFeature getFeature(EdeltaModelManager modelManager, String packageName, String className, String featureName) {
		return getEClass(modelManager, packageName, className)
				.getEStructuralFeature(featureName);
	}

	private EClass getEClass(EdeltaModelManager modelManager, String packageName, String className) {
		return (EClass) modelManager.getEPackage(
				packageName).getEClassifier(className);
	}

	private void assertGeneratedFiles(String subdir, String outputDir, String fileName) {
		try {
			assertFilesAreEquals(
				EXPECTATIONS + subdir + fileName,
				outputDir + fileName);
		} catch (IOException e) {
			e.printStackTrace();
			fail(e.getClass().getName() + ": " + e.getMessage());
		}
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
	 * @param baseDir
	 */
	private void copyModels(EdeltaModelMigrator modelMigrator, String baseDir) {
		modelMigrator.copyModels(baseDir);
	}
}
