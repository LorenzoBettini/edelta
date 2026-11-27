package edelta.lib.tests;

import static edelta.lib.EdeltaStageValueComparator.Result.DIFFERENT;
import static edelta.lib.EdeltaStageValueComparator.Result.EQUAL;
import static edelta.lib.EdeltaStageValueComparator.Result.UNKNOWN;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EcoreFactory;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.InternalEObject;
import org.eclipse.emf.ecore.impl.DynamicEObjectImpl;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.junit.jupiter.api.Test;

import edelta.lib.EdeltaStageValueComparator;
import edelta.lib.EdeltaStageValueComparator.Mappings;

/**
 * Negative identity and unusual EMF-value cases for the isolated comparator.
 *
 * @author Lorenzo Bettini
 */
class StageValueComparatorEdgeTest {
	private final EdeltaStageValueComparatorTest.Fixture f = new EdeltaStageValueComparatorTest.Fixture();

	@Test
	void owningObjectAndFeatureRequireExactAccessibleIdentity() {
		assertThat(compare(null, f.beforeAttribute, f.after, f.afterAttribute)).isEqualTo(UNKNOWN);
		assertThat(compare(f.before, f.beforeAttribute, null, f.afterAttribute)).isEqualTo(UNKNOWN);
		assertThat(compare(f.before, null, f.after, f.afterAttribute)).isEqualTo(UNKNOWN);
		assertThat(compare(f.before, f.beforeAttribute, f.after, null)).isEqualTo(UNKNOWN);
		assertThat(compare(f.before, f.beforeAttribute,
			EcoreUtil.create(f.afterClass), f.afterAttribute)).isEqualTo(UNKNOWN);
		f.ecores.remove(f.beforeClass);
		assertThat(f.compareAttribute()).isEqualTo(UNKNOWN);
		f.ecores.put(f.beforeClass, f.afterClass);
		f.ecores.remove(f.beforeAttribute);
		assertThat(f.compareAttribute()).isEqualTo(UNKNOWN);
		f.ecores.put(f.beforeAttribute, f.afterAttribute);

		var foreignSource = EcoreFactory.eINSTANCE.createEAttribute();
		f.ecores.put(foreignSource, f.afterAttribute);
		assertThat(compare(f.before, foreignSource, f.after, f.afterAttribute)).isEqualTo(UNKNOWN);
		var foreignTarget = EcoreFactory.eINSTANCE.createEAttribute();
		f.ecores.put(f.beforeAttribute, foreignTarget);
		assertThat(compare(f.before, f.beforeAttribute, f.after, foreignTarget)).isEqualTo(UNKNOWN);
		f.ecores.put(f.beforeAttribute, f.afterAttribute);
		f.beforeAttribute.setUpperBound(-1);
		assertThat(f.compareAttribute()).isEqualTo(UNKNOWN);
		f.beforeAttribute.setUpperBound(1);
		f.ecores.put(f.beforeAttribute, f.afterReference);
		assertThat(compare(f.before, f.beforeAttribute, f.after, f.afterReference))
			.isEqualTo(UNKNOWN);
	}

	@Test
	void attributeConversionsAndCollectionBoundariesUseJavaValues() {
		f.before.eSet(f.beforeAttribute, "1");
		f.afterAttribute.setEType(EcorePackage.Literals.EINT);
		f.after.eSet(f.afterAttribute, 1);
		assertThat(f.compareAttribute()).isEqualTo(DIFFERENT);
		var list = new EdeltaStageValueComparatorTest.Fixture();
		list.beforeAttribute.setUpperBound(-1);
		list.afterAttribute.setUpperBound(-1);
		list.before.eSet(list.beforeAttribute, List.of("a"));
		list.after.eSet(list.afterAttribute, List.of("a", "b"));
		assertThat(list.compareAttribute()).isEqualTo(DIFFERENT);
		list.after.eSet(list.afterAttribute, List.of("b"));
		assertThat(list.compareAttribute()).isEqualTo(DIFFERENT);
	}

