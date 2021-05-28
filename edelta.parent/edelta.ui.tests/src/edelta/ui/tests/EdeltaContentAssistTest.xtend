package edelta.ui.tests

import edelta.ui.internal.EdeltaActivator
import edelta.ui.tests.utils.EdeltaWorkbenchUtils
import edelta.ui.tests.utils.ProjectImportUtil
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.util.List
import java.util.stream.Collectors
import org.eclipse.core.resources.IFile
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
import org.eclipse.xtext.util.Strings
import org.junit.After
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

import static org.eclipse.xtext.ui.testing.util.IResourcesSetupUtil.*
import org.eclipse.core.runtime.CoreException

/**
 * The tests rely on the ecore files in:
 * /edelta.ui.tests.project/model/
 * 
 * @author Lorenzo Bettini
 */
@RunWith(XtextRunner)
@InjectWith(EdeltaUiInjectorProvider)
class EdeltaContentAssistTest extends AbstractContentAssistTest {

	static IJavaProject pluginJavaProject
	static String PROJECT_NAME = "edelta.ui.tests.project"

	@Rule
	public Flaky.Rule testRule = new Flaky.Rule();

	@BeforeClass
	def static void setUp() throws CoreException {
		EdeltaWorkbenchUtils.closeWelcomePage
		pluginJavaProject = ProjectImportUtil.importJavaProject(PROJECT_NAME)
		waitForBuild
	}

	/**
	 * just to make sure the project is not deleted
	 */
	@AfterClass
	def static void tearDown() {
	}

	/**
	 * we need to close existing editors since we use the
	 * resource of the open editor to test the content assist
	 * so we must make sure we always use a freshly opened editor
	 */
	@After
	def void after() {
		closeEditors
	}

	override getJavaProject(ResourceSet resourceSet) {
		pluginJavaProject
	}

	/**
	 * We need a real resource otherwise the Ecores are not found
	 * (they are collected using visible containers).
	 * 
	 * IMPORTANT: use Strings.newLine to avoid problems with missing \r in Windows
	 */
	override getResourceFor(InputStream inputStream) {
		val result = new BufferedReader(new InputStreamReader(inputStream)).
			lines().collect(Collectors.joining(Strings.newLine()));
		val createdFile = createFile(
			PROJECT_NAME+"/src/Test.edelta",
			result
		)
		waitForBuild
		val editor = openEditor(
			createdFile
		)
		return editor.document.readOnly[it]
	}

	def protected XtextEditor openEditor(IFile file) throws Exception {
		val openEditor = openEditor(file, EdeltaActivator.EDELTA_EDELTA);
		return EditorUtils.getXtextEditor(openEditor);
	}

	def private IEditorPart openEditor(IFile file, String editorId) throws Exception {
		return PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().openEditor(
				new FileEditorInput(file), editorId);
	}

	def static void closeEditors() {
		PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().closeAllEditors(false);
	}

	@Test def void testMetamodelsEcore() throws Exception {
		newBuilder.append("metamodel ").assertProposal('"ecore"')
	}

	@Test def void testMetamodelsLocalToProject() throws Exception {
		newBuilder.append("metamodel ").assertProposal('"mypackage"')
	}

