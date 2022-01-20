/**
 * 
 */
package edelta.lib;

import static java.util.Comparator.comparing;

import java.util.Collection;
import java.util.Comparator;
import java.util.Objects;
import java.util.stream.Collectors;

import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;

/**
 * Static utility functions acting on {@link Resource}s.
 * 
 * @author Lorenzo Bettini
 *
 */
public class EdeltaResourceUtils {

	private EdeltaResourceUtils() {
		// empty constructor never to be called
	}

	public static Collection<EPackage> getEPackages(Collection<Resource> resources) {
		return resources.stream()
			.map(EdeltaResourceUtils::getEPackage)
			.filter(Objects::nonNull)
			.sorted(ePackageComparator()) // we must be deterministic
			.collect(Collectors.toList());
	}

	public static EPackage getEPackage(Resource r) {
		var contents = r.getContents();
		if (contents.isEmpty())
			return null;
		var first = contents.get(0);
		if (!(first instanceof EPackage))
			return null;
		return (EPackage) first;
	}

	public static Comparator<EPackage> ePackageComparator() {
		return comparing(EPackage::getNsURI);
	}
}
