package edelta.interpreter;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.xtext.xbase.XExpression;

import edelta.lib.EdeltaModelMigrator.AppliedOperation;
import edelta.lib.EdeltaModelMigrator.MigrationObserver;

/**
 * Keeps only the latest proven attribute application for each preview object.
 * A copy stage replaces every object and feature key by exact copier identities.
 * The trace is private to one interpretation and never retains a copier.
 * <p>
 * For example, if an uppercase rule processes {@code alpha} and {@code beta},
 * then a suffix rule processes only {@code alpha}, the final two rows point
 * to different calls even though they share an EAttribute. A rule returning
 * its input unchanged still counts as the last application.
 *
 * @author Lorenzo Bettini
 */
public class EdeltaModelMigrationTrace implements MigrationObserver {
	/**
	 * A successful write awaiting the completed copier maps for its stage.
	 * The expression is captured now, while its user callback is still active.
	 *
	 * @author Lorenzo Bettini
	 */
	private record Pending(AppliedOperation operation, XExpression expression) {}

	private final Supplier<XExpression> currentExpression;
	private final List<Pending> pending = new ArrayList<>();
	private Map<EObject, Map<EAttribute, XExpression>> current = new IdentityHashMap<>();

	/**
	 * Creates a trace whose token supplier follows the current callback scope.
	 *
	 * @param currentExpression source token supplier for the executing callback
	 */
	public EdeltaModelMigrationTrace(Supplier<XExpression> currentExpression) {
		this.currentExpression = currentExpression;
	}

	@Override
	public void applied(AppliedOperation operation) {
		// The target may still be detached during copying. Wait for copied(),
		// which can check both resource membership and the completed identity map.
		pending.add(new Pending(operation, currentExpression.get()));
	}

	@Override
	public void copied(Map<EObject, EObject> objectCopies, Map<EObject, EObject> ecoreCopies,
			List<Resource> sources, List<Resource> targets) {
		var sourceResources = identitySet(sources);
		var targetResources = identitySet(targets);
		var uniqueTargets = new IdentityHashMap<EObject, EObject>();
		var ambiguousTargets = new IdentityHashMap<EObject, Boolean>();
		// A target reached from two sources has no unique object lineage.
		for (var entry : objectCopies.entrySet()) {
			var prior = uniqueTargets.putIfAbsent(entry.getValue(), entry.getKey());
			if (prior != null)
				ambiguousTargets.put(entry.getValue(), Boolean.TRUE);
		}
		var next = new IdentityHashMap<EObject, Map<EAttribute, XExpression>>();
		// Carry earlier applications through this stage. An unrelated later rule
		// leaves each untouched pair's expression in place.
		for (var entry : current.entrySet()) {
			var source = entry.getKey();
			var target = objectCopies.get(source);
			if (!validObjects(source, target, sourceResources, targetResources,
					ambiguousTargets)) continue;
			for (var attribute : entry.getValue().entrySet()) {
				var mapped = mappedAttribute(source, attribute.getKey(), target, ecoreCopies);
				if (mapped != null)
					put(next, target, mapped, attribute.getValue());
			}
		}
		// Apply this stage's successful writes in encounter order. They replace
		// inherited tokens only for their exact mapped object/attribute pair.
		for (var event : pending) {
			var operation = event.operation();
			var source = operation.sourceObject();
			var target = operation.targetObject();
			if (objectCopies.get(source) != target || !validObjects(source, target,
					sourceResources, targetResources, ambiguousTargets)) continue;
			if (!(operation.sourceFeature() instanceof EAttribute sourceAttribute)) continue;
			var mapped = mappedAttribute(source, sourceAttribute, target, ecoreCopies);
			if (mapped == null || mapped != operation.targetFeature()) continue;
			var entries = next.get(target);
			if (entries != null) entries.remove(mapped);
			// A successful write without a trustworthy user expression cancels an
			// older link for this pair; the outline then uses its declaration.
			if (event.expression() != null)
				put(next, target, mapped, event.expression());
		}
		current = next;
		pending.clear();
	}

	@Override
	public void invalidated() {
		// Custom copy/replacement procedures can break lineage without reporting
		// which pair they affected, so their boundary clears the whole trace.
		current.clear();
		pending.clear();
	}

	/**
	 * Returns a detached identity-map snapshot for the successfully published preview.
	 *
	 * @return only current target objects and attributes
	 */
	public Map<EObject, Map<EAttribute, XExpression>> snapshot() {
		var result = new IdentityHashMap<EObject, Map<EAttribute, XExpression>>();
		for (var entry : current.entrySet())
			result.put(entry.getKey(), new IdentityHashMap<>(entry.getValue()));
		return result;
	}

	private Map<Resource, Boolean> identitySet(List<Resource> resources) {
		var result = new IdentityHashMap<Resource, Boolean>();
		for (var resource : resources) result.put(resource, Boolean.TRUE);
		return result;
	}

	private boolean validObjects(EObject source, EObject target,
			Map<Resource, Boolean> sources, Map<Resource, Boolean> targets,
			Map<EObject, Boolean> ambiguous) {
		return target != null && sources.containsKey(source.eResource())
			&& targets.containsKey(target.eResource()) && !ambiguous.containsKey(target);
	}

	private EAttribute mappedAttribute(EObject source, EAttribute attribute, EObject target,
			Map<EObject, EObject> ecoreCopies) {
		// Names and URI fragments are insufficient: require the actual copied
		// EAttribute on the target class and a set value on both sides.
		if (!source.eIsSet(attribute)) return null;
		var mapped = ecoreCopies.get(attribute);
		if (!(mapped instanceof EAttribute targetAttribute)
				|| target.eClass().getEAllAttributes().stream().noneMatch(a -> a == targetAttribute)
				|| !target.eIsSet(targetAttribute))
			return null;
		return targetAttribute;
	}

	private void put(Map<EObject, Map<EAttribute, XExpression>> map, EObject object,
			EAttribute feature, XExpression expression) {
		map.computeIfAbsent(object, ignored -> new IdentityHashMap<>()).put(feature, expression);
	}
}
