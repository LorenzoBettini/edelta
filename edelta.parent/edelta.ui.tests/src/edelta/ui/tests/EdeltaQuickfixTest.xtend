package edelta.ui.tests

import com.google.inject.Inject
import edelta.ui.tests.utils.EdeltaPluginProjectHelper
import edelta.validation.EdeltaValidator
import org.eclipse.xtext.testing.InjectWith
import org.eclipse.xtext.testing.XtextRunner
import org.eclipse.xtext.ui.testing.AbstractQuickfixTest
import org.eclipse.xtext.ui.testing.util.IResourcesSetupUtil
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(XtextRunner)
@InjectWith(EdeltaUiInjectorProvider)
class EdeltaQuickfixTest extends AbstractQuickfixTest {

	@Inject EdeltaPluginProjectHelper projectHelper

	override protected getFileName() {
		/*
		 * Better to put Edelta file in a source folder
		 */
		"src/" + super.getFileName()
	}

	@Before def void setup() {
		/*
		 * Edelta requires a plug-in project to run the interpreter
		 * with edelta.lib as dependency
		 */
		projectHelper.createEdeltaPluginProject(projectName)

		IResourcesSetupUtil
			.createFile(
				projectName,
				"src/MySubPackages", "ecore",
				'''
				<?xml version="1.0" encoding="UTF-8"?>
				<ecore:EPackage xmi:version="2.0" xmlns:xmi="http://www.omg.org/XMI" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
				    xmlns:ecore="http://www.eclipse.org/emf/2002/Ecore" name="mainpackage" nsURI="http://my.mainpackage.org" nsPrefix="mainpackage">
				  <eClassifiers xsi:type="ecore:EClass" name="MyClass">
				    <eStructuralFeatures xsi:type="ecore:EAttribute" name="myAttribute" eType="ecore:EDataType http://www.eclipse.org/emf/2002/Ecore#//EString"/>
				    <eStructuralFeatures xsi:type="ecore:EReference" name="myReference" eType="ecore:EClass http://www.eclipse.org/emf/2002/Ecore#//EObject"/>
				  </eClassifiers>
				  <eSubpackages name="subpackage" nsURI="http://mysubpackage" nsPrefix="subpackage">
				    <eClassifiers xsi:type="ecore:EClass" name="MySubPackageClass"/>
				    <eClassifiers xsi:type="ecore:EClass" name="MyClass">
				      <eStructuralFeatures xsi:type="ecore:EAttribute" name="myAttribute" eType="ecore:EDataType http://www.eclipse.org/emf/2002/Ecore#//EString"/>
				      <eStructuralFeatures xsi:type="ecore:EReference" name="myReference" eType="ecore:EClass http://www.eclipse.org/emf/2002/Ecore#//EObject"/>
				    </eClassifiers>
				    <eSubpackages name="subsubpackage" nsURI="http://mysubsubpackage" nsPrefix="subsubpackage">
				      <eClassifiers xsi:type="ecore:EClass" name="MyClass">
				        <eStructuralFeatures xsi:type="ecore:EAttribute" name="myAttribute" eType="ecore:EDataType http://www.eclipse.org/emf/2002/Ecore#//EString"/>
				        <eStructuralFeatures xsi:type="ecore:EReference" name="myReference" eType="ecore:EClass http://www.eclipse.org/emf/2002/Ecore#//EObject"/>
				      </eClassifiers>
				    </eSubpackages>
				  </eSubpackages>
				</ecore:EPackage>
				'''
			)
	}

	@Test def fixSubPackageImport() {
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

	@Test def fixSubPackageImportWithSeveralImports() {
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

}
