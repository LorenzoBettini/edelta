package edelta.tests;

import com.google.inject.Inject;
import com.google.inject.Injector;
import edelta.interpreter.EdeltaSafeInterpreter;
import edelta.interpreter.IEdeltaInterpreter;
import edelta.tests.EdeltaInjectorProviderDerivedStateComputerWithoutInterpreter;
import edelta.tests.EdeltaInterpreterTest;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.xtend2.lib.StringConcatenation;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.XtextRunner;
import org.eclipse.xtext.xbase.lib.Procedures.Procedure1;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(XtextRunner.class)
@InjectWith(EdeltaInjectorProviderDerivedStateComputerWithoutInterpreter.class)
@SuppressWarnings("all")
public class EdeltaSafeInterpreterTest extends EdeltaInterpreterTest {
  @Inject
  private Injector injector;
  
  @Override
  public IEdeltaInterpreter createInterpreter() {
    return this.injector.<EdeltaSafeInterpreter>getInstance(EdeltaSafeInterpreter.class);
  }
  
  @Test
  @Override
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
    this.assertAfterInterpretationOfEdeltaModifyEcoreOperation(input, false, _function);
  }
  
  @Test
  @Override
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
    this.assertAfterInterpretationOfEdeltaModifyEcoreOperation(_builder, false, _function);
  }
  
  @Test(expected = EdeltaSafeInterpreter.EdeltaInterpreterRuntimeException.class)
  public void testEdeltaInterpreterRuntimeExceptionIsThrown() {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("import org.eclipse.emf.ecore.EClass");
    _builder.newLine();
    _builder.append("import edelta.interpreter.EdeltaSafeInterpreter.EdeltaInterpreterRuntimeException");
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
    this.assertAfterInterpretationOfEdeltaModifyEcoreOperation(_builder, _function);
  }
  
  @Test
  @Override
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
    this.assertAfterInterpretationOfEdeltaModifyEcoreOperation(_builder, _function);
  }
}
