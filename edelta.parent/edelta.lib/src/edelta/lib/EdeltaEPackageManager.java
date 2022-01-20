/**
 * 
 */
package edelta.lib;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.XMLResource;
import org.eclipse.emf.ecore.xmi.impl.EcoreResourceFactoryImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceImpl;

/**
 * Loads ecore files and corresponding {@link EPackage} elements.
 * 
 * @author Lorenzo Bettini
 *
 */
public class EdeltaEPackageManager {

	private static final Logger LOG = Logger.getLogger(EdeltaEPackageManager.class);

	/**
	 * Here we store the association between the Ecore file name and the
	 * corresponding loaded Resource.
	 */
	private Map<String, Resource> ecoreToResourceMap = new LinkedHashMap<>();

	/**
	 * Here we store the association between the model file name and the
	 * corresponding loaded Resource.
	 */
	private Map<String, Resource> modelToResourceMap = new LinkedHashMap<>();

	/**
	 * Here we store all the Ecores used by the Edelta
	 */
	private ResourceSet resourceSet = new ResourceSetImpl();

	/**
	 * Performs EMF initialization (resource factories and package registry)
	 */
	public EdeltaEPackageManager() {
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
		return loadResource(path, ecoreToResourceMap);
	}

	private Resource loadResource(String path, Map<String, Resource> resourceMap) {
		// make sure we have a complete file URI,
		// otherwise the saved modified files will contain
		// wrong references (i.e., with the prefixed relative path)
		var uri = URI.createFileURI(Paths.get(path).toAbsolutePath().toString());
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
		return EdeltaResourceUtils.getEPackages(ecoreToResourceMap.values())
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
		saveResources(outputPath, ecoreToResourceMap);
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
		return loadResource(path, modelToResourceMap);
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
		saveResources(outputPath, modelToResourceMap);
	}
}
