package edelta.resource.derivedstate;

import java.util.Collection;
import java.util.HashMap;

import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceImpl;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;

/**
 * Map of copied {@link EPackage}, where the key is the name.
 * 
 * @author Lorenzo Bettini
 *
 */
public class EdeltaCopiedEPackagesMap extends HashMap<String, EPackage> {

	private static final long serialVersionUID = 1L;

	private ResourceSet resourceSet = new ResourceSetImpl();

	public void storeCopies(Collection<EPackage> copies) {
		for (var copy : copies) {
			computeIfAbsent(copy.getName(), key -> {
				var resource = new ResourceImpl();
				resourceSet.getResources().add(resource);
				resource.getContents().add(copy);
				return copy;
			});
		}
	}

}
