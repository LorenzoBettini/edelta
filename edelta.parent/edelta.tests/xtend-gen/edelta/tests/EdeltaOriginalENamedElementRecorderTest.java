package edelta.tests;

import com.google.inject.Inject;
import edelta.edelta.EdeltaEcoreDirectReference;
import edelta.edelta.EdeltaEcoreQualifiedReference;
import edelta.edelta.EdeltaEcoreReference;
import edelta.edelta.EdeltaFactory;
import edelta.edelta.EdeltaProgram;
import edelta.scoping.EdeltaOriginalENamedElementRecorder;
import edelta.tests.EdeltaAbstractTest;
import edelta.tests.EdeltaInjectorProviderDerivedStateComputerWithoutInterpreter;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EEnumLiteral;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.xtend2.lib.StringConcatenation;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.XtextRunner;
import org.eclipse.xtext.xbase.lib.Extension;
import org.eclipse.xtext.xbase.lib.IterableExtensions;
import org.eclipse.xtext.xbase.lib.ObjectExtensions;
import org.eclipse.xtext.xbase.lib.Procedures.Procedure1;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(XtextRunner.class)
@InjectWith(EdeltaInjectorProviderDerivedStateComputerWithoutInterpreter.class)
@SuppressWarnings("all")
public class EdeltaOriginalENamedElementRecorderTest extends EdeltaAbstractTest {
  @Inject
  @Extension
  private EdeltaOriginalENamedElementRecorder _edeltaOriginalENamedElementRecorder;
  
  @Test
  public void testNull() {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("metamodel \"foo\"");
    _builder.newLine();
    EdeltaProgram _parseWithTestEcore = this.parseWithTestEcore(_builder);
    final Procedure1<EdeltaProgram> _function = (EdeltaProgram it) -> {
      this._edeltaOriginalENamedElementRecorder.recordOriginalENamedElement(null);
    };
    ObjectExtensions.<EdeltaProgram>operator_doubleArrow(_parseWithTestEcore, _function);
  }
  
  @Test
  public void testNullENamedElement() {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("metamodel \"foo\"");
    _builder.newLine();
    EdeltaProgram _parseWithTestEcore = this.parseWithTestEcore(_builder);
    final Procedure1<EdeltaProgram> _function = (EdeltaProgram it) -> {
      final EdeltaEcoreDirectReference ref = EdeltaFactory.eINSTANCE.createEdeltaEcoreDirectReference();
      this._edeltaOriginalENamedElementRecorder.recordOriginalENamedElement(ref);
      Assert.assertNull(ref.getOriginalEnamedelement());
    };
    ObjectExtensions.<EdeltaProgram>operator_doubleArrow(_parseWithTestEcore, _function);
  }
  
  @Test
  public void testUnresolvedENamedElement() {
    final EdeltaEcoreReference ref = this.ecoreReferenceExpression("ecoreref(NonExistant)").getReference();
    this._edeltaOriginalENamedElementRecorder.recordOriginalENamedElement(ref);
    Assert.assertNull(ref.getOriginalEnamedelement());
  }
  
  @Test
  public void testEClassifierDirectReference() {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("metamodel \"foo\"");
    _builder.newLine();
    _builder.newLine();
    _builder.append("modifyEcore aTest epackage foo {");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("ecoreref(FooClass)");
    _builder.newLine();
    _builder.append("}");
    _builder.newLine();
    EdeltaProgram _parseWithTestEcore = this.parseWithTestEcore(_builder);
    final Procedure1<EdeltaProgram> _function = (EdeltaProgram it) -> {
      final EdeltaEcoreReference ref = this.lastEcoreReferenceExpression(it).getReference();
      this._edeltaOriginalENamedElementRecorder.recordOriginalENamedElement(ref);
      final EClassifier original = this.getEClassiferByName(IterableExtensions.<EPackage>last(it.getMetamodels()), "FooClass");
      Assert.assertSame(original, ref.getOriginalEnamedelement());
    };
    ObjectExtensions.<EdeltaProgram>operator_doubleArrow(_parseWithTestEcore, _function);
  }
  
