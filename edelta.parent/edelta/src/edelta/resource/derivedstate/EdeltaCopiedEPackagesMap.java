package edelta.resource.derivedstate;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.emf.ecore.ENamedElement;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;

import com.google.common.collect.HashBiMap;

/**
 * Map of copied {@link EPackage}.
 * 
 * @author Lorenzo Bettini
 *
 */
public class EdeltaCopiedEPackagesMap {

	private final Map<String, EPackage> packagesByName = new HashMap<>();
	private Map<EObject, EObject> originalToCopy = Map.of();
	private Map<EObject,EObject> inverse;

	public void setCopies(Collection<EPackage> copies, Map<EObject, EObject> copyMap) {
		for (var copy : copies) {
			packagesByName.computeIfAbsent(copy.getName(), key -> copy);
		}
		originalToCopy = Map.copyOf(copyMap);
		inverse = HashBiMap.create(originalToCopy).inverse();
	}

	/**
	 * Returns an immutable snapshot of the original-to-copied Ecore element map.
	 * Its values are the exact objects stored in this derived state; callers must
	 * not detach or replace them. A later {@link #setCopies(Collection, Map)} may
	 * install a different map without changing this snapshot.
	 *
	 * @return the original-to-copied Ecore element map
	 */
	public Map<EObject, EObject> getOriginalToCopyMap() {
		return originalToCopy;
	}

	public Collection<EPackage> values() {
		return packagesByName.values();
	}

	public void clear() {
		packagesByName.clear();
		originalToCopy = Map.of();
		inverse = null;
	}

	public EPackage get(String name) {
		return packagesByName.get(name);
	}

	public ENamedElement getOriginal(ENamedElement e) {
		if (e == null || inverse == null)
			return null;
		return (ENamedElement) inverse.get(e);
	}

	/** Returns the imported source of a copied object, or null for a newly created object. */
	public EObject getOriginal(EObject e) {
		return inverse == null ? null : inverse.get(e);
	}
}
