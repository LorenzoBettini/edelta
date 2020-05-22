package edelta.refactorings.lib.tests

import edelta.lib.AbstractEdelta
import edelta.refactorings.lib.EdeltaBadSmellsChecker
import org.junit.Before
import org.junit.Test

import static org.assertj.core.api.Assertions.*
import static org.junit.Assert.*
import edelta.refactorings.lib.tests.utils.InMemoryLoggerAppender

class EdeltaBadSmellsCheckerTest extends AbstractTest {
	var EdeltaBadSmellsChecker checker
	var InMemoryLoggerAppender appender

	@Before
	def void setup() {
		checker = new EdeltaBadSmellsChecker
		appender = new InMemoryLoggerAppender
		checker.logger.addAppender(appender)
	}

	@Test
	def void test_ConstructorArgument() {
		checker = new EdeltaBadSmellsChecker(new AbstractEdelta() {})
		assertThat(checker)
			.isNotNull
	}

	@Test def void test_checkDuplicateFeatures_whenNoDuplicates() {
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
		checker.checkDuplicateFeatures(p)
		assertThat(appender.result).isEmpty
	}

	@Test def void test_checkDuplicateFeatures_withDuplicates() {
		val p = factory.createEPackage => [
			name = "pack"
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
				]
			]
		]
		checker.checkDuplicateFeatures(p)
		assertEquals('''
			WARN: pack.C1.A1: pack.C1.A1, duplicate features: pack.C2.A1, pack.C3.A1
			WARN: pack.C2.A1: pack.C2.A1, duplicate features: pack.C1.A1, pack.C3.A1
			WARN: pack.C3.A1: pack.C3.A1, duplicate features: pack.C1.A1, pack.C2.A1
		'''.toString,
			appender.result
		)
	}

}