  @Test
  public void testEClassifierQualifiedReference() {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("metamodel \"foo\"");
    _builder.newLine();
    _builder.newLine();
    _builder.append("modifyEcore aTest epackage foo {");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("ecoreref(foo.FooClass)");
    _builder.newLine();
    _builder.append("}");
    _builder.newLine();
    EdeltaProgram _parseWithTestEcore = this.parseWithTestEcore(_builder);
    final Procedure1<EdeltaProgram> _function = (EdeltaProgram it) -> {
      final EdeltaEcoreQualifiedReference ref = this.getEdeltaEcoreQualifiedReference(this.lastEcoreReferenceExpression(it).getReference());
      this._edeltaOriginalENamedElementRecorder.recordOriginalENamedElement(ref);
      final EClassifier original = this.getEClassiferByName(IterableExtensions.<EPackage>last(it.getMetamodels()), "FooClass");
      Assert.assertSame(original, ref.getOriginalEnamedelement());
      final EPackage originalPackage = IterableExtensions.<EPackage>last(it.getMetamodels());
      Assert.assertSame(originalPackage, ref.getQualification().getOriginalEnamedelement());
    };
    ObjectExtensions.<EdeltaProgram>operator_doubleArrow(_parseWithTestEcore, _function);
  }
  
  @Test
  public void testCreatedEClassifierDirectReference() {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("metamodel \"foo\"");
    _builder.newLine();
    _builder.newLine();
    _builder.append("modifyEcore aTest epackage foo {");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("addNewEClass(\"NewClass\")");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("ecoreref(NewClass)");
    _builder.newLine();
    _builder.append("}");
    _builder.newLine();
    EdeltaProgram _parseWithTestEcore = this.parseWithTestEcore(_builder);
    final Procedure1<EdeltaProgram> _function = (EdeltaProgram it) -> {
      final EdeltaEcoreReference ref = this.lastEcoreReferenceExpression(it).getReference();
      this._edeltaOriginalENamedElementRecorder.recordOriginalENamedElement(ref);
      Assert.assertNull(ref.getOriginalEnamedelement());
    };
    ObjectExtensions.<EdeltaProgram>operator_doubleArrow(_parseWithTestEcore, _function);
  }
  
  @Test
  public void testCreatedEClassifierQualifiedReference() {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("metamodel \"foo\"");
    _builder.newLine();
    _builder.newLine();
    _builder.append("modifyEcore aTest epackage foo {");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("addNewEClass(\"NewClass\")");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("ecoreref(foo.NewClass)");
    _builder.newLine();
    _builder.append("}");
    _builder.newLine();
    EdeltaProgram _parseWithTestEcore = this.parseWithTestEcore(_builder);
    final Procedure1<EdeltaProgram> _function = (EdeltaProgram it) -> {
      final EdeltaEcoreQualifiedReference ref = this.getEdeltaEcoreQualifiedReference(this.lastEcoreReferenceExpression(it).getReference());
      this._edeltaOriginalENamedElementRecorder.recordOriginalENamedElement(ref);
      Assert.assertNull(ref.getOriginalEnamedelement());
      final EPackage originalPackage = IterableExtensions.<EPackage>last(it.getMetamodels());
      Assert.assertSame(originalPackage, ref.getQualification().getOriginalEnamedelement());
    };
    ObjectExtensions.<EdeltaProgram>operator_doubleArrow(_parseWithTestEcore, _function);
  }
  
  @Test
  public void testEStrucutralFeatureDirectReference() {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("metamodel \"foo\"");
    _builder.newLine();
    _builder.newLine();
    _builder.append("modifyEcore aTest epackage foo {");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("ecoreref(myAttribute)");
    _builder.newLine();
    _builder.append("}");
    _builder.newLine();
    EdeltaProgram _parseWithTestEcore = this.parseWithTestEcore(_builder);
    final Procedure1<EdeltaProgram> _function = (EdeltaProgram it) -> {
      final EdeltaEcoreReference ref = this.lastEcoreReferenceExpression(it).getReference();
      this._edeltaOriginalENamedElementRecorder.recordOriginalENamedElement(ref);
      final EStructuralFeature original = this.getEStructuralFeatureByName(this.getEClassiferByName(IterableExtensions.<EPackage>last(it.getMetamodels()), "FooClass"), "myAttribute");
      Assert.assertSame(original, ref.getOriginalEnamedelement());
    };
    ObjectExtensions.<EdeltaProgram>operator_doubleArrow(_parseWithTestEcore, _function);
  }
  
