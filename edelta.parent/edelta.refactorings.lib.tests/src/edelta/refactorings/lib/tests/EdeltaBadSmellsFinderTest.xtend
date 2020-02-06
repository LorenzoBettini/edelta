package edelta.refactorings.lib.tests

import edelta.lib.AbstractEdelta
import edelta.refactorings.lib.EdeltaBadSmellsFinder
import org.eclipse.emf.ecore.ENamedElement
import org.junit.Before
import org.junit.Test

import static org.assertj.core.api.Assertions.*
import static org.junit.Assert.assertTrue
import static org.junit.Assert.assertNotNull
import static org.junit.Assert.assertEquals

class EdeltaBadSmellsFinderTest extends AbstractTest {
	var EdeltaBadSmellsFinder finder

	@Before
	def void setup() {
		finder = new EdeltaBadSmellsFinder
	}

	@Test
	def void test_ConstructorArgument() {
		finder = new EdeltaBadSmellsFinder(new AbstractEdelta() {})
		assertThat(finder)
			.isNotNull
	}

	@Test def void test_findDuplicateFeatures_whenNoDuplicates() {
		val p = factory.createEPackage => [
			createEClass("C1") => [
				createEAttribute("A1") => [
					EType = stringDataType
				]
			]
			createEClass("C2") => [
				createEAttribute("A1") => [
					EType = intDataType
				]
			]
		]
		val result = finder.findDuplicateFeatures(p)
		assertTrue("result: " + result, result.empty)
	}

