package edelta.ui.tests;

import static org.assertj.core.api.BDDAssertions.then;
import static org.eclipse.xtext.ui.testing.util.IResourcesSetupUtil.createFile;
import static org.eclipse.xtext.ui.testing.util.IResourcesSetupUtil.waitForBuild;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.Platform;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.XtextRunner;
import org.eclipse.xtext.ui.PluginImageHelper;
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
import edelta.ui.outline.EdeltaOutlineTreeProvider.ModelPropertyOutlineNode;
import edelta.ui.outline.EdeltaOutlineTreeProvider.ModelResourceOutlineNode;
import edelta.ui.testutils.EdeltaUiTestUtils;

/** Tests final in-memory model outline nodes in a real Edelta editor. @author Lorenzo Bettini */
@RunWith(XtextRunner.class)
@InjectWith(EdeltaUiInjectorProvider.class)
public class EdeltaModelPreviewOutlineTest extends CustomAbstractEditorTest {
	private static final String PROJECT = "edelta.ui.tests.project";
	private static final String FIRST = program("FirstChild");
	private static final String SECOND = program("SecondChild");
	private static final String DIRECT = """
		metamodel "preview"
		testmodels { "model/Preview.xmi" "model/EmptyId.xmi" }
		modifyEcore evolve epackage preview {
			ecoreref(Root.link).lowerBound = 1
		}
		""";
	private static final String RENAMED_FEATURE = """
		metamodel "preview"
		testmodels { "model/Preview.xmi" "model/EmptyId.xmi" }
		modifyEcore evolve epackage preview {
			ecoreref(Root.link).name = "primaryLink"
		}
		""";
	private static final String MULTIPLE_ATTRIBUTE = """
		import edelta.refactorings.lib.EdeltaRefactorings
		metamodel "preview"
		testmodels { "model/Preview.xmi" "model/EmptyId.xmi" }
		use EdeltaRefactorings as extension refactorings
		modifyEcore evolve epackage preview {
			refactorings.changeToMultiple(ecoreref(Child.name))
		}
		""";
	private static final String NEW_FEATURE = """
		metamodel "preview"
		metamodel "ecore"
		testmodels { "model/Preview.xmi" "model/EmptyId.xmi" }
		modifyEcore evolve epackage preview {
			ecoreref(Root).EStructuralFeatures += newEAttribute("extra", ecoreref(EString))
		}
		""";
	private static final String RESTORED_NAME = """
		metamodel "preview"
		testmodels { "model/Preview.xmi" "model/EmptyId.xmi" }
		modifyEcore evolve epackage preview {
			ecoreref(Child).name = "Temporary"
			ecoreref(Temporary).name = "Child"
		}
		""";
	private static final String SHARED_CHANGE = """
		import org.eclipse.emf.ecore.EReference
		metamodel "preview"
		testmodels { "model/Preview.xmi" "model/EmptyId.xmi" }
		def changeBoth(EReference feature) {
			feature.lowerBound = 1
			feature.upperBound = 3
		}
		modifyEcore evolve epackage preview {
			changeBoth(ecoreref(Root.links))
		}
		""";
	private static final String CONFLICTING = """
		metamodel "preview"
		testmodels { "model/Preview.xmi" "model/EmptyId.xmi" }
		modifyEcore evolve epackage preview {
			ecoreref(Root.links).lowerBound = 1
			ecoreref(Root.links).upperBound = 3
		}
		""";
	private static final String MIGRATED = """
		import edelta.refactorings.lib.EdeltaRefactorings
		metamodel "preview"
		metamodel "ecore"
		testmodels { "model/Preview.xmi" "model/EmptyId.xmi" }
		use EdeltaRefactorings as extension refactorings
		modifyEcore evolve epackage preview {
			val name = ecoreref(Child.name)
			modelMigration[
				transformAttributeValueRule(isRelatedTo(name), [value |
					value.toString.toUpperCase
				])
			]
			modelMigration[
				transformAttributeValueRule(isRelatedTo(name), [value |
					value.toString + "!"
				])
			]
			refactorings.changeToSingle(ecoreref(Root.links))
		}
		""";
	private static IFile file;

	@Inject
	private OutlineNodeElementOpener opener;

