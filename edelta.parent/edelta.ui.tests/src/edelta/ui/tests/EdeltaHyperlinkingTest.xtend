package edelta.ui.tests

import edelta.ui.tests.utils.ProjectImportUtil
import org.eclipse.core.resources.IFile
import org.eclipse.xtext.testing.InjectWith
import org.eclipse.xtext.testing.XtextRunner
import org.eclipse.xtext.ui.editor.XtextEditor
import org.eclipse.xtext.ui.editor.hyperlinking.XtextHyperlink
import org.eclipse.xtext.ui.testing.AbstractHyperlinkingTest
import org.eclipse.xtext.ui.testing.util.IResourcesSetupUtil
import org.eclipse.xtext.xbase.XAbstractFeatureCall
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(XtextRunner)
@InjectWith(EdeltaUiInjectorProvider)
class EdeltaHyperlinkingTest extends AbstractHyperlinkingTest {

	static val TEST_PROJECT = "edelta.ui.tests.project"

	XtextEditor xtextEditor

	/**
	 * Edelta requires a plug-in project to run the interpreter
	 * with edelta.lib as dependency
	 */
	@BeforeClass
	def static void importProject() {
		ProjectImportUtil.importProject(TEST_PROJECT)
		IResourcesSetupUtil.waitForBuild
	}

	/**
	 * Avoids deleting project
	 */
	override void setUp() {

	}

	/**
	 * Avoids deleting project
	 */
	override void tearDown() {
		waitForEventProcessing();
		closeEditors();
		waitForEventProcessing();
	}

	override protected getProjectName() {
		TEST_PROJECT
	}

	/**
	 * Better to put Edelta file in a source folder
	 */
	override protected getFileName() {
		"src/" + super.getFileName()
	}

	override protected openInEditor(IFile dslFile) {
		this.xtextEditor = super.openInEditor(dslFile)
		return xtextEditor
	}

	/**
	 * If we link to an XExpression its qualified name is null,
	 * and this leads to a NPE.
	 * 
	 * Don't use resourceSetProvider, because Java types would not be resolved 
	 * resourceSetProvider.get(project)
	 */
	override protected _target(XtextHyperlink hyperlink) {
		val document = xtextDocumentUtil.getXtextDocument(xtextEditor.internalSourceViewer)
		val resource = document.readOnly[it]
		val resourceSet = resource.resourceSet
		val eObject = resourceSet.getEObject(hyperlink.URI, true)
		if (eObject instanceof XAbstractFeatureCall) {
			return eObject.feature.simpleName
		} 
		return qualifiedNameProvider.getFullyQualifiedName(eObject).toString
	}

	@Test def hyperlinkOnExistingEClass() {
		'''
			metamodel "mypackage"
			
			modifyEcore aTest epackage mypackage {
				ecoreref(<|>MyClass<|>)
			}
		'''.hasHyperlinkTo("mypackage.MyClass")
	}

	@Test def hyperlinkOnQualifiedPart() {
		'''
			metamodel "mypackage"
			
			modifyEcore aTest epackage mypackage {
				ecoreref(<|>mypackage<|>.MyClass)
			}
		'''.hasHyperlinkTo("mypackage")
	}

	@Test def hyperlinkOnEPackage() {
		'''
			metamodel "mypackage"
			
			modifyEcore aTest epackage <|>mypackage<|> {
			}
		'''.hasHyperlinkTo("mypackage")
	}

	@Test def hyperlinkOnCreatedEClass() {
		'''
			metamodel "mypackage"
			
			modifyEcore aTest epackage mypackage {
				addNewEClass("NewClass")
				ecoreref(<|>NewClass<|>)
			}
		'''.hasHyperlinkTo("addNewEClass")
	}

	@Test def hyperlinkOnRenamedEClass() {
		'''
			metamodel "mypackage"
			
			modifyEcore aTest epackage mypackage {
				ecoreref(MyClass).name = "Renamed"
				ecoreref(<|>Renamed<|>)
			}
		'''.hasHyperlinkTo("setName")
	}

	@Test def hyperlinkOnForwardCreatedEClass() {
		'''
			metamodel "mypackage"
			
			modifyEcore aTest epackage mypackage {
				ecoreref(<|>NewClass<|>)
				addNewEClass("NewClass")
			}
		'''.hasHyperlinkTo("addNewEClass")
	}

}