	@Test
	void referencesNeedKnownEndpointsEvenForNullAndWrongValues() {
		assertThat(f.compareReference()).isEqualTo(EQUAL);
		var old = EcoreUtil.create(f.beforeClass);
		var copied = EcoreUtil.create(f.afterClass);
		f.before.eSet(f.beforeReference, old);
		f.objects.put(old, copied);
		assertThat(f.compareReference()).isEqualTo(DIFFERENT);
		f.before.eUnset(f.beforeReference);
		f.after.eSet(f.afterReference, copied);
		assertThat(f.compareReference()).isEqualTo(DIFFERENT);
		f.objects.remove(old);
		assertThat(f.compareReference()).isEqualTo(UNKNOWN);
		f.objects.put(old, copied);
		f.before.eSet(f.beforeReference, old);
		assertThat(f.compareReference()).isEqualTo(EQUAL);
		f.ecores.remove(f.beforeClass);
		assertThat(f.compareReference()).isEqualTo(UNKNOWN);
	}

	@Test
	void proxyAndUnsupportedReferenceShapesAreUnknown() {
		var old = EcoreUtil.create(f.beforeClass);
		var copied = EcoreUtil.create(f.afterClass);
		f.objects.put(old, copied);
		f.before.eSet(f.beforeReference, old);
		f.after.eSet(f.afterReference, copied);
		((InternalEObject) copied).eSetProxyURI(URI.createURI("memory:/outside.xmi#x"));
		assertThat(f.compareReference()).isEqualTo(UNKNOWN);
		((InternalEObject) copied).eSetProxyURI(null);
		f.beforeReference.setContainment(true);
		assertThat(f.compareReference()).isEqualTo(UNKNOWN);
		f.beforeReference.setContainment(false);
		f.afterReference.setContainment(true);
		assertThat(f.compareReference()).isEqualTo(UNKNOWN);
		f.afterReference.setContainment(false);
		var opposite = EcoreFactory.eINSTANCE.createEReference();
		opposite.setEType(f.beforeClass);
		opposite.setContainment(true);
		f.beforeReference.setEOpposite(opposite);
		assertThat(f.compareReference()).isEqualTo(UNKNOWN);
		f.beforeReference.setEOpposite(null);
		var targetOpposite = EcoreFactory.eINSTANCE.createEReference();
		targetOpposite.setEType(f.afterClass);
		targetOpposite.setContainment(true);
		f.afterReference.setEOpposite(targetOpposite);
		assertThat(f.compareReference()).isEqualTo(UNKNOWN);
	}

	@Test
	void referredClassNeedsItsOwnExactEcoreCopy() {
		var otherBefore = EcoreFactory.eINSTANCE.createEClass();
		var otherAfter = EcoreFactory.eINSTANCE.createEClass();
		f.beforeClass.getEPackage().getEClassifiers().add(otherBefore);
		f.afterClass.getEPackage().getEClassifiers().add(otherAfter);
		f.beforeReference.setEType(otherBefore);
		f.afterReference.setEType(otherAfter);
		var old = EcoreUtil.create(otherBefore);
		var target = EcoreUtil.create(otherAfter);
		f.before.eSet(f.beforeReference, old);
		f.after.eSet(f.afterReference, target);
		f.objects.put(old, target);
		// The owning classes map, but this separate referred class does not.
		assertThat(f.compareReference()).isEqualTo(UNKNOWN);
	}

	@Test
	void unorderedAndMixedKindsCannotInferAValueChange() {
		f.beforeAttribute.setUpperBound(-1);
		f.afterAttribute.setUpperBound(-1);
		f.before.eSet(f.beforeAttribute, List.of("a"));
		f.after.eSet(f.afterAttribute, List.of("a"));
		f.beforeAttribute.setOrdered(false);
		assertThat(f.compareAttribute()).isEqualTo(UNKNOWN);
		f.beforeReference.setUpperBound(-1);
		f.ecores.put(f.beforeReference, f.afterAttribute);
		assertThat(compare(f.before, f.beforeReference, f.after, f.afterAttribute))
			.isEqualTo(UNKNOWN);
		f.ecores.put(f.beforeReference, f.afterReference);
		f.beforeReference.setUpperBound(-1);
		f.afterReference.setUpperBound(-1);
		f.beforeReference.setOrdered(false);
		assertThat(f.compareReference()).isEqualTo(UNKNOWN);
		var targetUnordered = new EdeltaStageValueComparatorTest.Fixture();
		targetUnordered.beforeReference.setUpperBound(-1);
		targetUnordered.afterReference.setUpperBound(-1);
		targetUnordered.afterReference.setOrdered(false);
		assertThat(targetUnordered.compareReference()).isEqualTo(UNKNOWN);
	}

