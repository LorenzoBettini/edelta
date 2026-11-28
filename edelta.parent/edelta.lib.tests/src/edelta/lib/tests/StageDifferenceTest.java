package edelta.lib.tests;

import static edelta.lib.EdeltaStageValueComparator.Result.DIFFERENT;
import static edelta.lib.EdeltaStageValueComparator.Result.EQUAL;
import static edelta.lib.EdeltaStageValueComparator.Result.UNKNOWN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EcoreFactory;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.junit.jupiter.api.Test;

import edelta.lib.EdeltaStageValueComparator;
import edelta.lib.EdeltaStageValueComparator.Mappings;

/** Test-only probes of the migrator's copy, post-copy, and backup timing.
 * @author Lorenzo Bettini
 */
class StageDifferenceTest {
	@Test
	void stagedChangesNoOpsAndReverseUseEachStagesOwnSource() {
		var f = new StageMigrationFixture();
		var evolved = f.evolvedValue();
		f.migrator.transformAttributeValueRule(f.migrator.isRelatedTo(evolved),
			value -> value.toString().toUpperCase());
		var first = f.stages.get(0);
		assertThat(first.compare(f.first, f.value)).isEqualTo(DIFFERENT);
		assertThat(first.compare(f.second, f.value)).isEqualTo(EQUAL);
		assertThat(first.targets().getFirst().getResourceSet()).isNull();
		assertThat(f.stages.get(1).compare(first.objects().get(f.first), evolved)).isEqualTo(EQUAL);
		var nextSource = f.stages.get(1).objects().get(first.objects().get(f.first));
		var nextFeature = (EAttribute) f.stages.get(1).ecores().get(evolved);
		f.migrator.transformAttributeValueRule(f.migrator.isRelatedTo(evolved), value -> value);
		assertThat(f.stages.get(2).compare(nextSource, nextFeature)).isEqualTo(EQUAL);
		var reverseSource = f.stages.get(3).objects().get(f.stages.get(2).objects().get(nextSource));
		var reverseFeature = (EAttribute) f.stages.get(3).ecores().get(evolved);
		f.migrator.transformAttributeValueRule(f.migrator.isRelatedTo(evolved), value -> "hello");
		assertThat(f.stages.get(4).compare(reverseSource, reverseFeature)).isEqualTo(DIFFERENT);
		// The final copy is a lineage transfer, not another user transformation.
		f.migrator.copyModels();
		assertThat(f.stages).hasSize(7);
		assertThat(f.stages.get(6).compare(
			f.stages.get(5).objects().get(f.stages.get(4).objects().get(reverseSource)),
			(EAttribute) f.stages.get(5).ecores().get(evolved)))
			.isEqualTo(EQUAL);
	}

	@Test
	void laterUnrelatedFeatureRuleLeavesEarlierValueComparisonEqual() {
		var f = new StageMigrationFixture(true);
		var evolvedValue = f.evolvedValue();
		var evolvedTags = (EAttribute) f.migrator.getMigrated(f.tags);
		f.migrator.transformAttributeValueRule(f.migrator.isRelatedTo(evolvedValue),
			value -> value.toString().toUpperCase());
		assertThat(f.stages.get(0).compare(f.first, f.value)).isEqualTo(DIFFERENT);
		var nextSource = f.stages.get(1).objects().get(f.stages.get(0).objects().get(f.first));
		var nextValue = (EAttribute) f.stages.get(1).ecores().get(evolvedValue);
		var nextTags = (EAttribute) f.stages.get(1).ecores().get(evolvedTags);
		f.migrator.transformAttributeValueRule(f.migrator.isRelatedTo(evolvedTags),
			value -> List.of("tag!"));
		assertThat(f.stages.get(2).compare(nextSource, nextValue)).isEqualTo(EQUAL);
		assertThat(f.stages.get(2).compare(nextSource, nextTags)).isEqualTo(DIFFERENT);
	}

	@Test
	void copiedCallbackPrecedesPostCopyEvenWhenPostCopyChangesOrRestoresValue() {
		var f = new StageMigrationFixture();
		var evolved = f.evolvedValue();
		f.migrator.copyRule(feature -> false, (feature, source, target) -> {}, () -> {
			var target = f.stages.get(0).objects().get(f.first);
			assertThat(f.stages.get(0).compare(f.first, f.value)).isEqualTo(EQUAL);
			target.eSet(evolved, "changed after copied");
		});
		assertThat(f.stages.get(0).compare(f.first, f.value)).isEqualTo(DIFFERENT);
		assertThat(f.stages.get(1).compare(f.stages.get(0).objects().get(f.first), evolved))
			.isEqualTo(EQUAL);

		var laterSource = f.stages.get(1).objects().get(f.stages.get(0).objects().get(f.first));
		var laterFeature = (EAttribute) f.stages.get(1).ecores().get(evolved);
		f.migrator.copyRule(feature -> false, (feature, source, target) -> {}, () -> {
			var target = f.stages.get(2).objects().get(laterSource);
			assertThat(f.stages.get(2).compare(laterSource, laterFeature)).isEqualTo(EQUAL);
			target.eSet(evolved, "hello");
		});
		assertThat(f.stages.get(2).compare(laterSource, laterFeature)).isEqualTo(DIFFERENT);
		assertThat(f.invalidations).isEmpty();
	}

