package edelta.tests;

import com.google.inject.Inject;
import edelta.edelta.EdeltaEcoreReferenceExpression;
import edelta.edelta.EdeltaProgram;
import edelta.tests.EdeltaAbstractTest;
import edelta.tests.EdeltaInjectorProviderCustom;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.xtend2.lib.StringConcatenation;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.extensions.InjectionExtension;
import org.eclipse.xtext.xbase.XExpression;
import org.eclipse.xtext.xbase.lib.Extension;
import org.eclipse.xtext.xbase.lib.ObjectExtensions;
import org.eclipse.xtext.xbase.lib.Procedures.Procedure1;
import org.eclipse.xtext.xbase.typesystem.IBatchTypeResolver;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@ExtendWith(InjectionExtension.class)
@InjectWith(EdeltaInjectorProviderCustom.class)
@SuppressWarnings("all")
public class EdeltaTypeComputerTest extends EdeltaAbstractTest {
  @Inject
  @Extension
  private IBatchTypeResolver typeResolver;
  
  @ParameterizedTest
  @CsvSource({ "foo, EPackage", "FooClass, EClass", "FooDataType, EDataType", "FooEnum, EEnum", "myAttribute, EAttribute", "myReference, EReference", "FooEnumLiteral, EEnumLiteral", "NonExistant, ENamedElement" })
  public void testTypeOfEcoreReference(final String ecoreRefArg, final String expectedType) {
    this.assertType(
      (("ecoreref(" + ecoreRefArg) + ")"), 
      ("org.eclipse.emf.ecore." + expectedType));
  }
  
  @ParameterizedTest
  @CsvSource({ "\'val org.eclipse.emf.ecore.EClass c = ecoreref(NonExistant)\', EClass", "\'val org.eclipse.emf.ecore.EClass c = ecoreref(FooClass.NonExistant)\', EClass", "\'val Object c = ecoreref(NonExistant)\', ENamedElement" })
  public void testTypeOfEcoreReferenceWithExpectation(final String input, final String expectedType) {
    this.assertTypeOfRightExpression(input, 
      ("org.eclipse.emf.ecore." + expectedType));
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
    Assertions.assertEquals(EClass.class.getCanonicalName(), 
      this.typeResolver.resolveTypes(ecoreref).getActualType(ecoreref).getIdentifier());
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
    Assertions.assertEquals(EClass.class.getCanonicalName(), 
      this.typeResolver.resolveTypes(ecoreref).getActualType(ecoreref).getIdentifier());
  }
  
  private void assertType(final CharSequence input, final String expectedTypeFQN) {
    final EdeltaEcoreReferenceExpression ecoreRefExp = this.ecoreReferenceExpression(input);
    Assertions.assertEquals(expectedTypeFQN, 
      this.typeResolver.resolveTypes(ecoreRefExp).getActualType(ecoreRefExp).getIdentifier());
  }
  
  private void assertENamedElement(final CharSequence input) {
    this.assertType(input, "org.eclipse.emf.ecore.ENamedElement");
  }
  
  private XExpression assertTypeOfRightExpression(final CharSequence input, final String expectedTypeFQN) {
    XExpression _blockLastExpression = this.getBlockLastExpression(this.lastModifyEcoreOperation(this.parseWithTestEcore("\n\t\t\tmetamodel \"foo\"\n\t\t\t\n\t\t\tmodifyEcore aTest epackage foo {\n\t\t\t\t«input»\n\t\t\t}\n\t\t".replace("«input»", input))).getBody());
    final Procedure1<XExpression> _function = (XExpression it) -> {
      Assertions.assertEquals(expectedTypeFQN, 
        this.typeResolver.resolveTypes(it).getActualType(
          this.getVariableDeclaration(it).getRight()).getIdentifier());
    };
    return ObjectExtensions.<XExpression>operator_doubleArrow(_blockLastExpression, _function);
  }
}
