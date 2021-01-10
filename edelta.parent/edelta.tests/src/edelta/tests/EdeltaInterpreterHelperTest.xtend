package edelta.tests

import com.google.inject.Inject
import edelta.interpreter.EdeltaInterpreterHelper
import edelta.interpreter.EdeltaInterpreterRuntimeException
import edelta.lib.AbstractEdelta
import edelta.tests.additional.MyCustomEdelta
import edelta.tests.additional.MyCustomEdeltaThatCannotBeLoadedAtRuntime
import org.eclipse.xtext.common.types.util.JavaReflectAccess
import org.eclipse.xtext.testing.InjectWith
import org.eclipse.xtext.testing.XtextRunner
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

import static org.assertj.core.api.Assertions.*
import static org.junit.Assert.*

@RunWith(XtextRunner)
@InjectWith(EdeltaInjectorProviderForJavaReflectAccess)
class EdeltaInterpreterHelperTest extends EdeltaAbstractTest {

	@Inject EdeltaInterpreterHelper interpreterHelper

	@Inject JavaReflectAccess javaReflectAccess;

	var AbstractEdelta other

	static class InstantiateExceptionClass {

		new() {
			throw new InstantiationException
		}

	}

	@Before
	def void setup() {
		other = new AbstractEdelta() {
			
		}
	}

	@Test
	def void testSafeInstantiateOfValidUseAs() throws Exception {
		'''
			import edelta.tests.additional.MyCustomEdelta
			
			use MyCustomEdelta as my
		'''.parse.useAsClauses.head => [
			assertEquals(
				MyCustomEdelta,
				interpreterHelper.safeInstantiate(javaReflectAccess, it, other).class
			)
		]
	}

	@Test
	def void testSafeInstantiateOfUseAsWithoutType() throws Exception {
		'''
			use as my
		'''.parse.useAsClauses.head => [
			assertThat(
				interpreterHelper.safeInstantiate(javaReflectAccess, it, other).class
			).isNotNull
		]
	}

	@Test
	def void testSafeInstantiateOfValidUseAsWithoutType() throws Exception {
		'''
			import edelta.tests.EdeltaInterpreterHelperTest.InstantiateExceptionClass
			use InstantiateExceptionClass as my
		'''.parse.useAsClauses.head => [
			assertThat(
				interpreterHelper.safeInstantiate(javaReflectAccess, it, other).class
			).isNotNull
		]
	}

	@Test
	def void testSafeInstantiateOfUnresolvedUseAsType() throws Exception {
		assertThatThrownBy[
		'''
			use NonExistent as my
		'''.parse.useAsClauses.head => [
				interpreterHelper.safeInstantiate(javaReflectAccess, it, other).class
			]
		].isInstanceOf(IllegalStateException)
			.hasMessageContaining("Cannot resolve proxy")
	}

	@Test
	def void testSafeInstantiateOfValidUseAsButNotFoundAtRuntime() throws Exception {
		// this is a simulation of what would happen if a type is resolved
		// but the interpreter cannot load it with Class.forName
		// because the ClassLoader cannot find it
		// https://github.com/LorenzoBettini/edelta/issues/69
		assertThatThrownBy[
		'''
			import edelta.tests.additional.MyCustomEdeltaThatCannotBeLoadedAtRuntime
			
			use MyCustomEdeltaThatCannotBeLoadedAtRuntime as my
		'''.parse.useAsClauses.head => [
				interpreterHelper.safeInstantiate(javaReflectAccess, it, other).class
			]
		].isInstanceOf(EdeltaInterpreterRuntimeException)
			.hasMessageContaining('''The type '«MyCustomEdeltaThatCannotBeLoadedAtRuntime.name»' has been resolved but cannot be loaded by the interpreter''')
	}

	@Test
	def void testFilterOperationsWithNullEPackage() throws Exception {
		'''
		modifyEcore first epackage {}
		modifyEcore second epackage foo {}
		'''.parse => [
			assertThat(interpreterHelper.filterOperations(modifyEcoreOperations))
				.containsExactly(modifyEcoreOperations.last)
		]
	}

	@Test
	def void testFilterOperationsWithSubPackage() throws Exception {
		'''
		metamodel "mainpackage.mainsubpackage"

		modifyEcore aTest epackage mainsubpackage {
			
		}
		'''.parseWithTestEcoreWithSubPackage => [
			assertThat(interpreterHelper.filterOperations(modifyEcoreOperations))
				.isEmpty
		]
	}
}
