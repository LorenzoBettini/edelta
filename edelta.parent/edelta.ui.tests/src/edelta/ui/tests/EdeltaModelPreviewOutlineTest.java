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
	private static final String STAGED = """
		metamodel "preview"
		testmodels { "model/Preview.xmi" "model/EmptyId.xmi" }
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
		}
		""";
	private static final String MIXED = """
		import java.util.concurrent.atomic.AtomicInteger
		metamodel "preview"
		testmodels { "model/Preview.xmi" "model/EmptyId.xmi" }
		modifyEcore evolve epackage preview {
			val name = ecoreref(Child.name)
			val visits = new AtomicInteger(0)
			modelMigration[
				transformAttributeValueRule(isRelatedTo(name), [value |
					value.toString.toUpperCase
				])
			]
			modelMigration[
				transformAttributeValueRule([attribute |
					isRelatedTo(attribute, name) && visits.getAndIncrement() == 0
				], [value | value.toString + "!"])
			]
		}
		""";
	private static final String NO_OP = STAGED
		.replace("value.toString.toUpperCase", "value.toString")
		.replace("value.toString + \"!\"", "value.toString");
	private static final String CHANGED_THEN_NO_OP = STAGED
		.replace("value.toString + \"!\"", "value.toString");
	private static final String REVERSAL = STAGED
		.replace("value.toString + \"!\"", "value.toString.toLowerCase");
	private static final String CHANGED_REFERENCE = """
		metamodel "preview"
		testmodels { "model/Preview.xmi" "model/EmptyId.xmi" }
		modifyEcore evolve epackage preview {
			val link = ecoreref(Root.link)
			modelMigration[
				copyRule(isRelatedTo(link), [feature, source, target | ])
			]
		}
		""";
	private static final String CROSS_RESOURCE_REFERENCE = CHANGED_REFERENCE
		.replace("\"model/EmptyId.xmi\"", "\"model/Cross.xmi\"");
	private static final String CHANGED_KNOWN_CROSS_RESOURCE_REFERENCE = """
		import org.eclipse.emf.ecore.EObject
		metamodel "preview"
		testmodels { "model/CrossSource.xmi" "model/CrossTargets.xmi" }
		modifyEcore evolve epackage preview {
			val link = ecoreref(Root.link)
			modelMigration[
				copyRule(isRelatedTo(link), [feature, source, target |
					val first = source.eGet(feature) as EObject
					if (first !== null)
						target.eSet(link, getMigrated(first.eContainer.eContents.get(1)))
				])
			]
		}
		""";
	private static final String ECORE_AND_VALUE = """
		metamodel "preview"
		testmodels { "model/Preview.xmi" "model/EmptyId.xmi" }
		modifyEcore evolve epackage preview {
			val name = ecoreref(Child.name)
			modelMigration[
				transformAttributeValueRule(isRelatedTo(name), [value |
					value.toString.toUpperCase
				])
			]
			name.name = "label"
		}
		""";
	private static final String LIBRARY_MIGRATION = """
		import previewrefactorings.PreviewRefactorings
		metamodel "preview"
		testmodels { "model/Preview.xmi" "model/EmptyId.xmi" }
		use PreviewRefactorings as extension refactorings
		modifyEcore evolve epackage preview {
			refactorings.uppercase(ecoreref(Child.name))
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
		createFile(PROJECT + "/model/Cross.xmi", """
			<?xml version="1.0" encoding="UTF-8"?>
			<preview:Root xmi:version="2.0" xmlns:xmi="http://www.omg.org/XMI"
			 xmlns:preview="http://preview.example" id="cross1"
			 link="Preview.xmi#//@children.0"/>
			""");
		createFile(PROJECT + "/model/CrossSource.xmi", """
			<?xml version="1.0" encoding="UTF-8"?>
			<preview:Root xmi:version="2.0" xmlns:xmi="http://www.omg.org/XMI"
			 xmlns:preview="http://preview.example" id="crossSource"
			 link="CrossTargets.xmi#//@children.0"/>
			""");
		createFile(PROJECT + "/model/CrossTargets.xmi", """
			<?xml version="1.0" encoding="UTF-8"?>
			<preview:Root xmi:version="2.0" xmlns:xmi="http://www.omg.org/XMI"
			 xmlns:preview="http://preview.example" id="crossTargets">
			 <children name="alpha"/>
			 <children name="beta"/>
			</preview:Root>
			""");
		createFile(PROJECT + "/src/PreviewRefactorings.edelta", """
			import org.eclipse.emf.ecore.EAttribute
			package previewrefactorings
			metamodel "preview"
			def uppercase(EAttribute feature) {
				modelMigration[
					transformAttributeValueRule(isRelatedTo(feature), [value |
						value.toString.toUpperCase
					])
				]
			}
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
		assertMigrationNavigation(migrated.getChildren().getFirst(), MIGRATED, 1);
		assertNavigation(child(root, ModelPropertyOutlineNode.class,
			"links [0..1] = Child ALPHA! (#//@children.0)"),
			MIGRATED.indexOf("changeToSingle(ecoreref(Root.links))"));
	}

	@Test
	public void stagedAttributeRowsNavigateToSecondChangingMigration() {
		// Both visible values pass through uppercase and suffix; the editor
		// selection must land on the second invocation, not just any rule.
		editor.getInternalSourceViewer().getDocument().set(STAGED);
		var first = awaitNode(ModelObjectOutlineNode.class, "Child ALPHA!");
		var second = awaitNode(ModelObjectOutlineNode.class, "Child BETA!");
		assertMigrationNavigation(child(first, ModelPropertyOutlineNode.class, "name = \"ALPHA!\""),
			STAGED, 1);
		assertMigrationNavigation(child(second, ModelPropertyOutlineNode.class, "name = \"BETA!\""),
			STAGED, 1);
	}

	@Test
	public void sameAttributeRowsNavigateToDifferentChangingMigrations() {
		// The two rows share Child.name, but only ALPHA receives suffix. This
		// catches a feature-level hint accidentally assigned to both rows.
		editor.getInternalSourceViewer().getDocument().set(MIXED);
		var first = awaitNode(ModelObjectOutlineNode.class, "Child ALPHA!");
		var second = awaitNode(ModelObjectOutlineNode.class, "Child BETA");
		assertMigrationNavigation(child(first, ModelPropertyOutlineNode.class, "name = \"ALPHA!\""),
			MIXED, 1);
		assertMigrationNavigation(child(second, ModelPropertyOutlineNode.class, "name = \"BETA\""),
			MIXED, 0);
	}

	@Test
	public void noOpAndLaterEqualStageSelectOnlyProvenChanges() {
		editor.getInternalSourceViewer().getDocument().set(NO_OP);
		var unchanged = awaitNode(ModelObjectOutlineNode.class, "Child alpha");
		assertNavigation(child(unchanged, ModelPropertyOutlineNode.class, "name = \"alpha\""),
			NO_OP, "\"model/Preview.xmi\"");

		editor.getInternalSourceViewer().getDocument().set(CHANGED_THEN_NO_OP);
		var changed = awaitNode(ModelObjectOutlineNode.class, "Child ALPHA");
		assertMigrationNavigation(child(changed, ModelPropertyOutlineNode.class,
			"name = \"ALPHA\""), CHANGED_THEN_NO_OP, 0);
	}

	@Test
	public void restoringOriginalValueLinksToRestoringStage() {
		editor.getInternalSourceViewer().getDocument().set(REVERSAL);
		var restored = awaitNode(ModelObjectOutlineNode.class, "Child alpha");
		assertMigrationNavigation(child(restored, ModelPropertyOutlineNode.class,
			"name = \"alpha\""), REVERSAL, 1);
	}

	@Test
	public void changedReferenceUsesMigrationExpressionAndUntouchedReferenceUsesDeclaration() {
		editor.getInternalSourceViewer().getDocument().set(CHANGED_REFERENCE);
		var root = awaitNode(ModelObjectOutlineNode.class, "Root #root1");
		assertMigrationNavigation(child(root, ModelPropertyOutlineNode.class,
			"link [0..1] = <unset>"), CHANGED_REFERENCE, 0);
		assertNavigation(child(root, ModelPropertyOutlineNode.class,
			"links [0..*] = [Child alpha (#//@children.0), Child beta (#//@children.1)]"),
			CHANGED_REFERENCE, "\"model/Preview.xmi\"");
	}

	@Test
	public void unresolvedCrossResourceReferenceUsesDeclaration() {
		editor.getInternalSourceViewer().getDocument().set(CROSS_RESOURCE_REFERENCE);
		var root = awaitNode(ModelObjectOutlineNode.class, "Root #cross1");
		// The source endpoint is unresolved; neither its URI nor its display
		// text can establish exact copied-object correspondence.
		assertNavigation(child(root, ModelPropertyOutlineNode.class,
			"link [0..1] = <unset>"), CROSS_RESOURCE_REFERENCE, "\"model/Cross.xmi\"");
	}

	@Test
	public void changedKnownCrossResourceReferenceSelectsMigrationExpression() {
		editor.getInternalSourceViewer().getDocument().set(CHANGED_KNOWN_CROSS_RESOURCE_REFERENCE);
		var source = awaitNode(ModelObjectOutlineNode.class, "Root #crossSource");
		var targets = awaitNode(ModelObjectOutlineNode.class, "Root #crossTargets");
		var row = child(source, ModelPropertyOutlineNode.class,
			"link [0..1] = Child beta (CrossTargets.xmi#//@children.1)");
		var sourceObject = source.readOnly(object -> object);
		var secondTarget = ((ModelObjectOutlineNode) targets.getChildren().get(1))
			.readOnly(object -> object);
		then(sourceObject.eResource()).isNotSameAs(secondTarget.eResource());
		then(sourceObject.eGet(sourceObject.eClass().getEStructuralFeature("link")))
			.isSameAs(secondTarget);
		assertMigrationNavigation(row, CHANGED_KNOWN_CROSS_RESOURCE_REFERENCE, 0);
	}

	@Test
	public void visibleEcoreChangePrecedesInstanceValueChange() {
		editor.getInternalSourceViewer().getDocument().set(ECORE_AND_VALUE);
		var changed = awaitNode(ModelObjectOutlineNode.class, "Child");
		assertNavigation(child(changed, ModelPropertyOutlineNode.class, "label = \"ALPHA\""),
			ECORE_AND_VALUE, "name = \"label\"");
	}

	@Test
	public void libraryMigrationSelectsTheUserCall() {
		editor.getInternalSourceViewer().getDocument().set(LIBRARY_MIGRATION);
		var changed = awaitNode(ModelObjectOutlineNode.class, "Child ALPHA");
		assertNavigation(child(changed, ModelPropertyOutlineNode.class, "name = \"ALPHA\""),
			LIBRARY_MIGRATION, "uppercase(ecoreref(Child.name))");
	}

	@Test
	public void editingAndRestoringProgramReplacesAttributeNavigationTokens() {
		// Replacing the editor text disposes the old preview. Restoring the program
		// must create a fresh token and must not revive the old outline row.
		editor.getInternalSourceViewer().getDocument().set(STAGED);
		var transformed = awaitNode(ModelObjectOutlineNode.class, "Child ALPHA!");
		var oldRow = child(transformed, ModelPropertyOutlineNode.class, "name = \"ALPHA!\"");
		assertNavigation(oldRow, STAGED.lastIndexOf("modelMigration["));
		editor.getInternalSourceViewer().getDocument().set(FIRST);
		var original = awaitNode(ModelObjectOutlineNode.class, "FirstChild alpha");
		assertNavigation(child(original, ModelPropertyOutlineNode.class, "name = \"alpha\""),
			FIRST.indexOf("\"model/Preview.xmi\""));
		EObject oldOwner = oldRow.readOnly(object -> object);
		then(oldOwner).isNull();
		editor.getInternalSourceViewer().getDocument().set(STAGED);
		var restored = awaitNode(ModelObjectOutlineNode.class, "Child ALPHA!");
		assertNavigation(child(restored, ModelPropertyOutlineNode.class, "name = \"ALPHA!\""),
			STAGED.lastIndexOf("modelMigration["));
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

	private void assertNavigation(IOutlineNode node, String source, String selectedText) {
		assertNavigation(node, source.indexOf(selectedText));
		then(editor.getInternalSourceViewer().getSelectedRange().y).isEqualTo(selectedText.length());
	}

	/** Checks the complete selected call, including its nested closure brackets. */
	private void assertMigrationNavigation(IOutlineNode node, String source, int occurrence) {
		var start = -1;
		for (var i = 0; i <= occurrence; i++)
			start = source.indexOf("modelMigration[", start + 1);
		var depth = 0;
		var end = start;
		for (; end < source.length(); end++) {
			if (source.charAt(end) == '[') depth++;
			if (source.charAt(end) == ']' && --depth == 0) break;
		}
		opener.open(node, editor.getInternalSourceViewer());
		var selected = editor.getInternalSourceViewer().getSelectedRange();
		then(selected.x).isEqualTo(start);
		then(selected.y).isEqualTo(end - start + 1);
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
