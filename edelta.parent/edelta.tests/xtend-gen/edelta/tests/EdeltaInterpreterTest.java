package edelta.tests;

import com.google.inject.Inject;
import com.google.inject.Injector;
import edelta.edelta.EdeltaModifyEcoreOperation;
import edelta.edelta.EdeltaPackage;
import edelta.edelta.EdeltaProgram;
import edelta.interpreter.EdeltaInterpreter;
import edelta.interpreter.EdeltaInterpreterRuntimeException;
import edelta.interpreter.IEdeltaInterpreter;
import edelta.tests.EdeltaAbstractTest;
import edelta.tests.EdeltaInjectorProviderDerivedStateComputerWithoutInterpreter;
import edelta.tests.additional.MyCustomEdeltaThatCannotBeLoadedAtRuntime;
import edelta.tests.additional.MyCustomException;
import edelta.util.EdeltaCopiedEPackagesMap;
import edelta.validation.EdeltaValidator;
import java.util.Map;
import org.assertj.core.api.AbstractThrowableAssert;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.ThrowableAssert;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.xtend2.lib.StringConcatenation;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.XtextRunner;
import org.eclipse.xtext.xbase.lib.Functions.Function1;
import org.eclipse.xtext.xbase.lib.IterableExtensions;
import org.eclipse.xtext.xbase.lib.ObjectExtensions;
import org.eclipse.xtext.xbase.lib.Procedures.Procedure1;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(XtextRunner.class)
@InjectWith(EdeltaInjectorProviderDerivedStateComputerWithoutInterpreter.class)
@SuppressWarnings("all")
public class EdeltaInterpreterTest extends EdeltaAbstractTest {
  protected IEdeltaInterpreter interpreter;
  
  @Inject
  private Injector injector;
  
  public IEdeltaInterpreter createInterpreter() {
    return this.injector.<EdeltaInterpreter>getInstance(EdeltaInterpreter.class);
  }
  
  @Before
  public void setupInterpreter() {
    this.interpreter = this.createInterpreter();
    this.interpreter.setInterpreterTimeout(1200000);
  }
  
  @Test
  public void testCreateEClassAndCallLibMethod() {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("metamodel \"foo\"");
    _builder.newLine();
    _builder.newLine();
    _builder.append("modifyEcore aTest epackage foo {");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("addNewEClass(\"NewClass\") [");
    _builder.newLine();
    _builder.append("\t\t");
    _builder.append("EStructuralFeatures += newEAttribute(\"newTestAttr\") [");
    _builder.newLine();
    _builder.append("\t\t\t");
    _builder.append("EType = ecoreref(FooDataType)");
    _builder.newLine();
    _builder.append("\t\t");
    _builder.append("]");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("]");
    _builder.newLine();
    _builder.append("}");
    _builder.newLine();
    final Procedure1<EPackage> _function = (EPackage ePackage) -> {
      final EClass derivedEClass = this.getLastEClass(ePackage);
      Assert.assertEquals("NewClass", derivedEClass.getName());
      Assert.assertEquals(1, derivedEClass.getEStructuralFeatures().size());
      final EStructuralFeature attr = IterableExtensions.<EStructuralFeature>head(derivedEClass.getEStructuralFeatures());
      Assert.assertEquals("newTestAttr", attr.getName());
      Assert.assertEquals("FooDataType", attr.getEType().getName());
    };
    this.assertAfterInterpretationOfEdeltaModifyEcoreOperation(_builder, _function);
  }
  
  @Test
  public void testCreateEClassAndCallOperationThatThrows() {
    final ThrowableAssert.ThrowingCallable _function = () -> {
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
      final Procedure1<EPackage> _function_1 = (EPackage it) -> {
      };
      this.assertAfterInterpretationOfEdeltaModifyEcoreOperation(_builder, _function_1);
    };
    Assertions.assertThatThrownBy(_function).isInstanceOf(EdeltaInterpreter.EdeltaInterpreterWrapperException.class).hasCauseExactlyInstanceOf(MyCustomException.class);
  }
  
