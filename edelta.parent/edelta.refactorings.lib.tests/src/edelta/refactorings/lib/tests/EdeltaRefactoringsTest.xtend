package edelta.refactorings.lib.tests

import edelta.refactorings.lib.EdeltaRefactorings
import org.junit.Before
import org.junit.Test

import static extension org.assertj.core.api.Assertions.*
import org.eclipse.emf.ecore.EAttribute
import org.eclipse.emf.ecore.EReference
import org.eclipse.emf.ecore.EClass
import java.util.stream.Collectors
import edelta.lib.AbstractEdelta
import static org.junit.Assert.assertNull
import static org.junit.Assert.assertNotNull
import static org.junit.Assert.assertSame
import static org.junit.Assert.assertEquals
import org.eclipse.emf.ecore.EEnum

class EdeltaRefactoringsTest extends AbstractTest {
	var EdeltaRefactorings refactorings

	@Before
	def void setup() {
		refactorings = new EdeltaRefactorings
	}

	@Test
	def void test_ConstructorArgument() {
		refactorings = new EdeltaRefactorings(new AbstractEdelta() {})
		val c = createEClassWithoutPackage("C1")
		refactorings.addMandatoryAttr(c, "test", stringDataType)
		val attr = c.EStructuralFeatures.filter(EAttribute).head
		assertThat(attr)
			.returns("test", [name])
	}

	@Test
	def void test_addMandatoryAttr() {
		val c = createEClassWithoutPackage("C1")
		refactorings.addMandatoryAttr(c, "test", stringDataType)
		val attr = c.EStructuralFeatures.filter(EAttribute).head
		assertThat(attr)
			.returns("test", [name])
			.returns(stringDataType, [EAttributeType])
			.returns(true, [isRequired])
	}

	@Test
	def void test_mergeReferences() {
		val refType = createEClassWithoutPackage("RefType")
		val c = createEClassWithoutPackage("C1") => [
			createEReference("ref1") => [
				EType = refType
			]
			createEReference("ref2") => [
				EType = refType
			]
		]
		val merged = refactorings.mergeReferences("test", refType, 
			c.EStructuralFeatures.filter(EReference).toList
		)
		assertThat(c.EStructuralFeatures).isEmpty
		assertThat(merged)
			.returns("test", [name])
			.returns(refType, [EReferenceType])
	}

	@Test
	def void test_mergeAttributes() {
		val c = createEClassWithoutPackage("C1") => [
			createEAttribute("a1") => [
				EType = stringDataType
			]
			createEAttribute("a2") => [
				EType = stringDataType
			]
		]
		val merged = refactorings.mergeAttributes("test", stringDataType, 
			c.EStructuralFeatures.filter(EAttribute).toList
		)
		assertThat(c.EStructuralFeatures).isEmpty
		assertThat(merged)
			.returns("test", [name])
			.returns(stringDataType, [EAttributeType])
	}

	@Test
	def void test_introduceSubclasses() {
		val p = factory.createEPackage
		val enum = p.createEEnum("AnEnum") => [
			createEEnumLiteral("Lit1")
			createEEnumLiteral("Lit2")
		]
		val c = p.createEClass("C1") => [
			abstract = false
		]
		val attr = c.createEAttribute("attr") => [
			EType = enum
		]
		refactorings.introduceSubclasses(c, attr, enum)
		assertThat(c.isAbstract).isTrue
		assertThat(c.EStructuralFeatures).isEmpty
		assertThat(p.EClassifiers.filter(EClass))
			.hasSize(3)
			.allSatisfy[
				if (name != "C1") {
					assertThat(name).startsWith("Lit")
					assertThat(ESuperTypes)
						.containsExactly(c)
				}
			]
	}

