package edelta.tests

import com.google.inject.Inject
import edelta.edelta.EdeltaFactory
import org.eclipse.emf.common.util.BasicDiagnostic
import org.eclipse.emf.common.util.Diagnostic
import org.eclipse.xtext.testing.InjectWith
import org.eclipse.xtext.testing.XtextRunner
import org.eclipse.xtext.validation.IDiagnosticConverter
import org.junit.Test
import org.junit.runner.RunWith

import static org.junit.Assert.*

@RunWith(XtextRunner)
@InjectWith(EdeltaInjectorProvider)
class EdeltaDiagnosticConverterTest extends EdeltaAbstractTest {

	@Inject IDiagnosticConverter converter

	@Test
	def void testEcoreReferenceDiagnosticIsDiscarded() {
		val diagnostic = new BasicDiagnostic(Diagnostic.ERROR, null, 0, null,
			#[EdeltaFactory.eINSTANCE.createEdeltaEcoreDirectReference])
		converter.convertValidatorDiagnostic(
			diagnostic,
			[ issue |
				fail("unwanted issue: " + issue)
			]
		)
	}

	@Test
	def void testOtherDiagnosticAreNotDiscarded() {
		val diagnostic = new BasicDiagnostic(Diagnostic.ERROR, null, 0, null,
			#[EdeltaFactory.eINSTANCE.createEdeltaProgram])
		val issues = newLinkedList()
		converter.convertValidatorDiagnostic(
			diagnostic,
			[ issue |
				issues += issue
			]
		)
		assertEquals(1, issues.size)
	}

}
