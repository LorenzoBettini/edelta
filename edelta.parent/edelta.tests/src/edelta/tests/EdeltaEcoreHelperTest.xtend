package edelta.tests

import com.google.inject.Inject
import edelta.tests.injectors.EdeltaInjectorProviderCustom
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
	def void testProgramENamedElements() throws Exception {
		referencesToMetamodels.parseWithTestEcores.
			getProgramENamedElements.
			assertNamedElements(
				'''
				foo
				FooClass
				myAttribute
				myReference
				FooDataType
				FooEnum
				FooEnumLiteral
				bar
				BarClass
				myAttribute
				myReference
				BarDataType
				'''
			)
	}

	@Test
	def void testProgramENamedElementsWithCopiedEPackages() throws Exception {
		// note that the order is different w.r.t. the previous test
		// since here we retrieve copied EPackage that are stored in a Map
		referencesToMetamodelsWithCopiedEPackages.parseWithTestEcores.
			getProgramENamedElements.
			assertNamedElements(
				'''
				bar
				BarClass
				myAttribute
				myReference
				BarDataType
				foo
				FooClass
				myAttribute
				myReference
				FooDataType
				FooEnum
				FooEnumLiteral
				'''
			)
	}

	@Test
	def void testProgramENamedElementsWithSubPackages() throws Exception {
		// MyClass with myClassAttribute
		// is present in the package and in subpackages
		// so it appears several times
		referenceToMetamodelWithSubPackageWithCopiedEPackages
			.parseWithTestEcoreWithSubPackage.
			getProgramENamedElements.
			assertNamedElements(
				'''
				mainpackage
				MainFooClass
				myAttribute
				myReference
				MainFooDataType
				MainFooEnum
				FooEnumLiteral
				MyClass
				myClassAttribute
				mainsubpackage
				MainSubPackageFooClass
				mySubPackageAttribute
				mySubPackageReference
				MyClass
				myClassAttribute
				subsubpackage
				MyClass
				'''
			)
	}

	@Test
	def void testProgramWithCreatedEClassENamedElements() throws Exception {
		referenceToCreatedEClass.parseWithTestEcore => [
			getProgramENamedElements.
			assertNamedElements(
				'''
				foo
				FooClass
				myAttribute
				myReference
				FooDataType
				FooEnum
				FooEnumLiteral
				NewClass
				'''
			)
		// NewClass is the one created in the program
		]
	}

	@Test
	def void testCreateSnapshotOfAccessibleElementsWithUnresolvedEPackage() throws Exception {
		'''
		metamodel "nonexisting"
		'''.parseWithTestEcore => [
			createSnapshotOfAccessibleElements.
			assertAccessibleElements(
				'''

				'''
			)
		]
	}

	@Test
	def void testCreateSnapshotOfAccessibleElementsWithCreatedEClass() throws Exception {
		referenceToCreatedEClass.parseWithTestEcore => [
			createSnapshotOfAccessibleElements.
			assertAccessibleElements(
				'''
				foo
				foo.FooClass
				foo.FooClass.myAttribute
				foo.FooClass.myReference
				foo.FooDataType
				foo.FooEnum
				foo.FooEnum.FooEnumLiteral
				foo.NewClass
				'''
			)
		// NewClass is the one created in the program
		]
	}

	@Test
	def void testCreateSnapshotOfAccessibleElementsWithRemovedEClass() throws Exception {
		referenceToEClassRemoved.parseWithTestEcore => [
			createSnapshotOfAccessibleElements.
			assertAccessibleElements(
				'''
				foo
				foo.FooDataType
				foo.FooEnum
				foo.FooEnum.FooEnumLiteral
				'''
				// FooClass has been removed
			)
		]
	}

	@Test
	def void testGetCurrentAccessibleElementsWithCreatedEClass() throws Exception {
		referenceToCreatedEClass.parseWithTestEcore => [
			getCurrentAccessibleElements.
			assertAccessibleElements(
				'''
				foo
				foo.FooClass
				foo.FooClass.myAttribute
				foo.FooClass.myReference
				foo.FooDataType
				foo.FooEnum
				foo.FooEnum.FooEnumLiteral
				foo.NewClass
				'''
			)
		// NewClass is the one created in the program
		]
	}

	@Test
	def void testEPackageENamedElements() throws Exception {
		referenceToMetamodel.parseWithTestEcore => [
			getENamedElements(getEPackageByName("foo")).
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
	def void testEPackageENamedElementsWithSubPackages() throws Exception {
		referenceToMetamodelWithSubPackage.parseWithTestEcoreWithSubPackage => [
			getENamedElements(getEPackageByName("mainpackage")).
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
	def void testEPackageENamedElementsWithNewSubPackages() throws Exception {
		'''
			metamodel "foo"
			
			modifyEcore aTest epackage foo {
				addNewESubpackage("anewsubpackage", "aprefix", "aURI") [
					addNewEClass("AddedInSubpackage")
				]
			}
		'''.parseWithTestEcore => [
			getENamedElements(copiedEPackages.head.ESubpackages.head).
			assertNamedElements(
				'''
				AddedInSubpackage
				'''
			)
		]
	}

	@Test
	def void testSubPackageEPackageENamedElementsWithSubPackages() throws Exception {
		referenceToMetamodelWithSubPackage.parseWithTestEcoreWithSubPackage => [
			getENamedElements(getEPackageByName("mainpackage").ESubpackages.head).
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
	def void testSubSubPackageEPackageENamedElementsWithSubPackages() throws Exception {
		referenceToMetamodelWithSubPackage.parseWithTestEcoreWithSubPackage => [
			getENamedElements(
				getEPackageByName("mainpackage")
					.ESubpackages.head
					.ESubpackages.head
			).
			assertNamedElements(
				'''
				MyClass
				'''
			)
		]
	}

	@Test
	def void testEPackageENamedElementsWithCycleInSubPackages() throws Exception {
		referenceToMetamodelWithSubPackage.parseWithTestEcoreWithSubPackage => [
			val mainpackage = getEPackageByName("mainpackage")
			val subsubpackage = mainpackage
								.ESubpackages.head
								.ESubpackages.head
			// simulate the loop in the package relation
			subsubpackage.ESubpackages += mainpackage
			getENamedElements(subsubpackage).
			assertNamedElements(
				'''
				MyClass
				mainpackage
				'''
			)
			// it simply returns the first package of the loop
		]
	}

	@Test
	def void testENamedElementsWithWithTheSameNameInSubPackages() throws Exception {
		referenceToMetamodelWithSubPackage.parseWithTestEcoreWithSubPackage => [
			val mainpackage = getEPackageByName("mainpackage")
			val subsubpackage = mainpackage
								.ESubpackages.head
								.ESubpackages.head
			// both packages have a class with the same name but with different
			// structure
			getENamedElements(subsubpackage.getEClassiferByName("MyClass")).
				assertNamedElements(
				'''

				''')
			getENamedElements(mainpackage.getEClassiferByName("MyClass")).
				assertNamedElements(
				'''
				myClassAttribute
				''')
		]
	}

	@Test
	def void testEDataTypeENamedElements() throws Exception {
		referenceToMetamodel.parseWithTestEcore => [
			getENamedElements(getEClassifierByName("foo", "FooDataType")).
			assertNamedElements(
				'''

				'''
			)
		]
	}

	@Test
	def void testENumENamedElements() throws Exception {
		referenceToMetamodel.parseWithTestEcore => [
			getENamedElements(getEClassifierByName("foo", "FooEnum")).
			assertNamedElements(
				'''
				FooEnumLiteral
				'''
			)
		]
	}

	@Test
	def void testENumENamedElementsWithCreatedEClass() throws Exception {
		referenceToCreatedEClass.parseWithTestEcore => [
			getENamedElements(getEClassifierByName("foo", "FooEnum")).
			assertNamedElements(
				'''
				FooEnumLiteral
				'''
			)
		]
	}

	@Test
	def void testNullENamedElements() throws Exception {
		referenceToMetamodel.parseWithTestEcore => [
			Assert.assertTrue(getENamedElements(null).isEmpty)
		]
	}

	@Test
	def void testEClassENamedElements() throws Exception {
		referenceToMetamodel.parseWithTestEcore => [
			getENamedElements(getEClassifierByName("foo", "FooClass")).
			assertNamedElements(
				'''
				myAttribute
				myReference
				'''
			)
		]
	}

	@Test
	def void testEClassENamedElementsWithCreatedEClass() throws Exception {
		referenceToCreatedEClass.parseWithTestEcore => [
			getENamedElements(getEClassifierByName("foo", "FooClass")).
			assertNamedElements(
				'''
				myAttribute
				myReference
				'''
			)
		]
	}

	@Test
	def void testENamedElementsOfEPackage() throws Exception {
		referenceToMetamodelWithSubPackage.parseWithTestEcoreWithSubPackage => [
			getENamedElements(getEPackageByName("mainpackage")).
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
	def void testENamedElementsOfEClass() throws Exception {
		referenceToMetamodel.parseWithTestEcore => [
			getENamedElements(getEClassifierByName("foo", "FooClass")).
			assertNamedElements(
				'''
				myAttribute
				myReference
				'''
			)
		]
	}

	@Test
	def void testENamedElementsOfENum() throws Exception {
		referenceToMetamodel.parseWithTestEcore => [
			getENamedElements(getEClassifierByName("foo", "FooEnum")).
			assertNamedElements(
				'''
				FooEnumLiteral
				'''
			)
		]
	}
}
