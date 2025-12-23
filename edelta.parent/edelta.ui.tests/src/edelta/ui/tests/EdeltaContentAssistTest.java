package edelta.ui.tests;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.xtext.resource.XtextResource;
import org.eclipse.xtext.testing.Flaky;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.XtextRunner;
import org.eclipse.xtext.ui.editor.XtextEditor;
import org.eclipse.xtext.ui.editor.utils.EditorUtils;
import org.eclipse.xtext.ui.testing.AbstractContentAssistTest;
import org.eclipse.xtext.ui.testing.util.IResourcesSetupUtil;
import org.eclipse.xtext.xbase.lib.Conversions;
import org.eclipse.xtext.xbase.lib.Exceptions;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import edelta.testutils.EdeltaTestUtils;
import edelta.ui.internal.EdeltaActivator;
import edelta.ui.tests.utils.EdeltaWorkbenchUtils;
import edelta.ui.testutils.EdeltaUiTestUtils;

/**
 * The tests rely on the ecore files in: /edelta.ui.tests.project/model/
 * 
 * @author Lorenzo Bettini
 */
@RunWith(XtextRunner.class)
@InjectWith(EdeltaUiInjectorProvider.class)
public class EdeltaContentAssistTest extends AbstractContentAssistTest {
	private static final String PROJECT_NAME = "edelta.ui.tests.project";

	@Rule
	public Flaky.Rule testRule = new Flaky.Rule();

	@BeforeClass
	public static void setUp() throws CoreException {
		try {
			EdeltaWorkbenchUtils.closeWelcomePage();
			javaProject = EdeltaUiTestUtils
					.importJavaProject("../" + EdeltaContentAssistTest.PROJECT_NAME);
			IResourcesSetupUtil.waitForBuild();
		} catch (Throwable e) {
			throw Exceptions.sneakyThrow(e);
		}
	}

	/**
	 * we need to close existing editors since we use the resource of the open
	 * editor to test the content assist so we must make sure we always use a
	 * freshly opened editor
	 */
	@After
	public void after() {
		EdeltaContentAssistTest.closeEditors();
	}

	/**
	 * We need a real resource otherwise the Ecores are not found (they are
	 * collected using visible containers).
	 * 
	 * IMPORTANT: use "\n" and NOT Strings.newLine because Java text blocks
	 * do not contain "\r" (not even in Windows)
	 */
	@Override
	public XtextResource getResourceFor(InputStream inputStream) {
		try {
			var result = new BufferedReader
				(new InputStreamReader(inputStream)).lines()
					.collect(Collectors.joining("\n"));
			var createdFile = IResourcesSetupUtil
				.createFile((PROJECT_NAME + "/src/Test.edelta"), result);
			IResourcesSetupUtil.waitForBuild();
			var editor = this.openEditor(createdFile);
			return editor.getDocument().readOnly(it -> it);
		} catch (Throwable e) {
			throw Exceptions.sneakyThrow(e);
		}
	}

	protected XtextEditor openEditor(IFile file) throws Exception {
		return EditorUtils.getXtextEditor
			(this.openEditor(file, EdeltaActivator.EDELTA_EDELTA));
	}

	private IEditorPart openEditor(IFile file, String editorId) throws Exception {
		return PlatformUI.getWorkbench()
			.getActiveWorkbenchWindow()
			.getActivePage().openEditor(new FileEditorInput(file), editorId);
	}

	public static void closeEditors() {
		PlatformUI.getWorkbench()
			.getActiveWorkbenchWindow()
			.getActivePage().closeAllEditors(false);
	}

	@Test
	public void testMetamodelsEcore() throws Exception {
		newBuilder().append("metamodel ").assertProposal("\"ecore\"");
	}

	@Test
	public void testMetamodelsLocalToProject() throws Exception {
		newBuilder().append("metamodel ").assertProposal("\"mypackage\"");
	}

	/**
	 * mainpackage.subpackage and mainpackage.subpackage.subsubpackage must not be
	 * proposed, since they are subpackages, which cannot be directly imported
	 */
	@Test
	public void testMetamodelsInThePresenceOfSubpackages() throws Exception {
		newBuilder().append("metamodel ")
			.assertText(
			fromLinesOfStringsToStringArray("""
				"annotation"
				"data"
				"ecore"
				"mainpackage"
				"mypackage"
				"namespace"
				"type"
				"""));
	}

	@Test
	public void testNoNSURIProposalMetamodels() throws Exception {
		newBuilder().append("metamodel <|>")
			.assertNoProposalAtCursor("\"http://my.package.org\"");
	}

	@Test
	public void testUnqualifiedEcoreReference() throws Exception {
		newBuilder().append(
			"""
			metamodel "mypackage"
			modifyEcore aTest epackage mypackage { 
				ecoreref(""")
			.assertText(
			fromLinesOfStringsToStringArray("""
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
			"""));
	}

