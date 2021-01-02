package edelta.refactorings.lib.tests;

import edelta.lib.AbstractEdelta;
import edelta.lib.EdeltaLibrary;
import edelta.refactorings.lib.EdeltaBadSmellsChecker;
import edelta.refactorings.lib.tests.AbstractTest;
import edelta.refactorings.lib.tests.utils.InMemoryLoggerAppender;
import java.util.function.Consumer;
import org.assertj.core.api.Assertions;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.xtend2.lib.StringConcatenation;
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
    final Consumer<EPackage> _function = (EPackage it) -> {
      final Consumer<EClass> _function_1 = (EClass it_1) -> {
        EdeltaLibrary.addNewEAttribute(it_1, "A1", this.stringDataType);
      };
      EdeltaLibrary.addNewEClass(it, "C1", _function_1);
      final Consumer<EClass> _function_2 = (EClass it_1) -> {
        EdeltaLibrary.addNewEAttribute(it_1, "A1", this.intDataType);
      };
      EdeltaLibrary.addNewEClass(it, "C2", _function_2);
    };
    final EPackage p = this.createEPackage("p", _function);
    this.checker.checkDuplicateFeatures(p);
    Assertions.assertThat(this.appender.getResult()).isEmpty();
  }
  
  @Test
  public void test_checkDuplicateFeatures_withDuplicates() {
    final Consumer<EPackage> _function = (EPackage it) -> {
      final Consumer<EClass> _function_1 = (EClass it_1) -> {
        EdeltaLibrary.addNewEAttribute(it_1, "A1", this.stringDataType);
      };
      EdeltaLibrary.addNewEClass(it, "C1", _function_1);
      final Consumer<EClass> _function_2 = (EClass it_1) -> {
        EdeltaLibrary.addNewEAttribute(it_1, "A1", this.stringDataType);
      };
      EdeltaLibrary.addNewEClass(it, "C2", _function_2);
      final Consumer<EClass> _function_3 = (EClass it_1) -> {
        EdeltaLibrary.addNewEAttribute(it_1, "A1", this.stringDataType);
      };
      EdeltaLibrary.addNewEClass(it, "C3", _function_3);
    };
    final EPackage p = this.createEPackage("pack", _function);
    this.checker.checkDuplicateFeatures(p);
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("WARN: pack.C1.A1: pack.C1.A1, duplicate features: pack.C2.A1, pack.C3.A1");
    _builder.newLine();
    _builder.append("WARN: pack.C2.A1: pack.C2.A1, duplicate features: pack.C1.A1, pack.C3.A1");
    _builder.newLine();
    _builder.append("WARN: pack.C3.A1: pack.C3.A1, duplicate features: pack.C1.A1, pack.C2.A1");
    _builder.newLine();
    Assert.assertEquals(_builder.toString(), 
      this.appender.getResult());
  }
}
