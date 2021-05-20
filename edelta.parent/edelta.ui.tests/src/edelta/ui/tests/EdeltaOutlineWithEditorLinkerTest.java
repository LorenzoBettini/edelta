package edelta.ui.tests;

import static org.eclipse.xtext.ui.testing.util.IResourcesSetupUtil.createFile;

import java.lang.reflect.InvocationTargetException;

import org.assertj.core.api.Assertions;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.XtextRunner;
import org.eclipse.xtext.ui.editor.XtextEditor;
import org.eclipse.xtext.ui.editor.outline.impl.EObjectNode;
import org.eclipse.xtext.ui.editor.outline.impl.OutlinePage;
import org.eclipse.xtext.ui.testing.AbstractEditorTest;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import edelta.ui.internal.EdeltaActivator;
import edelta.ui.tests.utils.EdeltaWorkbenchUtils;
import edelta.ui.tests.utils.ProjectImportUtil;

@RunWith(XtextRunner.class)
@InjectWith(EdeltaUiInjectorProvider.class)
public class EdeltaOutlineWithEditorLinkerTest extends AbstractEditorTest {

	private static final String TEST_PROJECT = "edelta.ui.tests.project";

	private static String program = 
	"package foo\n" + 
	"\n" + 
	"metamodel \"mypackage\"\n" + 
	"\n" + 
	"def anOp() {\n" + 
	"}\n" + 
	"\n" + 
	"modifyEcore aTest epackage mypackage {\n" + 
	"	addNewEClass(\"MyNewClass\") [\n" + 
	"		addNewEAttribute(\"MyNewAttribute\", null)\n" + 
	"	]\n" + 
	"	ecoreref(MyClass)\n" + 
	"	ecoreref(MyDerivedClass) => [\n" + 
	"		addNewEAttribute(\"MyNewDerivedClassAttribute\", null)\n" + 
	"	]\n" + 
	"}";

	private XtextEditor editor;

	private OutlinePage outlinePage;

	private static IFile file;

	@Override
	protected String getEditorId() {
		return EdeltaActivator.EDELTA_EDELTA;
	}

	@BeforeClass
	public static void setupTestProject() throws InvocationTargetException, CoreException, InterruptedException {
		ProjectImportUtil.importJavaProject(TEST_PROJECT);
		file = createFile(
			TEST_PROJECT + "/src/Test.edelta",
			program
		);
		EdeltaWorkbenchUtils.waitForBuildWithRetries();
	}

	/**
	 * Avoids deleting project
	 */
	@Override
	public void setUp() throws Exception {
		editor = openEditor(file);
		outlinePage = editor.getAdapter(OutlinePage.class);
		Assertions.assertThat(outlinePage).isNotNull();
		editor.setFocus();
	}

	/**
	 * Avoids deleting project
	 */
	@Override
	public void tearDown() {
		waitForEventProcessing();
		closeEditors();
		waitForEventProcessing();
	}

	@Test
	public void testSelectOperation() throws Exception {
		whenEditorTextIsSelectedThenOutlineNodeIsSelected
			("anOp", "anOp() : Object");
	}

	@Test
	public void testSelectModifyEcore() throws Exception {
		whenEditorTextIsSelectedThenOutlineNodeIsSelected
			("modifyEcore", "aTest(EPackage) : void");
	}

	@Test
	public void testSelectNonResponsibleExpressionInModifyEcore() throws Exception {
		whenEditorTextIsSelectedThenOutlineNodeIsSelected
			("ecoreref(MyClass)", "aTest(EPackage) : void");
	}

	@Test
	public void testSelectExpressionThatCreatesEClass() throws Exception {
		whenEditorTextIsSelectedThenOutlineNodeIsSelected
			("addNewEClass", "MyNewClass");
	}

	@Test
	public void testSelectExpressionThatCreatesEAttributeInCreatedEClass() throws Exception {
		whenEditorTextIsSelectedThenOutlineNodeIsSelected
			("addNewEAttribute(\"MyNewAttribute", "MyNewAttribute");
	}

	@Test
	public void testSelectExpressionThatCreatesEAttributeInExistingEClass() throws Exception {
		whenEditorTextIsSelectedThenOutlineNodeIsSelected
			("addNewEAttribute(\"MyNewDerivedClassAttribute", "MyNewDerivedClassAttribute");
	}

	private void whenEditorTextIsSelectedThenOutlineNodeIsSelected(String textToSelect, String expectedNode) {
		editor.getInternalSourceViewer()
			.setSelectedRange(program.indexOf(textToSelect), 0);
		var selection = waitForSelection();
		assertEquals(expectedNode,
			((EObjectNode) selection.getFirstElement()).getText().toString());
	}

	@SuppressWarnings("all")
	private TreeSelection waitForSelection() {
		int attempts = 30;
		for (int i = 0; i < attempts; ++i) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				System.err.println("Interrupted while waiting for selection");
			}
			waitForEventProcessing();
			var selection = (TreeSelection) outlinePage.getTreeViewer().getSelection();
			if (!selection.isEmpty())
				return selection;
		}
		fail("No node is selected in the outline");
		return null;
	}

}
