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

	private Map<String, EPackage> packagesByName = new HashMap<>();
	private Map<EObject,EObject> inverse;

	public void setCopies(Collection<EPackage> copies, Map<EObject, EObject> copyMap) {
		for (var copy : copies) {
			packagesByName.computeIfAbsent(copy.getName(), key -> copy);
		}
		inverse = HashBiMap.create(copyMap).inverse();
	}

	public Collection<EPackage> values() {
		return packagesByName.values();
	}

	public void clear() {
		packagesByName.clear();
	}

	public EPackage get(String name) {
		return packagesByName.get(name);
	}

	public ENamedElement getOriginal(ENamedElement e) {
		if (e == null)
			return null;
		return (ENamedElement) inverse.get(e);
	}
}
