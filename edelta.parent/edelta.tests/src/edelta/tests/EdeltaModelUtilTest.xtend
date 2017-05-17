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
			
			createEClass First in foo
		'''.parseWithTestEcore => [
			assertSame(it, getProgram(lastExpression))
		]
	}

	@Test
	def void testGetChangeEClass() {
		'''
			metamodel "foo"
			
			changeEClass foo.First
		'''.parseWithTestEcore => [
			assertSame(lastExpression, getChangeEClass(lastExpression))
		]
	}

	@Test
	def void testGetEClassManipulation() {
		'''
			metamodel "foo"
			
			changeEClass foo.First
		'''.parseWithTestEcore => [
			assertSame(lastExpression, getEClassManipulation(lastExpression))
		]
	}
}
