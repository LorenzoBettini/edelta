package edelta.ui.tests

import edelta.ui.tests.utils.ProjectImportUtil
import edelta.validation.EdeltaValidator
import org.eclipse.xtext.testing.Flaky
import org.eclipse.xtext.testing.InjectWith
import org.eclipse.xtext.testing.XtextRunner
import org.eclipse.xtext.ui.testing.AbstractQuickfixTest
import org.eclipse.xtext.ui.testing.util.IResourcesSetupUtil
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.BeforeClass

/**
 * The tests rely on the ecore file:
 * /edelta.ui.tests.project/model/MySubPackages.ecore
 * 
 * @author Lorenzo Bettini
 */
@RunWith(XtextRunner)
@InjectWith(EdeltaUiInjectorProvider)
class EdeltaQuickfixTest extends AbstractQuickfixTest {

	static val TEST_PROJECT = "edelta.ui.tests.project"

	@Rule
	public Flaky.Rule testRule = new Flaky.Rule();

	@BeforeClass
	def static void importProject() {
		/*
		 * Edelta requires a plug-in project to run the interpreter
		 * with edelta.lib as dependency
		 */
		ProjectImportUtil.importProject(TEST_PROJECT)
		IResourcesSetupUtil.waitForBuild
	}

	/**
	 * Avoids deleting project
	 */
	override void setUp() {

	}

	/**
	 * Avoids deleting project
	 */
	override void tearDown() {
		waitForEventProcessing();
		closeEditors();
		waitForEventProcessing();
	}

	override protected getProjectName() {
		TEST_PROJECT
	}

	override protected getFileName() {
		/*
		 * Better to put Edelta file in a source folder
		 */
		"src/" + super.getFileName()
	}

	@Test @Flaky
	def fixSubPackageImport() {
		println("*** Executing fixSubPackageImport...")
		'''
			metamodel "mainpackage.subpackage"
		'''.testQuickfixesOn
		(EdeltaValidator.INVALID_SUBPACKAGE_IMPORT,
			new Quickfix("Import root EPackage",
			"Import root EPackage 'mainpackage'",
		'''
			metamodel "mainpackage"
		'''))
	}

	@Test @Flaky
	def fixSubPackageImportWithSeveralImports() {
		println("*** Executing fixSubPackageImportWithSeveralImports...")
		'''
			metamodel "foo"
			metamodel "mainpackage.subpackage.subsubpackage"
		'''.testQuickfixesOn
		(EdeltaValidator.INVALID_SUBPACKAGE_IMPORT,
			new Quickfix("Import root EPackage",
			"Import root EPackage 'mainpackage'",
		'''
			metamodel "foo"
			metamodel "mainpackage"
		'''))
	}

	@Test def fixAccessToRenamedElement() {
		'''
			metamodel "mainpackage"
			
			modifyEcore renaming epackage mainpackage {
				ecoreref(subsubpackage.MyClass.myAttribute).name = "Renamed"
			}
			
			modifyEcore access epackage mainpackage {
				ecoreref(subsubpackage.MyClass.myAttribute)
			}
		'''.testQuickfixesOn
		(EdeltaValidator.INTERPRETER_ACCESS_RENAMED_ELEMENT,
			new Quickfix("Use renamed element",
			"Use renamed element 'mainpackage.subpackage.subsubpackage.MyClass.Renamed'",
		'''
			metamodel "mainpackage"
			
			modifyEcore renaming epackage mainpackage {
				ecoreref(subsubpackage.MyClass.myAttribute).name = "Renamed"
			}
			
			modifyEcore access epackage mainpackage {
				ecoreref(mainpackage.subpackage.subsubpackage.MyClass.Renamed)
			}
		'''))
	}

	@Test def fixAmbiguousEcoreRef() {
		'''
			metamodel "mainpackage"
			
			modifyEcore aTest epackage mainpackage {
				ecoreref(MyClass)
			}
		'''.testQuickfixesOn
		(EdeltaValidator.AMBIGUOUS_REFERENCE,
			new Quickfix(
			"Fix ambiguity with 'mainpackage.MyClass'",
			"Fix ambiguity with 'mainpackage.MyClass'",
			'''
				metamodel "mainpackage"
				
				modifyEcore aTest epackage mainpackage {
					ecoreref(mainpackage.MyClass)
				}
			'''),
			new Quickfix(
			"Fix ambiguity with 'mainpackage.subpackage.MyClass'",
			"Fix ambiguity with 'mainpackage.subpackage.MyClass'",
			'''
				metamodel "mainpackage"
				
				modifyEcore aTest epackage mainpackage {
					ecoreref(mainpackage.subpackage.MyClass)
				}
			'''),
			new Quickfix(
			"Fix ambiguity with 'mainpackage.subpackage.subsubpackage.MyClass'",
			"Fix ambiguity with 'mainpackage.subpackage.subsubpackage.MyClass'",
			'''
				metamodel "mainpackage"
				
				modifyEcore aTest epackage mainpackage {
					ecoreref(mainpackage.subpackage.subsubpackage.MyClass)
				}
			'''))
	}

	@Test def fixRemoveDuplicateImport() {
		'''
			metamodel "bar"

			metamodel "bar"

			metamodel "foo"
		'''.testQuickfixesOn
		(EdeltaValidator.DUPLICATE_METAMODEL_IMPORT,
			new Quickfix("Remove duplicate metamodel import",
			"Remove duplicate metamodel import",
		'''
			metamodel "bar"
			
			
			metamodel "foo"
		'''))
	}

	@Test
	def fixMoveToRightPosition() {
		'''
			metamodel "foo"
			
			modifyEcore creation epackage foo {
				ecoreref(NewClass).abstract = true
			
				addNewEClass("NewClass")
				ecoreref(NewClass).ESuperTypes += ecoreref(FooClass)
			}
		'''.testQuickfixesOn
		(EdeltaValidator.INTERPRETER_ACCESS_NOT_YET_EXISTING_ELEMENT,
			new Quickfix("Move to the right position",
			"Move to the right position",
		'''
			metamodel "foo"
			
			modifyEcore creation epackage foo {
			
				addNewEClass("NewClass")
				ecoreref(NewClass).abstract = true
				ecoreref(NewClass).ESuperTypes += ecoreref(FooClass)
			}
		'''))
	}

}
