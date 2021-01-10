package edelta.tests;

import com.google.common.collect.Iterables;
import com.google.inject.Inject;
import edelta.edelta.EdeltaEcoreQualifiedReference;
import edelta.edelta.EdeltaEcoreReference;
import edelta.edelta.EdeltaEcoreReferenceExpression;
import edelta.edelta.EdeltaPackage;
import edelta.edelta.EdeltaProgram;
import edelta.tests.EdeltaAbstractTest;
import edelta.tests.EdeltaInjectorProviderCustom;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.ENamedElement;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.xtend2.lib.StringConcatenation;
import org.eclipse.xtext.naming.QualifiedName;
import org.eclipse.xtext.resource.IEObjectDescription;
import org.eclipse.xtext.scoping.IScopeProvider;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.XtextRunner;
import org.eclipse.xtext.xbase.XExpression;
import org.eclipse.xtext.xbase.lib.Exceptions;
import org.eclipse.xtext.xbase.lib.Extension;
import org.eclipse.xtext.xbase.lib.Functions.Function1;
import org.eclipse.xtext.xbase.lib.IterableExtensions;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(XtextRunner.class)
@InjectWith(EdeltaInjectorProviderCustom.class)
@SuppressWarnings("all")
public class EdeltaScopeProviderTest extends EdeltaAbstractTest {
  @Inject
  @Extension
  private IScopeProvider _iScopeProvider;
  
