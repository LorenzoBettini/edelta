package edelta.tests;

import com.google.inject.Inject;
import edelta.edelta.EdeltaProgram;
import edelta.interpreter.EdeltaInterpreterEdeltaImpl;
import edelta.resource.derivedstate.EdeltaDerivedStateHelper;
import edelta.tests.EdeltaAbstractTest;
import edelta.tests.EdeltaInjectorProviderCustom;
import edelta.validation.EdeltaValidator;
import java.util.Collections;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EcoreFactory;
import org.eclipse.xtend2.lib.StringConcatenation;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.XtextRunner;
import org.eclipse.xtext.xbase.XExpression;
import org.eclipse.xtext.xbase.XbasePackage;
import org.eclipse.xtext.xbase.lib.CollectionLiterals;
import org.eclipse.xtext.xbase.lib.IterableExtensions;
import org.eclipse.xtext.xbase.lib.ObjectExtensions;
import org.eclipse.xtext.xbase.lib.Procedures.Procedure1;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

@RunWith(XtextRunner.class)
@InjectWith(EdeltaInjectorProviderCustom.class)
@SuppressWarnings("all")
public class EdeltaInterpreterEdeltaImplTest extends EdeltaAbstractTest {
  private EdeltaInterpreterEdeltaImpl edelta;
  
  @Inject
  private EdeltaDerivedStateHelper derivedStateHelper;
  
  @Before
  public void setup() {
    EdeltaInterpreterEdeltaImpl _edeltaInterpreterEdeltaImpl = new EdeltaInterpreterEdeltaImpl(Collections.<EPackage>unmodifiableList(CollectionLiterals.<EPackage>newArrayList()), this.derivedStateHelper);
    this.edelta = _edeltaInterpreterEdeltaImpl;
    this.edelta.setLogger(Mockito.<Logger>spy(this.edelta.getLogger()));
  }
  
  @Test
  public void testFirstEPackageHasPrecedence() {
    EPackage _createEPackage = EcoreFactory.eINSTANCE.createEPackage();
    final Procedure1<EPackage> _function = (EPackage it) -> {
      it.setName("Test");
    };
    final EPackage p1 = ObjectExtensions.<EPackage>operator_doubleArrow(_createEPackage, _function);
    EPackage _createEPackage_1 = EcoreFactory.eINSTANCE.createEPackage();
    final Procedure1<EPackage> _function_1 = (EPackage it) -> {
      it.setName("Test");
    };
    final EPackage p2 = ObjectExtensions.<EPackage>operator_doubleArrow(_createEPackage_1, _function_1);
    EdeltaInterpreterEdeltaImpl _edeltaInterpreterEdeltaImpl = new EdeltaInterpreterEdeltaImpl(Collections.<EPackage>unmodifiableList(CollectionLiterals.<EPackage>newArrayList(p1, p2)), this.derivedStateHelper);
    this.edelta = _edeltaInterpreterEdeltaImpl;
    Assert.assertSame(p1, this.edelta.getEPackage("Test"));
  }
  
  @Test
  public void testShowErrorWithNullCurrentExpression() {
    this.edelta.showError(null, "an error");
    Mockito.<Logger>verify(this.edelta.getLogger()).log(Level.ERROR, ": an error");
  }
  
  @Test
  public void testShowErrorWithCurrentExpression() {
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
      this.edelta.setCurrentExpression(exp);
      this.edelta.showError(null, "an error");
      this._validationTestHelper.assertError(it, 
        XbasePackage.Literals.XVARIABLE_DECLARATION, 
        EdeltaValidator.LIVE_VALIDATION_ERROR, 
        input.lastIndexOf("val s = null"), 
        "val s = null".length(), 
        "an error");
    };
    ObjectExtensions.<EdeltaProgram>operator_doubleArrow(_parseWithTestEcore, _function);
  }
  
  @Test
  public void testShowErrorWithDifferentCorrespondingExpression() {
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
      this.edelta.setCurrentExpression(currentExpression);
      this.edelta.showError(problematic, "an error");
      this._validationTestHelper.assertError(it, 
        XbasePackage.Literals.XVARIABLE_DECLARATION, 
        EdeltaValidator.LIVE_VALIDATION_ERROR, 
        input.lastIndexOf("val s = null"), 
        "val s = null".length(), 
        "an error");
    };
    ObjectExtensions.<EdeltaProgram>operator_doubleArrow(_parseWithTestEcore, _function);
  }
  
  @Test
  public void testShowWarningWithNullCurrentExpression() {
    this.edelta.showWarning(null, "an error");
    Mockito.<Logger>verify(this.edelta.getLogger()).log(Level.WARN, ": an error");
  }
  
  @Test
  public void testShowWarningWithCurrentExpression() {
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
      this.edelta.setCurrentExpression(exp);
      this.edelta.showWarning(null, "a warning");
      this._validationTestHelper.assertWarning(it, 
        XbasePackage.Literals.XVARIABLE_DECLARATION, 
        EdeltaValidator.LIVE_VALIDATION_WARNING, 
        input.lastIndexOf("val s = null"), 
        "val s = null".length(), 
        "a warning");
    };
    ObjectExtensions.<EdeltaProgram>operator_doubleArrow(_parseWithTestEcore, _function);
  }
  
  @Test
  public void testShowWarningWithDifferentCorrespondingExpression() {
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
      this.edelta.setCurrentExpression(currentExpression);
      this.edelta.showWarning(problematic, "a warning");
      this._validationTestHelper.assertWarning(it, 
        XbasePackage.Literals.XVARIABLE_DECLARATION, 
        EdeltaValidator.LIVE_VALIDATION_WARNING, 
        input.lastIndexOf("val s = null"), 
        "val s = null".length(), 
        "a warning");
    };
    ObjectExtensions.<EdeltaProgram>operator_doubleArrow(_parseWithTestEcore, _function);
  }
}