	@Test
	def void test_extractSuperclass() {
		val p = factory.createEPackage
		val superClass = p.createEClass("SuperClass")
		val c1 = p.createEClass("C1")
		val c2 = p.createEClass("C2")
		val attr1 = c1.createEAttribute("attr")
		val attr2 = c2.createEAttribute("attr")
		assertThat(superClass.EStructuralFeatures).isEmpty
		assertThat(c1.EStructuralFeatures).isNotEmpty
		assertThat(c2.EStructuralFeatures).isNotEmpty

		refactorings.extractSuperclass(superClass, #[attr1, attr2])

		assertThat(c1.EStructuralFeatures).isEmpty
		assertThat(c2.EStructuralFeatures).isEmpty
		assertThat(superClass.EStructuralFeatures)
			.containsExactly(attr1)
		assertThat(c1.ESuperTypes).containsExactly(superClass)
		assertThat(c2.ESuperTypes).containsExactly(superClass)
	}

	@Test
	def void test_extractMetaClass() {
		val p = factory.createEPackage
		val person = p.createEClass("Person")
		val workPlace = p.createEClass("WorkPlace")
		val personWorks = person.createEReference("works") => [
			lowerBound = 1
		]
		val workPlacePersons = workPlace.createEReference("persons") => [
			EOpposite = personWorks
			EType = person
		]
		personWorks.EType = workPlace
		personWorks.EOpposite = workPlacePersons
		val workingPosition = p.createEClass("WorkingPosition")

		assertThat(workingPosition.EStructuralFeatures).isEmpty
		assertThat(workPlace.EStructuralFeatures)
			.contains(workPlacePersons)

		refactorings.extractMetaClass(workingPosition, personWorks, "position", "works")

		assertThat(workingPosition.EStructuralFeatures)
			.hasSize(2)
			.contains(workPlacePersons)
			.anySatisfy[
				assertThat
					.returns("works", [name])
					.returns(workPlace, [EType])
					.returns(1, [lowerBound])
			]
		assertThat(workPlace.EStructuralFeatures)
			.hasSize(1)
			.doesNotContain(workPlacePersons)
			.anySatisfy[
				assertThat
					.returns("position", [name])
					.returns(workingPosition, [EType])
			]
		assertThat(person.EStructuralFeatures)
			.containsExactly(personWorks)
	}

	@Test def void test_extractSuperClass() {
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
		// we know the duplicates, manually
		val duplicates = p.EClasses.map[EAttributes].flatten.toList
		refactorings.extractSuperclass(duplicates)
		val classifiersNames = p.EClassifiers.map[name]
		assertThat(classifiersNames)
			.hasSize(3)
			.containsExactly("C1", "C2", "A1Element")
		val classes = p.EClasses
		assertThat(classes.get(0).EAttributes).isEmpty
		assertThat(classes.get(1).EAttributes).isEmpty
		assertThat(classes.get(2).EAttributes).hasSize(1)
		val extracted = classes.get(2).EAttributes.head
		assertThat(extracted.name).isEqualTo("A1")
		assertThat(extracted.EAttributeType).isEqualTo(stringDataType)
	}

	@Test def void test_extractSuperClassUnique() {
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
		// we know the duplicates, manually
		val attributes = p.EClasses.map[EAttributes].flatten.toList
		refactorings.extractSuperclass(
			attributes.take(2).toList // A1 without lowerbound
		)
		refactorings.extractSuperclass(
			attributes.stream.skip(2).collect(Collectors.toList) // A1 with lowerbound
		)
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
		assertThat(extractedA1NoLowerBound.name).isEqualTo("A1")
		assertThat(extractedA1NoLowerBound.EAttributeType).isEqualTo(stringDataType)
		assertThat(extractedA1NoLowerBound.lowerBound).isZero
		val extractedA1WithLowerBound = classes.get(5).EAttributes.head
		assertThat(extractedA1WithLowerBound.name).isEqualTo("A1")
		assertThat(extractedA1WithLowerBound.EAttributeType).isEqualTo(stringDataType)
		assertThat(extractedA1WithLowerBound.lowerBound).isEqualTo(2)
	}

	@Test def void test_redundantContainerToEOpposite() {
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
		refactorings.redundantContainerToEOpposite(#[redundant -> opposite])
		assertNotNull(redundant.EOpposite)
		assertSame(redundant.EOpposite, opposite)
		assertSame(opposite.EOpposite, redundant)
	}

	@Test def void test_classificationByHierarchyToEnum() {
		val p = factory.createEPackage => [
			val base = createEClass("Base")
			createEClass("Derived1") => [
				ESuperTypes += base
			]
			createEClass("Derived2") => [
				ESuperTypes += base
			]
		]
		val base = p.EClasses.get(0)
		val derived1 = p.EClasses.get(1)
		val derived2 = p.EClasses.get(2)
		refactorings.classificationByHierarchyToEnum(#{base -> #[derived1, derived2]})
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
}