	@Test
	public void testUnqualifiedEcoreReferenceInOperation() throws Exception {
		newBuilder().append(
			"""
			metamodel "mypackage"
			def anOp() {
				ecoreref(""")
			.assertText(
			fromLinesOfStringsToStringArray("""
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
				"""));
	}

	@Test
	@Flaky
	public void testUnqualifiedEcoreReferenceWithPrefix() throws Exception {
		newBuilder().append(
			"""
			metamodel "mypackage"
			modifyEcore aTest epackage mypackage { 
				ecoreref(myd""")
			.assertText(
			fromLinesOfStringsToStringArray("""
				MyDataType
				MyDerivedClass
				myDerivedAttribute
				myDerivedReference
				"""));
	}

	@Test
	@Flaky
	public void testUnqualifiedEcoreReferenceWithPrefixInOperation() throws Exception {
		newBuilder().append(
			"""
			metamodel "mypackage"
			def anOp() {
				ecoreref(myd""")
			.assertText(
			fromLinesOfStringsToStringArray("""
				MyDataType
				MyDerivedClass
				myDerivedAttribute
				myDerivedReference
				"""));
	}

	@Test
	@Flaky
	public void testQualifiedEcoreReference() throws Exception {
		newBuilder().append(
			"""
			metamodel "mypackage"
			modifyEcore aTest epackage mypackage {
				ecoreref(MyClass.""")
			.assertText("myAttribute", "myReference");
	}

	@Test
	@Flaky
	public void testQualifiedEcoreReferenceInOperation() throws Exception {
		newBuilder().append(
			"""
			metamodel "mypackage"
			def anOp() {
				ecoreref(MyClass.""")
			.assertText("myAttribute", "myReference");
	}

	@Test
	@Flaky
	public void testEClassifierAfterCreatingAnEClass() throws Exception {
		newBuilder().append(
			"""
			metamodel "mypackage"
			
			modifyEcore aTest epackage mypackage {
				addNewEClass("NewClass")
				ecoreref(<|>)
				""")
			.assertProposalAtCursor("NewClass");
	}

	@Test
	@Flaky
	public void testEClassifierAfterRenamingAnEClass() throws Exception {
		var proposalTester = newBuilder().append(
			"""
			metamodel "mypackage"
			
			modifyEcore aTest epackage mypackage {
				ecoreref(MyClass).name = "Renamed"
				ecoreref(<|>)
			""");
		proposalTester.assertProposalAtCursor("Renamed");
		proposalTester.assertNoProposalAtCursor("MyClass");
	}

	@Test
	@Flaky
	public void testCreatedEAttributeDuringInterpretationIsProposed() throws Exception {
		System.out.println("*** Executing testCreatedEAttributeDuringInterpretationIsProposed...");
		newBuilder().append(
			"""
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
				ecoreref(""")
			.assertProposal("foo");
	}

	@Test
	@Flaky
	public void testUnqualifiedEcoreReferenceBeforeRemovalOfEClass() throws Exception {
		testContentAssistant("""
			metamodel "mypackage"
			modifyEcore aTest epackage mypackage {
				ecoreref(<|>);
				EClassifiers -= ecoreref(MyClass)
			}
			""",
			fromLinesOfStringsToStringArray("""
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
				"""));
		// MyClass is still present in that context so it is proposed
	}

	@Test
	@Flaky
	public void testQualifiedEcoreReferenceBeforeRemovalOfEClass() throws Exception {
		testContentAssistant(
			"""
			metamodel "mypackage"
			modifyEcore aTest epackage mypackage {
				ecoreref(MyClass.<|>);
				EClassifiers -= ecoreref(MyClass)
			}
			""",
			List.of("myAttribute", "myReference"));
		// MyClass is still present in that context so its features are proposed
	}

	@Test
	@Flaky
	public void testQualifiedEcoreReferenceBeforeRemovalOfEStructuralFeature() throws Exception {
		testContentAssistant(
			"""
			metamodel "mypackage"
			modifyEcore aTest epackage mypackage {
				ecoreref(MyClass.<|>);
				ecoreref(MyClass).EStructuralFeatures -= ecoreref(myReference)
			}
			""",
			List.of("myAttribute", "myReference"));
		// myReference is still present in that context so it is proposed
	}

	@Test
	@Flaky
	public void testQualifiedEcoreReferenceBeforeAdditionOfEStructuralFeature() throws Exception {
		testContentAssistant(
			"""
			metamodel "mypackage"
			modifyEcore aTest epackage mypackage {
				ecoreref(MyClass.<|>);
				ecoreref(MyClass).addNewEAttribute("myNewAttribute", null)
			}
			""",
			List.of("myAttribute", "myReference"));
		// myNewAttribute is not yet present in that context so it is not proposed
	}

	@Test
	@Flaky
	public void testUnqualifiedEcoreReferenceAfterRemoval() throws Exception {
		newBuilder().append("""
			metamodel "mypackage"
			modifyEcore aTest epackage mypackage { 
				EClassifiers -= ecoreref(MyBaseClass)
				ecoreref(""")
			.assertText(fromLinesOfStringsToStringArray("""
				MyClass
				MyDataType
				MyDerivedClass
				myAttribute
				myDerivedAttribute
				myDerivedReference
				myReference
				mypackage
				"""));
		// MyBaseClass and its features myBaseAttribute and myBaseReference
		// are not proposed since they are not present anymore in this context
	}

