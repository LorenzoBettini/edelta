package edelta.tests

import com.google.inject.Inject
import edelta.edelta.EdeltaFactory
import edelta.interpreter.EdeltaInterpreterHelper
import edelta.tests.additional.MyCustomEdelta
import org.eclipse.emf.ecore.EcoreFactory
import org.eclipse.emf.ecore.EcorePackage
import org.eclipse.xtext.common.types.util.JavaReflectAccess
import org.eclipse.xtext.testing.InjectWith
import org.eclipse.xtext.testing.XtextRunner
import org.junit.Test
import org.junit.runner.RunWith

import static org.assertj.core.api.Assertions.*
import static org.junit.Assert.*

@RunWith(XtextRunner)
@InjectWith(EdeltaInjectorProviderForJavaReflectAccess)
class EdeltaInterpreterHelperTest extends EdeltaAbstractTest {

	@Inject EdeltaInterpreterHelper interpreterHelper

	@Inject JavaReflectAccess javaReflectAccess;

	static class InstantiateExceptionClass {

		new() {
			throw new InstantiationException
		}

	}

	@Test
	def void testSafeInstantiateOfValidUseAs() {
		'''
			import edelta.tests.additional.MyCustomEdelta
			
			use MyCustomEdelta as my
		'''.parse.useAsClauses.head => [
			assertEquals(
				MyCustomEdelta,
				interpreterHelper.safeInstantiate(javaReflectAccess, it).class
			)
		]
	}

	@Test
	def void testSafeInstantiateOfUseAsWithoutType() {
		'''
			use as my
		'''.parse.useAsClauses.head => [
			assertEquals(
				Object,
				interpreterHelper.safeInstantiate(javaReflectAccess, it).class
			)
		]
	}

	@Test
	def void testSafeInstantiateOfValidUseAsWithoutType() {
		'''
			import edelta.tests.EdeltaInterpreterHelperTest.InstantiateExceptionClass
			use InstantiateExceptionClass as my
		'''.parse.useAsClauses.head => [
			assertEquals(
				Object,
				interpreterHelper.safeInstantiate(javaReflectAccess, it).class
			)
		]
	}

	@Test
	def void testSafeSetEAttributeType() {
		val attr = EcoreFactory.eINSTANCE.createEAttribute
		interpreterHelper.safeSetEAttributeType(attr,
			EdeltaFactory.eINSTANCE.createEdeltaEcoreDirectReference => [
				// something that is not an EClassifier
				enamedelement = EcoreFactory.eINSTANCE.createEReference
			]
		)
		assertNull(attr.EType)
		interpreterHelper.safeSetEAttributeType(attr,
			EdeltaFactory.eINSTANCE.createEdeltaEcoreDirectReference => [
				enamedelement = EcorePackage.eINSTANCE.EString
			]
		)
		assertEquals(EcorePackage.eINSTANCE.EString, attr.EType)
	}

	@Test
	def void testSafeSetEAttributeTypeWithNullType() {
		val attr = EcoreFactory.eINSTANCE.createEAttribute
		interpreterHelper.safeSetEAttributeType(attr, null)
		assertNull(attr.EType)
	}

	@Test
	def void testSafeInstantiateOfUnresolvedUseAsType() {
		assertThatThrownBy[
		'''
			use NonExistent as my
		'''.parse.useAsClauses.head => [
				interpreterHelper.safeInstantiate(javaReflectAccess, it).class
			]
		].isInstanceOf(IllegalStateException)
			.hasMessageContaining("Cannot resolve proxy")
	}

	@Test
	def void testSafeInstantiateOfValidUseAsButNotFoundAtRuntime() {
		// this is a simulation of what would happen if a type is resolved
		// but the interpreter cannot load it with Class.forName
		// because the ClassLoader cannot find it
		// https://github.com/LorenzoBettini/edelta/issues/69
		assertThatThrownBy[
		'''
			import edelta.tests..additional.MyCustomEdeltaThatCannotBeLoadedAtRuntime
			
			use MyCustomEdeltaThatCannotBeLoadedAtRuntime as my
		'''.parse.useAsClauses.head => [
				interpreterHelper.safeInstantiate(javaReflectAccess, it).class
			]
		].isInstanceOf(NullPointerException)
	}
}
