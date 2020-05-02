package edelta.tests

import com.google.inject.Inject
import edelta.edelta.EdeltaEcoreReference
import edelta.edelta.EdeltaEcoreReferenceInformation
import edelta.edelta.EdeltaProgram
import edelta.util.EdeltaEcoreReferenceInformationHelper
import org.eclipse.emf.ecore.EAttribute
import org.eclipse.xtext.testing.InjectWith
import org.eclipse.xtext.testing.XtextRunner
import org.junit.Test
import org.junit.runner.RunWith

import static org.assertj.core.api.Assertions.assertThat
import static org.mockito.Mockito.mock
import static org.mockito.Mockito.when
import org.eclipse.emf.ecore.EPackage

@RunWith(XtextRunner)
@InjectWith(EdeltaInjectorProviderDerivedStateComputerWithoutInterpreter)
class EdeltaEcoreReferenceInformationHelperTest extends EdeltaAbstractTest {

	@Inject EdeltaEcoreReferenceInformationHelper informationHelper

	@Test
	def void testWhenAlreadySetThenReturnsTheStoredInformation() {
		val ref = mock(EdeltaEcoreReference)
		val info = mock(EdeltaEcoreReferenceInformation)
		when(ref.information).thenReturn(info)
		assertThat(informationHelper.getOrComputeInformation(ref))
			.isSameAs(info)
	}

	@Test
	def void testReferenceToEPackage() {
		'''
		metamodel "foo"
		
		modifyEcore aTest epackage foo {
			ecoreref(foo)
		}
		'''.parseWithTestEcore.lastEcoreRef => [
			val info = informationHelper.getOrComputeInformation(it)
			assertThat(info)
				.returns("EPackage", [type])
				.returns("foo", [EPackageName])
				.returns(null, [EClassifierName])
				.returns(null, [ENamedElementName])
		]
	}

	@Test
	def void testReferenceToSubPackage() {
		'''
		metamodel "mainpackage"
		
		modifyEcore aTest epackage mainpackage {
			ecoreref(mainsubpackage)
		}
		'''.parseWithTestEcoreWithSubPackage.lastEcoreRef => [
			val info = informationHelper.getOrComputeInformation(it)
			assertThat(info)
				.returns("EPackage", [type])
				.returns("mainpackage.mainsubpackage", [EPackageName])
				.returns(null, [EClassifierName])
				.returns(null, [ENamedElementName])
		]
	}

	@Test
	def void testReferenceToEStructuralFeatureWithSubPackage() {
		'''
		metamodel "mainpackage"
		
		modifyEcore aTest epackage mainpackage {
			ecoreref(mySubPackageAttribute)
		}
		'''.parseWithTestEcoreWithSubPackage.lastEcoreRef => [
			val info = informationHelper.getOrComputeInformation(it)
			assertThat(info)
				.returns("EAttribute", [type])
				.returns("mainpackage.mainsubpackage", [EPackageName])
				.returns("MainSubPackageFooClass", [EClassifierName])
				.returns("mySubPackageAttribute", [ENamedElementName])
		]
	}

	@Test
	def void testReferenceToSubPackageWithCycle() {
		'''
		metamodel "mainpackage"
		
		modifyEcore aTest epackage mainpackage {
			ecoreref(subsubpackage)
		}
		'''.parseWithTestEcoreWithSubPackage.lastEcoreRef => [
			// create cycle in sub package relation
			val subpackage = enamedelement as EPackage
			subpackage.ESubpackages += subpackage.ESuperPackage
			val info = informationHelper.getOrComputeInformation(it)
			assertThat(info)
				.returns("EPackage", [type])
				// due to the cycle the parent relation is not visited
				.returns("subsubpackage", [EPackageName])
				.returns(null, [EClassifierName])
				.returns(null, [ENamedElementName])
		]
	}

	@Test
	def void testReferenceToEClassifier() {
		'''
		metamodel "foo"
		
		modifyEcore aTest epackage foo {
			ecoreref(FooClass)
		}
		'''.parseWithTestEcore.lastEcoreRef => [
			val info = informationHelper.getOrComputeInformation(it)
			assertThat(info)
				.returns("EClass", [type])
				.returns("foo", [EPackageName])
				.returns("FooClass", [EClassifierName])
				.returns(null, [ENamedElementName])
		]
	}

	@Test
	def void testReferenceToEEnumLiteral() {
		'''
		metamodel "foo"
		
		modifyEcore aTest epackage foo {
			ecoreref(FooEnumLiteral)
		}
		'''.parseWithTestEcore.lastEcoreRef => [
			val info = informationHelper.getOrComputeInformation(it)
			assertThat(info)
				.returns("EEnumLiteral", [type])
				.returns("foo", [EPackageName])
				.returns("FooEnum", [EClassifierName])
				.returns("FooEnumLiteral", [ENamedElementName])
		]
	}

	@Test
	def void testReferenceToEStructuralFeature() {
		'''
		metamodel "foo"
		
		modifyEcore aTest epackage foo {
			ecoreref(myReference)
		}
		'''.parseWithTestEcore.lastEcoreRef => [
			val info = informationHelper.getOrComputeInformation(it)
			assertThat(info)
				.returns("EReference", [type])
				.returns("foo", [EPackageName])
				.returns("FooClass", [EClassifierName])
				.returns("myReference", [ENamedElementName])
		]
	}

	@Test
	def void testReferenceToUnresolved() {
		'''
		metamodel "foo"
		
		modifyEcore aTest epackage foo {
			ecoreref(unknown)
		}
		'''.parseWithTestEcore.lastEcoreRef => [
			val info = informationHelper.getOrComputeInformation(it)
			assertThat(info)
				.returns("ENamedElement", [type])
				.returns("", [EPackageName])
				.returns("", [EClassifierName])
				.returns("", [ENamedElementName])
		]
	}

	@Test
	def void testAfterChange() {
		'''
		metamodel "foo"
		
		modifyEcore aTest epackage foo {
			ecoreref(myAttribute)
		}
		'''.parseWithTestEcore.lastEcoreRef => [
			var info = informationHelper.getOrComputeInformation(it)
			assertThat(info)
				.returns("EAttribute", [type])
				.returns("foo", [EPackageName])
				.returns("FooClass", [EClassifierName])
				.returns("myAttribute", [ENamedElementName])
			// change the underlying model
			val attr = it.enamedelement as EAttribute
			attr.name = "renamed"
			attr.EContainingClass.name = "Renamed"
			// but the information stored hasn't changed
			info = informationHelper.getOrComputeInformation(it)
			assertThat(info)
				.returns("EAttribute", [type])
				.returns("foo", [EPackageName])
				.returns("FooClass", [EClassifierName])
				.returns("myAttribute", [ENamedElementName])
		]
	}

	def private lastEcoreRef(EdeltaProgram p) {
		p.lastEcoreReferenceExpression
			.reference
	}
} 