package edelta.tests;

import com.google.inject.Inject;
import edelta.edelta.EdeltaEcoreDirectReference;
import edelta.edelta.EdeltaEcoreQualifiedReference;
import edelta.edelta.EdeltaEcoreReference;
import edelta.edelta.EdeltaFactory;
import edelta.edelta.EdeltaProgram;
import edelta.resource.derivedstate.EdeltaDerivedStateHelper;
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
import org.eclipse.xtext.xbase.lib.Exceptions;
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
  
  @Inject
  @Extension
  private EdeltaDerivedStateHelper _edeltaDerivedStateHelper;
  
  @Test
  public void testNull() {
    try {
      StringConcatenation _builder = new StringConcatenation();
      _builder.append("metamodel \"foo\"");
      _builder.newLine();
      EdeltaProgram _parseWithTestEcore = this.parseWithTestEcore(_builder);
      final Procedure1<EdeltaProgram> _function = (EdeltaProgram it) -> {
        this._edeltaOriginalENamedElementRecorder.recordOriginalENamedElement(null);
      };
      ObjectExtensions.<EdeltaProgram>operator_doubleArrow(_parseWithTestEcore, _function);
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }
  
  @Test
  public void testNullENamedElement() {
    try {
      StringConcatenation _builder = new StringConcatenation();
      _builder.append("metamodel \"foo\"");
      _builder.newLine();
      EdeltaProgram _parseWithTestEcore = this.parseWithTestEcore(_builder);
      final Procedure1<EdeltaProgram> _function = (EdeltaProgram it) -> {
        final EdeltaEcoreDirectReference ref = EdeltaFactory.eINSTANCE.createEdeltaEcoreDirectReference();
        this._edeltaOriginalENamedElementRecorder.recordOriginalENamedElement(ref);
        Assert.assertNull(this._edeltaDerivedStateHelper.getOriginalEnamedelement(ref));
      };
      ObjectExtensions.<EdeltaProgram>operator_doubleArrow(_parseWithTestEcore, _function);
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }
  
  @Test
  public void testUnresolvedENamedElement() {
    try {
      final EdeltaEcoreReference ref = this.ecoreReferenceExpression("ecoreref(NonExistant)").getReference();
      this._edeltaOriginalENamedElementRecorder.recordOriginalENamedElement(ref);
      Assert.assertNull(this._edeltaDerivedStateHelper.getOriginalEnamedelement(ref));
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }
  
  @Test
  public void testEClassifierDirectReference() {
    try {
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
        Assert.assertSame(original, this._edeltaDerivedStateHelper.getOriginalEnamedelement(ref));
      };
      ObjectExtensions.<EdeltaProgram>operator_doubleArrow(_parseWithTestEcore, _function);
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }
  
  @Test
  public void testSubPackageDirectReference() {
    try {
      StringConcatenation _builder = new StringConcatenation();
      _builder.append("metamodel \"mainpackage\"");
      _builder.newLine();
      _builder.newLine();
      _builder.append("modifyEcore aTest epackage mainpackage {");
      _builder.newLine();
      _builder.append("\t");
      _builder.append("ecoreref(mainsubpackage)");
      _builder.newLine();
      _builder.append("}");
      _builder.newLine();
      EdeltaProgram _parseWithTestEcoreWithSubPackage = this.parseWithTestEcoreWithSubPackage(_builder);
      final Procedure1<EdeltaProgram> _function = (EdeltaProgram it) -> {
        final EdeltaEcoreReference ref = this.lastEcoreReferenceExpression(it).getReference();
        this._edeltaOriginalENamedElementRecorder.recordOriginalENamedElement(ref);
        final EPackage original = IterableExtensions.<EPackage>head(IterableExtensions.<EPackage>last(it.getMetamodels()).getESubpackages());
        Assert.assertSame(original, this._edeltaDerivedStateHelper.getOriginalEnamedelement(ref));
      };
      ObjectExtensions.<EdeltaProgram>operator_doubleArrow(_parseWithTestEcoreWithSubPackage, _function);
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }
  
  @Test
  public void testSubPackageEClassDirectReference() {
    try {
      StringConcatenation _builder = new StringConcatenation();
      _builder.append("metamodel \"mainpackage\"");
      _builder.newLine();
      _builder.newLine();
      _builder.append("modifyEcore aTest epackage mainpackage {");
      _builder.newLine();
      _builder.append("\t");
      _builder.append("ecoreref(MainSubPackageFooClass)");
      _builder.newLine();
      _builder.append("}");
      _builder.newLine();
      EdeltaProgram _parseWithTestEcoreWithSubPackage = this.parseWithTestEcoreWithSubPackage(_builder);
      final Procedure1<EdeltaProgram> _function = (EdeltaProgram it) -> {
        final EdeltaEcoreReference ref = this.lastEcoreReferenceExpression(it).getReference();
        this._edeltaOriginalENamedElementRecorder.recordOriginalENamedElement(ref);
        final EClassifier original = this.getEClassiferByName(IterableExtensions.<EPackage>head(IterableExtensions.<EPackage>last(it.getMetamodels()).getESubpackages()), "MainSubPackageFooClass");
        Assert.assertNotNull(original);
        Assert.assertSame(original, this._edeltaDerivedStateHelper.getOriginalEnamedelement(ref));
      };
      ObjectExtensions.<EdeltaProgram>operator_doubleArrow(_parseWithTestEcoreWithSubPackage, _function);
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }
  
  @Test
  public void testSubSubPackageEClassQualifiedReference() {
    try {
      StringConcatenation _builder = new StringConcatenation();
      _builder.append("metamodel \"mainpackage\"");
      _builder.newLine();
      _builder.newLine();
      _builder.append("modifyEcore aTest epackage mainpackage {");
      _builder.newLine();
      _builder.append("\t");
      _builder.append("ecoreref(subsubpackage.MyClass)");
      _builder.newLine();
      _builder.append("}");
      _builder.newLine();
      EdeltaProgram _parseWithTestEcoreWithSubPackage = this.parseWithTestEcoreWithSubPackage(_builder);
      final Procedure1<EdeltaProgram> _function = (EdeltaProgram it) -> {
        final EdeltaEcoreReference ref = this.lastEcoreReferenceExpression(it).getReference();
        this._edeltaOriginalENamedElementRecorder.recordOriginalENamedElement(ref);
        final EClassifier original = this.getEClassiferByName(IterableExtensions.<EPackage>head(IterableExtensions.<EPackage>head(IterableExtensions.<EPackage>last(it.getMetamodels()).getESubpackages()).getESubpackages()), "MyClass");
        Assert.assertNotNull(original);
        Assert.assertSame(original, this._edeltaDerivedStateHelper.getOriginalEnamedelement(ref));
      };
      ObjectExtensions.<EdeltaProgram>operator_doubleArrow(_parseWithTestEcoreWithSubPackage, _function);
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }
  
  @Test
  public void testEClassifierQualifiedReference() {
    try {
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
        Assert.assertSame(original, this._edeltaDerivedStateHelper.getOriginalEnamedelement(ref));
        final EPackage originalPackage = IterableExtensions.<EPackage>last(it.getMetamodels());
        Assert.assertSame(originalPackage, this._edeltaDerivedStateHelper.getOriginalEnamedelement(ref.getQualification()));
      };
      ObjectExtensions.<EdeltaProgram>operator_doubleArrow(_parseWithTestEcore, _function);
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }
  
  @Test
  public void testCreatedEClassifierDirectReference() {
    try {
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
        Assert.assertNull(this._edeltaDerivedStateHelper.getOriginalEnamedelement(ref));
      };
      ObjectExtensions.<EdeltaProgram>operator_doubleArrow(_parseWithTestEcore, _function);
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }
  
  @Test
  public void testCreatedEClassifierQualifiedReference() {
    try {
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
        Assert.assertNull(this._edeltaDerivedStateHelper.getOriginalEnamedelement(ref));
        final EPackage originalPackage = IterableExtensions.<EPackage>last(it.getMetamodels());
        Assert.assertSame(originalPackage, this._edeltaDerivedStateHelper.getOriginalEnamedelement(ref.getQualification()));
      };
      ObjectExtensions.<EdeltaProgram>operator_doubleArrow(_parseWithTestEcore, _function);
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }
  
  @Test
  public void testEStrucutralFeatureDirectReference() {
    try {
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
        Assert.assertSame(original, this._edeltaDerivedStateHelper.getOriginalEnamedelement(ref));
      };
      ObjectExtensions.<EdeltaProgram>operator_doubleArrow(_parseWithTestEcore, _function);
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }
  
  @Test
  public void testEStrucutralFeatureQualifiedReference() {
    try {
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
        Assert.assertSame(originalEClass, this._edeltaDerivedStateHelper.getOriginalEnamedelement(ref.getQualification()));
        final EStructuralFeature original = this.getEStructuralFeatureByName(originalEClass, "myAttribute");
        Assert.assertSame(original, this._edeltaDerivedStateHelper.getOriginalEnamedelement(ref));
      };
      ObjectExtensions.<EdeltaProgram>operator_doubleArrow(_parseWithTestEcore, _function);
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }
  
  @Test
  public void testEStrucutralFeatureFullyQualifiedReference() {
    try {
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
          this._edeltaDerivedStateHelper.getOriginalEnamedelement(this.getEdeltaEcoreQualifiedReference(ref.getQualification()).getQualification()));
        final EClassifier originalEClass = this.getEClassiferByName(originalPackage, "FooClass");
        Assert.assertSame(originalEClass, this._edeltaDerivedStateHelper.getOriginalEnamedelement(ref.getQualification()));
        final EStructuralFeature original = this.getEStructuralFeatureByName(originalEClass, "myAttribute");
        Assert.assertSame(original, this._edeltaDerivedStateHelper.getOriginalEnamedelement(ref));
      };
      ObjectExtensions.<EdeltaProgram>operator_doubleArrow(_parseWithTestEcore, _function);
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }
  
  @Test
  public void testEEnumLiteraDirectReference() {
    try {
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
        Assert.assertSame(original, this._edeltaDerivedStateHelper.getOriginalEnamedelement(ref));
      };
      ObjectExtensions.<EdeltaProgram>operator_doubleArrow(_parseWithTestEcore, _function);
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }
}
