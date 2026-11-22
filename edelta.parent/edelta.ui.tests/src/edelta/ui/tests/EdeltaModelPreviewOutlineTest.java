package edelta.ui.tests;

import static org.assertj.core.api.BDDAssertions.then;
import static org.eclipse.xtext.ui.testing.util.IResourcesSetupUtil.createFile;
import static org.eclipse.xtext.ui.testing.util.IResourcesSetupUtil.waitForBuild;

import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.core.resources.IFile;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.XtextRunner;
import org.eclipse.xtext.ui.editor.XtextEditor;
import org.eclipse.xtext.ui.editor.outline.IOutlineNode;
import org.eclipse.xtext.ui.editor.outline.impl.OutlineNodeElementOpener;
import org.eclipse.xtext.ui.editor.outline.impl.OutlinePage;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.inject.Inject;

import edelta.ui.internal.EdeltaActivator;
import edelta.ui.outline.EdeltaOutlineTreeProvider.EcoreOutlineNode;
import edelta.ui.outline.EdeltaOutlineTreeProvider.ModelGroupOutlineNode;
import edelta.ui.outline.EdeltaOutlineTreeProvider.ModelObjectOutlineNode;
import edelta.ui.outline.EdeltaOutlineTreeProvider.ModelResourceOutlineNode;
import edelta.ui.testutils.EdeltaUiTestUtils;

/** Tests final in-memory model outline nodes in a real Edelta editor. @author Lorenzo Bettini */
@RunWith(XtextRunner.class)
@InjectWith(EdeltaUiInjectorProvider.class)
public class EdeltaModelPreviewOutlineTest extends CustomAbstractEditorTest {
	private static final String PROJECT = "edelta.ui.tests.project";
	private static final String FIRST = program("FirstChild");
	private static final String SECOND = program("SecondChild");
	private static IFile file;

	@Inject
	private OutlineNodeElementOpener opener;

	private XtextEditor editor;
	private OutlinePage outline;

	/** Installs a small metamodel and model in the Eclipse workspace. */
	@BeforeClass
	public static void setupProject() throws Exception {
		EdeltaUiTestUtils.importJavaProject("../" + PROJECT);
		createFile(PROJECT + "/model/Preview.ecore", """
			<?xml version="1.0" encoding="UTF-8"?>
			<ecore:EPackage xmi:version="2.0" xmlns:xmi="http://www.omg.org/XMI"
			 xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
			 xmlns:ecore="http://www.eclipse.org/emf/2002/Ecore"
			 name="preview" nsURI="http://preview.example" nsPrefix="preview">
			 <eClassifiers xsi:type="ecore:EClass" name="Root">
			  <eStructuralFeatures xsi:type="ecore:EAttribute" name="id" iD="true"
			   eType="ecore:EDataType http://www.eclipse.org/emf/2002/Ecore#//EString"/>
			  <eStructuralFeatures xsi:type="ecore:EReference" name="children"
			   upperBound="-1" containment="true" eType="#//Child"/>
			  <eStructuralFeatures xsi:type="ecore:EReference" name="link" eType="#//Child"/>
			 </eClassifiers>
			 <eClassifiers xsi:type="ecore:EClass" name="Child">
			  <eStructuralFeatures xsi:type="ecore:EAttribute" name="name"
			   eType="ecore:EDataType http://www.eclipse.org/emf/2002/Ecore#//EString"/>
			 </eClassifiers>
			</ecore:EPackage>
			""");
		createFile(PROJECT + "/model/Preview.xmi", """
			<?xml version="1.0" encoding="UTF-8"?>
			<preview:Root xmi:version="2.0" xmlns:xmi="http://www.omg.org/XMI" id="root1"
			 xmlns:preview="http://preview.example" link="//@children.0">
			 <children name="alpha"/>
			 <children name="beta"/>
			 <children name=""/>
			</preview:Root>
			""");
		createFile(PROJECT + "/model/EmptyId.xmi", """
			<?xml version="1.0" encoding="UTF-8"?>
			<preview:Root xmi:version="2.0" xmlns:xmi="http://www.omg.org/XMI"
			 xmlns:preview="http://preview.example" id=""/>
			""");
		file = createFile(PROJECT + "/src/PreviewOutline.edelta", FIRST);
		waitForBuild();
	}

