package edelta.tests

import com.google.inject.Inject
import edelta.util.EdeltaEcoreHelper
import org.eclipse.xtext.testing.InjectWith
import org.eclipse.xtext.testing.XtextRunner
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(XtextRunner)
@InjectWith(EdeltaInjectorProviderCustom)
class EdeltaEcoreHelperTest extends EdeltaAbstractTest {

	@Inject extension EdeltaEcoreHelper

	@Test
	def void testProgramENamedElements() {
		referencesToMetamodels.parseWithTestEcores.
			getProgramENamedElements.
			assertNamedElements(
				'''
				FooClass
				FooDataType
				FooEnum
				myAttribute
				myReference
				FooEnumLiteral
				BarClass
				BarDataType
				myAttribute
				myReference
				foo
				bar
				'''
			)
	}

	@Test
	def void testProgramWithCreatedEClassENamedElements() {
		referenceToCreatedEClass.parseWithTestEcore => [
			getProgramENamedElements.
			assertNamedElements(
				'''
				NewClass
				FooClass
				FooDataType
				FooEnum
				myAttribute
				myReference
				FooEnumLiteral
				foo
				'''
			)
		// NewClass is the one created in the program
		]
	}

	@Test
	def void testEPackageENamedElements() {
		referenceToMetamodel.parseWithTestEcore => [
			getENamedElements(getEPackageByName("foo"), it).
			assertNamedElements(
				'''
				FooClass
				FooDataType
				FooEnum
				'''
			)
		]
	}

	@Test
	def void testEPackageENamedElementsWithCreatedEClass() {
		referenceToCreatedEClass.parseWithTestEcore => [
			getENamedElements(getEPackageByName("foo"), it).
			assertNamedElements(
				'''
				NewClass
				FooClass
				FooDataType
				FooEnum
				'''
			)
		// NewClass is the one created in the program
		]
	}

	@Test
	def void testEDataTypeENamedElements() {
		referenceToMetamodel.parseWithTestEcore => [
			getENamedElements(getEClassifierByName("foo", "FooDataType"), it).
			assertNamedElements(
				'''

				'''
			)
		]
	}

	@Test
	def void testENumENamedElements() {
		referenceToMetamodel.parseWithTestEcore => [
			getENamedElements(getEClassifierByName("foo", "FooEnum"), it).
			assertNamedElements(
				'''
				FooEnumLiteral
				'''
			)
		]
	}

	@Test(expected=IllegalArgumentException)
	def void testNullENamedElements() {
		referenceToMetamodel.parseWithTestEcore => [
			getENamedElements(null, it)
		]
	}

	@Test
	def void testEClassENamedElements() {
		referenceToMetamodel.parseWithTestEcore => [
			getENamedElements(getEClassifierByName("foo", "FooClass"), it).
			assertNamedElements(
				'''
				myAttribute
				myReference
				'''
			)
		]
	}

	@Test
	def void testGetAllEClasses() {
		referenceToMetamodel.parseWithTestEcore => [
			getAllEClasses(getEPackageByName("foo")).
			assertNamedElements(
				'''
				FooClass
				'''
			)
		]
	}

}
