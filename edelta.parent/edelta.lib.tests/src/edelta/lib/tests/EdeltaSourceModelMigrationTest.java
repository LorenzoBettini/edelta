package edelta.lib.tests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import java.nio.file.Path;

import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.junit.jupiter.api.Test;

import edelta.lib.EdeltaEngine;
import edelta.lib.EdeltaModelManager;
import edelta.lib.EdeltaModelMigrator;
import edelta.lib.EdeltaRuntime;

/** Exercises source protection in ordinary migration and stage transitions.
 * @author Lorenzo Bettini
 */
class EdeltaSourceModelMigrationTest {
	private static final Path DATA = Path.of("../edelta.testdata/testdata/simpleTestData");

	@Test
	void engineCannotSaveBeforeExecution() throws Exception {
		var engine = new EdeltaEngine(other -> new EdeltaRuntime(other) {});
		assertThatThrownBy(() -> engine.saveModels(Files.createTempDirectory("no-migration-").toString()))
			.hasMessage("No completed model migration to save");
	}

	@Test
	void plainMigratorAllowsTargetAndEcoreWrites() {
		var originals = new EdeltaModelManager();
		originals.loadEcoreFile(DATA.resolve("My.ecore").toString());
		originals.loadModelFile(DATA.resolve("MyClass.xmi").toString());
		var migrator = new EdeltaModelMigrator(originals);
		var evolved = migrator.getEvolvingModelManager();
		var type = (EClass) evolved.getEPackage("mypackage").getEClassifier("MyClass");
		type.setName("Renamed");
		var attribute = (EAttribute) type.getEStructuralFeature("myClassStringAttribute");
		migrator.transformAttributeValueRule(migrator.isRelatedTo(attribute),
			value -> value.toString().toUpperCase());
		migrator.copyModels();
		var migrated = evolved.getModelResources().iterator().next().getContents().getFirst();
		migrated.eSet(attribute, "target remains writable");
		assertThat(migrated.eGet(attribute)).isEqualTo("target remains writable");
		assertThat(type.getName()).isEqualTo("Renamed");
	}

	@Test
	void engineRejectsCaughtWriteAndDoesNotSaveOrChangeInputXmi() throws Exception {
		var input = DATA.resolve("MyClass.xmi");
		var originalText = Files.readString(input);
		var output = Files.createTempDirectory("edelta-source-guard-output-");
		var engine = new EdeltaEngine(other -> new EdeltaRuntime(other) {
			@Override
			protected void doExecute() {
				modelMigration(migrator -> {
					var type = (EClass) migrator.getEvolvingModelManager()
						.getEPackage("mypackage").getEClassifier("MyClass");
					var attribute = (EAttribute) type.getEStructuralFeature("myClassStringAttribute");
					migrator.transformAttributeValueRule(migrator.isRelatedTo(attribute),
						(feature, source, value) -> {
							try {
								source.eSet(feature, "caught write");
							} catch (IllegalStateException ignored) {
								// EMF has already changed the object; completion must still fail.
							}
							return value;
						});
				});
			}
		});
		engine.loadEcoreFile(DATA.resolve("My.ecore").toString());
		var sourceResource = engine.loadModelFile(input.toString());
		assertThatThrownBy(engine::execute).isInstanceOf(IllegalStateException.class)
			.hasMessageMatching("Migration source model is read-only: .*MyClass.xmi :: MyClass#.*\\.myClassStringAttribute \\(SET\\)");
		assertThat(engine.getEvolvingModelManager()).isNull();
		assertThatThrownBy(() -> engine.save(output.toString()))
			.hasMessageContaining("Migration source model is read-only:");
		assertThat(Files.list(output)).isEmpty();
		assertThat(Files.readString(input)).isEqualTo(originalText);
		assertThat(sourceResource.eAdapters()).isEmpty();
	}

	@Test
	void backupBecomesGuardedSourceAfterStageSwitch() {
		var fixture = new StageMigrationFixture();
		var evolved = fixture.evolvedValue();
		fixture.migrator.transformAttributeValueRule(fixture.migrator.isRelatedTo(evolved),
			value -> value);
		var backup = fixture.stages.get(1).targets().getFirst();
		var source = backup.getContents().getFirst();
		var feature = source.eClass().getEStructuralFeature("value");
		assertThat(fixture.first.eAdapters()).isEmpty();
		assertThatThrownBy(() -> source.eSet(feature, "invalid second stage"))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageMatching("Migration source model is read-only: memory:/probe0.xmi :: Node#.*\\.value \\(SET\\)");
		fixture.migrator.close();
		assertThat(backup.eAdapters()).isEmpty();
	}

	@Test
	void directMigratorManagerCannotSaveAfterCaughtSourceWrite() throws Exception {
		var originals = new EdeltaModelManager();
		originals.loadEcoreFile(DATA.resolve("My.ecore").toString());
		var input = originals.loadModelFile(DATA.resolve("MyClass.xmi").toString());
		var migrator = new EdeltaModelMigrator(originals);
		var source = input.getContents().getFirst();
		var feature = source.eClass().getEStructuralFeature("myClassStringAttribute");
		try {
			source.eSet(feature, "caught");
		} catch (IllegalStateException ignored) {
			// The in-memory source changed, but the save gate retains the violation.
		}
		var output = Files.createTempDirectory("invalid-direct-output-");
		assertThatThrownBy(() -> migrator.getEvolvingModelManager().saveModels(output.toString()))
			.hasMessageMatching("Migration source model is read-only: .*MyClass.xmi :: MyClass#.*\\.myClassStringAttribute \\(SET\\)");
		assertThat(Files.list(output)).isEmpty();
		migrator.close();
	}
}
