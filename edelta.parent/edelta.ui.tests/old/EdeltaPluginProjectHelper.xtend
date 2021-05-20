package edelta.ui.tests.utils

import com.google.inject.Inject
import org.eclipse.ui.PlatformUI

import static org.eclipse.xtext.ui.testing.util.IResourcesSetupUtil.*

/**
 * Utility class for creating an Edelta Plug-in project for testing.
 * 
 * @author Lorenzo Bettini
 * 
 */
class EdeltaPluginProjectHelper {

	@Inject PluginProjectHelper pluginProjectHelper

	val public static PROJECT_NAME = "customPluginProject"

	def createEdeltaPluginProject(String projectName, String...dependencies) {
		val pluginJavaProject = pluginProjectHelper.createJavaPluginProject(
			projectName,
			(#["edelta.lib"]+dependencies).toList,
			#["model"]
		)
		createFile(projectName+"/model/My.ecore",
			'''
			<?xml version="1.0" encoding="UTF-8"?>
			<ecore:EPackage xmi:version="2.0" xmlns:xmi="http://www.omg.org/XMI" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
			    xmlns:ecore="http://www.eclipse.org/emf/2002/Ecore" name="mypackage" nsURI="http://my.package.org" nsPrefix="mypackage">
			  <eClassifiers xsi:type="ecore:EClass" name="MyClass">
			    <eStructuralFeatures xsi:type="ecore:EAttribute" name="myAttribute" eType="ecore:EDataType http://www.eclipse.org/emf/2002/Ecore#//EString"/>
			    <eStructuralFeatures xsi:type="ecore:EReference" name="myReference" eType="ecore:EClass http://www.eclipse.org/emf/2002/Ecore#//EObject"/>
			  </eClassifiers>
			  <eClassifiers xsi:type="ecore:EDataType" name="MyDataType" instanceClassName="java.lang.String"/>
			  <eClassifiers xsi:type="ecore:EClass" name="MyBaseClass">
			    <eStructuralFeatures xsi:type="ecore:EAttribute" name="myBaseAttribute" eType="ecore:EDataType http://www.eclipse.org/emf/2002/Ecore#//EString"/>
			    <eStructuralFeatures xsi:type="ecore:EReference" name="myBaseReference" eType="ecore:EClass http://www.eclipse.org/emf/2002/Ecore#//EObject"/>
			  </eClassifiers>
			  <eClassifiers xsi:type="ecore:EClass" name="MyDerivedClass" eSuperTypes="#//MyBaseClass">
			    <eStructuralFeatures xsi:type="ecore:EAttribute" name="myDerivedAttribute" eType="ecore:EDataType http://www.eclipse.org/emf/2002/Ecore#//EString"/>
			    <eStructuralFeatures xsi:type="ecore:EReference" name="myDerivedReference" eType="ecore:EClass http://www.eclipse.org/emf/2002/Ecore#//EObject"/>
			  </eClassifiers>
			</ecore:EPackage>
			'''
		)
		return pluginJavaProject
	}

	def static void closeWelcomePage() {
		if (PlatformUI.getWorkbench().getIntroManager().getIntro() !== null) {
			PlatformUI.getWorkbench().getIntroManager().closeIntro(
					PlatformUI.getWorkbench().getIntroManager().getIntro());
		}
	}

	def static void closeEditors() {
		PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().closeAllEditors(false);
	}

}
