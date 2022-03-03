package edelta.resource.derivedstate;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.emf.ecore.EPackage;

/**
 * Map of copied {@link EPackage}.
 * 
 * @author Lorenzo Bettini
 *
 */
public class EdeltaCopiedEPackagesMap {

	private Map<String, EPackage> packagesByName = new HashMap<>();

	public void setCopies(Collection<EPackage> copies) {
		for (var copy : copies) {
			packagesByName.computeIfAbsent(copy.getName(), key -> copy);
		}
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
}
