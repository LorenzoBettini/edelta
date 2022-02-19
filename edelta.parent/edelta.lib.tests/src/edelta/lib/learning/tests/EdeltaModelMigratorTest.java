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
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.assertj.core.api.Assertions;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.ENamedElement;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.EStructuralFeature.Setting;
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
import org.eclipse.xtext.xbase.lib.Functions.Function3;
import org.eclipse.xtext.xbase.lib.Procedures.Procedure3;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import edelta.lib.EdeltaEcoreUtil;
import edelta.lib.EdeltaResourceUtils;
import edelta.lib.EdeltaUtils;

public class EdeltaModelMigratorTest {

	private static final String TESTDATA = "testdata/";
	private static final String OUTPUT = "output/";
	private static final String EXPECTATIONS = "expectations/";

	EdeltaModelManager evolvingModelManager;

	/**
	 * A candidate for the model migration.
	 * 
	 * @author Lorenzo Bettini
	 *
	 */
	static class EdeltaModelMigrator {

		private String basedir;
		private EdeltaModelManager originalModelManager;
		private EdeltaModelManager evolvingModelManager;
		private Map<EObject, EObject> mapOfCopiedEcores;
		private EdeltaModelCopier modelCopier;

		public static interface CopyProcedure extends Procedure3<EStructuralFeature, EObject, EObject> {
			
		}

		public static interface AttributeTransformer extends Function3<EAttribute, EObject, Object, Object> {
			
		}

		public static interface AttributeValueTransformer extends Function<Object, Object> {
			
		}

		public EdeltaModelMigrator(String basedir, EdeltaModelManager originalModelManager, EdeltaModelManager evolvingModelManager) {
			this.basedir = basedir;
			this.originalModelManager = originalModelManager;
			this.evolvingModelManager = evolvingModelManager;
			this.mapOfCopiedEcores = evolvingModelManager.copyEcores(originalModelManager, basedir);
			this.modelCopier = new EdeltaModelCopier(
					mapOfCopiedEcores);
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
			copyModels(modelCopier, baseDir, originalModelManager, evolvingModelManager);
		}

		private void copyModels(EdeltaModelCopier edeltaModelCopier, String baseDir, EdeltaModelManager from, EdeltaModelManager into) {
			var map = from.getModelResourceMap();
			for (var entry : map.entrySet()) {
				var originalResource = (XMIResource) entry.getValue();
				var p = Paths.get(entry.getKey());
				final var fileName = p.getFileName().toString();
				var newResource = into.createModelResource
					(baseDir + fileName, originalResource);
				var root = originalResource.getContents().get(0);
				var copy = edeltaModelCopier.copy(root);
				if (copy != null)
					newResource.getContents().add(copy);
			}
			edeltaModelCopier.copyReferences();
		}

		public void transformAttributeValueRule(Predicate<EAttribute> predicate, AttributeValueTransformer function) {
			var customCopier = new EdeltaModelCopier(mapOfCopiedEcores) {
				private static final long serialVersionUID = 1L;

				@Override
				protected void copyAttributeValue(EAttribute eAttribute, EObject eObject, Object value, Setting setting) {
					if (predicate.test(eAttribute))
						value = function.apply(value);
					super.copyAttributeValue(eAttribute, eObject, value, setting);
				};
			};
			copyModels(customCopier, basedir, originalModelManager, evolvingModelManager);
			updateMigrationContext();
		}

		public void transformAttributeValueRule(Predicate<EAttribute> predicate,
				AttributeTransformer function) {
			var customCopier = new EdeltaModelCopier(mapOfCopiedEcores) {
				private static final long serialVersionUID = 1L;

				@Override
				protected void copyAttributeValue(EAttribute eAttribute, EObject eObject, Object value, Setting setting) {
					if (predicate.test(eAttribute)) {
						value = function.apply(eAttribute, eObject, value);
					}
					super.copyAttributeValue(eAttribute, eObject, value, setting);
				};
			};
			copyModels(customCopier, basedir, originalModelManager, evolvingModelManager);
			updateMigrationContext();
		}

		public void copyRule(Predicate<EStructuralFeature> predicate, CopyProcedure procedure) {
			var customCopier = new EdeltaModelCopier(mapOfCopiedEcores) {
				private static final long serialVersionUID = 1L;

				@Override
				protected void copyContainment(EReference eReference, EObject eObject, EObject copyEObject) {
					applyCopyRuleOrElse(eReference, eObject, copyEObject,
						() -> super.copyContainment(eReference, eObject, copyEObject));
				}

				@Override
				protected void copyAttribute(EAttribute eAttribute, EObject eObject, EObject copyEObject) {
					applyCopyRuleOrElse(eAttribute, eObject, copyEObject,
						() -> super.copyAttribute(eAttribute, eObject, copyEObject));
				}

				@Override
				protected void copyReference(EReference eReference, EObject eObject, EObject copyEObject) {
					applyCopyRuleOrElse(eReference, eObject, copyEObject,
						() -> super.copyReference(eReference, eObject, copyEObject));
				}

				private void applyCopyRuleOrElse(EStructuralFeature feature, EObject eObject, EObject copyEObject, Runnable runnable) {
					// if the multiplicity changes and the type changes we might
					// end up with a list with a single default value.
					// if instead we check that the original value of the object for the feature
					// is set we avoid such a situation.
					if (eObject.eIsSet(feature) && predicate.test(feature))
						procedure.apply(feature, eObject, copyEObject);
					else
						runnable.run();;
				}
			};
			copyModels(customCopier, basedir, originalModelManager, evolvingModelManager);
			updateMigrationContext();
		}

