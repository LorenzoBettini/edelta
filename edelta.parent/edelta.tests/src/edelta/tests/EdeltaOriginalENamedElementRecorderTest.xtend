package edelta.tests

import com.google.inject.Inject
import edelta.edelta.EdeltaFactory
import edelta.scoping.EdeltaOriginalENamedElementRecorder
import edelta.tests.injectors.EdeltaInjectorProviderDerivedStateComputerWithoutInterpreter
import org.eclipse.xtext.testing.InjectWith
import org.eclipse.xtext.testing.XtextRunner
import org.junit.Test
import org.junit.runner.RunWith

import static org.junit.Assert.*

@RunWith(XtextRunner)
@InjectWith(EdeltaInjectorProviderDerivedStateComputerWithoutInterpreter)
class EdeltaOriginalENamedElementRecorderTest extends EdeltaAbstractTest {

	@Inject extension EdeltaOriginalENamedElementRecorder

	@Test def void testNull() throws Exception {
		'''
			metamodel "foo"
		'''.parseWithTestEcore => [
			recordOriginalENamedElement(null)
		]
	}

	@Test def void testNullENamedElement() throws Exception {
		'''
			metamodel "foo"
		'''.parseWithTestEcore => [
			val ref = EdeltaFactory.eINSTANCE.createEdeltaEcoreDirectReference
			ref.recordOriginalENamedElement
			assertNull(ref.originalEnamedelement)
		]
	}

	@Test def void testUnresolvedENamedElement() throws Exception {
		val ref = "ecoreref(NonExistant)".ecoreReferenceExpression.reference
		ref.recordOriginalENamedElement
		assertNull(ref.originalEnamedelement)
	}

	@Test def void testEClassifierDirectReference() throws Exception {
		'''
			metamodel "foo"
			
			modifyEcore aTest epackage foo {
				ecoreref(FooClass)
			}
		'''.parseWithTestEcore => [
			val ref = lastEcoreReferenceExpression.reference
			ref.recordOriginalENamedElement
			val original = metamodels.last.getEClassiferByName("FooClass")
			assertSame(original, ref.originalEnamedelement)
		]
	}

	@Test def void testSubPackageDirectReference() throws Exception {
		'''
			metamodel "mainpackage"
			
			modifyEcore aTest epackage mainpackage {
				ecoreref(mainsubpackage)
			}
		'''.parseWithTestEcoreWithSubPackage => [
			val ref = lastEcoreReferenceExpression.reference
			ref.recordOriginalENamedElement
			val original = metamodels.last.ESubpackages.head
			assertSame(original, ref.originalEnamedelement)
		]
	}

	@Test def void testSubPackageEClassDirectReference() throws Exception {
		'''
			metamodel "mainpackage"
			
			modifyEcore aTest epackage mainpackage {
				ecoreref(MainSubPackageFooClass)
			}
		'''.parseWithTestEcoreWithSubPackage => [
			val ref = lastEcoreReferenceExpression.reference
			ref.recordOriginalENamedElement
			val original = metamodels.last.ESubpackages.head
				.getEClassiferByName("MainSubPackageFooClass")
			assertNotNull(original)
			assertSame(original, ref.originalEnamedelement)
		]
	}

	@Test def void testSubSubPackageEClassQualifiedReference() throws Exception {
		'''
			metamodel "mainpackage"
			
			modifyEcore aTest epackage mainpackage {
				ecoreref(subsubpackage.MyClass)
			}
		'''.parseWithTestEcoreWithSubPackage => [
			val ref = lastEcoreReferenceExpression.reference
			ref.recordOriginalENamedElement
			val original = metamodels.last.ESubpackages.head
				.ESubpackages.head
				.getEClassiferByName("MyClass")
			assertNotNull(original)
			assertSame(original, ref.originalEnamedelement)
		]
	}

	@Test def void testEClassifierQualifiedReference() throws Exception {
		'''
			metamodel "foo"
			
			modifyEcore aTest epackage foo {
				ecoreref(foo.FooClass)
			}
		'''.parseWithTestEcore => [
			val ref = lastEcoreReferenceExpression.reference.
				getEdeltaEcoreQualifiedReference
			ref.recordOriginalENamedElement
			val original = metamodels.last.getEClassiferByName("FooClass")
			assertSame(original, ref.originalEnamedelement)
			val originalPackage = metamodels.last
			assertSame(originalPackage, ref.qualification.originalEnamedelement)
		]
	}

	@Test def void testCreatedEClassifierDirectReference() throws Exception {
		'''
			metamodel "foo"
			
			modifyEcore aTest epackage foo {
				addNewEClass("NewClass")
				ecoreref(NewClass)
			}
		'''.parseWithTestEcore => [
			val ref = lastEcoreReferenceExpression.reference
			ref.recordOriginalENamedElement
			assertNull(ref.originalEnamedelement)
		]
	}

	@Test def void testCreatedEClassifierQualifiedReference() throws Exception {
		'''
			metamodel "foo"
			
			modifyEcore aTest epackage foo {
				addNewEClass("NewClass")
				ecoreref(foo.NewClass)
			}
		'''.parseWithTestEcore => [
			val ref = lastEcoreReferenceExpression.reference.
				getEdeltaEcoreQualifiedReference
			ref.recordOriginalENamedElement
			assertNull(ref.originalEnamedelement)
			// note that the package actually links to the original EPackage
			// not to the derived EPackage, but that's not a problem
			val originalPackage = metamodels.last
			assertSame(originalPackage, ref.qualification.originalEnamedelement)
		]
	}

	@Test def void testEStrucutralFeatureDirectReference() throws Exception {
		'''
			metamodel "foo"
			
			modifyEcore aTest epackage foo {
				ecoreref(myAttribute)
			}
		'''.parseWithTestEcore => [
			val ref = lastEcoreReferenceExpression.reference
			ref.recordOriginalENamedElement
			val original = metamodels.last.
				getEClassiferByName("FooClass").
				getEStructuralFeatureByName("myAttribute")
			assertSame(original, ref.originalEnamedelement)
		]
	}

	@Test def void testEStrucutralFeatureQualifiedReference() throws Exception {
		'''
			metamodel "foo"
			
			modifyEcore aTest epackage foo {
				ecoreref(FooClass.myAttribute)
			}
		'''.parseWithTestEcore => [
			val ref = lastEcoreReferenceExpression.reference.
				getEdeltaEcoreQualifiedReference
			ref.recordOriginalENamedElement
			val originalEClass = metamodels.last.getEClassiferByName("FooClass")
			assertSame(originalEClass, ref.qualification.originalEnamedelement)
			val original = originalEClass.
				getEStructuralFeatureByName("myAttribute")
			assertSame(original, ref.originalEnamedelement)
		]
	}

	@Test def void testEStrucutralFeatureFullyQualifiedReference() throws Exception {
		'''
			metamodel "foo"
			
			modifyEcore aTest epackage foo {
				ecoreref(foo.FooClass.myAttribute)
			}
		'''.parseWithTestEcore => [
			val ref = lastEcoreReferenceExpression.reference.
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

	@Test def void testEEnumLiteraDirectReference() throws Exception {
		'''
			metamodel "foo"
			
			modifyEcore aTest epackage foo {
				ecoreref(FooEnumLiteral)
			}
		'''.parseWithTestEcore => [
			val ref = lastEcoreReferenceExpression.reference
			ref.recordOriginalENamedElement
			val original = metamodels.last.
				getEClassiferByName("FooEnum").
				getEEnumLiteralByName("FooEnumLiteral")
			assertSame(original, ref.originalEnamedelement)
		]
	}

}
