package edelta.lib;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;

/**
 * Reads one immutable migration source property and its completed target property.
 * Correspondence comes exclusively from one copier's identity maps; this class
 * never changes a model, resolves a proxy, or retains values from another stage.
 *
 * @author Lorenzo Bettini
 */
public final class EdeltaStageValueComparator {
	/** Comparison result when exact evidence may be absent. */
	public enum Result { EQUAL, DIFFERENT, UNKNOWN }

	/**
	 * One stage's explicit mappings and reverse uniqueness index. The caller owns
	 * the maps and keeps them stable for the synchronous comparison.
	 *
	 * @author Lorenzo Bettini
	 */
	public static final class Mappings {
		private final Map<EObject, EObject> objects;
		private final Map<EObject, EObject> ecores;
		private final Map<EObject, EObject> reverse = new IdentityHashMap<>();
		private final Map<EObject, Boolean> ambiguous = new IdentityHashMap<>();

		/** Builds the uniqueness index once for this stage. */
		public Mappings(Map<EObject, EObject> objects, Map<EObject, EObject> ecores) {
			this.objects = Objects.requireNonNull(objects);
			this.ecores = Objects.requireNonNull(ecores);
			for (var entry : objects.entrySet()) {
				var prior = reverse.putIfAbsent(entry.getValue(), entry.getKey());
				if (prior != null)
					ambiguous.put(entry.getValue(), Boolean.TRUE);
			}
		}

		/** Returns only an exact, unique target object. */
		public EObject target(EObject source) {
			var target = objects.get(source);
			return target != null && !ambiguous.containsKey(target) ? target : null;
		}

		/** Returns the exact copied Ecore feature, if present. */
		public EStructuralFeature feature(EStructuralFeature source) {
			var mapped = ecores.get(source);
			return mapped instanceof EStructuralFeature feature ? feature : null;
		}

		private boolean matchesClass(EObject source, EObject target) {
			return target(source) == target && ecores.get(source.eClass()) == target.eClass();
		}

		private boolean knownReference(EObject target) {
			var source = reverse.get(target);
			return source != null && matchesClass(source, target);
		}
	}

	private EdeltaStageValueComparator() {}

	/**
	 * Compares exact owning objects and features after target reference fixup and
	 * post-copy work. A missing/ambiguous mapping, inaccessible feature, proxy or
	 * unsupported reference shape is UNKNOWN, never a guessed difference.
	 *
	 * @return EQUAL, DIFFERENT or UNKNOWN for this one stage property
	 */
	public static Result compare(EObject source, EStructuralFeature sourceFeature,
			EObject target, EStructuralFeature targetFeature, Mappings mappings) {
		if (source == null || target == null || sourceFeature == null || targetFeature == null
				|| !mappings.matchesClass(source, target)
				|| mappings.feature(sourceFeature) != targetFeature
				|| !containsIdentity(source.eClass().getEAllStructuralFeatures(), sourceFeature)
				|| !containsIdentity(target.eClass().getEAllStructuralFeatures(), targetFeature)
				|| sourceFeature.isMany() != targetFeature.isMany()) return Result.UNKNOWN;
		try {
			if (sourceFeature instanceof EAttribute && targetFeature instanceof EAttribute)
				return values(source.eGet(sourceFeature), target.eGet(targetFeature), false,
					mappings, sourceFeature.isOrdered() && targetFeature.isOrdered());
			if (sourceFeature instanceof EReference before && targetFeature instanceof EReference after
					&& !before.isContainment() && !after.isContainment()
					&& !before.isContainer() && !after.isContainer())
				return values(source.eGet(sourceFeature, false), target.eGet(targetFeature, false),
					true, mappings, sourceFeature.isOrdered() && targetFeature.isOrdered());
		} catch (RuntimeException exception) {
			// A successful migration must not fail because optional comparison
			// cannot read a property of an evolved dynamic model.
			return Result.UNKNOWN;
		}
		return Result.UNKNOWN;
	}

	private static boolean containsIdentity(List<EStructuralFeature> features,
			EStructuralFeature feature) {
		for (var candidate : features) if (candidate == feature) return true;
		return false;
	}

	private static Result values(Object source, Object target, boolean reference,
			Mappings mappings, boolean ordered) {
		if (source instanceof List<?> left && target instanceof List<?> right) {
			if (!ordered) return Result.UNKNOWN;
			if (left.size() != right.size()) return Result.DIFFERENT;
			var result = Result.EQUAL;
			for (var index = 0; index < left.size(); index++) {
				var element = scalar(left.get(index), right.get(index), reference, mappings);
				if (element == Result.UNKNOWN) result = Result.UNKNOWN;
				else if (element == Result.DIFFERENT && result != Result.UNKNOWN)
					result = Result.DIFFERENT;
			}
			return result;
		}
		if (source instanceof List<?> || target instanceof List<?>) return Result.UNKNOWN;
		return scalar(source, target, reference, mappings);
	}

	private static Result scalar(Object source, Object target, boolean reference,
			Mappings mappings) {
		if (!reference) return Objects.equals(source, target) ? Result.EQUAL : Result.DIFFERENT;
		if (source == null && target == null) return Result.EQUAL;
		if (source != null && !(source instanceof EObject) ||
				target != null && !(target instanceof EObject)) return Result.UNKNOWN;
		var before = (EObject) source;
		var after = (EObject) target;
		if (before != null && before.eIsProxy() || after != null && after.eIsProxy())
			return Result.UNKNOWN;
		if (before != null && mappings.target(before) == null ||
				after != null && !mappings.knownReference(after)) return Result.UNKNOWN;
		return before == null || after == null || mappings.target(before) != after
			? Result.DIFFERENT : Result.EQUAL;
	}
}
