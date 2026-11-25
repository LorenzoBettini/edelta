package edelta.tests;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EcoreFactory;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.impl.DynamicEObjectImpl;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.impl.ResourceImpl;
import org.eclipse.xtext.xbase.XExpression;
import org.eclipse.xtext.xbase.XbaseFactory;
import org.junit.Test;

import edelta.interpreter.EdeltaModelMigrationTrace;
import edelta.lib.EdeltaModelMigrator.AppliedOperation;

/** Exercises exact pair propagation and conservative invalidation without an editor.
 * @author Lorenzo Bettini
 */
public class EdeltaModelMigrationTraceTest {
	/** A source or output model stage. @author Lorenzo Bettini */
	private record Stage(Resource resource, EAttribute attribute, EObject first, EObject second) {}

	@Test
	public void onlyTheProcessedPairReceivesTheLaterExpressionAcrossCopies() {
		var firstExpression = XbaseFactory.eINSTANCE.createXStringLiteral();
		var secondExpression = XbaseFactory.eINSTANCE.createXStringLiteral();
		var token = new AtomicReference<XExpression>(firstExpression);
		var trace = new EdeltaModelMigrationTrace(token::get);
		var original = stage("alpha", "beta");
		var transformed = stage("ALPHA", "BETA");
		trace.applied(operation(original.first(), original.attribute(),
			transformed.first(), transformed.attribute()));
		trace.applied(operation(original.second(), original.attribute(),
			transformed.second(), transformed.attribute()));
		copy(trace, original, transformed);
		assertThat(expression(trace, transformed.first(), transformed.attribute()))
			.isSameAs(firstExpression);

		var next = stage("ALPHA!", "BETA");
		token.set(secondExpression);
		trace.applied(operation(transformed.first(), transformed.attribute(),
			next.first(), next.attribute()));
		copy(trace, transformed, next);
		assertThat(expression(trace, next.first(), next.attribute()))
			.isSameAs(secondExpression);
		assertThat(expression(trace, next.second(), next.attribute()))
			.isSameAs(firstExpression);
		assertThat(trace.snapshot()).doesNotContainKey(transformed.first());

		var finalStage = stage("ALPHA!", "BETA");
		copy(trace, next, finalStage);
		assertThat(expression(trace, finalStage.first(), finalStage.attribute()))
			.isSameAs(secondExpression);
		assertThat(expression(trace, finalStage.second(), finalStage.attribute()))
			.isSameAs(firstExpression);

		var noSource = stage("ALPHA!", "BETA");
		token.set(null);
		trace.applied(operation(finalStage.first(), finalStage.attribute(),
			noSource.first(), noSource.attribute()));
		copy(trace, finalStage, noSource);
		assertThat(expression(trace, noSource.first(), noSource.attribute())).isNull();
		assertThat(expression(trace, noSource.second(), noSource.attribute()))
			.isSameAs(firstExpression);
		trace.invalidated();
		assertThat(trace.snapshot()).isEmpty();
		var afterUnsupported = stage("ALPHA!", "BETA");
		token.set(firstExpression);
		trace.applied(operation(noSource.second(), noSource.attribute(),
			afterUnsupported.second(), afterUnsupported.attribute()));
		copy(trace, noSource, afterUnsupported);
		assertThat(expression(trace, afterUnsupported.first(), afterUnsupported.attribute()))
			.isNull();
		assertThat(expression(trace, afterUnsupported.second(), afterUnsupported.attribute()))
			.isSameAs(firstExpression);
	}

	@Test
	public void uncertainObjectOrFeatureMappingsNeverTransferAToken() {
		var expression = XbaseFactory.eINSTANCE.createXStringLiteral();
		var source = stage("one", "two");
		var target = stage("one", "two");
		var trace = seeded(expression, source);
		trace.copied(Map.of(), Map.of(source.attribute(), target.attribute()),
			List.of(source.resource()), List.of(target.resource()));
		assertThat(trace.snapshot()).isEmpty();

		trace = seeded(expression, source);
		var aliases = new IdentityHashMap<EObject, EObject>();
		aliases.put(source.first(), target.first());
		aliases.put(source.second(), target.first());
		trace.copied(aliases, Map.of(source.attribute(), target.attribute()),
			List.of(source.resource()), List.of(target.resource()));
		assertThat(trace.snapshot()).isEmpty();

		trace = seeded(expression, source);
		trace.copied(Map.of(source.first(), target.first()), Map.of(),
			List.of(source.resource()), List.of(target.resource()));
		assertThat(trace.snapshot()).isEmpty();

		trace = seeded(expression, source);
		trace.copied(Map.of(source.first(), target.first()),
			Map.of(source.attribute(), target.attribute()), List.of(),
			List.of(target.resource()));
		assertThat(trace.snapshot()).isEmpty();

		trace = seeded(expression, source);
		trace.copied(Map.of(source.first(), target.first()),
			Map.of(source.attribute(), target.attribute()), List.of(source.resource()),
			List.of());
		assertThat(trace.snapshot()).isEmpty();

		var unset = stage(null, "two");
		trace = seeded(expression, source);
		trace.copied(Map.of(source.first(), unset.first()),
			Map.of(source.attribute(), unset.attribute()), List.of(source.resource()),
			List.of(unset.resource()));
		assertThat(trace.snapshot()).isEmpty();
	}

