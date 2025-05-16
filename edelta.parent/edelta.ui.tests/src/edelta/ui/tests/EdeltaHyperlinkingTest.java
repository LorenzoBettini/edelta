package edelta.ui.tests;

import org.eclipse.core.resources.IFile;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.xtext.resource.XtextResource;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.XtextRunner;
import org.eclipse.xtext.ui.editor.XtextEditor;
import org.eclipse.xtext.ui.editor.hyperlinking.XtextHyperlink;
import org.eclipse.xtext.ui.editor.model.IXtextDocument;
import org.eclipse.xtext.ui.testing.AbstractHyperlinkingTest;
import org.eclipse.xtext.ui.testing.AbstractWorkbenchTest;
import org.eclipse.xtext.ui.testing.util.IResourcesSetupUtil;
import org.eclipse.xtext.xbase.XAbstractFeatureCall;
import org.eclipse.xtext.xbase.lib.Exceptions;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import edelta.ui.testutils.EdeltaUiTestUtils;

@RunWith(XtextRunner.class)
@InjectWith(EdeltaUiInjectorProvider.class)
public class EdeltaHyperlinkingTest extends AbstractHyperlinkingTest {
	private static final String TEST_PROJECT = "edelta.ui.tests.project";

	private XtextEditor xtextEditor;

	/**
	 * Edelta requires a plug-in project to run the interpreter with edelta.lib as
	 * dependency
	 */
	@BeforeClass
	public static void importProject() {
		try {
			EdeltaUiTestUtils.importProject("../" + TEST_PROJECT);
			IResourcesSetupUtil.waitForBuild();
		} catch (Throwable e) {
			throw Exceptions.sneakyThrow(e);
		}
	}

	/**
	 * Avoids deleting project
	 */
	@Override
	public void setUp() {
		// avoid deleting project
	}

	/**
	 * Avoids deleting project
	 */
	@Override
	public void tearDown() {
		waitForEventProcessing();
		AbstractWorkbenchTest.closeEditors();
		waitForEventProcessing();
	}

	@Override
	protected String getProjectName() {
		return EdeltaHyperlinkingTest.TEST_PROJECT;
	}

	/**
	 * Better to put Edelta file in a source folder
	 */
	@Override
	protected String getFileName() {
		return "src/" + super.getFileName();
	}

	@Override
	protected XtextEditor openInEditor(IFile dslFile) {
		xtextEditor = super.openInEditor(dslFile);
		return xtextEditor;
	}

	/**
	 * If we link to an XExpression its qualified name is null, and this leads to a
	 * NPE.
	 * 
	 * Don't use resourceSetProvider, because Java types would not be resolved
	 * resourceSetProvider.get(project)
	 */
	@Override
	protected String _target(XtextHyperlink hyperlink) {
		IXtextDocument document = xtextDocumentUtil
				.getXtextDocument(xtextEditor.getInternalSourceViewer());
		XtextResource resource = document.readOnly(it -> it);
		ResourceSet resourceSet = resource.getResourceSet();
		EObject eObject = resourceSet.getEObject(hyperlink.getURI(), true);
		if (eObject instanceof XAbstractFeatureCall) {
			return ((XAbstractFeatureCall) eObject).getFeature().getSimpleName();
		}
		return qualifiedNameProvider.getFullyQualifiedName(eObject).toString();
	}

	@Test
	public void hyperlinkOnExistingEClass() { // NOSONAR the assertion is in hasHyperlinkTo
		hasHyperlinkTo("""
			metamodel "mypackage"
			
			modifyEcore aTest epackage mypackage {
				ecoreref(<|>MyClass<|>)
			}
			""",
			"mypackage.MyClass");
	}

	@Test
	public void hyperlinkOnQualifiedPart() { // NOSONAR the assertion is in hasHyperlinkTo
		hasHyperlinkTo("""
			metamodel "mypackage"
			
			modifyEcore aTest epackage mypackage {
				ecoreref(<|>mypackage<|>.MyClass)
			}
			""",
			"mypackage");
	}

	@Test
	public void hyperlinkOnEPackage() { // NOSONAR the assertion is in hasHyperlinkTo
		hasHyperlinkTo("""
			metamodel "mypackage"
			
			modifyEcore aTest epackage <|>mypackage<|> {
			}
			""",
			"mypackage");
	}

	@Test
	public void hyperlinkOnCreatedEClass() { // NOSONAR the assertion is in hasHyperlinkTo
		hasHyperlinkTo("""
			metamodel "mypackage"
			
			modifyEcore aTest epackage mypackage {
				addNewEClass("NewClass")
				ecoreref(<|>NewClass<|>)
			}
			""",
			"addNewEClass");
	}

	@Test
	public void hyperlinkOnRenamedEClass() { // NOSONAR the assertion is in hasHyperlinkTo
		hasHyperlinkTo("""
			metamodel "mypackage"
			
			modifyEcore aTest epackage mypackage {
				ecoreref(MyClass).name = "Renamed"
				ecoreref(<|>Renamed<|>)
			}
			""",
			"setName");
	}

	@Test
	public void hyperlinkOnForwardCreatedEClass() { // NOSONAR the assertion is in hasHyperlinkTo
		hasHyperlinkTo("""
			metamodel "mypackage"
			
			modifyEcore aTest epackage mypackage {
				ecoreref(<|>NewClass<|>)
				addNewEClass("NewClass")
			}
			""",
			"addNewEClass");
	}
}
