package edelta.lib.tests;

import static edelta.lib.tests.StageValueComparator.Result.DIFFERENT;
import static edelta.lib.tests.StageValueComparator.Result.EQUAL;
import static edelta.lib.tests.StageValueComparator.Result.UNKNOWN;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EcoreFactory;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.InternalEObject;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.junit.jupiter.api.Test;

/** Direct contract tests for the isolated stage comparison probe.
 * @author Lorenzo Bettini
 */
class StageValueComparatorTest {
	private final Fixture fixture = new Fixture();

	@Test
	void equalAttributeValuesIgnoreExplicitDefaultVersusUnset() {
		fixture.beforeAttribute.setDefaultValueLiteral("same");
		fixture.afterAttribute.setDefaultValueLiteral("same");
		fixture.after.eSet(fixture.afterAttribute, "same");
		assertThat(fixture.compareAttribute()).isEqualTo(EQUAL);
		fixture.after.eSet(fixture.afterAttribute, "changed");
		assertThat(fixture.compareAttribute()).isEqualTo(DIFFERENT);
	}

	@Test
	void orderedAttributeListsCompareSizeAndPosition() {
		fixture.beforeAttribute.setUpperBound(-1);
		fixture.afterAttribute.setUpperBound(-1);
		fixture.before.eSet(fixture.beforeAttribute, List.of("a", "b"));
		fixture.after.eSet(fixture.afterAttribute, List.of("a", "b"));
		assertThat(fixture.compareAttribute()).isEqualTo(EQUAL);
		fixture.after.eSet(fixture.afterAttribute, List.of("b", "a"));
		assertThat(fixture.compareAttribute()).isEqualTo(DIFFERENT);
		fixture.afterAttribute.setOrdered(false);
		assertThat(fixture.compareAttribute()).isEqualTo(UNKNOWN);
	}

	@Test
	void referencesNeedExactObjectAndClassCopies() {
		var oldReferred = EcoreUtil.create(fixture.beforeClass);
		var newReferred = EcoreUtil.create(fixture.afterClass);
		var another = EcoreUtil.create(fixture.afterClass);
		fixture.before.eSet(fixture.beforeReference, oldReferred);
		fixture.after.eSet(fixture.afterReference, newReferred);
		fixture.objects.put(oldReferred, newReferred);
		assertThat(fixture.compareReference()).isEqualTo(EQUAL);
		fixture.after.eSet(fixture.afterReference, another);
		assertThat(fixture.compareReference()).isEqualTo(DIFFERENT);
		fixture.objects.remove(oldReferred);
		// A same-class object without exact lineage cannot prove a change.
		assertThat(fixture.compareReference()).isEqualTo(UNKNOWN);
		fixture.objects.put(oldReferred, newReferred);
		((InternalEObject) oldReferred).eSetProxyURI(URI.createURI("memory:/unselected.xmi#x"));
		assertThat(fixture.compareReference()).isEqualTo(UNKNOWN);
	}

	@Test
	void orderedReferenceListsNeedEveryExactMappedElement() {
		fixture.beforeReference.setUpperBound(-1);
		fixture.afterReference.setUpperBound(-1);
		var oldFirst = EcoreUtil.create(fixture.beforeClass);
		var oldSecond = EcoreUtil.create(fixture.beforeClass);
		var newFirst = EcoreUtil.create(fixture.afterClass);
		var newSecond = EcoreUtil.create(fixture.afterClass);
		fixture.objects.put(oldFirst, newFirst);
		fixture.objects.put(oldSecond, newSecond);
		fixture.before.eSet(fixture.beforeReference, List.of(oldFirst, oldSecond));
		fixture.after.eSet(fixture.afterReference, List.of(newFirst, newSecond));
		assertThat(fixture.compareReference()).isEqualTo(EQUAL);
		fixture.after.eSet(fixture.afterReference, List.of(newSecond, newFirst));
		assertThat(fixture.compareReference()).isEqualTo(DIFFERENT);
		fixture.objects.remove(oldSecond);
		// Once one element loses lineage, order alone cannot justify a source link.
		assertThat(fixture.compareReference()).isEqualTo(UNKNOWN);
	}

