package edelta.tests

import com.google.inject.Inject
import org.eclipse.emf.ecore.EcoreFactory
import org.eclipse.xtext.naming.IQualifiedNameProvider
import org.eclipse.xtext.testing.InjectWith
import org.eclipse.xtext.testing.XtextRunner
import org.junit.Test
import org.junit.runner.RunWith

import static extension org.junit.Assert.*

@RunWith(XtextRunner)
@InjectWith(EdeltaInjectorProvider)
class EdeltaQualifiedNameProviderTest extends EdeltaAbstractTest {

	@Inject extension IQualifiedNameProvider

	@Test
	def void testProgramWithoutPackage() {
		"edelta.__synthetic0".assertEquals(''''''.parse.fullyQualifiedName.toString)
	}

	@Test
	def void testProgramWithPackage() {
		"foo.__synthetic0".assertEquals('''package foo'''.parse.fullyQualifiedName.toString)
	}

	@Test
	def void testEPackageWithCycle() {
		val p1 = EcoreFactory.eINSTANCE.createEPackage => [ name = "p1" ]
		val p2 = EcoreFactory.eINSTANCE.createEPackage => [ name = "p2" ]
		p1.ESubpackages += p2
		assertEquals("p1.p2", p2.fullyQualifiedName.toString)
		p2.ESubpackages += p1
		assertEquals("p2", p2.fullyQualifiedName.toString)
	}

	@Test
	def void testAnyOtherElement() {
		val c = EcoreFactory.eINSTANCE.createEClass => [
			name = "foo"
		]
		"foo".assertEquals(
			c.fullyQualifiedName.toString
		)
	}

}
