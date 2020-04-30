package edelta.tests

import com.google.inject.Inject
import edelta.util.EdeltaEcoreHelper
import org.eclipse.xtext.testing.InjectWith
import org.eclipse.xtext.testing.XtextRunner
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert

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
	def void testProgramENamedElementsWithSubPackages() {
		referenceToMetamodelWithSubPackage.parseWithTestEcoreWithSubPackage.
			getProgramENamedElements.
			assertNamedElements(
				'''
				MainFooClass
				MainFooDataType
				MainFooEnum
				myAttribute
				myReference
				FooEnumLiteral
				mainsubpackage
				MainSubPackageFooClass
				mySubPackageAttribute
				mySubPackageReference
				mainpackage
				'''
			)
	}

	@Test
	def void testProgramWithCreatedEClassENamedElements() {
		referenceToCreatedEClass.parseWithTestEcore => [
			getProgramENamedElements.
			assertNamedElements(
				'''
				FooClass
				FooDataType
				FooEnum
				NewClass
				myAttribute
				myReference
				FooEnumLiteral
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
				FooClass
				FooDataType
				FooEnum
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
	def void testEPackageENamedElementsWithCreatedEClassWithoutCopiedEPackages() {
		referenceToCreatedEClass.parseWithTestEcore => [
			getENamedElementsWithoutCopiedEPackages(getEPackageByName("foo"), it).
			assertNamedElements(
				'''
				FooClass
				FooDataType
				FooEnum
				'''
			)
			// NewClass is the one created in the program
			// but it's in the copied EPackages, not used here
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

	@Test
	def void testENumENamedElementsWithCreatedEClass() {
		referenceToCreatedEClass.parseWithTestEcore => [
			getENamedElements(getEClassifierByName("foo", "FooEnum"), it).
			assertNamedElements(
				'''
				FooEnumLiteral
				FooEnumLiteral
				'''
			)
			// we also have copied EPackages, that's why the elements appear twice
		]
	}

	@Test
	def void testENumENamedElementsWithCreatedEClassWithoutCopiedEPackages() {
		referenceToCreatedEClass.parseWithTestEcore => [
			getENamedElementsWithoutCopiedEPackages(getEClassifierByName("foo", "FooEnum"), it).
			assertNamedElements(
				'''
				FooEnumLiteral
				'''
			)
			// we also have copied EPackages, that's why the elements appear twice
		]
	}

	@Test
	def void testNullENamedElements() {
		referenceToMetamodel.parseWithTestEcore => [
			Assert.assertTrue(getENamedElements(null, it).isEmpty)
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
	def void testEClassENamedElementsWithCreatedEClass() {
		referenceToCreatedEClass.parseWithTestEcore => [
			getENamedElements(getEClassifierByName("foo", "FooClass"), it).
			assertNamedElements(
				'''
				myAttribute
				myReference
				myAttribute
				myReference
				'''
			)
			// we also have copied EPackages, that's why the elements appear twice
		]
	}

	@Test
	def void testEClassENamedElementsWithCreatedEClassWithoutCopiedEPackages() {
		referenceToCreatedEClass.parseWithTestEcore => [
			getENamedElementsWithoutCopiedEPackages(getEClassifierByName("foo", "FooClass"), it).
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
