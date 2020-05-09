package edelta.tests;

import com.google.inject.Inject;
import edelta.edelta.EdeltaEcoreReferenceExpression;
import edelta.edelta.EdeltaProgram;
import edelta.tests.EdeltaAbstractTest;
import edelta.tests.EdeltaInjectorProviderCustom;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EEnum;
import org.eclipse.emf.ecore.EEnumLiteral;
import org.eclipse.emf.ecore.ENamedElement;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.xtend2.lib.StringConcatenation;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.XtextRunner;
import org.eclipse.xtext.xbase.XExpression;
import org.eclipse.xtext.xbase.lib.Extension;
import org.eclipse.xtext.xbase.lib.ObjectExtensions;
import org.eclipse.xtext.xbase.lib.Procedures.Procedure1;
import org.eclipse.xtext.xbase.typesystem.IBatchTypeResolver;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(XtextRunner.class)
@InjectWith(EdeltaInjectorProviderCustom.class)
@SuppressWarnings("all")
public class EdeltaTypeComputerTest extends EdeltaAbstractTest {
  @Inject
  @Extension
  private IBatchTypeResolver _iBatchTypeResolver;
  
  @Test
  public void testTypeOfReferenceToEPackage() {
    this.assertType("ecoreref(foo)", EPackage.class);
  }
  
  @Test
  public void testTypeOfReferenceToEClass() {
    this.assertType("ecoreref(FooClass)", EClass.class);
  }
  
  @Test
  public void testTypeOfReferenceToEDataType() {
    this.assertType("ecoreref(FooDataType)", EDataType.class);
  }
  
  @Test
  public void testTypeOfReferenceToEEnum() {
    this.assertType("ecoreref(FooEnum)", EEnum.class);
  }
  
  @Test
  public void testTypeOfReferenceToEAttribute() {
    this.assertType("ecoreref(myAttribute)", EAttribute.class);
  }
  
  @Test
  public void testTypeOfReferenceToEReference() {
    this.assertType("ecoreref(myReference)", EReference.class);
  }
  
  @Test
  public void testTypeOfReferenceToEEnumLiteral() {
    this.assertType("ecoreref(FooEnumLiteral)", EEnumLiteral.class);
  }
  
  @Test
  public void testTypeOfReferenceToUnresolvedENamedElement() {
    this.assertType("ecoreref(NonExistant)", ENamedElement.class);
  }
  
  @Test
  public void testTypeOfReferenceToUnresolvedENamedElementWithExpectations() {
    this.assertTypeOfRightExpression("val org.eclipse.emf.ecore.EClass c = ecoreref(NonExistant)", EClass.class);
  }
  
  @Test
  public void testTypeOfReferenceToUnresolvedQualifiedENamedElementWithExpectations() {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("val org.eclipse.emf.ecore.EClass c = ecoreref(FooClass.NonExistant)");
    _builder.newLine();
    this.assertTypeOfRightExpression(_builder, EClass.class);
  }
  
  @Test
  public void testTypeOfReferenceToUnresolvedENamedElementAtLeastENamedElement() {
    this.assertTypeOfRightExpression("val Object c = ecoreref(NonExistant)", ENamedElement.class);
  }
  
  @Test
  public void testTypeOfReferenceToNullNamedElement() {
    this.assertENamedElement("ecoreref");
  }
  
  @Test
  public void testTypeOfReferenceToNullNamedElement2() {
    this.assertENamedElement("ecoreref()");
  }
  
  @Test
  public void testTypeForRenamedEClassInModifyEcore() {
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
    final EdeltaEcoreReferenceExpression ecoreref = this.getEdeltaEcoreReferenceExpression(this.getBlockLastExpression(this.lastModifyEcoreOperation(prog).getBody()));
    Assert.assertEquals(EClass.class.getCanonicalName(), 
      this._iBatchTypeResolver.resolveTypes(ecoreref).getActualType(ecoreref).getIdentifier());
  }
  
  @Test
  public void testTypeForRenamedQualifiedEClassInModifyEcore() {
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
    final EdeltaEcoreReferenceExpression ecoreref = this.getEdeltaEcoreReferenceExpression(this.getBlockLastExpression(this.lastModifyEcoreOperation(prog).getBody()));
    Assert.assertEquals(EClass.class.getCanonicalName(), 
      this._iBatchTypeResolver.resolveTypes(ecoreref).getActualType(ecoreref).getIdentifier());
  }
  
  private EdeltaEcoreReferenceExpression assertType(final CharSequence input, final Class<?> expected) {
    EdeltaEcoreReferenceExpression _ecoreReferenceExpression = this.ecoreReferenceExpression(input);
    final Procedure1<EdeltaEcoreReferenceExpression> _function = (EdeltaEcoreReferenceExpression it) -> {
      Assert.assertEquals(expected.getCanonicalName(), 
        this._iBatchTypeResolver.resolveTypes(it).getActualType(it).getIdentifier());
    };
    return ObjectExtensions.<EdeltaEcoreReferenceExpression>operator_doubleArrow(_ecoreReferenceExpression, _function);
  }
  
  private EdeltaEcoreReferenceExpression assertENamedElement(final CharSequence input) {
    EdeltaEcoreReferenceExpression _ecoreReferenceExpression = this.ecoreReferenceExpression(input);
    final Procedure1<EdeltaEcoreReferenceExpression> _function = (EdeltaEcoreReferenceExpression it) -> {
      Assert.assertEquals(ENamedElement.class.getCanonicalName(), 
        this._iBatchTypeResolver.resolveTypes(it).getActualType(it).getIdentifier());
    };
    return ObjectExtensions.<EdeltaEcoreReferenceExpression>operator_doubleArrow(_ecoreReferenceExpression, _function);
  }
  
  private XExpression assertTypeOfRightExpression(final CharSequence input, final Class<?> expected) {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("metamodel \"foo\"");
    _builder.newLine();
    _builder.newLine();
    _builder.append("modifyEcore aTest epackage foo {");
    _builder.newLine();
    _builder.append("\t");
    _builder.append(input, "\t");
    _builder.newLineIfNotEmpty();
    _builder.append("}");
    _builder.newLine();
    XExpression _blockLastExpression = this.getBlockLastExpression(this.lastModifyEcoreOperation(this.parseWithTestEcore(_builder)).getBody());
    final Procedure1<XExpression> _function = (XExpression it) -> {
      Assert.assertEquals(expected.getCanonicalName(), 
        this._iBatchTypeResolver.resolveTypes(it).getActualType(
          this.getVariableDeclaration(it).getRight()).getIdentifier());
    };
    return ObjectExtensions.<XExpression>operator_doubleArrow(_blockLastExpression, _function);
  }
}
