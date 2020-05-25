package edelta.tests

import com.google.inject.Inject
import edelta.interpreter.EdeltaInterpreterDiagnosticHelper
import edelta.resource.derivedstate.EdeltaDerivedStateHelper
import org.eclipse.xtext.testing.InjectWith
import org.eclipse.xtext.testing.XtextRunner
import org.junit.Test
import org.junit.runner.RunWith

import static org.eclipse.xtext.xbase.XbasePackage.Literals.*

@RunWith(XtextRunner)
@InjectWith(EdeltaInjectorProviderCustom)
class EdeltaInterpreterDiagnosticHelperTest extends EdeltaAbstractTest {

	@Inject EdeltaInterpreterDiagnosticHelper diagnosticHelper

	@Inject EdeltaDerivedStateHelper derivedStateHelper

	@Test def void testAddErrorWithCurrentExpression() {
		val input = '''
		metamodel "foo"
		
		modifyEcore aTest epackage foo {
			val s = null
		}
		'''
		input.parseWithTestEcore => [
			val exp = lastModifyEcoreOperation.body.blockLastExpression
			diagnosticHelper.setCurrentExpression(exp)
			diagnosticHelper.addError(null, "issueCode", "an error")
			assertError(
				XVARIABLE_DECLARATION,
				"issueCode",
				input.lastIndexOf("val s = null"),
				"val s = null".length,
				"an error"
			)
		]
	}

	@Test def void testAddErrorWithDifferentCorrespondingExpression() {
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
			diagnosticHelper.setCurrentExpression(currentExpression)
			diagnosticHelper.addError(problematic, "issueCode", "an error")
			// the error is associated to the associated expression to problematic object
			// not with the current expression
			assertError(
				XVARIABLE_DECLARATION,
				"issueCode",
				input.lastIndexOf("val s = null"),
				"val s = null".length,
				"an error"
			)
		]
	}

	@Test def void testAddWarningWithCurrentExpression() {
		val input = '''
		metamodel "foo"
		
		modifyEcore aTest epackage foo {
			val s = null
		}
		'''
		input.parseWithTestEcore => [
			val exp = lastModifyEcoreOperation.body.blockLastExpression
			diagnosticHelper.setCurrentExpression(exp)
			diagnosticHelper.addWarning(null, "issueCode", "a warning")
			assertWarning(
				XVARIABLE_DECLARATION,
				"issueCode",
				input.lastIndexOf("val s = null"),
				"val s = null".length,
				"a warning"
			)
		]
	}

	@Test def void testAddWarningWithDifferentCorrespondingExpression() {
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
			diagnosticHelper.setCurrentExpression(currentExpression)
			diagnosticHelper.addWarning(problematic, "issueCode", "a warning")
			// the warning is associated to the associated expression to problematic object
			// not with the current expression
			assertWarning(
				XVARIABLE_DECLARATION,
				"issueCode",
				input.lastIndexOf("val s = null"),
				"val s = null".length,
				"a warning"
			)
		]
	}

}