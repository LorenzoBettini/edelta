package edelta.refactorings.lib.tests

import edelta.lib.AbstractEdelta
import edelta.refactorings.lib.EdeltaBadSmellsResolver
import org.junit.Before
import org.junit.Test

import static org.assertj.core.api.Assertions.*

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
}