	@Test
	public void invalidOrUnattributedEventsCannotCreateOrRetainHints() {
		var expression = XbaseFactory.eINSTANCE.createXStringLiteral();
		var token = new AtomicReference<XExpression>(expression);
		var source = stage("one", "two");
		var target = stage("one", "two");
		var trace = new EdeltaModelMigrationTrace(token::get);
		trace.applied(operation(source.first(), source.attribute(),
			target.second(), target.attribute()));
		copy(trace, source, target);
		assertThat(trace.snapshot()).isEmpty();
		trace.applied(operation(source.first(), source.attribute(),
			target.first(), target.attribute()));
		trace.copied(Map.of(source.first(), target.first()),
			Map.of(source.attribute(), target.attribute()), List.of(),
			List.of(target.resource()));
		assertThat(trace.snapshot()).isEmpty();
		trace.applied(new AppliedOperation(source.first(),
			EcorePackage.Literals.ECLASS__ESTRUCTURAL_FEATURES,
			target.first(), target.attribute()));
		copy(trace, source, target);
		assertThat(trace.snapshot()).isEmpty();
		trace.applied(operation(source.first(), source.attribute(),
			target.first(), target.attribute()));
		trace.copied(Map.of(source.first(), target.first()), Map.of(),
			List.of(source.resource()), List.of(target.resource()));
		assertThat(trace.snapshot()).isEmpty();
		trace.applied(new AppliedOperation(source.first(), source.attribute(),
			target.first(), EcorePackage.Literals.ENAMED_ELEMENT__NAME));
		copy(trace, source, target);
		assertThat(trace.snapshot()).isEmpty();

		var unset = stage(null, "two");
		trace.applied(operation(unset.first(), unset.attribute(),
			target.first(), target.attribute()));
		copy(trace, unset, target);
		assertThat(trace.snapshot()).isEmpty();
		var foreign = stage("one", "two");
		trace.applied(operation(source.first(), source.attribute(),
			target.first(), foreign.attribute()));
		trace.copied(Map.of(source.first(), target.first()),
			Map.of(source.attribute(), foreign.attribute()), List.of(source.resource()),
			List.of(target.resource()));
		assertThat(trace.snapshot()).isEmpty();

		trace.applied(operation(target.first(), target.attribute(),
			source.first(), source.attribute()));
		trace.invalidated();
		assertThat(trace.snapshot()).isEmpty();
	}

	private EdeltaModelMigrationTrace seeded(XExpression expression, Stage source) {
		var token = new AtomicReference<XExpression>(expression);
		var trace = new EdeltaModelMigrationTrace(token::get);
		var before = stage("one", "two");
		trace.applied(operation(before.first(), before.attribute(),
			source.first(), source.attribute()));
		copy(trace, before, source);
		return trace;
	}

	private XExpression expression(EdeltaModelMigrationTrace trace, EObject object,
			EAttribute attribute) {
		var features = trace.snapshot().get(object);
		return features == null ? null : features.get(attribute);
	}

	private AppliedOperation operation(EObject source, EAttribute sourceAttribute,
			EObject target, EAttribute targetAttribute) {
		return new AppliedOperation(source, sourceAttribute, target, targetAttribute);
	}

	private void copy(EdeltaModelMigrationTrace trace, Stage source, Stage target) {
		trace.copied(Map.of(source.first(), target.first(), source.second(), target.second()),
			Map.of(source.attribute(), target.attribute()), List.of(source.resource()),
			List.of(target.resource()));
	}

	private Stage stage(String firstValue, String secondValue) {
		var pack = EcoreFactory.eINSTANCE.createEPackage();
		pack.setName("example");
		var type = EcoreFactory.eINSTANCE.createEClass();
		type.setName("Sample");
		var attribute = EcoreFactory.eINSTANCE.createEAttribute();
		attribute.setName("value");
		attribute.setEType(EcorePackage.Literals.ESTRING);
		type.getEStructuralFeatures().add(attribute);
		pack.getEClassifiers().add(type);
		new ResourceImpl().getContents().add(pack);
		var first = new DynamicEObjectImpl(type);
		var second = new DynamicEObjectImpl(type);
		if (firstValue != null) first.eSet(attribute, firstValue);
		if (secondValue != null) second.eSet(attribute, secondValue);
		var resource = new ResourceImpl();
		resource.getContents().add(first);
		resource.getContents().add(second);
		return new Stage(resource, attribute, first, second);
	}
}
