/**
 * 
 */
package edelta.lib;

import static java.util.Comparator.comparing;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import org.eclipse.emf.common.util.URI;
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

	/**
	 * Returns all the {@link EPackage} instances found as first element
	 * of the given {@link Resource}s.
	 * 
	 * @param resources
	 * @return
	 */
	public static Stream<EPackage> getEPackages(Collection<Resource> resources) {
		return getEPackagesStream(resources); // we must be deterministic
	}

	/**
	 * Returns a stream with all the {@link EPackage} instances found as first
	 * element of the given {@link Resource}s.
	 * 
	 * @param resources
	 * @return
	 */
	public static Stream<EPackage> getEPackagesStream(Collection<Resource> resources) {
		return resources.stream()
			.map(EdeltaResourceUtils::getEPackage)
			.filter(Objects::nonNull)
			.sorted(ePackageComparator());
	}

	/**
	 * Return the first element of the {@link Resource} as an {@link EPackage} or
	 * null otherwise (for example, the resource is empty or the first element is
	 * not an {@link EPackage}),
	 * 
	 * @param r
	 * @return
	 */
	public static EPackage getEPackage(Resource r) {
		var contents = r.getContents();
		if (contents.isEmpty())
			return null;
		var first = contents.get(0);
		if (!(first instanceof EPackage))
			return null;
		return (EPackage) first;
	}

	/**
	 * Compares the {@link EPackage} by their {@link EPackage#getNsURI()}.
	 * 
	 * @return
	 */
	public static Comparator<EPackage> ePackageComparator() {
		return comparing(EPackage::getNsURI);
	}

	/**
	 * Returns the file name of the given {@link Resource}
	 * 
	 * @param resource
	 * @return
	 */
	public static String getFileName(Resource resource) {
		var resourceURI = resource.getURI();
		var resourcePath = Paths.get(resourceURI.toFileString());
		return resourcePath.getFileName().toString();
	}

	/**
	 * Gets the relative path of a resource from a base path.
	 *
	 * @param resource The EMF resource
	 * @param basePath The base path
	 * @return The relative path, or the full path if no base path matches
	 */
	public static String getRelativePath(Resource resource, String basePath) {
		return getRelativePath(resource, List.of(basePath));
	}

	/**
	 * Gets the relative path of a resource from one of the provided base paths.
	 *
	 * @param resource  The EMF resource
	 * @param basePaths Collection of possible base paths
	 * @return The relative path, or the full path if no base path matches
	 */
	public static String getRelativePath(Resource resource, Collection<String> basePaths) {
		URI resourceURI = resource.getURI();
		String resourcePath = resourceURI.toString();

		// Ensure base paths are absolute and normalized
		basePaths = basePaths.stream()
			.map(path -> Paths.get(path).toAbsolutePath().normalize().toString())
			.toList();

		// Sort base paths by length (descending) to match the most specific path first
		List<String> sortedPaths = new ArrayList<>(basePaths);
		sortedPaths.sort((a, b) -> Integer.compare(b.length(), a.length()));

		for (String basePath : sortedPaths) {
			URI baseURI = URI.createFileURI(basePath);
			String baseURIString = baseURI.toString();

			if (resourcePath.startsWith(baseURIString)) {
				return resourceURI.deresolve(baseURI).path();
			}
		}

		// If no base path matches, return the full path
		return resourceURI.path();
	}
}
