package edelta.tests

import com.google.inject.Inject
import edelta.edelta.EdeltaFactory
import edelta.scoping.EdeltaOriginalENamedElementRecorder
import org.eclipse.xtext.testing.InjectWith
import org.eclipse.xtext.testing.XtextRunner
import org.junit.Test
import org.junit.runner.RunWith

import static org.junit.Assert.*

@RunWith(XtextRunner)
@InjectWith(EdeltaInjectorProviderCustom)
class EdeltaOriginalENamedElementRecorderTest extends EdeltaAbstractTest {

	@Inject extension EdeltaOriginalENamedElementRecorder

	@Test def void testNullENamedElement() {
		'''
			metamodel "foo"
			
			createEClass NewClass in foo {}
			ecoreref(FooClass)
		'''.parseWithTestEcore => [
			val ref = EdeltaFactory.eINSTANCE.createEdeltaEcoreDirectReference
			ref.recordOriginalENamedElement
			assertNull(ref.originalEnamedelement)
		]
	}

	@Test def void testUnresolvedENamedElement() {
		'''
			metamodel "foo"
			
			createEClass NewClass in foo {}
			ecoreref(NonExistant)
		'''.parseWithTestEcore => [
			val ref = lastExpression.edeltaEcoreReferenceExpression.reference
			ref.recordOriginalENamedElement
			assertNull(ref.originalEnamedelement)
		]
	}

	@Test def void testEClassifierDirectReference() {
		'''
			metamodel "foo"
			
			createEClass NewClass in foo {}
			ecoreref(FooClass)
		'''.parseWithTestEcore => [
			val ref = lastExpression.edeltaEcoreReferenceExpression.reference
			ref.recordOriginalENamedElement
			val original = metamodels.last.getEClassiferByName("FooClass")
			assertSame(original, ref.originalEnamedelement)
		]
	}

	@Test def void testEClassifierQualifiedReference() {
		'''
			metamodel "foo"
			
			createEClass NewClass in foo {}
			ecoreref(foo.FooClass)
		'''.parseWithTestEcore => [
			val ref = lastExpression.
				edeltaEcoreReferenceExpression.reference.
				getEdeltaEcoreQualifiedReference
			ref.recordOriginalENamedElement
			val original = metamodels.last.getEClassiferByName("FooClass")
			assertSame(original, ref.originalEnamedelement)
			val originalPackage = metamodels.last
			assertSame(originalPackage, ref.qualification.originalEnamedelement)
		]
	}

	@Test def void testCreatedEClassifierDirectReference() {
		'''
			metamodel "foo"
			
			createEClass NewClass in foo {}
			ecoreref(NewClass)
		'''.parseWithTestEcore => [
			val ref = lastExpression.edeltaEcoreReferenceExpression.reference
			ref.recordOriginalENamedElement
			val original = derivedStateLastEClass
			assertSame(original, ref.originalEnamedelement)
		]
	}

	@Test def void testCreatedEClassifierQualifiedReference() {
		'''
			metamodel "foo"
			
			createEClass NewClass in foo {}
			ecoreref(foo.NewClass)
		'''.parseWithTestEcore => [
			val ref = lastExpression.
				edeltaEcoreReferenceExpression.reference.
				getEdeltaEcoreQualifiedReference
			ref.recordOriginalENamedElement
			val original = derivedStateLastEClass
			assertSame(original, ref.originalEnamedelement)
			// note that the package actually links to the original EPackage
			// not to the derived EPackage, but that's not a problem
			val originalPackage = metamodels.last
			assertSame(originalPackage, ref.qualification.originalEnamedelement)
		]
	}

	@Test def void testEStrucutralFeatureDirectReference() {
		'''
			metamodel "foo"
			
			createEClass NewClass in foo {}
			ecoreref(myAttribute)
		'''.parseWithTestEcore => [
			val ref = lastExpression.edeltaEcoreReferenceExpression.reference
			ref.recordOriginalENamedElement
			val original = metamodels.last.
				getEClassiferByName("FooClass").
				getEStructuralFeatureByName("myAttribute")
			assertSame(original, ref.originalEnamedelement)
		]
	}

	@Test def void testEStrucutralFeatureQualifiedReference() {
		'''
			metamodel "foo"
			
			createEClass NewClass in foo {}
			ecoreref(FooClass.myAttribute)
		'''.parseWithTestEcore => [
			val ref = lastExpression.
				edeltaEcoreReferenceExpression.reference.
				getEdeltaEcoreQualifiedReference
			ref.recordOriginalENamedElement
			val originalEClass = metamodels.last.getEClassiferByName("FooClass")
			assertSame(originalEClass, ref.qualification.originalEnamedelement)
			val original = originalEClass.
				getEStructuralFeatureByName("myAttribute")
			assertSame(original, ref.originalEnamedelement)
		]
	}

	@Test def void testEStrucutralFeatureFullyQualifiedReference() {
		'''
			metamodel "foo"
			
			createEClass NewClass in foo {}
			ecoreref(foo.FooClass.myAttribute)
		'''.parseWithTestEcore => [
			val ref = lastExpression.
				edeltaEcoreReferenceExpression.reference.
				getEdeltaEcoreQualifiedReference
			ref.recordOriginalENamedElement
			val originalPackage = metamodels.last
			assertSame(originalPackage,
				ref.qualification.
					edeltaEcoreQualifiedReference.qualification.originalEnamedelement
			)
			val originalEClass = originalPackage.getEClassiferByName("FooClass")
			assertSame(originalEClass, ref.qualification.originalEnamedelement)
			val original = originalEClass.
				getEStructuralFeatureByName("myAttribute")
			assertSame(original, ref.originalEnamedelement)
		]
	}
}