	@Inject
	private PluginImageHelper imageHelper;

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
			  <eStructuralFeatures xsi:type="ecore:EReference" name="links"
			   upperBound="-1" eType="#//Child"/>
			  <eStructuralFeatures xsi:type="ecore:EReference" name="empty"
			   containment="true" eType="#//Empty"/>
			 </eClassifiers>
			 <eClassifiers xsi:type="ecore:EClass" name="Child">
			  <eStructuralFeatures xsi:type="ecore:EAttribute" name="name"
			   eType="ecore:EDataType http://www.eclipse.org/emf/2002/Ecore#//EString"/>
			 </eClassifiers>
			 <eClassifiers xsi:type="ecore:EClass" name="Empty"/>
			</ecore:EPackage>
			""");
		createFile(PROJECT + "/model/Preview.xmi", """
			<?xml version="1.0" encoding="UTF-8"?>
			<preview:Root xmi:version="2.0" xmlns:xmi="http://www.omg.org/XMI" id="root1"
			 xmlns:preview="http://preview.example" link="//@children.0"
			 links="//@children.0 //@children.1">
			 <children name="alpha"/>
			 <children name="beta"/>
			 <children name=""/>
			 <empty/>
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
	public void showsContainmentAndNavigatesToDeclaration() throws IOException {
		// Check the complete preview subtree, including resource order and every containment child.
		var group = awaitNode(ModelGroupOutlineNode.class, "Evolved test models");
		then(outlineStringRepresentation(group)).isEqualTo("""
			Evolved test models
			  Preview.xmi
			    Root #root1
			      FirstChild alpha
			        name = "alpha"
			      FirstChild beta
			        name = "beta"
			      FirstChild
			        name = ""
			      Empty
			      id = "root1"
			      link [0..1] = FirstChild alpha (#//@children.0)
			      links [0..*] = [FirstChild alpha (#//@children.0), FirstChild beta (#//@children.1)]
			  EmptyId.xmi
			    Root
			      id = ""
			      link [0..1] = <unset>
			      links [0..*] = []
			""");

		// The linked Child appears once under Root; the non-containment link adds no node.
		var emptyId = child(group, ModelResourceOutlineNode.class, "EmptyId.xmi");
		child(emptyId, ModelObjectOutlineNode.class, "Root");
		var resource = child(group, ModelResourceOutlineNode.class, "Preview.xmi");
		var root = child(resource, ModelObjectOutlineNode.class, "Root #root1");
		then(root.getChildren()).hasSize(7);
		then(root.getChildren()).extracting(node -> node.getText().toString())
			.containsExactly("FirstChild alpha", "FirstChild beta", "FirstChild", "Empty",
				"id = \"root1\"", "link [0..1] = FirstChild alpha (#//@children.0)",
				"links [0..*] = [FirstChild alpha (#//@children.0), FirstChild beta (#//@children.1)]");
		var firstChild = (ModelObjectOutlineNode) root.getChildren().getFirst();
		EObject childObject = firstChild.readOnly(object -> object);
		EObject rootObject = root.readOnly(object -> object);
		then(rootObject).isSameAs(childObject.eContainer());
		then(root.getChildren().getFirst().getChildren()).hasSize(1);
		var link = child(root, ModelPropertyOutlineNode.class,
			"link [0..1] = FirstChild alpha (#//@children.0)");
		assertOutlineImages(group, emptyId, resource, root, link);

		// Only the renamed class uses the expression; unrelated rows use the path.
		assertNavigationAndUnload(resource, root, link, rootObject);
	}

	private void assertOutlineImages(ModelGroupOutlineNode group,
			ModelResourceOutlineNode emptyId, ModelResourceOutlineNode resource,
			ModelObjectOutlineNode root, ModelPropertyOutlineNode link) throws IOException {
		var attributeImage = assertEmfIcon("EAttribute.gif");
		var referenceImage = assertEmfIcon("EReference.gif");
		then(attributeImage).isNotSameAs(referenceImage);
		var attribute = child(root, ModelPropertyOutlineNode.class, "id = \"root1\"");
		then(outlineImage(attribute)).isSameAs(attributeImage);
		then(attribute.getChildren()).isEmpty();
		then(outlineImage(link)).isSameAs(referenceImage);
		then(outlineImage(child(root, ModelPropertyOutlineNode.class,
			"links [0..*] = [FirstChild alpha (#//@children.0), FirstChild beta (#//@children.1)]")))
			.isSameAs(referenceImage);
		then(link.getChildren()).isEmpty();
		then(child(root, ModelObjectOutlineNode.class, "Empty").getChildren()).isEmpty();
		then(outlineImage(group)).isSameAs(PlatformUI.getWorkbench().getSharedImages()
			.getImage(ISharedImages.IMG_OBJ_FOLDER))
			.isNotNull().satisfies(image -> then(image.isDisposed()).isFalse());
		then(EdeltaActivator.getInstance().getBundle().getEntry("icons/ModelFile.gif")).isNotNull();
		var modelImage = imageHelper.getImage("ModelFile.gif");
		then(modelImage).isNotNull().satisfies(image -> then(image.isDisposed()).isFalse());
		then(modelImage).isNotSameAs(PlatformUI.getWorkbench().getSharedImages()
			.getImage(ISharedImages.IMG_OBJ_FILE));
		then(outlineImage(resource)).isSameAs(modelImage);
		then(outlineImage(emptyId)).isSameAs(modelImage);
	}

