package edelta.tests

import com.google.inject.Inject
import edelta.util.EdeltaModelUtil
import org.eclipse.xtext.testing.InjectWith
import org.eclipse.xtext.testing.XtextRunner
import org.junit.Test
import org.junit.runner.RunWith

import static org.junit.Assert.*

@RunWith(XtextRunner)
@InjectWith(EdeltaInjectorProviderCustom)
class EdeltaModelUtilTest extends EdeltaAbstractTest {

	@Inject extension EdeltaModelUtil

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
