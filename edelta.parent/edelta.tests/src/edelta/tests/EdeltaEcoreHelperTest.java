package edelta.tests;

import static org.eclipse.xtext.xbase.lib.IterableExtensions.head;
import static org.junit.Assert.assertTrue;

import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.XtextRunner;
import org.eclipse.xtext.xbase.lib.IterableExtensions;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.inject.Inject;

import edelta.tests.injectors.EdeltaInjectorProviderCustom;
import edelta.util.EdeltaEcoreHelper;

@RunWith(XtextRunner.class)
@InjectWith(EdeltaInjectorProviderCustom.class)
public class EdeltaEcoreHelperTest extends EdeltaAbstractTest {
	@Inject
	private EdeltaEcoreHelper ecoreHelper;

	@Test
	public void testProgramENamedElements() throws Exception {
		assertNamedElements(ecoreHelper
			.getProgramENamedElements(parseWithTestEcores(inputs.referencesToMetamodels())), """
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
				""");
	}

	@Test
	public void testProgramENamedElementsWithCopiedEPackages() throws Exception {
		// note that the order is different w.r.t. the previous test
		// since here we retrieve copied EPackage that are stored in a Map
		assertNamedElements(ecoreHelper
			.getProgramENamedElements(
				parseWithTestEcores(inputs.referencesToMetamodelsWithCopiedEPackages())), """
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
			""");
	}

	@Test
	public void testProgramENamedElementsWithSubPackages() throws Exception {
		// MyClass with myClassAttribute
		// is present in the package and in subpackages
		// so it appears several times
		assertNamedElements(ecoreHelper
			.getProgramENamedElements(
				parseWithTestEcoreWithSubPackage(
					inputs.referenceToMetamodelWithSubPackageWithCopiedEPackages())), """
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
			""");
	}

	@Test
	public void testProgramWithCreatedEClassENamedElements() throws Exception {
		assertNamedElements(ecoreHelper
			.getProgramENamedElements(
				parseWithTestEcore(inputs.referenceToCreatedEClass())), """
			foo
			FooClass
			myAttribute
			myReference
			FooDataType
			FooEnum
			FooEnumLiteral
			NewClass
			""");
		// NewClass is the one created in the program
	}

	@Test
	public void testCreateSnapshotOfAccessibleElementsWithUnresolvedEPackage() throws Exception {
		assertAccessibleElements(
			ecoreHelper.createSnapshotOfAccessibleElements(
				parseWithTestEcore("metamodel \"nonexisting\"")), "");
	}

	@Test
	public void testCreateSnapshotOfAccessibleElementsWithCreatedEClass() throws Exception {
		assertAccessibleElements(ecoreHelper
				.createSnapshotOfAccessibleElements(
					parseWithTestEcore(inputs.referenceToCreatedEClass())), """
			foo
			foo.FooClass
			foo.FooClass.myAttribute
			foo.FooClass.myReference
			foo.FooDataType
			foo.FooEnum
			foo.FooEnum.FooEnumLiteral
			foo.NewClass
			""");
		// NewClass is the one created in the program
	}

	@Test
	public void testCreateSnapshotOfAccessibleElementsWithRemovedEClass() throws Exception {
		assertAccessibleElements(ecoreHelper
				.createSnapshotOfAccessibleElements(
					parseWithTestEcore(inputs.referenceToEClassRemoved())), """
			foo
			foo.FooDataType
			foo.FooEnum
			foo.FooEnum.FooEnumLiteral
			""");
		// FooClass has been removed
	}

	@Test
	public void testGetCurrentAccessibleElementsWithCreatedEClass() throws Exception {
		assertAccessibleElements(ecoreHelper
				.getCurrentAccessibleElements(
					parseWithTestEcore(inputs.referenceToCreatedEClass())), """
			foo
			foo.FooClass
			foo.FooClass.myAttribute
			foo.FooClass.myReference
			foo.FooDataType
			foo.FooEnum
			foo.FooEnum.FooEnumLiteral
			foo.NewClass
			""");
		// NewClass is the one created in the program
	}

	@Test
	public void testEPackageENamedElements() throws Exception {
		var prog = parseWithTestEcore(inputs.referenceToMetamodel());
		assertNamedElements(ecoreHelper
				.getENamedElements(getEPackageByName(prog, "foo")), """
			FooClass
			FooDataType
			FooEnum
			""");
	}

	@Test
	public void testEPackageENamedElementsWithSubPackages() throws Exception {
		var prog = parseWithTestEcoreWithSubPackage(inputs.referenceToMetamodelWithSubPackage());
		assertNamedElements(ecoreHelper
				.getENamedElements(getEPackageByName(prog, "mainpackage")), """
			MainFooClass
			MainFooDataType
			MainFooEnum
			MyClass
			mainsubpackage
			""");
	}

	@Test
	public void testEPackageENamedElementsWithNewSubPackages() throws Exception {
		var prog = parseWithTestEcore("""
			metamodel "foo"

			modifyEcore aTest epackage foo {
				addNewESubpackage("anewsubpackage", "aprefix", "aURI") [
					addNewEClass("AddedInSubpackage")
				]
			}
			""");
		assertNamedElements(
			ecoreHelper.getENamedElements(
				head(getCopiedEPackages(prog)).getESubpackages().get(0)),
			"AddedInSubpackage");
	}

