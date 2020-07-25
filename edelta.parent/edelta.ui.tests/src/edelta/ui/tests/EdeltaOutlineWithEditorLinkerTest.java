package edelta.ui.tests;

import static org.eclipse.xtext.ui.testing.util.IResourcesSetupUtil.createFile;
import static org.eclipse.xtext.ui.testing.util.IResourcesSetupUtil.waitForBuild;

import org.assertj.core.api.Assertions;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.XtextRunner;
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

	@Override
	protected String getEditorId() {
		return EdeltaActivator.EDELTA_EDELTA;
	}

	@Override
	public void setUp() throws Exception {
		super.setUp();
		edeltaProjectHelper.createEdeltaPluginProject(TEST_PROJECT);
	}

	@Test
	public void testValidProject() throws Exception {
		final var program = "package foo\n" + 
		"			\n" + 
		"			metamodel \"mypackage\"\n" + 
		"			\n" + 
		"			modifyEcore aTest epackage mypackage {\n" + 
		"				ecoreref(MyClass)\n" + 
		"			}";
		var file = createFile(
			TEST_PROJECT + "/src/Test.edelta",
			program
		);
		// we need to wait for build twice when we run all the UI tests
		waitForBuild();
		var editor = openEditor(file);
		// the ContentOutline must not be active,
		// otherwise the selection change event is ignored by OutlineWithEditorLinker
		// var outlinePart = editor.getEditorSite().getPage().showView("org.eclipse.ui.views.ContentOutline");
		var outlinePage = editor.getAdapter(OutlinePage.class);
		Assertions.assertThat(outlinePage).isNotNull();
		editor.setFocus();
		editor.getInternalSourceViewer().setSelectedRange(program.indexOf("modifyEcore"), 0);
		Thread.sleep(2000);
		executeAsyncDisplayJobs();
		var selection = (TreeSelection) outlinePage.getTreeViewer().getSelection();
		Assertions.assertThat(selection.isEmpty()).isFalse();
		assertEquals("aTest(EPackage) : void",
			((EObjectNode) selection.getFirstElement()).getText().toString());
	}

	private void executeAsyncDisplayJobs() {
		while(Display.getCurrent().readAndDispatch()) {
		}
	}
}
