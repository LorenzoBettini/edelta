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
		recordOriginalENamedElement(null)
	}

	@Test def void testNullENamedElement() throws Exception {
		val ref = EdeltaFactory.eINSTANCE.createEdeltaEcoreDirectReference
		ref.recordOriginalENamedElement
		assertNull(ref.originalEnamedelement)
	}

	@Test def void testUnresolvedENamedElement() throws Exception {
		val ref = "ecoreref(NonExistant)".ecoreReferenceExpression.reference
		ref.recordOriginalENamedElement
		assertNull(ref.originalEnamedelement)
	}

	@Test def void testEClassifierDirectReference() throws Exception {
		var prog = '''
			metamodel "foo"
			
			modifyEcore aTest epackage foo {
				ecoreref(FooClass)
			}
		'''.parseWithTestEcore

		val ref = prog.lastEcoreReferenceExpression.reference
		ref.recordOriginalENamedElement
		val original = prog.metamodels.last.getEClassiferByName("FooClass")
		assertSame(original, ref.originalEnamedelement)
	}

	@Test def void testEClassifierQualifiedReference() throws Exception {
		var prog = '''
			metamodel "foo"
			
			modifyEcore aTest epackage foo {
				ecoreref(foo.FooClass)
			}
		'''.parseWithTestEcore

		val ref = prog.lastEcoreReferenceExpression.reference.getEdeltaEcoreQualifiedReference
		ref.recordOriginalENamedElement
		val original = prog.metamodels.last.getEClassiferByName("FooClass")
		assertSame(original, ref.originalEnamedelement)
		val originalPackage = prog.metamodels.last
		assertSame(originalPackage, ref.qualification.originalEnamedelement)
	}

	@Test def void testCreatedEClassifierDirectReference() throws Exception {
		var prog = '''
			metamodel "foo"
			
			modifyEcore aTest epackage foo {
				addNewEClass("NewClass")
				ecoreref(NewClass)
			}
		'''.parseWithTestEcore

		val ref = prog.lastEcoreReferenceExpression.reference
		ref.recordOriginalENamedElement
		assertNull(ref.originalEnamedelement)
	}

	@Test def void testCreatedEClassifierQualifiedReference() throws Exception {
		var prog = '''
			metamodel "foo"
			
			modifyEcore aTest epackage foo {
				addNewEClass("NewClass")
				ecoreref(foo.NewClass)
			}
		'''.parseWithTestEcore

		val ref = prog.lastEcoreReferenceExpression.reference.getEdeltaEcoreQualifiedReference
		ref.recordOriginalENamedElement
		assertNull(ref.originalEnamedelement)
		// note that the package actually links to the original EPackage
		// not to the derived EPackage, but that's not a problem
		val originalPackage = prog.metamodels.last
		assertSame(originalPackage, ref.qualification.originalEnamedelement)
	}

}
