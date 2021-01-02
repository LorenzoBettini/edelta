package edelta.tests;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EcoreFactory;
import org.junit.Before;
import org.junit.Test;

import edelta.interpreter.EdeltaInterpreterDiagnosticHelper;
import edelta.interpreter.EdeltaInterpreterIssuePresenter;
import edelta.validation.EdeltaValidator;

public class EdeltaInterpreterIssuePresenterTest extends EdeltaAbstractTest {
	private EdeltaInterpreterIssuePresenter issuePresenter;

	private EdeltaInterpreterDiagnosticHelper diagnosticHelper;

	@Before
	public void setup() {
		diagnosticHelper = mock(EdeltaInterpreterDiagnosticHelper.class);
		issuePresenter = new EdeltaInterpreterIssuePresenter(diagnosticHelper);
	}

	@Test
	public void testShowError() {
		final EClass element = EcoreFactory.eINSTANCE.createEClass();
		issuePresenter.showError(element, "a message");
		verify(diagnosticHelper).addError(element,
				EdeltaValidator.LIVE_VALIDATION_ERROR, "a message");
	}

	@Test
	public void testShowWarning() {
		final EClass element = EcoreFactory.eINSTANCE.createEClass();
		issuePresenter.showWarning(element, "a message");
		verify(diagnosticHelper).addWarning(element,
				EdeltaValidator.LIVE_VALIDATION_WARNING, "a message");
	}
}
