package edelta.ui.outline;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;

/** Collects set, non-derived, non-containment references of an Ecore object for the outline. */
public class EdeltaEcoreReferenceTargets {

	/** Returns targets in reference declaration order, preserving multi-value order. */
	public List<EObject> get(final EObject source) {
		final var targets = new ArrayList<EObject>();
		for (final EReference reference : source.eClass().getEAllReferences()) {
			if (reference.isContainment() || reference.isDerived() || !source.eIsSet(reference)) {
				continue;
			}
			final var value = source.eGet(reference);
			if (reference.isMany()) {
				((Collection<?>) value).stream().map(EObject.class::cast).forEach(targets::add);
			} else {
				Stream.ofNullable((EObject) value).forEach(targets::add);
			}
		}
		return targets;
	}
}
