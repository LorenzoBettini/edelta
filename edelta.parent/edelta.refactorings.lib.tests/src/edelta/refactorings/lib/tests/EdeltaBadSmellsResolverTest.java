package edelta.refactorings.lib.tests;

import static java.util.Arrays.asList;
import static org.eclipse.xtext.xbase.lib.IterableExtensions.head;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

import org.assertj.core.api.Assertions;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EPackage;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import edelta.lib.EdeltaDefaultRuntime;
import edelta.lib.EdeltaModelManager;
import edelta.refactorings.lib.EdeltaBadSmellsResolver;
import edelta.testutils.EdeltaTestUtils;

public class EdeltaBadSmellsResolverTest extends AbstractTest {
	private EdeltaBadSmellsResolver resolver;

	private String testModelDirectory;

	private List<String> testModelFiles;

	private EdeltaModelManager modelManager;

	@Before
	public void setup() {
		modelManager = new EdeltaModelManager();
		resolver = new EdeltaBadSmellsResolver(new EdeltaDefaultRuntime(modelManager));
	}

	/**
	 * This should be commented out when we want to copy the generated modified
	 * Ecore into the test-output-expectations directory, typically, the first time
	 * we write a new test.
	 * 
	 * We need to clean the modified directory when tests are stable, so that
	 * modified Ecore with validation errors do not fill the project with error
	 * markers.
	 * 
	 * @throws IOException
	 */
	@After
	public void cleanModifiedOutputDirectory() throws IOException {
		EdeltaTestUtils.cleanDirectory(AbstractTest.MODIFIED);
	}


	private void loadModelFiles(String testModelDirectory, String... testModelFiles) {
		this.testModelDirectory = testModelDirectory;
		this.testModelFiles = asList(testModelFiles);
		for (String testModelFile : testModelFiles) {
			modelManager
				.loadEcoreFile(AbstractTest.TESTECORES +
					testModelDirectory + "/" + testModelFile);
		}
	}

	private void assertModifiedFiles() throws IOException {
		// no need to explicitly validate:
		// if the ecore file is saved then it is valid
		for (String testModelFile : testModelFiles) {
			EdeltaTestUtils.assertFilesAreEquals(
					AbstractTest.EXPECTATIONS + testModelDirectory + "/" + testModelFile,
					AbstractTest.MODIFIED + testModelFile);
		}
	}

	@Test
	public void test_resolveDuplicatedFeatures() throws IOException {
		loadModelFiles("resolveDuplicatedFeatures", "TestEcore.ecore");
		resolver.resolveDuplicatedFeatures(resolver.getEPackage("p"));
		modelManager.saveEcores(AbstractTest.MODIFIED);
		assertModifiedFiles();
	}

	@Test
	public void test_resolveDeadClassifiers() throws IOException {
		loadModelFiles("resolveDeadClassifiers", "TestEcore.ecore");
		// specifies to remove only Unused2 even if there are other
		// dead classifiers
		resolver.resolveDeadClassifiers(resolver.getEPackage("p"),
			it -> Objects.equals(it.getName(), "Unused2"));
		modelManager.saveEcores(AbstractTest.MODIFIED);
		assertModifiedFiles();
	}

	@Test
	public void test_resolveDeadClassifiersAlwaysTrue() throws IOException {
		loadModelFiles("resolveDeadClassifiersAlwaysTrue", "TestEcore.ecore");
		// all classifiers considered dead classifiers will be removed
		resolver.resolveDeadClassifiers(resolver.getEPackage("p"));
		modelManager.saveEcores(AbstractTest.MODIFIED);
		assertModifiedFiles();
	}

	@Test
	public void test_resolveDeadClassifiersParallel() throws IOException {
		// one class in the other ecore has a reference to Unused1,
		// which is then not considered dead classifier
		// one class in the other ecore extends Unused2,
		// which is then not considered dead classifier
		loadModelFiles("resolveDeadClassifiersParallel",
				"TestEcoreReferred.ecore", "TestEcoreReferring.ecore");
		resolver.resolveDeadClassifiers(resolver.getEPackage("p"));
		modelManager.saveEcores(AbstractTest.MODIFIED);
		assertModifiedFiles();
	}

	@Test
	public void test_resolveRedundantContainers() throws IOException {
		loadModelFiles("resolveRedundantContainers", "TestEcore.ecore");
		resolver.resolveRedundantContainers(resolver.getEPackage("p"));
		modelManager.saveEcores(AbstractTest.MODIFIED);
		assertModifiedFiles();
	}

	@Test
	public void test_resolveClassificationByHierarchy() throws IOException {
		loadModelFiles("resolveClassificationByHierarchy", "TestEcore.ecore");
		resolver.resolveClassificationByHierarchy(resolver.getEPackage("p"));
		modelManager.saveEcores(AbstractTest.MODIFIED);
		assertModifiedFiles();
	}

	@Test
	public void test_resolveConcreteAbstractMetaclass() throws IOException {
		loadModelFiles("resolveConcreteAbstractMetaclass", "TestEcore.ecore");
		resolver.resolveConcreteAbstractMetaclass(resolver.getEPackage("p"));
		modelManager.saveEcores(AbstractTest.MODIFIED);
		assertModifiedFiles();
	}

	@Test
	public void test_resolveAbstractConcreteMetaclass() {
		final EPackage p = createEPackage("p",
			pack -> stdLib.addNewAbstractEClass(pack, "AbstractConcreteMetaclass"));
		final EClass c = head(EClasses(p));
		resolver.resolveAbstractConcreteMetaclass(p);
		Assertions.assertThat(c.isAbstract()).isFalse();
	}

	@Test
	public void test_resolveAbstractSubclassesOfConcreteSuperclasses() throws IOException {
		loadModelFiles("resolveAbstractSubclassesOfConcreteSuperclasses", "TestEcore.ecore");
		resolver.resolveAbstractSubclassesOfConcreteSuperclasses(resolver.getEPackage("p"));
		modelManager.saveEcores(AbstractTest.MODIFIED);
		assertModifiedFiles();
	}

	@Test
	public void test_resolveDuplicatedFeaturesInSubclasses() throws IOException {
		loadModelFiles("resolveDuplicatedFeaturesInSubclasses", "TestEcore.ecore");
		resolver.resolveDuplicatedFeaturesInSubclasses(resolver.getEPackage("p"));
		modelManager.saveEcores(AbstractTest.MODIFIED);
		assertModifiedFiles();
	}
}