		private void updateMigrationContext() {
			// here we copy the Ecores and models that have been migrated
			var backup = new EdeltaModelManager();
			// first create a copy of the evolved ecores
			// map: orig -> copy
			// orig are the evolved ecores, copy are the backup Ecores
			var map = backup.copyEcores(evolvingModelManager, basedir);
			// now create a copy of the evolved models
			// we have to use our custom Copier because that will correctly
			// create copies of models referring to the backup ecores
			copyModels(new EdeltaModelCopier(map), basedir, evolvingModelManager, backup);
			// now we need an inverted map, because the backup is meant to become the
			// new originals, for the next model migrations
			mapOfCopiedEcores = HashBiMap.create(map).inverse();
			modelCopier = new EdeltaModelCopier(mapOfCopiedEcores);
			evolvingModelManager.clearModels();
			// the original model manager is updated with the copies we have just created
			originalModelManager = backup;

			// we must set the SettingDelegate to null
			// to force its recreation, so that it takes into
			// consideration possible changes of the feature, e.g., multiplicity
			EdeltaResourceUtils.getEPackages(
				evolvingModelManager.getEcoreResourceMap().values()).stream()
				.flatMap(p -> EdeltaUtils.allEClasses(p).stream())
				.flatMap(c -> c.getEStructuralFeatures().stream())
				.forEach(f -> ((EStructuralFeature.Internal)f).setSettingDelegate(null));
		}

		public boolean isRelatedTo(ENamedElement origEcoreElement, ENamedElement evolvedEcoreElement) {
			return modelCopier.isRelatedTo(origEcoreElement, evolvedEcoreElement);
		}

		public <T extends ENamedElement> Predicate<T> isRelatedTo(T evolvedEcoreElement) {
			return modelCopier.relatesTo(evolvedEcoreElement);
		}

		public CopyProcedure multiplicityAwareCopy(EStructuralFeature feature) {
			return (EStructuralFeature oldFeature, EObject oldObj, EObject newObj) -> {
				// if we come here the old feature was set
				EdeltaEcoreUtil.setValueForFeature(
					newObj,
					feature,
					// use the upper bound of the destination feature, since it might
					// be different from the original one
					EdeltaEcoreUtil
						.getValueForFeature(oldObj, oldFeature, feature.getUpperBound())
				);
			};
		}
	
