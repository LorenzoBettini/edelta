/*
 * generated by Xtext 2.20.0
 */
package edelta.ui.wizard

import org.eclipse.core.runtime.Status
import org.eclipse.jdt.core.JavaCore
import org.eclipse.xtext.ui.XtextProjectHelper
import org.eclipse.xtext.ui.util.PluginProjectFactory
import org.eclipse.xtext.ui.wizard.template.IProjectGenerator
import org.eclipse.xtext.ui.wizard.template.IProjectTemplateProvider
import org.eclipse.xtext.ui.wizard.template.ProjectTemplate

import static org.eclipse.core.runtime.IStatus.*

/**
 * Create a list with all project templates to be shown in the template new project wizard.
 * 
 * Each template is able to generate one or more projects. Each project can be configured such that any number of files are included.
 */
class EdeltaProjectTemplateProvider implements IProjectTemplateProvider {
	override getProjectTemplates() {
		#[new EdeltaExampleProjectTemplate]
	}
}

@ProjectTemplate(label="Edelta Example Project", icon="project_template.png", description="<p><b>Edelta Example Project</b></p>
<p>An Edelta Example Project, with some initial contents: an Ecore file and an Edelta file.</p>")
final class EdeltaExampleProjectTemplate {
	val advanced = check("Advanced:", false)
	val advancedGroup = group("Properties")
	val path = text("Package:", "com/example", "The package path to place the files in", advancedGroup)

	override protected updateVariables() {
		path.enabled = advanced.value
		if (!advanced.value) {
			path.value = "com/example"
		}
	}

	override protected validate() {
		if (path.value.matches('[a-z][a-z0-9_]*(/[a-z][a-z0-9_]*)*'))
			null
		else
			new Status(ERROR, "Wizard", "'" + path + "' is not a valid package name")
	}

	override generateProjects(IProjectGenerator generator) {
		generator.generate(new PluginProjectFactory => [
			projectName = projectInfo.projectName
			location = projectInfo.locationPath
			projectNatures += #[JavaCore.NATURE_ID, "org.eclipse.pde.PluginNature", XtextProjectHelper.NATURE_ID]
			builderIds += #[JavaCore.BUILDER_ID, XtextProjectHelper.BUILDER_ID]
			requiredBundles += "edelta.lib"
			// shouldn't we also create the edelta-gen folder?
			folders += #["src", "model"]
			addFile("modified/README", '''
				Modified ecores will be saved here (see Main.java file)
			'''
			)
			addFile("model/My.ecore", '''
				<?xml version="1.0" encoding="UTF-8"?>
				<ecore:EPackage xmi:version="2.0" xmlns:xmi="http://www.omg.org/XMI" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
				    xmlns:ecore="http://www.eclipse.org/emf/2002/Ecore" name="myecore" nsURI="http://www.eclipse.org/emf/2002/Myecore" nsPrefix="myecore">
				  <eClassifiers xsi:type="ecore:EClass" name="MyEClass">
				    <eStructuralFeatures xsi:type="ecore:EAttribute" name="astring" eType="ecore:EDataType http://www.eclipse.org/emf/2002/Ecore#//EString"/>
				  </eClassifiers>
				  <eClassifiers xsi:type="ecore:EEnum" name="MyENum">
				    <eLiterals name="FirstEnumLiteral"/>
				    <eLiterals name="SecondEnumLiteral" value="1"/>
				  </eClassifiers>
				  <eClassifiers xsi:type="ecore:EClass" name="MyOtherEClass"/>
				</ecore:EPackage>
			''')
			addFile('''src/«path»/Example.edelta''', '''
				import org.eclipse.emf.ecore.EcoreFactory
				
				// IMPORTANT: ecores must be in a source directory
				// otherwise you can't refer to them
				
				package «path.value.replaceAll("/", ".")»
				
				// import existing metamodels
				metamodel "myecore"
				metamodel "ecore" // this one should usually be there
				
				// you can define reusable functions...
				
				/*
				 * Reusable function to create a new EClass with the
				 * specified name, setting MyEClass as its superclass
				 * @param name
				 */
				def myReusableCreateSubclassOfMyEClass(String name) {
					newEClass(name) => [
						// refer to Ecore elements with ecoreref
						ESuperTypes += ecoreref(MyEClass)
					]
				}
				
				// ...and then modification blocks
				// look at the "Outline" view, which immediately shows the modified EPackages
				
				// specify modifications of an EPackage
				modifyEcore someModifications epackage myecore {
					// the currently modified package is available
					// through the implicit parameter 'it', similar to 'this'	
				
					// use the standard Edelta library functions
					addNewEClass("NewClass") [
						// initialize it in a lambda block
						// where the new class is available through the implicit parameter 'it'
						addNewEAttribute("myStringAttribute", ecoreref(EString))
						// references to Ecore elements can be fully qualified
						addNewEReference("myReference", ecoreref(myecore.MyEClass)) [
							// initialization as above
							// the current element is available through the implicit parameter 'it'
							// use syntactic sugar for setters
							upperBound = -1;
							containment = true;
							lowerBound = 0
						]
					]
					// you could also modify existing Ecore elements manually
					ecoreref(MyENum).ELiterals += EcoreFactory.eINSTANCE.createEEnumLiteral => [
						// => [] is the 'with' operator
						name = "ANewEnumLiteral"
						value = 3
					]
					// or again with Edelta library functions
					ecoreref(MyENum).addNewEEnumLiteral("AnotherNewEnumLiteral") [
						value = 4
					]
				}
				
				// you can have several modification blocks for the same EPackage
				modifyEcore otherModifications epackage myecore {
					// you can call the reusable functions you defined
					addEClass(myReusableCreateSubclassOfMyEClass("ASubclassOfMyEClass"))
					// remember you can use the 'with' operator
					addEClass(myReusableCreateSubclassOfMyEClass("AnotherSubclassOfMyEClass") => [
						// and refer to new classes you created in previous modification blocks
						ESuperTypes += ecoreref(NewClass)
					])
				
					// you can rename existing classes
					ecoreref(MyOtherEClass).name = "RenamedClass"
					// and the renamed version is immediately available
					ecoreref(RenamedClass).addNewEAttribute("addedNow", ecoreref(EInt))
				}
			''')
			addFile('''src/«path»/Main.java''', '''
				package «path.value.replaceAll("/", ".")»;
				
				import edelta.lib.AbstractEdelta;
				
				public class Main {
				
					public static void main(String[] args) throws Exception {
						// Create an instance of the generated Java class
						AbstractEdelta edelta = new Example();
						// Make sure you load all the used Ecores (Ecore.ecore is always loaded)
						edelta.loadEcoreFile("model/My.ecore");
						// Execute the actual transformations defined in the DSL
						edelta.execute();
						// Save the modified Ecore model into a new path
						edelta.saveModifiedEcores("modified");
					}
				}
			''')
		])
	}
}
