package edelta.tests;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.nio.file.Path;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.UndeclaredThrowableException;

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
		assertThat(issues.getFirst().getMessage())
			.isEqualTo("Model preview interpretation failed for selected models.");
	}

	@Test
	public void previewFailurePublishesExactCauseAndStackLayout() throws Exception {
		var program = parseWithTestEcore("metamodel \"foo\" modifyEcore test epackage foo {}");
		var input = new NumberFormatException("invalid age");
		input.setStackTrace(new StackTraceElement[] {
			new StackTraceElement("org.eclipse.xtext.Runner", "invoke", "Runner.java", 7),
			new StackTraceElement("example.AgeConverter", "convert", "/tmp/build/AgeConverter.java", 12),
			new StackTraceElement("java.lang.reflect.Method", "invoke", "Method.java", 9),
			new StackTraceElement("edelta.lib.EdeltaModelMigrator", "copyRule", "EdeltaModelMigrator.java", 34),
			new StackTraceElement("example.Preview", "run", "Preview.java", 56),
			new StackTraceElement("example.App", "main", "App.java", 78)
		});
		var wrapper = new InvocationTargetException(input);
		wrapper.setStackTrace(new StackTraceElement[0]);
		var failure = new IllegalStateException("cannot migrate age", wrapper);
		failure.setStackTrace(new StackTraceElement[0]);
		diagnosticHelper.addPreviewFailure(program, new EdeltaModelPreview.Failure(
			"MIGRATION", program, Path.of("models/MyClass.xmi"), failure, "MyClass at /"));
		var issues = validationTestHelper.validate(program).stream()
			.filter(issue -> EdeltaValidator.MODEL_PREVIEW_FAILURE.equals(issue.getCode()))
			.toList();
		assertThat(issues).hasSize(1);
		assertThat(issues.getFirst().getMessage()).isEqualTo("""
			Model preview migration failed for 'models/MyClass.xmi' (resource root MyClass at /): IllegalStateException: cannot migrate age
			Caused by: NumberFormatException: invalid age
			Relevant stack:
			  at example.AgeConverter.convert(AgeConverter.java:12)
			  at edelta.lib.EdeltaModelMigrator.copyRule(EdeltaModelMigrator.java:34)
			  at example.Preview.run(Preview.java:56)
			  ... further application frames omitted""");
	}

	@Test
	public void previewFailureWithFrameworkOnlyStackHasNoStackExcerpt() throws Exception {
		var program = parseWithTestEcore("metamodel \"foo\" modifyEcore test epackage foo {}");
		var cause = new IllegalArgumentException("bad input");
		cause.setStackTrace(new StackTraceElement[] {
			new StackTraceElement("org.eclipse.emf.Loader", "load", "Loader.java", 5)
		});
		diagnosticHelper.addPreviewFailure(program, new EdeltaModelPreview.Failure(
			"LOAD_FAILED", program, Path.of("models\\bad.xmi"), cause));
		var issue = validationTestHelper.validate(program).stream()
			.filter(it -> EdeltaValidator.MODEL_PREVIEW_FAILURE.equals(it.getCode()))
			.findFirst().orElseThrow();
		assertThat(issue.getMessage()).isEqualTo(
			"Cannot load model preview input 'models/bad.xmi': IllegalArgumentException: bad input");
	}

	@Test
	public void nestedCausesAreBoundedAndEmptyStacksStaySilent() throws Exception {
		var program = parseWithTestEcore("metamodel \"foo\" modifyEcore test epackage foo {}");
		var deepest = new IllegalArgumentException("bad value");
		deepest.setStackTrace(new StackTraceElement[0]);
		var third = new IllegalStateException("third", deepest);
		third.setStackTrace(new StackTraceElement[0]);
		var second = new RuntimeException("second", third);
		second.setStackTrace(new StackTraceElement[0]);
		var first = new RuntimeException("first", second);
		first.setStackTrace(new StackTraceElement[0]);
		diagnosticHelper.addPreviewFailure(program, new EdeltaModelPreview.Failure(
			"MIGRATION", program, Path.of("models/a.xmi"), first));
		var issue = validationTestHelper.validate(program).stream()
			.filter(it -> EdeltaValidator.MODEL_PREVIEW_FAILURE.equals(it.getCode()))
			.findFirst().orElseThrow();
		assertThat(issue.getMessage()).isEqualTo("""
			Model preview migration failed for 'models/a.xmi': RuntimeException: first
			Caused by: RuntimeException: second
			Caused by: IllegalStateException: third
			  ... further causes omitted""");
	}

	@Test
	public void missingInputWithoutCauseRemainsConcise() throws Exception {
		var program = parseWithTestEcore("metamodel \"foo\" modifyEcore test epackage foo {}");
		diagnosticHelper.addPreviewFailure(program, new EdeltaModelPreview.Failure(
			"MISSING", program, Path.of("models/missing.xmi"), null));
		var issue = validationTestHelper.validate(program).stream()
			.filter(it -> EdeltaValidator.MODEL_PREVIEW_FAILURE.equals(it.getCode()))
			.findFirst().orElseThrow();
		assertThat(issue.getMessage())
			.isEqualTo("Model preview input 'models/missing.xmi' does not exist.");
	}

	@Test
	public void reflectionWrapperAndOneUsefulOuterFrame() throws Exception {
		var program = parseWithTestEcore("metamodel \"foo\" modifyEcore test epackage foo {}");
		var inner = new IllegalStateException(" ");
		inner.setStackTrace(new StackTraceElement[0]);
		var wrapper = new UndeclaredThrowableException(inner);
		wrapper.setStackTrace(new StackTraceElement[0]);
		var outer = new RuntimeException("outer", wrapper);
		outer.setStackTrace(new StackTraceElement[] {
			new StackTraceElement("example.Work", "run", "Work.java", 3)
		});
		diagnosticHelper.addPreviewFailure(program, new EdeltaModelPreview.Failure(
			"MIGRATION", program, Path.of("models/a.xmi"), outer));
		var issue = validationTestHelper.validate(program).stream()
			.filter(it -> EdeltaValidator.MODEL_PREVIEW_FAILURE.equals(it.getCode()))
			.findFirst().orElseThrow();
		assertThat(issue.getMessage()).isEqualTo("""
			Model preview migration failed for 'models/a.xmi': RuntimeException: outer
			Caused by: IllegalStateException
			Relevant stack:
			  at example.Work.run(Work.java:3)""");
	}

	@Test
	public void longMultilineCauseAndNonApplicationFramesAreBounded() throws Exception {
		var program = parseWithTestEcore("metamodel \"foo\" modifyEcore test epackage foo {}");
		var cause = new IllegalArgumentException("a".repeat(121) + "\nmore");
		cause.setStackTrace(new StackTraceElement[] {
			new StackTraceElement("example.Unknown", "run", null, -1),
			new StackTraceElement("example.Native", "run", "Native.java", -2),
			new StackTraceElement("jdk.internal.Runner", "run", "Runner.java", 1),
			new StackTraceElement("sun.reflect.Runner", "run", "Runner.java", 1),
			new StackTraceElement("com.google.inject.Runner", "run", "Runner.java", 1),
			new StackTraceElement("edelta.interpreter.Runner", "run", "Runner.java", 1),
			new StackTraceElement("example.Work", "run", "Work.java", 8)
		});
		diagnosticHelper.addPreviewFailure(program, new EdeltaModelPreview.Failure(
			"SETUP", program, null, cause));
		var issue = validationTestHelper.validate(program).stream()
			.filter(it -> EdeltaValidator.MODEL_PREVIEW_FAILURE.equals(it.getCode()))
			.findFirst().orElseThrow();
		assertThat(issue.getMessage()).isEqualTo("Cannot initialize model preview: " +
			"IllegalArgumentException: " + "a".repeat(119) + "…\nRelevant stack:\n" +
			"  at example.Work.run(Work.java:8)");
	}

	@Test
	public void cyclicCauseDoesNotRepeatForever() throws Exception {
		var program = parseWithTestEcore("metamodel \"foo\" modifyEcore test epackage foo {}");
		var cause = new IllegalStateException("cycle") {
			private static final long serialVersionUID = 1L;
			@Override
			public synchronized Throwable getCause() {
				return this;
			}
		};
		cause.setStackTrace(new StackTraceElement[0]);
		diagnosticHelper.addPreviewFailure(program, new EdeltaModelPreview.Failure(
			"MIGRATION", program, null, cause));
		var issue = validationTestHelper.validate(program).stream()
			.filter(it -> EdeltaValidator.MODEL_PREVIEW_FAILURE.equals(it.getCode()))
			.findFirst().orElseThrow();
		assertThat(issue.getMessage()).isEqualTo(
			"Model preview migration failed for selected models: : cycle");
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
