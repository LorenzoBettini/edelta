package edelta.ui.tests.utils;

import java.util.List;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.ui.PlatformUI;
import org.eclipse.xtext.ui.testing.util.IResourcesSetupUtil;

import com.google.inject.Inject;

/**
 * Utility class for creating an Edelta Plug-in project for testing.
 * 
 * @author Lorenzo Bettini
 */
public class EdeltaPluginProjectHelper {
	@Inject
	private PluginProjectHelper pluginProjectHelper;

	public static final String PROJECT_NAME = "customPluginProject";

	public IJavaProject createEdeltaPluginProject(final String projectName) throws Exception {
		final IJavaProject pluginJavaProject = pluginProjectHelper.createJavaPluginProject(projectName,
				List.of("edelta.lib"),
				List.of("model"));
		IResourcesSetupUtil.createFile(projectName + "/model/My.ecore",
			"""
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
			"""
		);
		return pluginJavaProject;
	}

	public static void closeWelcomePage() {
		var introManager = PlatformUI.getWorkbench().getIntroManager();
		var intro = introManager.getIntro();
		if (intro != null) {
			introManager.closeIntro(intro);
		}
	}

	public static void closeEditors() {
		PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().closeAllEditors(false);
	}
}
