package edelta.tests;

import com.google.inject.Inject;
import com.google.inject.Injector;
import edelta.edelta.EdeltaEcoreReferenceExpression;
import edelta.edelta.EdeltaModifyEcoreOperation;
import edelta.edelta.EdeltaPackage;
import edelta.edelta.EdeltaProgram;
import edelta.interpreter.EdeltaInterpreter;
import edelta.interpreter.EdeltaInterpreterFactory;
import edelta.interpreter.EdeltaInterpreterRuntimeException;
import edelta.interpreter.EdeltaInterpreterWrapperException;
import edelta.resource.derivedstate.EdeltaCopiedEPackagesMap;
import edelta.resource.derivedstate.EdeltaDerivedStateHelper;
import edelta.tests.EdeltaAbstractTest;
import edelta.tests.EdeltaInjectorProviderDerivedStateComputerWithoutInterpreter;
import edelta.tests.additional.MyCustomEdeltaThatCannotBeLoadedAtRuntime;
import edelta.tests.additional.MyCustomException;
import edelta.validation.EdeltaValidator;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.assertj.core.api.AbstractThrowableAssert;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.ThrowableAssert;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.ENamedElement;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.xtend2.lib.StringConcatenation;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.XtextRunner;
import org.eclipse.xtext.xbase.XExpression;
import org.eclipse.xtext.xbase.XbasePackage;
import org.eclipse.xtext.xbase.lib.CollectionLiterals;
import org.eclipse.xtext.xbase.lib.Exceptions;
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
  @Inject
  private EdeltaInterpreter interpreter;
  
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
      final EdeltaInterpreter anotherInterprter = interpreterFactory.create(this._parseHelper.parse("").eResource());
      Assertions.assertThat(anotherInterprter.getClass()).isSameAs(this.interpreter.getClass());
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
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
    _builder.append("EStructuralFeatures += newEAttribute(\"newTestAttr\", ecoreref(FooDataType))");
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
    Assertions.assertThatThrownBy(_function).isInstanceOf(EdeltaInterpreterWrapperException.class).hasCauseExactlyInstanceOf(MyCustomException.class);
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
    _builder.append("ecoreref( // EdeltaEcoreReference.enamedelement is null");
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
  
  @Test
  public void testNullEcoreRef() {
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
    _builder.append("ecoreref // EdeltaEcoreReference is null");
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
    _builder.append("ecoreref(RenamedClass).getEStructuralFeatures +=");
    _builder.newLine();
    _builder.append("\t\t");
    _builder.append("newEAttribute(\"added\", ecoreref(FooDataType))");
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
    _builder.append("ecoreref(RenamedClass).getEStructuralFeatures +=");
    _builder.newLine();
    _builder.append("\t\t");
    _builder.append("newEAttribute(\"added\", ecoreref(FooDataType))");
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
  public void testTimeoutWarning() {
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
        final String offendingString = "Thread.sleep(1000)";
        final int initialIndex = input.lastIndexOf(offendingString);
        this._validationTestHelper.assertWarning(it, 
          XbasePackage.eINSTANCE.getXMemberFeatureCall(), 
          EdeltaValidator.INTERPRETER_TIMEOUT, initialIndex, offendingString.length(), 
          "Timeout while interpreting");
      };
      ObjectExtensions.<EClass>operator_doubleArrow(_lastEClass, _function_1);
    };
    this.assertAfterInterpretationOfEdeltaModifyEcoreOperation(input, _function);
  }
  
  @Test
  public void testTimeoutWarningWhenCallingJavaCode() {
    this.interpreter.setInterpreterTimeout(2000);
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("import org.eclipse.emf.ecore.EClass");
    _builder.newLine();
    _builder.append("import edelta.tests.additional.MyCustomEdeltaWithTimeout");
    _builder.newLine();
    _builder.newLine();
    _builder.append("metamodel \"foo\"");
    _builder.newLine();
    _builder.newLine();
    _builder.append("use MyCustomEdeltaWithTimeout as extension mylib");
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
        final String offendingString = "op(EClassifiers.last as EClass)";
        final int initialIndex = input.lastIndexOf(offendingString);
        this._validationTestHelper.assertWarning(it, 
          XbasePackage.eINSTANCE.getXFeatureCall(), 
          EdeltaValidator.INTERPRETER_TIMEOUT, initialIndex, offendingString.length(), 
          "Timeout while interpreting");
      };
      ObjectExtensions.<EClass>operator_doubleArrow(_lastEClass, _function_1);
    };
    this.assertAfterInterpretationOfEdeltaModifyEcoreOperation(input, _function);
  }
  
  @Test
  public void testTimeoutWarningWithSeveralFiles() {
    this.interpreter.setInterpreterTimeout(2000);
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("import org.eclipse.emf.ecore.EClass");
    _builder.newLine();
    _builder.newLine();
    _builder.append("def op1(EClass c) : void {");
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
    final String lib1 = _builder.toString();
    StringConcatenation _builder_1 = new StringConcatenation();
    _builder_1.append("import org.eclipse.emf.ecore.EClass");
    _builder_1.newLine();
    _builder_1.newLine();
    _builder_1.append("import edelta.__synthetic0");
    _builder_1.newLine();
    _builder_1.newLine();
    _builder_1.append("use __synthetic0 as extension mylib1");
    _builder_1.newLine();
    _builder_1.newLine();
    _builder_1.append("def op(EClass c) : void {");
    _builder_1.newLine();
    _builder_1.append("\t");
    _builder_1.append("op1(c)");
    _builder_1.newLine();
    _builder_1.append("}");
    _builder_1.newLine();
    final String lib2 = _builder_1.toString();
    StringConcatenation _builder_2 = new StringConcatenation();
    _builder_2.append("import org.eclipse.emf.ecore.EClass");
    _builder_2.newLine();
    _builder_2.append("import edelta.__synthetic1");
    _builder_2.newLine();
    _builder_2.newLine();
    _builder_2.append("metamodel \"foo\"");
    _builder_2.newLine();
    _builder_2.newLine();
    _builder_2.append("use __synthetic1 as extension mylib");
    _builder_2.newLine();
    _builder_2.newLine();
    _builder_2.append("modifyEcore aModificationTest epackage foo {");
    _builder_2.newLine();
    _builder_2.append("\t");
    _builder_2.append("EClassifiers += newEClass(\"ANewClass\")");
    _builder_2.newLine();
    _builder_2.append("\t");
    _builder_2.append("op(EClassifiers.last as EClass)");
    _builder_2.newLine();
    _builder_2.append("}");
    _builder_2.newLine();
    final String input = _builder_2.toString();
    final Procedure1<EPackage> _function = (EPackage derivedEPackage) -> {
      EClass _lastEClass = this.getLastEClass(derivedEPackage);
      final Procedure1<EClass> _function_1 = (EClass it) -> {
        Assert.assertEquals("ANewClass", it.getName());
        Assert.assertEquals(Boolean.valueOf(false), Boolean.valueOf(it.isAbstract()));
        final String offendingString = "op(EClassifiers.last as EClass)";
        final int initialIndex = input.lastIndexOf(offendingString);
        this._validationTestHelper.assertWarning(it, 
          XbasePackage.eINSTANCE.getXFeatureCall(), 
          EdeltaValidator.INTERPRETER_TIMEOUT, initialIndex, offendingString.length(), 
          "Timeout while interpreting");
      };
      ObjectExtensions.<EClass>operator_doubleArrow(_lastEClass, _function_1);
    };
    this.assertAfterInterpretationOfEdeltaModifyEcoreOperation(Collections.<CharSequence>unmodifiableList(CollectionLiterals.<CharSequence>newArrayList(lib1, lib2, input)), true, _function);
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
    _builder.append("EStructuralFeatures += newEAttribute(\"newTestAttr\", ecoreref(MainFooDataType))");
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
    _builder.append("EStructuralFeatures += newEAttribute(\"newTestAttr\", ecoreref(MainFooDataType))");
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
    _builder.append("EStructuralFeatures += newEAttribute(\"newTestAttr\", ecoreref(MainFooDataType))");
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
    _builder.append("EStructuralFeatures += newEAttribute(\"newTestAttr\", ecoreref(MainFooDataType))");
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
  
  @Test
  public void testModifyEcoreAndCallOperationFromExternalUseAs() {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("import org.eclipse.emf.ecore.EClass");
    _builder.newLine();
    _builder.newLine();
    _builder.append("package test1");
    _builder.newLine();
    _builder.newLine();
    _builder.append("def op(EClass c) : void {");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("c.op2");
    _builder.newLine();
    _builder.append("}");
    _builder.newLine();
    _builder.append("def op2(EClass c) : void {");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("c.abstract = true");
    _builder.newLine();
    _builder.append("}");
    _builder.newLine();
    StringConcatenation _builder_1 = new StringConcatenation();
    _builder_1.append("import org.eclipse.emf.ecore.EClass");
    _builder_1.newLine();
    _builder_1.append("import test1.__synthetic0");
    _builder_1.newLine();
    _builder_1.newLine();
    _builder_1.append("package test2");
    _builder_1.newLine();
    _builder_1.newLine();
    _builder_1.append("metamodel \"foo\"");
    _builder_1.newLine();
    _builder_1.newLine();
    _builder_1.append("use test1.__synthetic0 as my");
    _builder_1.newLine();
    _builder_1.newLine();
    _builder_1.append("modifyEcore aModificationTest epackage foo {");
    _builder_1.newLine();
    _builder_1.append("\t");
    _builder_1.append("my.op(ecoreref(FooClass))");
    _builder_1.newLine();
    _builder_1.append("}");
    _builder_1.newLine();
    final Procedure1<EPackage> _function = (EPackage derivedEPackage) -> {
      EClass _firstEClass = this.getFirstEClass(derivedEPackage);
      final Procedure1<EClass> _function_1 = (EClass it) -> {
        Assert.assertTrue(it.isAbstract());
      };
      ObjectExtensions.<EClass>operator_doubleArrow(_firstEClass, _function_1);
    };
    this.assertAfterInterpretationOfEdeltaModifyEcoreOperation(
      Collections.<CharSequence>unmodifiableList(CollectionLiterals.<CharSequence>newArrayList(_builder, _builder_1)), true, _function);
  }
  
  @Test
  public void testModifyEcoreAndCallOperationFromExternalUseAsExtension() {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("import org.eclipse.emf.ecore.EClass");
    _builder.newLine();
    _builder.newLine();
    _builder.append("package test1");
    _builder.newLine();
    _builder.newLine();
    _builder.append("def op(EClass c) : void {");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("c.op2");
    _builder.newLine();
    _builder.append("}");
    _builder.newLine();
    _builder.append("def op2(EClass c) : void {");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("c.abstract = true");
    _builder.newLine();
    _builder.append("}");
    _builder.newLine();
    StringConcatenation _builder_1 = new StringConcatenation();
    _builder_1.append("import org.eclipse.emf.ecore.EClass");
    _builder_1.newLine();
    _builder_1.append("import test1.__synthetic0");
    _builder_1.newLine();
    _builder_1.newLine();
    _builder_1.append("package test2");
    _builder_1.newLine();
    _builder_1.newLine();
    _builder_1.append("metamodel \"foo\"");
    _builder_1.newLine();
    _builder_1.newLine();
    _builder_1.append("use test1.__synthetic0 as extension my");
    _builder_1.newLine();
    _builder_1.newLine();
    _builder_1.append("modifyEcore aModificationTest epackage foo {");
    _builder_1.newLine();
    _builder_1.append("\t");
    _builder_1.append("ecoreref(FooClass).op");
    _builder_1.newLine();
    _builder_1.append("}");
    _builder_1.newLine();
    final Procedure1<EPackage> _function = (EPackage derivedEPackage) -> {
      EClass _firstEClass = this.getFirstEClass(derivedEPackage);
      final Procedure1<EClass> _function_1 = (EClass it) -> {
        Assert.assertTrue(it.isAbstract());
      };
      ObjectExtensions.<EClass>operator_doubleArrow(_firstEClass, _function_1);
    };
    this.assertAfterInterpretationOfEdeltaModifyEcoreOperation(
      Collections.<CharSequence>unmodifiableList(CollectionLiterals.<CharSequence>newArrayList(_builder, _builder_1)), true, _function);
  }
  
  @Test
  public void testModifyEcoreAndCallOperationFromExternalUseAsWithSeveralFiles() {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("import org.eclipse.emf.ecore.EClass");
    _builder.newLine();
    _builder.newLine();
    _builder.append("package test1");
    _builder.newLine();
    _builder.newLine();
    _builder.append("def op2(EClass c) : void {");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("c.abstract = true");
    _builder.newLine();
    _builder.append("}");
    _builder.newLine();
    StringConcatenation _builder_1 = new StringConcatenation();
    _builder_1.append("import org.eclipse.emf.ecore.EClass");
    _builder_1.newLine();
    _builder_1.append("import test1.__synthetic0");
    _builder_1.newLine();
    _builder_1.newLine();
    _builder_1.append("package test2");
    _builder_1.newLine();
    _builder_1.newLine();
    _builder_1.append("use test1.__synthetic0 as extension my");
    _builder_1.newLine();
    _builder_1.newLine();
    _builder_1.append("def op(EClass c) : void {");
    _builder_1.newLine();
    _builder_1.append("\t");
    _builder_1.append("c.op2");
    _builder_1.newLine();
    _builder_1.append("}");
    _builder_1.newLine();
    StringConcatenation _builder_2 = new StringConcatenation();
    _builder_2.append("import org.eclipse.emf.ecore.EClass");
    _builder_2.newLine();
    _builder_2.append("import test2.__synthetic1");
    _builder_2.newLine();
    _builder_2.newLine();
    _builder_2.append("package test3");
    _builder_2.newLine();
    _builder_2.newLine();
    _builder_2.append("metamodel \"foo\"");
    _builder_2.newLine();
    _builder_2.newLine();
    _builder_2.append("use test2.__synthetic1 as extension my");
    _builder_2.newLine();
    _builder_2.newLine();
    _builder_2.append("modifyEcore aModificationTest epackage foo {");
    _builder_2.newLine();
    _builder_2.append("\t");
    _builder_2.append("ecoreref(FooClass).op");
    _builder_2.newLine();
    _builder_2.append("}");
    _builder_2.newLine();
    final Procedure1<EPackage> _function = (EPackage derivedEPackage) -> {
      EClass _firstEClass = this.getFirstEClass(derivedEPackage);
      final Procedure1<EClass> _function_1 = (EClass it) -> {
        Assert.assertTrue(it.isAbstract());
      };
      ObjectExtensions.<EClass>operator_doubleArrow(_firstEClass, _function_1);
    };
    this.assertAfterInterpretationOfEdeltaModifyEcoreOperation(
      Collections.<CharSequence>unmodifiableList(CollectionLiterals.<CharSequence>newArrayList(_builder, _builder_1, _builder_2)), true, _function);
  }
  
  @Test
  public void testElementExpressionForCreatedEClassWithEdeltaAPI() {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("import org.eclipse.emf.ecore.EcoreFactory");
    _builder.newLine();
    _builder.newLine();
    _builder.append("metamodel \"foo\"");
    _builder.newLine();
    _builder.newLine();
    _builder.append("modifyEcore aTest epackage foo {");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("addNewEClass(\"NewClass\")");
    _builder.newLine();
    _builder.append("}");
    _builder.newLine();
    EdeltaProgram _parseWithTestEcore = this.parseWithTestEcore(_builder);
    final Procedure1<EdeltaProgram> _function = (EdeltaProgram it) -> {
      final EdeltaCopiedEPackagesMap map = this.interpretProgram(it);
      final EClassifier createClass = map.get("foo").getEClassifier("NewClass");
      final XExpression exp = this.derivedStateHelper.getEnamedElementXExpressionMap(it.eResource()).get(createClass);
      Assert.assertNotNull(exp);
      Assert.assertEquals("addNewEClass", this.getFeatureCall(exp).getFeature().getSimpleName());
    };
    ObjectExtensions.<EdeltaProgram>operator_doubleArrow(_parseWithTestEcore, _function);
  }
  
  @Test
  public void testEcoreRefExpExpressionForCreatedEClassWithEdeltaAPI() {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("import org.eclipse.emf.ecore.EcoreFactory");
    _builder.newLine();
    _builder.newLine();
    _builder.append("metamodel \"foo\"");
    _builder.newLine();
    _builder.newLine();
    _builder.append("modifyEcore aTest epackage foo {");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("addNewEClass(\"NewClass\")");
    _builder.newLine();
    _builder.append("}");
    _builder.newLine();
    _builder.append("modifyEcore anotherTest epackage foo {");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("ecoreref(NewClass)");
    _builder.newLine();
    _builder.append("}");
    _builder.newLine();
    EdeltaProgram _parseWithTestEcore = this.parseWithTestEcore(_builder);
    final Procedure1<EdeltaProgram> _function = (EdeltaProgram it) -> {
      this.interpretProgram(it);
      EdeltaEcoreReferenceExpression _last = IterableExtensions.<EdeltaEcoreReferenceExpression>last(this.getAllEcoreReferenceExpressions(it));
      final Procedure1<EdeltaEcoreReferenceExpression> _function_1 = (EdeltaEcoreReferenceExpression it_1) -> {
        this.assertEcoreRefExpElementMapsToXExpression(it_1, it_1.getReference().getEnamedelement(), "addNewEClass");
      };
      ObjectExtensions.<EdeltaEcoreReferenceExpression>operator_doubleArrow(_last, _function_1);
    };
    ObjectExtensions.<EdeltaProgram>operator_doubleArrow(_parseWithTestEcore, _function);
  }
  
  @Test
  public void testEcoreRefExpExpressionForCreatedEClassWithOperation() {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("import org.eclipse.emf.ecore.EPackage");
    _builder.newLine();
    _builder.newLine();
    _builder.append("metamodel \"foo\"");
    _builder.newLine();
    _builder.newLine();
    _builder.append("def create(EPackage it) {");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("addNewEClass(\"NewClass\")");
    _builder.newLine();
    _builder.append("}");
    _builder.newLine();
    _builder.append("modifyEcore anotherTest epackage foo {");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("create(it)");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("ecoreref(NewClass)");
    _builder.newLine();
    _builder.append("}");
    _builder.newLine();
    EdeltaProgram _parseWithTestEcore = this.parseWithTestEcore(_builder);
    final Procedure1<EdeltaProgram> _function = (EdeltaProgram it) -> {
      this.interpretProgram(it);
      EdeltaEcoreReferenceExpression _last = IterableExtensions.<EdeltaEcoreReferenceExpression>last(this.getAllEcoreReferenceExpressions(it));
      final Procedure1<EdeltaEcoreReferenceExpression> _function_1 = (EdeltaEcoreReferenceExpression it_1) -> {
        this.assertEcoreRefExpElementMapsToXExpression(it_1, it_1.getReference().getEnamedelement(), "addNewEClass");
      };
      ObjectExtensions.<EdeltaEcoreReferenceExpression>operator_doubleArrow(_last, _function_1);
    };
    ObjectExtensions.<EdeltaProgram>operator_doubleArrow(_parseWithTestEcore, _function);
  }
  
  @Test
  public void testEcoreRefExpExpressionForCreatedEClassWithOperationInAnotherFile() {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("import org.eclipse.emf.ecore.EPackage");
    _builder.newLine();
    _builder.newLine();
    _builder.append("def create(EPackage it) {");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("addNewEClass(\"NewClass\")");
    _builder.newLine();
    _builder.append("}");
    _builder.newLine();
    StringConcatenation _builder_1 = new StringConcatenation();
    _builder_1.append("metamodel \"foo\"");
    _builder_1.newLine();
    _builder_1.newLine();
    _builder_1.append("use edelta.__synthetic0 as extension my");
    _builder_1.newLine();
    _builder_1.newLine();
    _builder_1.append("modifyEcore anotherTest epackage foo {");
    _builder_1.newLine();
    _builder_1.append("\t");
    _builder_1.append("create(it)");
    _builder_1.newLine();
    _builder_1.append("\t");
    _builder_1.append("ecoreref(NewClass)");
    _builder_1.newLine();
    _builder_1.append("}");
    _builder_1.newLine();
    EdeltaProgram _parseSeveralWithTestEcore = this.parseSeveralWithTestEcore(
      Collections.<CharSequence>unmodifiableList(CollectionLiterals.<CharSequence>newArrayList(_builder, _builder_1)));
    final Procedure1<EdeltaProgram> _function = (EdeltaProgram it) -> {
      this.interpretProgram(it);
      EdeltaEcoreReferenceExpression _last = IterableExtensions.<EdeltaEcoreReferenceExpression>last(this.getAllEcoreReferenceExpressions(it));
      final Procedure1<EdeltaEcoreReferenceExpression> _function_1 = (EdeltaEcoreReferenceExpression it_1) -> {
        this.assertEcoreRefExpElementMapsToXExpression(it_1, it_1.getReference().getEnamedelement(), "create");
      };
      ObjectExtensions.<EdeltaEcoreReferenceExpression>operator_doubleArrow(_last, _function_1);
    };
    ObjectExtensions.<EdeltaProgram>operator_doubleArrow(_parseSeveralWithTestEcore, _function);
  }
  
  @Test
  public void testEcoreRefExpForCreatedEClassRenamed() {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("import org.eclipse.emf.ecore.EcoreFactory");
    _builder.newLine();
    _builder.newLine();
    _builder.append("metamodel \"foo\"");
    _builder.newLine();
    _builder.newLine();
    _builder.append("modifyEcore aTest epackage foo {");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("addNewEClass(\"NewClass\")");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("ecoreref(NewClass).name = \"Renamed\"");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("ecoreref(Renamed)");
    _builder.newLine();
    _builder.append("}");
    _builder.newLine();
    EdeltaProgram _parseWithTestEcore = this.parseWithTestEcore(_builder);
    final Procedure1<EdeltaProgram> _function = (EdeltaProgram it) -> {
      this.interpretProgram(it);
      final List<EdeltaEcoreReferenceExpression> ecoreRefs = this.getAllEcoreReferenceExpressions(it);
      EdeltaEcoreReferenceExpression _head = IterableExtensions.<EdeltaEcoreReferenceExpression>head(ecoreRefs);
      final Procedure1<EdeltaEcoreReferenceExpression> _function_1 = (EdeltaEcoreReferenceExpression it_1) -> {
        this.assertEcoreRefExpElementMapsToXExpression(it_1, it_1.getReference().getEnamedelement(), "addNewEClass");
      };
      ObjectExtensions.<EdeltaEcoreReferenceExpression>operator_doubleArrow(_head, _function_1);
      EdeltaEcoreReferenceExpression _last = IterableExtensions.<EdeltaEcoreReferenceExpression>last(ecoreRefs);
      final Procedure1<EdeltaEcoreReferenceExpression> _function_2 = (EdeltaEcoreReferenceExpression it_1) -> {
        this.assertEcoreRefExpElementMapsToXExpression(it_1, it_1.getReference().getEnamedelement(), "setName");
      };
      ObjectExtensions.<EdeltaEcoreReferenceExpression>operator_doubleArrow(_last, _function_2);
    };
    ObjectExtensions.<EdeltaProgram>operator_doubleArrow(_parseWithTestEcore, _function);
  }
  
  @Test
  public void testEcoreRefExpForCreatedSubPackage() {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("import org.eclipse.emf.ecore.EcoreFactory");
    _builder.newLine();
    _builder.newLine();
    _builder.append("metamodel \"foo\"");
    _builder.newLine();
    _builder.newLine();
    _builder.append("modifyEcore aTest epackage foo {");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("addNewEClass(\"NewClass\")");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("ecoreref(NewClass) // 0");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("addNewESubpackage(\"subpackage\", \"subpackage\", \"subpackage\") [");
    _builder.newLine();
    _builder.append("\t\t");
    _builder.append("addEClass(ecoreref(NewClass)) // 1");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("]");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("ecoreref(NewClass) // 2");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("ecoreref(subpackage) // 3");
    _builder.newLine();
    _builder.append("}");
    _builder.newLine();
    EdeltaProgram _parseWithTestEcore = this.parseWithTestEcore(_builder);
    final Procedure1<EdeltaProgram> _function = (EdeltaProgram it) -> {
      this.interpretProgram(it);
      List<EdeltaEcoreReferenceExpression> _allEcoreReferenceExpressions = this.getAllEcoreReferenceExpressions(it);
      final Procedure1<List<EdeltaEcoreReferenceExpression>> _function_1 = (List<EdeltaEcoreReferenceExpression> it_1) -> {
        EdeltaEcoreReferenceExpression _get = it_1.get(0);
        final Procedure1<EdeltaEcoreReferenceExpression> _function_2 = (EdeltaEcoreReferenceExpression it_2) -> {
          this.assertEcoreRefExpElementMapsToXExpression(it_2, it_2.getReference().getEnamedelement(), "addNewEClass");
        };
        ObjectExtensions.<EdeltaEcoreReferenceExpression>operator_doubleArrow(_get, _function_2);
        EdeltaEcoreReferenceExpression _get_1 = it_1.get(1);
        final Procedure1<EdeltaEcoreReferenceExpression> _function_3 = (EdeltaEcoreReferenceExpression it_2) -> {
          this.assertEcoreRefExpElementMapsToXExpression(it_2, it_2.getReference().getEnamedelement(), "addNewEClass");
        };
        ObjectExtensions.<EdeltaEcoreReferenceExpression>operator_doubleArrow(_get_1, _function_3);
        EdeltaEcoreReferenceExpression _get_2 = it_1.get(2);
        final Procedure1<EdeltaEcoreReferenceExpression> _function_4 = (EdeltaEcoreReferenceExpression it_2) -> {
          this.assertEcoreRefExpElementMapsToXExpression(it_2, it_2.getReference().getEnamedelement(), "addEClass");
        };
        ObjectExtensions.<EdeltaEcoreReferenceExpression>operator_doubleArrow(_get_2, _function_4);
        EdeltaEcoreReferenceExpression _get_3 = it_1.get(3);
        final Procedure1<EdeltaEcoreReferenceExpression> _function_5 = (EdeltaEcoreReferenceExpression it_2) -> {
          this.assertEcoreRefExpElementMapsToXExpression(it_2, it_2.getReference().getEnamedelement(), "addNewESubpackage");
        };
        ObjectExtensions.<EdeltaEcoreReferenceExpression>operator_doubleArrow(_get_3, _function_5);
      };
      ObjectExtensions.<List<EdeltaEcoreReferenceExpression>>operator_doubleArrow(_allEcoreReferenceExpressions, _function_1);
    };
    ObjectExtensions.<EdeltaProgram>operator_doubleArrow(_parseWithTestEcore, _function);
  }
  
  @Test
  public void testEcoreRefExpForExistingEClass() {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("import org.eclipse.emf.ecore.EcoreFactory");
    _builder.newLine();
    _builder.newLine();
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
      this.interpretProgram(it);
      final List<EdeltaEcoreReferenceExpression> ecoreRefs = this.getAllEcoreReferenceExpressions(it);
      EdeltaEcoreReferenceExpression _head = IterableExtensions.<EdeltaEcoreReferenceExpression>head(ecoreRefs);
      final Procedure1<EdeltaEcoreReferenceExpression> _function_1 = (EdeltaEcoreReferenceExpression it_1) -> {
        final XExpression exp = this.derivedStateHelper.getEcoreReferenceExpressionState(it_1).getEnamedElementXExpressionMap().get(it_1.getReference().getEnamedelement());
        Assert.assertNull(exp);
      };
      ObjectExtensions.<EdeltaEcoreReferenceExpression>operator_doubleArrow(_head, _function_1);
    };
    ObjectExtensions.<EdeltaProgram>operator_doubleArrow(_parseWithTestEcore, _function);
  }
  
  @Test
  public void testEcoreRefExpForCreatedEClassRenamedInInitializer() {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("import org.eclipse.emf.ecore.EcoreFactory");
    _builder.newLine();
    _builder.newLine();
    _builder.append("metamodel \"foo\"");
    _builder.newLine();
    _builder.newLine();
    _builder.append("modifyEcore aTest epackage foo {");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("addNewEClass(\"NewClass\") [");
    _builder.newLine();
    _builder.append("\t\t");
    _builder.append("ecoreref(NewClass)");
    _builder.newLine();
    _builder.append("\t\t");
    _builder.append("name = \"Renamed\"");
    _builder.newLine();
    _builder.append("\t\t");
    _builder.append("abstract = true");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("]");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("ecoreref(Renamed)");
    _builder.newLine();
    _builder.append("}");
    _builder.newLine();
    EdeltaProgram _parseWithTestEcore = this.parseWithTestEcore(_builder);
    final Procedure1<EdeltaProgram> _function = (EdeltaProgram it) -> {
      this.interpretProgram(it);
      final List<EdeltaEcoreReferenceExpression> ecoreRefs = this.getAllEcoreReferenceExpressions(it);
      EdeltaEcoreReferenceExpression _head = IterableExtensions.<EdeltaEcoreReferenceExpression>head(ecoreRefs);
      final Procedure1<EdeltaEcoreReferenceExpression> _function_1 = (EdeltaEcoreReferenceExpression it_1) -> {
        this.assertEcoreRefExpElementMapsToXExpression(it_1, it_1.getReference().getEnamedelement(), "addNewEClass");
      };
      ObjectExtensions.<EdeltaEcoreReferenceExpression>operator_doubleArrow(_head, _function_1);
      EdeltaEcoreReferenceExpression _last = IterableExtensions.<EdeltaEcoreReferenceExpression>last(ecoreRefs);
      final Procedure1<EdeltaEcoreReferenceExpression> _function_2 = (EdeltaEcoreReferenceExpression it_1) -> {
        this.assertEcoreRefExpElementMapsToXExpression(it_1, it_1.getReference().getEnamedelement(), "setName");
      };
      ObjectExtensions.<EdeltaEcoreReferenceExpression>operator_doubleArrow(_last, _function_2);
    };
    ObjectExtensions.<EdeltaProgram>operator_doubleArrow(_parseWithTestEcore, _function);
  }
  
  @Test
  public void testElementExpressionMapForCreatedEClassWithEMFAPI() {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("import org.eclipse.emf.ecore.EcoreFactory");
    _builder.newLine();
    _builder.newLine();
    _builder.append("metamodel \"foo\"");
    _builder.newLine();
    _builder.newLine();
    _builder.append("modifyEcore aTest epackage foo {");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("EClassifiers += EcoreFactory.eINSTANCE.createEClass");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("EClassifiers.last.name = \"NewClass\"");
    _builder.newLine();
    _builder.append("}");
    _builder.newLine();
    _builder.append("modifyEcore anotherTest epackage foo {");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("ecoreref(NewClass)");
    _builder.newLine();
    _builder.append("}");
    _builder.newLine();
    EdeltaProgram _parseWithTestEcore = this.parseWithTestEcore(_builder);
    final Procedure1<EdeltaProgram> _function = (EdeltaProgram it) -> {
      final EdeltaCopiedEPackagesMap map = this.interpretProgram(it);
      final EClassifier createClass = map.get("foo").getEClassifier("NewClass");
      final XExpression exp = this.derivedStateHelper.getEnamedElementXExpressionMap(it.eResource()).get(createClass);
      Assert.assertNotNull(exp);
      Assert.assertEquals("setName", this.getFeatureCall(exp).getFeature().getSimpleName());
    };
    ObjectExtensions.<EdeltaProgram>operator_doubleArrow(_parseWithTestEcore, _function);
  }
  
  @Test
  public void testElementExpressionMapForCreatedEClassWithDoubleArrow() {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("import org.eclipse.emf.ecore.EcoreFactory");
    _builder.newLine();
    _builder.newLine();
    _builder.append("metamodel \"foo\"");
    _builder.newLine();
    _builder.newLine();
    _builder.append("modifyEcore aTest epackage foo {");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("EClassifiers += EcoreFactory.eINSTANCE.createEClass => [");
    _builder.newLine();
    _builder.append("\t\t");
    _builder.append("name = \"NewClass\"");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("]");
    _builder.newLine();
    _builder.append("}");
    _builder.newLine();
    _builder.append("modifyEcore anotherTest epackage foo {");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("ecoreref(NewClass)");
    _builder.newLine();
    _builder.append("}");
    _builder.newLine();
    EdeltaProgram _parseWithTestEcore = this.parseWithTestEcore(_builder);
    final Procedure1<EdeltaProgram> _function = (EdeltaProgram it) -> {
      final EdeltaCopiedEPackagesMap map = this.interpretProgram(it);
      final EClassifier createClass = map.get("foo").getEClassifier("NewClass");
      final XExpression exp = this.derivedStateHelper.getEnamedElementXExpressionMap(it.eResource()).get(createClass);
      Assert.assertNotNull(exp);
      Assert.assertEquals("setName", this.getFeatureCall(exp).getFeature().getSimpleName());
    };
    ObjectExtensions.<EdeltaProgram>operator_doubleArrow(_parseWithTestEcore, _function);
  }
  
  @Test
  public void testElementExpressionMapForCreatedEClassWithoutName() {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("import org.eclipse.emf.ecore.EcoreFactory");
    _builder.newLine();
    _builder.newLine();
    _builder.append("metamodel \"foo\"");
    _builder.newLine();
    _builder.newLine();
    _builder.append("modifyEcore aTest epackage foo {");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("EClassifiers += EcoreFactory.eINSTANCE.createEClass");
    _builder.newLine();
    _builder.append("}");
    _builder.newLine();
    EdeltaProgram _parseWithTestEcore = this.parseWithTestEcore(_builder);
    final Procedure1<EdeltaProgram> _function = (EdeltaProgram it) -> {
      final EdeltaCopiedEPackagesMap map = this.interpretProgram(it);
      final EClass createClass = this.getLastEClass(map.get("foo"));
      final XExpression exp = this.derivedStateHelper.getEnamedElementXExpressionMap(it.eResource()).get(createClass);
      Assert.assertNotNull(exp);
      Assert.assertEquals("operator_add", this.getFeatureCall(exp).getFeature().getSimpleName());
    };
    ObjectExtensions.<EdeltaProgram>operator_doubleArrow(_parseWithTestEcore, _function);
  }
  
  @Test
  public void testElementExpressionMapForCreatedEClassWithMethodCall() {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("import org.eclipse.emf.ecore.EcoreFactory");
    _builder.newLine();
    _builder.newLine();
    _builder.append("metamodel \"foo\"");
    _builder.newLine();
    _builder.newLine();
    _builder.append("modifyEcore aTest epackage foo {");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("getEClassifiers().add(EcoreFactory.eINSTANCE.createEClass)");
    _builder.newLine();
    _builder.append("}");
    _builder.newLine();
    EdeltaProgram _parseWithTestEcore = this.parseWithTestEcore(_builder);
    final Procedure1<EdeltaProgram> _function = (EdeltaProgram it) -> {
      final EdeltaCopiedEPackagesMap map = this.interpretProgram(it);
      final EClass createClass = this.getLastEClass(map.get("foo"));
      final XExpression exp = this.derivedStateHelper.getEnamedElementXExpressionMap(it.eResource()).get(createClass);
      Assert.assertNotNull(exp);
      Assert.assertEquals("add", this.getFeatureCall(exp).getFeature().getSimpleName());
    };
    ObjectExtensions.<EdeltaProgram>operator_doubleArrow(_parseWithTestEcore, _function);
  }
  
  @Test
  public void testReferenceToEClassRemoved() {
    final String input = this._inputs.referenceToEClassRemoved().toString();
    EdeltaProgram _parseWithTestEcore = this.parseWithTestEcore(input);
    final Procedure1<EdeltaProgram> _function = (EdeltaProgram it) -> {
      final ThrowableAssert.ThrowingCallable _function_1 = () -> {
        this.interpretProgram(it);
      };
      Assertions.assertThatThrownBy(_function_1).isInstanceOf(EdeltaInterpreterWrapperException.class);
      this._validationTestHelper.assertError(it, 
        EdeltaPackage.eINSTANCE.getEdeltaEcoreReferenceExpression(), 
        EdeltaValidator.INTERPRETER_ACCESS_REMOVED_ELEMENT, 
        input.lastIndexOf("FooClass"), 
        "FooClass".length(), 
        "The element is not available anymore in this context: \'FooClass\'");
      this.assertErrorsAsStrings(it, "The element is not available anymore in this context: \'FooClass\'");
    };
    ObjectExtensions.<EdeltaProgram>operator_doubleArrow(_parseWithTestEcore, _function);
  }
  
  @Test
  public void testReferenceToEClassRemovedInLoop() {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("metamodel \"foo\"");
    _builder.newLine();
    _builder.newLine();
    _builder.append("modifyEcore creation epackage foo {");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("addNewEClass(\"NewClass1\")");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("for (var i = 0; i < 3; i++)");
    _builder.newLine();
    _builder.append("\t\t");
    _builder.append("EClassifiers -= ecoreref(NewClass1) // the second time it doesn\'t exist anymore");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("addNewEClass(\"NewClass2\")");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("for (var i = 0; i < 3; i++)");
    _builder.newLine();
    _builder.append("\t\t");
    _builder.append("EClassifiers -= ecoreref(NewClass2) // the second time it doesn\'t exist anymore");
    _builder.newLine();
    _builder.append("}");
    _builder.newLine();
    final String input = _builder.toString();
    EdeltaProgram _parseWithTestEcore = this.parseWithTestEcore(input);
    final Procedure1<EdeltaProgram> _function = (EdeltaProgram it) -> {
      this.interpretProgram(it);
      this._validationTestHelper.assertError(it, 
        EdeltaPackage.eINSTANCE.getEdeltaEcoreReferenceExpression(), 
        EdeltaValidator.INTERPRETER_ACCESS_REMOVED_ELEMENT, 
        input.lastIndexOf("NewClass1"), 
        "NewClass1".length(), 
        "The element is not available anymore in this context: \'NewClass1\'");
      this._validationTestHelper.assertError(it, 
        EdeltaPackage.eINSTANCE.getEdeltaEcoreReferenceExpression(), 
        EdeltaValidator.INTERPRETER_ACCESS_REMOVED_ELEMENT, 
        input.lastIndexOf("NewClass2"), 
        "NewClass2".length(), 
        "The element is not available anymore in this context: \'NewClass2\'");
      StringConcatenation _builder_1 = new StringConcatenation();
      _builder_1.append("The element is not available anymore in this context: \'NewClass1\'");
      _builder_1.newLine();
      _builder_1.append("The element is not available anymore in this context: \'NewClass2\'");
      _builder_1.newLine();
      this.assertErrorsAsStrings(it, _builder_1);
    };
    ObjectExtensions.<EdeltaProgram>operator_doubleArrow(_parseWithTestEcore, _function);
  }
  
  @Test
  public void testReferenceToCreatedEClassRemoved() {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("metamodel \"foo\"");
    _builder.newLine();
    _builder.newLine();
    _builder.append("modifyEcore creation epackage foo {");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("addNewEClass(\"NewClass\")");
    _builder.newLine();
    _builder.append("}");
    _builder.newLine();
    _builder.append("modifyEcore removed epackage foo {");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("EClassifiers -= ecoreref(NewClass)");
    _builder.newLine();
    _builder.append("}");
    _builder.newLine();
    _builder.append("modifyEcore accessing epackage foo {");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("ecoreref(NewClass) // this doesn\'t exist anymore");
    _builder.newLine();
    _builder.append("}");
    _builder.newLine();
    final String input = _builder.toString();
    EdeltaProgram _parseWithTestEcore = this.parseWithTestEcore(input);
    final Procedure1<EdeltaProgram> _function = (EdeltaProgram it) -> {
      this.interpretProgram(it);
      this._validationTestHelper.assertError(it, 
        EdeltaPackage.eINSTANCE.getEdeltaEcoreReferenceExpression(), 
        EdeltaValidator.INTERPRETER_ACCESS_REMOVED_ELEMENT, 
        input.lastIndexOf("NewClass"), 
        "NewClass".length(), 
        "The element is not available anymore in this context: \'NewClass\'");
      this.assertErrorsAsStrings(it, "The element is not available anymore in this context: \'NewClass\'");
    };
    ObjectExtensions.<EdeltaProgram>operator_doubleArrow(_parseWithTestEcore, _function);
  }
  
  @Test
  public void testReferenceToEClassRenamed() {
    final String input = this._inputs.referenceToEClassRenamed().toString();
    EdeltaProgram _parseWithTestEcore = this.parseWithTestEcore(input);
    final Procedure1<EdeltaProgram> _function = (EdeltaProgram it) -> {
      this.interpretProgram(it);
      this._validationTestHelper.assertError(it, 
        EdeltaPackage.eINSTANCE.getEdeltaEcoreReferenceExpression(), 
        EdeltaValidator.INTERPRETER_ACCESS_RENAMED_ELEMENT, 
        input.lastIndexOf("FooClass"), 
        "FooClass".length(), 
        "The element \'FooClass\' is now available as \'foo.Renamed\'");
      this.assertErrorsAsStrings(it, "The element \'FooClass\' is now available as \'foo.Renamed\'");
    };
    ObjectExtensions.<EdeltaProgram>operator_doubleArrow(_parseWithTestEcore, _function);
  }
  
  @Test
  public void testReferenceToCreatedEClassRenamed() {
    final String input = this._inputs.referenceToCreatedEClassRenamed().toString();
    EdeltaProgram _parseWithTestEcore = this.parseWithTestEcore(input);
    final Procedure1<EdeltaProgram> _function = (EdeltaProgram it) -> {
      this.interpretProgram(it);
      this._validationTestHelper.assertError(it, 
        EdeltaPackage.eINSTANCE.getEdeltaEcoreReferenceExpression(), 
        EdeltaValidator.INTERPRETER_ACCESS_RENAMED_ELEMENT, 
        input.lastIndexOf("NewClass"), 
        "NewClass".length(), 
        "The element \'NewClass\' is now available as \'foo.changed\'");
      this.assertErrorsAsStrings(it, "The element \'NewClass\' is now available as \'foo.changed\'");
    };
    ObjectExtensions.<EdeltaProgram>operator_doubleArrow(_parseWithTestEcore, _function);
  }
  
  private void assertAfterInterpretationOfEdeltaModifyEcoreOperation(final CharSequence input, final Procedure1<? super EPackage> testExecutor) {
    this.assertAfterInterpretationOfEdeltaModifyEcoreOperation(input, true, testExecutor);
  }
  
  private void assertAfterInterpretationOfEdeltaModifyEcoreOperation(final CharSequence input, final boolean doValidate, final Procedure1<? super EPackage> testExecutor) {
    final EdeltaProgram program = this.parseWithTestEcore(input);
    this.assertAfterInterpretationOfEdeltaModifyEcoreOperation(program, doValidate, testExecutor);
  }
  
  private void assertAfterInterpretationOfEdeltaModifyEcoreOperation(final List<CharSequence> inputs, final boolean doValidate, final Procedure1<? super EPackage> testExecutor) {
    final EdeltaProgram program = this.parseSeveralWithTestEcore(inputs);
    this.assertAfterInterpretationOfEdeltaModifyEcoreOperation(program, doValidate, testExecutor);
  }
  
  private void assertAfterInterpretationOfEdeltaModifyEcoreOperation(final EdeltaProgram program, final boolean doValidate, final Procedure1<? super EPackage> testExecutor) {
    this.assertAfterInterpretationOfEdeltaModifyEcoreOperation(this.interpreter, program, testExecutor);
    if (doValidate) {
      this._validationTestHelper.assertNoErrors(program);
    }
  }
  
  private void assertAfterInterpretationOfEdeltaModifyEcoreOperation(final EdeltaInterpreter interpreter, final EdeltaProgram program, final Procedure1<? super EPackage> testExecutor) {
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
  
  private EdeltaCopiedEPackagesMap interpretProgram(final EdeltaProgram program) {
    final Function1<EPackage, String> _function = (EPackage it) -> {
      return it.getName();
    };
    Map<String, EPackage> _map = IterableExtensions.<String, EPackage>toMap(this.getCopiedEPackages(program), _function);
    final EdeltaCopiedEPackagesMap copiedEPackagesMap = new EdeltaCopiedEPackagesMap(_map);
    this.interpreter.evaluateModifyEcoreOperations(program, copiedEPackagesMap);
    return copiedEPackagesMap;
  }
  
  private void assertEcoreRefExpElementMapsToXExpression(final EdeltaEcoreReferenceExpression it, final ENamedElement element, final String expectedFeatureCallSimpleName) {
    final XExpression exp = this.derivedStateHelper.getEcoreReferenceExpressionState(it).getEnamedElementXExpressionMap().get(element);
    Assert.assertNotNull(exp);
    Assert.assertEquals(expectedFeatureCallSimpleName, this.getFeatureCall(exp).getFeature().getSimpleName());
  }
}
