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
import org.eclipse.xtext.testing.extensions.InjectionExtension;
import org.eclipse.xtext.xbase.lib.Extension;
import org.eclipse.xtext.xbase.lib.ObjectExtensions;
import org.eclipse.xtext.xbase.lib.Procedures.Procedure1;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@ExtendWith(InjectionExtension.class)
@InjectWith(EdeltaInjectorProviderCustom.class)
@SuppressWarnings("all")
public class EdeltaCompilerUtilTest extends EdeltaAbstractTest {
  @Inject
  @Extension
  private EdeltaCompilerUtil edeltaCompilerUtil;
  
  @Test
  public void testGetEPackageNameOrNull() {
    final EcoreFactory factory = EcoreFactory.eINSTANCE;
    Assertions.assertNull(this.edeltaCompilerUtil.getEPackageNameOrNull(null));
    EPackage _createEPackage = factory.createEPackage();
    final Procedure1<EPackage> _function = (EPackage it) -> {
      it.setName("test");
    };
    final EPackage p = ObjectExtensions.<EPackage>operator_doubleArrow(_createEPackage, _function);
    Assertions.assertEquals("test", this.edeltaCompilerUtil.getEPackageNameOrNull(p));
  }
  
  @ParameterizedTest
  @CsvSource({ "ecoreref(FooClass), \'getEClass(\"foo\", \"FooClass\")\'", "ecoreref(myAttribute), \'getEAttribute(\"foo\", \"FooClass\", \"myAttribute\")\'", "ecoreref(FooEnumLiteral), \'getEEnumLiteral(\"foo\", \"FooEnum\", \"FooEnumLiteral\")\'", "ecoreref(foo), \'getEPackage(\"foo\")\'", "ecoreref, \'null\'", "ecoreref(), \'getENamedElement()\'", "ecoreref(NonExistant), \'getENamedElement(\"\", \"\", \"\")\'" })
  public void testGetStringForEcoreReferenceExpression(final String input, final String expected) {
    final EdeltaEcoreReferenceExpression ecoreRefExp = this.ecoreReferenceExpression(input);
    Assertions.assertEquals(expected, 
      this.edeltaCompilerUtil.getStringForEcoreReferenceExpression(ecoreRefExp));
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
      Assertions.assertEquals("getEAttribute(\"mainpackage.mainsubpackage\", \"MainSubPackageFooClass\", \"mySubPackageAttribute\")", this.edeltaCompilerUtil.getStringForEcoreReferenceExpression(it));
    };
    ObjectExtensions.<EdeltaEcoreReferenceExpression>operator_doubleArrow(_lastEcoreReferenceExpression, _function);
  }
}