	private void assertNavigationAndUnload(ModelResourceOutlineNode resource,
			ModelObjectOutlineNode root, ModelPropertyOutlineNode link, EObject rootObject) {
		assertNavigation(resource, FIRST.indexOf("\"model/Preview.xmi\""));
		assertNavigation(root, FIRST.indexOf("\"model/Preview.xmi\""));
		var renamed = root.getChildren().getFirst();
		assertNavigation(renamed, FIRST.indexOf("name = \"FirstChild\""));
		assertNavigation(renamed.getChildren().getFirst(), FIRST.indexOf("\"model/Preview.xmi\""));
		assertNavigation(link, FIRST.indexOf("\"model/Preview.xmi\""));
		EObject propertyOwner = link.readOnly(object -> object);
		then(propertyOwner).isSameAs(rootObject);
		propertyOwner.eResource().unload();
		EObject unloadedOwner = link.readOnly(object -> object);
		then(unloadedOwner).isNull();
	}

	@Test
	public void directlyChangedReferenceBoundNavigatesToUserExpression() {
		editor.getInternalSourceViewer().getDocument().set(DIRECT);
		var row = awaitNode(ModelPropertyOutlineNode.class,
			"link [1..1] = Child alpha (#//@children.0)");
		assertNavigation(row, DIRECT.indexOf("lowerBound = 1"));
		assertNavigation(row.getParent(), DIRECT.indexOf("\"model/Preview.xmi\""));
	}

	@Test
	public void renamedFeatureNavigatesToUserExpression() {
		editor.getInternalSourceViewer().getDocument().set(RENAMED_FEATURE);
		var row = awaitNode(ModelPropertyOutlineNode.class,
			"primaryLink [0..1] = Child alpha (#//@children.0)");
		assertNavigation(row, RENAMED_FEATURE.indexOf("name = \"primaryLink\""));
	}

	@Test
	public void attributeScalarToListNavigatesToUserCall() {
		editor.getInternalSourceViewer().getDocument().set(MULTIPLE_ATTRIBUTE);
		var row = awaitNode(ModelPropertyOutlineNode.class, "name = [\"alpha\"]");
		assertNavigation(row, MULTIPLE_ATTRIBUTE.indexOf("changeToMultiple(ecoreref(Child.name))"));
	}

	@Test
	public void newlyAddedFeatureKeepsDeclarationFallback() {
		editor.getInternalSourceViewer().getDocument().set(NEW_FEATURE);
		var row = awaitNode(ModelPropertyOutlineNode.class, "extra = <unset>");
		assertNavigation(row, NEW_FEATURE.indexOf("\"model/Preview.xmi\""));
	}

	@Test
	public void classRenamedBackToOriginalKeepsDeclarationFallback() {
		editor.getInternalSourceViewer().getDocument().set(RESTORED_NAME);
		var instance = awaitNode(ModelObjectOutlineNode.class, "Child alpha");
		assertNavigation(instance, RESTORED_NAME.indexOf("\"model/Preview.xmi\""));
	}

	@Test
	public void oneCallChangingBothVisibleBoundsKeepsDeclarationFallback() {
		editor.getInternalSourceViewer().getDocument().set(SHARED_CHANGE);
		var row = awaitNode(ModelPropertyOutlineNode.class,
			"links [1..3] = [Child alpha (#//@children.0), Child beta (#//@children.1)]");
		assertNavigation(row, SHARED_CHANGE.indexOf("\"model/Preview.xmi\""));
	}

	@Test
	public void separateRelevantChangesKeepDeclarationFallback() {
		editor.getInternalSourceViewer().getDocument().set(CONFLICTING);
		var row = awaitNode(ModelPropertyOutlineNode.class,
			"links [1..3] = [Child alpha (#//@children.0), Child beta (#//@children.1)]");
		assertNavigation(row, CONFLICTING.indexOf("\"model/Preview.xmi\""));
	}

