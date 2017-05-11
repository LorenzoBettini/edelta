package edelta.tests

import edelta.resource.EdeltaDerivedStateEClass
import org.eclipse.xtext.testing.InjectWith
import org.eclipse.xtext.testing.XtextRunner
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(XtextRunner)
@InjectWith(EdeltaInjectorProviderCustom)
class EdeltaDerivedStateEClassTest extends EdeltaAbstractTest {

	@Test
	def void testSuperTypesWithReferenceToEClass() {
		val ref = referenceToEClass.parseWithTestEcore.
			lastExpression.edeltaEcoreReferenceExpression.reference
		val c = new EdeltaDerivedStateEClass(#[ref])
		assertNamedElements(c.ESuperTypes, "FooClass\n")
	}

	@Test
	def void testSuperTypesWithUnresolvedEcoreReference() {
		val ref = "ecoreref(Foo)".parseWithTestEcore.
			lastExpression.edeltaEcoreReferenceExpression.reference
		val c = new EdeltaDerivedStateEClass(#[ref])
		assertNamedElements(c.ESuperTypes, "\n")
	}

	@Test
	def void testAllFeaturesWithReferenceToEClass() {
		val ref = referenceToEClass.parseWithTestEcore.
			lastExpression.edeltaEcoreReferenceExpression.reference
		val c = new EdeltaDerivedStateEClass(#[ref])
		assertNamedElements(c.EAllStructuralFeatures,
			// these are inherited from the ecore reference FooClass
			'''
			myAttribute
			myReference
			'''
		)
	}

}
