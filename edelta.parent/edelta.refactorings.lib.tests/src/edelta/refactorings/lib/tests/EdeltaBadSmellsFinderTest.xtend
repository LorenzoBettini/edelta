package edelta.refactorings.lib.tests

import edelta.lib.AbstractEdelta
import edelta.refactorings.lib.EdeltaBadSmellsFinder
import org.junit.Before
import org.junit.Test

import static org.assertj.core.api.Assertions.*

import static extension edelta.lib.EdeltaLibrary.*
import org.assertj.core.util.Maps

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
		val p = createEPackage("p") [
			addNewEClass("C1") [
				addNewEAttribute("a1", stringDataType)
			]
			addNewEClass("C2") [
				addNewEAttribute("a1", intDataType)
			]
		]
		val result = finder.findDuplicateFeatures(p)
		assertThat(result).isEmpty
	}

	@Test def void test_findDuplicateFeatures_withDuplicates() {
		val p = createEPackage("p") [
			addNewEClass("C1") [
				addNewEAttribute("a1", stringDataType)
			]
			addNewEClass("C2") [
				addNewEAttribute("a1", stringDataType)
			]
		]
		val result = finder.findDuplicateFeatures(p)
		assertThat(result)
			.containsExactly(
				entry(p.findEStructuralFeature("C1", "a1"),
					#[
						p.findEStructuralFeature("C1", "a1"),
						p.findEStructuralFeature("C2", "a1")
					]
				)
			)
	}

	@Test def void test_findDuplicateFeatures_withDifferingAttributesByLowerBound() {
		val p = createEPackage("p") [
			addNewEClass("C1") [
				addNewEAttribute("a1", stringDataType) [
					lowerBound = 1 // different lowerbound from C2.a1
				]
			]
			addNewEClass("C2") [
				addNewEAttribute("a1", stringDataType) [
					lowerBound = 2 // different lowerbound from C1.a1
				]
			]
		]
		val result = finder.findDuplicateFeatures(p)
		assertThat(result).isEmpty
	}

	@Test def void test_findDuplicateFeatures_withDifferingContainment() {
		val p = createEPackage("p") [
			addNewEClass("C1") => [
				addNewContainmentEReference("r1", eClassReference)
			]
			addNewEClass("C2") => [
				addNewEReference("r1", eClassReference)
			]
		]
		val result = finder.findDuplicateFeatures(p)
		assertThat(result).isEmpty
	}

	@Test def void test_findDuplicateFeatures_withCustomEqualityPredicate() {
		val p = createEPackage("p") [
			addNewEClass("C1") [
				addNewEAttribute("a1", stringDataType) [
					lowerBound = 1 // different lowerbound from C2.a1
				]
			]
			addNewEClass("C2") [
				addNewEAttribute("a1", stringDataType) [
					lowerBound = 2 // different lowerbound from C1.a1
				]
			]
		]
		// only check name and type, thus lowerBound is ignored
		// for comparison.
		val result = finder.
			findDuplicateFeaturesCustom(p) [
				f1, f2 | f1.name == f2.name && f1.EType == f2.EType
			]
		assertThat(result)
			.containsExactly(
				entry(p.findEStructuralFeature("C1", "a1"),
					#[
						p.findEStructuralFeature("C1", "a1"),
						p.findEStructuralFeature("C2", "a1")
					]
				)
			)
	}

	@Test def void test_findRedundantContainers() {
		val p = createEPackage("p") [
			val containedWithRedundant = addNewEClass("ContainedWithRedundant")
			val containedWithOpposite = addNewEClass("ContainedWithOpposite")
			val containedWithContained = addNewEClass("ContainedWithContained")
			val containedWithOptional = addNewEClass("ContainedWithOptional")
			val anotherClass = addNewEClass("AnotherClass")
			val containedWithUnrelated = addNewEClass("Unrelated")
			val container = addNewEClass("Container") [
				addNewContainmentEReference("containedWithRedundant", containedWithRedundant)
				addNewContainmentEReference("containedWithUnrelated", containedWithUnrelated)
				addNewContainmentEReference("containedWithOpposite", containedWithOpposite)
				addNewContainmentEReference("containedWithOptional", containedWithOptional)
			]
			containedWithRedundant.addNewEReference("redundant", container) [
				lowerBound = 1
			]
			containedWithUnrelated.addNewEReference("unrelated", anotherClass) [
				lowerBound = 1
			]
			containedWithOpposite.addNewEReference("correctWithOpposite", container) [
				lowerBound = 1
				EOpposite = container.EReferences.last
			]
			// this is correct since it's another containment relation
			containedWithContained.addNewContainmentEReference("correctWithContainment", container) [
				lowerBound = 1
			]
			// this is correct since it's not required
			containedWithOptional.addNewEReference("correctNotRequired", container)
		]
		val result = finder.findRedundantContainers(p)
		// we expect the pair
		// redundant -> containedWithRedundant
		assertThat(result)
			.containsExactly(
				p.findEReference("ContainedWithRedundant", "redundant") ->
				p.findEReference("Container", "containedWithRedundant")
			)
	}

	@Test def void test_findDeadClassifiers() {
		val p = createEPackage("p") [
			addNewEClass("Unused1")
			val used1 = addNewEClass("Used1")
			val used2 = addNewEClass("Used2")
			addNewEClass("Unused2") [
				addNewContainmentEReference("used1", used1)
				addNewEReference("used2", used2)
			]
		]
		val result = finder.findDeadClassifiers(p)
		assertThat(result)
			.containsExactly(p.findEClass("Unused1"))
	}

	@Test def void test_hasNoReferenceInThisPackage() {
		val otherPackage = createEPackage("otherPackage")
		val used1 = otherPackage.addNewEClass("Used1")
		val p = createEPackage("p") [
			addNewEClass("HasNoReferenceInThisPackage") [
				// has a reference to a class in a different package
				addNewEReference("used1", used1)
			]
		]
		assertThat(finder.hasNoReferenceInThisPackage(p.EClasses.head))
			.isTrue
	}

	@Test def void test_findClassificationByHierarchy() {
		val p = createEPackage("p") [
			val base = addNewEClass("Base")
			base.addNewSubclass("Derived1")
			base.addNewSubclass("Derived2")
			// not in result because has features
			base.addNewSubclass("DerivedOK") [
				addNewEAttribute("anAttribute", stringDataType)
			]
			// not in result because it's referenced
			val referenced = base.addNewSubclass("DerivedOK2")
			val another = addNewEClass("Another") [
				addNewEReference("aRef", referenced)
			]
			// not in result because has several superclasses
			base.addNewSubclass("DerivedOK3") [
				ESuperTypes += another
			]
		]
		val result = finder.findClassificationByHierarchy(p)
		assertThat(result)
			.containsExactly(
				entry(p.findEClass("Base"),
					#[
						p.findEClass("Derived1"),
						p.findEClass("Derived2")
					]
			))
	}

	@Test def void test_findClassificationByHierarchy_withOneSubclass() {
		val p = createEPackage("p") [
			addNewEClass("Base") [
				addNewSubclass("Derived1")
			]
		]
		val result = finder.findClassificationByHierarchy(p)
		assertThat(result)
			.isEmpty
	}

	@Test def void test_findConcreteAbstractMetaclasses() {
		val p = createEPackage("p") [
			val base = addNewEClass("ConcreteAbstractMetaclass")
			val other = addNewAbstractEClass("CorrectAbstractMetaclass")
			val referred = addNewEClass("NonBaseClass")
			base.addNewSubclass("Derived1")
			other.addNewSubclass("Derived2")
			addNewEClass("Another") [
				addNewEReference("aRef", referred)
			]
		]
		var result = finder.findConcreteAbstractMetaclasses(p)
		assertThat(result)
			.containsExactly(p.findEClass("ConcreteAbstractMetaclass"))
	}

	@Test def void test_findAbstractConcreteMetaclasses() {
		val p = createEPackage("p") [
			addNewAbstractEClass("AbstractConcreteMetaclass")
			addNewAbstractEClass("AbstractMetaclass") [
				addNewSubclass("Derived1")
			]
		]
		var result = finder.findAbstractConcreteMetaclasses(p)
		assertThat(result)
			.containsExactly(p.findEClass("AbstractConcreteMetaclass"))
	}

	@Test def void test_findAbstractSubclassesOfConcreteSuperclasses() {
		val p = createEPackage("p") [
			val abstractSuperclass = addNewAbstractEClass("AbstractSuperclass")
			val concreteSuperclass1 = addNewEClass("ConcreteSuperclass1")
			val concreteSuperclass2 = addNewEClass("ConcreteSuperclass2")
			addNewAbstractEClass("WithoutSmell") [
				ESuperTypes += #[concreteSuperclass1, abstractSuperclass]
			]
			addNewAbstractEClass("WithSmell") [
				ESuperTypes += #[concreteSuperclass1, concreteSuperclass2]
			]
		]
		var result = finder.findAbstractSubclassesOfConcreteSuperclasses(p)
		assertThat(result)
			.containsOnly(p.findEClass("WithSmell"))
	}

	@Test def void test_directSubclasses() {
		val p = createEPackage("p") [
			val superclass = addNewEClass("ASuperclass")
			val subclass1 = superclass.addNewSubclass("ASubclass1")
			subclass1.addNewSubclass("ASubclass1Subclass")
			superclass.addNewSubclass("ASubclass2")
		]
		assertThat(finder.directSubclasses(p.findEClass("ASuperclass")).map[name])
			.containsExactlyInAnyOrder("ASubclass1", "ASubclass2")
		assertThat(finder.directSubclasses(p.findEClass("ASubclass1")).map[name])
			.containsExactlyInAnyOrder("ASubclass1Subclass")
		assertThat(finder.directSubclasses(p.findEClass("ASubclass1Subclass")))
			.isEmpty
	}

	@Test def void test_findDuplicateFeaturesInSubclasses() {
		val p = createEPackage("p") [
			val superclassWithDuplicatesInSubclasses =
				addNewEClass("SuperClassWithDuplicatesInSubclasses")
			superclassWithDuplicatesInSubclasses.addNewSubclass("C1") [
				addNewEAttribute("A1", stringDataType)
			]
			superclassWithDuplicatesInSubclasses.addNewSubclass("C2") [
				addNewEAttribute("A1", stringDataType)
			]
			val superclassWithoutDuplicatesInAllSubclasses =
				addNewEClass("SuperClassWithoutDuplicatesInAllSubclasses")
			superclassWithoutDuplicatesInAllSubclasses.addNewSubclass("D1") [
				addNewEAttribute("A1", stringDataType)
			]
			superclassWithoutDuplicatesInAllSubclasses.addNewSubclass("D2") [
				addNewEAttribute("A1", stringDataType)
			]
			superclassWithoutDuplicatesInAllSubclasses.addNewSubclass("D3") [
				addNewEAttribute("A1", intDataType) // all subclasses must have the duplicate
					// this is not a duplicate
			]
		]
		val result = finder.findDuplicateFeaturesInSubclasses(p)
		assertThat(result)
			.containsExactly(
				entry(
					p.findEClass("SuperClassWithDuplicatesInSubclasses"),
					Maps.newHashMap(
						p.findEStructuralFeature("C1", "A1"),
						#[
							p.findEStructuralFeature("C1", "A1"),
							p.findEStructuralFeature("C2", "A1")
						]
					)
				)
			)
	}

}
