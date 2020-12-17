package edelta.tests;

import com.google.inject.Inject;
import edelta.compiler.EdeltaCompilerUtil;
import edelta.edelta.EdeltaEcoreReferenceExpression;
import edelta.tests.EdeltaAbstractTest;
import edelta.tests.EdeltaInjectorProviderCustom;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EcoreFactory;
import org.eclipse.xtend2.lib.StringConcatenation;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.XtextRunner;
import org.eclipse.xtext.xbase.lib.Extension;
import org.eclipse.xtext.xbase.lib.ObjectExtensions;
import org.eclipse.xtext.xbase.lib.Procedures.Procedure1;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(XtextRunner.class)
@InjectWith(EdeltaInjectorProviderCustom.class)
@SuppressWarnings("all")
public class EdeltaCompilerUtilTest extends EdeltaAbstractTest {
  @Inject
  @Extension
  private EdeltaCompilerUtil _edeltaCompilerUtil;
  
  @Test
  public void testGetEPackageNameOrNull() {
    final EcoreFactory factory = EcoreFactory.eINSTANCE;
    Assert.assertNull(this._edeltaCompilerUtil.getEPackageNameOrNull(null));
    EPackage _createEPackage = factory.createEPackage();
    final Procedure1<EPackage> _function = (EPackage it) -> {
      it.setName("test");
    };
    final EPackage p = ObjectExtensions.<EPackage>operator_doubleArrow(_createEPackage, _function);
    Assert.assertEquals("test", this._edeltaCompilerUtil.getEPackageNameOrNull(p));
  }
  
  @Test
  public void testGetStringForEcoreReferenceExpressionEClass() {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("ecoreref(FooClass)");
    EdeltaEcoreReferenceExpression _ecoreReferenceExpression = this.ecoreReferenceExpression(_builder);
    final Procedure1<EdeltaEcoreReferenceExpression> _function = (EdeltaEcoreReferenceExpression it) -> {
      Assert.assertEquals("getEClass(\"foo\", \"FooClass\")", this._edeltaCompilerUtil.getStringForEcoreReferenceExpression(it));
    };
    ObjectExtensions.<EdeltaEcoreReferenceExpression>operator_doubleArrow(_ecoreReferenceExpression, _function);
  }
  
  @Test
  public void testGetStringForEcoreReferenceExpressionEAttribute() {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("ecoreref(myAttribute)");
    EdeltaEcoreReferenceExpression _ecoreReferenceExpression = this.ecoreReferenceExpression(_builder);
    final Procedure1<EdeltaEcoreReferenceExpression> _function = (EdeltaEcoreReferenceExpression it) -> {
      Assert.assertEquals("getEAttribute(\"foo\", \"FooClass\", \"myAttribute\")", this._edeltaCompilerUtil.getStringForEcoreReferenceExpression(it));
    };
    ObjectExtensions.<EdeltaEcoreReferenceExpression>operator_doubleArrow(_ecoreReferenceExpression, _function);
  }
  
  @Test
  public void testGetStringForEcoreReferenceExpressionEEnumLiteral() {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("ecoreref(FooEnumLiteral)");
    EdeltaEcoreReferenceExpression _ecoreReferenceExpression = this.ecoreReferenceExpression(_builder);
    final Procedure1<EdeltaEcoreReferenceExpression> _function = (EdeltaEcoreReferenceExpression it) -> {
      Assert.assertEquals("getEEnumLiteral(\"foo\", \"FooEnum\", \"FooEnumLiteral\")", this._edeltaCompilerUtil.getStringForEcoreReferenceExpression(it));
    };
    ObjectExtensions.<EdeltaEcoreReferenceExpression>operator_doubleArrow(_ecoreReferenceExpression, _function);
  }
  
  @Test
  public void testGetStringForEcoreReferenceExpressionEPackage() {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("ecoreref(foo)");
    EdeltaEcoreReferenceExpression _ecoreReferenceExpression = this.ecoreReferenceExpression(_builder);
    final Procedure1<EdeltaEcoreReferenceExpression> _function = (EdeltaEcoreReferenceExpression it) -> {
      Assert.assertEquals("getEPackage(\"foo\")", this._edeltaCompilerUtil.getStringForEcoreReferenceExpression(it));
    };
    ObjectExtensions.<EdeltaEcoreReferenceExpression>operator_doubleArrow(_ecoreReferenceExpression, _function);
  }
  
  @Test
  public void testGetStringForEcoreReferenceExpressionIncomplete() {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("ecoreref");
    EdeltaEcoreReferenceExpression _ecoreReferenceExpression = this.ecoreReferenceExpression(_builder);
    final Procedure1<EdeltaEcoreReferenceExpression> _function = (EdeltaEcoreReferenceExpression it) -> {
      Assert.assertEquals("null", this._edeltaCompilerUtil.getStringForEcoreReferenceExpression(it));
    };
    ObjectExtensions.<EdeltaEcoreReferenceExpression>operator_doubleArrow(_ecoreReferenceExpression, _function);
  }
  
  @Test
  public void testGetStringForEcoreReferenceExpressionIncomplete2() {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("ecoreref()");
    EdeltaEcoreReferenceExpression _ecoreReferenceExpression = this.ecoreReferenceExpression(_builder);
    final Procedure1<EdeltaEcoreReferenceExpression> _function = (EdeltaEcoreReferenceExpression it) -> {
      Assert.assertEquals("getENamedElement()", this._edeltaCompilerUtil.getStringForEcoreReferenceExpression(it));
    };
    ObjectExtensions.<EdeltaEcoreReferenceExpression>operator_doubleArrow(_ecoreReferenceExpression, _function);
  }
  
  @Test
  public void testGetStringForEcoreReferenceExpressionUnresolved() {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("ecoreref(NonExistant)");
    EdeltaEcoreReferenceExpression _ecoreReferenceExpression = this.ecoreReferenceExpression(_builder);
    final Procedure1<EdeltaEcoreReferenceExpression> _function = (EdeltaEcoreReferenceExpression it) -> {
      Assert.assertEquals("getENamedElement(\"\", \"\", \"\")", this._edeltaCompilerUtil.getStringForEcoreReferenceExpression(it));
    };
    ObjectExtensions.<EdeltaEcoreReferenceExpression>operator_doubleArrow(_ecoreReferenceExpression, _function);
  }
  
  @Test
  public void testGetStringForEcoreReferenceExpressionEAttributeInSubPackage() {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("metamodel \"mainpackage\"");
    _builder.newLine();
    _builder.newLine();
    _builder.append("modifyEcore aTest epackage mainpackage {");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("ecoreref(mySubPackageAttribute)");
    _builder.newLine();
    _builder.append("}");
    _builder.newLine();
    EdeltaEcoreReferenceExpression _lastEcoreReferenceExpression = this.lastEcoreReferenceExpression(this.parseWithTestEcoreWithSubPackage(_builder));
    final Procedure1<EdeltaEcoreReferenceExpression> _function = (EdeltaEcoreReferenceExpression it) -> {
      Assert.assertEquals("getEAttribute(\"mainpackage.mainsubpackage\", \"MainSubPackageFooClass\", \"mySubPackageAttribute\")", this._edeltaCompilerUtil.getStringForEcoreReferenceExpression(it));
    };
    ObjectExtensions.<EdeltaEcoreReferenceExpression>operator_doubleArrow(_lastEcoreReferenceExpression, _function);
  }
}
