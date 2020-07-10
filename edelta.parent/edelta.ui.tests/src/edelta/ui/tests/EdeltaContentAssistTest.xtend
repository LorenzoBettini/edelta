package edelta.ui.tests

import edelta.ui.internal.EdeltaActivator
import edelta.ui.tests.utils.EdeltaPluginProjectHelper
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.util.List
import java.util.stream.Collectors
import org.eclipse.core.resources.IFile
import org.eclipse.core.runtime.NullProgressMonitor
import org.eclipse.emf.ecore.resource.ResourceSet
import org.eclipse.jdt.core.IJavaProject
import org.eclipse.ui.IEditorPart
import org.eclipse.ui.PlatformUI
import org.eclipse.ui.part.FileEditorInput
import org.eclipse.xtext.testing.Flaky
import org.eclipse.xtext.testing.InjectWith
import org.eclipse.xtext.testing.XtextRunner
import org.eclipse.xtext.ui.editor.XtextEditor
import org.eclipse.xtext.ui.editor.utils.EditorUtils
import org.eclipse.xtext.ui.testing.AbstractContentAssistTest
import org.junit.After
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

import static edelta.ui.tests.utils.EdeltaPluginProjectHelper.*
import static org.eclipse.xtext.ui.testing.util.IResourcesSetupUtil.*

@RunWith(XtextRunner)
@InjectWith(EdeltaUiInjectorProvider)
class EdeltaContentAssistTest extends AbstractContentAssistTest {

	static IJavaProject pluginJavaProject

	@Rule
	public Flaky.Rule testRule = new Flaky.Rule();

	// cursor position marker
	val cursor = '''<|>'''

