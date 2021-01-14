package edelta.refactorings.lib.tests;

import static edelta.lib.EdeltaLibrary.addNewAbstractEClass;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.xtext.xbase.lib.IterableExtensions.head;

import java.io.IOException;

import org.assertj.core.api.Assertions;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EPackage;
import org.junit.Before;
import org.junit.Test;

import java.util.Objects;

import edelta.lib.AbstractEdelta;
import edelta.refactorings.lib.EdeltaBadSmellsResolver;
import edelta.testutils.EdeltaTestUtils;

public class EdeltaBadSmellsResolverTest extends AbstractTest {
	private EdeltaBadSmellsResolver resolver;

	private String testModelDirectory;

	private String testModelFile;

	@Before
	public void setup() {
		resolver = new EdeltaBadSmellsResolver();
	}

	private void loadModelFile(final String testModelDirectory, final String testModelFile) {
		this.testModelDirectory = testModelDirectory;
		this.testModelFile = testModelFile;
		resolver
			.loadEcoreFile(AbstractTest.TESTECORES +
				testModelDirectory + "/" + testModelFile);
	}

	private void assertModifiedFile() throws IOException {
		// no need to explicitly validate:
		// if the ecore file is saved then it is valid
		EdeltaTestUtils.assertFilesAreEquals(
			AbstractTest.EXPECTATIONS + testModelDirectory + "/" + testModelFile,
			AbstractTest.MODIFIED + testModelFile);
	}

	@Test
	public void test_ConstructorArgument() {
		resolver = new EdeltaBadSmellsResolver(new AbstractEdelta() {
		});
		assertThat(resolver).isNotNull();
	}

	@Test
	public void test_resolveDuplicatedFeatures() throws IOException {
		loadModelFile("resolveDuplicatedFeatures", "TestEcore.ecore");
		resolver.resolveDuplicatedFeatures(resolver.getEPackage("p"));
		resolver.saveModifiedEcores(AbstractTest.MODIFIED);
		assertModifiedFile();
	}

	@Test
	public void test_resolveDeadClassifiers() throws IOException {
		loadModelFile("resolveDeadClassifiers", "TestEcore.ecore");
		resolver.resolveDeadClassifiers(resolver.getEPackage("p"),
			it -> Objects.equals(it.getName(), "Unused2"));
		resolver.saveModifiedEcores(AbstractTest.MODIFIED);
		assertModifiedFile();
	}

	@Test
	public void test_resolveRedundantContainers() throws IOException {
		loadModelFile("resolveRedundantContainers", "TestEcore.ecore");
		resolver.resolveRedundantContainers(resolver.getEPackage("p"));
		resolver.saveModifiedEcores(AbstractTest.MODIFIED);
		assertModifiedFile();
	}

	@Test
	public void test_resolveClassificationByHierarchy() throws IOException {
		loadModelFile("resolveClassificationByHierarchy", "TestEcore.ecore");
		resolver.resolveClassificationByHierarchy(resolver.getEPackage("p"));
		resolver.saveModifiedEcores(AbstractTest.MODIFIED);
		assertModifiedFile();
	}

	@Test
	public void test_resolveConcreteAbstractMetaclass() throws IOException {
		loadModelFile("resolveConcreteAbstractMetaclass", "TestEcore.ecore");
		resolver.resolveConcreteAbstractMetaclass(resolver.getEPackage("p"));
		resolver.saveModifiedEcores(AbstractTest.MODIFIED);
		assertModifiedFile();
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
		loadModelFile("resolveAbstractSubclassesOfConcreteSuperclasses", "TestEcore.ecore");
		resolver.resolveAbstractSubclassesOfConcreteSuperclasses(resolver.getEPackage("p"));
		resolver.saveModifiedEcores(AbstractTest.MODIFIED);
		assertModifiedFile();
	}

	@Test
	public void test_resolveDuplicatedFeaturesInSubclasses() throws IOException {
		loadModelFile("resolveDuplicatedFeaturesInSubclasses", "TestEcore.ecore");
		resolver.resolveDuplicatedFeaturesInSubclasses(resolver.getEPackage("p"));
		resolver.saveModifiedEcores(AbstractTest.MODIFIED);
		assertModifiedFile();
	}
}
