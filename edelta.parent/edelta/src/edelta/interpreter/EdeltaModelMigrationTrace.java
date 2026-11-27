package edelta.interpreter;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.xtext.xbase.XExpression;

import edelta.lib.EdeltaModelMigrator.CopyStage;
import edelta.lib.EdeltaModelMigrator.MigrationObserver;
import edelta.lib.EdeltaStageValueComparator;
import edelta.lib.EdeltaStageValueComparator.Mappings;
import edelta.lib.EdeltaStageValueComparator.Result;

/**
 * Tracks the last proven value-changing expression for each exact preview
 * object and feature. Each completed user stage compares its immutable source
 * with the target after reference fixup and post-copy work. Later copies carry
 * tokens only through exact lineage; uncertain correspondence drops them.
 * The trace belongs to one interpretation and retains no earlier-stage values.
 *
 * @author Lorenzo Bettini
 */
public class EdeltaModelMigrationTrace implements MigrationObserver {
	private final Supplier<XExpression> currentExpression;
	private Map<EObject, Map<EStructuralFeature, XExpression>> expressions =
		new IdentityHashMap<>();
	private CopyStage latestStage;

	/**
	 * Creates a trace whose token supplier follows the current callback scope.
	 *
	 * @param currentExpression source token supplier for the executing callback
	 */
	public EdeltaModelMigrationTrace(Supplier<XExpression> currentExpression) {
		this.currentExpression = currentExpression;
	}

	@Override
	public void copied(CopyStage stage) {
		var mappings = new Mappings(stage.objectCopies(), stage.ecoreCopies());
		var sources = identitySet(stage.sources());
		var targets = identitySet(stage.targets());
		var next = new IdentityHashMap<EObject, Map<EStructuralFeature, XExpression>>();
		for (var entry : expressions.entrySet()) {
			var source = entry.getKey();
			var target = mappings.target(source);
			if (!exactObjects(source, target, mappings, sources, targets)) continue;
			for (var token : entry.getValue().entrySet()) {
				var feature = token.getKey();
				var mapped = mappings.feature(feature);
				var result = EdeltaStageValueComparator.compare(source, feature, target,
					mapped, mappings);
				// A non-user copy must preserve the value as well as its identity.
				if (result != Result.UNKNOWN && (stage.userOperation() || result == Result.EQUAL))
					put(next, target, mapped, token.getValue());
			}
		}
		expressions = next;
		latestStage = stage;
	}

	@Override
	public void completed(CopyStage stage) {
		// A nested callback can supersede this stage before its completion arrives.
		if (stage != latestStage || !stage.userOperation()) return;
		var mappings = new Mappings(stage.objectCopies(), stage.ecoreCopies());
		var sources = identitySet(stage.sources());
		var targets = identitySet(stage.targets());
		var expression = currentExpression.get();
		for (var entry : stage.objectCopies().entrySet()) {
			var source = entry.getKey();
			var target = entry.getValue();
			if (!exactObjects(source, target, mappings, sources, targets)) continue;
			for (var feature : source.eClass().getEAllStructuralFeatures()) {
				if (feature instanceof EReference reference &&
						(reference.isContainment() || reference.isContainer())) continue;
				var mapped = mappings.feature(feature);
				var result = EdeltaStageValueComparator.compare(source, feature, target,
					mapped, mappings);
				if (result == Result.EQUAL) continue;
				remove(target, mapped);
				if (result == Result.DIFFERENT && expression != null)
					put(expressions, target, mapped, expression);
			}
		}
		latestStage = null;
	}

	@Override
	public void lineageInvalidated() {
		expressions.clear();
		latestStage = null;
	}

	/**
	 * Returns a detached identity-map snapshot of the current object/feature tokens.
	 * Only final-copy identities should be published to a preview.
	 *
	 * @return the exact object and feature attribution map
	 */
	public Map<EObject, Map<EStructuralFeature, XExpression>> snapshot() {
		var result = new IdentityHashMap<EObject, Map<EStructuralFeature, XExpression>>();
		for (var entry : expressions.entrySet())
			result.put(entry.getKey(), new IdentityHashMap<>(entry.getValue()));
		return result;
	}

	private Map<Resource, Boolean> identitySet(List<Resource> resources) {
		var result = new IdentityHashMap<Resource, Boolean>();
		for (var resource : resources) result.put(resource, Boolean.TRUE);
		return result;
	}

	private boolean exactObjects(EObject source, EObject target, Mappings mappings,
			Map<Resource, Boolean> sources, Map<Resource, Boolean> targets) {
		return target != null && mappings.target(source) == target
			&& sources.containsKey(source.eResource()) && targets.containsKey(target.eResource());
	}

	private void remove(EObject object, EStructuralFeature feature) {
		var entries = expressions.get(object);
		if (entries != null) {
			entries.remove(feature);
			if (entries.isEmpty()) expressions.remove(object);
		}
	}

	private void put(Map<EObject, Map<EStructuralFeature, XExpression>> map,
			EObject object, EStructuralFeature feature, XExpression expression) {
		map.computeIfAbsent(object, ignored -> new IdentityHashMap<>()).put(feature, expression);
	}
}
