package edelta.tests;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EcoreFactory;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.impl.DynamicEObjectImpl;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.impl.ResourceImpl;
import org.eclipse.xtext.xbase.XExpression;
import org.eclipse.xtext.xbase.XbaseFactory;
import org.junit.Test;

import edelta.interpreter.EdeltaModelMigrationTrace;
import edelta.lib.EdeltaModelMigrator.CopyStage;

/**
 * Direct migration-trace tests with fresh EObject/Ecore identities at every stage.
 *
 * @author Lorenzo Bettini
 */
public class EdeltaModelMigrationTraceTest {
	private record Stage(Resource resource, EClass type, EAttribute attribute, EReference link,
			EObject first, EObject second) {}

	@Test
	public void equalCopyRulePreservesEarlierChange() {
		var firstCall = XbaseFactory.eINSTANCE.createXStringLiteral();
		var laterCall = XbaseFactory.eINSTANCE.createXStringLiteral();
		var token = new AtomicReference<XExpression>(firstCall);
		var trace = new EdeltaModelMigrationTrace(token::get);
		var original = stage("hello", "HELLO");
		var upper = stage("HELLO", "HELLO");
		complete(trace, copy(original, upper, true));
		assertThat(expression(trace, upper.first(), upper.attribute())).isSameAs(firstCall);
		assertThat(expression(trace, upper.second(), upper.attribute())).isNull();

		var backup = stage("HELLO", "HELLO");
		var backupCopy = copy(upper, backup, false);
		trace.copied(backupCopy);
		trace.completed(backupCopy);
		var equal = stage("HELLO", "HELLO");
		token.set(laterCall);
		// An equal later stage must preserve the earlier value-changing call.
		complete(trace, copy(backup, equal, true));
		assertThat(expression(trace, equal.first(), equal.attribute())).isSameAs(firstCall);

		var reversed = stage("hello", "HELLO");
		complete(trace, copy(equal, reversed, true));
		assertThat(expression(trace, reversed.first(), reversed.attribute())).isSameAs(laterCall);
		var finalCopy = stage("hello", "HELLO");
		trace.copied(copy(reversed, finalCopy, false));
		assertThat(expression(trace, finalCopy.first(), finalCopy.attribute()))
			.isSameAs(laterCall);
	}

	@Test
	public void unknownFeatureRemapAndMergedReplacementDropOnlyProvenanceTheyDestroy() {
		var call = XbaseFactory.eINSTANCE.createXStringLiteral();
		var trace = new EdeltaModelMigrationTrace(() -> call);
		var original = stage("one", "two");
		var changed = stage("ONE", "TWO");
		complete(trace, copy(original, changed, true));
		var remapped = stage("ONE", "TWO");
		var noFeatureMap = new CopyStage(Map.of(changed.first(), remapped.first(),
			changed.second(), remapped.second()),
			Map.of(changed.type(), remapped.type()),
			List.of(changed.resource()), List.of(remapped.resource()), true);
		complete(trace, noFeatureMap);
		// Matching values and feature names cannot replace a missing feature map.
		assertThat(trace.snapshot()).isEmpty();

		var from = stage("again", "TWO");
		complete(trace, copy(remapped, from, true));
		assertThat(expression(trace, from.first(), from.attribute())).isSameAs(call);
		var shared = stage("again", "TWO");
		// A many-to-one replacement has no unique owner for either old token.
		var merged = new CopyStage(Map.of(from.first(), shared.first(),
			from.second(), shared.first()), Map.of(from.type(), shared.type(),
			from.attribute(), shared.attribute()),
			List.of(from.resource()), List.of(shared.resource()), true);
		complete(trace, merged);
		assertThat(trace.snapshot()).isEmpty();
	}

	@Test
	public void aPartialStageCarriesOnlyTheMappedInstance() {
		var call = XbaseFactory.eINSTANCE.createXStringLiteral();
		var trace = new EdeltaModelMigrationTrace(() -> call);
		var source = stage("one", "two");
		var changed = stage("ONE", "TWO");
		complete(trace, copy(source, changed, true));
		var later = stage("ONE", "TWO");
		var partial = new CopyStage(Map.of(changed.second(), later.second()),
			Map.of(changed.type(), later.type(), changed.attribute(), later.attribute(),
				changed.link(), later.link()),
			List.of(changed.resource()), List.of(later.resource()), true);
		complete(trace, partial);
		assertThat(expression(trace, later.first(), later.attribute())).isNull();
		assertThat(expression(trace, later.second(), later.attribute())).isSameAs(call);
	}

	@Test
	public void differentValueWithoutUserTokenAndStaleCompletionNeverGuess() {
		var call = XbaseFactory.eINSTANCE.createXStringLiteral();
		var token = new AtomicReference<XExpression>(call);
		var trace = new EdeltaModelMigrationTrace(token::get);
		var first = stage("one", "two");
		var changed = stage("ONE", "two");
		complete(trace, copy(first, changed, true));
		var later = stage("again", "two");
		token.set(null);
		complete(trace, copy(changed, later, true));
		assertThat(trace.snapshot()).isEmpty();
		var stale = copy(later, stage("stale", "two"), true);
		trace.copied(stale);
		var newest = copy(later, stage("new", "two"), true);
		trace.copied(newest);
		trace.completed(stale);
		assertThat(trace.snapshot()).isEmpty();
		trace.completed(newest);
		assertThat(trace.snapshot()).isEmpty();
	}

