package edelta.tests

import com.google.inject.Inject
import edelta.interpreter.EdeltaInterpreterEdeltaImpl
import edelta.resource.derivedstate.EdeltaDerivedStateHelper
import edelta.validation.EdeltaValidator
import org.apache.log4j.Level
import org.apache.log4j.Logger
import org.eclipse.emf.ecore.EcoreFactory
import org.eclipse.xtext.testing.InjectWith
import org.eclipse.xtext.testing.XtextRunner
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

import static org.eclipse.xtext.xbase.XbasePackage.Literals.*
import static org.junit.Assert.*
import static org.mockito.Mockito.*

@RunWith(XtextRunner)
@InjectWith(EdeltaInjectorProviderCustom)
class EdeltaInterpreterEdeltaImplTest extends EdeltaAbstractTest {
	var EdeltaInterpreterEdeltaImpl edelta

	var Logger logger

	@Inject EdeltaDerivedStateHelper derivedStateHelper

	@Before
	def void setup() {
		edelta = new EdeltaInterpreterEdeltaImpl(#[], derivedStateHelper) {
			override getLogger() {
				logger = spy(super.getLogger)
			}
		}
	}

	@Test def void testFirstEPackageHasPrecedence() {
		val p1 = EcoreFactory.eINSTANCE.createEPackage => [name = "Test"]
		val p2 = EcoreFactory.eINSTANCE.createEPackage => [name = "Test"]
		edelta = new EdeltaInterpreterEdeltaImpl(#[p1, p2], derivedStateHelper)
		assertSame(p1, edelta.getEPackage("Test"))
	}

	@Test def void testShowErrorWithNullCurrentExpression() {
		edelta.showError(null, "an error")
		verify(logger).log(Level.ERROR, ": an error")
	}

	@Test def void testShowErrorWithCurrentExpression() {
		val input = '''
		metamodel "foo"
		
		modifyEcore aTest epackage foo {
			val s = null
		}
		'''
		input.parseWithTestEcore => [
			val exp = lastModifyEcoreOperation.body.blockLastExpression
			edelta.setCurrentExpression(exp)
			edelta.showError(null, "an error")
			assertError(
				XVARIABLE_DECLARATION,
				EdeltaValidator.LIVE_VALIDATION_ERROR,
				input.lastIndexOf("val s = null"),
				"val s = null".length,
				"an error"
			)
		]
	}

	@Test def void testShowErrorWithDifferentCorrespondingExpression() {
		val input = '''
		metamodel "foo"
		
		modifyEcore aTest epackage foo {
			var s = null
			s = null
		}
		'''
		input.parseWithTestEcore => [
			// just an ENamedElement to pass to show method as
			// the problematic object
			val problematic = copiedEPackages.last
			// associate it to the first expression in the derived state
			val correspondingExpression = lastModifyEcoreOperation.body.blockFirstExpression
			derivedStateHelper.getEnamedElementXExpressionMap(eResource)
				.put(problematic, correspondingExpression)
			val currentExpression = lastModifyEcoreOperation.body.blockLastExpression
			edelta.setCurrentExpression(currentExpression)
			edelta.showError(problematic, "an error")
			// the error is associated to the associated expression to problematic object
			// not with the current expression
			assertError(
				XVARIABLE_DECLARATION,
				EdeltaValidator.LIVE_VALIDATION_ERROR,
				input.lastIndexOf("val s = null"),
				"val s = null".length,
				"an error"
			)
		]
	}

	@Test def void testShowWarningWithNullCurrentExpression() {
		edelta.showWarning(null, "an error")
		verify(logger).log(Level.WARN, ": an error")
	}

	@Test def void testShowWarningWithCurrentExpression() {
		val input = '''
		metamodel "foo"
		
		modifyEcore aTest epackage foo {
			val s = null
		}
		'''
		input.parseWithTestEcore => [
			val exp = lastModifyEcoreOperation.body.blockLastExpression
			edelta.setCurrentExpression(exp)
			edelta.showWarning(null, "a warning")
			assertWarning(
				XVARIABLE_DECLARATION,
				EdeltaValidator.LIVE_VALIDATION_WARNING,
				input.lastIndexOf("val s = null"),
				"val s = null".length,
				"a warning"
			)
		]
	}

	@Test def void testShowWarningWithDifferentCorrespondingExpression() {
		val input = '''
		metamodel "foo"
		
		modifyEcore aTest epackage foo {
			var s = null
			s = null
		}
		'''
		input.parseWithTestEcore => [
			// just an ENamedElement to pass to show method as
			// the problematic object
			val problematic = copiedEPackages.last
			// associate it to the first expression in the derived state
			val correspondingExpression = lastModifyEcoreOperation.body.blockFirstExpression
			derivedStateHelper.getEnamedElementXExpressionMap(eResource)
				.put(problematic, correspondingExpression)
			val currentExpression = lastModifyEcoreOperation.body.blockLastExpression
			edelta.setCurrentExpression(currentExpression)
			edelta.showWarning(problematic, "a warning")
			// the warning is associated to the associated expression to problematic object
			// not with the current expression
			assertWarning(
				XVARIABLE_DECLARATION,
				EdeltaValidator.LIVE_VALIDATION_WARNING,
				input.lastIndexOf("val s = null"),
				"val s = null".length,
				"a warning"
			)
		]
	}

}