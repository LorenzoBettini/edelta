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
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.xtext.naming.IQualifiedNameProvider;
import org.eclipse.xtext.util.IResourceScopeCache;

import com.google.common.base.Predicate;
import com.google.inject.Inject;

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
	 * Returns all the {@link ENamedElement} in the context's program: it uses the
	 * copied EPackages if present, otherwise it uses the original imported
	 * metamodels, but NOT both.
	 * 
	 * @param context
	 * @return
	 */
	public Iterable<ENamedElement> getProgramENamedElements(final EObject context) {
		return cache.get("getProgramENamedElements", context.eResource(), () -> {
			final var epackages = getCurrentEPackagesToProcess(context);
			return epackages.stream()
				.flatMap(this::getAllENamedElements)
				.toList();
		});
	}

	/**
	 * Creates a snapshot of {@link EdeltaAccessibleElements} in the specified
	 * context; elements of the snapshots are copies.
	 * 
	 * @param context
	 * @return
	 */
	public EdeltaAccessibleElements createSnapshotOfAccessibleElements(EObject context) {
		return cache.get("createSnapshotOfAccessibleElements", context.eResource(),
			() -> fromEPackagesToAccessibleElements
				(EcoreUtil.copyAll(getCurrentEPackagesToProcess(context))));
	}

	/**
	 * Returns {@link EdeltaAccessibleElements} using the current state of the
	 * copied {@link EPackage}s, that is, the current state of the metamodels where
	 * interpreted operations have been applied.
	 * 
	 * @param context
	 * @return
	 */
	public EdeltaAccessibleElements getCurrentAccessibleElements(EObject context) {
		return cache.get("getCurrentAccessibleElements", context.eResource(),
			() -> fromEPackagesToAccessibleElements(getCurrentEPackagesToProcess(context)));
	}

	/**
	 * Returns {@link EdeltaAccessibleElements} using the passed copied
	 * {@link EPackage}s.
	 * 
	 * @param epackages
	 * @return
	 */
	public EdeltaAccessibleElements fromEPackagesToAccessibleElements(final Collection<EPackage> epackages) {
		return epackages.stream()
			.flatMap(this::getAllENamedElements)
			.map(it -> new EdeltaAccessibleElement(it,
					qualifiedNameProvider.getFullyQualifiedName(it)))
			// qualified name is null for unresolved proxies
			.filter(it -> Objects.nonNull(it.getQualifiedName()))
			.collect(Collectors.toCollection(EdeltaAccessibleElements::new));
	}

	private Collection<EPackage> getCurrentEPackagesToProcess(final EObject context) {
		final var prog = EdeltaModelUtil.getProgram(context);
		final var copied = derivedStateHelper.getCopiedEPackagesMap(prog.eResource())
				.values();
		// copied EPackage are present only when there's at least one modifyEcore
		return copied.isEmpty() ? EdeltaModelUtil.getMetamodels(prog) : copied;
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
