package edelta.ui.tests;

import static org.eclipse.xtext.ui.testing.util.IResourcesSetupUtil.createFile;
import static org.eclipse.xtext.ui.testing.util.IResourcesSetupUtil.waitForBuild;

import org.assertj.core.api.Assertions;
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

	private String program = 
	"package foo\n" + 
	"\n" + 
	"metamodel \"mypackage\"\n" + 
	"\n" + 
	"def anOp() {\n" + 
	"}\n" + 
	"\n" + 
	"modifyEcore aTest epackage mypackage {\n" + 
	"	addNewEClass(\"MyNewClass\")\n" + 
	"	ecoreref(MyClass)\n" + 
	"}";

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
		waitForBuild();
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
	public void testSelectExpressionThatCreatesEClass() throws Exception {
		whenEditorTextIsSelectedThenOutlineNodeIsSelected
			("addNewEClass", "MyNewClass");
	}

	private void whenEditorTextIsSelectedThenOutlineNodeIsSelected(String textToSelect, String expectedNode) throws InterruptedException {
		editor.getInternalSourceViewer()
			.setSelectedRange(program.indexOf(textToSelect), 0);
		var selection = waitForSelection();
		assertEquals(expectedNode,
			((EObjectNode) selection.getFirstElement()).getText().toString());
	}

	@SuppressWarnings("all")
	private TreeSelection waitForSelection() throws InterruptedException {
		int attempts = 6;
		for (int i = 0; i < attempts; ++i) {
			executeAsyncDisplayJobs();
			var selection = (TreeSelection) outlinePage.getTreeViewer().getSelection();
			if (!selection.isEmpty())
				return selection;
			Thread.sleep(500);
		}
		fail("No node is selected in the outline");
		return null;
	}

	private void executeAsyncDisplayJobs() {
		while(Display.getCurrent().readAndDispatch()) {
		}
	}
}
