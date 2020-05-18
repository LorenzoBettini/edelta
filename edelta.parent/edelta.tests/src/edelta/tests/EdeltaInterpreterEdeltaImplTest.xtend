package edelta.tests

import static extension org.junit.Assert.*
import org.junit.Test
import org.eclipse.emf.ecore.EcoreFactory
import edelta.interpreter.EdeltaInterpreterEdeltaImpl
import org.junit.Before

class EdeltaInterpreterEdeltaImplTest {
	var EdeltaInterpreterEdeltaImpl edelta

	@Before
	def void setup() {
		edelta = new EdeltaInterpreterEdeltaImpl(#[])
	}

	@Test def void testFirstEPackageHasPrecedence() {
		val p1 = EcoreFactory.eINSTANCE.createEPackage => [name = "Test"]
		val p2 = EcoreFactory.eINSTANCE.createEPackage => [name = "Test"]
		edelta = new EdeltaInterpreterEdeltaImpl(#[p1, p2])
		assertSame(p1, edelta.getEPackage("Test"))
	}
}