	@BeforeClass
	def static void setUp() {
		closeWelcomePage
		val injector = EdeltaActivator.getInstance().getInjector(EdeltaActivator.EDELTA_EDELTA);
		val projectHelper = injector.getInstance(EdeltaPluginProjectHelper)
		pluginJavaProject = projectHelper.createEdeltaPluginProject(PROJECT_NAME)
		waitForBuild
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

	def static void closeEditors() {
		PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().closeAllEditors(false);
	}

	@Test def void testMetamodelsEcore() {
		newBuilder.append("metamodel ").assertProposal('"ecore"')
	}

	@Test def void testMetamodelsLocalToProject() {
		newBuilder.append("metamodel ").assertProposal('"mypackage"')
	}

	@Test def void testMetamodelsInThePresenceOfSubpackages() {
		createFile(PROJECT_NAME+"/model/MySubPackages.ecore",
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
		waitForBuild // required to index the new ecore file
		// mainpackage.subpackage and mainpackage.subpackage.subsubpackage
		// must not be proposed, since they are subpackages,
		// which cannot be directly imported
		newBuilder.append('metamodel ')
			.assertText(
				'''
				"annotation"
				"data"
				"ecore"
				"mainpackage"
				"mypackage"
				"namespace"
				"type"
				'''.fromLinesOfStringsToStringArray)
	}

	@Test def void testNoNSURIProposalMetamodels() {
		newBuilder.append("metamodel <|>").assertNoProposalAtCursor('"http://my.package.org"')
	}

	@Test def void testUnqualifiedEcoreReference() {
		newBuilder.append('''
			metamodel "mypackage"
			modifyEcore aTest epackage mypackage { 
				ecoreref(''').
			assertText('''
				MyBaseClass
				MyClass
				MyDataType
				MyDerivedClass
				myAttribute
				myBaseAttribute
				myBaseReference
				myDerivedAttribute
				myDerivedReference
				myReference
				mypackage
				'''.fromLinesOfStringsToStringArray)
	}

	@Test def void testQualifiedEcoreReference() {
		// don't use Xtend ''' ''' strings since the content assist test
		// seems to have problems with \r in Windows...
		newBuilder.append("metamodel \"mypackage\"
				modifyEcore aTest epackage mypackage {
					ecoreref(MyClass.").
			assertText('myAttribute', 'myReference')
	}

	@Test def void testEClassifierAfterCreatingAnEClass() {
		newBuilder.append('''
			metamodel "mypackage"
			
			modifyEcore aTest epackage mypackage {
				addNewEClass("AAA")
				ecoreref(''').
			assertProposal('AAA')
	}

	@Test def void testEClassifierAfterRenamingAnEClass() {
		newBuilder.append('''
			metamodel "mypackage"
			
			modifyEcore aTest epackage mypackage {
				ecoreref(MyClass).name = "Renamed"
				ecoreref(''').
			assertProposal('Renamed')
	}

	@Test @Flaky
	def void testCreatedEAttributeDuringInterpretationIsProposed() {
		println("*** Executing testCreatedEAttributeDuringInterpretationIsProposed...")
		newBuilder.append('''
			import org.eclipse.emf.ecore.EClass
			
			metamodel "mypackage"
			// don't rely on ecore, since the input files are not saved
			// during the test, thus external libraries are not seen
			// metamodel "ecore"
			
			def myNewAttribute(EClass c, String name) {
				c.EStructuralFeatures += newEAttribute(name, ecoreref(MyDataType))
			}
			
			modifyEcore aTest epackage mypackage {
				myNewAttribute(addNewEClass("A"), "foo")
				ecoreref(''').
			assertProposal('foo')
	}

	@Test def void testQualifiedEcoreReferenceBeforeRemovalOfEClass() {
		'''
		metamodel "mypackage"
		modifyEcore aTest epackage mypackage {
			ecoreref(MyClass.«cursor»);
			EClassifiers -= ecoreref(MyClass)
		}'''.
			testContentAssistant(#['myAttribute', 'myReference'])
	}

	@Test def void testUnqualifiedEcoreReferenceAfterRemoval() {
		newBuilder.append('''
			metamodel "mypackage"
			modifyEcore aTest epackage mypackage { 
				EClassifiers -= ecoreref(MyBaseClass)
				ecoreref(''').
			assertText('''
				MyClass
				MyDataType
				MyDerivedClass
				myAttribute
				myDerivedAttribute
				myDerivedReference
				myReference
				mypackage
				'''.fromLinesOfStringsToStringArray)
		// MyBaseClass and its features myBaseAttribute and myBaseReference
		// are not proposed since they are not present anymore in this context
	}

	@Test def void testQualifiedEcoreReferenceAfterRemoval() {
		newBuilder.append('''
			metamodel "mypackage"
			modifyEcore aTest epackage mypackage { 
				ecoreref(MyBaseClass).EStructuralFeatures -= ecoreref(myBaseAttribute)
				ecoreref(MyBaseClass.''').
			assertText('''
				myBaseReference
				'''.fromLinesOfStringsToStringArray)
		// myBaseAttribute is not proposed since it's not present anymore in this context
	}

	@Test def void testUnqualifiedEcoreReferenceAfterRename() {
		newBuilder.append('''
			metamodel "mypackage"
			modifyEcore aTest epackage mypackage { 
				ecoreref(MyBaseClass).name = "Renamed"
				ecoreref(''').
			assertText('''
				MyClass
				MyDataType
				MyDerivedClass
				Renamed
				myAttribute
				myBaseAttribute
				myBaseReference
				myDerivedAttribute
				myDerivedReference
				myReference
				mypackage
				'''.fromLinesOfStringsToStringArray)
		// MyBaseClass is proposed with its new name Renamed
		// and its features myBaseAttribute and myBaseReference
		// are still proposed since they are still present in this context
	}

	def private fromLinesOfStringsToStringArray(CharSequence strings) {
		strings.toString.replaceAll("\r", "").split("\n")
	}

	private def void testContentAssistant(CharSequence text, List<String> expectedProposals) {
		val cursorPosition = text.toString.indexOf(cursor)
		val content = text.toString.replace(cursor, "")

		newBuilder.append(content).
		assertTextAtCursorPosition(cursorPosition, expectedProposals)
	}
}
