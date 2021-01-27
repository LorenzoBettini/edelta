package edelta.refactorings.lib.tests;

import static edelta.lib.EdeltaLibrary.addNewAbstractEClass;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.xtext.xbase.lib.IterableExtensions.head;

import java.io.IOException;

import org.assertj.core.api.Assertions;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EPackage;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Objects;

import edelta.lib.AbstractEdelta;
import edelta.refactorings.lib.EdeltaBadSmellsResolver;
import edelta.testutils.EdeltaTestUtils;

public class EdeltaBadSmellsResolverTest extends AbstractTest {
	private EdeltaBadSmellsResolver resolver;

	private String testModelDirectory;

	private List<String> testModelFiles;

	@Before
	public void setup() {
		resolver = new EdeltaBadSmellsResolver();
	}

	private void loadModelFiles(String testModelDirectory, String... testModelFiles) {
		this.testModelDirectory = testModelDirectory;
		this.testModelFiles = asList(testModelFiles);
		for (String testModelFile : testModelFiles) {
			resolver
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
	public void test_ConstructorArgument() {
		resolver = new EdeltaBadSmellsResolver(new AbstractEdelta() {
		});
		assertThat(resolver).isNotNull();
	}

	@Test
	public void test_resolveDuplicatedFeatures() throws IOException {
		loadModelFiles("resolveDuplicatedFeatures", "TestEcore.ecore");
		resolver.resolveDuplicatedFeatures(resolver.getEPackage("p"));
		resolver.saveModifiedEcores(AbstractTest.MODIFIED);
		assertModifiedFiles();
	}

	@Test
	public void test_resolveDeadClassifiers() throws IOException {
		loadModelFiles("resolveDeadClassifiers", "TestEcore.ecore");
		// specifies to remove only Unused2 even if there are other
		// dead classifiers
		resolver.resolveDeadClassifiers(resolver.getEPackage("p"),
			it -> Objects.equals(it.getName(), "Unused2"));
		resolver.saveModifiedEcores(AbstractTest.MODIFIED);
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
		resolver.resolveDeadClassifiers(resolver.getEPackage("p"),
			it -> true);
		resolver.saveModifiedEcores(AbstractTest.MODIFIED);
		assertModifiedFiles();
	}

	@Test
	public void test_resolveRedundantContainers() throws IOException {
		loadModelFiles("resolveRedundantContainers", "TestEcore.ecore");
		resolver.resolveRedundantContainers(resolver.getEPackage("p"));
		resolver.saveModifiedEcores(AbstractTest.MODIFIED);
		assertModifiedFiles();
	}

	@Test
	public void test_resolveClassificationByHierarchy() throws IOException {
		loadModelFiles("resolveClassificationByHierarchy", "TestEcore.ecore");
		resolver.resolveClassificationByHierarchy(resolver.getEPackage("p"));
		resolver.saveModifiedEcores(AbstractTest.MODIFIED);
		assertModifiedFiles();
	}

	@Test
	public void test_resolveConcreteAbstractMetaclass() throws IOException {
		loadModelFiles("resolveConcreteAbstractMetaclass", "TestEcore.ecore");
		resolver.resolveConcreteAbstractMetaclass(resolver.getEPackage("p"));
		resolver.saveModifiedEcores(AbstractTest.MODIFIED);
		assertModifiedFiles();
	}

	@Test
	public void test_resolveAbstractConcreteMetaclass() {
		final EPackage p = createEPackage("p",
			pack -> addNewAbstractEClass(pack, "AbstractConcreteMetaclass"));
		final EClass c = head(EClasses(p));
		resolver.resolveAbstractConcreteMetaclass(p);
		Assertions.assertThat(c.isAbstract()).isFalse();
	}

	@Test
	public void test_resolveAbstractSubclassesOfConcreteSuperclasses() throws IOException {
		loadModelFiles("resolveAbstractSubclassesOfConcreteSuperclasses", "TestEcore.ecore");
		resolver.resolveAbstractSubclassesOfConcreteSuperclasses(resolver.getEPackage("p"));
		resolver.saveModifiedEcores(AbstractTest.MODIFIED);
		assertModifiedFiles();
	}

	@Test
	public void test_resolveDuplicatedFeaturesInSubclasses() throws IOException {
		loadModelFiles("resolveDuplicatedFeaturesInSubclasses", "TestEcore.ecore");
		resolver.resolveDuplicatedFeaturesInSubclasses(resolver.getEPackage("p"));
		resolver.saveModifiedEcores(AbstractTest.MODIFIED);
		assertModifiedFiles();
	}
}
