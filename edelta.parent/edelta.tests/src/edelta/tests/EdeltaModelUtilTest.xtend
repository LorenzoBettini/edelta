package edelta.tests

import org.eclipse.xtext.testing.InjectWith
import org.eclipse.xtext.testing.XtextRunner
import org.junit.Test
import org.junit.runner.RunWith

import static edelta.util.EdeltaModelUtil.*
import static org.junit.Assert.*

@RunWith(XtextRunner)
@InjectWith(EdeltaInjectorProviderCustom)
class EdeltaModelUtilTest extends EdeltaAbstractTest {

	@Test
	def void testGetProgram() {
		'''
			metamodel "foo"
			
			modifyEcore aTest epackage foo {}
		'''.parseWithTestEcore => [
			assertSame(it, getProgram(lastModifyEcoreOperation))
		]
	}

}