		public AttributeTransformer multiplicityAwareTranformer(EAttribute attribute, Function<Object, Object> transformer) {
			return (feature, oldObj, oldValue) ->
				// if we come here the old attribute was set
				EdeltaEcoreUtil.unwrapCollection(
					// use the upper bound of the destination attribute, since it might
					// be different from the original one
					EdeltaEcoreUtil.wrapAsCollection(oldValue, attribute.getUpperBound())
						.stream()
						.map(transformer)
						.collect(Collectors.toList()),
					attribute
				);
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

		private boolean isStillThere(EObject target) {
			return target != null && !isNotThereAnymore(target);
		}

		private boolean isNotThereAnymore(EObject target) {
			return target != null && target.eResource() == null;
		}

		public boolean isRelatedTo(ENamedElement origEcoreElement, ENamedElement evolvedEcoreElement) {
			return isStillThere(evolvedEcoreElement) &&
				origEcoreElement == ecoreCopyMap.inverse().get(evolvedEcoreElement);
		}

		public <T extends ENamedElement> Predicate<T> relatesTo(T evolvedEcoreElements) {
			return origEcoreElement -> isRelatedTo(origEcoreElement, evolvedEcoreElements);
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

		public void clearModels() {
			var map = getModelResourceMap();
			for (var entry : map.entrySet()) {
				var resource = (XMIResource) entry.getValue();
				resource.getResourceSet()
					.getResources().remove(resource);
			}
			map.clear();
		}
	}

	@BeforeClass
	public static void clearOutput() throws IOException {
		cleanDirectoryAndFirstSubdirectories(OUTPUT);
	}

	@Before
	public void setup() {
		evolvingModelManager = new EdeltaModelManager();
	}

	private EdeltaModelMigrator setupMigrator(
			String subdir,
			Collection<String> ecoreFiles,
			Collection<String> modelFiles
		) {
		var basedir = TESTDATA + subdir;
		var originalModelManager = new EdeltaModelManager();
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

	@Test
	public void testToUpperCaseStringAttributes() throws IOException {
		var subdir = "toUpperCaseStringAttributes/";

		var modelMigrator = setupMigrator(
			subdir,
			of("My.ecore"),
			of("MyClass.xmi", "MyClass2.xmi", "MyClass3.xmi")
		);

		modelMigrator.transformAttributeValueRule(
			a ->
				a.getEAttributeType() == EcorePackage.eINSTANCE.getEString(),
			oldValue ->
				oldValue.toString().toUpperCase()
		);

		copyModelsSaveAndAssertOutputs(
			modelMigrator,
			subdir,
			subdir,
			of("My.ecore"),
			of("MyClass.xmi", "MyClass2.xmi", "MyClass3.xmi")
		);
	}

	@Test
	public void testToUpperCaseSingleAttribute() throws IOException {
		var subdir = "toUpperCaseStringAttributes/";

		var modelMigrator = setupMigrator(
			subdir,
			of("My.ecore"),
			of("MyClass.xmi", "MyClass2.xmi", "MyClass3.xmi")
		);

		var attribute = getAttribute(evolvingModelManager,
				"mypackage", "MyClass", "myAttribute");
		modelMigrator.transformAttributeValueRule(
			a ->
				modelMigrator.isRelatedTo(a, attribute),
			oldValue ->
				oldValue.toString().toUpperCase()
		);

		copyModelsSaveAndAssertOutputs(
			modelMigrator,
			subdir,
			"toUpperCaseSingleAttribute/",
			of("My.ecore"),
			of("MyClass.xmi", "MyClass2.xmi", "MyClass3.xmi")
		);
	}

	@Test
	public void testToUpperCaseSingleAttributeAndRenamedBefore() throws IOException {
		var subdir = "toUpperCaseStringAttributes/";

		var modelMigrator = setupMigrator(
			subdir,
			of("My.ecore"),
			of("MyClass.xmi", "MyClass2.xmi", "MyClass3.xmi")
		);

		var attribute = getAttribute(evolvingModelManager,
				"mypackage", "MyClass", "myAttribute");
		attribute.setName("myAttributeRenamed");
		modelMigrator.transformAttributeValueRule(
			a ->
				modelMigrator.isRelatedTo(a, attribute),
			oldValue ->
				oldValue.toString().toUpperCase()
		);

		copyModelsSaveAndAssertOutputs(
			modelMigrator,
			subdir,
			"toUpperCaseSingleAttributeAndRenamedBefore/",
			of("My.ecore"),
			of("MyClass.xmi", "MyClass2.xmi", "MyClass3.xmi")
		);
	}

	@Test
	public void testToUpperCaseSingleAttributeAndRenamedAfter() throws IOException {
		var subdir = "toUpperCaseStringAttributes/";

		var modelMigrator = setupMigrator(
			subdir,
			of("My.ecore"),
			of("MyClass.xmi", "MyClass2.xmi", "MyClass3.xmi")
		);

		var attribute = getAttribute(evolvingModelManager,
				"mypackage", "MyClass", "myAttribute");
		modelMigrator.transformAttributeValueRule(
			a ->
				modelMigrator.isRelatedTo(a, attribute),
			oldValue ->
				oldValue.toString().toUpperCase()
		);
		attribute.setName("myAttributeRenamed");

		copyModelsSaveAndAssertOutputs(
			modelMigrator,
			subdir,
			"toUpperCaseSingleAttributeAndRenamedBefore/",
			of("My.ecore"),
			of("MyClass.xmi", "MyClass2.xmi", "MyClass3.xmi")
		);
	}

	@Test
	public void testToUpperCaseSingleAttributeAndMakeMultipleBefore() throws IOException {
		var subdir = "toUpperCaseStringAttributes/";

		var modelMigrator = setupMigrator(
			subdir,
			of("My.ecore"),
			of("MyClass.xmi", "MyClass2.xmi", "MyClass3.xmi")
		);

		var attribute = getAttribute(evolvingModelManager,
				"mypackage", "MyClass", "myAttribute");

		makeMultiple(modelMigrator, attribute);

		modelMigrator.transformAttributeValueRule(
			modelMigrator.isRelatedTo(attribute),
			modelMigrator.multiplicityAwareTranformer(attribute,
				o -> o.toString().toUpperCase())
		);

		copyModelsSaveAndAssertOutputs(
			modelMigrator,
			subdir,
			"toUpperCaseSingleAttributeAndMakeMultiple/",
			of("My.ecore"),
			of("MyClass.xmi", "MyClass2.xmi", "MyClass3.xmi")
		);
	}

	@Test
	public void testToUpperCaseSingleAttributeAndMakeMultipleAfter() throws IOException {
		var subdir = "toUpperCaseStringAttributes/";

		var modelMigrator = setupMigrator(
			subdir,
			of("My.ecore"),
			of("MyClass.xmi", "MyClass2.xmi", "MyClass3.xmi")
		);

		var attribute = getAttribute(evolvingModelManager,
				"mypackage", "MyClass", "myAttribute");

		modelMigrator.transformAttributeValueRule(
			a ->
				modelMigrator.isRelatedTo(a, attribute),
			oldValue ->
				oldValue.toString().toUpperCase()
		);

		makeMultiple(modelMigrator, attribute);

		copyModelsSaveAndAssertOutputs(
			modelMigrator,
			subdir,
			"toUpperCaseSingleAttributeAndMakeMultiple/",
			of("My.ecore"),
			of("MyClass.xmi", "MyClass2.xmi", "MyClass3.xmi")
		);
	}

	@Test
	public void testToUpperCaseSingleAttributeMultiple() throws IOException {
		var subdir = "toUpperCaseStringAttributesMultiple/";

		var modelMigrator = setupMigrator(
			subdir,
			of("My.ecore"),
			of("MyClass.xmi", "MyClass2.xmi", "MyClass3.xmi")
		);

		var attribute = getAttribute(evolvingModelManager,
				"mypackage", "MyClass", "myAttribute");

		modelMigrator.transformAttributeValueRule(
			modelMigrator.isRelatedTo(attribute),
			modelMigrator.multiplicityAwareTranformer(attribute,
				o -> o.toString().toUpperCase())
		);

		copyModelsSaveAndAssertOutputs(
			modelMigrator,
			subdir,
			subdir,
			of("My.ecore"),
			of("MyClass.xmi", "MyClass2.xmi", "MyClass3.xmi")
		);
	}

	@Test
	public void testToUpperCaseSingleAttributeMultipleAndMakeSingleBefore() throws IOException {
		var subdir = "toUpperCaseStringAttributesMultiple/";

		var modelMigrator = setupMigrator(
			subdir,
			of("My.ecore"),
			of("MyClass.xmi", "MyClass2.xmi", "MyClass3.xmi")
		);

		var attribute = getAttribute(evolvingModelManager,
				"mypackage", "MyClass", "myAttribute");

		makeSingle(modelMigrator, attribute);

		modelMigrator.transformAttributeValueRule(
			modelMigrator.isRelatedTo(attribute),
			modelMigrator.multiplicityAwareTranformer(attribute,
				o -> o.toString().toUpperCase())
		);

		copyModelsSaveAndAssertOutputs(
			modelMigrator,
			subdir,
			"toUpperCaseSingleAttributeMultipleAndMakeSingle/",
			of("My.ecore"),
			of("MyClass.xmi", "MyClass2.xmi", "MyClass3.xmi")
		);
	}

	@Test
	public void testToUpperCaseSingleAttributeMultipleAndMakeSingleAfter() throws IOException {
		var subdir = "toUpperCaseStringAttributesMultiple/";

		var modelMigrator = setupMigrator(
			subdir,
			of("My.ecore"),
			of("MyClass.xmi", "MyClass2.xmi", "MyClass3.xmi")
		);

		var attribute = getAttribute(evolvingModelManager,
				"mypackage", "MyClass", "myAttribute");

		modelMigrator.transformAttributeValueRule(
			modelMigrator.isRelatedTo(attribute),
			modelMigrator.multiplicityAwareTranformer(attribute,
				o -> o.toString().toUpperCase())
		);

		makeSingle(modelMigrator, attribute);

		copyModelsSaveAndAssertOutputs(
			modelMigrator,
			subdir,
			"toUpperCaseSingleAttributeMultipleAndMakeSingle/",
			of("My.ecore"),
			of("MyClass.xmi", "MyClass2.xmi", "MyClass3.xmi")
		);
	}

	@Test
	public void testMakeSingle() throws IOException {
		var subdir = "toUpperCaseStringAttributesMultiple/";

		var modelMigrator = setupMigrator(
			subdir,
			of("My.ecore"),
			of("MyClass.xmi", "MyClass2.xmi", "MyClass3.xmi")
		);

		var attribute = getAttribute(evolvingModelManager,
				"mypackage", "MyClass", "myAttribute");

		makeSingle(modelMigrator, attribute);

		copyModelsSaveAndAssertOutputs(
			modelMigrator,
			subdir,
			"makeSingle/",
			of("My.ecore"),
			of("MyClass.xmi", "MyClass2.xmi", "MyClass3.xmi")
		);
	}

	@Test
	public void testMakeMultiple() throws IOException {
		var subdir = "toUpperCaseStringAttributes/";

		var modelMigrator = setupMigrator(
			subdir,
			of("My.ecore"),
			of("MyClass.xmi", "MyClass2.xmi", "MyClass3.xmi")
		);

		var attribute = getAttribute(evolvingModelManager,
				"mypackage", "MyClass", "myAttribute");

		makeMultiple(modelMigrator, attribute);

		copyModelsSaveAndAssertOutputs(
			modelMigrator,
			subdir,
			"makeMultiple/",
			of("My.ecore"),
			of("MyClass.xmi", "MyClass2.xmi", "MyClass3.xmi")
		);
	}

	@Test
	public void testMakeMultipleTo2() throws IOException {
		var subdir = "toUpperCaseStringAttributesMultiple/";

		var modelMigrator = setupMigrator(
			subdir,
			of("My.ecore"),
			of("MyClass.xmi", "MyClass2.xmi", "MyClass3.xmi")
		);

		var attribute = getAttribute(evolvingModelManager,
				"mypackage", "MyClass", "myAttribute");

		makeMultiple(modelMigrator, attribute, 2);

		copyModelsSaveAndAssertOutputs(
			modelMigrator,
			subdir,
			"makeMultipleTo2/",
			of("My.ecore"),
			of("MyClass.xmi", "MyClass2.xmi", "MyClass3.xmi")
		);
	}

	@Test
	public void testMakeMultipleAndMakeSingle() throws IOException {
		var subdir = "toUpperCaseStringAttributes/";

		var modelMigrator = setupMigrator(
			subdir,
			of("My.ecore"),
			of("MyClass.xmi", "MyClass2.xmi", "MyClass3.xmi")
		);

		var attribute = getAttribute(evolvingModelManager,
				"mypackage", "MyClass", "myAttribute");

		makeMultiple(modelMigrator, attribute);

		makeSingle(modelMigrator, attribute);

		copyModelsSaveAndAssertOutputs(
			modelMigrator,
			subdir,
			"makeMultipleAndMakeSingle/",
			of("My.ecore"),
			of("MyClass.xmi", "MyClass2.xmi", "MyClass3.xmi")
		);
	}

	/**
	 * From the metamodel point of view we get the same Ecore,
	 * but of course from the model point of view, during the first migration,
	 * we lose some elements (the ones after the first one).
	 * 
	 * @throws IOException
	 */
	@Test
	public void testMakeSingleAndMakeMultiple() throws IOException {
		var subdir = "toUpperCaseStringAttributesMultiple/";

		var modelMigrator = setupMigrator(
			subdir,
			of("My.ecore"),
			of("MyClass.xmi", "MyClass2.xmi", "MyClass3.xmi")
		);

		var attribute = getAttribute(evolvingModelManager,
				"mypackage", "MyClass", "myAttribute");

		makeSingle(modelMigrator, attribute);

		makeMultiple(modelMigrator, attribute);

		copyModelsSaveAndAssertOutputs(
			modelMigrator,
			subdir,
			"makeSingleAndMakeMultiple/",
			of("My.ecore"),
			of("MyClass.xmi", "MyClass2.xmi", "MyClass3.xmi")
		);
	}

	@Test
	public void testChangedAttributeTypeWithoutProperMigration() {
		var subdir = "changedAttributeType/";

		var modelMigrator = setupMigrator(
			subdir,
			of("My.ecore"),
			of("MyClass.xmi")
		);

		// actual refactoring
		getAttribute(evolvingModelManager, "mypackage", "MyClass", "myAttribute")
			.setEType(EcorePackage.eINSTANCE.getEInt());

		Assertions.assertThatThrownBy(() -> // NOSONAR
		copyModelsSaveAndAssertOutputs(
			modelMigrator,
			subdir,
			"should not get here/",
			of("My.ecore"),
			of("MyClass.xmi")
		))
		.isInstanceOf(ClassCastException.class)
		.hasMessageContaining(
			"The value of type 'class java.lang.String' must be of type 'class java.lang.Integer'");
	}

	@Test
	public void testChangedAttributeTypeWithAttributeMigrator() throws IOException {
		var subdir = "changedAttributeType/";

		var modelMigrator = setupMigrator(
			subdir,
			of("My.ecore"),
			of("MyClass.xmi", "MyClass2.xmi", "MyClass3.xmi")
		);

		// actual refactoring
		var attributeName = "myAttribute";
		var attribute = getAttribute(evolvingModelManager, "mypackage", "MyClass", attributeName);
		attribute.setEType(EcorePackage.eINSTANCE.getEInt());

		// custom migration rule
		modelMigrator.transformAttributeValueRule(
			a ->
				modelMigrator.isRelatedTo(a, attribute),
			(feature, o, oldValue) -> {
				// if we come here the old attribute was set
				return EdeltaEcoreUtil.unwrapCollection(
					EdeltaEcoreUtil.wrapAsCollection(oldValue, -1)
						.stream()
						.map(val -> {
							try {
								return Integer.parseInt(val.toString());
							} catch (NumberFormatException e) {
								return -1;
							}
						})
						.collect(Collectors.toList()),
					feature);
			}
		);

		copyModelsSaveAndAssertOutputs(
			modelMigrator,
			subdir,
			subdir,
			of("My.ecore"),
			of("MyClass.xmi", "MyClass2.xmi", "MyClass3.xmi")
		);
	}

	@Test
	public void testChangeAttributeType() throws IOException {
		var subdir = "changedAttributeType/";

		var modelMigrator = setupMigrator(
			subdir,
			of("My.ecore"),
			of("MyClass.xmi", "MyClass2.xmi", "MyClass3.xmi")
		);

		// actual refactoring
		var attributeName = "myAttribute";
		var attribute = getAttribute(evolvingModelManager, "mypackage", "MyClass", attributeName);

		changeAttributeType(modelMigrator, attribute,
			EcorePackage.eINSTANCE.getEInt(),
			val -> {
				try {
					return Integer.parseInt(val.toString());
				} catch (NumberFormatException e) {
					return -1;
				}
			}
		);

		copyModelsSaveAndAssertOutputs(
			modelMigrator,
			subdir,
			subdir,
			of("My.ecore"),
			of("MyClass.xmi", "MyClass2.xmi", "MyClass3.xmi")
		);
	}

	@Test
	public void testChangeAttributeTypeAlternative() throws IOException {
		var subdir = "changedAttributeType/";

		var modelMigrator = setupMigrator(
			subdir,
			of("My.ecore"),
			of("MyClass.xmi", "MyClass2.xmi", "MyClass3.xmi")
		);

		// actual refactoring
		var attributeName = "myAttribute";
		var attribute = getAttribute(evolvingModelManager, "mypackage", "MyClass", attributeName);

		changeAttributeTypeAlternative(modelMigrator, attribute,
			EcorePackage.eINSTANCE.getEInt(),
			val -> {
				try {
					return Integer.parseInt(val.toString());
				} catch (NumberFormatException e) {
					return -1;
				}
			}
		);

		copyModelsSaveAndAssertOutputs(
			modelMigrator,
			subdir,
			subdir,
			of("My.ecore"),
			of("MyClass.xmi", "MyClass2.xmi", "MyClass3.xmi")
		);
	}

	/**
	 * Since the type (from String to int) is changed before changing it to
	 * multiple, string values that were null would become 0 int values, so they would
	 * become a singleton list with 0 (MyClass.xmi). Instead, changing the
	 * multiplicity before, will lead to an empty list for original null string
	 * values. These two behaviors must be the same, that's why we have
	 * the check eObject.eIsSet(feature) in {@link EdeltaModelMigrator#copyRule(Predicate, EdeltaModelMigrator.CopyProcedure)}.
	 * 
	 * @throws IOException
	 */
	@Test
	public void testChangeAttributeTypeAndMutiplicityAfter() throws IOException {
		var subdir = "changedAttributeType/";

		var modelMigrator = setupMigrator(
			subdir,
			of("My.ecore"),
			of("MyClass.xmi", "MyClass2.xmi", "MyClass3.xmi")
		);

		// actual refactoring
		var attributeName = "myAttribute";
		var attribute = getAttribute(evolvingModelManager, "mypackage", "MyClass", attributeName);

		changeAttributeType(modelMigrator, attribute,
			EcorePackage.eINSTANCE.getEInt(),
			val -> {
				try {
					return Integer.parseInt(val.toString());
				} catch (NumberFormatException e) {
					return -1;
				}
			}
		);

		makeMultiple(modelMigrator, attribute);

		copyModelsSaveAndAssertOutputs(
			modelMigrator,
			subdir,
			"changedAttributeTypeAndMultiplicity/",
			of("My.ecore"),
			of("MyClass.xmi", "MyClass2.xmi", "MyClass3.xmi")
		);
	}

	@Test
	public void testChangeAttributeTypeAndMutiplicityAfterAlternative() throws IOException {
		var subdir = "changedAttributeType/";

		var modelMigrator = setupMigrator(
			subdir,
			of("My.ecore"),
			of("MyClass.xmi", "MyClass2.xmi", "MyClass3.xmi")
		);

		// actual refactoring
		var attributeName = "myAttribute";
		var attribute = getAttribute(evolvingModelManager, "mypackage", "MyClass", attributeName);

		changeAttributeTypeAlternative(modelMigrator, attribute,
			EcorePackage.eINSTANCE.getEInt(),
			val -> {
				try {
					return Integer.parseInt(val.toString());
				} catch (NumberFormatException e) {
					return -1;
				}
			}
		);

		makeMultiple(modelMigrator, attribute);

		copyModelsSaveAndAssertOutputs(
			modelMigrator,
			subdir,
			"changedAttributeTypeAndMultiplicity/",
			of("My.ecore"),
			of("MyClass.xmi", "MyClass2.xmi", "MyClass3.xmi")
		);
	}

	@Test
	public void testChangeAttributeTypeAndMutiplicityBefore() throws IOException {
		var subdir = "changedAttributeType/";

		var modelMigrator = setupMigrator(
			subdir,
			of("My.ecore"),
			of("MyClass.xmi", "MyClass2.xmi", "MyClass3.xmi")
		);

		// actual refactoring
		var attributeName = "myAttribute";
		var attribute = getAttribute(evolvingModelManager, "mypackage", "MyClass", attributeName);

		makeMultiple(modelMigrator, attribute);

		changeAttributeType(modelMigrator, attribute,
			EcorePackage.eINSTANCE.getEInt(),
			val -> {
				try {
					return Integer.parseInt(val.toString());
				} catch (NumberFormatException e) {
					return -1;
				}
			}
		);

		copyModelsSaveAndAssertOutputs(
			modelMigrator,
			subdir,
			"changedAttributeTypeAndMultiplicity/",
			of("My.ecore"),
			of("MyClass.xmi", "MyClass2.xmi", "MyClass3.xmi")
		);
	}

	@Test
	public void testChangeAttributeTypeAndMutiplicityBeforeAlternative() throws IOException {
		var subdir = "changedAttributeType/";

		var modelMigrator = setupMigrator(
			subdir,
			of("My.ecore"),
			of("MyClass.xmi", "MyClass2.xmi", "MyClass3.xmi")
		);

		// actual refactoring
		var attributeName = "myAttribute";
		var attribute = getAttribute(evolvingModelManager, "mypackage", "MyClass", attributeName);

		makeMultiple(modelMigrator, attribute);

		changeAttributeTypeAlternative(modelMigrator, attribute,
			EcorePackage.eINSTANCE.getEInt(),
			val -> {
				try {
					return Integer.parseInt(val.toString());
				} catch (NumberFormatException e) {
					return -1;
				}
			}
		);

		copyModelsSaveAndAssertOutputs(
			modelMigrator,
			subdir,
			"changedAttributeTypeAndMultiplicity/",
			of("My.ecore"),
			of("MyClass.xmi", "MyClass2.xmi", "MyClass3.xmi")
		);
	}

	@Test
	public void testChangedAttributeTypeWithCopyRule() throws IOException {
		var subdir = "changedAttributeType/";

		var modelMigrator = setupMigrator(
			subdir,
			of("My.ecore"),
			of("MyClass.xmi")
		);

		// actual refactoring
		var attributeName = "myAttribute";
		var attribute = getAttribute(evolvingModelManager, "mypackage", "MyClass", attributeName);
		attribute.setEType(EcorePackage.eINSTANCE.getEInt());

		// custom migration rule
		modelMigrator.copyRule(
			a ->
				modelMigrator.isRelatedTo(a, attribute),
			(feature, oldObj, newObj) -> {
				// feature is the feature of the original ecore
				// oldObj is the original model's object,
				// newObj is the copy, so it's the new model's object
				// feature must be used to access th eold model's object's value
				// attribute is from the evolved ecore
				newObj.eSet(attribute,
					Integer.parseInt(
						oldObj.eGet(feature).toString()));
			}
		);

		copyModelsSaveAndAssertOutputs(
			modelMigrator,
			subdir,
			subdir,
			of("My.ecore"),
			of("MyClass.xmi")
		);
	}

	@Test
	public void testChangedMultiAttributeType() throws IOException {
		var subdir = "changedMultiAttributeType/";

		var modelMigrator = setupMigrator(
			subdir,
			of("My.ecore"),
			of("MyClass.xmi")
		);

		// actual refactoring
		var attributeName = "myAttribute";
		var attribute = getAttribute(evolvingModelManager, "mypackage", "MyClass", attributeName);

		changeAttributeType(modelMigrator, attribute,
			EcorePackage.eINSTANCE.getEInt(),
			val -> {
				try {
					return Integer.parseInt(val.toString());
				} catch (NumberFormatException e) {
					return -1;
				}
			}
		);

		copyModelsSaveAndAssertOutputs(
			modelMigrator,
			subdir,
			subdir,
			of("My.ecore"),
			of("MyClass.xmi")
		);
	}

	@Test
	public void testChangedMultiAttributeTypeAlternative() throws IOException {
		var subdir = "changedMultiAttributeType/";

		var modelMigrator = setupMigrator(
			subdir,
			of("My.ecore"),
			of("MyClass.xmi")
		);

		// actual refactoring
		var attributeName = "myAttribute";
		var attribute = getAttribute(evolvingModelManager, "mypackage", "MyClass", attributeName);

		changeAttributeTypeAlternative(modelMigrator, attribute,
			EcorePackage.eINSTANCE.getEInt(),
			val -> {
				try {
					return Integer.parseInt(val.toString());
				} catch (NumberFormatException e) {
					return -1;
				}
			}
		);

		copyModelsSaveAndAssertOutputs(
			modelMigrator,
			subdir,
			subdir,
			of("My.ecore"),
			of("MyClass.xmi")
		);
	}

	@Test
	public void testChangedMultiAttributeTypeAndMultiplicity() throws IOException {
		var subdir = "changedMultiAttributeType/";

		var modelMigrator = setupMigrator(
			subdir,
			of("My.ecore"),
			of("MyClass.xmi")
		);

		// actual refactoring
		var attributeName = "myAttribute";
		var attribute = getAttribute(evolvingModelManager, "mypackage", "MyClass", attributeName);

		changeAttributeType(modelMigrator, attribute,
			EcorePackage.eINSTANCE.getEInt(),
			val -> {
				try {
					return Integer.parseInt(val.toString());
				} catch (NumberFormatException e) {
					return -1;
				}
			}
		);

		makeSingle(modelMigrator, attribute);

		copyModelsSaveAndAssertOutputs(
			modelMigrator,
			subdir,
			"changedMultiAttributeTypeAndMultiplicity/",
			of("My.ecore"),
			of("MyClass.xmi")
		);
	}

	@Test
	public void testChangedMultiAttributeTypeAndMultiplicityAlternative() throws IOException {
		var subdir = "changedMultiAttributeType/";

		var modelMigrator = setupMigrator(
			subdir,
			of("My.ecore"),
			of("MyClass.xmi")
		);

		// actual refactoring
		var attributeName = "myAttribute";
		var attribute = getAttribute(evolvingModelManager, "mypackage", "MyClass", attributeName);

		changeAttributeTypeAlternative(modelMigrator, attribute,
			EcorePackage.eINSTANCE.getEInt(),
			val -> {
				try {
					return Integer.parseInt(val.toString());
				} catch (NumberFormatException e) {
					return -1;
				}
			}
		);

		makeSingle(modelMigrator, attribute);

		copyModelsSaveAndAssertOutputs(
			modelMigrator,
			subdir,
			"changedMultiAttributeTypeAndMultiplicity/",
			of("My.ecore"),
			of("MyClass.xmi")
		);
	}

	@Test
	public void testChangedMultiAttributeTypeAndMultiplicityTo2() throws IOException {
		var subdir = "changedMultiAttributeType/";

		var modelMigrator = setupMigrator(
			subdir,
			of("My.ecore"),
			of("MyClass.xmi")
		);

		// actual refactoring
		var attributeName = "myAttribute";
		var attribute = getAttribute(evolvingModelManager, "mypackage", "MyClass", attributeName);

		makeMultiple(modelMigrator, attribute, 2);

		changeAttributeType(modelMigrator, attribute,
			EcorePackage.eINSTANCE.getEInt(),
			val -> {
				try {
					return Integer.parseInt(val.toString());
				} catch (NumberFormatException e) {
					return -1;
				}
			}
		);

		copyModelsSaveAndAssertOutputs(
			modelMigrator,
			subdir,
			"changedMultiAttributeTypeAndMultiplicityTo2/",
			of("My.ecore"),
			of("MyClass.xmi")
		);
	}

	@Test
	public void testChangedMultiAttributeTypeAndMultiplicityTo2Alternative() throws IOException {
		var subdir = "changedMultiAttributeType/";

		var modelMigrator = setupMigrator(
			subdir,
			of("My.ecore"),
			of("MyClass.xmi")
		);

		// actual refactoring
		var attributeName = "myAttribute";
		var attribute = getAttribute(evolvingModelManager, "mypackage", "MyClass", attributeName);

		makeMultiple(modelMigrator, attribute, 2);

		changeAttributeTypeAlternative(modelMigrator, attribute,
			EcorePackage.eINSTANCE.getEInt(),
			val -> {
				try {
					return Integer.parseInt(val.toString());
				} catch (NumberFormatException e) {
					return -1;
				}
			}
		);

		copyModelsSaveAndAssertOutputs(
			modelMigrator,
			subdir,
			"changedMultiAttributeTypeAndMultiplicityTo2/",
			of("My.ecore"),
			of("MyClass.xmi")
		);
	}

	@Test
	public void testChangedAttributeNameAndType() throws IOException {
		var subdir = "changedAttributeType/";

		var modelMigrator = setupMigrator(
			subdir,
			of("My.ecore"),
			of("MyClass.xmi")
		);

		// actual refactoring
		var attributeName = "myAttribute";
		var attribute = getAttribute(evolvingModelManager, "mypackage", "MyClass", attributeName);
		attribute.setName("newName");
		attribute.setEType(EcorePackage.eINSTANCE.getEInt());

		// custom migration rule
		modelMigrator.transformAttributeValueRule(
			modelMigrator.isRelatedTo(attribute),
			(feature, oldObj, oldValue) -> {
				var eClass = oldObj.eClass();
				return Integer.parseInt(
					oldObj.eGet(eClass.getEStructuralFeature(attributeName)).toString());
			}
		);

		copyModelsSaveAndAssertOutputs(
			modelMigrator,
			subdir,
			"changedAttributeNameAndType/",
			of("My.ecore"),
			of("MyClass.xmi")
		);
	}

	@Test
	public void testChangedAttributeTypeAndName() throws IOException {
		var subdir = "changedAttributeType/";

		var modelMigrator = setupMigrator(
			subdir,
			of("My.ecore"),
			of("MyClass.xmi")
		);

		// actual refactoring
		var attributeName = "myAttribute";
		var attribute = getAttribute(evolvingModelManager, "mypackage", "MyClass", attributeName);
		attribute.setEType(EcorePackage.eINSTANCE.getEInt());

		// custom migration rule
		modelMigrator.transformAttributeValueRule(
				modelMigrator.isRelatedTo(attribute),
			(feature, o, oldValue) -> {
				// o is the old object,
				// so we must use the original feature to retrieve the value to copy
				// that is, don't use attribute, which is the one of the new package
				var eClass = o.eClass();
				return Integer.parseInt(
					o.eGet(eClass.getEStructuralFeature(attributeName)).toString());
			}
		);
		attribute.setName("newName");

		copyModelsSaveAndAssertOutputs(
			modelMigrator,
			subdir,
			"changedAttributeTypeAndName/",
			of("My.ecore"),
			of("MyClass.xmi")
		);
	}

	@Test
	public void testMergeAttributes() throws IOException {
		var subdir = "mergeAttributes/";

		var modelMigrator = setupMigrator(
			subdir,
			of("Person.ecore"),
			of("Person.xmi")
		);

		var firstName = getAttribute(evolvingModelManager,
				"person", "Person", "firstname");
		var lastName = getAttribute(evolvingModelManager,
				"person", "Person", "lastname");
		// refactoring
		EcoreUtil.remove(lastName);
		// rename the first attribute among the ones to merge
		firstName.setName("fullName");
		// specify the converter using firstname and lastname original values
		modelMigrator.transformAttributeValueRule(
			modelMigrator.isRelatedTo(firstName),
			(feature, o, oldValue) -> {
				// o is the old object,
				// so we must use the original feature to retrieve the value to copy
				// that is, don't use attribute, which is the one of the new package
				var eClass = o.eClass();
				return 
					o.eGet(feature) +
					" " +
					o.eGet(eClass.getEStructuralFeature("lastname"));
			}
		);

		copyModelsSaveAndAssertOutputs(
			modelMigrator,
			subdir,
			subdir,
			of("Person.ecore"),
			of("Person.xmi")
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
			(fileName ->
				assertGeneratedFiles(fileName, outputdir, output, fileName));
		modelFiles.forEach
			(fileName ->
				assertGeneratedFiles(fileName, outputdir, output, fileName));
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
		assertGeneratedFiles(null, subdir, outputDir, fileName);
	}

	private void assertGeneratedFiles(String message, String subdir, String outputDir, String fileName) {
		try {
			assertFilesAreEquals(
				message,
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

	// SIMULATION OF REFACTORINGS THAT WILL BE PART OF OUR LIBRARY LATER

	/**
	 * Makes this feature multiple (upper = -1)
	 * 
	 * @param feature
	 */
	private static void makeMultiple(EdeltaModelMigrator modelMigrator, EStructuralFeature feature) {
		makeMultiple(modelMigrator, feature, -1);
	}

	/**
	 * Makes this feature multiple with a specific upper bound
	 * 
	 * @param feature
	 * @param upperBound
	 */
	private static void makeMultiple(EdeltaModelMigrator modelMigrator, EStructuralFeature feature, int upperBound) {
		feature.setUpperBound(upperBound);
		modelMigrator.copyRule(
			modelMigrator.isRelatedTo(feature),
			modelMigrator.multiplicityAwareCopy(feature)
		);
	}

	/**
	 * Makes this feature single (upper = 1)
	 * 
	 * @param feature
	 */
	private static void makeSingle(EdeltaModelMigrator modelMigrator, EStructuralFeature feature) {
		feature.setUpperBound(1);
		modelMigrator.copyRule(
			modelMigrator.isRelatedTo(feature),
			modelMigrator.multiplicityAwareCopy(feature)
		);
	}

	/**
	 * Changes the type of the attribute and when migrating the model
	 * it applies the passed lambda to transform the value or values
	 * (transparently).
	 * 
	 * @param modelMigrator
	 * @param attribute
	 * @param type
	 * @param singleValueTransformer
	 */
	private void changeAttributeType(EdeltaModelMigrator modelMigrator, EAttribute attribute,
			EDataType type, Function<Object, Object> singleValueTransformer) {
		attribute.setEType(type);
		modelMigrator.copyRule(
			a ->
				modelMigrator.isRelatedTo(a, attribute),
			(feature, oldObj, newObj) -> {
				// if we come here the old attribute was set
				EdeltaEcoreUtil.setValueForFeature(
					newObj,
					attribute,
					// use the upper bound of the destination attribute, since it might
					// be different from the original one
					EdeltaEcoreUtil.getValueForFeature(oldObj, feature, attribute.getUpperBound())
						.stream()
						.map(singleValueTransformer)
						.collect(Collectors.toList())
				);
			}
		);
	}

	/**
	 * Changes the type of the attribute and when migrating the model
	 * it applies the passed lambda to transform the value or values
	 * (transparently).
	 * 
	 * @param modelMigrator
	 * @param attribute
	 * @param type
	 * @param singleValueTransformer
	 */
	private void changeAttributeTypeAlternative(EdeltaModelMigrator modelMigrator, EAttribute attribute,
			EDataType type, Function<Object, Object> singleValueTransformer) {
		attribute.setEType(type);
		modelMigrator.transformAttributeValueRule(
			a ->
				modelMigrator.isRelatedTo(a, attribute),
			(feature, oldObj, oldValue) ->
				// if we come here the old attribute was set
				EdeltaEcoreUtil.unwrapCollection(
					// use the upper bound of the destination attribute, since it might
					// be different from the original one
					EdeltaEcoreUtil.wrapAsCollection(oldValue, attribute.getUpperBound())
						.stream()
						.map(singleValueTransformer)
						.collect(Collectors.toList()),
					attribute
				)
		);
	}
}
