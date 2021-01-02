package edelta.refactorings.lib.tests

import edelta.lib.AbstractEdelta
import edelta.refactorings.lib.EdeltaBadSmellsChecker
import org.junit.Before
import org.junit.Test

import static org.assertj.core.api.Assertions.*
import static org.junit.Assert.*
import edelta.refactorings.lib.tests.utils.InMemoryLoggerAppender
import static extension edelta.lib.EdeltaLibrary.*

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
		val p = createEPackage("p") [
			addNewEClass("C1") [
				addNewEAttribute("A1", stringDataType)
			]
			addNewEClass("C2") [
				addNewEAttribute("A1", intDataType)
			]
		]
		checker.checkDuplicateFeatures(p)
		assertThat(appender.result).isEmpty
	}

	@Test def void test_checkDuplicateFeatures_withDuplicates() {
		val p = createEPackage("pack") [
			addNewEClass("C1") [
				addNewEAttribute("A1", stringDataType)
			]
			addNewEClass("C2") [
				addNewEAttribute("A1", stringDataType)
			]
			addNewEClass("C3") [
				addNewEAttribute("A1", stringDataType)
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
