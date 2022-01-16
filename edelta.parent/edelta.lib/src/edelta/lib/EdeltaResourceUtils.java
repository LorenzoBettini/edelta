/**
 * 
 */
package edelta.lib;

import static java.util.Comparator.comparing;

import java.util.Collection;
import java.util.Comparator;
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
			.sorted(ePackageComparator()) // we must be deterministic
			.collect(Collectors.toList());
	}

	public static EPackage getEPackage(Resource r) {
		return (EPackage) r.getContents().get(0);
	}

	public static Comparator<EPackage> ePackageComparator() {
		return comparing(EPackage::getNsURI);
	}
}
