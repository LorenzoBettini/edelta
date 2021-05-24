/**
 * 
 */
package edelta.lib;

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
	 */
	public void loadEcoreFile(String path) {
		// make sure we have a complete file URI,
		// otherwise the saved modified ecore will contain
		// wrong references (i.e., with the prefixed relative path)
		var uri = URI.createFileURI(Paths.get(path).toAbsolutePath().toString());
		// Demand load resource for this file.
		LOG.info("Loading " + path + " (URI: " + uri + ")");
		ecoreToResourceMap.put(path, resourceSet.getResource(uri, true));
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
		return resourceSet.getResources().
			stream().
			map(resource -> resource.getContents().get(0)).
			filter(EPackage.class::isInstance).
			map(EPackage.class::cast).
			filter(p -> p.getName().equals(packageName)).
			findAny().
			orElse(null);
	}

	public Set<Entry<String, Resource>> getResourceMapEntrySet() {
		return ecoreToResourceMap.entrySet();
	}
}
