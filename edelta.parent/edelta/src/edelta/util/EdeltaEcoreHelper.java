package edelta.util;

import static com.google.common.collect.Iterables.filter;
import static java.util.Collections.emptyList;
import static org.eclipse.xtext.EcoreUtil2.eAllOfType;

import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.emf.ecore.ENamedElement;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.xtext.util.IResourceScopeCache;

import com.google.inject.Inject;

import edelta.resource.derivedstate.EdeltaDerivedStateHelper;

/**
 * Helper methods for accessing Ecore elements.
 * 
 * Computations that require filtering or mapping are cached in the scope of the
 * resource.
 * 
 * @author Lorenzo Bettini
 */
public class EdeltaEcoreHelper {
	@Inject
	private IResourceScopeCache cache;

	@Inject
	private EdeltaDerivedStateHelper derivedStateHelper;

	/**
	 * Returns all the ENamedElements in the program: it uses the copied EPackages
	 * if present, otherwise it uses the original imported metamodels, but NOT both.
	 */
	public Iterable<ENamedElement> getProgramENamedElements(final EObject context) {
		return cache.get("getProgramENamedElements", context.eResource(), () -> {
			final var prog = EdeltaModelUtil.getProgram(context);
			final var copied = derivedStateHelper.getCopiedEPackagesMap(prog.eResource())
					.values();
			// copied EPackage are present only when there's at least one modifyEcore
			final var epackages =
				copied.isEmpty() ? prog.getMetamodels() : copied;
			return epackages.stream()
				.flatMap(p -> getAllENamedElements(p).stream())
				.collect(Collectors.toList());
		});
	}

	/**
	 * Returns all ENamedElements of the passed EPackage, recursively, including
	 * subpackages and getAllENamedElements on each subpackage
	 */
	private List<ENamedElement> getAllENamedElements(final EPackage e) {
		return eAllOfType(e, ENamedElement.class);
	}

	public Iterable<ENamedElement> getENamedElements(final ENamedElement e) {
		if (e == null) {
			return emptyList();
		}
		return filter(e.eContents(), ENamedElement.class);
	}
}