  @Test
  public void testSuperScope() {
    try {
      StringConcatenation _builder = new StringConcatenation();
      _builder.append("modifyEcore aTest epackage foo {");
      _builder.newLine();
      _builder.append("\t");
      _builder.append("this.");
      _builder.newLine();
      _builder.append("}");
      _builder.newLine();
      this._iScopeProvider.getScope(this.getBlockLastExpression(this.lastModifyEcoreOperation(this.parseHelper.parse(_builder)).getBody()), EdeltaPackage.eINSTANCE.getEdeltaModifyEcoreOperation_Body());
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }
  
  @Test
  public void testScopeForMetamodel() {
    EdeltaProgram _parseWithTestEcore = this.parseWithTestEcore(this.inputs.referenceToMetamodel());
    EReference _edeltaProgram_Metamodels = EdeltaPackage.eINSTANCE.getEdeltaProgram_Metamodels();
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("foo");
    _builder.newLine();
    this.assertScope(_parseWithTestEcore, _edeltaProgram_Metamodels, _builder);
  }
  
  @Test
  public void testScopeForMetamodels() {
    EdeltaProgram _parseWithTestEcores = this.parseWithTestEcores(this.inputs.referencesToMetamodels());
    EReference _edeltaProgram_Metamodels = EdeltaPackage.eINSTANCE.getEdeltaProgram_Metamodels();
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("foo");
    _builder.newLine();
    _builder.append("bar");
    _builder.newLine();
    this.assertScope(_parseWithTestEcores, _edeltaProgram_Metamodels, _builder);
  }
  
  @Test
  public void testScopeForEnamedElementInProgram() {
    EdeltaProgram _parseWithTestEcore = this.parseWithTestEcore(this.inputs.referenceToMetamodelWithCopiedEPackage());
    EReference _edeltaEcoreReference_Enamedelement = EdeltaPackage.eINSTANCE.getEdeltaEcoreReference_Enamedelement();
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("foo");
    _builder.newLine();
    _builder.append("FooClass");
    _builder.newLine();
    _builder.append("myAttribute");
    _builder.newLine();
    _builder.append("myReference");
    _builder.newLine();
    _builder.append("FooDataType");
    _builder.newLine();
    _builder.append("FooEnum");
    _builder.newLine();
    _builder.append("FooEnumLiteral");
    _builder.newLine();
    this.assertScope(_parseWithTestEcore, _edeltaEcoreReference_Enamedelement, _builder);
  }
  
  @Test
  public void testScopeForEnamedElementWithSubPackageInProgram() {
    EdeltaProgram _parseWithTestEcoreWithSubPackage = this.parseWithTestEcoreWithSubPackage(this.inputs.referenceToMetamodelWithSubPackageWithCopiedEPackages());
    EReference _edeltaEcoreReference_Enamedelement = EdeltaPackage.eINSTANCE.getEdeltaEcoreReference_Enamedelement();
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("mainpackage");
    _builder.newLine();
    _builder.append("MainFooClass");
    _builder.newLine();
    _builder.append("myAttribute");
    _builder.newLine();
    _builder.append("myReference");
    _builder.newLine();
    _builder.append("MainFooDataType");
    _builder.newLine();
    _builder.append("MainFooEnum");
    _builder.newLine();
    _builder.append("FooEnumLiteral");
    _builder.newLine();
    _builder.append("MyClass");
    _builder.newLine();
    _builder.append("myClassAttribute");
    _builder.newLine();
    _builder.append("mainsubpackage");
    _builder.newLine();
    _builder.append("MainSubPackageFooClass");
    _builder.newLine();
    _builder.append("mySubPackageAttribute");
    _builder.newLine();
    _builder.append("mySubPackageReference");
    _builder.newLine();
    _builder.append("MyClass");
    _builder.newLine();
    _builder.append("myClassAttribute");
    _builder.newLine();
    _builder.append("subsubpackage");
    _builder.newLine();
    _builder.append("MyClass");
    _builder.newLine();
    this.assertScope(_parseWithTestEcoreWithSubPackage, _edeltaEcoreReference_Enamedelement, _builder);
  }
  
  @Test
  public void testScopeForEnamedElementInEcoreReferenceExpressionWithSubPackages() {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("metamodel \"mainpackage\"");
    _builder.newLine();
    _builder.newLine();
    _builder.append("modifyEcore aTest epackage mainpackage {");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("ecoreref(");
    _builder.newLine();
    _builder.append("}");
    _builder.newLine();
    EdeltaEcoreReference _reference = this.lastEcoreReferenceExpression(this.parseWithTestEcoreWithSubPackage(_builder)).getReference();
    EReference _edeltaEcoreReference_Enamedelement = EdeltaPackage.eINSTANCE.getEdeltaEcoreReference_Enamedelement();
    StringConcatenation _builder_1 = new StringConcatenation();
    _builder_1.append("mainpackage");
    _builder_1.newLine();
    _builder_1.append("MainFooClass");
    _builder_1.newLine();
    _builder_1.append("myAttribute");
    _builder_1.newLine();
    _builder_1.append("myReference");
    _builder_1.newLine();
    _builder_1.append("MainFooDataType");
    _builder_1.newLine();
    _builder_1.append("MainFooEnum");
    _builder_1.newLine();
    _builder_1.append("FooEnumLiteral");
    _builder_1.newLine();
    _builder_1.append("MyClass");
    _builder_1.newLine();
    _builder_1.append("myClassAttribute");
    _builder_1.newLine();
    _builder_1.append("mainsubpackage");
    _builder_1.newLine();
    _builder_1.append("MainSubPackageFooClass");
    _builder_1.newLine();
    _builder_1.append("mySubPackageAttribute");
    _builder_1.newLine();
    _builder_1.append("mySubPackageReference");
    _builder_1.newLine();
    _builder_1.append("MyClass");
    _builder_1.newLine();
    _builder_1.append("myClassAttribute");
    _builder_1.newLine();
    _builder_1.append("subsubpackage");
    _builder_1.newLine();
    _builder_1.append("MyClass");
    _builder_1.newLine();
    this.assertScope(_reference, _edeltaEcoreReference_Enamedelement, _builder_1);
  }
  
  @Test
  public void testScopeForSubPackageInEcoreReferenceExpressionWithSubPackages() {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("metamodel \"mainpackage\"");
    _builder.newLine();
    _builder.newLine();
    _builder.append("modifyEcore aTest epackage mainpackage {");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("ecoreref(mainsubpackage.");
    _builder.newLine();
    _builder.append("}");
    _builder.newLine();
    EdeltaEcoreReference _reference = this.lastEcoreReferenceExpression(this.parseWithTestEcoreWithSubPackage(_builder)).getReference();
    EReference _edeltaEcoreReference_Enamedelement = EdeltaPackage.eINSTANCE.getEdeltaEcoreReference_Enamedelement();
    StringConcatenation _builder_1 = new StringConcatenation();
    _builder_1.append("MainSubPackageFooClass");
    _builder_1.newLine();
    _builder_1.append("MyClass");
    _builder_1.newLine();
    _builder_1.append("subsubpackage");
    _builder_1.newLine();
    this.assertScope(_reference, _edeltaEcoreReference_Enamedelement, _builder_1);
  }
  
  @Test
  public void testScopeForSubPackageEClassInEcoreReferenceExpressionWithSubPackages() {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("metamodel \"mainpackage\"");
    _builder.newLine();
    _builder.newLine();
    _builder.append("modifyEcore aTest epackage mainpackage {");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("ecoreref(mainsubpackage.MainSubPackageFooClass.");
    _builder.newLine();
    _builder.append("}");
    _builder.newLine();
    EdeltaEcoreReference _reference = this.lastEcoreReferenceExpression(this.parseWithTestEcoreWithSubPackage(_builder)).getReference();
    EReference _edeltaEcoreReference_Enamedelement = EdeltaPackage.eINSTANCE.getEdeltaEcoreReference_Enamedelement();
    StringConcatenation _builder_1 = new StringConcatenation();
    _builder_1.append("mySubPackageAttribute");
    _builder_1.newLine();
    _builder_1.append("mySubPackageReference");
    _builder_1.newLine();
    this.assertScope(_reference, _edeltaEcoreReference_Enamedelement, _builder_1);
  }
  
  @Test
  public void testScopeForSubSubPackageInEcoreReferenceExpressionWithSubPackages() {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("metamodel \"mainpackage\"");
    _builder.newLine();
    _builder.newLine();
    _builder.append("modifyEcore aTest epackage mainpackage {");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("ecoreref(mainsubpackage.subsubpackage.");
    _builder.newLine();
    _builder.append("}");
    _builder.newLine();
    EdeltaEcoreReference _reference = this.lastEcoreReferenceExpression(this.parseWithTestEcoreWithSubPackage(_builder)).getReference();
    EReference _edeltaEcoreReference_Enamedelement = EdeltaPackage.eINSTANCE.getEdeltaEcoreReference_Enamedelement();
    StringConcatenation _builder_1 = new StringConcatenation();
    _builder_1.append("MyClass");
    _builder_1.newLine();
    this.assertScope(_reference, _edeltaEcoreReference_Enamedelement, _builder_1);
  }
  
  @Test
  public void testScopeForEnamedElementInEcoreReferenceExpression() {
    EdeltaEcoreReference _reference = this.ecoreReferenceExpression("ecoreref(").getReference();
    EReference _edeltaEcoreReference_Enamedelement = EdeltaPackage.eINSTANCE.getEdeltaEcoreReference_Enamedelement();
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("foo");
    _builder.newLine();
    _builder.append("FooClass");
    _builder.newLine();
    _builder.append("myAttribute");
    _builder.newLine();
    _builder.append("myReference");
    _builder.newLine();
    _builder.append("FooDataType");
    _builder.newLine();
    _builder.append("FooEnum");
    _builder.newLine();
    _builder.append("FooEnumLiteral");
    _builder.newLine();
    this.assertScope(_reference, _edeltaEcoreReference_Enamedelement, _builder);
  }
  
  @Test
  public void testScopeForEnamedElementInEcoreReferenceExpressionQualifiedPackage() {
    EdeltaEcoreReference _reference = this.ecoreReferenceExpression("ecoreref(foo.").getReference();
    EReference _edeltaEcoreReference_Enamedelement = EdeltaPackage.eINSTANCE.getEdeltaEcoreReference_Enamedelement();
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("FooClass");
    _builder.newLine();
    _builder.append("FooDataType");
    _builder.newLine();
    _builder.append("FooEnum");
    _builder.newLine();
    this.assertScope(_reference, _edeltaEcoreReference_Enamedelement, _builder);
  }
  
  @Test
  public void testScopeForEnamedElementInEcoreReferenceExpressionQualifiedEClass() {
    EdeltaEcoreReference _reference = this.ecoreReferenceExpression("ecoreref(foo.FooClass.").getReference();
    EReference _edeltaEcoreReference_Enamedelement = EdeltaPackage.eINSTANCE.getEdeltaEcoreReference_Enamedelement();
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("myAttribute");
    _builder.newLine();
    _builder.append("myReference");
    _builder.newLine();
    this.assertScope(_reference, _edeltaEcoreReference_Enamedelement, _builder);
  }
  
  @Test
  public void testScopeForReferenceToCreatedEClassWithTheSameNameAsAnExistingEClass() {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("metamodel \"foo\"");
    _builder.newLine();
    _builder.newLine();
    _builder.append("modifyEcore aTest epackage foo {");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("addNewEClass(\"FooClass\")");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("ecoreref(FooClass)");
    _builder.newLine();
    _builder.append("}");
    _builder.newLine();
    final EdeltaProgram prog = this.parseWithTestEcore(_builder);
    XExpression _blockLastExpression = this.getBlockLastExpression(this.lastModifyEcoreOperation(prog).getBody());
    final EdeltaEcoreReferenceExpression eclassExp = ((EdeltaEcoreReferenceExpression) _blockLastExpression);
    Assert.assertSame(
      this.getFirstEClass(IterableExtensions.<EPackage>head(this.getCopiedEPackages(prog))), 
      eclassExp.getReference().getEnamedelement());
  }
  
  @Test
  public void testScopeForReferenceToCopiedEPackageEClassifierInModifyEcore() {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("metamodel \"foo\"");
    _builder.newLine();
    _builder.newLine();
    _builder.append("modifyEcore aTest epackage foo {");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("ecoreref(FooDataType)");
    _builder.newLine();
    _builder.append("}");
    _builder.newLine();
    final EdeltaProgram prog = this.parseWithTestEcore(_builder);
    XExpression _blockLastExpression = this.getBlockLastExpression(this.lastModifyEcoreOperation(prog).getBody());
    final EdeltaEcoreReferenceExpression eclassExp = ((EdeltaEcoreReferenceExpression) _blockLastExpression);
    ENamedElement _enamedelement = eclassExp.getReference().getEnamedelement();
    final EDataType dataType = ((EDataType) _enamedelement);
    Assert.assertSame(
      IterableExtensions.<EDataType>head(Iterables.<EDataType>filter(IterableExtensions.<EPackage>head(this.getCopiedEPackages(prog)).getEClassifiers(), EDataType.class)), dataType);
  }
  
  @Test
  public void testScopeForReferenceToOriginalEPackageEClassifierInOperation() {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("metamodel \"foo\"");
    _builder.newLine();
    _builder.newLine();
    _builder.append("def anOp() {");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("ecoreref(FooDataType)");
    _builder.newLine();
    _builder.append("}");
    _builder.newLine();
    final EdeltaProgram prog = this.parseWithTestEcore(_builder);
    XExpression _blockLastExpression = this.getBlockLastExpression(this.lastOperation(prog).getBody());
    final EdeltaEcoreReferenceExpression eclassExp = ((EdeltaEcoreReferenceExpression) _blockLastExpression);
    ENamedElement _enamedelement = eclassExp.getReference().getEnamedelement();
    final EDataType dataType = ((EDataType) _enamedelement);
    Assert.assertSame(
      IterableExtensions.<EDataType>head(Iterables.<EDataType>filter(IterableExtensions.<EPackage>head(prog.getMetamodels()).getEClassifiers(), EDataType.class)), dataType);
  }
  
  @Test
  public void testScopeForReferenceToCopiedEPackageEClassifierInOperationWhenTheresAModifyEcore() {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("metamodel \"foo\"");
    _builder.newLine();
    _builder.newLine();
    _builder.append("def anOp() {");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("ecoreref(FooDataType)");
    _builder.newLine();
    _builder.append("}");
    _builder.newLine();
    _builder.newLine();
    _builder.append("// this triggers copied EPackage");
    _builder.newLine();
    _builder.append("modifyEcore aTest epackage foo {");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("ecoreref(FooDataType)");
    _builder.newLine();
    _builder.append("}");
    _builder.newLine();
    final EdeltaProgram prog = this.parseWithTestEcore(_builder);
    XExpression _blockLastExpression = this.getBlockLastExpression(this.lastOperation(prog).getBody());
    final EdeltaEcoreReferenceExpression eclassExp = ((EdeltaEcoreReferenceExpression) _blockLastExpression);
    ENamedElement _enamedelement = eclassExp.getReference().getEnamedelement();
    final EDataType dataType = ((EDataType) _enamedelement);
    Assert.assertSame(
      IterableExtensions.<EDataType>head(Iterables.<EDataType>filter(IterableExtensions.<EPackage>head(this.getCopiedEPackages(prog)).getEClassifiers(), EDataType.class)), dataType);
  }
  
  @Test
  public void testScopeForReferenceToCreatedEAttribute() {
    EdeltaEcoreReference _reference = this.lastEcoreReferenceExpression(this.parseWithTestEcore(this.inputs.referenceToCreatedEAttributeSimple())).getReference();
    EReference _edeltaEcoreReference_Enamedelement = EdeltaPackage.eINSTANCE.getEdeltaEcoreReference_Enamedelement();
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("foo");
    _builder.newLine();
    _builder.append("FooClass");
    _builder.newLine();
    _builder.append("myAttribute");
    _builder.newLine();
    _builder.append("myReference");
    _builder.newLine();
    _builder.append("FooDataType");
    _builder.newLine();
    _builder.append("FooEnum");
    _builder.newLine();
    _builder.append("FooEnumLiteral");
    _builder.newLine();
    _builder.append("NewClass");
    _builder.newLine();
    _builder.append("newAttribute");
    _builder.newLine();
    _builder.append("newAttribute2");
    _builder.newLine();
    this.assertScope(_reference, _edeltaEcoreReference_Enamedelement, _builder);
  }
  
  @Test
  public void testScopeForReferenceToCreatedEAttributeChangingNameInBody() {
    EdeltaEcoreReference _reference = this.lastEcoreReferenceExpression(this.parseWithTestEcore(this.inputs.referenceToCreatedEAttributeRenamed())).getReference();
    EReference _edeltaEcoreReference_Enamedelement = EdeltaPackage.eINSTANCE.getEdeltaEcoreReference_Enamedelement();
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("foo");
    _builder.newLine();
    _builder.append("FooClass");
    _builder.newLine();
    _builder.append("myAttribute");
    _builder.newLine();
    _builder.append("myReference");
    _builder.newLine();
    _builder.append("FooDataType");
    _builder.newLine();
    _builder.append("FooEnum");
    _builder.newLine();
    _builder.append("FooEnumLiteral");
    _builder.newLine();
    _builder.append("NewClass");
    _builder.newLine();
    _builder.append("changed");
    _builder.newLine();
    this.assertScope(_reference, _edeltaEcoreReference_Enamedelement, _builder);
  }
  
  @Test
  public void testScopeForModifyEcore() {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("metamodel \"foo\"");
    _builder.newLine();
    _builder.append("metamodel \"bar\"");
    _builder.newLine();
    _builder.newLine();
    _builder.append("modifyEcore aTest epackage foo {}");
    _builder.newLine();
    EdeltaProgram _parseWithTestEcores = this.parseWithTestEcores(_builder);
    EReference _edeltaModifyEcoreOperation_Epackage = EdeltaPackage.eINSTANCE.getEdeltaModifyEcoreOperation_Epackage();
    StringConcatenation _builder_1 = new StringConcatenation();
    _builder_1.append("foo");
    _builder_1.newLine();
    _builder_1.append("bar");
    _builder_1.newLine();
    this.assertScope(_parseWithTestEcores, _edeltaModifyEcoreOperation_Epackage, _builder_1);
  }
  
  @Test
  public void testScopeForRenamedEClassInModifyEcore() {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("metamodel \"foo\"");
    _builder.newLine();
    _builder.append("metamodel \"bar\"");
    _builder.newLine();
    _builder.append("modifyEcore aTest epackage foo {");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("ecoreref(foo.FooClass).name = \"RenamedClass\"");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("ecoreref(foo.RenamedClass)");
    _builder.newLine();
    _builder.append("}");
    _builder.newLine();
    EdeltaEcoreReference _reference = this.getEdeltaEcoreReferenceExpression(this.getBlockLastExpression(this.lastModifyEcoreOperation(this.parseWithTestEcore(_builder)).getBody())).getReference();
    EReference _edeltaEcoreReference_Enamedelement = EdeltaPackage.eINSTANCE.getEdeltaEcoreReference_Enamedelement();
    StringConcatenation _builder_1 = new StringConcatenation();
    _builder_1.append("RenamedClass");
    _builder_1.newLine();
    _builder_1.append("FooDataType");
    _builder_1.newLine();
    _builder_1.append("FooEnum");
    _builder_1.newLine();
    this.assertScope(_reference, _edeltaEcoreReference_Enamedelement, _builder_1);
  }
  
  @Test
  public void testScopeForEnamedElementInEcoreReferenceExpressionReferringToRenamedEClassInModifyEcore() {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("metamodel \"foo\"");
    _builder.newLine();
    _builder.append("metamodel \"bar\"");
    _builder.newLine();
    _builder.append("modifyEcore aTest epackage foo {");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("ecoreref(foo.FooClass).name = \"RenamedClass\"");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("ecoreref(foo.RenamedClass.)");
    _builder.newLine();
    _builder.append("}");
    _builder.newLine();
    EdeltaEcoreReference _reference = this.getEdeltaEcoreReferenceExpression(this.getBlockLastExpression(this.lastModifyEcoreOperation(this.parseWithTestEcore(_builder)).getBody())).getReference();
    EReference _edeltaEcoreReference_Enamedelement = EdeltaPackage.eINSTANCE.getEdeltaEcoreReference_Enamedelement();
    StringConcatenation _builder_1 = new StringConcatenation();
    _builder_1.append("myAttribute");
    _builder_1.newLine();
    _builder_1.append("myReference");
    _builder_1.newLine();
    this.assertScope(_reference, _edeltaEcoreReference_Enamedelement, _builder_1);
  }
  
  @Test
  public void testScopeForFeaturesOfRenamedEClassInModifyEcore() {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("metamodel \"foo\"");
    _builder.newLine();
    _builder.append("metamodel \"bar\"");
    _builder.newLine();
    _builder.append("modifyEcore aTest epackage foo {");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("ecoreref(foo.FooClass).name = \"RenamedClass\"");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("ecoreref(RenamedClass).EStructuralFeatures +=");
    _builder.newLine();
    _builder.append("\t\t");
    _builder.append("newEAttribute(\"addedAttribute\", ecoreref(FooDataType))");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("ecoreref(RenamedClass.)");
    _builder.newLine();
    _builder.append("}");
    _builder.newLine();
    EdeltaEcoreReference _reference = this.getEdeltaEcoreReferenceExpression(this.getBlockLastExpression(this.lastModifyEcoreOperation(this.parseWithTestEcore(_builder)).getBody())).getReference();
    EReference _edeltaEcoreReference_Enamedelement = EdeltaPackage.eINSTANCE.getEdeltaEcoreReference_Enamedelement();
    StringConcatenation _builder_1 = new StringConcatenation();
    _builder_1.append("myAttribute");
    _builder_1.newLine();
    _builder_1.append("myReference");
    _builder_1.newLine();
    _builder_1.append("addedAttribute");
    _builder_1.newLine();
    this.assertScope(_reference, _edeltaEcoreReference_Enamedelement, _builder_1);
  }
  
  @Test
  public void testLinkForRenamedEClassInModifyEcore() {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("metamodel \"foo\"");
    _builder.newLine();
    _builder.newLine();
    _builder.append("modifyEcore aTest epackage foo {");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("ecoreref(foo.FooClass).name = \"RenamedClass\"");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("ecoreref(RenamedClass)");
    _builder.newLine();
    _builder.append("}");
    _builder.newLine();
    final EdeltaProgram prog = this.parseWithTestEcore(_builder);
    this.validationTestHelper.assertNoErrors(prog);
    final EdeltaEcoreReference referred = this.getEdeltaEcoreReferenceExpression(this.getBlockLastExpression(this.lastModifyEcoreOperation(prog).getBody())).getReference();
    final EPackage copiedEPackage = IterableExtensions.<EPackage>head(this.getCopiedEPackages(prog));
    ENamedElement _enamedelement = referred.getEnamedelement();
    Assert.assertSame(
      this.getEClassiferByName(copiedEPackage, "RenamedClass"), 
      ((EClass) _enamedelement));
  }
  
  @Test
  public void testLinkForRenamedQualifiedEClassInModifyEcore() {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("metamodel \"foo\"");
    _builder.newLine();
    _builder.newLine();
    _builder.append("modifyEcore aTest epackage foo {");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("ecoreref(foo.FooClass).name = \"RenamedClass\"");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("ecoreref(foo.RenamedClass)");
    _builder.newLine();
    _builder.append("}");
    _builder.newLine();
    final EdeltaProgram prog = this.parseWithTestEcore(_builder);
    this.validationTestHelper.assertNoErrors(prog);
    EdeltaEcoreReference _reference = this.getEdeltaEcoreReferenceExpression(this.getBlockLastExpression(this.lastModifyEcoreOperation(prog).getBody())).getReference();
    final EdeltaEcoreQualifiedReference referred = ((EdeltaEcoreQualifiedReference) _reference);
    final EPackage copiedEPackage = IterableExtensions.<EPackage>head(this.getCopiedEPackages(prog));
    ENamedElement _enamedelement = referred.getEnamedelement();
    Assert.assertSame(
      this.getEClassiferByName(copiedEPackage, "RenamedClass"), 
      ((EClass) _enamedelement));
    ENamedElement _enamedelement_1 = referred.getQualification().getEnamedelement();
    Assert.assertSame(copiedEPackage, 
      ((EPackage) _enamedelement_1));
  }
  
  private void assertScope(final EObject context, final EReference reference, final CharSequence expected) {
    String _string = expected.toString();
    final Function1<IEObjectDescription, QualifiedName> _function = (IEObjectDescription it) -> {
      return it.getName();
    };
    String _join = IterableExtensions.join(IterableExtensions.<IEObjectDescription, QualifiedName>map(this._iScopeProvider.getScope(context, reference).getAllElements(), _function), "\n");
    String _plus = (_join + "\n");
    this.assertEqualsStrings(_string, _plus);
  }
}
