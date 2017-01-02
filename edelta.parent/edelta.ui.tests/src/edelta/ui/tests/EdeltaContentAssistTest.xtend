package edelta.ui.tests

import edelta.ui.internal.EdeltaActivator
import edelta.ui.tests.utils.PDETargetPlatformUtils
import edelta.ui.tests.utils.PluginProjectHelper
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.util.stream.Collectors
import org.eclipse.core.resources.IFile
import org.eclipse.core.runtime.NullProgressMonitor
import org.eclipse.emf.ecore.resource.ResourceSet
import org.eclipse.jdt.core.IJavaProject
import org.eclipse.ui.IEditorPart
import org.eclipse.ui.PlatformUI
import org.eclipse.ui.part.FileEditorInput
import org.eclipse.xtext.junit4.InjectWith
import org.eclipse.xtext.junit4.XtextRunner
import org.eclipse.xtext.ui.editor.XtextEditor
import org.eclipse.xtext.ui.editor.utils.EditorUtils
import org.eclipse.xtext.xbase.junit.ui.AbstractContentAssistTest
import org.junit.After
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith

import static org.eclipse.xtext.junit4.ui.util.IResourcesSetupUtil.*

@RunWith(XtextRunner)
@InjectWith(EdeltaUiInjectorProvider)
class EdeltaContentAssistTest extends AbstractContentAssistTest {

	static IJavaProject pluginJavaProject

	val static PROJECT_NAME = "customPluginProject"

	@BeforeClass
	def static void setUp() {
		// needed when building with Tycho, otherwise, dependencies
		// in the MANIFEST of the created project will not be visible
		PDETargetPlatformUtils.setTargetPlatform();

		closeWelcomePage
		val injector = EdeltaActivator.getInstance().getInjector(EdeltaActivator.EDELTA_EDELTA);
		val projectHelper = injector.getInstance(PluginProjectHelper)
		pluginJavaProject = projectHelper.createJavaPluginProject(PROJECT_NAME, newArrayList("edelta.lib"))
		createFile(PROJECT_NAME+"/src/My.ecore",
			'''
			<?xml version="1.0" encoding="UTF-8"?>
			<ecore:EPackage xmi:version="2.0" xmlns:xmi="http://www.omg.org/XMI" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
			    xmlns:ecore="http://www.eclipse.org/emf/2002/Ecore" name="mypackage" nsURI="http://my.package.org" nsPrefix="mypackage">
			  <eClassifiers xsi:type="ecore:EClass" name="MyClass">
			    <eStructuralFeatures xsi:type="ecore:EAttribute" name="myAttribute" eType="ecore:EDataType http://www.eclipse.org/emf/2002/Ecore#//EString"/>
			    <eStructuralFeatures xsi:type="ecore:EReference" name="myReference" eType="ecore:EClass http://www.eclipse.org/emf/2002/Ecore#//EObject"/>
			  </eClassifiers>
			  <eClassifiers xsi:type="ecore:EDataType" name="MyDataType"/>
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
	}

	@AfterClass
	def static void tearDown() {
		pluginJavaProject.project.delete(true, new NullProgressMonitor)
	}

	@After
	def void after() {
		// we need to close existing editors since we use the
		// resource of the open editor to test the content assist
		// so we must make sure we always use a freshly opened editor
		closeEditors
	}

	override getJavaProject(ResourceSet resourceSet) {
		pluginJavaProject
	}

	/**
	 * We need a real resource otherwise the Ecores are not found
	 * (they are collected using visible containers)
	 */
	override getResourceFor(InputStream inputStream) {
		val result = new BufferedReader(new InputStreamReader(inputStream)).
			lines().collect(Collectors.joining("\n"));
		val editor = openEditor(
			createFile(
				PROJECT_NAME+"/src/Test.edelta",
				result
			)
		)
		return editor.document.readOnly[it]
	}

	def protected XtextEditor openEditor(IFile file) {
		val openEditor = openEditor(file, EdeltaActivator.EDELTA_EDELTA);
		return EditorUtils.getXtextEditor(openEditor);
	}

	def private IEditorPart openEditor(IFile file, String editorId) {
		return PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().openEditor(
				new FileEditorInput(file), editorId);
	}

	def static protected void closeWelcomePage() {
		if (PlatformUI.getWorkbench().getIntroManager().getIntro() != null) {
			PlatformUI.getWorkbench().getIntroManager().closeIntro(
					PlatformUI.getWorkbench().getIntroManager().getIntro());
		}
	}

	def static void closeEditors() {
		PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().closeAllEditors(false);
	}

	@Test def void testMetamodelsEcore() {
		newBuilder.append("metamodel ").assertProposal('"ecore"')
	}

	@Test def void testMetamodelsLocalToProject() {
		newBuilder.append("metamodel ").assertProposal('"mypackage"')
	}

	@Test def void testNoNSURIProposalMetamodels() {
		newBuilder.append("metamodel <|>").assertNoProposalAtCursor('"http://my.package.org"')
	}

	@Test def void testEClassifier() {
		newBuilder.append('metamodel "mypackage" eclassifier ').
			assertText('MyClass', 'MyBaseClass', 'MyDerivedClass', 'MyDataType')
	}

	@Test def void testEClass() {
		newBuilder.append('metamodel "mypackage" eclass ').
			assertText('MyClass', 'MyBaseClass', 'MyDerivedClass')
	}

	@Test def void testEDataType() {
		newBuilder.append('metamodel "mypackage" edatatype ').
			assertText('MyDataType')
	}

	@Test def void testEFeature() {
		newBuilder.append('metamodel "mypackage" efeature ').
			assertText('myAttribute', 'myBaseAttribute', 'myDerivedAttribute',
				'myReference', 'myBaseReference', 'myDerivedReference'
			)
	}

	@Test def void testEAttribute() {
		newBuilder.append('metamodel "mypackage" eattribute ').
			assertText('myAttribute', 'myBaseAttribute', 'myDerivedAttribute')
	}

	@Test def void testEReference() {
		newBuilder.append('metamodel "mypackage" ereference ').
			assertText('myReference', 'myBaseReference', 'myDerivedReference')
	}
}
