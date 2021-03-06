package edelta.tests

import edelta.tests.injectors.EdeltaInjectorProviderCustom
import org.eclipse.emf.ecore.EcoreFactory
import org.eclipse.xtext.testing.InjectWith
import org.eclipse.xtext.testing.XtextRunner
import org.eclipse.xtext.xbase.XIfExpression
import org.junit.Test
import org.junit.runner.RunWith

import static edelta.util.EdeltaModelUtil.*
import static org.assertj.core.api.Assertions.assertThat
import static org.junit.Assert.*

@RunWith(XtextRunner)
@InjectWith(EdeltaInjectorProviderCustom)
class EdeltaModelUtilTest extends EdeltaAbstractTest {

	@Test
	def void testGetProgram() throws Exception {
		'''
			metamodel "foo"
			
			modifyEcore aTest epackage foo {}
		'''.parseWithTestEcore => [
			assertSame(it, getProgram(lastModifyEcoreOperation))
		]
	}

	@Test
	def void testHasCycleInSuperPackageWithNoCycle() throws Exception {
		val ePackage = createEPackage("p") [
			ESubpackages += createEPackage("p2") [
				ESubpackages += createEPackage("p3")
			]
		]
		assertFalse(hasCycleInSuperPackage(
			ePackage.ESubpackages.head.ESubpackages.head
		))
	}

	@Test
	def void testHasCycleInSuperPackageWithCycle() throws Exception {
		val ePackage = createEPackage("p") [
			ESubpackages += createEPackage("p2") [
				ESubpackages += createEPackage("p3")
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
	def void testFindRootSuperPackage() throws Exception {
		val rootPackage = createEPackage("p") [
			ESubpackages += createEPackage("p2") [
				ESubpackages += createEPackage("p3")
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
	def void testGetEcoreReferenceText() throws Exception {
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
		.lastModifyEcoreOperationBlock
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
	def void testGetMetamodelImportText() throws Exception {
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

	@Test
	def void testHasCycleInHierarchy() throws Exception {
		val ecoreFactory = EcoreFactory.eINSTANCE
		val c1 = ecoreFactory.createEClass
		assertThat(hasCycleInHierarchy(c1)).isFalse
		val c2 = ecoreFactory.createEClass
		c2.ESuperTypes += c1
		assertThat(hasCycleInHierarchy(c2)).isFalse
		val c3 = ecoreFactory.createEClass
		c3.ESuperTypes += c2
		assertThat(hasCycleInHierarchy(c3)).isFalse
		val c4 = ecoreFactory.createEClass
		c3.ESuperTypes += c4
		assertThat(hasCycleInHierarchy(c3)).isFalse
		// cycle
		c1.ESuperTypes += c3
		assertThat(hasCycleInHierarchy(c4)).isFalse
		assertThat(hasCycleInHierarchy(c3)).isTrue
		assertThat(hasCycleInHierarchy(c2)).isTrue
		assertThat(hasCycleInHierarchy(c1)).isTrue
	}

	@Test
	def void testGetContainingBlockXExpression() throws Exception {
		val input = '''
			metamodel "foo"
			
			modifyEcore aTest epackage foo {
				ecoreref(FooClass) // 0
				ecoreref(FooClass).abstract = true // 1
				ecoreref(FooClass).ESuperTypes += null // 2
				if (true) {
					ecoreref(FooClass).ESuperTypes += null // 3
				}
			}
		'''
		input.parseWithTestEcore => [
			val mainBlock = lastModifyEcoreOperationBlock
			val ecoreRefs = allEcoreReferenceExpressions.map[reference]
			var ecoreRef = ecoreRefs.get(0)
			assertThat(getContainingBlockXExpression(ecoreRef))
				.isSameAs(ecoreRef.eContainer)
			ecoreRef = ecoreRefs.get(1)
			assertThat(getContainingBlockXExpression(ecoreRef))
				.isSameAs(mainBlock.expressions.get(1))
			ecoreRef = ecoreRefs.get(2)
			assertThat(getContainingBlockXExpression(ecoreRef))
				.isSameAs(mainBlock.expressions.get(2))
			ecoreRef = ecoreRefs.get(3)
			assertThat(getContainingBlockXExpression(ecoreRef))
				.isSameAs(
					(mainBlock.expressions.get(3) as XIfExpression)
						.then.blockFirstExpression
				)
		]
	}
}
