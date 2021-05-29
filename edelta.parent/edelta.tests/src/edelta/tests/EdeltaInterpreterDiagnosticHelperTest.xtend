package edelta.tests

import com.google.inject.Inject
import edelta.interpreter.EdeltaInterpreterDiagnosticHelper
import edelta.tests.injectors.EdeltaInjectorProviderCustom
import org.eclipse.xtext.testing.InjectWith
import org.eclipse.xtext.testing.XtextRunner
import org.junit.Test
import org.junit.runner.RunWith

import static org.eclipse.xtext.xbase.XbasePackage.Literals.*

@RunWith(XtextRunner)
@InjectWith(EdeltaInjectorProviderCustom)
class EdeltaInterpreterDiagnosticHelperTest extends EdeltaAbstractTest {

	@Inject EdeltaInterpreterDiagnosticHelper diagnosticHelper

	@Test def void testAddErrorWithCurrentExpression() throws Exception {
		val input = '''
		metamodel "foo"
		
		modifyEcore aTest epackage foo {
			val s = null
		}
		'''
		input.parseWithTestEcore => [
			val exp = lastModifyEcoreOperationLastExpression
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

	@Test def void testAddErrorWithDifferentCorrespondingExpression() throws Exception {
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
			val correspondingExpression = lastModifyEcoreOperationFirstExpression
			derivedStateHelper.getEnamedElementXExpressionMap(eResource)
				.put(problematic, correspondingExpression)
			val currentExpression = lastModifyEcoreOperationLastExpression
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

	@Test def void testAddWarningWithCurrentExpression() throws Exception {
		val input = '''
		metamodel "foo"
		
		modifyEcore aTest epackage foo {
			val s = null
		}
		'''
		input.parseWithTestEcore => [
			val exp = lastModifyEcoreOperationLastExpression
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

	@Test def void testAddWarningWithDifferentCorrespondingExpression() throws Exception {
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
			val correspondingExpression = lastModifyEcoreOperationFirstExpression
			derivedStateHelper.getEnamedElementXExpressionMap(eResource)
				.put(problematic, correspondingExpression)
			val currentExpression = lastModifyEcoreOperationLastExpression
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