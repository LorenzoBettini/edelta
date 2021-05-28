package edelta.ui.tests;

import edelta.ui.tests.utils.ProjectImportUtil;
import org.eclipse.core.resources.IFile;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.xtend2.lib.StringConcatenation;
import org.eclipse.xtext.resource.XtextResource;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.XtextRunner;
import org.eclipse.xtext.ui.editor.XtextEditor;
import org.eclipse.xtext.ui.editor.hyperlinking.XtextHyperlink;
import org.eclipse.xtext.ui.editor.model.IXtextDocument;
import org.eclipse.xtext.ui.testing.AbstractHyperlinkingTest;
import org.eclipse.xtext.ui.testing.AbstractWorkbenchTest;
import org.eclipse.xtext.ui.testing.util.IResourcesSetupUtil;
import org.eclipse.xtext.util.concurrent.IUnitOfWork;
import org.eclipse.xtext.xbase.XAbstractFeatureCall;
import org.eclipse.xtext.xbase.lib.Exceptions;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(XtextRunner.class)
@InjectWith(EdeltaUiInjectorProvider.class)
@SuppressWarnings("all")
public class EdeltaHyperlinkingTest extends AbstractHyperlinkingTest {
  private static final String TEST_PROJECT = "edelta.ui.tests.project";
  
  private XtextEditor xtextEditor;
  
  /**
   * Edelta requires a plug-in project to run the interpreter
   * with edelta.lib as dependency
   */
  @BeforeClass
  public static void importProject() {
    try {
      ProjectImportUtil.importProject(EdeltaHyperlinkingTest.TEST_PROJECT);
      IResourcesSetupUtil.waitForBuild();
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }
  
  /**
   * Avoids deleting project
   */
  @Override
  public void setUp() {
  }
  
  /**
   * Avoids deleting project
   */
  @Override
  public void tearDown() {
    this.waitForEventProcessing();
    AbstractWorkbenchTest.closeEditors();
    this.waitForEventProcessing();
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
    String _fileName = super.getFileName();
    return ("src/" + _fileName);
  }
  
  @Override
  protected XtextEditor openInEditor(final IFile dslFile) {
    this.xtextEditor = super.openInEditor(dslFile);
    return this.xtextEditor;
  }
  
  /**
   * If we link to an XExpression its qualified name is null,
   * and this leads to a NPE.
   * 
   * Don't use resourceSetProvider, because Java types would not be resolved
   * resourceSetProvider.get(project)
   */
  @Override
  protected String _target(final XtextHyperlink hyperlink) {
    final IXtextDocument document = this.xtextDocumentUtil.getXtextDocument(this.xtextEditor.getInternalSourceViewer());
    final IUnitOfWork<XtextResource, XtextResource> _function = (XtextResource it) -> {
      return it;
    };
    final XtextResource resource = document.<XtextResource>readOnly(_function);
    final ResourceSet resourceSet = resource.getResourceSet();
    final EObject eObject = resourceSet.getEObject(hyperlink.getURI(), true);
    if ((eObject instanceof XAbstractFeatureCall)) {
      return ((XAbstractFeatureCall)eObject).getFeature().getSimpleName();
    }
    return this.qualifiedNameProvider.getFullyQualifiedName(eObject).toString();
  }
  
  @Test
  public void hyperlinkOnExistingEClass() {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("metamodel \"mypackage\"");
    _builder.newLine();
    _builder.newLine();
    _builder.append("modifyEcore aTest epackage mypackage {");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("ecoreref(<|>MyClass<|>)");
    _builder.newLine();
    _builder.append("}");
    _builder.newLine();
    this.hasHyperlinkTo(_builder, "mypackage.MyClass");
  }
  
  @Test
  public void hyperlinkOnQualifiedPart() {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("metamodel \"mypackage\"");
    _builder.newLine();
    _builder.newLine();
    _builder.append("modifyEcore aTest epackage mypackage {");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("ecoreref(<|>mypackage<|>.MyClass)");
    _builder.newLine();
    _builder.append("}");
    _builder.newLine();
    this.hasHyperlinkTo(_builder, "mypackage");
  }
  
  @Test
  public void hyperlinkOnEPackage() {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("metamodel \"mypackage\"");
    _builder.newLine();
    _builder.newLine();
    _builder.append("modifyEcore aTest epackage <|>mypackage<|> {");
    _builder.newLine();
    _builder.append("}");
    _builder.newLine();
    this.hasHyperlinkTo(_builder, "mypackage");
  }
  
  @Test
  public void hyperlinkOnCreatedEClass() {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("metamodel \"mypackage\"");
    _builder.newLine();
    _builder.newLine();
    _builder.append("modifyEcore aTest epackage mypackage {");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("addNewEClass(\"NewClass\")");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("ecoreref(<|>NewClass<|>)");
    _builder.newLine();
    _builder.append("}");
    _builder.newLine();
    this.hasHyperlinkTo(_builder, "addNewEClass");
  }
  
  @Test
  public void hyperlinkOnRenamedEClass() {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("metamodel \"mypackage\"");
    _builder.newLine();
    _builder.newLine();
    _builder.append("modifyEcore aTest epackage mypackage {");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("ecoreref(MyClass).name = \"Renamed\"");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("ecoreref(<|>Renamed<|>)");
    _builder.newLine();
    _builder.append("}");
    _builder.newLine();
    this.hasHyperlinkTo(_builder, "setName");
  }
  
  @Test
  public void hyperlinkOnForwardCreatedEClass() {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("metamodel \"mypackage\"");
    _builder.newLine();
    _builder.newLine();
    _builder.append("modifyEcore aTest epackage mypackage {");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("ecoreref(<|>NewClass<|>)");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("addNewEClass(\"NewClass\")");
    _builder.newLine();
    _builder.append("}");
    _builder.newLine();
    this.hasHyperlinkTo(_builder, "addNewEClass");
  }
}
