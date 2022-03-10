package edelta.tests;

import static org.eclipse.xtext.xbase.lib.IterableExtensions.last;

import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.XtextRunner;
import org.eclipse.xtext.xbase.XbasePackage;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.inject.Inject;

import edelta.interpreter.EdeltaInterpreterDiagnosticHelper;
import edelta.tests.injectors.EdeltaInjectorProviderCustom;

@RunWith(XtextRunner.class)
@InjectWith(EdeltaInjectorProviderCustom.class)
public class EdeltaInterpreterDiagnosticHelperTest extends EdeltaAbstractTest {
	@Inject
	private EdeltaInterpreterDiagnosticHelper diagnosticHelper;

	@Test
	public void testAddErrorWithCurrentExpression() throws Exception {
		var input =
			"metamodel \"foo\"\n"
			+ "\n"
			+ "modifyEcore aTest epackage foo {\n"
			+ "	val s = null\n"
			+ "}";
		var prog = parseWithTestEcore(input);
		var exp = getLastModifyEcoreOperationLastExpression(prog);
		diagnosticHelper.setCurrentExpression(exp);
		diagnosticHelper.addError(null, "issueCode", "an error");
		validationTestHelper.assertError(
			prog,
			XbasePackage.Literals.XVARIABLE_DECLARATION,
			"issueCode",
			input.lastIndexOf("val s = null"),
			"val s = null".length(),
			"an error"
		);
	}

	@Test
	public void testAddErrorWithDifferentCorrespondingExpression() throws Exception {
		var input =
			"metamodel \"foo\"\n"
			+ "\n"
			+ "modifyEcore aTest epackage foo {\n"
			+ "	var s = null\n"
			+ "	s = null\n"
			+ "}";
		var prog = parseWithTestEcore(input);
		// just an ENamedElement to pass to show method as
		// the problematic object
		var problematic = last(getCopiedEPackages(prog));
		// associate it to the first expression in the derived state
		var correspondingExpression = getLastModifyEcoreOperationFirstExpression(prog);
		derivedStateHelper.getEnamedElementXExpressionMap(
				prog.eResource())
				.put(problematic, correspondingExpression);
		var currentExpression = getLastModifyEcoreOperationLastExpression(prog);
		diagnosticHelper.setCurrentExpression(currentExpression);
		diagnosticHelper.addError(problematic, "issueCode", "an error");
		// the error is associated to the associated expression to problematic object
		// not with the current expression
		validationTestHelper.assertError(
			prog,
			XbasePackage.Literals.XVARIABLE_DECLARATION,
			"issueCode",
			input.lastIndexOf("val s = null"),
			"val s = null".length(),
			"an error"
		);
	}

	@Test
	public void testAddWarningWithCurrentExpression() throws Exception {
		var input =
			"metamodel \"foo\"\n"
			+ "\n"
			+ "modifyEcore aTest epackage foo {\n"
			+ "	val s = null\n"
			+ "}";
		var prog = parseWithTestEcore(input);
		var exp = getLastModifyEcoreOperationLastExpression(prog);
		diagnosticHelper.setCurrentExpression(exp);
		diagnosticHelper.addWarning(null, "issueCode", "a warning");
		validationTestHelper.assertWarning(
			prog,
			XbasePackage.Literals.XVARIABLE_DECLARATION,
			"issueCode",
			input.lastIndexOf("val s = null"),
			"val s = null".length(),
			"a warning"
		);
	}

	@Test
	public void testAddWarningWithDifferentCorrespondingExpression() throws Exception {
		var input =
			"metamodel \"foo\"\n"
			+ "\n"
			+ "modifyEcore aTest epackage foo {\n"
			+ "	var s = null\n"
			+ "	s = null\n"
			+ "}";
		var prog = parseWithTestEcore(input);
		// just an ENamedElement to pass to show method as
		// the problematic object
		var problematic = last(getCopiedEPackages(prog));
		// associate it to the first expression in the derived state
		var correspondingExpression = 
				getLastModifyEcoreOperationFirstExpression(prog);
		derivedStateHelper.getEnamedElementXExpressionMap(
				prog.eResource())
				.put(problematic,
						correspondingExpression);
		var currentExpression = getLastModifyEcoreOperationLastExpression(prog);
		diagnosticHelper.setCurrentExpression(currentExpression);
		diagnosticHelper.addWarning(problematic, "issueCode", "a warning");
		// the warning is associated to the associated expression to problematic object
		// not with the current expression
		validationTestHelper.assertWarning(
			prog,
			XbasePackage.Literals.XVARIABLE_DECLARATION,
			"issueCode",
			input.lastIndexOf("val s = null"),
			"val s = null".length(),
			"a warning"
		);
	}
}
