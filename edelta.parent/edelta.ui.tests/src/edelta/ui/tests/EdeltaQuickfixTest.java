package edelta.ui.tests;

import org.eclipse.xtext.testing.Flaky;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.XtextRunner;
import org.eclipse.xtext.ui.testing.AbstractQuickfixTest;
import org.eclipse.xtext.ui.testing.AbstractWorkbenchTest;
import org.eclipse.xtext.ui.testing.util.IResourcesSetupUtil;
import org.eclipse.xtext.xbase.lib.InputOutput;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import edelta.ui.tests.utils.ProjectImportUtil;
import edelta.validation.EdeltaValidator;

/**
 * The tests rely on the ecore file:
 * /edelta.ui.tests.project/model/MySubPackages.ecore
 * 
 * @author Lorenzo Bettini
 */
@RunWith(XtextRunner.class)
@InjectWith(EdeltaUiInjectorProvider.class)
public class EdeltaQuickfixTest extends AbstractQuickfixTest {
	private static String TEST_PROJECT = "edelta.ui.tests.project";

	@Rule
	public Flaky.Rule testRule = new Flaky.Rule();

	@BeforeClass
	public static void importProject() throws Exception {
		ProjectImportUtil.importProject(EdeltaQuickfixTest.TEST_PROJECT);
		IResourcesSetupUtil.waitForBuild();
	}

	/**
	 * Avoids deleting project
	 */
	@Override
	public void setUp() {
	}

	/**
	 * Avoids deleting project
	 */
	@Override
	public void tearDown() {
		waitForEventProcessing();
		AbstractWorkbenchTest.closeEditors();
		waitForEventProcessing();
	}

	@Override
	protected String getProjectName() {
		return TEST_PROJECT;
	}

	/**
	 * Better to put Edelta file in a source folder
	 */
	@Override
	protected String getFileName() {
		return "src/" + super.getFileName();
	}

	@Test
	@Flaky
	public void fixSubPackageImport() {
		InputOutput.<String>println("*** Executing fixSubPackageImport...");
		testQuickfixesOn(
			"metamodel \"mainpackage.subpackage\"",
			EdeltaValidator.INVALID_SUBPACKAGE_IMPORT,
				new AbstractQuickfixTest.Quickfix(
					"Import root EPackage",
					"Import root EPackage \'mainpackage\'",
					"metamodel \"mainpackage\""));
	}

	@Test
	@Flaky
	public void fixSubPackageImportWithSeveralImports() {
		testQuickfixesOn(
			"""
			metamodel "foo"
			metamodel "mainpackage.subpackage.subsubpackage"
			""",
			EdeltaValidator.INVALID_SUBPACKAGE_IMPORT,
			new AbstractQuickfixTest.Quickfix(
				"Import root EPackage",
				"Import root EPackage \'mainpackage\'",
				"""
				metamodel "foo"
				metamodel "mainpackage"
				"""));
	}

	@Test
	public void fixAccessToRenamedElement() {
		testQuickfixesOn(
			"""
			metamodel "mainpackage"
			
			modifyEcore renaming epackage mainpackage {
				ecoreref(subsubpackage.MyClass.myAttribute).name = "Renamed"
			}
			
			modifyEcore access epackage mainpackage {
				ecoreref(subsubpackage.MyClass.myAttribute)
			}
			""",
			EdeltaValidator.INTERPRETER_ACCESS_RENAMED_ELEMENT,
			new AbstractQuickfixTest.Quickfix(
				"Use renamed element",
				"Use renamed element \'mainpackage.subpackage.subsubpackage.MyClass.Renamed\'",
				"""
				metamodel "mainpackage"
				
				modifyEcore renaming epackage mainpackage {
					ecoreref(subsubpackage.MyClass.myAttribute).name = "Renamed"
				}
				
				modifyEcore access epackage mainpackage {
					ecoreref(mainpackage.subpackage.subsubpackage.MyClass.Renamed)
				}
				"""));
	}

	@Test
	public void fixAmbiguousEcoreRef() {
		testQuickfixesOn(
			"""
			metamodel "mainpackage"
			
			modifyEcore aTest epackage mainpackage {
				ecoreref(MyClass)
			}
			""",
			EdeltaValidator.AMBIGUOUS_REFERENCE,
			new AbstractQuickfixTest.Quickfix(
				"Fix ambiguity with \'mainpackage.MyClass\'", "Fix ambiguity with \'mainpackage.MyClass\'",
				"""
				metamodel "mainpackage"
				
				modifyEcore aTest epackage mainpackage {
					ecoreref(mainpackage.MyClass)
				}
				"""),
			new AbstractQuickfixTest.Quickfix(
				"Fix ambiguity with \'mainpackage.subpackage.MyClass\'",
				"Fix ambiguity with \'mainpackage.subpackage.MyClass\'",
				"""
				metamodel "mainpackage"
				
				modifyEcore aTest epackage mainpackage {
					ecoreref(mainpackage.subpackage.MyClass)
				}
				"""),
			new AbstractQuickfixTest.Quickfix(
				"Fix ambiguity with \'mainpackage.subpackage.subsubpackage.MyClass\'",
				"Fix ambiguity with \'mainpackage.subpackage.subsubpackage.MyClass\'",
				"""
				metamodel "mainpackage"
				
				modifyEcore aTest epackage mainpackage {
					ecoreref(mainpackage.subpackage.subsubpackage.MyClass)
				}
				"""));
	}

	@Test
	public void fixRemoveDuplicateImport() {
		testQuickfixesOn(
			"""
			metamodel "bar"

			metamodel "bar"

			metamodel "foo"
			""",
			EdeltaValidator.DUPLICATE_METAMODEL_IMPORT,
			new AbstractQuickfixTest.Quickfix(
				"Remove duplicate metamodel import",
				"Remove duplicate metamodel import",
				"""
				metamodel "bar"
				
				
				metamodel "foo"
				"""));
	}

	@Test
	public void fixMoveToRightPosition() {
		testQuickfixesOn(
			"""
			metamodel "foo"
			
			modifyEcore creation epackage foo {
				ecoreref(NewClass).abstract = true
			
				addNewEClass("NewClass")
				ecoreref(NewClass).ESuperTypes += ecoreref(FooClass)
			}
			""",
			EdeltaValidator.INTERPRETER_ACCESS_NOT_YET_EXISTING_ELEMENT,
			new AbstractQuickfixTest.Quickfix(
				"Move to the right position",
				"Move to the right position",
				"""
				metamodel "foo"
				
				modifyEcore creation epackage foo {
				
					addNewEClass("NewClass")
					ecoreref(NewClass).abstract = true
					ecoreref(NewClass).ESuperTypes += ecoreref(FooClass)
				}
				"""));
	}
}
