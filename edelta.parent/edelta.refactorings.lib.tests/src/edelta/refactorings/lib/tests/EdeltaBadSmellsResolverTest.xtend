package edelta.refactorings.lib.tests

import edelta.lib.AbstractEdelta
import edelta.refactorings.lib.EdeltaBadSmellsResolver
import org.eclipse.emf.ecore.EClass
import org.eclipse.emf.ecore.EEnum
import org.junit.Before
import org.junit.Test

import static org.assertj.core.api.Assertions.*
import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertNotNull
import static org.junit.Assert.assertNull
import static org.junit.Assert.assertSame
import static org.junit.Assert.assertFalse
import static org.junit.Assert.assertTrue
import edelta.testutils.EdeltaTestUtils

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
		val p = factory.createEPackage => [
			val containedWithRedundant = createEClass("ContainedWithRedundant")
			val container = createEClass("Container") => [
				createEReference("containedWithRedundant") => [
					EType = containedWithRedundant
					containment = true
				]
			]
			containedWithRedundant.createEReference("redundant") => [
				EType = container
				lowerBound = 1
			]
		]
		val redundant = p.EClasses.head.EReferences.head
		val opposite = p.EClasses.last.EReferences.head
		assertNull(redundant.EOpposite)
		assertNull(opposite.EOpposite)
		resolver.resolveRedundantContainers(p)
		assertNotNull(redundant.EOpposite)
		assertSame(redundant.EOpposite, opposite)
		assertSame(opposite.EOpposite, redundant)
	}

	@Test def void test_resolveClassificationByHierarchy() {
		val p = factory.createEPackage => [
			val base = createEClass("Base")
			createEClass("Derived1") => [
				ESuperTypes += base
			]
			createEClass("Derived2") => [
				ESuperTypes += base
			]
		]
		resolver.resolveClassificationByHierarchy(p)
		assertEquals(2, p.EClassifiers.size)
		val enum = p.EClassifiers.last as EEnum
		assertEquals("BaseType", enum.name)
		val eLiterals = enum.ELiterals
		assertEquals(2, eLiterals.size)
		assertEquals("DERIVED1", eLiterals.get(0).name)
		assertEquals("DERIVED2", eLiterals.get(1).name)
		assertEquals(0, eLiterals.get(0).value)
		assertEquals(1, eLiterals.get(1).value)
		val c = p.EClassifiers.head as EClass
		val attr = findEAttribute(c, "baseType")
		assertSame(enum, attr.EType)
	}

	@Test def void test_resolveConcreteAbstractMetaclass() {
		val p = factory.createEPackage => [
			val base = createEClass("ConcreteAbstractMetaclass")
			createEClass("Derived1") => [
				ESuperTypes += base
			]
		]
		val c = p.EClasses.head
		assertFalse(c.abstract)
		resolver.resolveConcreteAbstractMetaclass(p)
		assertTrue(c.abstract)
	}

	@Test def void test_resolveAbstractConcreteMetaclass() {
		val p = factory.createEPackage => [
			createEClass("AbstractConcreteMetaclass") => [
				abstract = true
			]
		]
		val c = p.EClasses.head
		assertTrue(c.abstract)
		resolver.resolveAbstractConcreteMetaclass(p)
		assertFalse(c.abstract)
	}

	@Test def void test_resolveAbstractSubclassesOfConcreteSuperclasses() {
		val p = factory.createEPackage => [
			val abstractSuperclass = createEClass("AbstractSuperclass") => [
				abstract = true
			]
			val concreteSuperclass1 = createEClass("ConcreteSuperclass1")
			val concreteSuperclass2 = createEClass("ConcreteSuperclass2")
			createEClass("WithoutSmell") => [
				abstract = true
				ESuperTypes += #[concreteSuperclass1, abstractSuperclass]
			]
			createEClass("WithSmell") => [
				abstract = true
				ESuperTypes += #[concreteSuperclass1, concreteSuperclass2]
			]
		]
		assertThat(p.EClasses.last.isAbstract).isTrue
		resolver.resolveAbstractSubclassesOfConcreteSuperclasses(p)
		assertThat(p.EClasses.last.isAbstract).isFalse
	}

	@Test def void test_resolveDuplicateFeaturesInSubclasses() {
		val p = createEPackage("p") => [
			val superclassWithDuplicatesInSubclasses = createEClass("SuperClassWithDuplicatesInSubclasses")
			createEClass("C1") => [
				ESuperTypes += superclassWithDuplicatesInSubclasses
				createEAttribute("A1") => [
					EType = stringDataType
				]
			]
			createEClass("C2") => [
				ESuperTypes += superclassWithDuplicatesInSubclasses
				createEAttribute("A1") => [
					EType = stringDataType
				]
			]
			val superclassWithoutDuplicatesInAllSubclasses = createEClass("SuperClassWithoutDuplicatesInAllSubclasses")
			createEClass("D1") => [
				ESuperTypes += superclassWithoutDuplicatesInAllSubclasses
				createEAttribute("A1") => [
					EType = stringDataType
				]
			]
			createEClass("D2") => [
				ESuperTypes += superclassWithoutDuplicatesInAllSubclasses
				createEAttribute("A1") => [
					EType = stringDataType
				]
			]
			createEClass("D3") => [
				ESuperTypes += superclassWithoutDuplicatesInAllSubclasses
				createEAttribute("A1") => [
					EType = intDataType // all subclasses must have the duplicate
					// this is not a duplicate
				]
			]
		]
		val superClass = p.EClasses.head
		val classesWithDuplicates = p.EClasses.filter[name.startsWith("C")]
		assertThat(classesWithDuplicates
			.map[EStructuralFeatures]
			.flatten
			.map[name]
			.toSet)
			.containsOnly("A1")
		assertThat(superClass.EStructuralFeatures).isEmpty
		resolver.resolveDuplicateFeaturesInSubclasses(p)
		assertThat(classesWithDuplicates.map[EStructuralFeatures].flatten)
			.isEmpty
		assertThat(superClass.EStructuralFeatures.map[name])
			.containsOnly("A1")
	}
}