	@Test
	@Flaky
	public void testQualifiedEcoreReferenceAfterRemovalOfEStructuralFeature() throws Exception {
		newBuilder().append("""
			metamodel "mypackage"
			modifyEcore aTest epackage mypackage { 
				ecoreref(MyBaseClass).EStructuralFeatures -= ecoreref(myBaseAttribute)
				ecoreref(MyBaseClass.""")
			.assertText(fromLinesOfStringsToStringArray("""
				myBaseReference
				"""));
		// myBaseAttribute is not proposed since it's not present anymore in this context
	}

	@Test
	@Flaky
	public void testQualifiedEcoreReferenceAfterAdditionOfEStructuralFeature() throws Exception {
		testContentAssistant("""
			metamodel "mypackage"
			modifyEcore aTest epackage mypackage {
				ecoreref(MyClass).addNewEAttribute("myNewAttribute", null)
				ecoreref(MyClass.<|>);
			}
			""",
			List.of("myAttribute", "myReference", "myNewAttribute"));
		// myNewAttribute is now present in that context so it is proposed
	}

	@Test
	@Flaky
	public void testUnqualifiedEcoreReferenceBeforeRename() throws Exception {
		testContentAssistant("""
			metamodel "mypackage"
			modifyEcore aTest epackage mypackage {
				ecoreref(<|>)
				ecoreref(MyBaseClass).name = "Renamed"
			}
			""",
			fromLinesOfStringsToStringArray("""
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
				"""));
		// MyBaseClass is not yet renamed in this context
	}

	@Test
	@Flaky
	public void testUnqualifiedEcoreReferenceAfterRename() throws Exception {
		newBuilder().append("""
			metamodel "mypackage"
			modifyEcore aTest epackage mypackage { 
				ecoreref(MyBaseClass).name = "Renamed"
				ecoreref(""")
			.assertText(
			fromLinesOfStringsToStringArray("""
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
				"""));
		// MyBaseClass is proposed with its new name Renamed
		// and its features myBaseAttribute and myBaseReference
		// are still proposed since they are still present in this context
	}

	@Test
	@Flaky
	public void testForAmbiguousReferencesFullyQualifiedNameIsProposed() throws Exception {
		newBuilder().append("""
			metamodel "mainpackage"
			
			modifyEcore aTest epackage mainpackage {
				ecoreref(My""")
			.assertText(
			fromLinesOfStringsToStringArray("""
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
				"""));
	}

	@Test
	@Flaky
	public void testForAmbiguousReferencesFullyQualifiedNameIsReplaced() throws Exception {
		newBuilder().append("""
			metamodel "mainpackage"
			
			modifyEcore aTest epackage mainpackage {
				ecoreref(My""")
		.applyProposal("mainpackage.subpackage.MyClass.myAttribute")
		.expectContent("""
			metamodel "mainpackage"
			
			modifyEcore aTest epackage mainpackage {
				ecoreref(mainpackage.subpackage.MyClass.myAttribute""");
	}

	@Test
	@Flaky
	public void testForAmbiguousReferencesFullyQualifiedNameIsProposedInOperation() throws Exception {
		newBuilder().append("""
			metamodel "mainpackage"
			
			def anOp() {
				ecoreref(My""")
		.assertText(
		fromLinesOfStringsToStringArray("""
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
			"""));
	}

	@Test
	@Flaky
	public void testForAmbiguousReferencesFullyQualifiedNameIsReplacedInOperation() throws Exception {
		newBuilder().append("""
			metamodel "mainpackage"
			
			def anOp() {
				ecoreref(My""")
		.applyProposal("mainpackage.subpackage.MyClass.myAttribute")
		.expectContent("""
			metamodel "mainpackage"
			
			def anOp() {
				ecoreref(mainpackage.subpackage.MyClass.myAttribute""");
	}

	@Test
	public void testProposalForMigrationToNsUri() throws Exception {
		newBuilder().append("migrate \"http://my.package.org\" to ")
			.assertText("\"http://my.package.org\"");
	}

	private String[] fromLinesOfStringsToStringArray(CharSequence strings) {
		return EdeltaTestUtils.removeCR(strings.toString()).split("\n");
	}

	private void testContentAssistant(CharSequence text, List<String> expectedProposals) throws Exception {
		testContentAssistant(text, 
			((String[]) Conversions.unwrapArray(expectedProposals, String.class)));
	}

	private void testContentAssistant(CharSequence text, String[] expectedProposals) throws Exception {
		var cursorPosition = text.toString().indexOf("<|>");
		var content = text.toString().replace("<|>", "");
		newBuilder().append(content)
			.assertTextAtCursorPosition(cursorPosition,
					expectedProposals);
	}

}
