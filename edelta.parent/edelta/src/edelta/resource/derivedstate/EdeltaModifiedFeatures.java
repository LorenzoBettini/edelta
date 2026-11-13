package edelta.resource.derivedstate;

import java.util.IdentityHashMap;
import java.util.Map;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.xtext.xbase.XExpression;

/** Records the last expression that changed each feature of an evolved Ecore object. */
public class EdeltaModifiedFeatures {

	/** Whether the value was set before the new object entered the evolved metamodel. */
	public record Change(XExpression expression, boolean initial) {}

	private final Map<EObject, Map<EStructuralFeature, Change>> changes = new IdentityHashMap<>();
	private final Map<EObject, Boolean> initialized = new IdentityHashMap<>();

	public void record(EObject object, EStructuralFeature feature, XExpression expression) {
		if (expression != null) {
			changes.computeIfAbsent(object, ignored -> new IdentityHashMap<>())
				.put(feature, new Change(expression, false));
		}
	}

	/** Records observable initial values only once, including values on contained objects. */
	public void recordInitial(EObject object, XExpression expression) {
		if (expression == null || initialized.putIfAbsent(object, Boolean.TRUE) != null) {
			return;
		}
		for (var feature : object.eClass().getEAllStructuralFeatures()) {
			if (!feature.isDerived() && !feature.isTransient() && object.eIsSet(feature)) {
				changes.computeIfAbsent(object, ignored -> new IdentityHashMap<>())
					.putIfAbsent(feature, new Change(expression, true));
			}
		}
		object.eContents().forEach(child -> recordInitial(child, expression));
	}

	public Change get(EObject object, EStructuralFeature feature) {
		var objectChanges = changes.get(object);
		return objectChanges == null ? null : objectChanges.get(feature);
	}

	public void clear() {
		changes.clear();
		initialized.clear();
	}
}