	@Test
	void postCopySourceMutationIsRejected() {
		var f = new StageMigrationFixture();
		assertThatThrownBy(() -> f.migrator.copyRule(feature -> false,
			(feature, source, target) -> {}, () -> {
			assertThat(f.stages.get(0).compare(f.first, f.value)).isEqualTo(EQUAL);
			f.first.eSet(f.value, "source changed after copy");
		})).isInstanceOf(IllegalStateException.class)
			.hasMessageMatching("Migration source model is read-only: .* :: Node#.*\\.value \\(SET\\)");
	}

	@Test
	void copyProcedureSourceMutationIsRejectedBeforeCopiedCallback() {
		var f = new StageMigrationFixture();
		var evolved = f.evolvedValue();
		assertThatThrownBy(() -> f.migrator.copyRule(feature -> feature == f.value,
			(feature, source, target) -> {
				// A custom procedure can change the input before any copied event.
				source.eSet(feature, "mutated source");
				target.eSet(evolved, "hello");
			})).isInstanceOf(IllegalStateException.class)
				.hasMessageMatching("Migration source model is read-only: .* :: Node#.*\\.value \\(SET\\)");
		assertThat(f.stages).isEmpty();
	}

	@Test
	void crossResourceReferencesAreFixedBeforeCopiedAndUseExactIdentity() {
		var f = new StageMigrationFixture();
		f.migrator.copyRule(feature -> false, (feature, source, target) -> {});
		var stage = f.stages.get(0);
		var target = stage.objects().get(f.first);
		var targetFeature = (org.eclipse.emf.ecore.EReference) stage.ecores().get(f.link);
		assertThat(target.eGet(targetFeature)).isSameAs(stage.objects().get(f.second));
		assertThat(EdeltaStageValueComparator.compare(f.first, f.link, target, targetFeature,
			new Mappings(stage.objects(), stage.ecores()))).isEqualTo(EQUAL);
		target.eSet(targetFeature, target);
		assertThat(EdeltaStageValueComparator.compare(f.first, f.link, target, targetFeature,
			new Mappings(stage.objects(), stage.ecores()))).isEqualTo(DIFFERENT);
	}

	@Test
	void featureMoveWithSameIdentityAndValueRemainsEqual() {
		var f = new StageMigrationFixture();
		var evolved = f.evolvedValue();
		var parent = EcoreFactory.eINSTANCE.createEClass();
		parent.setName("Parent");
		var targetClass = (EClass) f.migrator.getMigrated(f.node);
		targetClass.getEPackage().getEClassifiers().add(parent);
		targetClass.getEStructuralFeatures().remove(evolved);
		parent.getEStructuralFeatures().add(evolved);
		targetClass.getESuperTypes().add(parent);
		f.migrator.transformAttributeValueRule(feature -> false, value -> value);
		assertThat(f.stages.get(0).compare(f.first, f.value)).isEqualTo(EQUAL);
	}

	@Test
	void remappedFeaturesAndDirectCopyFromHaveNoGenericStageEvidence() {
		var f = new StageMigrationFixture();
		var evolved = f.evolvedValue();
		var replacement = EcoreFactory.eINSTANCE.createEAttribute();
		replacement.setName("replacement");
		replacement.setEType(evolved.getEType());
		f.migrator.getMigrated(f.node).getEStructuralFeatures().add(replacement);
		f.migrator.mapFeatureRule(evolved, replacement);
		var stage = f.stages.get(0);
		assertThat(stage.objects().get(f.first).eGet(replacement)).isEqualTo("hello");
		// The ordinary Ecore copy map still points at the old feature, not the remap.
		assertThat(EdeltaStageValueComparator.compare(f.first, f.value,
			stage.objects().get(f.first), replacement, new Mappings(stage.objects(), stage.ecores())))
			.isEqualTo(UNKNOWN);
		var count = f.stages.size();
		var target = stage.objects().get(f.first);
		target.eSet(replacement, "temporary");
		f.migrator.copyFrom(target, replacement, f.first, f.value);
		assertThat(f.stages).hasSize(count);
		assertThat(f.invalidations).hasSize(1);
	}

	@Test
	void featureMigratorDestinationAndMergedReplacementNeedAdditionalProof() {
		var f = new StageMigrationFixture();
		var evolved = f.evolvedValue();
		var replacement = EcoreFactory.eINSTANCE.createEAttribute();
		replacement.setName("replacement");
		replacement.setEType(evolved.getEType());
		f.migrator.getMigrated(f.node).getEStructuralFeatures().add(replacement);
		f.migrator.featureMigratorRule(feature -> feature == f.value,
			(feature, source, target) -> replacement);
		var stage = f.stages.get(0);
		assertThat(stage.objects().get(f.first).eGet(replacement)).isEqualTo("hello");
		assertThat(EdeltaStageValueComparator.compare(f.first, f.value,
			stage.objects().get(f.first), replacement, new Mappings(stage.objects(), stage.ecores())))
			.isEqualTo(UNKNOWN);

		var other = new StageMigrationFixture();
		var shared = EcoreUtil.create((EClass) other.migrator.getMigrated(other.node));
		other.migrator.createInstanceRule(sourceClass -> true, source -> shared);
		var replacementStage = other.stages.get(0);
		assertThat(replacementStage.objects().get(other.first)).isSameAs(shared);
		assertThat(replacementStage.objects().get(other.second)).isSameAs(shared);
		assertThat(replacementStage.compare(other.first, other.value)).isEqualTo(UNKNOWN);
	}
}
