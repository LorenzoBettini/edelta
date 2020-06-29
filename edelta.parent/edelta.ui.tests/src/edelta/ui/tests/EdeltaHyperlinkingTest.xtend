package edelta.ui.tests

import com.google.inject.Inject
import edelta.ui.tests.utils.EdeltaPluginProjectHelper
import org.eclipse.core.resources.IFile
import org.eclipse.xtext.testing.InjectWith
import org.eclipse.xtext.testing.XtextRunner
import org.eclipse.xtext.ui.editor.XtextEditor
import org.eclipse.xtext.ui.editor.hyperlinking.XtextHyperlink
import org.eclipse.xtext.ui.testing.AbstractHyperlinkingTest
import org.eclipse.xtext.xbase.XAbstractFeatureCall
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(XtextRunner)
@InjectWith(EdeltaUiInjectorProvider)
class EdeltaHyperlinkingTest extends AbstractHyperlinkingTest {

	@Inject EdeltaPluginProjectHelper projectHelper

	XtextEditor xtextEditor

	override protected getFileName() {
		/*
		 * Better to put Edelta file in a source folder
		 */
		"src/" + super.getFileName()
	}

	override void setUp() {
		super.setUp
		/*
		 * Edelta requires a plug-in project to run the interpreter
		 * with edelta.lib as dependency
		 */
		projectHelper.createEdeltaPluginProject(projectName)
	}

	override protected openInEditor(IFile dslFile) {
		this.xtextEditor = super.openInEditor(dslFile)
		return xtextEditor
	}

	/**
	 * If we link to an XExpression its qualified name is null,
	 * and this leads to a NPE
	 */
	override protected _target(XtextHyperlink hyperlink) {
		// don't use resourceSetProvider, because Java types would not be resolved
		// resourceSetProvider.get(project)
		val document = xtextDocumentUtil.getXtextDocument(xtextEditor.internalSourceViewer)
		val resource = document.readOnly[it]
		val resourceSet = resource.resourceSet
		val eObject = resourceSet.getEObject(hyperlink.URI, true)
		switch (eObject) {
			XAbstractFeatureCall: eObject.feature.simpleName
			default: qualifiedNameProvider.getFullyQualifiedName(eObject).toString
		}
	}

	@Test def hyperlinkOnExistingEClass() {
		'''
			metamodel "mypackage"
			
			modifyEcore aTest epackage mypackage {
				ecoreref(«c»MyClass«c»)
			}
		'''.hasHyperlinkTo("mypackage.MyClass")
	}

	@Test def hyperlinkOnQualifiedPart() {
		'''
			metamodel "mypackage"
			
			modifyEcore aTest epackage mypackage {
				ecoreref(«c»mypackage«c».MyClass)
			}
		'''.hasHyperlinkTo("mypackage")
	}

	@Test def hyperlinkOnEPackage() {
		'''
			metamodel "mypackage"
			
			modifyEcore aTest epackage «c»mypackage«c» {
			}
		'''.hasHyperlinkTo("mypackage")
	}

	@Test def hyperlinkOnCreatedEClass() {
		'''
			metamodel "mypackage"
			
			modifyEcore aTest epackage mypackage {
				addNewEClass("NewClass")
				ecoreref(«c»NewClass«c»)
			}
		'''.hasHyperlinkTo("addNewEClass")
	}

	@Test def hyperlinkOnRenamedEClass() {
		'''
			metamodel "mypackage"
			
			modifyEcore aTest epackage mypackage {
				ecoreref(MyClass).name = "Renamed"
				ecoreref(«c»Renamed«c»)
			}
		'''.hasHyperlinkTo("setName")
	}

	@Test def hyperlinkOnForwardCreatedEClass() {
		'''
			metamodel "mypackage"
			
			modifyEcore aTest epackage mypackage {
				ecoreref(«c»NewClass«c»)
				addNewEClass("NewClass")
			}
		'''.hasHyperlinkTo("addNewEClass")
	}

}
