package edelta.ui.tests

import edelta.ui.internal.EdeltaActivator
import edelta.ui.tests.utils.PDETargetPlatformUtils
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

import static edelta.ui.tests.utils.EdeltaPluginProjectHelper.*
import static org.eclipse.xtext.junit4.ui.util.IResourcesSetupUtil.*
import edelta.ui.tests.utils.EdeltaPluginProjectHelper

@RunWith(XtextRunner)
@InjectWith(EdeltaUiInjectorProvider)
class EdeltaContentAssistTest extends AbstractContentAssistTest {

	static IJavaProject pluginJavaProject

	@BeforeClass
	def static void setUp() {
		// needed when building with Tycho, otherwise, dependencies
		// in the MANIFEST of the created project will not be visible
		PDETargetPlatformUtils.setTargetPlatform();

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

	@Test def void testNoNSURIProposalMetamodels() {
		newBuilder.append("metamodel <|>").assertNoProposalAtCursor('"http://my.package.org"')
	}

	@Test def void testUnqualifiedEcoreReference() {
		newBuilder.append('metamodel "mypackage" ecoreref(').
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
		newBuilder.append('metamodel "mypackage" ecoreref(MyClass.').
			assertText('myAttribute', 'myReference')
	}

	@Test def void testEClassifierAfterCreatingAnEClass() {
		newBuilder.append('''
			metamodel "mypackage"
			
			createEClass AAA in mypackage
			
			ecoreref(
			''').
			assertProposal('AAA')
	}

	def private fromLinesOfStringsToStringArray(CharSequence strings) {
		strings.toString.replaceAll("\r", "").split("\n")
	}
}