	@Test
	void listWithUnknownElementCannotUseALaterDifferentElement() {
		f.beforeReference.setUpperBound(-1);
		f.afterReference.setUpperBound(-1);
		var unknown = EcoreUtil.create(f.beforeClass);
		var known = EcoreUtil.create(f.beforeClass);
		var other = EcoreUtil.create(f.beforeClass);
		var another = EcoreUtil.create(f.beforeClass);
		var knownTarget = EcoreUtil.create(f.afterClass);
		var otherTarget = EcoreUtil.create(f.afterClass);
		var anotherTarget = EcoreUtil.create(f.afterClass);
		f.objects.put(known, knownTarget);
		f.objects.put(other, otherTarget);
		f.objects.put(another, anotherTarget);
		f.before.eSet(f.beforeReference, List.of(unknown, known));
		f.after.eSet(f.afterReference, List.of(otherTarget, anotherTarget));
		assertThat(f.compareReference()).isEqualTo(UNKNOWN);
	}

	@Test
	void malformedDynamicListAndReferenceValuesStayUnknown() {
		f.beforeAttribute.setUpperBound(-1);
		f.afterAttribute.setUpperBound(-1);
		f.before.eSet(f.beforeAttribute, List.of("a"));
		var scalarTarget = new DynamicEObjectImpl(f.afterClass) {
			@Override public Object eGet(EStructuralFeature feature) { return "a"; }
		};
		f.objects.put(f.before, scalarTarget);
		assertThat(compare(f.before, f.beforeAttribute, scalarTarget, f.afterAttribute))
			.isEqualTo(UNKNOWN);
		var scalarSource = new DynamicEObjectImpl(f.beforeClass) {
			@Override public Object eGet(EStructuralFeature feature) { return "a"; }
		};
		f.after.eSet(f.afterAttribute, List.of("a"));
		f.objects.clear();
		f.objects.put(scalarSource, f.after);
		assertThat(compare(scalarSource, f.beforeAttribute, f.after, f.afterAttribute))
			.isEqualTo(UNKNOWN);

		var badSource = new DynamicEObjectImpl(f.beforeClass) {
			@Override public Object eGet(EStructuralFeature feature, boolean resolve) {
				return "not an EObject";
			}
		};
		f.objects.clear();
		f.objects.put(badSource, f.after);
		assertThat(compare(badSource, f.beforeReference, f.after, f.afterReference))
			.isEqualTo(UNKNOWN);
		var badTarget = new DynamicEObjectImpl(f.afterClass) {
			@Override public Object eGet(EStructuralFeature feature, boolean resolve) {
				return "not an EObject";
			}
		};
		f.objects.clear();
		f.objects.put(f.before, badTarget);
		assertThat(compare(f.before, f.beforeReference, badTarget, f.afterReference))
			.isEqualTo(UNKNOWN);
	}

	@Test
	void unreadableDynamicPropertyIsUnknownInsteadOfBreakingMigration() {
		var unreadable = new DynamicEObjectImpl(f.beforeClass) {
			@Override
			public Object eGet(EStructuralFeature feature) {
				throw new IllegalStateException("unreadable");
			}
		};
		f.objects.clear();
		f.objects.put(unreadable, f.after);
		assertThat(compare(unreadable, f.beforeAttribute, f.after, f.afterAttribute))
			.isEqualTo(UNKNOWN);
	}

	private EdeltaStageValueComparator.Result compare(EObject source,
			EStructuralFeature sourceFeature, EObject target, EStructuralFeature targetFeature) {
		return EdeltaStageValueComparator.compare(source, sourceFeature, target,
			targetFeature, new Mappings(f.objects, f.ecores));
	}
}
