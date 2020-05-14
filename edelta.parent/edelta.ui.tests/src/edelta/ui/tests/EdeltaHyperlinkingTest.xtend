package edelta.ui.tests

import com.google.inject.Inject
import edelta.ui.tests.utils.EdeltaPluginProjectHelper
import org.eclipse.xtext.testing.InjectWith
import org.eclipse.xtext.testing.XtextRunner
import org.eclipse.xtext.ui.editor.hyperlinking.XtextHyperlink
import org.eclipse.xtext.ui.testing.AbstractHyperlinkingTest
import org.eclipse.xtext.xbase.XAbstractFeatureCall
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(XtextRunner)
@InjectWith(EdeltaUiInjectorProvider)
class EdeltaHyperlinkingTest extends AbstractHyperlinkingTest {

	@Inject EdeltaPluginProjectHelper projectHelper

	override protected getFileName() {
		/*
		 * Better to put Edelta file in a source folder
		 */
		"src/" + super.getFileName()
	}

	@Before def void setup() {
		/*
		 * Edelta requires a plug-in project to run the interpreter
		 * with edelta.lib as dependency
		 */
		projectHelper.createEdeltaPluginProject(projectName)
	}

	/**
	 * If we link to an XExpression its qualified name is null,
	 * and this leads to a NPE
	 */
	override protected _target(XtextHyperlink hyperlink) {
		val resourceSet = resourceSetProvider.get(project)
		val eObject = resourceSet.getEObject(hyperlink.URI, true)
		switch (eObject) {
			XAbstractFeatureCall: eObject.toString
			default: eObject.fullyQualifiedName.toString
		}
	}

	@Test def hyperlinkOnExistingEClass() {
		'''
		metamodel "mypackage"

		modifyEcore aTest epackage mypackage {
			ecoreref(«c»MyClass«c»)
		}
		'''
		.hasHyperlinkTo("mypackage.MyClass")
	}

	@Test def hyperlinkOnCreatedEClass() {
		'''
		metamodel "mypackage"

		modifyEcore aTest epackage mypackage {
			addNewEClass("NewClass")
			ecoreref(«c»NewClass«c»)
		}
		'''
		.hasHyperlinkTo("addNewEClass(<XStringLiteralImpl>)")
	}

	@Test def hyperlinkOnRenamedEClass() {
		'''
		metamodel "mypackage"

		modifyEcore aTest epackage mypackage {
			ecoreref(MyClass).name = "Renamed"
			ecoreref(«c»Renamed«c»)
		}
		'''
		.hasHyperlinkTo("<EdeltaEcoreReferenceExpressionImpl>.name = <XStringLiteralImpl>")
	}

}