	private static String program(String className) {
		return """
			metamodel "preview"
			testmodels { "model/Preview.xmi" "model/EmptyId.xmi" }
			modifyEcore evolve epackage preview {
				ecoreref(Child).name = "%s"
			}
			""".formatted(className);
	}

	@Override
	protected String getEditorId() {
		return EdeltaActivator.EDELTA_EDELTA;
	}

	@Override
	public void setUp() throws Exception {
		super.setUp();
		editor = openEditor(file);
		outline = editor.getAdapter(OutlinePage.class);
		then(outline).isNotNull();
		editor.setFocus();
	}

	@Test
	public void showsContainmentAndNavigatesToDeclaration() {
		// Check the complete preview subtree, including resource order and every containment child.
		var group = awaitNode(ModelGroupOutlineNode.class, "Evolved test models");
		then(outlineStringRepresentation(group)).isEqualTo("""
			Evolved test models
			  Preview.xmi
			    Root #root1
			      FirstChild alpha
			      FirstChild beta
			      FirstChild
			  EmptyId.xmi
			    Root
			""");

		// The linked Child appears once under Root; the non-containment link adds no node.
		var emptyId = child(group, ModelResourceOutlineNode.class, "EmptyId.xmi");
		child(emptyId, ModelObjectOutlineNode.class, "Root");
		var resource = child(group, ModelResourceOutlineNode.class, "Preview.xmi");
		var root = child(resource, ModelObjectOutlineNode.class, "Root #root1");
		then(root.getChildren()).hasSize(3);
		then(root.getChildren()).extracting(node -> node.getText().toString())
			.containsExactly("FirstChild alpha", "FirstChild beta", "FirstChild");
		var firstChild = (ModelObjectOutlineNode) root.getChildren().getFirst();
		EObject childObject = firstChild.readOnly(object -> object);
		EObject rootObject = root.readOnly(object -> object);
		then(rootObject).isSameAs(childObject.eContainer());
		then(root.getChildren().getFirst().getChildren()).isEmpty();

		// Opening either level must select the quoted path in the actual editor.
		assertNavigation(resource, FIRST.indexOf("\"model/Preview.xmi\""));
		assertNavigation(root, FIRST.indexOf("\"model/Preview.xmi\""));
		assertNavigation(root.getChildren().getFirst(), FIRST.indexOf("\"model/Preview.xmi\""));
	}

	@Test
	public void editingProgramReplacesPreviewNodesAndPreservesEcoreLinking() {
		// Keep old nodes, then edit the metamodel so every Child instance changes its class label.
		var old = awaitNode(ModelObjectOutlineNode.class, "FirstChild alpha");
		var oldResource = (ModelResourceOutlineNode) old.getParent().getParent();
		editor.getInternalSourceViewer().getDocument().set(SECOND);
		var fresh = awaitNode(ModelObjectOutlineNode.class, "SecondChild alpha");
		then(fresh).isNotSameAs(old);
		EObject freshObject = fresh.readOnly(object -> object);
		EObject oldObject = old.readOnly(object -> object);
		then(freshObject).isNotNull();
		then(oldObject).isNull();
		EObject oldDeclaration = oldResource.readOnly(object -> object);
		then(oldDeclaration).isNull();
		then(findNode(root(), ModelObjectOutlineNode.class, "FirstChild alpha")).isNull();

		// New nodes still navigate to the declaration, and editor-to-Ecore linking still works.
		assertNavigation(fresh, SECOND.indexOf("\"model/Preview.xmi\""));
		var ecore = awaitNode(EcoreOutlineNode.class, "SecondChild");
		then(ecore).isNotNull();
		editor.getInternalSourceViewer().setSelectedRange(SECOND.indexOf("ecoreref(Child).name"), 0);
		then(awaitSelected()).isEqualTo("SecondChild");
	}

