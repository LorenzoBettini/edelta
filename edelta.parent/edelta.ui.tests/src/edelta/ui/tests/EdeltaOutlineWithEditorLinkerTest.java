package edelta.ui.tests;

import static org.eclipse.xtext.ui.testing.util.IResourcesSetupUtil.*;

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.assertj.core.api.Assertions;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.XtextRunner;
import org.eclipse.xtext.ui.editor.XtextEditor;
import org.eclipse.xtext.ui.editor.outline.impl.EObjectNode;
import org.eclipse.xtext.ui.editor.outline.impl.OutlinePage;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import edelta.ui.internal.EdeltaActivator;
import edelta.ui.testutils.EdeltaUiTestUtils;

@RunWith(XtextRunner.class)
@InjectWith(EdeltaUiInjectorProvider.class)
public class EdeltaOutlineWithEditorLinkerTest extends CustomAbstractEditorTest {

	private static final String TEST_PROJECT = "edelta.ui.tests.project";

	private static String program = """
			package foo
			
			metamodel "mypackage"
			
			def anOp() {
			}
			
			modifyEcore aTest epackage mypackage {
				addNewEClass("MyNewClass") [
					addNewEAttribute("MyNewAttribute", null)
				]
				ecoreref(MyClass)
				ecoreref(MyDerivedClass) => [
					addNewEAttribute("MyNewDerivedClassAttribute", null)
				]
			}
			""";

	private XtextEditor editor;

	private OutlinePage outlinePage;

	private static IFile file;

	@Override
	protected String getEditorId() {
		return EdeltaActivator.EDELTA_EDELTA;
	}

	@BeforeClass
	public static void setupTestProject() throws InvocationTargetException, CoreException, InterruptedException {
		EdeltaUiTestUtils.importJavaProject("../" + TEST_PROJECT);
		file = createFile(
			TEST_PROJECT + "/src/Test.edelta",
			program
		);
		waitForBuild();
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

	@Test
	public void testSelectOperation() {
		whenEditorTextIsSelectedThenOutlineNodeIsSelected
			("anOp", "anOp() : Object");
	}

	@Test
	public void testSelectModifyEcore() {
		whenEditorTextIsSelectedThenOutlineNodeIsSelected
			("modifyEcore", "aTest(EPackage) : void");
	}

	@Test
	public void testSelectNonResponsibleExpressionInModifyEcore() {
		whenEditorTextIsSelectedThenOutlineNodeIsSelected
			("ecoreref(MyClass)", "aTest(EPackage) : void");
	}

	@Test
	public void testSelectExpressionThatCreatesEClass() {
		whenEditorTextIsSelectedThenOutlineNodeIsSelected
			("addNewEClass", "MyNewClass");
	}

	@Test
	public void testSelectExpressionThatCreatesEAttributeInCreatedEClass() {
		whenEditorTextIsSelectedThenOutlineNodeIsSelected
			("addNewEAttribute(\"MyNewAttribute", "MyNewAttribute");
	}

	@Test
	public void testSelectExpressionThatCreatesEAttributeInExistingEClass() {
		whenEditorTextIsSelectedThenOutlineNodeIsSelected
			("addNewEAttribute(\"MyNewDerivedClassAttribute", "MyNewDerivedClassAttribute");
	}

	private void whenEditorTextIsSelectedThenOutlineNodeIsSelected(String textToSelect, String expectedNode) {
		editor.getInternalSourceViewer()
			.setSelectedRange(program.indexOf(textToSelect), 0);
		Assertions.assertThat(waitForSelection(expectedNode))
			.as("Outline selection after selecting '%s'", textToSelect)
			.isEqualTo(expectedNode);
	}

	/** Processes UI events until the expected node is selected or the deadline expires. */
	private String waitForSelection(final String expectedNode) {
		var display = outlinePage.getTreeViewer().getControl().getDisplay();
		var timedOut = new AtomicBoolean();
		Runnable deadline = () -> timedOut.set(true);
		display.timerExec(3000, deadline);
		try {
			var actualNode = selectedNodeText();
			while (!expectedNode.equals(actualNode) && !timedOut.get()) {
				if (!display.readAndDispatch() && !timedOut.get()) {
					display.sleep();
				}
				actualNode = selectedNodeText();
			}
			return actualNode;
		} finally {
			display.timerExec(-1, deadline);
		}
	}

	/** Returns the selected Ecore node's label, or null while no such node is selected. */
	private String selectedNodeText() {
		var selection = (TreeSelection) outlinePage.getTreeViewer().getSelection();
		return selection.getFirstElement() instanceof EObjectNode node
			? node.getText().toString() : null;
	}

}
