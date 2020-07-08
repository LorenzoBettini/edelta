package edelta.tests

import com.google.inject.Inject
import edelta.util.EdeltaEcoreHelper
import org.eclipse.xtext.testing.InjectWith
import org.eclipse.xtext.testing.XtextRunner
import org.junit.Assert
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
	def void testProgramENamedElementsWithCopiedEPackages() {
		// note that the order is different w.r.t. the previous test
		// since here we retrieve copied EPackage that are stored in a Map
		referencesToMetamodelsWithCopiedEPackages.parseWithTestEcores.
			getProgramENamedElements.
			assertNamedElements(
				'''
				BarClass
				BarDataType
				myAttribute
				myReference
				FooClass
				FooDataType
				FooEnum
				myAttribute
				myReference
				FooEnumLiteral
				bar
				foo
				'''
			)
	}

	@Test
	def void testProgramENamedElementsWithSubPackages() {
		// MyClass with myClassAttribute
		// is present in the package and in subpackages
		// so it appears several times
		referenceToMetamodelWithSubPackageWithCopiedEPackages
			.parseWithTestEcoreWithSubPackage.
			getProgramENamedElements.
			assertNamedElements(
				'''
				MainFooClass
				MainFooDataType
				MainFooEnum
				MyClass
				myAttribute
				myReference
				FooEnumLiteral
				myClassAttribute
				mainsubpackage
				MainSubPackageFooClass
				MyClass
				mySubPackageAttribute
				mySubPackageReference
				myClassAttribute
				subsubpackage
				MyClass
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
	def void testEPackageENamedElementsWithSubPackages() {
		referenceToMetamodelWithSubPackage.parseWithTestEcoreWithSubPackage => [
			getENamedElements(getEPackageByName("mainpackage"), it).
			assertNamedElements(
				'''
				MainFooClass
				MainFooDataType
				MainFooEnum
				MyClass
				mainsubpackage
				'''
			)
		]
	}

	@Test
	def void testEPackageENamedElementsWithNewSubPackages() {
		'''
			metamodel "foo"
			
			modifyEcore aTest epackage foo {
				addNewESubpackage("anewsubpackage", "aprefix", "aURI") [
					addNewEClass("AddedInSubpackage")
				]
			}
		'''.parseWithTestEcore => [
			getENamedElements(
				copiedEPackages.head.ESubpackages.head, it
			).
			assertNamedElements(
				'''
				AddedInSubpackage
				'''
			)
		]
	}

	@Test
	def void testSubPackageEPackageENamedElementsWithSubPackages() {
		referenceToMetamodelWithSubPackage.parseWithTestEcoreWithSubPackage => [
			getENamedElements(
				getEPackageByName("mainpackage").ESubpackages.head, it
			).
			assertNamedElements(
				'''
				MainSubPackageFooClass
				MyClass
				subsubpackage
				'''
			)
		]
	}

	@Test
	def void testSubSubPackageEPackageENamedElementsWithSubPackages() {
		referenceToMetamodelWithSubPackage.parseWithTestEcoreWithSubPackage => [
			getENamedElements(
				getEPackageByName("mainpackage")
					.ESubpackages.head
					.ESubpackages.head, it
			).
			assertNamedElements(
				'''
				MyClass
				'''
			)
		]
	}

	@Test
	def void testEPackageENamedElementsWithCycleInSubPackages() {
		referenceToMetamodelWithSubPackage.parseWithTestEcoreWithSubPackage => [
			val mainpackage = getEPackageByName("mainpackage")
			val subsubpackage = mainpackage
								.ESubpackages.head
								.ESubpackages.head
			// simulate the loop in the package relation
			subsubpackage.ESubpackages += mainpackage
			getENamedElements(subsubpackage, it).
			assertNamedElements(
				'''
				MyClass
				mainpackage
				MyClass
				mainpackage
				'''
			)
			// it simply returns the first package of the loop
		]
	}

	@Test
	def void testENamedElementsWithWithTheSameNameInSubPackages() {
		referenceToMetamodelWithSubPackage.parseWithTestEcoreWithSubPackage => [
			val mainpackage = getEPackageByName("mainpackage")
			val subsubpackage = mainpackage
								.ESubpackages.head
								.ESubpackages.head
			// both packages have a class with the same name but with different
			// structure
			getENamedElements(subsubpackage.getEClassiferByName("MyClass"), it).
				assertNamedElements(
				'''

				''')
			getENamedElements(mainpackage.getEClassiferByName("MyClass"), it).
				assertNamedElements(
				'''
				myClassAttribute
				''')
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
