package edelta.lib.learning.tests;

import static edelta.testutils.EdeltaTestUtils.assertFilesAreEquals;
import static edelta.testutils.EdeltaTestUtils.cleanDirectoryAndFirstSubdirectories;
import static java.util.List.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.log4j.Logger;
import org.assertj.core.api.Assertions;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.ENamedElement;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.EStructuralFeature.Setting;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.InternalEObject;
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
import org.eclipse.xtext.xbase.lib.StringExtensions;
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

		/**
		 * @author Lorenzo Bettini
		 *
		 * @param <T> the type of the {@link EObject} passed to the predicate
		 */
		public static abstract class AbstractModelMigrationRule<T extends EObject> {
			private Predicate<T> predicate;

			protected AbstractModelMigrationRule(Predicate<T> predicate) {
				this.predicate = predicate;
			}

			public boolean canApply(T arg) {
				return predicate.test(arg);
			}
		}

		/**
		 * @author Lorenzo Bettini
		 *
		 * @param <T> the type of the {@link EObject} passed to the predicate
		 * @param <T3> the type of the third argument passed to the function
		 * @param <R> the type of the returned value when applying the function
		 */
		public static class ModelMigrationFunctionRule<T extends EObject, T3, R> extends AbstractModelMigrationRule<T> {
			private Function3<T, EObject, T3, R> function;

			public ModelMigrationFunctionRule(Predicate<T> predicate, Function3<T, EObject, T3, R> function) {
				super(predicate);
				this.function = function;
			}

			public R apply(T arg, EObject oldObj, T3 arg3) {
				return function.apply(arg, oldObj, arg3);
			}
		}

		public static class ModelMigrationCopyFeatureRule extends AbstractModelMigrationRule<EStructuralFeature> {
			private Procedure3<EStructuralFeature, EObject, EObject> procedure;

			protected ModelMigrationCopyFeatureRule(Predicate<EStructuralFeature> predicate, Procedure3<EStructuralFeature, EObject, EObject> procedure) {
				super(predicate);
				this.procedure = procedure;
			}

			public void apply(EStructuralFeature feature, EObject oldObj, EObject newObj) {
				procedure.apply(feature, oldObj, newObj);
			}
		}

		private BiMap<EObject, EObject> ecoreCopyMap;

		/**
		 * This stores mappings from new elements to previous elements added during the
		 * evolution (for example, with a copy) to keep track of the chain of origins.
		 */
		private Map<EObject, Collection<EObject>> elementAssociations = new HashMap<>();

		private Collection<ModelMigrationFunctionRule<EAttribute, Object, Object>> valueMigrators = new ArrayList<>();

		private Collection<ModelMigrationFunctionRule<EStructuralFeature, EObject, EStructuralFeature>> featureMigrators = new ArrayList<>();

		private Collection<ModelMigrationCopyFeatureRule> copyRules = new ArrayList<>();

		public EdeltaModelMigrator(Map<EObject, EObject> ecoreCopyMap) {
			this.ecoreCopyMap = HashBiMap.create(ecoreCopyMap);
		}

		public void addEAttributeMigrator(Predicate<EAttribute> predicate, Function3<EAttribute, EObject, Object, Object> function) {
			valueMigrators.add(
				new ModelMigrationFunctionRule<>(
					predicate,
					function
				)
			);
		}

		public void addFeatureMigrator(Predicate<EStructuralFeature> predicate, Function3<EStructuralFeature, EObject, EObject, EStructuralFeature> function) {
			featureMigrators.add(
				new ModelMigrationFunctionRule<>(
					predicate,
					function
				)
			);
		}

		public void addCopyMigrator(Predicate<EStructuralFeature> predicate, Procedure3<EStructuralFeature, EObject, EObject> procedure) {
			copyRules.add(
				new ModelMigrationCopyFeatureRule(
					predicate,
					procedure
				)
			);
		}

		@Override
		protected EClass getTarget(EClass eClass) {
			return getMapped(eClass);
		}

		@Override
		protected EStructuralFeature getTarget(EStructuralFeature eStructuralFeature) {
			return getMapped(eStructuralFeature);
		}

		@Override
		protected Setting getTarget(EStructuralFeature eStructuralFeature, EObject eObject, EObject copyEObject) {
			EStructuralFeature targetEStructuralFeature = featureMigrators.stream()
				.filter(m -> m.canApply(eStructuralFeature))
				.map(m -> m.apply(eStructuralFeature, eObject, copyEObject))
				.filter(this::isStillThere)
				.findFirst()
				.orElse(null);
			return targetEStructuralFeature == null ?
				super.getTarget(eStructuralFeature, eObject, copyEObject)
				: ((InternalEObject) copyEObject).eSetting(targetEStructuralFeature);
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
			var first = copyRules.stream()
				.filter(m -> m.canApply(feature))
				.findFirst();
			first.ifPresentOrElse(
				m -> m.apply(feature, eObject, copyEObject),
				runnable
			);
		}

		@Override
		protected void copyAttributeValue(EAttribute eAttribute, EObject eObject, Object value, Setting setting) {
			var map = valueMigrators.stream()
				.filter(m -> m.canApply(eAttribute))
				.map(m -> m.apply(eAttribute, eObject, value));
			var newValue = map
				.findFirst()
				.orElse(value);
			super.copyAttributeValue(eAttribute, eObject, newValue, setting);
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

		public void addAssociation(EObject copy, EObject original) {
			elementAssociations.computeIfAbsent(copy, k -> new HashSet<>())
				.add(original);
		}

		public Collection<EObject> originals(EObject o) {
			return computeOriginals
					(elementAssociations
							.getOrDefault(o, Collections.emptyList()));
		}

		private Collection<EObject> computeOriginals(Collection<EObject> collection) {
			return collection.stream()
					.flatMap(o -> {
						var originals = originals(o);
						if (originals.isEmpty())
							return Stream.of(o);
						return originals.stream();
					})
					.collect(Collectors.toSet());
		}

		public boolean isRelatedTo(ENamedElement origEcoreElement, ENamedElement evolvedEcoreElement) {
			return isStillThere(evolvedEcoreElement) &&
				(
				origEcoreElement == ecoreCopyMap.inverse().get(evolvedEcoreElement)
				||
				originals(evolvedEcoreElement).stream()
					.map(o -> ecoreCopyMap.inverse().get(o))
					.anyMatch(o -> o == origEcoreElement)
				);
		}

		public <T extends ENamedElement> Predicate<T> relatesTo(T evolvedEcoreElements) {
			return origEcoreElement -> isRelatedTo(origEcoreElement, evolvedEcoreElements);
		}

		public boolean isRelatedToAtLeastOneOf(ENamedElement origEcoreElement, Collection<? extends ENamedElement> evolvedEcoreElements) {
			return evolvedEcoreElements.stream()
				.anyMatch(e -> isRelatedTo(origEcoreElement, e));
		}

		public <T extends ENamedElement> Predicate<T> relatesToAtLeastOneOf(Collection<? extends T> evolvedEcoreElements) {
			return origEcoreElement -> isRelatedToAtLeastOneOf(origEcoreElement, evolvedEcoreElements);
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
		var modelMigrator =
			new EdeltaModelMigrator(
				evolvingModelManager.copyEcores(originalModelManager, basedir));
		return modelMigrator;
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
		modelMigrator.addEAttributeMigrator(
			a ->
				modelMigrator.isRelatedTo(a, attribute),
			(feature, o, oldValue) -> {
				// if we come here the old attribute was set
				return EdeltaEcoreUtil.unwrapCollection(
					EdeltaEcoreUtil.wrapAsCollection(oldValue)
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
		modelMigrator.addCopyMigrator(
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
		modelMigrator.addEAttributeMigrator(
			modelMigrator.relatesTo(attribute),
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
		modelMigrator.addEAttributeMigrator(
			a ->
				modelMigrator.isRelatedTo(a, attribute),
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
		modelMigrator.addEAttributeMigrator(
			a ->
				modelMigrator.isRelatedTo(a, firstName),
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

	@Test
	public void testElementAssociations() {
		var subdir = "unchanged/";

		var modelMigrator = setupMigrator(
			subdir,
			of("My.ecore"),
			of("MyClass.xmi")
		);

		var origfeature1 = getAttribute(originalModelManager,
				"mypackage", "MyClass", "myClassStringAttribute");
		var origfeature2 = getFeature(originalModelManager,
				"mypackage", "MyRoot", "myReferences");

		var feature1 = getAttribute(evolvingModelManager,
				"mypackage", "MyClass", "myClassStringAttribute");
		var feature2 = getFeature(evolvingModelManager,
				"mypackage", "MyRoot", "myReferences");

		// automatic existing associations
		assertTrue(modelMigrator.isRelatedTo(origfeature1, feature1));
		assertTrue(modelMigrator.isRelatedTo(origfeature2, feature2));
		assertFalse(modelMigrator.isRelatedTo(origfeature2, feature1));
		assertFalse(modelMigrator.isRelatedTo(origfeature1, feature2));

		// createCopy also creates associations
		var copyOfFeature1 = createCopy(modelMigrator, feature1);
		var copyOfCopy = createCopy(modelMigrator, copyOfFeature1);
		var singleCopy = createSingleCopy(modelMigrator, List.of(feature1, feature2));

		// make sure the copies are in the resource
		evolvingModelManager.getEPackage("mypackage").getEClassifiers().add(
			EdeltaUtils.newEClass("TestClass", c -> {
				c.getEStructuralFeatures()
					.addAll(List.of(copyOfFeature1, copyOfCopy, singleCopy));
			})
		);

		assertThat(modelMigrator.originals(copyOfFeature1))
			.containsExactlyInAnyOrder(feature1);

		assertThat(modelMigrator.originals(copyOfCopy))
			.containsExactlyInAnyOrder(feature1);

		assertTrue(modelMigrator.isRelatedTo(origfeature1, copyOfFeature1));
		assertTrue(modelMigrator.isRelatedTo(origfeature1, copyOfCopy));
		assertFalse(modelMigrator.isRelatedTo(origfeature2, copyOfFeature1));
		assertFalse(modelMigrator.isRelatedTo(origfeature2, copyOfCopy));

		assertTrue(modelMigrator.isRelatedToAtLeastOneOf(origfeature1,
				List.of(copyOfFeature1, copyOfCopy)));

		// explicit associations
		modelMigrator.addAssociation(copyOfCopy, feature2);

		assertFalse(modelMigrator.isRelatedTo(origfeature2, copyOfFeature1));
		// now this is true
		assertTrue(modelMigrator.isRelatedTo(origfeature2, copyOfCopy));

		assertThat(modelMigrator.originals(copyOfCopy))
			.containsExactlyInAnyOrder(feature1, feature2);

		// remove an element from its resource
		EcoreUtil.remove(copyOfCopy);
		assertFalse(modelMigrator.isRelatedTo(origfeature1, copyOfCopy));

		assertTrue(modelMigrator.isRelatedToAtLeastOneOf(origfeature1,
				List.of(copyOfCopy, copyOfFeature1)));

		assertThat(modelMigrator.originals(singleCopy))
			.containsExactlyInAnyOrder(feature1, feature2);
		assertTrue(modelMigrator.isRelatedTo(origfeature1, singleCopy));
		assertTrue(modelMigrator.isRelatedTo(origfeature2, singleCopy));
	}

	@Test
	public void testReplaceWithCopy() throws IOException {
		var subdir = "unchanged/";

		var modelMigrator = setupMigrator(
			subdir,
			of("My.ecore"),
			of("MyClass.xmi")
		);

		// actual refactoring
		var attribute = getAttribute(evolvingModelManager,
				"mypackage", "MyClass", "myClassStringAttribute");

		replaceWithCopy(modelMigrator, attribute, "myAttributeRenamed");

		copyModelsSaveAndAssertOutputs(
			modelMigrator,
			subdir,
			"replaceWithCopy/",
			of("My.ecore"),
			of("MyClass.xmi")
		);
	}

	@Test
	public void testReplaceWithCopyTwice() throws IOException {
		var subdir = "unchanged/";

		var modelMigrator = setupMigrator(
			subdir,
			of("My.ecore"),
			of("MyClass.xmi")
		);

		// actual refactoring
		var attribute = getAttribute(evolvingModelManager,
				"mypackage", "MyClass", "myClassStringAttribute");
		var copied = replaceWithCopy(modelMigrator, attribute, "myAttributeRenamed");
		replaceWithCopy(modelMigrator, copied, "myAttributeRenamedTwice");

		copyModelsSaveAndAssertOutputs(
			modelMigrator,
			subdir,
			"replaceWithCopyTwice/",
			of("My.ecore"),
			of("MyClass.xmi")
		);
	}

	/**
	 * Note that with pull up the migrated model is actually the same as the
	 * original one, but we have to adjust some mappings to make the copy work,
	 * because the value from the original object has to be put in an inherited
	 * attribute in the copied object.
	 * 
	 * @throws IOException
	 */
	@Test
	public void testPullUpFeatures() throws IOException {
		var subdir = "pullUpFeatures/";

		var modelMigrator = setupMigrator(
			subdir,
			of("PersonList.ecore"),
			of("List.xmi")
		);

		var personClass = getEClass(evolvingModelManager,
				"PersonList", "Person");
		var studentName = getFeature(evolvingModelManager,
				"PersonList", "Student", "name");
		var employeeName = getFeature(evolvingModelManager,
				"PersonList", "Employee", "name");
		// refactoring
		pullUp(modelMigrator,
				personClass,
				List.of(studentName, employeeName));

		copyModelsSaveAndAssertOutputs(
			modelMigrator,
			subdir,
			subdir,
			of("PersonList.ecore"),
			of("List.xmi")
		);
	}

	@Test
	public void testPullUpReferences() throws IOException {
		var subdir = "pullUpReferences/";

		var modelMigrator = setupMigrator(
			subdir,
			of("PersonList.ecore"),
			of("List.xmi")
		);

		var personClass = getEClass(evolvingModelManager,
				"PersonList", "Person");
		var studentAddress = getFeature(evolvingModelManager,
				"PersonList", "Student", "address");
		var employeeAddress = getFeature(evolvingModelManager,
				"PersonList", "Employee", "address");
		// refactoring
		pullUp(modelMigrator,
				personClass,
				List.of(studentAddress, employeeAddress));

		copyModelsSaveAndAssertOutputs(
			modelMigrator,
			subdir,
			subdir,
			of("PersonList.ecore"),
			of("List.xmi")
		);
	}

	@Test
	public void testPullUpContainmentReferences() throws IOException {
		var subdir = "pullUpContainmentReferences/";

		var modelMigrator = setupMigrator(
			subdir,
			of("PersonList.ecore"),
			of("List.xmi")
		);

		var personClass = getEClass(evolvingModelManager,
				"PersonList", "Person");
		var studentAddress = getFeature(evolvingModelManager,
				"PersonList", "Student", "address");
		var employeeAddress = getFeature(evolvingModelManager,
				"PersonList", "Employee", "address");
		// refactoring
		pullUp(modelMigrator,
				personClass,
				List.of(studentAddress, employeeAddress));

		copyModelsSaveAndAssertOutputs(
			modelMigrator,
			subdir,
			subdir,
			of("PersonList.ecore"),
			of("List.xmi")
		);
	}

	@Test
	public void testPushDownFeatures() throws IOException {
		var subdir = "pushDownFeatures/";

		var modelMigrator = setupMigrator(
			subdir,
			of("PersonList.ecore"),
			of("List.xmi")
		);

		var personClass = getEClass(evolvingModelManager,
				"PersonList", "Person");
		var personName = personClass.getEStructuralFeature("name");
		var studentClass = getEClass(evolvingModelManager,
				"PersonList", "Student");
		var employeeClass = getEClass(evolvingModelManager,
				"PersonList", "Employee");
		// refactoring
		pushDown(modelMigrator,
				personName,
				List.of(studentClass, employeeClass));

		copyModelsSaveAndAssertOutputs(
			modelMigrator,
			subdir,
			subdir,
			of("PersonList.ecore"),
			of("List.xmi")
		);
	}

	@Test
	public void testPullUpAndPushDown() throws IOException {
		var subdir = "pullUpFeatures/";

		var modelMigrator = setupMigrator(
			subdir,
			of("PersonList.ecore"),
			of("List.xmi")
		);

		var personClass = getEClass(evolvingModelManager,
				"PersonList", "Person");
		var studentClass = getEClass(evolvingModelManager,
				"PersonList", "Student");
		var employeeClass = getEClass(evolvingModelManager,
				"PersonList", "Employee");
		var studentName = studentClass.getEStructuralFeature("name");
		var employeeName = employeeClass.getEStructuralFeature("name");
		// refactoring
		var personName = pullUp(modelMigrator,
				personClass,
				List.of(studentName, employeeName));
		pushDown(modelMigrator,
				personName,
				List.of(studentClass, employeeClass));

		copyModelsSaveAndAssertOutputs(
			modelMigrator,
			subdir,
			"pullUpAndPushDown/",
			of("PersonList.ecore"),
			of("List.xmi")
		);
	}

	@Test
	public void testPushDownAndPullUp() throws IOException {
		var subdir = "pushDownFeatures/";

		var modelMigrator = setupMigrator(
			subdir,
			of("PersonList.ecore"),
			of("List.xmi")
		);

		var personClass = getEClass(evolvingModelManager,
				"PersonList", "Person");
		var personName = personClass.getEStructuralFeature("name");
		var studentClass = getEClass(evolvingModelManager,
				"PersonList", "Student");
		var employeeClass = getEClass(evolvingModelManager,
				"PersonList", "Employee");
		// refactoring
		var features = pushDown(modelMigrator,
				personName,
				List.of(studentClass, employeeClass));
		pullUp(modelMigrator,
				personClass,
				features);

		copyModelsSaveAndAssertOutputs(
			modelMigrator,
			subdir,
			"pushDownAndPullUp/",
			of("PersonList.ecore"),
			of("List.xmi")
		);
	}

	@Test
	public void testMakeBidirectional() throws IOException {
		var subdir = "makeBidirectional/";

		var modelMigrator = setupMigrator(
			subdir,
			of("PersonList.ecore"),
			of("List.xmi")
		);

		var personWorks = getReference(evolvingModelManager,
				"PersonList", "Person", "works");
		var workPlace = getEClass(evolvingModelManager,
				"PersonList", "WorkPlace");
		// refactoring
		var workPlacePerson = EdeltaUtils.newEReference("person", null);
		workPlace.getEStructuralFeatures().add(workPlacePerson);
		EdeltaUtils.makeBidirectional(personWorks, workPlacePerson);

		copyModelsSaveAndAssertOutputs(
			modelMigrator,
			subdir,
			subdir,
			of("PersonList.ecore"),
			of("List.xmi")
		);
	}

	@Test
	public void testReferenceToClassUnidirectional() throws IOException {
		var subdir = "referenceToClassUnidirectional/";

		var modelMigrator = setupMigrator(
			subdir,
			of("PersonList.ecore"),
			of("List.xmi")
		);

		var personWorks = getReference(evolvingModelManager,
				"PersonList", "Person", "works");
		// refactoring
		referenceToClass(modelMigrator, personWorks, "WorkingPosition");

		copyModelsSaveAndAssertOutputs(
			modelMigrator,
			subdir,
			subdir,
			of("PersonList.ecore"),
			of("List.xmi")
		);
	}

	@Test
	public void testReferenceToClassBidirectional() throws IOException {
		var subdir = "referenceToClassBidirectional/";

		var modelMigrator = setupMigrator(
			subdir,
			of("PersonList.ecore"),
			of("List.xmi")
		);

		var personWorks = getReference(evolvingModelManager,
				"PersonList", "Person", "works");
		// refactoring
		referenceToClass(modelMigrator, personWorks, "WorkingPosition");

		// TODO: handle model migration

		copyModelsSaveAndAssertOutputs(
			modelMigrator,
			subdir,
			subdir,
			of("PersonList.ecore"),
			of("List.xmi")
		);
	}

	@Test
	public void testMakeBidirectionalExisting() throws IOException {
		var subdir = "makeBidirectionalExisting/";
	
		var modelMigrator = setupMigrator(
			subdir,
			of("PersonList.ecore"),
			of("List.xmi")
		);
	
		var personWorks = getReference(evolvingModelManager,
				"PersonList", "Person", "works");
		// refactoring
		var workPlacePerson = getReference(evolvingModelManager,
				"PersonList", "WorkPlace", "person");
		assertNotNull(workPlacePerson);
		// this should not change anything
		EdeltaUtils.makeBidirectional(personWorks, workPlacePerson);
	
		copyModelsSaveAndAssertOutputs(
			modelMigrator,
			subdir,
			subdir,
			of("PersonList.ecore"),
			of("List.xmi")
		);
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
			var copy = modelMigrator.copy(root);
			if (copy != null)
				newResource.getContents().add(copy);
		}
		modelMigrator.copyReferences();
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
		modelMigrator.addCopyMigrator(
			a ->
				modelMigrator.isRelatedTo(a, attribute),
			(feature, oldObj, newObj) -> {
				// if we come here the old attribute was set
				EdeltaEcoreUtil.setValueForFeature(
					newObj,
					attribute,
					EdeltaEcoreUtil.getValueForFeature(oldObj, feature)
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
		modelMigrator.addEAttributeMigrator(
			a ->
				modelMigrator.isRelatedTo(a, attribute),
			(feature, oldObj, oldValue) ->
				// if we come here the old attribute was set
				EdeltaEcoreUtil.unwrapCollection(
					EdeltaEcoreUtil.wrapAsCollection(oldValue)
						.stream()
						.map(singleValueTransformer)
						.collect(Collectors.toList()),
					attribute
				)
		);
	}

	private EAttribute replaceWithCopy(EdeltaModelMigrator modelMigrator, EAttribute attribute, String newName) {
		var copy = createCopy(modelMigrator, attribute);
		copy.setName(newName);
		var containingClass = attribute.getEContainingClass();
		EdeltaUtils.removeElement(attribute);
		containingClass.getEStructuralFeatures().add(copy);
		modelMigrator.addFeatureMigrator(
			f ->
				modelMigrator.isRelatedTo(f, copy),
			(feature, o, oldValue) -> copy);
		return copy;
	}

	private <T extends EObject> T createCopy(EdeltaModelMigrator modelMigrator, T o) {
		var copy = EcoreUtil.copy(o);
		modelMigrator.addAssociation(copy, o);
		return copy;
	}

	private <T extends EObject> T createSingleCopy(EdeltaModelMigrator modelMigrator, Collection<T> elements) {
		var iterator = elements.iterator();
		var copy = createCopy(modelMigrator, iterator.next());
		while (iterator.hasNext()) {
			modelMigrator.addAssociation(copy, iterator.next());
		}
		return copy;
	}

	private EStructuralFeature pullUp(EdeltaModelMigrator modelMigrator,
			EClass superClass, Collection<EStructuralFeature> features) {
		var pulledUp = createSingleCopy(modelMigrator, features);
		superClass.getEStructuralFeatures().add(pulledUp);
		EdeltaUtils.removeAllElements(features);
		// remember we must map the original metamodel element to the new one
		modelMigrator.addFeatureMigrator(
			f -> // the feature of the original metamodel
				modelMigrator.isRelatedTo(f, pulledUp),
			(feature, o, oldValue) -> { // the object of the original model
				// the result can be safely returned
				// independently from the object's class, since the
				// predicate already matched
				return pulledUp;
			}
		);
		return pulledUp;
	}

	private Collection<EStructuralFeature> pushDown(EdeltaModelMigrator modelMigrator,
			EStructuralFeature featureToPush, Collection<EClass> subClasses) {
		var pushedDownFeatures = new HashMap<EClass, EStructuralFeature>();
		for (var subClass : subClasses) {
			var pushedDown = createCopy(modelMigrator, featureToPush);
			pushedDownFeatures.put(subClass, pushedDown);
			// we add it in the very first position just to have exactly the
			// same Ecore model as the starting one of pullUpFeatures
			// but just for testing purposes: we verify that the output is
			// exactly the same as the original model of pullUpFeatures
			subClass.getEStructuralFeatures().add(0, pushedDown);
		}
		EdeltaUtils.removeElement(featureToPush);
		// remember we must compare to the original metamodel element
		modelMigrator.addFeatureMigrator(
			modelMigrator.relatesToAtLeastOneOf(pushedDownFeatures.values()),
			(feature, oldObj, newObj) -> { // the object of the original model
				// the result depends on the EClass of the original
				// object being copied, but the map was built
				// using evolved classes
				return pushedDownFeatures.get(newObj.eClass());
			}
		);
		return pushedDownFeatures.values();
	}

	/**
	 * Replaces an EReference with an EClass (with the given name, the same package
	 * as the package of the reference's containing class), updating possible
	 * opposite reference, so that a relation can be extended with additional
	 * features. The original reference will be made a containment reference, (its
	 * other properties will not be changed) to the added EClass (and made
	 * bidirectional).
	 * 
	 * For example, given
	 * 
	 * <pre>
	 *    b2    b1
	 * A <-------> C
	 * </pre>
	 * 
	 * (where the opposite "b2" might not be present) if we pass "b1" and the name
	 * "B", then the result will be
	 * 
	 * <pre>
	 *    a     b1    b2    c
	 * A <-------> B <------> C
	 * </pre>
	 * 
	 * where "b1" will be a containment reference. Note the names inferred for the
	 * new additional opposite references.
	 * 
	 * @param name      the name for the extracted class
	 * @param reference the reference to turn into a reference to the extracted
	 *                  class
	 * @return the extracted class
	 */
	private EClass referenceToClass(EdeltaModelMigrator modelMigrator,
			EReference reference, String name) {
		// checkNotContainment reference:
		// "Cannot apply referenceToClass on containment reference"
		var ePackage = reference.getEContainingClass().getEPackage();
		var extracted = EdeltaUtils.newEClass(name);
		ePackage.getEClassifiers().add(extracted);
		var extractedRef = addMandatoryReference(extracted, 
			fromTypeToFeatureName(reference.getEType()),
			reference.getEReferenceType());
		final EReference eOpposite = reference.getEOpposite();
		if ((eOpposite != null)) {
			EdeltaUtils.makeBidirectional(eOpposite, extractedRef);
		}
		reference.setEType(extracted);
		makeContainmentBidirectional(reference);
		modelMigrator.addCopyMigrator(
			f ->
				modelMigrator.isRelatedTo(f, eOpposite),
			(feature, oldObj, newObj) -> {
				// the opposite reference now changed its type
				// so we have to skip the copy or we'll have a ClassCastException
				// the bidirectionality will be implied in the next migrator
			}
		);
		modelMigrator.addCopyMigrator(
			f ->
				modelMigrator.isRelatedTo(f, reference),
			(feature, oldObj, newObj) -> { // the object of the original model
				var oldValue = oldObj.eGet(feature);
				if (oldValue == null)
					return; // it wasn't set in the original model
				// since this is NOT a containment reference
				// the referred oldValue has already been copied
				var copied = modelMigrator.get(oldValue);
				var created = EcoreUtil.create(extracted);
				// the bidirectionality is implied
				created.eSet(extractedRef, copied);
				newObj.eSet(reference, created);
			}
		);
		return extracted;
	}

	private String fromTypeToFeatureName(final EClassifier type) {
		return StringExtensions.toFirstLower(type.getName());
	}

	/**
	 * Makes the EReference, which is assumed to be already part of an EClass, a
	 * single required containment reference, adds to the referred type, which is
	 * assumed to be set, an opposite required single reference.
	 * 
	 * @param reference
	 */
	private EReference makeContainmentBidirectional(final EReference reference) {
		EdeltaUtils.makeContainment(reference);
		final EClass owner = reference.getEContainingClass();
		final EClass referredType = reference.getEReferenceType();
		EReference addedMandatoryReference = this.addMandatoryReference(referredType,
				this.fromTypeToFeatureName(owner), owner);
		EdeltaUtils.makeBidirectional(addedMandatoryReference, reference);
		return addedMandatoryReference;
	}

	private EReference addMandatoryReference(final EClass eClass, final String referenceName, final EClass type) {
		var reference = EdeltaUtils.newEReference(referenceName, type, it -> 
			EdeltaUtils.makeSingleRequired(it)
		);
		eClass.getEStructuralFeatures().add(reference);
		return reference;
	}

}
