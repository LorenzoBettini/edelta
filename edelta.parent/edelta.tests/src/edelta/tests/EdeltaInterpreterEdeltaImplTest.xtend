package edelta.tests

import edelta.interpreter.EdeltaInterpreterDiagnosticHelper
import edelta.interpreter.EdeltaInterpreterEdeltaImpl
import edelta.validation.EdeltaValidator
import org.eclipse.emf.ecore.EcoreFactory
import org.junit.Before
import org.junit.Test

import static org.junit.Assert.*
import static org.mockito.Mockito.*

class EdeltaInterpreterEdeltaImplTest extends EdeltaAbstractTest {
	var EdeltaInterpreterEdeltaImpl edelta

	var EdeltaInterpreterDiagnosticHelper diagnosticHelper

	@Before
	def void setup() {
		diagnosticHelper = mock(EdeltaInterpreterDiagnosticHelper)
		edelta = new EdeltaInterpreterEdeltaImpl(#[], diagnosticHelper)
	}

	@Test def void testFirstEPackageHasPrecedence() {
		val p1 = EcoreFactory.eINSTANCE.createEPackage => [name = "Test"]
		val p2 = EcoreFactory.eINSTANCE.createEPackage => [name = "Test"]
		edelta = new EdeltaInterpreterEdeltaImpl(#[p1, p2], diagnosticHelper)
		assertSame(p1, edelta.getEPackage("Test"))
	}

	@Test def void testShowError() {
		val element = EcoreFactory.eINSTANCE.createEClass
		edelta.showError(element, "a message")
		verify(diagnosticHelper)
			.addError(element, EdeltaValidator.LIVE_VALIDATION_ERROR, "a message")
	}

	@Test def void testShowWarning() {
		val element = EcoreFactory.eINSTANCE.createEClass
		edelta.showWarning(element, "a message")
		verify(diagnosticHelper)
			.addWarning(element, EdeltaValidator.LIVE_VALIDATION_WARNING, "a message")
	}

}