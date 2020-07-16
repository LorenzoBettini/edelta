package edelta.util;

import static com.google.common.collect.Iterables.filter;
import static java.util.Collections.emptyList;
import static org.eclipse.xtext.EcoreUtil2.eAllOfType;

import java.util.Collection;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.emf.ecore.ENamedElement;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EOperation;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.xtext.naming.IQualifiedNameProvider;
import org.eclipse.xtext.util.IResourceScopeCache;

import com.google.common.base.Predicate;
import com.google.inject.Inject;

import edelta.lib.EdeltaEcoreUtil;
import edelta.resource.derivedstate.EdeltaAccessibleElement;
import edelta.resource.derivedstate.EdeltaAccessibleElements;
import edelta.resource.derivedstate.EdeltaDerivedStateHelper;

/**
 * Helper methods for accessing Ecore elements.
 * 
 * @author Lorenzo Bettini
 */
public class EdeltaEcoreHelper {
	@Inject
	private IResourceScopeCache cache;

	@Inject
	private EdeltaDerivedStateHelper derivedStateHelper;

	@Inject
	private IQualifiedNameProvider qualifiedNameProvider;

	/**
	 * Returns all the ENamedElements in the program: it uses the copied EPackages
	 * if present, otherwise it uses the original imported metamodels, but NOT both.
	 */
	public Collection<ENamedElement> getProgramENamedElements(final EObject context) {
		return cache.get("getProgramENamedElements", context.eResource(), () -> {
			final var epackages = getEPackagesToProcess(context);
			return epackages.stream()
				.flatMap(this::getAllENamedElements)
				.collect(Collectors.toList());
		});
	}

	public EdeltaAccessibleElements computeAccessibleElements(EObject context) {
		return cache.get("computeAccessibleElements", context.eResource(), () -> {
			final var epackages = getEPackagesToProcess(context);
			final var snapshot = EdeltaEcoreUtil.copyEPackages(epackages).stream()
				.flatMap(this::getAllENamedElements)
				.map(it -> new EdeltaAccessibleElement(it,
						qualifiedNameProvider.getFullyQualifiedName(it)))
				// qualified name is null for unresolved proxies
				.filter(it -> Objects.nonNull(it.getQualifiedName()))
				.collect(Collectors.toList());
			return new EdeltaAccessibleElements(snapshot);
		});
	}

	private Collection<EPackage> getEPackagesToProcess(final EObject context) {
		final var prog = EdeltaModelUtil.getProgram(context);
		final var copied = derivedStateHelper.getCopiedEPackagesMap(prog.eResource())
				.values();
		// copied EPackage are present only when there's at least one modifyEcore
		return copied.isEmpty() ? prog.getMetamodels() : copied;
	}

	private Stream<ENamedElement> getAllENamedElements(final EPackage e) {
		return eAllOfType(e, ENamedElement.class).stream()
				.filter(filterENamedElement());
	}

	/**
	 * Returns the {@link ENamedElement}s directly contained in the passed
	 * {@link ENamedElement}.
	 * 
	 * @param e
	 * @return
	 */
	public Iterable<ENamedElement> getENamedElements(final ENamedElement e) {
		if (e == null) {
			return emptyList();
		}
		return filter(
			filter(e.eContents(), ENamedElement.class),
			filterENamedElement());
	}

	private Predicate<? super ENamedElement> filterENamedElement() {
		// see https://github.com/LorenzoBettini/edelta/issues/220
		return el -> !(el instanceof EOperation);
	}

}