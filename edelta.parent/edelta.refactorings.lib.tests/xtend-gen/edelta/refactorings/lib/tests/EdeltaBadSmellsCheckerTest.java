package edelta.refactorings.lib.tests;

import edelta.lib.AbstractEdelta;
import edelta.refactorings.lib.EdeltaBadSmellsChecker;
import edelta.refactorings.lib.tests.AbstractTest;
import edelta.refactorings.lib.tests.utils.InMemoryLoggerAppender;
import org.assertj.core.api.Assertions;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.xtend2.lib.StringConcatenation;
import org.eclipse.xtext.xbase.lib.ObjectExtensions;
import org.eclipse.xtext.xbase.lib.Procedures.Procedure1;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

@SuppressWarnings("all")
public class EdeltaBadSmellsCheckerTest extends AbstractTest {
  private EdeltaBadSmellsChecker checker;
  
  private InMemoryLoggerAppender appender;
  
  @Before
  public void setup() {
    EdeltaBadSmellsChecker _edeltaBadSmellsChecker = new EdeltaBadSmellsChecker();
    this.checker = _edeltaBadSmellsChecker;
    InMemoryLoggerAppender _inMemoryLoggerAppender = new InMemoryLoggerAppender();
    this.appender = _inMemoryLoggerAppender;
    this.checker.getLogger().addAppender(this.appender);
  }
  
  @Test
  public void test_ConstructorArgument() {
    EdeltaBadSmellsChecker _edeltaBadSmellsChecker = new EdeltaBadSmellsChecker(new AbstractEdelta() {
    });
    this.checker = _edeltaBadSmellsChecker;
    Assertions.<EdeltaBadSmellsChecker>assertThat(this.checker).isNotNull();
  }
  
  @Test
  public void test_checkDuplicateFeatures_whenNoDuplicates() {
    EPackage _createEPackage = this.factory.createEPackage();
    final Procedure1<EPackage> _function = (EPackage it) -> {
      EClass _createEClass = this.createEClass(it, "C1");
      final Procedure1<EClass> _function_1 = (EClass it_1) -> {
        EAttribute _createEAttribute = this.createEAttribute(it_1, "A1");
        final Procedure1<EAttribute> _function_2 = (EAttribute it_2) -> {
          it_2.setEType(this.stringDataType);
        };
        ObjectExtensions.<EAttribute>operator_doubleArrow(_createEAttribute, _function_2);
      };
      ObjectExtensions.<EClass>operator_doubleArrow(_createEClass, _function_1);
      EClass _createEClass_1 = this.createEClass(it, "C2");
      final Procedure1<EClass> _function_2 = (EClass it_1) -> {
        EAttribute _createEAttribute = this.createEAttribute(it_1, "A1");
        final Procedure1<EAttribute> _function_3 = (EAttribute it_2) -> {
          it_2.setEType(this.intDataType);
        };
        ObjectExtensions.<EAttribute>operator_doubleArrow(_createEAttribute, _function_3);
      };
      ObjectExtensions.<EClass>operator_doubleArrow(_createEClass_1, _function_2);
    };
    final EPackage p = ObjectExtensions.<EPackage>operator_doubleArrow(_createEPackage, _function);
    this.checker.checkDuplicateFeatures(p);
    Assertions.assertThat(this.appender.getResult()).isEmpty();
  }
  
  @Test
  public void test_checkDuplicateFeatures_withDuplicates() {
    EPackage _createEPackage = this.factory.createEPackage();
    final Procedure1<EPackage> _function = (EPackage it) -> {
      it.setName("pack");
      EClass _createEClass = this.createEClass(it, "C1");
      final Procedure1<EClass> _function_1 = (EClass it_1) -> {
        EAttribute _createEAttribute = this.createEAttribute(it_1, "A1");
        final Procedure1<EAttribute> _function_2 = (EAttribute it_2) -> {
          it_2.setEType(this.stringDataType);
        };
        ObjectExtensions.<EAttribute>operator_doubleArrow(_createEAttribute, _function_2);
      };
      ObjectExtensions.<EClass>operator_doubleArrow(_createEClass, _function_1);
      EClass _createEClass_1 = this.createEClass(it, "C2");
      final Procedure1<EClass> _function_2 = (EClass it_1) -> {
        EAttribute _createEAttribute = this.createEAttribute(it_1, "A1");
        final Procedure1<EAttribute> _function_3 = (EAttribute it_2) -> {
          it_2.setEType(this.stringDataType);
        };
        ObjectExtensions.<EAttribute>operator_doubleArrow(_createEAttribute, _function_3);
      };
      ObjectExtensions.<EClass>operator_doubleArrow(_createEClass_1, _function_2);
      EClass _createEClass_2 = this.createEClass(it, "C3");
      final Procedure1<EClass> _function_3 = (EClass it_1) -> {
        EAttribute _createEAttribute = this.createEAttribute(it_1, "A1");
        final Procedure1<EAttribute> _function_4 = (EAttribute it_2) -> {
          it_2.setEType(this.stringDataType);
        };
        ObjectExtensions.<EAttribute>operator_doubleArrow(_createEAttribute, _function_4);
      };
      ObjectExtensions.<EClass>operator_doubleArrow(_createEClass_2, _function_3);
    };
    final EPackage p = ObjectExtensions.<EPackage>operator_doubleArrow(_createEPackage, _function);
    this.checker.checkDuplicateFeatures(p);
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("WARN: pack.C1.A1: Duplicate features: pack.C2.A1, pack.C3.A1");
    _builder.newLine();
    _builder.append("WARN: pack.C2.A1: Duplicate features: pack.C1.A1, pack.C3.A1");
    _builder.newLine();
    _builder.append("WARN: pack.C3.A1: Duplicate features: pack.C1.A1, pack.C2.A1");
    _builder.newLine();
    Assert.assertEquals(_builder.toString(), 
      this.appender.getResult());
  }
}
