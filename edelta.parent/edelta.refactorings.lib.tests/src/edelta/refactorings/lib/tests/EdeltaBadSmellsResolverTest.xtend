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

class EdeltaBadSmellsResolverTest extends AbstractTest {
	var EdeltaBadSmellsResolver resolver

	@Before
	def void setup() {
		resolver = new EdeltaBadSmellsResolver
	}

	@Test
	def void test_ConstructorArgument() {
		resolver = new EdeltaBadSmellsResolver(new AbstractEdelta() {})
		assertThat(resolver)
			.isNotNull
	}

	@Test def void test_resolveDuplicateFeatures() {
		val p = factory.createEPackage => [
			createEClass("C1") => [
				createEAttribute("A1") => [
					EType = stringDataType
				]
			]
			createEClass("C2") => [
				createEAttribute("A1") => [
					EType = stringDataType
				]
			]
			createEClass("C3") => [
				createEAttribute("A1") => [
					EType = stringDataType
					lowerBound = 2
				]
			]
			createEClass("C4") => [
				createEAttribute("A1") => [
					EType = stringDataType
					lowerBound = 2
				]
			]
		]
		resolver.resolveDuplicatedFeatures(p)
		val classifiersNames = p.EClassifiers.map[name]
		assertThat(classifiersNames)
			.containsExactly("C1", "C2", "C3", "C4", "A1Element", "A1Element1")
		val classes = p.EClasses
		assertThat(classes.get(0).EAttributes).isEmpty // C1
		assertThat(classes.get(1).EAttributes).isEmpty // C2
		assertThat(classes.get(2).EAttributes).isEmpty // C3
		assertThat(classes.get(3).EAttributes).isEmpty // C4
		assertThat(classes.get(4).EAttributes).hasSize(1) // WithA1EString
		assertThat(classes.get(5).EAttributes).hasSize(1) // WithA1EString1
		val extractedA1NoLowerBound = classes.get(4).EAttributes.head
		assertThat(extractedA1NoLowerBound)
			.returns("A1", [name])
			.returns(stringDataType, [EAttributeType])
			.returns(0, [lowerBound])
		val extractedA1WithLowerBound = classes.get(5).EAttributes.head
		assertThat(extractedA1WithLowerBound)
			.returns("A1", [name])
			.returns(stringDataType, [EAttributeType])
			.returns(2, [lowerBound])
	}

@	Test def void test_resolveDeadClassifiers() {
		val p = factory.createEPackage => [
			createEClass("Unused1")
			createEClass("Unused2")
			val used1 = createEClass("Used1")
			val used2 = createEClass("Used2")
			createEClass("Unused3") => [
				createEReference("used1") => [
					EType = used1
					containment = true
				]
				createEReference("used2") => [
					EType = used2
					containment = false
				]
			]
		]
		resolver.resolveDeadClassifiers(p, [name == "Unused2"])
		assertThat(p.EClassifiers)
			.hasSize(4)
			.noneMatch[name == "Unused2"]
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
		assertEquals(1, eLiterals.get(0).value)
		assertEquals(2, eLiterals.get(1).value)
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

}