	@Test
	public void stagedMigrationAndLibraryMultiplicityRefreshPropertyRows() {
		var old = child(awaitNode(ModelGroupOutlineNode.class, "Evolved test models"),
			ModelResourceOutlineNode.class, "Preview.xmi");
		editor.getInternalSourceViewer().getDocument().set(MIGRATED);
		var migrated = awaitNode(ModelObjectOutlineNode.class, "Child ALPHA!");
		then(migrated.getChildren()).extracting(node -> node.getText().toString())
			.containsExactly("name = \"ALPHA!\"");
		var root = (ModelObjectOutlineNode) migrated.getParent();
		then(root.getChildren()).extracting(node -> node.getText().toString())
			.contains("links [0..1] = Child ALPHA! (#//@children.0)");
		then(root.getChildren()).noneMatch(node -> node.getText().toString()
			.startsWith("links [0..*]"));
		EObject oldDeclaration = old.readOnly(object -> object);
		then(oldDeclaration).isNull();
		assertNavigation(migrated, MIGRATED.indexOf("\"model/Preview.xmi\""));
		assertNavigation(migrated.getChildren().getFirst(),
			MIGRATED.indexOf("\"model/Preview.xmi\""));
		assertNavigation(child(root, ModelPropertyOutlineNode.class,
			"links [0..1] = Child ALPHA! (#//@children.0)"),
			MIGRATED.indexOf("changeToSingle(ecoreref(Root.links))"));
	}

	@Test
	public void editingProgramReplacesPreviewNodesAndPreservesEcoreLinking() {
		// Keep old nodes, then edit the metamodel so every Child instance changes its class label.
		var old = awaitNode(ModelObjectOutlineNode.class, "FirstChild alpha");
		var oldResource = (ModelResourceOutlineNode) old.getParent().getParent();
		var oldProperty = child(old, ModelPropertyOutlineNode.class, "name = \"alpha\"");
		var oldRoot = (ModelObjectOutlineNode) old.getParent();
		var oldReference = child(oldRoot, ModelPropertyOutlineNode.class,
			"link [0..1] = FirstChild alpha (#//@children.0)");
		var attributeImage = outlineImage(oldProperty);
		var referenceImage = outlineImage(oldReference);
		then(attributeImage).isNotNull();
		then(referenceImage).isNotNull().isNotSameAs(attributeImage);
		editor.getInternalSourceViewer().getDocument().set(SECOND);
		var fresh = awaitNode(ModelObjectOutlineNode.class, "SecondChild alpha");
		then(fresh).isNotSameAs(old);
		EObject freshObject = fresh.readOnly(object -> object);
		EObject oldObject = old.readOnly(object -> object);
		then(freshObject).isNotNull();
		then(oldObject).isNull();
		EObject oldDeclaration = oldResource.readOnly(object -> object);
		then(oldDeclaration).isNull();
		EObject oldPropertyOwner = oldProperty.readOnly(object -> object);
		then(oldPropertyOwner).isNull();
		then(findNode(root(), ModelObjectOutlineNode.class, "FirstChild alpha")).isNull();
		then(attributeImage.isDisposed()).isFalse();
		then(referenceImage.isDisposed()).isFalse();
		then(outlineImage(child(fresh, ModelPropertyOutlineNode.class, "name = \"alpha\"")))
			.isSameAs(attributeImage);
		var freshRoot = (ModelObjectOutlineNode) fresh.getParent();
		then(outlineImage(child(freshRoot, ModelPropertyOutlineNode.class,
			"link [0..1] = SecondChild alpha (#//@children.0)")))
			.isSameAs(referenceImage);

		// Fresh nodes use the new source expression; stale objects and rows are detached.
		assertNavigation(fresh, SECOND.indexOf("name = \"SecondChild\""));
		assertNavigation(child(fresh, ModelPropertyOutlineNode.class, "name = \"alpha\""),
			SECOND.indexOf("\"model/Preview.xmi\""));
		then(fresh.getSignificantTextRegion().getOffset())
			.isEqualTo(SECOND.indexOf("name = \"SecondChild\""));
		// Editor-to-Ecore linking still works.
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

	/** Returns the image displayed by the outline for the given node. */
	private Image outlineImage(IOutlineNode node) {
		return ((ILabelProvider) outline.getTreeViewer().getLabelProvider()).getImage(node);
	}

	private org.eclipse.swt.graphics.Image assertEmfIcon(String name) throws IOException {
		var ownEntry = EdeltaActivator.getInstance().getBundle().getEntry("icons/" + name);
		var emfBundle = Platform.getBundle("org.eclipse.emf.ecore.edit");
		then(ownEntry).isNotNull();
		then(emfBundle).isNotNull();
		var emfEntry = emfBundle.getEntry("icons/full/obj16/" + name);
		then(emfEntry).isNotNull();
		try (var own = ownEntry.openStream(); var emf = emfEntry.openStream()) {
			then(own.readAllBytes()).isEqualTo(emf.readAllBytes());
		}
		var image = imageHelper.getImage(name);
		then(image).isNotNull().satisfies(value -> then(value.isDisposed()).isFalse());
		return image;
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
