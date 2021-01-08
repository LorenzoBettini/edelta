package edelta.tests;

import com.google.inject.Inject;
import com.google.inject.Injector;
import edelta.edelta.EdeltaModifyEcoreOperation;
import edelta.edelta.EdeltaProgram;
import edelta.interpreter.EdeltaInterpreter;
import edelta.interpreter.EdeltaInterpreterFactory;
import edelta.interpreter.EdeltaInterpreterRuntimeException;
import edelta.interpreter.EdeltaSafeInterpreter;
import edelta.resource.derivedstate.EdeltaDerivedStateHelper;
import edelta.tests.EdeltaAbstractTest;
import edelta.tests.EdeltaInjectorProviderDerivedStateComputerWithoutSafeInterpreter;
import org.assertj.core.api.Assertions;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.xtend2.lib.StringConcatenation;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.XtextRunner;
import org.eclipse.xtext.xbase.lib.Exceptions;
import org.eclipse.xtext.xbase.lib.Procedures.Procedure1;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(XtextRunner.class)
@InjectWith(EdeltaInjectorProviderDerivedStateComputerWithoutSafeInterpreter.class)
@SuppressWarnings("all")
public class EdeltaSafeInterpreterTest extends EdeltaAbstractTest {
  @Inject
  private EdeltaSafeInterpreter interpreter;
  
  @Inject
  private Injector injector;
  
  @Inject
  private EdeltaDerivedStateHelper derivedStateHelper;
  
  @Before
  public void setupInterpreter() {
    this.interpreter.setInterpreterTimeout(1200000);
  }
  