	@Test
	void missingEClassCorrespondenceIsUnknownEvenForEqualAttributeValues() {
		fixture.before.eSet(fixture.beforeAttribute, "same");
		fixture.after.eSet(fixture.afterAttribute, "same");
		fixture.ecores.remove(fixture.beforeClass);
		assertThat(fixture.compareAttribute()).isEqualTo(UNKNOWN);
	}

	@Test
	void sourceInheritedFeatureAndTargetDeclaredFeatureCanCompareEqual() {
		var parent = EcoreFactory.eINSTANCE.createEClass();
		parent.setName("Parent");
		fixture.beforeClass.getEPackage().getEClassifiers().add(parent);
		fixture.beforeClass.getEStructuralFeatures().remove(fixture.beforeAttribute);
		parent.getEStructuralFeatures().add(fixture.beforeAttribute);
		fixture.beforeClass.getESuperTypes().add(parent);
		fixture.objects.remove(fixture.before);
		fixture.before = EcoreUtil.create(fixture.beforeClass);
		fixture.objects.put(fixture.before, fixture.after);
		fixture.before.eSet(fixture.beforeAttribute, "same");
		fixture.after.eSet(fixture.afterAttribute, "same");
		assertThat(fixture.compareAttribute()).isEqualTo(EQUAL);
	}

	@Test
	void ambiguousObjectOrFeatureMappingIsUnknown() {
		fixture.before.eSet(fixture.beforeAttribute, "a");
		fixture.after.eSet(fixture.afterAttribute, "b");
		var otherSource = EcoreUtil.create(fixture.beforeClass);
		fixture.objects.put(otherSource, fixture.after);
		assertThat(fixture.compareAttribute()).isEqualTo(UNKNOWN);
		fixture.objects.remove(otherSource);
		fixture.ecores.remove(fixture.beforeAttribute);
		assertThat(fixture.compareAttribute()).isEqualTo(UNKNOWN);
	}

	private static final class Fixture {
		final EClass beforeClass = EcoreFactory.eINSTANCE.createEClass();
		final EClass afterClass = EcoreFactory.eINSTANCE.createEClass();
		final EAttribute beforeAttribute = EcoreFactory.eINSTANCE.createEAttribute();
		final EAttribute afterAttribute = EcoreFactory.eINSTANCE.createEAttribute();
		final EReference beforeReference = EcoreFactory.eINSTANCE.createEReference();
		final EReference afterReference = EcoreFactory.eINSTANCE.createEReference();
		EObject before;
		final EObject after;
		final Map<EObject, EObject> objects = new IdentityHashMap<>();
		final Map<EObject, EObject> ecores = new IdentityHashMap<>();

		Fixture() {
			var beforePackage = EcoreFactory.eINSTANCE.createEPackage();
			var afterPackage = EcoreFactory.eINSTANCE.createEPackage();
			beforePackage.setName("before");
			afterPackage.setName("after");
			beforePackage.getEClassifiers().add(beforeClass);
			afterPackage.getEClassifiers().add(afterClass);
			beforeClass.setName("Node");
			afterClass.setName("RenamedNode");
			beforeAttribute.setName("value");
			afterAttribute.setName("value");
			beforeAttribute.setEType(EcorePackage.Literals.ESTRING);
			afterAttribute.setEType(EcorePackage.Literals.ESTRING);
			beforeReference.setName("link");
			afterReference.setName("link");
			beforeReference.setEType(beforeClass);
			afterReference.setEType(afterClass);
			beforeClass.getEStructuralFeatures().addAll(List.of(beforeAttribute, beforeReference));
			afterClass.getEStructuralFeatures().addAll(List.of(afterAttribute, afterReference));
			before = EcoreUtil.create(beforeClass);
			after = EcoreUtil.create(afterClass);
			objects.put(before, after);
			ecores.put(beforeClass, afterClass);
			ecores.put(beforeAttribute, afterAttribute);
			ecores.put(beforeReference, afterReference);
		}

		StageValueComparator.Result compareAttribute() {
			return StageValueComparator.compare(before, beforeAttribute, after, afterAttribute,
				objects, ecores);
		}

		StageValueComparator.Result compareReference() {
			return StageValueComparator.compare(before, beforeReference, after, afterReference,
				objects, ecores);
		}
	}
}
