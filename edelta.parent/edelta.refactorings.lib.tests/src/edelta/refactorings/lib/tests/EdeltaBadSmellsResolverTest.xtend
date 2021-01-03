package edelta.refactorings.lib.tests

import edelta.lib.AbstractEdelta
import edelta.refactorings.lib.EdeltaBadSmellsResolver
import edelta.testutils.EdeltaTestUtils
import org.junit.Before
import org.junit.Test

import static org.assertj.core.api.Assertions.*

import static extension edelta.lib.EdeltaLibrary.*

class EdeltaBadSmellsResolverTest extends AbstractTest {
	var EdeltaBadSmellsResolver resolver

	var String testModelDirectory;

	var String testModelFile;

	@Before
	def void setup() {
		resolver = new EdeltaBadSmellsResolver
	}

	def private void loadModelFile(String testModelDirectory, String testModelFile) {
		this.testModelDirectory = testModelDirectory;
		this.testModelFile = testModelFile;
		resolver
			.loadEcoreFile(AbstractTest.TESTECORES +
					testModelDirectory +
					"/" +
					testModelFile);
	}

	def private void assertModifiedFile() {
		// no need to explicitly validate:
		// if the ecore file is saved then it is valid
		EdeltaTestUtils.assertFilesAreEquals(
				AbstractTest.EXPECTATIONS +
					testModelDirectory +
					"/" +
					testModelFile,
				AbstractTest.MODIFIED + testModelFile);
	}

	@Test
	def void test_ConstructorArgument() {
		resolver = new EdeltaBadSmellsResolver(new AbstractEdelta() {})
		assertThat(resolver)
			.isNotNull
	}

	@Test def void test_resolveDuplicatedFeatures() {
		loadModelFile("resolveDuplicatedFeatures", "TestEcore.ecore")
		resolver.resolveDuplicatedFeatures(resolver.getEPackage("p"))
		resolver.saveModifiedEcores(MODIFIED);
		assertModifiedFile
	}

@	Test def void test_resolveDeadClassifiers() {
		loadModelFile("resolveDeadClassifiers", "TestEcore.ecore")
		resolver.resolveDeadClassifiers(resolver.getEPackage("p"), [name == "Unused2"])
		resolver.saveModifiedEcores(MODIFIED);
		assertModifiedFile
	}

	@Test def void test_resolveRedundantContainers() {
		loadModelFile("resolveRedundantContainers", "TestEcore.ecore")
		resolver.resolveRedundantContainers(resolver.getEPackage("p"))
		resolver.saveModifiedEcores(MODIFIED);
		assertModifiedFile
	}

	@Test def void test_resolveClassificationByHierarchy() {
		loadModelFile("resolveClassificationByHierarchy", "TestEcore.ecore")
		resolver.resolveClassificationByHierarchy(resolver.getEPackage("p"))
		resolver.saveModifiedEcores(MODIFIED);
		assertModifiedFile
	}

	@Test def void test_resolveConcreteAbstractMetaclass() {
		loadModelFile("resolveConcreteAbstractMetaclass", "TestEcore.ecore")
		resolver.resolveConcreteAbstractMetaclass(resolver.getEPackage("p"))
		resolver.saveModifiedEcores(MODIFIED);
		assertModifiedFile
	}

	@Test def void test_resolveAbstractConcreteMetaclass() {
		val p = createEPackage("p") [
			addNewAbstractEClass("AbstractConcreteMetaclass")
		]
		val c = p.EClasses.head
		resolver.resolveAbstractConcreteMetaclass(p)
		assertThat(c.abstract).isFalse
	}

	@Test def void test_resolveAbstractSubclassesOfConcreteSuperclasses() {
		loadModelFile("resolveAbstractSubclassesOfConcreteSuperclasses", "TestEcore.ecore")
		resolver.resolveAbstractSubclassesOfConcreteSuperclasses(resolver.getEPackage("p"))
		resolver.saveModifiedEcores(MODIFIED);
		assertModifiedFile
	}

	@Test def void test_resolveDuplicateFeaturesInSubclasses() {
		loadModelFile("resolveDuplicateFeaturesInSubclasses", "TestEcore.ecore")
		resolver.resolveDuplicateFeaturesInSubclasses(resolver.getEPackage("p"))
		resolver.saveModifiedEcores(MODIFIED);
		assertModifiedFile
	}
}