  @Test
  public void testCreateEClassAndCallOperationFromUseAsReferringToUnknownType() {
    final ThrowableAssert.ThrowingCallable _function = () -> {
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
      final Procedure1<EPackage> _function_1 = (EPackage it) -> {
      };
      this.assertAfterInterpretationOfEdeltaModifyEcoreOperation(_builder, false, _function_1);
    };
    Assertions.assertThatThrownBy(_function).isInstanceOf(IllegalStateException.class).hasMessageContaining("Cannot resolve proxy");
  }
  
  @Test
  public void testCreateEClassAndCallOperationFromUseAsButNotFoundAtRuntime() {
    final ThrowableAssert.ThrowingCallable _function = () -> {
      StringConcatenation _builder = new StringConcatenation();
      _builder.append("import edelta.tests.additional.MyCustomEdeltaThatCannotBeLoadedAtRuntime");
      _builder.newLine();
      _builder.newLine();
      _builder.append("metamodel \"foo\"");
      _builder.newLine();
      _builder.newLine();
      _builder.append("use MyCustomEdeltaThatCannotBeLoadedAtRuntime as my");
      _builder.newLine();
      _builder.newLine();
      _builder.append("modifyEcore aTest epackage foo {");
      _builder.newLine();
      _builder.append("\t");
      _builder.append("my.aMethod()");
      _builder.newLine();
      _builder.append("}");
      _builder.newLine();
      final Procedure1<EPackage> _function_1 = (EPackage it) -> {
      };
      this.assertAfterInterpretationOfEdeltaModifyEcoreOperation(_builder, false, _function_1);
    };
    AbstractThrowableAssert<?, ? extends Throwable> _isInstanceOf = Assertions.assertThatThrownBy(_function).isInstanceOf(EdeltaInterpreterRuntimeException.class);
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("The type \'");
    String _name = MyCustomEdeltaThatCannotBeLoadedAtRuntime.class.getName();
    _builder.append(_name);
    _builder.append("\' has been resolved but cannot be loaded by the interpreter");
    _isInstanceOf.hasMessageContaining(_builder.toString());
  }
  
  @Test
  public void testCreateEClassAndCreateEAttribute() {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("metamodel \"foo\"");
    _builder.newLine();
    _builder.newLine();
    _builder.append("modifyEcore aTest epackage foo {");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("addNewEClass(\"NewClass\") [");
    _builder.newLine();
    _builder.append("\t\t");
    _builder.append("addNewEAttribute(\"newTestAttr\", ecoreref(FooDataType)) [");
    _builder.newLine();
    _builder.append("\t\t\t");
    _builder.append("lowerBound = -1");
    _builder.newLine();
    _builder.append("\t\t");
    _builder.append("]");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("]");
    _builder.newLine();
    _builder.append("}");
    _builder.newLine();
    final Procedure1<EPackage> _function = (EPackage ePackage) -> {
      final EClass derivedEClass = this.getLastEClass(ePackage);
      Assert.assertEquals("NewClass", derivedEClass.getName());
      Assert.assertEquals(1, derivedEClass.getEStructuralFeatures().size());
      final EStructuralFeature attr = IterableExtensions.<EStructuralFeature>head(derivedEClass.getEStructuralFeatures());
      Assert.assertEquals("newTestAttr", attr.getName());
      Assert.assertEquals((-1), attr.getLowerBound());
      Assert.assertEquals("FooDataType", attr.getEType().getName());
    };
    this.assertAfterInterpretationOfEdeltaModifyEcoreOperation(_builder, _function);
  }
  
  @Test
  public void testRenameEClassAndCreateEAttributeAndCallOperationFromUseAs() {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("import edelta.tests.additional.MyCustomEdelta");
    _builder.newLine();
    _builder.newLine();
    _builder.append("metamodel \"foo\"");
    _builder.newLine();
    _builder.newLine();
    _builder.append("use MyCustomEdelta as my");
    _builder.newLine();
    _builder.newLine();
    _builder.append("modifyEcore aTest epackage foo {");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("ecoreref(foo.FooClass).name = \"Renamed\"");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("ecoreref(Renamed).addNewEAttribute(\"newTestAttr\", ecoreref(FooDataType)) [");
    _builder.newLine();
    _builder.append("\t\t");
    _builder.append("my.setAttributeBounds(it, 1, -1)");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("]");
    _builder.newLine();
    _builder.append("}");
    _builder.newLine();
    final Procedure1<EPackage> _function = (EPackage ePackage) -> {
      final EClass derivedEClass = this.getFirstEClass(ePackage);
      Assert.assertEquals("Renamed", derivedEClass.getName());
      final EStructuralFeature attr = IterableExtensions.<EStructuralFeature>last(derivedEClass.getEStructuralFeatures());
      Assert.assertEquals("newTestAttr", attr.getName());
      Assert.assertEquals(1, attr.getLowerBound());
      Assert.assertEquals((-1), attr.getUpperBound());
      Assert.assertEquals("FooDataType", attr.getEType().getName());
    };
    this.assertAfterInterpretationOfEdeltaModifyEcoreOperation(_builder, _function);
  }
  