	@Test def void test_findDuplicateFeatures_withDuplicates() {
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
		]
		val result = finder.findDuplicateFeatures(p)
		val expected = p.EClasses.map[EStructuralFeatures].flatten
		val actual = result.values.flatten
		assertIterable(actual, expected)
	}

	@Test def void test_findDuplicateFeatures_withDifferingAttributesByLowerBound() {
		val p = factory.createEPackage => [
			createEClass("C1") => [
				createEAttribute("A1") => [
					EType = stringDataType
					lowerBound = 1 // different lowerbound from C2.A1
				]
			]
			createEClass("C2") => [
				createEAttribute("A1") => [
					EType = stringDataType
					lowerBound = 2 // different lowerbound from C1.A1
				]
			]
		]
		val result = finder.findDuplicateFeatures(p)
		assertTrue("result: " + result, result.empty)
	}

	@Test def void test_findDuplicateFeatures_withDifferingContainment() {
		val p = factory.createEPackage => [
			createEClass("C1") => [
				createEReference("r1") => [
					EType = eClassReference
					containment = true
				]
			]
			createEClass("C2") => [
				createEReference("r1") => [
					EType = eClassReference
					containment = false
				]
			]
		]
		val result = finder.findDuplicateFeatures(p)
		assertTrue("result: " + result, result.empty)
	}

	@Test def void test_findDuplicateFeatures_withCustomEqualityPredicate() {
		val p = factory.createEPackage => [
			createEClass("C1") => [
				createEAttribute("A1") => [
					EType = stringDataType
					lowerBound = 1 // different lowerbound from C2.A1
				]
			]
			createEClass("C2") => [
				createEAttribute("A1") => [
					EType = stringDataType
					lowerBound = 2 // different lowerbound from C1.A1
				]
			]
		]
		// only check name and type, thus lowerBound is ignored
		// for comparison.
		val result = finder.
			findDuplicateFeaturesCustom(p) [
				f1, f2 | f1.name == f2.name && f1.EType == f2.EType
			]
		val expected = p.EClasses.map[EStructuralFeatures].flatten
		val actual = result.values.flatten
		assertIterable(actual, expected)
	}

	@Test def void test_findRedundantContainers() {
		val p = factory.createEPackage => [
			val containedWithRedundant = createEClass("ContainedWithRedundant")
			val containedWithOpposite = createEClass("ContainedWithOpposite")
			val containedWithContained = createEClass("ContainedWithContained")
			val containedWithOptional = createEClass("ContainedWithOptional")
			val anotherClass = createEClass("AnotherClass")
			val containedWithUnrelated = createEClass("Unrelated")
			val container = createEClass("Container") => [
				createEReference("containedWithRedundant") => [
					EType = containedWithRedundant
					containment = true
				]
				createEReference("containedWithUnrelated") => [
					EType = containedWithUnrelated
					containment = true
				]
				createEReference("containedWithOpposite") => [
					EType = containedWithOpposite
					containment = true
				]
				createEReference("containedWithOptional") => [
					EType = containedWithOptional
					containment = true
				]
			]
			containedWithRedundant.createEReference("redundant") => [
				EType = container
				lowerBound = 1
			]
			containedWithUnrelated.createEReference("unrelated") => [
				EType = anotherClass
				lowerBound = 1
			]
			containedWithOpposite.createEReference("correctWithOpposite") => [
				EType = container
				lowerBound = 1
				EOpposite = container.EReferences.last
			]
			containedWithContained.createEReference("correctWithContainment") => [
				EType = container
				lowerBound = 1
				// this is correct since it's another contament relation
				containment = true
			]
			containedWithOptional.createEReference("correctNotRequired") => [
				EType = container
				// this is correct since it's not required
			]
		]
		val result = finder.findRedundantContainers(p)
		// we expect the pair
		// redundant -> containedWithRedundant
		val expected = p.EClasses.head.EReferences.head -> p.EClasses.last.EReferences.head
		val actual = result.head
		assertThat(result).hasSize(1)
		assertNotNull(expected)
		assertNotNull(actual)
		assertEquals(expected, actual)
	}

	@Test def void test_findDeadClassifiers() {
		val p = factory.createEPackage => [
			createEClass("Unused1")
			val used1 = createEClass("Used1")
			val used2 = createEClass("Used2")
			createEClass("Unused2") => [
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
		val result = finder.findDeadClassifiers(p)
		assertIterable(result, #[p.EClasses.head])
	}

	@Test def void test_hasNoReferenceInThisPackage() {
		val otherPackage = factory.createEPackage
		val used1 = otherPackage.createEClass("Used1")
		val p = factory.createEPackage => [
			createEClass("HasNoReferenceInThisPackage") => [
				// has a reference to a class in a different package
				createEReference("used1") => [
					EType = used1
					containment = false
				]
			]
		]
		assertThat(finder.hasNoReferenceInThisPackage(p.EClasses.head))
			.isTrue
	}

	@Test def void test_findClassificationByHierarchy() {
		val p = factory.createEPackage => [
			val base = createEClass("Base")
			createEClass("Derived1") => [
				ESuperTypes += base
			]
			createEClass("Derived2") => [
				ESuperTypes += base
			]
			createEClass("Derived2") => [
				ESuperTypes += base
				// not in result because has features
				createEAttribute("anAttribute") => [
					EType = stringDataType
				]
			]
			val referenced = createEClass("Derived3") => [
				// not in result because it's referenced
				ESuperTypes += base
			]
			val another = createEClass("Another") => [
				createEReference("aRef") => [
					EType = referenced
				]
			]
			createEClass("Derived4") => [
				ESuperTypes += base
				ESuperTypes += another
				// not in result because has several superclasses
			]
		]
		val result = finder.findClassificationByHierarchy(p)
		assertThat(result)
			.containsExactlyEntriesOf(newHashMap(
				p.EClasses.head -> newArrayList(
					p.EClasses.get(1),
					p.EClasses.get(2)
				)
			))
	}

	@Test def void test_findConcreteAbstractMetaclasses() {
		val p = factory.createEPackage => [
			val base = createEClass("ConcreteAbstractMetaclass")
			val other = createEClass("CorrectAbstractMetaclass") => [
				abstract = true
			]
			val referred = createEClass("NonBaseClass")
			createEClass("Derived1") => [
				ESuperTypes += base
			]
			createEClass("Derived2") => [
				ESuperTypes += other
			]
			createEClass("Another") => [
				createEReference("aRef") => [
					EType = referred
				]
			]
		]
		var result = finder.findConcreteAbstractMetaclasses(p)
		assertIterable(result, #[p.EClasses.head])
	}

	@Test def void test_findAbstractConcreteMetaclasses() {
		val p = factory.createEPackage => [
			createEClass("AbstractConcreteMetaclass") => [
				abstract = true
			]
			val base = createEClass("AbstractMetaclass") => [
				abstract = true
			]
			createEClass("Derived1") => [
				ESuperTypes += base
			]
		]
		var result = finder.findAbstractConcreteMetaclasses(p)
		assertIterable(result, #[p.EClasses.head])
	}

	@Test def void test_findAbstractSubclassesOfConcreteSuperclass() {
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
		var result = finder.findAbstractSubclassesOfConcreteSuperclass(p)
		assertIterable(result, #[p.EClasses.last])
	}

	def private <T extends ENamedElement> void assertIterable(Iterable<T> actual, Iterable<? extends T> expected) {
		assertThat(actual).containsExactlyInAnyOrder(expected)
	}
}
