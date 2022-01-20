/**
 * 
 */
package edelta.lib;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.log4j.Logger;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.EcoreResourceFactoryImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;

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
	private HashMap<String, Resource> ecoreToResourceMap = new LinkedHashMap<>();

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
			new XMIResourceFactoryImpl());

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
		// make sure we have a complete file URI,
		// otherwise the saved modified ecore will contain
		// wrong references (i.e., with the prefixed relative path)
		var uri = URI.createFileURI(Paths.get(path).toAbsolutePath().toString());
		// Demand load resource for this file.
		LOG.info("Loading " + path + " (URI: " + uri + ")");
		var resource = resourceSet.getResource(uri, true);
		ecoreToResourceMap.put(path, resource);
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

	private Set<Entry<String, Resource>> getResourceMapEntrySet() {
		return ecoreToResourceMap.entrySet();
	}

	/**
	 * Saves the modified EPackages as Ecore files in the specified
	 * output path.
	 * 
	 * The final path of the generated Ecore files is made of the
	 * specified outputPath and the original loaded Ecore
	 * file names.
	 * 
	 * @param outputPath
	 * @throws IOException 
	 */
	public void saveEcores(String outputPath) throws IOException {
		for (Entry<String, Resource> entry : getResourceMapEntrySet()) {
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
}
