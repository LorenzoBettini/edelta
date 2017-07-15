package edelta.tests

import static extension org.junit.Assert.*
import org.junit.Test
import org.eclipse.emf.ecore.EcoreFactory
import edelta.interpreter.EdeltaInterpterEdeltaImpl

class EdeltaInterpreterEdeltaImplTest {

	@Test def void testFirstEPackageHasPrecedence() {
		val p1 = EcoreFactory.eINSTANCE.createEPackage => [name = "Test"]
		val p2 = EcoreFactory.eINSTANCE.createEPackage => [name = "Test"]
		val e = new EdeltaInterpterEdeltaImpl(#[p1, p2])
		assertSame(p1, e.getEPackage("Test"))
	}
}