	@Test
	public void testSubPackageEPackageENamedElementsWithSubPackages() throws Exception {
		var prog = parseWithTestEcoreWithSubPackage(
				inputs.referenceToMetamodelWithSubPackage());
		assertNamedElements(ecoreHelper
				.getENamedElements(
					getEPackageByName(prog, "mainpackage")
						.getESubpackages().get(0)), """
			MainSubPackageFooClass
			MyClass
			subsubpackage
			""");
	}

	@Test
	public void testSubSubPackageEPackageENamedElementsWithSubPackages() throws Exception {
		var prog = parseWithTestEcoreWithSubPackage(inputs.referenceToMetamodelWithSubPackage());
		assertNamedElements(
			ecoreHelper.getENamedElements(
				getEPackageByName(prog, "mainpackage")
					.getESubpackages().get(0)
					.getESubpackages().get(0)),
			"MyClass");
	}

	@Test
	public void testEPackageENamedElementsWithCycleInSubPackages() throws Exception {
		var prog = parseWithTestEcoreWithSubPackage(inputs.referenceToMetamodelWithSubPackage());
		var mainpackage = getEPackageByName(prog, "mainpackage");
		var subsubpackage = mainpackage
			.getESubpackages().get(0)
			.getESubpackages().get(0);
		// simulate the loop in the package relation
		subsubpackage.getESubpackages().add(mainpackage);
		assertNamedElements(
			ecoreHelper.getENamedElements(subsubpackage),
			"MyClass\nmainpackage");
		// it simply returns the first package of the loop
	}

	@Test
	public void testENamedElementsWithWithTheSameNameInSubPackages() throws Exception {
		var prog = parseWithTestEcoreWithSubPackage(inputs.referenceToMetamodelWithSubPackage());
		var mainpackage = getEPackageByName(prog, "mainpackage");
		var subsubpackage = mainpackage.getESubpackages().get(0).getESubpackages().get(0);
		// both packages have a class with the same name but with different
		// structure
		assertNamedElements(
			ecoreHelper
				.getENamedElements(getEClassiferByName(subsubpackage, "MyClass")),
			"");
		assertNamedElements(
			ecoreHelper
				.getENamedElements(getEClassiferByName(mainpackage, "MyClass")),
			"myClassAttribute");
	}

	@Test
	public void testEDataTypeENamedElements() throws Exception {
		var prog = parseWithTestEcore(inputs.referenceToMetamodel());
		assertNamedElements(
			ecoreHelper
				.getENamedElements(getEClassifierByName(prog, "foo", "FooDataType")),
			"");
	}

	@Test
	public void testENumENamedElements() throws Exception {
		var prog = parseWithTestEcore(inputs.referenceToMetamodel());
		assertNamedElements(
			ecoreHelper
				.getENamedElements(getEClassifierByName(prog, "foo", "FooEnum")),
			"FooEnumLiteral");
	}

	@Test
	public void testENumENamedElementsWithCreatedEClass() throws Exception {
		var prog = parseWithTestEcore(inputs.referenceToCreatedEClass());
		assertNamedElements(
			ecoreHelper
				.getENamedElements(getEClassifierByName(prog, "foo", "FooEnum")),
			"FooEnumLiteral");
	}

	@Test
	public void testNullENamedElements() throws Exception {
		parseWithTestEcore(inputs.referenceToMetamodel());
		assertTrue(IterableExtensions.isEmpty(ecoreHelper.getENamedElements(null)));
	}

	@Test
	public void testEClassENamedElements() throws Exception {
		var prog = parseWithTestEcore(inputs.referenceToMetamodel());
		assertNamedElements(
			ecoreHelper
				.getENamedElements(getEClassifierByName(prog, "foo", "FooClass")),
			"myAttribute\nmyReference");
	}

	@Test
	public void testEClassENamedElementsWithCreatedEClass() throws Exception {
		var prog = parseWithTestEcore(inputs.referenceToCreatedEClass());
		assertNamedElements(
			ecoreHelper
				.getENamedElements(getEClassifierByName(prog, "foo", "FooClass")),
			"myAttribute\nmyReference");
	}

	@Test
	public void testENamedElementsOfEPackage() throws Exception {
		var prog = parseWithTestEcoreWithSubPackage(inputs.referenceToMetamodelWithSubPackage());
		assertNamedElements(
				ecoreHelper
				.getENamedElements(getEPackageByName(prog, "mainpackage")), """
			MainFooClass
			MainFooDataType
			MainFooEnum
			MyClass
			mainsubpackage
			""");
	}

	@Test
	public void testENamedElementsOfEClass() throws Exception {
		var prog = parseWithTestEcore(inputs.referenceToMetamodel());
		assertNamedElements(
			ecoreHelper
				.getENamedElements(getEClassifierByName(prog, "foo", "FooClass")),
			"myAttribute\nmyReference");
	}

	@Test
	public void testENamedElementsOfENum() throws Exception {
		var prog = parseWithTestEcore(inputs.referenceToMetamodel());
		assertNamedElements(
			ecoreHelper
				.getENamedElements(getEClassifierByName(prog, "foo", "FooEnum")),
			"FooEnumLiteral");
	}
}