  @Test
  public void testEClassCreatedFromUseAs() {
    final Procedure1<EPackage> _function = (EPackage ePackage) -> {
      final EClass eClass = this.getLastEClass(ePackage);
      Assert.assertEquals("ANewClass", eClass.getName());
      EStructuralFeature _head = IterableExtensions.<EStructuralFeature>head(eClass.getEStructuralFeatures());
      Assert.assertEquals("aNewAttr", 
        ((EAttribute) _head).getName());
    };
    this.assertAfterInterpretationOfEdeltaModifyEcoreOperation(this._inputs.useAsCustomEdeltaCreatingEClass(), _function);
  }
  
  @Test
  public void testEClassCreatedFromUseAsAsExtension() {
    final Procedure1<EPackage> _function = (EPackage ePackage) -> {
      final EClass eClass = this.getLastEClass(ePackage);
      Assert.assertEquals("ANewClass", eClass.getName());
      EStructuralFeature _head = IterableExtensions.<EStructuralFeature>head(eClass.getEStructuralFeatures());
      Assert.assertEquals("aNewAttr", 
        ((EAttribute) _head).getName());
    };
    this.assertAfterInterpretationOfEdeltaModifyEcoreOperation(this._inputs.useAsCustomEdeltaAsExtensionCreatingEClass(), _function);
  }
  
  @Test
  public void testEClassCreatedFromStatefulUseAs() {
    final Procedure1<EPackage> _function = (EPackage ePackage) -> {
      final EClass eClass = this.getLastEClass(ePackage);
      Assert.assertEquals("ANewClass3", eClass.getName());
      EStructuralFeature _head = IterableExtensions.<EStructuralFeature>head(eClass.getEStructuralFeatures());
      Assert.assertEquals("aNewAttr4", 
        ((EAttribute) _head).getName());
    };
    this.assertAfterInterpretationOfEdeltaModifyEcoreOperation(this._inputs.useAsCustomStatefulEdeltaCreatingEClass(), _function);
  }
  
  @Test
  public void testNullBody() {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("import org.eclipse.emf.ecore.EClass");
    _builder.newLine();
    _builder.newLine();
    _builder.append("metamodel \"foo\"");
    _builder.newLine();
    _builder.newLine();
    _builder.append("modifyEcore aTest epackage foo {");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("addNewEClass(\"NewClass1\")");
    _builder.newLine();
    _builder.append("}");
    _builder.newLine();
    _builder.append("// here the body is null, but the interpreter");
    _builder.newLine();
    _builder.append("// avoids NPE");
    _builder.newLine();
    _builder.append("modifyEcore aTest2 epackage foo");
    _builder.newLine();
    final String input = _builder.toString();
    final Procedure1<EPackage> _function = (EPackage it) -> {
    };
    this.assertAfterInterpretationOfEdeltaModifyEcoreOperation(input, false, _function);
  }
  
  @Test
  public void testNullEcoreRefNamedElement() {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("import org.eclipse.emf.ecore.EClass");
    _builder.newLine();
    _builder.newLine();
    _builder.append("metamodel \"foo\"");
    _builder.newLine();
    _builder.newLine();
    _builder.append("modifyEcore aTest epackage foo {");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("addNewEClass(\"ANewClass1\")");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("ecoreref(");
    _builder.newLine();
    _builder.append("}");
    _builder.newLine();
    final String input = _builder.toString();
    final Procedure1<EPackage> _function = (EPackage ePackage) -> {
      final EClass eClass = this.getLastEClass(ePackage);
      Assert.assertEquals("ANewClass1", eClass.getName());
    };
    this.assertAfterInterpretationOfEdeltaModifyEcoreOperation(input, false, _function);
  }
  