  @Test
  public void sanityTestCheck() {
    try {
      final EdeltaInterpreterFactory interpreterFactory = this.injector.<EdeltaInterpreterFactory>getInstance(EdeltaInterpreterFactory.class);
      final EdeltaInterpreter anotherInterprter = interpreterFactory.create(this.parseHelper.parse("").eResource());
      Assertions.assertThat(anotherInterprter.getClass()).isSameAs(this.interpreter.getClass());
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }
  
  @Test
  public void testCorrectInterpretation() {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("package test");
    _builder.newLine();
    _builder.newLine();
    _builder.append("metamodel \"foo\"");
    _builder.newLine();
    _builder.newLine();
    _builder.append("modifyEcore aTest epackage foo {");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("addNewEClass(\"First\")");
    _builder.newLine();
    _builder.append("}");
    _builder.newLine();
    final String input = _builder.toString();
    final Procedure1<EPackage> _function = (EPackage ePackage) -> {
      final EClass derivedEClass = this.getLastEClass(ePackage);
      Assert.assertEquals("First", derivedEClass.getName());
    };
    this.assertAfterInterpretationOfEdeltaModifyEcoreOperation(this.parseWithTestEcore(input), _function);
  }
  
  @Test
  public void testOperationWithErrorsDueToWrongParsing() {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("package test");
    _builder.newLine();
    _builder.newLine();
    _builder.append("metamodel \"foo\"");
    _builder.newLine();
    _builder.newLine();
    _builder.append("modifyEcore aTest epackage foo {");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("addNewEClass(\"First\")");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("eclass First");
    _builder.newLine();
    _builder.append("}");
    _builder.newLine();
    final String input = _builder.toString();
    final Procedure1<EPackage> _function = (EPackage ePackage) -> {
      final EClass derivedEClass = this.getLastEClass(ePackage);
      Assert.assertEquals("First", derivedEClass.getName());
    };
    this.assertAfterInterpretationOfEdeltaModifyEcoreOperation(this.parseWithTestEcore(input), _function);
  }
  
  @Test
  public void testCreateEClassAndCallOperationFromUseAsReferringToUnknownType() {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("metamodel \"foo\"");
    _builder.newLine();
    _builder.newLine();
    _builder.append("use NonExistant as my");
    _builder.newLine();
    _builder.newLine();
    _builder.append("modifyEcore aTest epackage foo {");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("val c = addNewEClass(\"NewClass\")");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("my.createANewEAttribute(c)");
    _builder.newLine();
    _builder.append("}");
    _builder.newLine();
    final Procedure1<EPackage> _function = (EPackage ePackage) -> {
      final EClass derivedEClass = this.getLastEClass(ePackage);
      Assert.assertEquals("NewClass", derivedEClass.getName());
    };
    this.assertAfterInterpretationOfEdeltaModifyEcoreOperation(this.parseWithTestEcore(_builder), _function);
  }
  
  @Test(expected = EdeltaInterpreterRuntimeException.class)
  public void testEdeltaInterpreterRuntimeExceptionIsThrown() {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("import org.eclipse.emf.ecore.EClass");
    _builder.newLine();
    _builder.append("import edelta.interpreter.EdeltaInterpreterRuntimeException");
    _builder.newLine();
    _builder.newLine();
    _builder.append("metamodel \"foo\"");
    _builder.newLine();
    _builder.newLine();
    _builder.append("def op(EClass c) : void {");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("throw new EdeltaInterpreterRuntimeException(\"test\")");
    _builder.newLine();
    _builder.append("}");
    _builder.newLine();
    _builder.newLine();
    _builder.append("modifyEcore aTest epackage foo {");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("op(addNewEClass(\"NewClass\"))");
    _builder.newLine();
    _builder.append("}");
    _builder.newLine();
    final Procedure1<EPackage> _function = (EPackage it) -> {
    };
    this.assertAfterInterpretationOfEdeltaModifyEcoreOperation(this.parseWithTestEcore(_builder), _function);
  }
  
  @Test
  public void testCreateEClassAndCallOperationThatThrows() {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("import org.eclipse.emf.ecore.EClass");
    _builder.newLine();
    _builder.append("import edelta.tests.additional.MyCustomException");
    _builder.newLine();
    _builder.newLine();
    _builder.append("metamodel \"foo\"");
    _builder.newLine();
    _builder.newLine();
    _builder.append("def op(EClass c) : void {");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("throw new MyCustomException");
    _builder.newLine();
    _builder.append("}");
    _builder.newLine();
    _builder.newLine();
    _builder.append("modifyEcore aTest epackage foo {");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("addNewEClass(\"NewClass\") [");
    _builder.newLine();
    _builder.append("\t\t");
    _builder.append("op(it)");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("]");
    _builder.newLine();
    _builder.append("}");
    _builder.newLine();
    final Procedure1<EPackage> _function = (EPackage it) -> {
    };
    this.assertAfterInterpretationOfEdeltaModifyEcoreOperation(this.parseWithTestEcore(_builder), _function);
  }
  
  @Test
  public void testThrowNullPointerException() {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("import org.eclipse.emf.ecore.EClass");
    _builder.newLine();
    _builder.append("import edelta.tests.additional.MyCustomException");
    _builder.newLine();
    _builder.newLine();
    _builder.append("metamodel \"foo\"");
    _builder.newLine();
    _builder.newLine();
    _builder.append("def op(EClass c) : void {");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("throw new NullPointerException");
    _builder.newLine();
    _builder.append("}");
    _builder.newLine();
    _builder.newLine();
    _builder.append("modifyEcore aTest epackage foo {");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("addNewEClass(\"NewClass\") [");
    _builder.newLine();
    _builder.append("\t\t");
    _builder.append("op(it)");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("]");
    _builder.newLine();
    _builder.append("}");
    _builder.newLine();
    final Procedure1<EPackage> _function = (EPackage it) -> {
    };
    this.assertAfterInterpretationOfEdeltaModifyEcoreOperation(this.parseWithTestEcore(_builder), _function);
  }
  
  private void assertAfterInterpretationOfEdeltaModifyEcoreOperation(final EdeltaProgram program, final Procedure1<? super EPackage> testExecutor) {
    final EdeltaModifyEcoreOperation it = this.lastModifyEcoreOperation(program);
    this.interpreter.evaluateModifyEcoreOperations(program);
    final String packageName = it.getEpackage().getName();
    final EPackage epackage = this.derivedStateHelper.getCopiedEPackagesMap(program.eResource()).get(packageName);
    testExecutor.apply(epackage);
  }
}