	@Test
	public void referenceTokenSurvivesRemovalOfAnUnattributedAttributeToken() {
		var call = XbaseFactory.eINSTANCE.createXStringLiteral();
		var token = new AtomicReference<XExpression>(call);
		var trace = new EdeltaModelMigrationTrace(token::get);
		var original = stage("one", "two");
		var changed = stage("ONE", "two");
		original.first().eSet(original.link(), original.second());
		changed.first().eSet(changed.link(), changed.first());
		complete(trace, copy(original, changed, true));
		assertThat(expression(trace, changed.first(), changed.link())).isSameAs(call);
		var later = stage("again", "two");
		later.first().eSet(later.link(), later.first());
		token.set(null);
		complete(trace, copy(changed, later, true));
		assertThat(expression(trace, later.first(), later.attribute())).isNull();
		assertThat(expression(trace, later.first(), later.link())).isSameAs(call);
	}

	@Test
	public void backupDifferenceCannotBecomeAUserModification() {
		var call = XbaseFactory.eINSTANCE.createXStringLiteral();
		var trace = new EdeltaModelMigrationTrace(() -> call);
		var original = stage("one", "two");
		var changed = stage("ONE", "two");
		complete(trace, copy(original, changed, true));
		var inconsistentBackup = stage("different", "two");
		trace.copied(copy(changed, inconsistentBackup, false));
		assertThat(trace.snapshot()).isEmpty();
	}

	@Test
	public void unstagedDirectWriteClearsLineageAndSnapshotIsDetached() {
		var call = XbaseFactory.eINSTANCE.createXStringLiteral();
		var trace = new EdeltaModelMigrationTrace(() -> call);
		var original = stage("one", "two");
		var changed = stage("ONE", "two");
		complete(trace, copy(original, changed, true));
		var snapshot = trace.snapshot();
		assertThat(expression(trace, changed.first(), changed.attribute())).isSameAs(call);
		snapshot.get(changed.first()).clear();
		assertThat(expression(trace, changed.first(), changed.attribute())).isSameAs(call);
		trace.lineageInvalidated();
		assertThat(trace.snapshot()).isEmpty();
	}

	@Test
	public void nonComparableReferenceKindsAndMissingResourcesNeverGainTokens() {
		var call = XbaseFactory.eINSTANCE.createXStringLiteral();
		var trace = new EdeltaModelMigrationTrace(() -> call);
		var source = stage("one", "two");
		var target = stage("one", "two");
		source.link().setContainment(true);
		complete(trace, copy(source, target, true));
		assertThat(trace.snapshot()).isEmpty();
		var containerSource = stage("one", "two");
		var opposite = EcoreFactory.eINSTANCE.createEReference();
		opposite.setEType(containerSource.type());
		opposite.setContainment(true);
		containerSource.link().setEOpposite(opposite);
		complete(trace, copy(containerSource, stage("one", "two"), true));
		assertThat(trace.snapshot()).isEmpty();

		var missingSource = copy(stage("one", "two"), stage("ONE", "two"), true);
		complete(trace, new CopyStage(missingSource.objectCopies(),
			missingSource.ecoreCopies(), List.of(), missingSource.targets(), true));
		var missingTarget = copy(stage("one", "two"), stage("ONE", "two"), true);
		complete(trace, new CopyStage(missingTarget.objectCopies(),
			missingTarget.ecoreCopies(), missingTarget.sources(), List.of(), true));
		assertThat(trace.snapshot()).isEmpty();
	}

	private void complete(EdeltaModelMigrationTrace trace, CopyStage stage) {
		trace.copied(stage);
		trace.completed(stage);
	}

	private CopyStage copy(Stage source, Stage target, boolean userOperation) {
		return new CopyStage(Map.of(source.first(), target.first(),
			source.second(), target.second()),
			Map.of(source.type(), target.type(), source.attribute(), target.attribute(),
				source.link(), target.link()),
			List.of(source.resource()), List.of(target.resource()), userOperation);
	}

	private XExpression expression(EdeltaModelMigrationTrace trace, EObject object,
			EStructuralFeature feature) {
		var entries = trace.snapshot().get(object);
		return entries == null ? null : entries.get(feature);
	}

	private Stage stage(String firstValue, String secondValue) {
		var pack = EcoreFactory.eINSTANCE.createEPackage();
		var type = EcoreFactory.eINSTANCE.createEClass();
		var attribute = EcoreFactory.eINSTANCE.createEAttribute();
		var link = EcoreFactory.eINSTANCE.createEReference();
		attribute.setName("value");
		attribute.setEType(EcorePackage.Literals.ESTRING);
		link.setName("link");
		link.setEType(type);
		type.getEStructuralFeatures().addAll(List.of(attribute, link));
		pack.getEClassifiers().add(type);
		new ResourceImpl().getContents().add(pack);
		var first = new DynamicEObjectImpl(type);
		var second = new DynamicEObjectImpl(type);
		first.eSet(attribute, firstValue);
		second.eSet(attribute, secondValue);
		var resource = new ResourceImpl();
		resource.getContents().addAll(List.of(first, second));
		return new Stage(resource, type, attribute, link, first, second);
	}
}
