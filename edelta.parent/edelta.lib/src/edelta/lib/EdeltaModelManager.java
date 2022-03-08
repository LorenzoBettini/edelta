package edelta.lib;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import org.apache.log4j.Logger;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
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

/**
 * Loads Ecore files and model files, and provides some methods.
 * 
 * @author Lorenzo Bettini
 *
 */
public class EdeltaModelManager {

	private static final Logger LOG = Logger.getLogger(EdeltaModelManager.class);

	/**
	 * Here we store loaded Ecore Resources.
	 */
	private Collection<Resource> ecoreResources = new ArrayList<>();

	/**
	 * Here we store the loaded model Resources.
	 */
	private Collection<Resource> modelResources = new ArrayList<>();

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
		return loadResource(path, ecoreResources);
	}

	private Resource loadResource(String path, Collection<Resource> resourceMap) {
		var uri = createAbsoluteFileURI(path);
		// Demand load resource for this file.
		LOG.info("Loading " + path + " (URI: " + uri + ")");
		var resource = resourceSet.getResource(uri, true);
		resourceMap.add(resource);
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
		return EdeltaResourceUtils.getEPackages(ecoreResources)
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
		saveResources(outputPath, ecoreResources);
	}

	private void saveResources(String outputPath, Collection<Resource> resourceMap) throws IOException {
		for (var resource : resourceMap) {
			var fileName = EdeltaResourceUtils.getFileName(resource);
			LOG.info("Saving " + outputPath + "/" + fileName);
			var newFile = new File(outputPath, fileName);
			newFile.getParentFile().mkdirs();
			var fos = new FileOutputStream(newFile);
			resource.save(fos, null);
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
		return loadResource(path, modelResources);
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
		saveResources(outputPath, modelResources);
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
		return createResource(path, prototypeResource, ecoreResources);
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
		return createResource(path, prototypeResource, modelResources);
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
	private Resource createResource(String path, XMIResource prototypeResource, Collection<Resource> resourceMap) {
		var uri = createAbsoluteFileURI(path);
		LOG.info("Creating " + path + " (URI: " + uri + ")");
		var resource = (XMIResource) resourceSet.createResource(uri);
		resource.getDefaultLoadOptions().putAll(prototypeResource.getDefaultLoadOptions());
		resource.getDefaultSaveOptions().putAll(prototypeResource.getDefaultSaveOptions());
		resource.setEncoding(prototypeResource.getEncoding());
		resourceMap.add(resource);
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

	public Collection<Resource> getModelResources() {
		return modelResources;
	}

	public Collection<Resource> getEcoreResources() {
		return ecoreResources;
	}

	public Map<EObject, EObject> copyEcores(EdeltaModelManager otherModelManager, String basedir) {
		var otherEcoreResources = otherModelManager.getEcoreResources();
		var ecoreCopier = new Copier();
		for (var resource : otherEcoreResources) {
			var originalResource = (XMIResource) resource;
			var fileName = EdeltaResourceUtils.getFileName(originalResource);
			var newResource = this.createEcoreResource
				(basedir + fileName, originalResource);
			var root = originalResource.getContents().get(0);
			newResource.getContents().add(ecoreCopier.copy(root));
		}
		ecoreCopier.copyReferences();
		return ecoreCopier;
	}

	public void clearModels() {
		var models = getModelResources();
		for (var resource : models) {
			resource.getResourceSet()
				.getResources().remove(resource);
		}
		models.clear();
	}
}