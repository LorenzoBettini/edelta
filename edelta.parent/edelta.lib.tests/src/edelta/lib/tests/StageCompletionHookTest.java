package edelta.lib.tests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/**
 * Verifies the user-stage boundary separately from backup and final copies.
 *
 * @author Lorenzo Bettini
 */
class StageCompletionHookTest {
	@Test
	void completionSeesLegalTargetOnlyPostCopyAndExactReferenceFixup() {
		var fixture = new StageMigrationFixture();
		var evolved = fixture.evolvedValue();
		fixture.migrator.copyRule(feature -> false, (feature, source, target) -> {}, () -> {
			var target = fixture.stages.getFirst().objects().get(fixture.first);
			assertThat(target.eGet(fixture.migrator.getMigrated(fixture.link)))
				.isSameAs(fixture.stages.getFirst().objects().get(fixture.second));
			// postCopy reads the guarded source and writes only the target.
			target.eSet(evolved, fixture.first.eGet(fixture.value) + "!");
			assertThat(fixture.completed).isEmpty();
		});
		assertThat(fixture.completed).hasSize(1);
		assertThat(fixture.completed.getFirst().userOperation()).isTrue();
		assertThat(fixture.completed.getFirst().objectCopies())
			.isSameAs(fixture.stages.getFirst().objects());
		assertThatThrownBy(fixture.stages.getFirst().objects()::clear)
			.isInstanceOf(UnsupportedOperationException.class);
		assertThat(fixture.stages).hasSize(2);
		fixture.migrator.copyModels();
		assertThat(fixture.stages).hasSize(3);
		assertThat(fixture.completed).hasSize(1);
	}

	@Test
	void failedPostCopyDoesNotReportCompletion() {
		var fixture = new StageMigrationFixture();
		assertThatThrownBy(() -> fixture.migrator.copyRule(feature -> false,
			(feature, source, target) -> {}, () -> {
				throw new IllegalArgumentException("post-copy failure");
			})).isInstanceOf(IllegalArgumentException.class);
		assertThat(fixture.stages).hasSize(1);
		assertThat(fixture.completed).isEmpty();
	}
}
