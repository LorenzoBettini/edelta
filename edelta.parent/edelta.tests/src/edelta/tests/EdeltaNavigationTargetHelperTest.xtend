package edelta.tests

import com.google.inject.Inject
import edelta.navigation.EdeltaNavigationTargetHelper
import org.eclipse.emf.ecore.EcoreFactory
import org.eclipse.xtext.testing.InjectWith
import org.eclipse.xtext.testing.XtextRunner
import org.junit.Test
import org.junit.runner.RunWith

import static org.assertj.core.api.Assertions.*

@RunWith(XtextRunner)
@InjectWith(EdeltaInjectorProviderCustom)
class EdeltaNavigationTargetHelperTest extends EdeltaAbstractTest {

	@Inject EdeltaNavigationTargetHelper navigationTargetHelper

	@Test
	def void testNotEdeltaEcoreReference() {
		assertThat(navigationTargetHelper.getTarget(EcoreFactory.eINSTANCE.createEClass))
	}

	@Test
	def void testTargetInTheImportedMetamodel() {
		'''
		metamodel "foo"
		
		modifyEcore aTest epackage foo {
			ecoreref(FooClass)
		}
		'''.parseWithTestEcore => [
			val target =
				navigationTargetHelper.getTarget(allEcoreReferenceExpressions.head.reference)
			val original = metamodels.head.getEClassiferByName("FooClass")
			assertThat(target).isNotNull
			assertThat(target).isSameAs(original)
		]
	}

	@Test
	def void testTargetAsXExpression() {
		'''
		metamodel "foo"
		
		modifyEcore aTest epackage foo {
			addNewEClass("NewClass")
			ecoreref(NewClass)
		}
		'''.parseWithTestEcore => [
			val target =
				navigationTargetHelper.getTarget(allEcoreReferenceExpressions.head.reference)
			val exp = lastModifyEcoreOperation.body.block.expressions.head
			assertThat(target).isNotNull
			assertThat(target).isSameAs(exp)
		]
	}

	@Test
	def void testTargetOfForwardReference() {
		'''
		metamodel "foo"
		
		modifyEcore aTest epackage foo {
			ecoreref(NewClass)
			addNewEClass("NewClass")
		}
		'''.parseWithTestEcore => [
			val target =
				navigationTargetHelper.getTarget(allEcoreReferenceExpressions.head.reference)
			val exp = lastModifyEcoreOperation.body.block.expressions.last
			assertThat(target).isNotNull
			assertThat(target).isSameAs(exp)
		]
	}
}
