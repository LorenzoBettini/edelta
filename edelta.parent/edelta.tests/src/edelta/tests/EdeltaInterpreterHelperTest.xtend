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
import org.eclipse.xtext.common.types.access.impl.ClassFinder
import edelta.lib.AbstractEdelta
import edelta.EdeltaRuntimeModule

@RunWith(XtextRunner)
@InjectWith(EdeltaInjectorProviderForJavaReflectAccess)
class EdeltaInterpreterHelperTest extends EdeltaAbstractTest {

	static class EdeltaInjectorProviderForJavaReflectAccess extends EdeltaInjectorProvider {
		override protected EdeltaRuntimeModule createRuntimeModule() {
			return new EdeltaRuntimeModule() {
				override bindClassLoaderToInstance() {
					return EdeltaInjectorProvider.getClassLoader();
				}
	
				def Class<? extends JavaReflectAccess> bindJavaReflectAccess() {
					MockJavaReflectAccess
				}
			}
		}
	}

	/**
	 * Fake implementation that, when loaded at runtime by the interpreter, should
	 * not be loaded.
	 */
	static class MyCustomEdeltaThatCannotBeLoadedAtRuntime extends AbstractEdelta {
		
	}

	/**
	 * Fake implementation that, when we try to load MyCustomEdeltaThatCannotBeLoadedAtRuntime
	 * with forName it returns null.
	 */
	static class MockJavaReflectAccess extends JavaReflectAccess {
		var ClassLoader classLoader

		@Inject
		override setClassLoader(ClassLoader classLoader) {
			super.setClassLoader(classLoader)
			this.classLoader = classLoader
		}

		override getClassFinder() {
			return new ClassFinder(classLoader) {
				override forName(String name) throws ClassNotFoundException {
					if (name == MyCustomEdeltaThatCannotBeLoadedAtRuntime.name)
						return null
					return super.forName(name)
				}
			}
		}
	}

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
			import edelta.tests.EdeltaInterpreterHelperTest$MyCustomEdeltaThatCannotBeLoadedAtRuntime
			
			use MyCustomEdeltaThatCannotBeLoadedAtRuntime as my
		'''.parse.useAsClauses.head => [
				interpreterHelper.safeInstantiate(javaReflectAccess, it).class
			]
		].isInstanceOf(NullPointerException)
	}
}
