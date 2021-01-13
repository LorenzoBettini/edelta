package edelta.ui.tests;

import static org.eclipse.xtext.ui.testing.util.IResourcesSetupUtil.*;

import org.assertj.core.api.Assertions;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.XtextRunner;
import org.eclipse.xtext.ui.editor.XtextEditor;
import org.eclipse.xtext.ui.editor.outline.impl.EObjectNode;
import org.eclipse.xtext.ui.editor.outline.impl.OutlinePage;
import org.eclipse.xtext.ui.testing.AbstractEditorTest;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.inject.Inject;

import edelta.ui.internal.EdeltaActivator;
import edelta.ui.tests.utils.EdeltaPluginProjectHelper;

@RunWith(XtextRunner.class)
@InjectWith(EdeltaUiInjectorProvider.class)
public class EdeltaOutlineWithEditorLinkerTest extends AbstractEditorTest {

	@Inject
	private EdeltaPluginProjectHelper edeltaProjectHelper;

	private static final String TEST_PROJECT = "mytestproject";

	private String program = """
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

	@Override
	protected String getEditorId() {
		return EdeltaActivator.EDELTA_EDELTA;
	}

	@Override
	public void setUp() throws Exception {
		super.setUp();
		edeltaProjectHelper.createEdeltaPluginProject(TEST_PROJECT);
		var file = createFile(
			TEST_PROJECT + "/src/Test.edelta",
			program
		);
		// we need to wait for build twice when we run all the UI tests
		// don't call waitForBuild because in case of an exception
		// we want the full stack trace:
		// this test seems to flaky in the CI
		try {
			ResourcesPlugin
				.getWorkspace()
				.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, null);
		} catch (CoreException e) {
			e.printStackTrace();
			throw e;
		}
		editor = openEditor(file);
		outlinePage = editor.getAdapter(OutlinePage.class);
		Assertions.assertThat(outlinePage).isNotNull();
		editor.setFocus();
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
			executeAsyncDisplayJobs();
			var selection = (TreeSelection) outlinePage.getTreeViewer().getSelection();
			if (!selection.isEmpty())
				return selection;
		}
		fail("No node is selected in the outline");
		return null;
	}

	private void executeAsyncDisplayJobs() {
		while(Display.getCurrent().readAndDispatch()) {
		}
	}
}