  @Test(expected = IllegalArgumentException.class)
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
    final Procedure1<EPackage> _function = (EPackage it) -> {
    };
    this.assertAfterInterpretationOfEdeltaModifyEcoreOperation(input, false, _function);
  }
  
  @Test
  public void testUnresolvedEcoreReference() {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("import org.eclipse.emf.ecore.EClass");
    _builder.newLine();
    _builder.newLine();
    _builder.append("metamodel \"foo\"");
    _builder.newLine();
    _builder.newLine();
    _builder.append("modifyEcore aTest epackage foo {");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("addNewEClass(\"NewClass1\")");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("ecoreref(nonexist) // this won\'t break the interpreter");
    _builder.newLine();
    _builder.append("}");
    _builder.newLine();
    final String input = _builder.toString();
    final Procedure1<EPackage> _function = (EPackage derivedEPackage) -> {
      EClass _lastEClass = this.getLastEClass(derivedEPackage);
      final Procedure1<EClass> _function_1 = (EClass it) -> {
        Assert.assertEquals("NewClass1", it.getName());
      };
      ObjectExtensions.<EClass>operator_doubleArrow(_lastEClass, _function_1);
    };
    this.assertAfterInterpretationOfEdeltaModifyEcoreOperation(input, false, _function);
  }
  
  @Test
  public void testUnresolvedEcoreReferenceQualified() {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("import org.eclipse.emf.ecore.EClass");
    _builder.newLine();
    _builder.newLine();
    _builder.append("metamodel \"foo\"");
    _builder.newLine();
    _builder.newLine();
    _builder.append("modifyEcore aTest epackage foo {");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("addNewEClass(\"NewClass1\")");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("ecoreref(nonexist.bang) // this won\'t break the interpreter");
    _builder.newLine();
    _builder.append("}");
    _builder.newLine();
    final String input = _builder.toString();
    final Procedure1<EPackage> _function = (EPackage derivedEPackage) -> {
      EClass _lastEClass = this.getLastEClass(derivedEPackage);
      final Procedure1<EClass> _function_1 = (EClass it) -> {
        Assert.assertEquals("NewClass1", it.getName());
      };
      ObjectExtensions.<EClass>operator_doubleArrow(_lastEClass, _function_1);
    };
    this.assertAfterInterpretationOfEdeltaModifyEcoreOperation(input, false, _function);
  }
  
  @Test
  public void testModifyOperationCreateEClass() {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("package test");
    _builder.newLine();
    _builder.newLine();
    _builder.append("metamodel \"foo\"");
    _builder.newLine();
    _builder.newLine();
    _builder.append("modifyEcore aModificationTest epackage foo {");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("EClassifiers += newEClass(\"ANewClass\") [");
    _builder.newLine();
    _builder.append("\t\t");
    _builder.append("ESuperTypes += newEClass(\"Base\")");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("]");
    _builder.newLine();
    _builder.append("}");
    _builder.newLine();
    final String input = _builder.toString();
    final Procedure1<EPackage> _function = (EPackage derivedEPackage) -> {
      EClass _lastEClass = this.getLastEClass(derivedEPackage);
      final Procedure1<EClass> _function_1 = (EClass it) -> {
        Assert.assertEquals("ANewClass", it.getName());
        Assert.assertEquals("Base", IterableExtensions.<EClass>last(it.getESuperTypes()).getName());
      };
      ObjectExtensions.<EClass>operator_doubleArrow(_lastEClass, _function_1);
    };
    this.assertAfterInterpretationOfEdeltaModifyEcoreOperation(input, _function);
  }
  
  @Test
  public void testModifyEcoreAndCallOperation() {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("import org.eclipse.emf.ecore.EClass");
    _builder.newLine();
    _builder.newLine();
    _builder.append("metamodel \"foo\"");
    _builder.newLine();
    _builder.newLine();
    _builder.append("def op(EClass c) : void {");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("c.abstract = true");
    _builder.newLine();
    _builder.append("}");
    _builder.newLine();
    _builder.newLine();
    _builder.append("modifyEcore aModificationTest epackage foo {");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("EClassifiers += newEClass(\"ANewClass\") [");
    _builder.newLine();
    _builder.append("\t\t");
    _builder.append("ESuperTypes += newEClass(\"Base\")");
    _builder.newLine();
    _builder.append("\t\t");
    _builder.append("op(it)");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("]");
    _builder.newLine();
    _builder.append("}");
    _builder.newLine();
    final Procedure1<EPackage> _function = (EPackage derivedEPackage) -> {
      EClass _lastEClass = this.getLastEClass(derivedEPackage);
      final Procedure1<EClass> _function_1 = (EClass it) -> {
        Assert.assertEquals("ANewClass", it.getName());
        Assert.assertEquals("Base", IterableExtensions.<EClass>last(it.getESuperTypes()).getName());
        Assert.assertTrue(it.isAbstract());
      };
      ObjectExtensions.<EClass>operator_doubleArrow(_lastEClass, _function_1);
    };
    this.assertAfterInterpretationOfEdeltaModifyEcoreOperation(_builder, _function);
  }
  
  @Test
  public void testModifyEcoreRenameClassAndAddAttribute() {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("import org.eclipse.emf.ecore.EClass");
    _builder.newLine();
    _builder.newLine();
    _builder.append("metamodel \"foo\"");
    _builder.newLine();
    _builder.newLine();
    _builder.append("def op(EClass c) : void {");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("c.abstract = true");
    _builder.newLine();
    _builder.append("}");
    _builder.newLine();
    _builder.newLine();
    _builder.append("modifyEcore aTest epackage foo {");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("ecoreref(foo.FooClass).name = \"RenamedClass\"");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("ecoreref(RenamedClass).ESuperTypes += newEClass(\"Base\")");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("op(ecoreref(RenamedClass))");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("ecoreref(RenamedClass).getEStructuralFeatures += newEAttribute(\"added\")");
    _builder.newLine();
    _builder.append("}");
    _builder.newLine();
    final Procedure1<EPackage> _function = (EPackage derivedEPackage) -> {
      EClass _firstEClass = this.getFirstEClass(derivedEPackage);
      final Procedure1<EClass> _function_1 = (EClass it) -> {
        Assert.assertEquals("RenamedClass", it.getName());
        Assert.assertEquals("Base", IterableExtensions.<EClass>last(it.getESuperTypes()).getName());
        Assert.assertTrue(it.isAbstract());
        Assert.assertEquals("added", IterableExtensions.<EStructuralFeature>last(it.getEStructuralFeatures()).getName());
      };
      ObjectExtensions.<EClass>operator_doubleArrow(_firstEClass, _function_1);
    };
    this.assertAfterInterpretationOfEdeltaModifyEcoreOperation(_builder, true, _function);
  }
  
  @Test
  public void testModifyEcoreRenameClassAndAddAttribute2() {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("import org.eclipse.emf.ecore.EClass");
    _builder.newLine();
    _builder.newLine();
    _builder.append("metamodel \"foo\"");
    _builder.newLine();
    _builder.newLine();
    _builder.append("modifyEcore aTest epackage foo {");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("ecoreref(foo.FooClass).name = \"RenamedClass\"");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("ecoreref(RenamedClass).getEStructuralFeatures += newEAttribute(\"added\")");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("ecoreref(RenamedClass.added)");
    _builder.newLine();
    _builder.append("}");
    _builder.newLine();
    final Procedure1<EPackage> _function = (EPackage derivedEPackage) -> {
      EClass _firstEClass = this.getFirstEClass(derivedEPackage);
      final Procedure1<EClass> _function_1 = (EClass it) -> {
        Assert.assertEquals("RenamedClass", it.getName());
        Assert.assertEquals("added", IterableExtensions.<EStructuralFeature>last(it.getEStructuralFeatures()).getName());
      };
      ObjectExtensions.<EClass>operator_doubleArrow(_firstEClass, _function_1);
    };
    this.assertAfterInterpretationOfEdeltaModifyEcoreOperation(_builder, true, _function);
  }
  
  @Test
  public void testTimeoutInCancelIndicator() {
    this.interpreter.setInterpreterTimeout(2000);
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("import org.eclipse.emf.ecore.EClass");
    _builder.newLine();
    _builder.newLine();
    _builder.append("metamodel \"foo\"");
    _builder.newLine();
    _builder.newLine();
    _builder.append("def op(EClass c) : void {");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("var i = 10;");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("while (i >= 0) {");
    _builder.newLine();
    _builder.append("\t\t");
    _builder.append("Thread.sleep(1000);");
    _builder.newLine();
    _builder.append("\t\t");
    _builder.append("i++");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("}");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("// this will never be executed");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("c.abstract = true");
    _builder.newLine();
    _builder.append("}");
    _builder.newLine();
    _builder.newLine();
    _builder.append("modifyEcore aModificationTest epackage foo {");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("EClassifiers += newEClass(\"ANewClass\")");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("op(EClassifiers.last as EClass)");
    _builder.newLine();
    _builder.append("}");
    _builder.newLine();
    final String input = _builder.toString();
    final Procedure1<EPackage> _function = (EPackage derivedEPackage) -> {
      EClass _lastEClass = this.getLastEClass(derivedEPackage);
      final Procedure1<EClass> _function_1 = (EClass it) -> {
        Assert.assertEquals("ANewClass", it.getName());
        Assert.assertEquals(Boolean.valueOf(false), Boolean.valueOf(it.isAbstract()));
        final int initialIndex = input.lastIndexOf("{");
        EClass _edeltaModifyEcoreOperation = EdeltaPackage.eINSTANCE.getEdeltaModifyEcoreOperation();
        int _lastIndexOf = input.lastIndexOf("}");
        int _minus = (_lastIndexOf - initialIndex);
        int _plus = (_minus + 1);
        this._validationTestHelper.assertWarning(it, _edeltaModifyEcoreOperation, 
          EdeltaValidator.INTERPRETER_TIMEOUT, initialIndex, _plus, 
          "Timeout interpreting initialization block");
      };
      ObjectExtensions.<EClass>operator_doubleArrow(_lastEClass, _function_1);
    };
    this.assertAfterInterpretationOfEdeltaModifyEcoreOperation(input, _function);
  }
  
  @Test
  public void testCreateEClassInSubPackage() {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("metamodel \"mainpackage\"");
    _builder.newLine();
    _builder.newLine();
    _builder.append("modifyEcore aTest epackage mainpackage {");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("ecoreref(mainsubpackage).addNewEClass(\"NewClass\") [");
    _builder.newLine();
    _builder.append("\t\t");
    _builder.append("EStructuralFeatures += newEAttribute(\"newTestAttr\") [");
    _builder.newLine();
    _builder.append("\t\t\t");
    _builder.append("EType = ecoreref(MainFooDataType)");
    _builder.newLine();
    _builder.append("\t\t");
    _builder.append("]");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("]");
    _builder.newLine();
    _builder.append("}");
    _builder.newLine();
    final Procedure1<EPackage> _function = (EPackage ePackage) -> {
      final EClass derivedEClass = this.getLastEClass(IterableExtensions.<EPackage>head(ePackage.getESubpackages()));
      Assert.assertEquals("NewClass", derivedEClass.getName());
      Assert.assertEquals(1, derivedEClass.getEStructuralFeatures().size());
      final EStructuralFeature attr = IterableExtensions.<EStructuralFeature>head(derivedEClass.getEStructuralFeatures());
      Assert.assertEquals("newTestAttr", attr.getName());
      Assert.assertEquals("MainFooDataType", attr.getEType().getName());
    };
    this.assertAfterInterpretationOfEdeltaModifyEcoreOperation(this.parseWithTestEcoreWithSubPackage(_builder), true, _function);
  }
  
  @Test
  public void testCreateEClassInSubSubPackage() {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("metamodel \"mainpackage\"");
    _builder.newLine();
    _builder.newLine();
    _builder.append("modifyEcore aTest epackage mainpackage {");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("ecoreref(subsubpackage).addNewEClass(\"NewClass\") [");
    _builder.newLine();
    _builder.append("\t\t");
    _builder.append("EStructuralFeatures += newEAttribute(\"newTestAttr\") [");
    _builder.newLine();
    _builder.append("\t\t\t");
    _builder.append("EType = ecoreref(MainFooDataType)");
    _builder.newLine();
    _builder.append("\t\t");
    _builder.append("]");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("]");
    _builder.newLine();
    _builder.append("}");
    _builder.newLine();
    final Procedure1<EPackage> _function = (EPackage ePackage) -> {
      final EClass derivedEClass = this.getLastEClass(IterableExtensions.<EPackage>head(IterableExtensions.<EPackage>head(ePackage.getESubpackages()).getESubpackages()));
      Assert.assertEquals("NewClass", derivedEClass.getName());
      Assert.assertEquals(1, derivedEClass.getEStructuralFeatures().size());
      final EStructuralFeature attr = IterableExtensions.<EStructuralFeature>head(derivedEClass.getEStructuralFeatures());
      Assert.assertEquals("newTestAttr", attr.getName());
      Assert.assertEquals("MainFooDataType", attr.getEType().getName());
    };
    this.assertAfterInterpretationOfEdeltaModifyEcoreOperation(this.parseWithTestEcoreWithSubPackage(_builder), true, _function);
  }
  
  @Test
  public void testCreateEClassInNewSubPackage() {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("import org.eclipse.emf.ecore.EcoreFactory");
    _builder.newLine();
    _builder.newLine();
    _builder.append("metamodel \"mainpackage\"");
    _builder.newLine();
    _builder.newLine();
    _builder.append("modifyEcore aTest epackage mainpackage {");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("ESubpackages += EcoreFactory.eINSTANCE.createEPackage => [");
    _builder.newLine();
    _builder.append("\t\t");
    _builder.append("name = \"anewsubpackage\"");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("]");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("ecoreref(anewsubpackage).addNewEClass(\"NewClass\") [");
    _builder.newLine();
    _builder.append("\t\t");
    _builder.append("EStructuralFeatures += newEAttribute(\"newTestAttr\") [");
    _builder.newLine();
    _builder.append("\t\t\t");
    _builder.append("EType = ecoreref(MainFooDataType)");
    _builder.newLine();
    _builder.append("\t\t");
    _builder.append("]");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("]");
    _builder.newLine();
    _builder.append("}");
    _builder.newLine();
    final Procedure1<EPackage> _function = (EPackage ePackage) -> {
      final EPackage newSubPackage = IterableExtensions.<EPackage>last(ePackage.getESubpackages());
      Assert.assertEquals("anewsubpackage", newSubPackage.getName());
      final EClass derivedEClass = this.getLastEClass(IterableExtensions.<EPackage>last(ePackage.getESubpackages()));
      Assert.assertEquals("NewClass", derivedEClass.getName());
      Assert.assertEquals(1, derivedEClass.getEStructuralFeatures().size());
      final EStructuralFeature attr = IterableExtensions.<EStructuralFeature>head(derivedEClass.getEStructuralFeatures());
      Assert.assertEquals("newTestAttr", attr.getName());
      Assert.assertEquals("MainFooDataType", attr.getEType().getName());
    };
    this.assertAfterInterpretationOfEdeltaModifyEcoreOperation(this.parseWithTestEcoreWithSubPackage(_builder), true, _function);
  }
  
  @Test
  public void testComplexInterpretationWithRenamingAndSubPackages() {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("import org.eclipse.emf.ecore.EcoreFactory");
    _builder.newLine();
    _builder.newLine();
    _builder.append("metamodel \"mainpackage\"");
    _builder.newLine();
    _builder.newLine();
    _builder.append("modifyEcore aTest epackage mainpackage {");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("ESubpackages += EcoreFactory.eINSTANCE.createEPackage => [");
    _builder.newLine();
    _builder.append("\t\t");
    _builder.append("name = \"anewsubpackage\"");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("]");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("ecoreref(anewsubpackage).addNewEClass(\"NewClass\") [");
    _builder.newLine();
    _builder.append("\t\t");
    _builder.append("EStructuralFeatures += newEAttribute(\"newTestAttr\") [");
    _builder.newLine();
    _builder.append("\t\t\t");
    _builder.append("EType = ecoreref(MainFooDataType)");
    _builder.newLine();
    _builder.append("\t\t");
    _builder.append("]");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("]");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("ecoreref(NewClass).name = \"RenamedClass\"");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("ecoreref(RenamedClass).getEStructuralFeatures +=");
    _builder.newLine();
    _builder.append("\t\t");
    _builder.append("newEAttribute(\"added\", ecoreref(MainFooDataType))");
    _builder.newLine();
    _builder.append("}");
    _builder.newLine();
    final Procedure1<EPackage> _function = (EPackage ePackage) -> {
      final EPackage newSubPackage = IterableExtensions.<EPackage>last(ePackage.getESubpackages());
      Assert.assertEquals("anewsubpackage", newSubPackage.getName());
      final EClass derivedEClass = this.getLastEClass(IterableExtensions.<EPackage>last(ePackage.getESubpackages()));
      Assert.assertEquals("RenamedClass", derivedEClass.getName());
      Assert.assertEquals(2, derivedEClass.getEStructuralFeatures().size());
      final EStructuralFeature attr1 = IterableExtensions.<EStructuralFeature>head(derivedEClass.getEStructuralFeatures());
      Assert.assertEquals("newTestAttr", attr1.getName());
      Assert.assertEquals("MainFooDataType", attr1.getEType().getName());
      final EStructuralFeature attr2 = IterableExtensions.<EStructuralFeature>last(derivedEClass.getEStructuralFeatures());
      Assert.assertEquals("added", attr2.getName());
      Assert.assertEquals("MainFooDataType", attr2.getEType().getName());
    };
    this.assertAfterInterpretationOfEdeltaModifyEcoreOperation(this.parseWithTestEcoreWithSubPackage(_builder), true, _function);
  }
  
  @Test
  public void testInterpreterOnSubPackageIsNotExecuted() {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("metamodel \"mainpackage.mainsubpackage\"");
    _builder.newLine();
    _builder.newLine();
    _builder.append("modifyEcore aTest epackage mainsubpackage {");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("// this should not be executed");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("throw new MyCustomException");
    _builder.newLine();
    _builder.append("}");
    _builder.newLine();
    final Procedure1<EPackage> _function = (EPackage ePackage) -> {
    };
    this.assertAfterInterpretationOfEdeltaModifyEcoreOperation(this.parseWithTestEcoreWithSubPackage(_builder), false, _function);
  }
  
  protected void assertAfterInterpretationOfEdeltaModifyEcoreOperation(final CharSequence input, final Procedure1<? super EPackage> testExecutor) {
    this.assertAfterInterpretationOfEdeltaModifyEcoreOperation(input, true, testExecutor);
  }
  
  protected void assertAfterInterpretationOfEdeltaModifyEcoreOperation(final CharSequence input, final boolean doValidate, final Procedure1<? super EPackage> testExecutor) {
    final EdeltaProgram program = this.parseWithTestEcore(input);
    this.assertAfterInterpretationOfEdeltaModifyEcoreOperation(program, doValidate, testExecutor);
  }
  
  protected void assertAfterInterpretationOfEdeltaModifyEcoreOperation(final EdeltaProgram program, final boolean doValidate, final Procedure1<? super EPackage> testExecutor) {
    this.assertAfterInterpretationOfEdeltaModifyEcoreOperation(this.interpreter, program, doValidate, testExecutor);
    if (doValidate) {
      this._validationTestHelper.assertNoErrors(program);
    }
  }
  
  private void assertAfterInterpretationOfEdeltaModifyEcoreOperation(final IEdeltaInterpreter interpreter, final EdeltaProgram program, final boolean doValidate, final Procedure1<? super EPackage> testExecutor) {
    final EdeltaModifyEcoreOperation it = this.lastModifyEcoreOperation(program);
    final Function1<EPackage, String> _function = (EPackage it_1) -> {
      return it_1.getName();
    };
    Map<String, EPackage> _map = IterableExtensions.<String, EPackage>toMap(this.getCopiedEPackages(it), _function);
    final EdeltaCopiedEPackagesMap copiedEPackagesMap = new EdeltaCopiedEPackagesMap(_map);
    interpreter.evaluateModifyEcoreOperations(program, copiedEPackagesMap);
    final String packageName = it.getEpackage().getName();
    final EPackage epackage = copiedEPackagesMap.get(packageName);
    testExecutor.apply(epackage);
  }
}
