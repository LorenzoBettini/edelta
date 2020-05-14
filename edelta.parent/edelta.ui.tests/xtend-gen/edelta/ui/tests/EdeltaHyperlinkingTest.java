package edelta.ui.tests;

import com.google.inject.Inject;
import edelta.ui.tests.EdeltaUiInjectorProvider;
import edelta.ui.tests.utils.EdeltaPluginProjectHelper;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.xtend2.lib.StringConcatenation;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.XtextRunner;
import org.eclipse.xtext.ui.editor.hyperlinking.XtextHyperlink;
import org.eclipse.xtext.ui.testing.AbstractHyperlinkingTest;
import org.eclipse.xtext.xbase.XAbstractFeatureCall;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(XtextRunner.class)
@InjectWith(EdeltaUiInjectorProvider.class)
@SuppressWarnings("all")
public class EdeltaHyperlinkingTest extends AbstractHyperlinkingTest {
  @Inject
  private EdeltaPluginProjectHelper projectHelper;
  
  @Override
  protected String getFileName() {
    String _fileName = super.getFileName();
    return ("src/" + _fileName);
  }
  
  @Before
  public void setup() {
    this.projectHelper.createEdeltaPluginProject(this.getProjectName());
  }
  
  /**
   * If we link to an XExpression its qualified name is null,
   * and this leads to a NPE
   */
  @Override
  protected String _target(final XtextHyperlink hyperlink) {
    String _xblockexpression = null;
    {
      final ResourceSet resourceSet = this.resourceSetProvider.get(this.project);
      final EObject eObject = resourceSet.getEObject(hyperlink.getURI(), true);
      String _switchResult = null;
      boolean _matched = false;
      if (eObject instanceof XAbstractFeatureCall) {
        _matched=true;
        _switchResult = ((XAbstractFeatureCall)eObject).toString();
      }
      if (!_matched) {
        _switchResult = this._iQualifiedNameProvider.getFullyQualifiedName(eObject).toString();
      }
      _xblockexpression = _switchResult;
    }
    return _xblockexpression;
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
    _builder.append("ecoreref(");
    _builder.append(this.c, "\t");
    _builder.append("MyClass");
    _builder.append(this.c, "\t");
    _builder.append(")");
    _builder.newLineIfNotEmpty();
    _builder.append("}");
    _builder.newLine();
    this.hasHyperlinkTo(_builder, "mypackage.MyClass");
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
    _builder.append("ecoreref(");
    _builder.append(this.c, "\t");
    _builder.append("NewClass");
    _builder.append(this.c, "\t");
    _builder.append(")");
    _builder.newLineIfNotEmpty();
    _builder.append("}");
    _builder.newLine();
    this.hasHyperlinkTo(_builder, "addNewEClass(<XStringLiteralImpl>)");
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
    _builder.append("ecoreref(");
    _builder.append(this.c, "\t");
    _builder.append("Renamed");
    _builder.append(this.c, "\t");
    _builder.append(")");
    _builder.newLineIfNotEmpty();
    _builder.append("}");
    _builder.newLine();
    this.hasHyperlinkTo(_builder, "<EdeltaEcoreReferenceExpressionImpl>.name = <XStringLiteralImpl>");
  }
}
