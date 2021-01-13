package edelta.tests;

import static org.junit.Assert.*;

import java.util.ArrayList;

import org.eclipse.emf.common.util.BasicDiagnostic;
import org.eclipse.emf.common.util.Diagnostic;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.XtextRunner;
import org.eclipse.xtext.validation.IDiagnosticConverter;
import org.eclipse.xtext.validation.Issue;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.inject.Inject;

import edelta.edelta.EdeltaFactory;

@RunWith(XtextRunner.class)
@InjectWith(EdeltaInjectorProvider.class)
public class EdeltaDiagnosticConverterTest {
	@Inject
	private IDiagnosticConverter converter;

	@Test
	public void testEcoreReferenceDiagnosticIsDiscarded() {
		var diagnostic = new BasicDiagnostic(Diagnostic.ERROR, null, 0, null,
				new Object[] {
					EdeltaFactory.eINSTANCE.createEdeltaEcoreDirectReference()
				});
		converter.convertValidatorDiagnostic(diagnostic,
			issue -> fail(("unwanted issue: " + issue)));
	}

	@Test
	public void testOtherDiagnosticAreNotDiscarded() {
		var prog = EdeltaFactory.eINSTANCE.createEdeltaProgram();
		var diagnostic = new BasicDiagnostic(Diagnostic.ERROR, null, 0, null,
				new Object[] { prog });
		var issues = new ArrayList<Issue>();
		converter.convertValidatorDiagnostic(diagnostic, issues::add);
		assertEquals(1, issues.size());
	}
}
