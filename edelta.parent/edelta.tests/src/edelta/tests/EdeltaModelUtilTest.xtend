package edelta.tests

import org.eclipse.emf.ecore.EcoreFactory
import org.eclipse.xtext.testing.InjectWith
import org.eclipse.xtext.testing.XtextRunner
import org.junit.Test
import org.junit.runner.RunWith

import static edelta.util.EdeltaModelUtil.*
import static org.assertj.core.api.Assertions.assertThat
import static org.junit.Assert.*

@RunWith(XtextRunner)
@InjectWith(EdeltaInjectorProviderCustom)
class EdeltaModelUtilTest extends EdeltaAbstractTest {

	@Test
	def void testGetProgram() {
		'''
			metamodel "foo"
			
			modifyEcore aTest epackage foo {}
		'''.parseWithTestEcore => [
			assertSame(it, getProgram(lastModifyEcoreOperation))
		]
	}

	@Test
	def void testHasCycleInSuperPackageWithNoCycle() {
		val ecoreFactory = EcoreFactory.eINSTANCE
		val ePackage = ecoreFactory.createEPackage() => [
			ESubpackages += ecoreFactory.createEPackage() => [
				ESubpackages += ecoreFactory.createEPackage()
			]
		]
		assertFalse(hasCycleInSuperPackage(
			ePackage.ESubpackages.head.ESubpackages.head
		))
	}

	@Test
	def void testHasCycleInSuperPackageWithCycle() {
		val ecoreFactory = EcoreFactory.eINSTANCE
		val ePackage = ecoreFactory.createEPackage() => [
			ESubpackages += ecoreFactory.createEPackage() => [
				ESubpackages += ecoreFactory.createEPackage()
			]
		]
		
		val subSubPackage = ePackage.ESubpackages.head.ESubpackages.head
		// force the cycle
		subSubPackage.ESubpackages += ePackage
		assertTrue(hasCycleInSuperPackage(
			subSubPackage
		))
	}

	@Test
	def void testFindRootSuperPackage() {
		val ecoreFactory = EcoreFactory.eINSTANCE
		val rootPackage = ecoreFactory.createEPackage() => [
			ESubpackages += ecoreFactory.createEPackage() => [
				ESubpackages += ecoreFactory.createEPackage()
			]
		]
		assertThat(findRootSuperPackage(rootPackage.ESubpackages.head.ESubpackages.head))
			.isSameAs(rootPackage)
		assertThat(findRootSuperPackage(rootPackage.ESubpackages.head))
			.isSameAs(rootPackage)
		assertThat(findRootSuperPackage(rootPackage))
			.isNull
	}

	@Test
	def void testGetEcoreReferenceText() {
		'''
			metamodel "foo"
			
			modifyEcore aTest epackage foo {
				ecoreref(FooClass)
				ecoreref(foo.FooClass)
				ecoreref(NonExistingClass)
				ecoreref()
			}
		'''
		.parseWithTestEcore
		.lastModifyEcoreOperation.body.block
		.expressions => [
			assertEquals("FooClass",
				getEcoreReferenceText(get(0).edeltaEcoreReference))
			assertEquals("foo.FooClass",
				getEcoreReferenceText(get(1).edeltaEcoreReference))
			assertEquals("NonExistingClass",
				getEcoreReferenceText(get(2).edeltaEcoreReference))
			assertEquals("",
				getEcoreReferenceText(get(3).edeltaEcoreReference))
		]
	}

	@Test
	def void testGetMetamodelImportText() {
		val input = '''
			metamodel "foo"
			metamodel "bar"
			metamodel "foo"
		'''
		input.parseWithTestEcore => [
			assertEquals('"foo"',
				getMetamodelImportText(it, 0))
			assertEquals('"bar"',
				getMetamodelImportText(it, 1))
			val node = getMetamodelImportNodes(it).get(1) // metamodel "bar"
			assertEquals(input.indexOf('"bar"'), node.offset)
			assertEquals('"bar"'.length, node.length)
			assertEquals(input.indexOf("metamodel", 2),
				node.previousSibling.previousSibling.offset) // the second metamodel
		]
	}

}