	@Test
	public void removingProgramClearsPreviewSection() {
		// Clear the editor and wait for the outline to replace the old preview tree.
		awaitNode(ModelGroupOutlineNode.class, "Evolved test models");
		editor.getInternalSourceViewer().getDocument().set("");
		var display = outline.getTreeViewer().getControl().getDisplay();
		var timedOut = new AtomicBoolean();
		Runnable deadline = () -> timedOut.set(true);
		display.timerExec(5000, deadline);
		try {
			while (findNode(root(), ModelGroupOutlineNode.class, "Evolved test models") != null
					&& !timedOut.get()) {
				if (!display.readAndDispatch() && !timedOut.get()) display.sleep();
			}
			then(findNode(root(), ModelGroupOutlineNode.class,
				"Evolved test models")).isNull();
		} finally {
			display.timerExec(-1, deadline);
		}
	}

	private void assertNavigation(IOutlineNode node, int offset) {
		opener.open(node, editor.getInternalSourceViewer());
		then(editor.getInternalSourceViewer().getSelectedRange().x).isEqualTo(offset);
	}

	private IOutlineNode root() {
		return (IOutlineNode) outline.getTreeViewer().getInput();
	}

	private String outlineStringRepresentation(IOutlineNode node) {
		var result = new StringBuilder();
		appendOutline(node, result, 0);
		return result.toString();
	}

	private void appendOutline(IOutlineNode node, StringBuilder result, int depth) {
		result.append("  ".repeat(depth)).append(node.getText()).append('\n');
		for (var child : node.getChildren()) {
			appendOutline(child, result, depth + 1);
		}
	}

	private <T extends IOutlineNode> T awaitNode(Class<T> type, String text) {
		var display = outline.getTreeViewer().getControl().getDisplay();
		var timedOut = new AtomicBoolean();
		Runnable deadline = () -> timedOut.set(true);
		display.timerExec(5000, deadline);
		try {
			T node;
			while ((node = findNode(root(), type, text)) == null && !timedOut.get()) {
				if (!display.readAndDispatch() && !timedOut.get()) display.sleep();
			}
			then(node).as("Outline node %s", text).isNotNull();
			return node;
		} finally {
			display.timerExec(-1, deadline);
		}
	}

	private String awaitSelected() {
		var display = outline.getTreeViewer().getControl().getDisplay();
		var timedOut = new AtomicBoolean();
		Runnable deadline = () -> timedOut.set(true);
		display.timerExec(5000, deadline);
		try {
			String label;
			while (!(label = selectedText()).equals("SecondChild") && !timedOut.get()) {
				if (!display.readAndDispatch() && !timedOut.get()) display.sleep();
			}
			return label;
		} finally {
			display.timerExec(-1, deadline);
		}
	}

	private String selectedText() {
		var selected = ((TreeSelection) outline.getTreeViewer().getSelection()).getFirstElement();
		return selected instanceof IOutlineNode node ? node.getText().toString() : "";
	}

	private <T extends IOutlineNode> T child(IOutlineNode parent, Class<T> type, String text) {
		return parent.getChildren().stream().filter(type::isInstance)
			.map(type::cast).filter(node -> text.equals(node.getText().toString()))
			.findFirst().orElseThrow();
	}

	private <T extends IOutlineNode> T findNode(IOutlineNode parent, Class<T> type,
			String text) {
		if (type.isInstance(parent) && text.equals(parent.getText().toString())) {
			return type.cast(parent);
		}
		for (var child : parent.getChildren()) {
			var match = findNode(child, type, text);
			if (match != null) return match;
		}
		return null;
	}
}