  @Test
  public void testEStrucutralFeatureQualifiedReference() {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("metamodel \"foo\"");
    _builder.newLine();
    _builder.newLine();
    _builder.append("modifyEcore aTest epackage foo {");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("ecoreref(FooClass.myAttribute)");
    _builder.newLine();
    _builder.append("}");
    _builder.newLine();
    EdeltaProgram _parseWithTestEcore = this.parseWithTestEcore(_builder);
    final Procedure1<EdeltaProgram> _function = (EdeltaProgram it) -> {
      final EdeltaEcoreQualifiedReference ref = this.getEdeltaEcoreQualifiedReference(this.lastEcoreReferenceExpression(it).getReference());
      this._edeltaOriginalENamedElementRecorder.recordOriginalENamedElement(ref);
      final EClassifier originalEClass = this.getEClassiferByName(IterableExtensions.<EPackage>last(it.getMetamodels()), "FooClass");
      Assert.assertSame(originalEClass, ref.getQualification().getOriginalEnamedelement());
      final EStructuralFeature original = this.getEStructuralFeatureByName(originalEClass, "myAttribute");
      Assert.assertSame(original, ref.getOriginalEnamedelement());
    };
    ObjectExtensions.<EdeltaProgram>operator_doubleArrow(_parseWithTestEcore, _function);
  }
  
  @Test
  public void testEStrucutralFeatureFullyQualifiedReference() {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("metamodel \"foo\"");
    _builder.newLine();
    _builder.newLine();
    _builder.append("modifyEcore aTest epackage foo {");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("ecoreref(foo.FooClass.myAttribute)");
    _builder.newLine();
    _builder.append("}");
    _builder.newLine();
    EdeltaProgram _parseWithTestEcore = this.parseWithTestEcore(_builder);
    final Procedure1<EdeltaProgram> _function = (EdeltaProgram it) -> {
      final EdeltaEcoreQualifiedReference ref = this.getEdeltaEcoreQualifiedReference(this.lastEcoreReferenceExpression(it).getReference());
      this._edeltaOriginalENamedElementRecorder.recordOriginalENamedElement(ref);
      final EPackage originalPackage = IterableExtensions.<EPackage>last(it.getMetamodels());
      Assert.assertSame(originalPackage, 
        this.getEdeltaEcoreQualifiedReference(ref.getQualification()).getQualification().getOriginalEnamedelement());
      final EClassifier originalEClass = this.getEClassiferByName(originalPackage, "FooClass");
      Assert.assertSame(originalEClass, ref.getQualification().getOriginalEnamedelement());
      final EStructuralFeature original = this.getEStructuralFeatureByName(originalEClass, "myAttribute");
      Assert.assertSame(original, ref.getOriginalEnamedelement());
    };
    ObjectExtensions.<EdeltaProgram>operator_doubleArrow(_parseWithTestEcore, _function);
  }
  
  @Test
  public void testEEnumLiteraDirectReference() {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("metamodel \"foo\"");
    _builder.newLine();
    _builder.newLine();
    _builder.append("modifyEcore aTest epackage foo {");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("ecoreref(FooEnumLiteral)");
    _builder.newLine();
    _builder.append("}");
    _builder.newLine();
    EdeltaProgram _parseWithTestEcore = this.parseWithTestEcore(_builder);
    final Procedure1<EdeltaProgram> _function = (EdeltaProgram it) -> {
      final EdeltaEcoreReference ref = this.lastEcoreReferenceExpression(it).getReference();
      this._edeltaOriginalENamedElementRecorder.recordOriginalENamedElement(ref);
      final EEnumLiteral original = this.getEEnumLiteralByName(this.getEClassiferByName(IterableExtensions.<EPackage>last(it.getMetamodels()), "FooEnum"), "FooEnumLiteral");
      Assert.assertSame(original, ref.getOriginalEnamedelement());
    };
    ObjectExtensions.<EdeltaProgram>operator_doubleArrow(_parseWithTestEcore, _function);
  }
}
