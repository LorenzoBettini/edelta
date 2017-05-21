package edelta.tests

import com.google.inject.Inject
import edelta.edelta.EdeltaFactory
import edelta.interpreter.EdeltaInterpreterHelper
import edelta.resource.EdeltaDerivedStateComputer
import edelta.tests.additional.MyCustomEdelta
import org.eclipse.emf.ecore.EcoreFactory
import org.eclipse.emf.ecore.EcorePackage
import org.eclipse.xtext.common.types.util.JavaReflectAccess
import org.eclipse.xtext.testing.InjectWith
import org.eclipse.xtext.testing.XtextRunner
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

import static org.junit.Assert.*
import edelta.interpreter.EdeltaInterpreter
import edelta.edelta.EdeltaEcoreBaseEClassManipulationWithBlockExpression
import org.eclipse.emf.ecore.EClass
import org.eclipse.xtext.common.types.JvmGenericType

@RunWith(XtextRunner)
@InjectWith(EdeltaInjectorProviderCustom)
class EdeltaInterpreterHelperTest extends EdeltaAbstractTest {

	@Inject EdeltaInterpreterHelper interpreterHelper

	@Inject JavaReflectAccess javaReflectAccess;

	@Inject EdeltaDerivedStateComputer derivedStateComputer

	static class InstantiateExceptionClass {

		new() {
			throw new InstantiationException
		}

	}

	@Before
	def void disableInterpreterInDerivedStateComputer() {
		// derived state computer is a singleton so we can
		// safely set its interpreter globally
		derivedStateComputer.interpreter = new EdeltaInterpreter() {

			override run(EdeltaEcoreBaseEClassManipulationWithBlockExpression e, EClass c, JvmGenericType javaType) {
				// avoid the derived state computer run the interpreter
				// since the tests in this class must concern interpreter only
				// and we don't want side effects from the derived state computer
				// running the interpreter
			}

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
}
