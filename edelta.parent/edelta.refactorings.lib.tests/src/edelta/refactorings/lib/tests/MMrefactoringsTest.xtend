package edelta.refactorings.lib.tests

import gssi.refactorings.MMrefactorings
import org.junit.Before
import org.junit.Test

import static org.assertj.core.api.Assertions.*
import org.eclipse.emf.ecore.EAttribute

class MMrefactoringsTest extends AbstractTest {
	var MMrefactorings refactorings

	@Before
	def void setup() {
		refactorings = new MMrefactorings
	}

	@Test
	def void test_addMandatoryAttr() {
		val c = createEClass("C1")
		refactorings.addMandatoryAttr("test", stringDataType, c)
		val attr = c.EStructuralFeatures.filter(EAttribute).head
		assertThat(attr)
			.returns("test", [name])
			.returns(stringDataType, [EAttributeType])
			.returns(true, [isRequired])
	}
}
