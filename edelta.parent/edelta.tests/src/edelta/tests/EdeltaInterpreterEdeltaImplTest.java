package edelta.tests;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.Collections;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EcoreFactory;
import org.junit.Before;
import org.junit.Test;

import edelta.interpreter.EdeltaInterpreterDiagnosticHelper;
import edelta.interpreter.EdeltaInterpreterEdeltaImpl;
import edelta.validation.EdeltaValidator;

public class EdeltaInterpreterEdeltaImplTest extends EdeltaAbstractTest {
	private EdeltaInterpreterEdeltaImpl edelta;

	private EdeltaInterpreterDiagnosticHelper diagnosticHelper;

	@Before
	public void setup() {
		diagnosticHelper = mock(EdeltaInterpreterDiagnosticHelper.class);
		edelta = new EdeltaInterpreterEdeltaImpl(
				Collections.emptyList(),
				diagnosticHelper);
	}

	@Test
	public void testFirstEPackageHasPrecedence() {
		EPackage p1 = EcoreFactory.eINSTANCE.createEPackage();
		p1.setName("Test");
		EPackage p2 = EcoreFactory.eINSTANCE.createEPackage();
		p2.setName("Test");
		edelta = new EdeltaInterpreterEdeltaImpl(
				asList(p1, p2),
				diagnosticHelper);
		assertSame(p1, edelta.getEPackage("Test"));
	}

	@Test
	public void testShowError() {
		final EClass element = EcoreFactory.eINSTANCE.createEClass();
		edelta.showError(element, "a message");
		verify(diagnosticHelper).addError(element,
				EdeltaValidator.LIVE_VALIDATION_ERROR, "a message");
	}

	@Test
	public void testShowWarning() {
		final EClass element = EcoreFactory.eINSTANCE.createEClass();
		edelta.showWarning(element, "a message");
		verify(diagnosticHelper).addWarning(element,
				EdeltaValidator.LIVE_VALIDATION_WARNING, "a message");
	}
}
