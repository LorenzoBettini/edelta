package edelta.ui.tests;

import static org.eclipse.xtext.ui.testing.util.IResourcesSetupUtil.*;

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.assertj.core.api.Assertions;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.XtextRunner;
import org.eclipse.xtext.ui.editor.XtextEditor;
import org.eclipse.xtext.ui.editor.outline.IOutlineNode;
import org.eclipse.xtext.ui.editor.outline.impl.OutlineNodeElementOpener;
import org.eclipse.xtext.ui.editor.outline.impl.OutlinePage;
import org.eclipse.xtext.ui.editor.outline.actions.OutlineWithEditorLinker;
import org.eclipse.xtext.util.TextRegion;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.inject.Inject;
import com.google.inject.Injector;

import edelta.ui.internal.EdeltaActivator;
import edelta.ui.outline.EdeltaOutlineTreeProvider.EcoreOutlineNode;
import edelta.ui.outline.EdeltaOutlineTreeProvider.EcorePropertyOutlineNode;
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
					addNewEReference("publications", ecoreref(MyClass)) [
						upperBound = -1
					]
				]
				ecoreref(MyClass).addNewEReference("referencesNewClass", ecoreref(MyNewClass))
				ecoreref(MyNewClass).addNewEReference("backReference", ecoreref(MyClass)) [
					EOpposite = ecoreref(MyClass.referencesNewClass)
				]
				ecoreref(MyClass.referencesNewClass).EOpposite = ecoreref(MyNewClass.backReference)
				ecoreref(MyClass)
				ecoreref(MyDerivedClass) => [
					addNewEAttribute("MyNewDerivedClassAttribute", null)
				]
				ecoreref(MyClass).name = "Publication"
				addNewEClass("First"); addNewEClass("Second")
			}
			""";

	private XtextEditor editor;

	private OutlinePage outlinePage;

	private static IFile file;

	@Inject
	private OutlineNodeElementOpener elementOpener;

	@Inject
	private Injector injector;

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
		whenEditorOffsetIsSelectedThenOutlineNodeIsSelected(
			program.indexOf("ecoreref(MyClass)\n"),
			"aTest(EPackage) : void");
	}

	@Test
	public void testSelectExpressionThatCreatesEClass() {
		whenEditorTextIsSelectedThenOutlineNodeIsSelected
			("addNewEClass", "MyNewClass");
	}

	@Test
	public void testSelectCreatedClassFromIndentationAndAfterExpression() {
		var expression = program.indexOf("addNewEClass(\"First\")");
		whenEditorOffsetIsSelectedThenOutlineNodeIsSelected(expression - 1, "First");
		var second = program.indexOf("addNewEClass(\"Second\")");
		whenEditorOffsetIsSelectedThenOutlineNodeIsSelected(
			second + "addNewEClass(\"Second\")".length(), "Second");
	}

	@Test
	public void testSelectRenamedClassBeforeNameAssignment() {
		var expression = program.indexOf("ecoreref(MyClass).name = \"Publication\"");
		whenEditorOffsetIsSelectedThenOutlineNodeIsSelected(expression, "Publication");
	}

	@Test
	public void testSelectNearestExpressionOnSharedLine() {
		var first = program.indexOf("addNewEClass(\"First\")");
		var second = program.indexOf("addNewEClass(\"Second\")");
		whenEditorOffsetIsSelectedThenOutlineNodeIsSelected(first - 1, "First");
		whenEditorOffsetIsSelectedThenOutlineNodeIsSelected(
			second + "addNewEClass(\"Second\")".length(), "Second");
	}

	@Test
	public void testSelectExpressionThatCreatesEAttributeInCreatedEClass() {
		whenEditorTextIsSelectedThenOutlineNodeIsSelected
			("addNewEAttribute(\"MyNewAttribute", "MyNewAttribute");
	}

	@Test
	public void testSelectNestedReferenceAcrossItsLine() {
		var line = program.indexOf("addNewEReference(\"publications\"");
		var end = program.indexOf('\n', line);
		for (var offset = line - 2; offset < end; offset++) {
			whenEditorOffsetIsSelectedThenOutlineNodeIsSelected(offset, "publications : Publication");
		}
	}

	@Test
	public void testSelectNestedUpperBoundAcrossItsLine() {
		var line = program.indexOf("upperBound = -1");
		var end = program.indexOf('\n', line);
		for (var offset = line - 2; offset < end; offset++) {
			whenEditorOffsetIsSelectedThenOutlineNodeIsSelected(offset, "upperBound = -1");
		}
	}

	@Test
	public void testSelectExpressionThatCreatesEAttributeInExistingEClass() {
		whenEditorTextIsSelectedThenOutlineNodeIsSelected
			("addNewEAttribute(\"MyNewDerivedClassAttribute", "MyNewDerivedClassAttribute");
	}

	@Test
	public void testInitialNamePropertyIsBoldAndLinksToCreation() {
		var property = findPropertyNode(outlinePage.getTreeViewer().getInput(), "name = MyNewClass");
		Assertions.assertThat(property).isNotNull();
		Assertions.assertThat(property.hasChildren()).isFalse();
		EObject propertyOwner = property.readOnly(object -> object);
		EObject element = ((EcoreOutlineNode) property.getParent()).readOnly(object -> object);
		Assertions.assertThat(propertyOwner).isSameAs(element);
		Assertions.assertThat(property.getText()).isInstanceOf(StyledString.class);
		Assertions.assertThat(((StyledString) property.getText()).getStyleRanges()).isNotEmpty();
		Assertions.assertThat(property.getSignificantTextRegion().getOffset())
			.isEqualTo(program.indexOf("addNewEClass(\"MyNewClass\")"));

		elementOpener.open(property, editor.getInternalSourceViewer());
		Assertions.assertThat(editor.getInternalSourceViewer().getSelectedRange().x)
			.isEqualTo(program.indexOf("addNewEClass(\"MyNewClass\")"));
	}

	@Test
	public void testEditorLinkSelectsContainmentNodeForCreation() {
		whenEditorTextIsSelectedThenOutlineNodeIsSelected("addNewEClass", "MyNewClass");
		var selection = (TreeSelection) outlinePage.getTreeViewer().getSelection();
		Assertions.assertThat(selection.getFirstElement()).isInstanceOf(EcoreOutlineNode.class);
	}

	@Test
	public void testChangedOppositePropertyIsBoldAndDoesNotRecurse() {
		var property = findPropertyNode(outlinePage.getTreeViewer().getInput(), "eOpposite = ");
		Assertions.assertThat(property).isNotNull();
		Assertions.assertThat(property.hasChildren()).isFalse();
		Assertions.assertThat(property.getText()).isInstanceOf(StyledString.class);
		Assertions.assertThat(((StyledString) property.getText()).getStyleRanges()).isNotEmpty();
		Assertions.assertThat(property.getSignificantTextRegion()).isNotNull();
		var offset = property.getSignificantTextRegion().getOffset();
		Assertions.assertThat(offset).isGreaterThanOrEqualTo(
			program.indexOf("ecoreref(MyClass.referencesNewClass).EOpposite"));
		whenEditorOffsetIsSelectedThenOutlineNodeIsSelected(offset, property.getText().toString());
	}

	@Test
	public void testLinkerIgnoresMissingSourceRegion() throws Exception {
		var linker = injector.getInstance(OutlineWithEditorLinker.class);
		var method = linker.getClass().getDeclaredMethod("contains",
			org.eclipse.xtext.util.ITextRegion.class, org.eclipse.xtext.util.ITextRegion.class);
		method.setAccessible(true);
		Assertions.assertThat(method.invoke(linker, null, new TextRegion(0, 0))).isEqualTo(false);
	}

	@Test
	public void testLinkerIgnoresStaleEditorOffset() throws Exception {
		var linker = injector.getInstance(OutlineWithEditorLinker.class);
		var viewerField = OutlineWithEditorLinker.class.getDeclaredField("textViewer");
		viewerField.setAccessible(true);
		viewerField.set(linker, editor.getInternalSourceViewer());
		var method = linker.getClass().getDeclaredMethod("lineAt", int.class);
		method.setAccessible(true);
		var line = (org.eclipse.jface.text.IRegion) method.invoke(linker, -1);
		Assertions.assertThat(line.getLength()).isZero();
	}

	@Test
	public void testLineSearchIgnoresElementWithoutSourceRegion() throws Exception {
		var node = findEcoreNode((IOutlineNode) outlinePage.getTreeViewer().getInput(), "MyNewClass");
		Assertions.assertThat(node).isNotNull();
		var linker = injector.getInstance(OutlineWithEditorLinker.class);
		var method = linker.getClass().getDeclaredMethod("hasSourceRegion", IOutlineNode.class);
		method.setAccessible(true);
		Assertions.assertThat(method.invoke(linker, node)).isEqualTo(true);
		var region = node.getSignificantTextRegion();
		try {
			node.setShortTextRegion(null);
			Assertions.assertThat(method.invoke(linker, node)).isEqualTo(false);
		} finally {
			node.setShortTextRegion(region);
		}
	}

	@Test
	public void testLineSearchIgnoresNonEcoreNode() throws Exception {
		var linker = injector.getInstance(OutlineWithEditorLinker.class);
		var method = linker.getClass().getDeclaredMethod("isResponsibleNode", IOutlineNode.class);
		method.setAccessible(true);
		var root = (IOutlineNode) outlinePage.getTreeViewer().getInput();
		Assertions.assertThat(method.invoke(linker, root)).isEqualTo(false);
	}

	private EcoreOutlineNode findEcoreNode(IOutlineNode node, String label) {
		for (var child : node.getChildren()) {
			if (child instanceof EcoreOutlineNode ecoreNode && label.equals(child.getText().toString())) {
				return ecoreNode;
			}
			var match = findEcoreNode(child, label);
			if (match != null) {
				return match;
			}
		}
		return null;
	}

	private EcorePropertyOutlineNode findPropertyNode(Object root, String label) {
		if (!(root instanceof IOutlineNode node)) {
			return null;
		}
		for (var child : node.getChildren()) {
			if (child instanceof EcorePropertyOutlineNode property &&
					property.getText().toString().startsWith(label) &&
					property.getText() instanceof StyledString) {
				return property;
			}
			var result = findPropertyNode(child, label);
			if (result != null) {
				return result;
			}
		}
		return null;
	}

	private void whenEditorTextIsSelectedThenOutlineNodeIsSelected(String textToSelect, String expectedNode) {
		whenEditorOffsetIsSelectedThenOutlineNodeIsSelected(program.indexOf(textToSelect), expectedNode);
	}

	private void whenEditorOffsetIsSelectedThenOutlineNodeIsSelected(int offset, String expectedNode) {
		editor.getInternalSourceViewer().setSelectedRange(offset, 0);
		Assertions.assertThat(waitForSelection(expectedNode))
			.as("Outline selection at offset %d", offset)
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

	/** Returns the selected outline node's label, or null while none is selected. */
	private String selectedNodeText() {
		var selection = (TreeSelection) outlinePage.getTreeViewer().getSelection();
		return selection.getFirstElement() instanceof IOutlineNode node
			? node.getText().toString() : null;
	}

}
