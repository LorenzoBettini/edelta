package edelta.tests;

import com.google.inject.Inject;
import edelta.edelta.EdeltaProgram;
import edelta.interpreter.EdeltaInterpreterDiagnosticHelper;
import edelta.resource.derivedstate.EdeltaDerivedStateHelper;
import edelta.tests.EdeltaAbstractTest;
import edelta.tests.EdeltaInjectorProviderCustom;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.xtend2.lib.StringConcatenation;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.XtextRunner;
import org.eclipse.xtext.xbase.XExpression;
import org.eclipse.xtext.xbase.XbasePackage;
import org.eclipse.xtext.xbase.lib.IterableExtensions;
import org.eclipse.xtext.xbase.lib.ObjectExtensions;
import org.eclipse.xtext.xbase.lib.Procedures.Procedure1;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(XtextRunner.class)
@InjectWith(EdeltaInjectorProviderCustom.class)
@SuppressWarnings("all")
public class EdeltaInterpreterDiagnosticHelperTest extends EdeltaAbstractTest {
  @Inject
  private EdeltaInterpreterDiagnosticHelper diagnosticHelper;
  
  @Inject
  private EdeltaDerivedStateHelper derivedStateHelper;
  
  @Test
  public void testAddErrorWithCurrentExpression() throws Exception {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("metamodel \"foo\"");
    _builder.newLine();
    _builder.newLine();
    _builder.append("modifyEcore aTest epackage foo {");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("val s = null");
    _builder.newLine();
    _builder.append("}");
    _builder.newLine();
    final String input = _builder.toString();
    EdeltaProgram _parseWithTestEcore = this.parseWithTestEcore(input);
    final Procedure1<EdeltaProgram> _function = (EdeltaProgram it) -> {
      final XExpression exp = this.getBlockLastExpression(this.lastModifyEcoreOperation(it).getBody());
      this.diagnosticHelper.setCurrentExpression(exp);
      this.diagnosticHelper.addError(null, "issueCode", "an error");
      this.validationTestHelper.assertError(it, 
        XbasePackage.Literals.XVARIABLE_DECLARATION, 
        "issueCode", 
        input.lastIndexOf("val s = null"), 
        "val s = null".length(), 
        "an error");
    };
    ObjectExtensions.<EdeltaProgram>operator_doubleArrow(_parseWithTestEcore, _function);
  }
  
  @Test
  public void testAddErrorWithDifferentCorrespondingExpression() throws Exception {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("metamodel \"foo\"");
    _builder.newLine();
    _builder.newLine();
    _builder.append("modifyEcore aTest epackage foo {");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("var s = null");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("s = null");
    _builder.newLine();
    _builder.append("}");
    _builder.newLine();
    final String input = _builder.toString();
    EdeltaProgram _parseWithTestEcore = this.parseWithTestEcore(input);
    final Procedure1<EdeltaProgram> _function = (EdeltaProgram it) -> {
      final EPackage problematic = IterableExtensions.<EPackage>last(this.getCopiedEPackages(it));
      final XExpression correspondingExpression = this.getBlockFirstExpression(this.lastModifyEcoreOperation(it).getBody());
      this.derivedStateHelper.getEnamedElementXExpressionMap(it.eResource()).put(problematic, correspondingExpression);
      final XExpression currentExpression = this.getBlockLastExpression(this.lastModifyEcoreOperation(it).getBody());
      this.diagnosticHelper.setCurrentExpression(currentExpression);
      this.diagnosticHelper.addError(problematic, "issueCode", "an error");
      this.validationTestHelper.assertError(it, 
        XbasePackage.Literals.XVARIABLE_DECLARATION, 
        "issueCode", 
        input.lastIndexOf("val s = null"), 
        "val s = null".length(), 
        "an error");
    };
    ObjectExtensions.<EdeltaProgram>operator_doubleArrow(_parseWithTestEcore, _function);
  }
  
  @Test
  public void testAddWarningWithCurrentExpression() throws Exception {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("metamodel \"foo\"");
    _builder.newLine();
    _builder.newLine();
    _builder.append("modifyEcore aTest epackage foo {");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("val s = null");
    _builder.newLine();
    _builder.append("}");
    _builder.newLine();
    final String input = _builder.toString();
    EdeltaProgram _parseWithTestEcore = this.parseWithTestEcore(input);
    final Procedure1<EdeltaProgram> _function = (EdeltaProgram it) -> {
      final XExpression exp = this.getBlockLastExpression(this.lastModifyEcoreOperation(it).getBody());
      this.diagnosticHelper.setCurrentExpression(exp);
      this.diagnosticHelper.addWarning(null, "issueCode", "a warning");
      this.validationTestHelper.assertWarning(it, 
        XbasePackage.Literals.XVARIABLE_DECLARATION, 
        "issueCode", 
        input.lastIndexOf("val s = null"), 
        "val s = null".length(), 
        "a warning");
    };
    ObjectExtensions.<EdeltaProgram>operator_doubleArrow(_parseWithTestEcore, _function);
  }
  
  @Test
  public void testAddWarningWithDifferentCorrespondingExpression() throws Exception {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("metamodel \"foo\"");
    _builder.newLine();
    _builder.newLine();
    _builder.append("modifyEcore aTest epackage foo {");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("var s = null");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("s = null");
    _builder.newLine();
    _builder.append("}");
    _builder.newLine();
    final String input = _builder.toString();
    EdeltaProgram _parseWithTestEcore = this.parseWithTestEcore(input);
    final Procedure1<EdeltaProgram> _function = (EdeltaProgram it) -> {
      final EPackage problematic = IterableExtensions.<EPackage>last(this.getCopiedEPackages(it));
      final XExpression correspondingExpression = this.getBlockFirstExpression(this.lastModifyEcoreOperation(it).getBody());
      this.derivedStateHelper.getEnamedElementXExpressionMap(it.eResource()).put(problematic, correspondingExpression);
      final XExpression currentExpression = this.getBlockLastExpression(this.lastModifyEcoreOperation(it).getBody());
      this.diagnosticHelper.setCurrentExpression(currentExpression);
      this.diagnosticHelper.addWarning(problematic, "issueCode", "a warning");
      this.validationTestHelper.assertWarning(it, 
        XbasePackage.Literals.XVARIABLE_DECLARATION, 
        "issueCode", 
        input.lastIndexOf("val s = null"), 
        "val s = null".length(), 
        "a warning");
    };
    ObjectExtensions.<EdeltaProgram>operator_doubleArrow(_parseWithTestEcore, _function);
  }
}
