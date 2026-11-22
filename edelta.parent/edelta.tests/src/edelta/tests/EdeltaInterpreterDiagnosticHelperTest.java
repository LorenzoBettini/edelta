package edelta.tests;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.nio.file.Path;

import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.impl.ResourceImpl;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.XtextRunner;
import org.eclipse.xtext.xbase.XbasePackage;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.inject.Inject;

import edelta.interpreter.EdeltaInterpreterDiagnosticHelper;
import edelta.edelta.EdeltaFactory;
import edelta.resource.derivedstate.EdeltaModelPreview;
import edelta.validation.EdeltaValidator;

@RunWith(XtextRunner.class)
@InjectWith(EdeltaInjectorProvider.class)
public class EdeltaInterpreterDiagnosticHelperTest extends EdeltaAbstractTest {
	@Inject
	private EdeltaInterpreterDiagnosticHelper diagnosticHelper;

	@Test
	public void nullExpressionUsesTheProgramWhenAvailable() throws Exception {
		var program = parseWithTestEcore("metamodel \"foo\" modifyEcore test epackage foo {}");
		diagnosticHelper.setFallbackSource(program);
		diagnosticHelper.setCurrentExpression(null);
		diagnosticHelper.addError(null, "issueCode", "an error");
		validationTestHelper.assertError(program,
			edelta.edelta.EdeltaPackage.Literals.EDELTA_PROGRAM,
			"issueCode", 0,
			"metamodel \"foo\" modifyEcore test epackage foo {}".length(), "an error");
	}

	@Test
	public void nullExpressionWithoutProgramDoesNotCrash() {
		diagnosticHelper.setFallbackSource(null);
		diagnosticHelper.setCurrentExpression(null);
		assertThatCode(() -> diagnosticHelper.addWarning(null, "issueCode", "a warning"))
			.doesNotThrowAnyException();
	}

	@Test
	public void detachedFallbackDoesNotCrash() {
		diagnosticHelper.setFallbackSource(EdeltaFactory.eINSTANCE.createEdeltaProgram());
		assertThatCode(() -> diagnosticHelper.addError(null, "issueCode", "an error"))
			.doesNotThrowAnyException();
	}

	@Test
	public void foreignExpressionCannotReplaceTheUserProgram() throws Exception {
		var program = parseWithTestEcore("metamodel \"foo\" modifyEcore test epackage foo {}");
		var other = parseWithTestEcore("metamodel \"foo\" modifyEcore other epackage foo { 1 }");
		diagnosticHelper.setFallbackSource(program);
		diagnosticHelper.setCurrentExpression(getLastModifyEcoreOperationLastExpression(other));
		diagnosticHelper.addError(null, "issueCode", "an error");
		validationTestHelper.assertError(program,
			edelta.edelta.EdeltaPackage.Literals.EDELTA_PROGRAM,
			"issueCode", 0,
			"metamodel \"foo\" modifyEcore test epackage foo {}".length(), "an error");
	}

	@Test
	public void previewFailureWithForeignSourceFallsBackToActiveProgram() throws Exception {
		var program = parseWithTestEcore("metamodel \"foo\" modifyEcore test epackage foo {}");
		var other = parseWithTestEcore("metamodel \"foo\" modifyEcore other epackage foo { 1 }");
		diagnosticHelper.addPreviewFailure(program, new EdeltaModelPreview.Failure(
			"MIGRATION", getLastModifyEcoreOperationLastExpression(other),
			Path.of("model.xmi"), new NullPointerException()));
		var issues = validationTestHelper.validate(program).stream()
			.filter(issue -> EdeltaValidator.MODEL_PREVIEW_FAILURE.equals(issue.getCode()))
			.toList();
		assertThat(issues).hasSize(1);
		assertThat(issues.getFirst().getOffset()).isZero();
		assertThat(issues.getFirst().getMessage()).contains("model.xmi", "NullPointerException");
	}

	@Test
	public void previewFailureWithoutSourceOrCauseUsesProgram() throws Exception {
		var program = parseWithTestEcore("metamodel \"foo\" modifyEcore test epackage foo {}");
		diagnosticHelper.addPreviewFailure(program,
			new EdeltaModelPreview.Failure("INTERPRETATION", null, null, null));
		var issues = validationTestHelper.validate(program).stream()
			.filter(issue -> EdeltaValidator.MODEL_PREVIEW_FAILURE.equals(issue.getCode()))
			.toList();
		assertThat(issues).hasSize(1);
		assertThat(issues.getFirst().getOffset()).isZero();
		assertThat(issues.getFirst().getMessage()).contains("selected models");
	}

	@Test
	public void previewIssueCleanupPreservesOtherResourceDiagnostics() throws Exception {
		var program = parseWithTestEcore("metamodel \"foo\" modifyEcore test epackage foo {}");
		var resource = new ResourceImpl();
		var unrelated = mock(Resource.Diagnostic.class);
		resource.getErrors().add(unrelated);
		resource.getErrors().add(new org.eclipse.xtext.validation.EObjectDiagnosticImpl(
			org.eclipse.xtext.diagnostics.Severity.ERROR,
			EdeltaValidator.MODEL_PREVIEW_INPUT_FAILURE, "input", program, null, -1,
			new String[] {}));
		resource.getErrors().add(new org.eclipse.xtext.validation.EObjectDiagnosticImpl(
			org.eclipse.xtext.diagnostics.Severity.ERROR,
			EdeltaValidator.MODEL_PREVIEW_FAILURE, "execution", program, null, -1,
			new String[] {}));
		var unrelatedObjectIssue = new org.eclipse.xtext.validation.EObjectDiagnosticImpl(
			org.eclipse.xtext.diagnostics.Severity.ERROR,
			"otherCode", "other", program, null, -1, new String[] {});
		resource.getErrors().add(unrelatedObjectIssue);
		diagnosticHelper.clearPreviewDiagnostics(resource);
		assertThat(resource.getErrors()).containsExactly(unrelated, unrelatedObjectIssue);
	}

	@Test
	public void testAddErrorWithCurrentExpression() throws Exception {
		var input =
			"""
			metamodel "foo"
			
			modifyEcore aTest epackage foo {
				val s = null
			}
			""";
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
			"""
			metamodel "foo"
			
			modifyEcore aTest epackage foo {
				var s = null
				s = null
			}
			""";
		var prog = parseWithTestEcore(input);
		// just an ENamedElement to pass to show method as
		// the problematic object
		var problematic = getLastCopiedEPackage(prog);
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
			"""
			metamodel "foo"
			
			modifyEcore aTest epackage foo {
				val s = null
			}
			""";
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
			"""
			metamodel "foo"
			
			modifyEcore aTest epackage foo {
				var s = null
				s = null
			}
			""";
		var prog = parseWithTestEcore(input);
		// just an ENamedElement to pass to show method as
		// the problematic object
		var problematic = getLastCopiedEPackage(prog);
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