	/**
	 * mainpackage.subpackage and mainpackage.subpackage.subsubpackage
	 * must not be proposed, since they are subpackages,
	 * which cannot be directly imported
	 */
	@Test def void testMetamodelsInThePresenceOfSubpackages() throws Exception {
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


	@Test def void testNoNSURIProposalMetamodels() throws Exception {
		newBuilder.append("metamodel <|>").assertNoProposalAtCursor('"http://my.package.org"')
	}

	@Test def void testUnqualifiedEcoreReference() throws Exception {
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

	@Test def void testUnqualifiedEcoreReferenceInOperation() throws Exception {
		newBuilder.append('''
			metamodel "mypackage"
			def anOp() {
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

	@Test @Flaky
	def void testUnqualifiedEcoreReferenceWithPrefix() throws Exception {
		newBuilder.append('''
			metamodel "mypackage"
			modifyEcore aTest epackage mypackage { 
				ecoreref(myd''').
			assertText('''
				MyDataType
				MyDerivedClass
				myDerivedAttribute
				myDerivedReference
				'''.fromLinesOfStringsToStringArray)
	}

	@Test @Flaky
	def void testUnqualifiedEcoreReferenceWithPrefixInOperation() throws Exception {
		newBuilder.append('''
			metamodel "mypackage"
			def anOp() {
				ecoreref(myd''').
			assertText('''
				MyDataType
				MyDerivedClass
				myDerivedAttribute
				myDerivedReference
				'''.fromLinesOfStringsToStringArray)
	}

	@Test @Flaky
	def void testQualifiedEcoreReference() throws Exception {
		newBuilder.append('''
			metamodel "mypackage"
			modifyEcore aTest epackage mypackage {
				ecoreref(MyClass.''').
			assertText('myAttribute', 'myReference')
	}

	@Test @Flaky
	def void testQualifiedEcoreReferenceInOperation() throws Exception {
		newBuilder.append('''
			metamodel "mypackage"
			def anOp() {
				ecoreref(MyClass.''').
			assertText('myAttribute', 'myReference')
	}

	@Test @Flaky
	def void testEClassifierAfterCreatingAnEClass() throws Exception {
		newBuilder.append('''
			metamodel "mypackage"
			
			modifyEcore aTest epackage mypackage {
				addNewEClass("AAA")
				ecoreref(''').
			assertProposal('AAA')
	}

	@Test @Flaky
	def void testEClassifierAfterRenamingAnEClass() throws Exception {
		newBuilder.append('''
			metamodel "mypackage"
			
			modifyEcore aTest epackage mypackage {
				ecoreref(MyClass).name = "Renamed"
				ecoreref(''').
			assertProposal('Renamed')
	}

	@Test @Flaky
	def void testCreatedEAttributeDuringInterpretationIsProposed() throws Exception {
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

	@Test @Flaky
	def void testUnqualifiedEcoreReferenceBeforeRemovalOfEClass() throws Exception {
		'''
		metamodel "mypackage"
		modifyEcore aTest epackage mypackage {
			ecoreref(<|>);
			EClassifiers -= ecoreref(MyClass)
		}'''.
			testContentAssistant(
				'''
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
				'''.fromLinesOfStringsToStringArray
			)
		// MyClass is still present in that context so it is proposed
	}

	@Test @Flaky
	def void testQualifiedEcoreReferenceBeforeRemovalOfEClass() throws Exception {
		'''
		metamodel "mypackage"
		modifyEcore aTest epackage mypackage {
			ecoreref(MyClass.<|>);
			EClassifiers -= ecoreref(MyClass)
		}'''.
			testContentAssistant(List.of('myAttribute', 'myReference'))
		// MyClass is still present in that context so its features are proposed
	}

	@Test @Flaky
	def void testQualifiedEcoreReferenceBeforeRemovalOfEStructuralFeature() throws Exception {
		'''
		metamodel "mypackage"
		modifyEcore aTest epackage mypackage {
			ecoreref(MyClass.<|>);
			ecoreref(MyClass).EStructuralFeatures -= ecoreref(myReference)
		}'''.
			testContentAssistant(List.of('myAttribute', 'myReference'))
		// myReference is still present in that context so it is proposed
	}

	@Test @Flaky
	def void testQualifiedEcoreReferenceBeforeAdditionOfEStructuralFeature() throws Exception {
		'''
		metamodel "mypackage"
		modifyEcore aTest epackage mypackage {
			ecoreref(MyClass.<|>);
			ecoreref(MyClass).addNewEAttribute("myNewAttribute", null)
		}'''.
			testContentAssistant(List.of('myAttribute', 'myReference'))
		// myNewAttribute is not yet present in that context so it is not proposed
	}

	@Test @Flaky
	def void testUnqualifiedEcoreReferenceAfterRemoval() throws Exception {
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

	@Test @Flaky
	def void testQualifiedEcoreReferenceAfterRemovalOfEStructuralFeature() throws Exception {
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

	@Test @Flaky
	def void testQualifiedEcoreReferenceAfterAdditionOfEStructuralFeature() throws Exception {
		'''
		metamodel "mypackage"
		modifyEcore aTest epackage mypackage {
			ecoreref(MyClass).addNewEAttribute("myNewAttribute", null)
			ecoreref(MyClass.<|>);
		}'''.
			testContentAssistant(List.of('myAttribute', 'myReference', "myNewAttribute"))
		// myNewAttribute is now present in that context so it is proposed
	}

	@Test @Flaky
	def void testUnqualifiedEcoreReferenceBeforeRename() throws Exception {
		'''
		metamodel "mypackage"
		modifyEcore aTest epackage mypackage {
			ecoreref(<|>)
			ecoreref(MyBaseClass).name = "Renamed"
		}'''.
			testContentAssistant(
				'''
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
				'''.fromLinesOfStringsToStringArray
			)
		// MyBaseClass is not yet renamed in this context
	}

	@Test @Flaky
	def void testUnqualifiedEcoreReferenceAfterRename() throws Exception {
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

	@Test @Flaky
	def void testForAmbiguousReferencesFullyQualifiedNameIsProposed() throws Exception {
		newBuilder.append('''
			metamodel "mainpackage"
			
			modifyEcore aTest epackage mainpackage {
				ecoreref(My''')
			.assertText(
				'''
				MySubPackageClass
				mainpackage.MyClass
				mainpackage.MyClass.myAttribute
				mainpackage.MyClass.myReference
				mainpackage.subpackage.MyClass
				mainpackage.subpackage.MyClass.myAttribute
				mainpackage.subpackage.MyClass.myReference
				mainpackage.subpackage.subsubpackage.MyClass
				mainpackage.subpackage.subsubpackage.MyClass.myAttribute
				mainpackage.subpackage.subsubpackage.MyClass.myReference
				'''.fromLinesOfStringsToStringArray)
	}

	@Test @Flaky
	def void testForAmbiguousReferencesFullyQualifiedNameIsReplaced() throws Exception {
		newBuilder.append('''
			metamodel "mainpackage"
			
			modifyEcore aTest epackage mainpackage {
				ecoreref(My''')
			.applyProposal("mainpackage.subpackage.MyClass.myAttribute")
			.expectContent('''
			metamodel "mainpackage"
			
			modifyEcore aTest epackage mainpackage {
				ecoreref(mainpackage.subpackage.MyClass.myAttribute''')
	}

	@Test @Flaky
	def void testForAmbiguousReferencesFullyQualifiedNameIsProposedInOperation() throws Exception {
		newBuilder.append('''
			metamodel "mainpackage"
			
			def anOp() {
				ecoreref(My''')
			.assertText(
				'''
				MySubPackageClass
				mainpackage.MyClass
				mainpackage.MyClass.myAttribute
				mainpackage.MyClass.myReference
				mainpackage.subpackage.MyClass
				mainpackage.subpackage.MyClass.myAttribute
				mainpackage.subpackage.MyClass.myReference
				mainpackage.subpackage.subsubpackage.MyClass
				mainpackage.subpackage.subsubpackage.MyClass.myAttribute
				mainpackage.subpackage.subsubpackage.MyClass.myReference
				'''.fromLinesOfStringsToStringArray)
	}

	@Test @Flaky
	def void testForAmbiguousReferencesFullyQualifiedNameIsReplacedInOperation() throws Exception {
		newBuilder.append('''
			metamodel "mainpackage"
			
			def anOp() {
				ecoreref(My''')
			.applyProposal("mainpackage.subpackage.MyClass.myAttribute")
			.expectContent('''
			metamodel "mainpackage"
			
			def anOp() {
				ecoreref(mainpackage.subpackage.MyClass.myAttribute''')
	}

	def private fromLinesOfStringsToStringArray(CharSequence strings) {
		strings.toString.replace("\r", "").split("\n")
	}

	private def void testContentAssistant(CharSequence text, List<String> expectedProposals) throws Exception {
		val cursorPosition = text.toString.indexOf("<|>")
		val content = text.toString.replace("<|>", "")

		newBuilder.append(content).
		assertTextAtCursorPosition(cursorPosition, expectedProposals)
	}
}
