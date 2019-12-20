package edelta.refactorings.lib.tests

import edelta.refactorings.lib.MMrefactorings
import org.junit.Before
import org.junit.Test

import static extension org.assertj.core.api.Assertions.*
import org.eclipse.emf.ecore.EAttribute
import org.eclipse.emf.ecore.EReference
import org.eclipse.emf.ecore.EClass

class MMrefactoringsTest extends AbstractTest {
	var MMrefactorings refactorings

	@Before
	def void setup() {
		refactorings = new MMrefactorings
	}

	@Test
	def void test_addMandatoryAttr() {
		val c = createEClass("C1")
		refactorings.addMandatoryAttr(c, "test", stringDataType)
		val attr = c.EStructuralFeatures.filter(EAttribute).head
		assertThat(attr)
			.returns("test", [name])
			.returns(stringDataType, [EAttributeType])
			.returns(true, [isRequired])
	}

	@Test
	def void test_mergeReferences() {
		val refType = createEClass("RefType")
		val c = createEClass("C1") => [
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
		val c = createEClass("C1") => [
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
}
