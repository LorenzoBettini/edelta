package edelta.lib.tests;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;

/**
 * Test-only probe for one completed copy stage. The caller supplies exact copier
 * identities and a proven feature pair. It does not resolve proxies, normalize
 * datatype values, compare unordered lists, or infer remapped features.
 *
 * @author Lorenzo Bettini
 */
final class StageValueComparator {
	enum Result { EQUAL, DIFFERENT, UNKNOWN }

	private StageValueComparator() {}

	static Result compare(EObject source, EStructuralFeature sourceFeature,
			EObject target, EStructuralFeature targetFeature,
			Map<EObject, EObject> objects, Map<EObject, EObject> ecores) {
		if (source == null || target == null || sourceFeature == null || targetFeature == null
				|| objects.get(source) != target || ecores.get(source.eClass()) != target.eClass()
				|| ecores.get(sourceFeature) != targetFeature
				|| !source.eClass().getEAllStructuralFeatures().contains(sourceFeature)
				|| !target.eClass().getEAllStructuralFeatures().contains(targetFeature)
				|| !uniqueTarget(target, objects)) return Result.UNKNOWN;
		if (sourceFeature instanceof EAttribute && targetFeature instanceof EAttribute) {
			return values(source.eGet(sourceFeature), target.eGet(targetFeature), false, objects, ecores,
				sourceFeature.isOrdered() && targetFeature.isOrdered());
		}
		if (sourceFeature instanceof EReference && targetFeature instanceof EReference) {
			return values(source.eGet(sourceFeature, false), target.eGet(targetFeature, false), true,
				objects, ecores, sourceFeature.isOrdered() && targetFeature.isOrdered());
		}
		return Result.UNKNOWN;
	}

	private static boolean uniqueTarget(EObject target, Map<EObject, EObject> objects) {
		var count = 0;
		for (var copy : objects.values()) if (copy == target && ++count > 1) return false;
		return count == 1;
	}

	private static Result values(Object source, Object target, boolean reference,
			Map<EObject, EObject> objects, Map<EObject, EObject> ecores, boolean ordered) {
		if (source instanceof List<?> left && target instanceof List<?> right) {
			if (!ordered) return Result.UNKNOWN;
			if (left.size() != right.size()) return Result.DIFFERENT;
			var result = Result.EQUAL;
			for (var index = 0; index < left.size(); index++) {
				var element = scalar(left.get(index), right.get(index), reference, objects, ecores);
				if (element == Result.UNKNOWN) result = Result.UNKNOWN;
				else if (element == Result.DIFFERENT && result != Result.UNKNOWN) result = Result.DIFFERENT;
			}
			return result;
		}
		if (source instanceof List<?> || target instanceof List<?>) return Result.DIFFERENT;
		return scalar(source, target, reference, objects, ecores);
	}

	private static Result scalar(Object source, Object target, boolean reference,
			Map<EObject, EObject> objects, Map<EObject, EObject> ecores) {
		if (!reference) return Objects.equals(source, target) ? Result.EQUAL : Result.DIFFERENT;
		if (source == null || target == null)
			return source == target ? Result.EQUAL : Result.DIFFERENT;
		if (!(source instanceof EObject before) || !(target instanceof EObject after)
				|| before.eIsProxy() || after.eIsProxy()) return Result.UNKNOWN;
		var mapped = objects.get(before);
		if (mapped == null || !uniqueTarget(mapped, objects)
				|| ecores.get(before.eClass()) != mapped.eClass()) return Result.UNKNOWN;
		return mapped == after ? Result.EQUAL : Result.DIFFERENT;
	}
}
