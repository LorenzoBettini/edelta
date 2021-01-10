package edelta.tests;

import com.google.common.collect.Iterables;
import com.google.inject.Inject;
import com.google.inject.Injector;
import edelta.edelta.EdeltaEcoreReference;
import edelta.edelta.EdeltaEcoreReferenceExpression;
import edelta.edelta.EdeltaModifyEcoreOperation;
import edelta.edelta.EdeltaPackage;
import edelta.edelta.EdeltaProgram;
import edelta.interpreter.EdeltaInterpreter;
import edelta.interpreter.EdeltaInterpreterFactory;
import edelta.interpreter.EdeltaInterpreterRuntimeException;
import edelta.interpreter.EdeltaInterpreterWrapperException;
import edelta.resource.derivedstate.EdeltaAccessibleElement;
import edelta.resource.derivedstate.EdeltaAccessibleElements;
import edelta.resource.derivedstate.EdeltaCopiedEPackagesMap;
import edelta.resource.derivedstate.EdeltaDerivedStateHelper;
import edelta.resource.derivedstate.EdeltaENamedElementXExpressionMap;
import edelta.resource.derivedstate.EdeltaUnresolvedEcoreReferences;
import edelta.tests.EdeltaAbstractTest;
import edelta.tests.additional.EdeltaEContentAdapter;
import edelta.tests.additional.MyCustomEdeltaThatCannotBeLoadedAtRuntime;
import edelta.tests.additional.MyCustomException;
import edelta.tests.injectors.EdeltaInjectorProviderDerivedStateComputerWithoutInterpreter;
import edelta.validation.EdeltaValidator;
import java.util.Collections;
import java.util.List;
import org.assertj.core.api.AbstractThrowableAssert;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.ThrowableAssert;
import org.assertj.core.api.iterable.ThrowingExtractor;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;
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
import org.eclipse.xtext.xbase.lib.ListExtensions;
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
  public void sanityTestCheck() throws Exception {
    final EdeltaInterpreterFactory interpreterFactory = this.injector.<EdeltaInterpreterFactory>getInstance(EdeltaInterpreterFactory.class);
    final EdeltaInterpreter anotherInterprter = interpreterFactory.create(this.parseHelper.parse("").eResource());
    Assertions.assertThat(anotherInterprter.getClass()).isSameAs(this.interpreter.getClass());
  }
  
  @Test
  public void makeSureModificationsToOriginalEPackageAreDetected() throws Exception {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("metamodel \"foo\"");
    _builder.newLine();
    _builder.newLine();
    _builder.append("modifyEcore aTest epackage foo {");
    _builder.newLine();
    _builder.append("}");
    _builder.newLine();
    final EdeltaProgram prog = this.parseWithTestEcore(_builder);
    final ThrowableAssert.ThrowingCallable _function = () -> {
      EPackage _head = IterableExtensions.<EPackage>head(prog.getMetamodels());
      _head.setName("changed");
    };
    Assertions.assertThatThrownBy(_function).isInstanceOf(EdeltaEContentAdapter.EdeltaEContentAdapterException.class);
  }
  
  @Test
  public void testCreateEClassAndCallLibMethod() throws Exception {
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
  public void testCreateEClassAndCallOperationThatThrows() throws Exception {
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
  public void testThrowNullPointerException() throws Exception {
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
      final Procedure1<EPackage> _function_1 = (EPackage it) -> {
      };
      this.assertAfterInterpretationOfEdeltaModifyEcoreOperation(_builder, _function_1);
    };
    Assertions.assertThatThrownBy(_function).isInstanceOf(EdeltaInterpreterWrapperException.class).hasCauseExactlyInstanceOf(NullPointerException.class);
  }
  
  @Test
  public void testCreateEClassAndCallOperationFromUseAsReferringToUnknownType() throws Exception {
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
    _builder.append("my.createANewEAttribute(c) // this won\'t break the interpreter");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("addNewEClass(\"AnotherNewClass\")");
    _builder.newLine();
    _builder.append("}");
    _builder.newLine();
    final Procedure1<EPackage> _function = (EPackage derivedEPackage) -> {
      EClass _lastEClass = this.getLastEClass(derivedEPackage);
      final Procedure1<EClass> _function_1 = (EClass it) -> {
        Assert.assertEquals("AnotherNewClass", it.getName());
      };
      ObjectExtensions.<EClass>operator_doubleArrow(_lastEClass, _function_1);
    };
    this.assertAfterInterpretationOfEdeltaModifyEcoreOperation(_builder, false, _function);
  }
  
  @Test
  public void testCreateEClassAndCallOperationFromUseAsButNotFoundAtRuntime() throws Exception {
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
  public void testCreateEClassAndCreateEAttribute() throws Exception {
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
  public void testRenameEClassAndCreateEAttributeAndCallOperationFromUseAs() throws Exception {
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
  public void testEClassCreatedFromUseAs() throws Exception {
    final Procedure1<EPackage> _function = (EPackage ePackage) -> {
      final EClass eClass = this.getLastEClass(ePackage);
      Assert.assertEquals("ANewClass", eClass.getName());
      EStructuralFeature _head = IterableExtensions.<EStructuralFeature>head(eClass.getEStructuralFeatures());
      Assert.assertEquals("aNewAttr", 
        ((EAttribute) _head).getName());
    };
    this.assertAfterInterpretationOfEdeltaModifyEcoreOperation(this.inputs.useAsCustomEdeltaCreatingEClass(), _function);
  }
  
  @Test
  public void testEClassCreatedFromUseAsAsExtension() throws Exception {
    final Procedure1<EPackage> _function = (EPackage ePackage) -> {
      final EClass eClass = this.getLastEClass(ePackage);
      Assert.assertEquals("ANewClass", eClass.getName());
      EStructuralFeature _head = IterableExtensions.<EStructuralFeature>head(eClass.getEStructuralFeatures());
      Assert.assertEquals("aNewAttr", 
        ((EAttribute) _head).getName());
    };
    this.assertAfterInterpretationOfEdeltaModifyEcoreOperation(this.inputs.useAsCustomEdeltaAsExtensionCreatingEClass(), _function);
  }
  
  @Test
  public void testEClassCreatedFromStatefulUseAs() throws Exception {
    final Procedure1<EPackage> _function = (EPackage ePackage) -> {
      final EClass eClass = this.getLastEClass(ePackage);
      Assert.assertEquals("ANewClass3", eClass.getName());
      EStructuralFeature _head = IterableExtensions.<EStructuralFeature>head(eClass.getEStructuralFeatures());
      Assert.assertEquals("aNewAttr4", 
        ((EAttribute) _head).getName());
    };
    this.assertAfterInterpretationOfEdeltaModifyEcoreOperation(this.inputs.useAsCustomStatefulEdeltaCreatingEClass(), _function);
  }
  
  @Test
  public void testNullBody() throws Exception {
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
  public void testNullEcoreRefNamedElement() throws Exception {
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
  public void testNullEcoreRef() throws Exception {
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
  
  @Test
  public void testOperationWithErrorsDueToWrongParsing() throws Exception {
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
    _builder.append("addNewEClass(\"NewClass1\")");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("eclass NewClass1 // this won\'t break the interpreter");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("ecoreref(NewClass1).abstract = true");
    _builder.newLine();
    _builder.append("}");
    _builder.newLine();
    final String input = _builder.toString();
    final Procedure1<EPackage> _function = (EPackage derivedEPackage) -> {
      EClass _lastEClass = this.getLastEClass(derivedEPackage);
      final Procedure1<EClass> _function_1 = (EClass it) -> {
        Assert.assertEquals("NewClass1", it.getName());
        Assertions.assertThat(it.isAbstract()).isTrue();
      };
      ObjectExtensions.<EClass>operator_doubleArrow(_lastEClass, _function_1);
    };
    this.assertAfterInterpretationOfEdeltaModifyEcoreOperation(input, false, _function);
  }
  
  @Test
  public void testUnresolvedEcoreReference() throws Exception {
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
    _builder.append("\t");
    _builder.append("ecoreref(NewClass1).abstract = true");
    _builder.newLine();
    _builder.append("}");
    _builder.newLine();
    final String input = _builder.toString();
    final Procedure1<EPackage> _function = (EPackage derivedEPackage) -> {
      EClass _lastEClass = this.getLastEClass(derivedEPackage);
      final Procedure1<EClass> _function_1 = (EClass it) -> {
        Assert.assertEquals("NewClass1", it.getName());
        Assertions.assertThat(it.isAbstract()).isTrue();
      };
      ObjectExtensions.<EClass>operator_doubleArrow(_lastEClass, _function_1);
    };
    this.assertAfterInterpretationOfEdeltaModifyEcoreOperation(input, false, _function);
  }
  
  @Test
  public void testUnresolvedEcoreReferenceMethodCall() throws Exception {
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
    _builder.append("ecoreref(nonexist).abstract = true // this won\'t break the interpreter");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("addNewEClass(\"NewClass1\")");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("ecoreref(NewClass1).abstract = true");
    _builder.newLine();
    _builder.append("}");
    _builder.newLine();
    final String input = _builder.toString();
    final Procedure1<EPackage> _function = (EPackage derivedEPackage) -> {
      EClass _lastEClass = this.getLastEClass(derivedEPackage);
      final Procedure1<EClass> _function_1 = (EClass it) -> {
        Assert.assertEquals("NewClass1", it.getName());
        Assertions.assertThat(it.isAbstract()).isTrue();
      };
      ObjectExtensions.<EClass>operator_doubleArrow(_lastEClass, _function_1);
    };
    this.assertAfterInterpretationOfEdeltaModifyEcoreOperation(input, false, _function);
  }
  
  @Test
  public void testUnresolvedEcoreReferenceMethodCall2() throws Exception {
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
    _builder.append("ecoreref(nonexist).ESuperTypes += ecoreref(MyClass) // this won\'t break the interpreter");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("addNewEClass(\"NewClass1\")");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("ecoreref(NewClass1).abstract = true");
    _builder.newLine();
    _builder.append("}");
    _builder.newLine();
    final String input = _builder.toString();
    final Procedure1<EPackage> _function = (EPackage derivedEPackage) -> {
      EClass _lastEClass = this.getLastEClass(derivedEPackage);
      final Procedure1<EClass> _function_1 = (EClass it) -> {
        Assert.assertEquals("NewClass1", it.getName());
        Assertions.assertThat(it.isAbstract()).isTrue();
      };
      ObjectExtensions.<EClass>operator_doubleArrow(_lastEClass, _function_1);
    };
    this.assertAfterInterpretationOfEdeltaModifyEcoreOperation(input, false, _function);
  }
  
  @Test
  public void testUnresolvedEcoreReferenceMethodCall3() throws Exception {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("import org.eclipse.emf.ecore.EClass");
    _builder.newLine();
    _builder.newLine();
    _builder.append("metamodel \"foo\"");
    _builder.newLine();
    _builder.newLine();
    _builder.append("modifyEcore creation epackage foo {");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("addNewEClass(\"NewClass\")");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("// note that ESuperTypes is resolved, but not the argument");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("ecoreref(NewClass).ESuperTypes += ecoreref(AnotherNewClass) // this won\'t break the interpreter");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("addNewEClass(\"AnotherNewClass\")");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("// the next one is not resolved, BAD");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("ecoreref(AnotherNewClass).abstract = true");
    _builder.newLine();
    _builder.append("}");
    _builder.newLine();
    final String input = _builder.toString();
    final Procedure1<EPackage> _function = (EPackage derivedEPackage) -> {
      EClass _lastEClass = this.getLastEClass(derivedEPackage);
      final Procedure1<EClass> _function_1 = (EClass it) -> {
        Assert.assertEquals("AnotherNewClass", it.getName());
        Assertions.assertThat(it.isAbstract()).isTrue();
      };
      ObjectExtensions.<EClass>operator_doubleArrow(_lastEClass, _function_1);
    };
    this.assertAfterInterpretationOfEdeltaModifyEcoreOperation(input, false, _function);
  }
  
  @Test
  public void testUnresolvedEcoreReferenceQualified() throws Exception {
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
  public void testModifyOperationCreateEClass() throws Exception {
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
  public void testModifyEcoreAndCallOperation() throws Exception {
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
  public void testModifyEcoreRenameClassAndAddAttribute() throws Exception {
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
  public void testModifyEcoreRenameClassAndAddAttribute2() throws Exception {
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
  public void testTimeoutWarning() throws Exception {
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
        this.validationTestHelper.assertWarning(it, 
          XbasePackage.eINSTANCE.getXMemberFeatureCall(), 
          EdeltaValidator.INTERPRETER_TIMEOUT, initialIndex, offendingString.length(), 
          "Timeout while interpreting");
      };
      ObjectExtensions.<EClass>operator_doubleArrow(_lastEClass, _function_1);
    };
    this.assertAfterInterpretationOfEdeltaModifyEcoreOperation(input, _function);
  }
  
  @Test
  public void testTimeoutWarningWhenCallingJavaCode() throws Exception {
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
        this.validationTestHelper.assertWarning(it, 
          XbasePackage.eINSTANCE.getXFeatureCall(), 
          EdeltaValidator.INTERPRETER_TIMEOUT, initialIndex, offendingString.length(), 
          "Timeout while interpreting");
      };
      ObjectExtensions.<EClass>operator_doubleArrow(_lastEClass, _function_1);
    };
    this.assertAfterInterpretationOfEdeltaModifyEcoreOperation(input, _function);
  }
  
  @Test
  public void testTimeoutWarningWithSeveralFiles() throws Exception {
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
        this.validationTestHelper.assertWarning(it, 
          XbasePackage.eINSTANCE.getXFeatureCall(), 
          EdeltaValidator.INTERPRETER_TIMEOUT, initialIndex, offendingString.length(), 
          "Timeout while interpreting");
      };
      ObjectExtensions.<EClass>operator_doubleArrow(_lastEClass, _function_1);
    };
    this.assertAfterInterpretationOfEdeltaModifyEcoreOperation(Collections.<CharSequence>unmodifiableList(CollectionLiterals.<CharSequence>newArrayList(lib1, lib2, input)), true, _function);
  }
  
  @Test
  public void testCreateEClassInSubPackage() throws Exception {
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
  public void testCreateEClassInSubSubPackage() throws Exception {
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
  public void testCreateEClassInNewSubPackage() throws Exception {
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
  public void testComplexInterpretationWithRenamingAndSubPackages() throws Exception {
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
  public void testInterpreterOnSubPackageIsNotExecuted() throws Exception {
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
  public void testModifyEcoreAndCallOperationFromExternalUseAs() throws Exception {
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
  public void testModifyEcoreAndCallOperationFromExternalUseAsExtension() throws Exception {
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
  public void testModifyEcoreAndCallOperationFromExternalUseAsWithSeveralFiles() throws Exception {
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
  public void testModificationsOfMetamodelsAcrossSeveralFilesIntroducingDepOnAnotherMetamodel() throws Exception {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("import org.eclipse.emf.ecore.EClass");
    _builder.newLine();
    _builder.newLine();
    _builder.append("package test1");
    _builder.newLine();
    _builder.newLine();
    _builder.append("metamodel \"bar\"");
    _builder.newLine();
    _builder.newLine();
    _builder.append("def setBaseClass(EClass c) : void {");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("c.getESuperTypes += ecoreref(BarClass)");
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
    _builder_1.append("// the other file\'s operation will set the");
    _builder_1.newLine();
    _builder_1.append("\t");
    _builder_1.append("// base class of foo.FooClass to bar.BarClass");
    _builder_1.newLine();
    _builder_1.append("\t");
    _builder_1.append("ecoreref(FooClass).setBaseClass");
    _builder_1.newLine();
    _builder_1.append("\t");
    _builder_1.append("// now the foo package refers to bar package");
    _builder_1.newLine();
    _builder_1.append("\t");
    _builder_1.newLine();
    _builder_1.append("\t");
    _builder_1.append("// now modify the bar\'s class");
    _builder_1.newLine();
    _builder_1.append("\t");
    _builder_1.append("ecoreref(FooClass).ESuperTypes.head.abstract = true");
    _builder_1.newLine();
    _builder_1.append("}");
    _builder_1.newLine();
    final EdeltaProgram program = this.parseSeveralWithTestEcores(
      Collections.<CharSequence>unmodifiableList(CollectionLiterals.<CharSequence>newArrayList(_builder, _builder_1)));
    final Procedure1<EPackage> _function = (EPackage derivedEPackage) -> {
      EClass _firstEClass = this.getFirstEClass(derivedEPackage);
      final Procedure1<EClass> _function_1 = (EClass it) -> {
        final Function1<EClass, String> _function_2 = (EClass it_1) -> {
          return it_1.getName();
        };
        Assertions.<String>assertThat(ListExtensions.<EClass, String>map(it.getESuperTypes(), _function_2)).containsExactly("BarClass");
        Assertions.assertThat(IterableExtensions.<EClass>head(it.getESuperTypes()).isAbstract()).isTrue();
      };
      ObjectExtensions.<EClass>operator_doubleArrow(_firstEClass, _function_1);
    };
    this.assertAfterInterpretationOfEdeltaModifyEcoreOperation(program, true, _function);
    final ThrowingExtractor<EPackage, String, Exception> _function_1 = (EPackage it) -> {
      return it.getName();
    };
    Assertions.<EPackage>assertThat(this.getCopiedEPackages(program)).<String, Exception>extracting(_function_1).containsExactlyInAnyOrder("foo", "bar");
  }
  
  @Test
  public void testRenameReferencesAcrossEPackages() throws Exception {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("package test");
    _builder.newLine();
    _builder.newLine();
    _builder.append("metamodel \"testecoreforreferences1\"");
    _builder.newLine();
    _builder.append("metamodel \"testecoreforreferences2\"");
    _builder.newLine();
    _builder.newLine();
    _builder.append("modifyEcore aTest1 epackage testecoreforreferences1 {");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("// renames WorkPlace.persons to renamedPersons");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("ecoreref(Person.works).EOpposite.name = \"renamedPersons\"");
    _builder.newLine();
    _builder.append("}");
    _builder.newLine();
    _builder.append("modifyEcore aTest2 epackage testecoreforreferences2 {");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("// renames Person.works to renamedWorks");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("// using the already renamed feature (was persons)");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("ecoreref(renamedPersons).EOpposite.name = \"renamedWorks\"");
    _builder.newLine();
    _builder.append("}");
    _builder.newLine();
    EdeltaProgram _parseWithTestEcoresWithReferences = this.parseWithTestEcoresWithReferences(_builder);
    final Procedure1<EdeltaProgram> _function = (EdeltaProgram it) -> {
      try {
        final EdeltaCopiedEPackagesMap map = this.interpretProgram(it);
        final EPackage testecoreforreferences1 = map.get("testecoreforreferences1");
        final EPackage testecoreforreferences2 = map.get("testecoreforreferences2");
        final EClass person = this.getEClassByName(testecoreforreferences1, "Person");
        final Function1<EReference, String> _function_1 = (EReference it_1) -> {
          return it_1.getName();
        };
        Assertions.<String>assertThat(IterableExtensions.<EReference, String>map(Iterables.<EReference>filter(person.getEStructuralFeatures(), EReference.class), _function_1)).containsOnly("renamedWorks");
        final EClass workplace = this.getEClassByName(testecoreforreferences2, "WorkPlace");
        final Function1<EReference, String> _function_2 = (EReference it_1) -> {
          return it_1.getName();
        };
        Assertions.<String>assertThat(IterableExtensions.<EReference, String>map(Iterables.<EReference>filter(workplace.getEStructuralFeatures(), EReference.class), _function_2)).containsOnly("renamedPersons");
        final EdeltaUnresolvedEcoreReferences unresolvedEcoreRefs = this.derivedStateHelper.getUnresolvedEcoreReferences(it.eResource());
        Assertions.<EdeltaEcoreReference>assertThat(unresolvedEcoreRefs).isEmpty();
      } catch (Throwable _e) {
        throw Exceptions.sneakyThrow(_e);
      }
    };
    ObjectExtensions.<EdeltaProgram>operator_doubleArrow(_parseWithTestEcoresWithReferences, _function);
  }
  
  @Test
  public void testRenameReferencesAcrossEPackagesModifyingOnePackageOnly() throws Exception {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("package test");
    _builder.newLine();
    _builder.newLine();
    _builder.append("metamodel \"testecoreforreferences1\"");
    _builder.newLine();
    _builder.append("metamodel \"testecoreforreferences2\"");
    _builder.newLine();
    _builder.newLine();
    _builder.append("modifyEcore aTest1 epackage testecoreforreferences1 {");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("// renames WorkPlace.persons to renamedPersons");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("ecoreref(Person.works).EOpposite.name = \"renamedPersons\"");
    _builder.newLine();
    _builder.append("}");
    _builder.newLine();
    EdeltaProgram _parseWithTestEcoresWithReferences = this.parseWithTestEcoresWithReferences(_builder);
    final Procedure1<EdeltaProgram> _function = (EdeltaProgram it) -> {
      try {
        final EdeltaCopiedEPackagesMap map = this.interpretProgram(it);
        final EPackage testecoreforreferences1 = map.get("testecoreforreferences1");
        final EPackage testecoreforreferences2 = map.get("testecoreforreferences2");
        final EClass person = this.getEClassByName(testecoreforreferences1, "Person");
        final Function1<EReference, String> _function_1 = (EReference it_1) -> {
          return it_1.getEOpposite().getName();
        };
        Assertions.<String>assertThat(IterableExtensions.<EReference, String>map(Iterables.<EReference>filter(person.getEStructuralFeatures(), EReference.class), _function_1)).containsOnly("renamedPersons");
        final EClass workplace = this.getEClassByName(testecoreforreferences2, "WorkPlace");
        final Function1<EReference, String> _function_2 = (EReference it_1) -> {
          return it_1.getName();
        };
        Assertions.<String>assertThat(IterableExtensions.<EReference, String>map(Iterables.<EReference>filter(workplace.getEStructuralFeatures(), EReference.class), _function_2)).containsOnly("renamedPersons");
        Assertions.<EReference>assertThat(IterableExtensions.<EReference>head(Iterables.<EReference>filter(person.getEStructuralFeatures(), EReference.class)).getEOpposite()).isSameAs(IterableExtensions.<EReference>head(Iterables.<EReference>filter(workplace.getEStructuralFeatures(), EReference.class)));
        final EdeltaUnresolvedEcoreReferences unresolvedEcoreRefs = this.derivedStateHelper.getUnresolvedEcoreReferences(it.eResource());
        Assertions.<EdeltaEcoreReference>assertThat(unresolvedEcoreRefs).isEmpty();
      } catch (Throwable _e) {
        throw Exceptions.sneakyThrow(_e);
      }
    };
    ObjectExtensions.<EdeltaProgram>operator_doubleArrow(_parseWithTestEcoresWithReferences, _function);
  }
  
  @Test
  public void testElementExpressionForCreatedEClassWithEdeltaAPI() throws Exception {
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
      try {
        final EdeltaCopiedEPackagesMap map = this.interpretProgram(it);
        final EClassifier createClass = map.get("foo").getEClassifier("NewClass");
        final XExpression exp = this.derivedStateHelper.getEnamedElementXExpressionMap(it.eResource()).get(createClass);
        Assert.assertNotNull(exp);
        Assert.assertEquals("addNewEClass", this.getFeatureCall(exp).getFeature().getSimpleName());
      } catch (Throwable _e) {
        throw Exceptions.sneakyThrow(_e);
      }
    };
    ObjectExtensions.<EdeltaProgram>operator_doubleArrow(_parseWithTestEcore, _function);
  }
  
  @Test
  public void testEcoreRefExpExpressionForCreatedEClassWithEdeltaAPI() throws Exception {
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
      try {
        this.interpretProgram(it);
        EdeltaEcoreReferenceExpression _last = IterableExtensions.<EdeltaEcoreReferenceExpression>last(this.getAllEcoreReferenceExpressions(it));
        final Procedure1<EdeltaEcoreReferenceExpression> _function_1 = (EdeltaEcoreReferenceExpression it_1) -> {
          try {
            this.assertEcoreRefExpElementMapsToXExpression(it_1.getReference(), "addNewEClass");
          } catch (Throwable _e) {
            throw Exceptions.sneakyThrow(_e);
          }
        };
        ObjectExtensions.<EdeltaEcoreReferenceExpression>operator_doubleArrow(_last, _function_1);
      } catch (Throwable _e) {
        throw Exceptions.sneakyThrow(_e);
      }
    };
    ObjectExtensions.<EdeltaProgram>operator_doubleArrow(_parseWithTestEcore, _function);
  }
  
  @Test
  public void testEcoreRefExpExpressionForCreatedEClassWithOperation() throws Exception {
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
      try {
        this.interpretProgram(it);
        EdeltaEcoreReferenceExpression _last = IterableExtensions.<EdeltaEcoreReferenceExpression>last(this.getAllEcoreReferenceExpressions(it));
        final Procedure1<EdeltaEcoreReferenceExpression> _function_1 = (EdeltaEcoreReferenceExpression it_1) -> {
          try {
            this.assertEcoreRefExpElementMapsToXExpression(it_1.getReference(), "addNewEClass");
          } catch (Throwable _e) {
            throw Exceptions.sneakyThrow(_e);
          }
        };
        ObjectExtensions.<EdeltaEcoreReferenceExpression>operator_doubleArrow(_last, _function_1);
      } catch (Throwable _e) {
        throw Exceptions.sneakyThrow(_e);
      }
    };
    ObjectExtensions.<EdeltaProgram>operator_doubleArrow(_parseWithTestEcore, _function);
  }
  
  @Test
  public void testEcoreRefExpExpressionForCreatedEClassWithOperationInAnotherFile() throws Exception {
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
      try {
        this.interpretProgram(it);
        EdeltaEcoreReferenceExpression _last = IterableExtensions.<EdeltaEcoreReferenceExpression>last(this.getAllEcoreReferenceExpressions(it));
        final Procedure1<EdeltaEcoreReferenceExpression> _function_1 = (EdeltaEcoreReferenceExpression it_1) -> {
          try {
            this.assertEcoreRefExpElementMapsToXExpression(it_1.getReference(), "create");
          } catch (Throwable _e) {
            throw Exceptions.sneakyThrow(_e);
          }
        };
        ObjectExtensions.<EdeltaEcoreReferenceExpression>operator_doubleArrow(_last, _function_1);
      } catch (Throwable _e) {
        throw Exceptions.sneakyThrow(_e);
      }
    };
    ObjectExtensions.<EdeltaProgram>operator_doubleArrow(_parseSeveralWithTestEcore, _function);
  }
  
  @Test
  public void testEcoreRefExpForCreatedEClassRenamed() throws Exception {
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
      try {
        this.interpretProgram(it);
        final List<EdeltaEcoreReferenceExpression> ecoreRefs = this.getAllEcoreReferenceExpressions(it);
        EdeltaEcoreReferenceExpression _head = IterableExtensions.<EdeltaEcoreReferenceExpression>head(ecoreRefs);
        final Procedure1<EdeltaEcoreReferenceExpression> _function_1 = (EdeltaEcoreReferenceExpression it_1) -> {
          try {
            this.assertEcoreRefExpElementMapsToXExpression(it_1.getReference(), "addNewEClass");
          } catch (Throwable _e) {
            throw Exceptions.sneakyThrow(_e);
          }
        };
        ObjectExtensions.<EdeltaEcoreReferenceExpression>operator_doubleArrow(_head, _function_1);
        EdeltaEcoreReferenceExpression _last = IterableExtensions.<EdeltaEcoreReferenceExpression>last(ecoreRefs);
        final Procedure1<EdeltaEcoreReferenceExpression> _function_2 = (EdeltaEcoreReferenceExpression it_1) -> {
          try {
            this.assertEcoreRefExpElementMapsToXExpression(it_1.getReference(), "setName");
          } catch (Throwable _e) {
            throw Exceptions.sneakyThrow(_e);
          }
        };
        ObjectExtensions.<EdeltaEcoreReferenceExpression>operator_doubleArrow(_last, _function_2);
      } catch (Throwable _e) {
        throw Exceptions.sneakyThrow(_e);
      }
    };
    ObjectExtensions.<EdeltaProgram>operator_doubleArrow(_parseWithTestEcore, _function);
  }
  
  @Test
  public void testEcoreRefExpForCreatedSubPackage() throws Exception {
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
      try {
        this.interpretProgram(it);
        List<EdeltaEcoreReferenceExpression> _allEcoreReferenceExpressions = this.getAllEcoreReferenceExpressions(it);
        final Procedure1<List<EdeltaEcoreReferenceExpression>> _function_1 = (List<EdeltaEcoreReferenceExpression> it_1) -> {
          EdeltaEcoreReferenceExpression _get = it_1.get(0);
          final Procedure1<EdeltaEcoreReferenceExpression> _function_2 = (EdeltaEcoreReferenceExpression it_2) -> {
            try {
              this.assertEcoreRefExpElementMapsToXExpression(it_2.getReference(), "addNewEClass");
            } catch (Throwable _e) {
              throw Exceptions.sneakyThrow(_e);
            }
          };
          ObjectExtensions.<EdeltaEcoreReferenceExpression>operator_doubleArrow(_get, _function_2);
          EdeltaEcoreReferenceExpression _get_1 = it_1.get(1);
          final Procedure1<EdeltaEcoreReferenceExpression> _function_3 = (EdeltaEcoreReferenceExpression it_2) -> {
            try {
              this.assertEcoreRefExpElementMapsToXExpression(it_2.getReference(), "addNewEClass");
            } catch (Throwable _e) {
              throw Exceptions.sneakyThrow(_e);
            }
          };
          ObjectExtensions.<EdeltaEcoreReferenceExpression>operator_doubleArrow(_get_1, _function_3);
          EdeltaEcoreReferenceExpression _get_2 = it_1.get(2);
          final Procedure1<EdeltaEcoreReferenceExpression> _function_4 = (EdeltaEcoreReferenceExpression it_2) -> {
            try {
              this.assertEcoreRefExpElementMapsToXExpression(it_2.getReference(), "addEClass");
            } catch (Throwable _e) {
              throw Exceptions.sneakyThrow(_e);
            }
          };
          ObjectExtensions.<EdeltaEcoreReferenceExpression>operator_doubleArrow(_get_2, _function_4);
          EdeltaEcoreReferenceExpression _get_3 = it_1.get(3);
          final Procedure1<EdeltaEcoreReferenceExpression> _function_5 = (EdeltaEcoreReferenceExpression it_2) -> {
            try {
              this.assertEcoreRefExpElementMapsToXExpression(it_2.getReference(), "addNewESubpackage");
            } catch (Throwable _e) {
              throw Exceptions.sneakyThrow(_e);
            }
          };
          ObjectExtensions.<EdeltaEcoreReferenceExpression>operator_doubleArrow(_get_3, _function_5);
        };
        ObjectExtensions.<List<EdeltaEcoreReferenceExpression>>operator_doubleArrow(_allEcoreReferenceExpressions, _function_1);
      } catch (Throwable _e) {
        throw Exceptions.sneakyThrow(_e);
      }
    };
    ObjectExtensions.<EdeltaProgram>operator_doubleArrow(_parseWithTestEcore, _function);
  }
  
  @Test
  public void testEcoreRefExpForExistingEClass() throws Exception {
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
      try {
        this.interpretProgram(it);
        final List<EdeltaEcoreReferenceExpression> ecoreRefs = this.getAllEcoreReferenceExpressions(it);
        EdeltaEcoreReferenceExpression _head = IterableExtensions.<EdeltaEcoreReferenceExpression>head(ecoreRefs);
        final Procedure1<EdeltaEcoreReferenceExpression> _function_1 = (EdeltaEcoreReferenceExpression it_1) -> {
          final XExpression exp = this.derivedStateHelper.getResponsibleExpression(it_1.getReference());
          Assert.assertNull(exp);
        };
        ObjectExtensions.<EdeltaEcoreReferenceExpression>operator_doubleArrow(_head, _function_1);
      } catch (Throwable _e) {
        throw Exceptions.sneakyThrow(_e);
      }
    };
    ObjectExtensions.<EdeltaProgram>operator_doubleArrow(_parseWithTestEcore, _function);
  }
  
  @Test
  public void testEcoreRefExpForCreatedEClassRenamedInInitializer() throws Exception {
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
      try {
        this.interpretProgram(it);
        final List<EdeltaEcoreReferenceExpression> ecoreRefs = this.getAllEcoreReferenceExpressions(it);
        EdeltaEcoreReferenceExpression _head = IterableExtensions.<EdeltaEcoreReferenceExpression>head(ecoreRefs);
        final Procedure1<EdeltaEcoreReferenceExpression> _function_1 = (EdeltaEcoreReferenceExpression it_1) -> {
          try {
            this.assertEcoreRefExpElementMapsToXExpression(it_1.getReference(), "addNewEClass");
          } catch (Throwable _e) {
            throw Exceptions.sneakyThrow(_e);
          }
        };
        ObjectExtensions.<EdeltaEcoreReferenceExpression>operator_doubleArrow(_head, _function_1);
        EdeltaEcoreReferenceExpression _last = IterableExtensions.<EdeltaEcoreReferenceExpression>last(ecoreRefs);
        final Procedure1<EdeltaEcoreReferenceExpression> _function_2 = (EdeltaEcoreReferenceExpression it_1) -> {
          try {
            this.assertEcoreRefExpElementMapsToXExpression(it_1.getReference(), "setName");
          } catch (Throwable _e) {
            throw Exceptions.sneakyThrow(_e);
          }
        };
        ObjectExtensions.<EdeltaEcoreReferenceExpression>operator_doubleArrow(_last, _function_2);
      } catch (Throwable _e) {
        throw Exceptions.sneakyThrow(_e);
      }
    };
    ObjectExtensions.<EdeltaProgram>operator_doubleArrow(_parseWithTestEcore, _function);
  }
  
  @Test
  public void testEcoreRefExpForCreatedEClassRenamedInInitializer2() throws Exception {
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
    _builder.append("ecoreref(NewClass).name = \"Renamed\"");
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
      try {
        this.interpretProgram(it);
        final List<EdeltaEcoreReferenceExpression> ecoreRefs = this.getAllEcoreReferenceExpressions(it);
        EdeltaEcoreReferenceExpression _head = IterableExtensions.<EdeltaEcoreReferenceExpression>head(ecoreRefs);
        final Procedure1<EdeltaEcoreReferenceExpression> _function_1 = (EdeltaEcoreReferenceExpression it_1) -> {
          try {
            this.assertEcoreRefExpElementMapsToXExpression(it_1.getReference(), "addNewEClass");
          } catch (Throwable _e) {
            throw Exceptions.sneakyThrow(_e);
          }
        };
        ObjectExtensions.<EdeltaEcoreReferenceExpression>operator_doubleArrow(_head, _function_1);
        EdeltaEcoreReferenceExpression _last = IterableExtensions.<EdeltaEcoreReferenceExpression>last(ecoreRefs);
        final Procedure1<EdeltaEcoreReferenceExpression> _function_2 = (EdeltaEcoreReferenceExpression it_1) -> {
          try {
            this.assertEcoreRefExpElementMapsToXExpression(it_1.getReference(), "setName");
          } catch (Throwable _e) {
            throw Exceptions.sneakyThrow(_e);
          }
        };
        ObjectExtensions.<EdeltaEcoreReferenceExpression>operator_doubleArrow(_last, _function_2);
      } catch (Throwable _e) {
        throw Exceptions.sneakyThrow(_e);
      }
    };
    ObjectExtensions.<EdeltaProgram>operator_doubleArrow(_parseWithTestEcore, _function);
  }
  
  @Test
  public void testElementExpressionMapForCreatedEClassWithEMFAPI() throws Exception {
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
      try {
        final EdeltaCopiedEPackagesMap map = this.interpretProgram(it);
        final EClassifier createClass = map.get("foo").getEClassifier("NewClass");
        final XExpression exp = this.derivedStateHelper.getEnamedElementXExpressionMap(it.eResource()).get(createClass);
        Assert.assertNotNull(exp);
        Assert.assertEquals("setName", this.getFeatureCall(exp).getFeature().getSimpleName());
      } catch (Throwable _e) {
        throw Exceptions.sneakyThrow(_e);
      }
    };
    ObjectExtensions.<EdeltaProgram>operator_doubleArrow(_parseWithTestEcore, _function);
  }
  
  @Test
  public void testElementExpressionMapForCreatedEClassWithDoubleArrow() throws Exception {
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
      try {
        final EdeltaCopiedEPackagesMap map = this.interpretProgram(it);
        final EClassifier createClass = map.get("foo").getEClassifier("NewClass");
        final XExpression exp = this.derivedStateHelper.getEnamedElementXExpressionMap(it.eResource()).get(createClass);
        Assert.assertNotNull(exp);
        Assert.assertEquals("setName", this.getFeatureCall(exp).getFeature().getSimpleName());
      } catch (Throwable _e) {
        throw Exceptions.sneakyThrow(_e);
      }
    };
    ObjectExtensions.<EdeltaProgram>operator_doubleArrow(_parseWithTestEcore, _function);
  }
  
  @Test
  public void testElementExpressionMapForCreatedEClassWithoutName() throws Exception {
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
      try {
        final EdeltaCopiedEPackagesMap map = this.interpretProgram(it);
        final EClass createClass = this.getLastEClass(map.get("foo"));
        final XExpression exp = this.derivedStateHelper.getEnamedElementXExpressionMap(it.eResource()).get(createClass);
        Assert.assertNotNull(exp);
        Assert.assertEquals("operator_add", this.getFeatureCall(exp).getFeature().getSimpleName());
      } catch (Throwable _e) {
        throw Exceptions.sneakyThrow(_e);
      }
    };
    ObjectExtensions.<EdeltaProgram>operator_doubleArrow(_parseWithTestEcore, _function);
  }
  
  @Test
  public void testElementExpressionMapForCreatedEClassWithMethodCall() throws Exception {
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
      try {
        final EdeltaCopiedEPackagesMap map = this.interpretProgram(it);
        final EClass createClass = this.getLastEClass(map.get("foo"));
        final XExpression exp = this.derivedStateHelper.getEnamedElementXExpressionMap(it.eResource()).get(createClass);
        Assert.assertNotNull(exp);
        Assert.assertEquals("add", this.getFeatureCall(exp).getFeature().getSimpleName());
      } catch (Throwable _e) {
        throw Exceptions.sneakyThrow(_e);
      }
    };
    ObjectExtensions.<EdeltaProgram>operator_doubleArrow(_parseWithTestEcore, _function);
  }
  
  @Test
  public void testReferenceToEClassRemoved() throws Exception {
    final String input = this.inputs.referenceToEClassRemoved().toString();
    EdeltaProgram _parseWithTestEcore = this.parseWithTestEcore(input);
    final Procedure1<EdeltaProgram> _function = (EdeltaProgram it) -> {
      final ThrowableAssert.ThrowingCallable _function_1 = () -> {
        this.interpretProgram(it);
      };
      Assertions.assertThatThrownBy(_function_1).isInstanceOf(EdeltaInterpreterWrapperException.class);
      this.validationTestHelper.assertError(it, 
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
  public void testReferenceToEClassDeleted() throws Exception {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("import static org.eclipse.emf.ecore.util.EcoreUtil.delete");
    _builder.newLine();
    _builder.newLine();
    _builder.append("metamodel \"foo\"");
    _builder.newLine();
    _builder.newLine();
    _builder.append("modifyEcore aTest epackage foo {");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("delete(ecoreref(FooClass))");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("ecoreref(FooClass).abstract // this doesn\'t exist anymore");
    _builder.newLine();
    _builder.append("}");
    _builder.newLine();
    final String input = _builder.toString();
    EdeltaProgram _parseWithTestEcore = this.parseWithTestEcore(input);
    final Procedure1<EdeltaProgram> _function = (EdeltaProgram it) -> {
      final ThrowableAssert.ThrowingCallable _function_1 = () -> {
        this.interpretProgram(it);
      };
      Assertions.assertThatThrownBy(_function_1).isInstanceOf(EdeltaInterpreterWrapperException.class);
      this.validationTestHelper.assertError(it, 
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
  public void testReferenceToEClassRemovedInLoop() throws Exception {
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
      try {
        this.interpretProgram(it);
        this.validationTestHelper.assertError(it, 
          EdeltaPackage.eINSTANCE.getEdeltaEcoreReferenceExpression(), 
          EdeltaValidator.INTERPRETER_ACCESS_REMOVED_ELEMENT, 
          input.lastIndexOf("NewClass1"), 
          "NewClass1".length(), 
          "The element is not available anymore in this context: \'NewClass1\'");
        this.validationTestHelper.assertError(it, 
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
      } catch (Throwable _e) {
        throw Exceptions.sneakyThrow(_e);
      }
    };
    ObjectExtensions.<EdeltaProgram>operator_doubleArrow(_parseWithTestEcore, _function);
  }
  
  @Test
  public void testReferenceToCreatedEClassRemoved() throws Exception {
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
      try {
        this.interpretProgram(it);
        this.validationTestHelper.assertError(it, 
          EdeltaPackage.eINSTANCE.getEdeltaEcoreReferenceExpression(), 
          EdeltaValidator.INTERPRETER_ACCESS_REMOVED_ELEMENT, 
          input.lastIndexOf("NewClass"), 
          "NewClass".length(), 
          "The element is not available anymore in this context: \'NewClass\'");
        this.assertErrorsAsStrings(it, "The element is not available anymore in this context: \'NewClass\'");
      } catch (Throwable _e) {
        throw Exceptions.sneakyThrow(_e);
      }
    };
    ObjectExtensions.<EdeltaProgram>operator_doubleArrow(_parseWithTestEcore, _function);
  }
  
  @Test
  public void testReferenceToEClassRenamed() throws Exception {
    final String input = this.inputs.referenceToEClassRenamed().toString();
    EdeltaProgram _parseWithTestEcore = this.parseWithTestEcore(input);
    final Procedure1<EdeltaProgram> _function = (EdeltaProgram it) -> {
      try {
        this.interpretProgram(it);
        this.validationTestHelper.assertError(it, 
          EdeltaPackage.eINSTANCE.getEdeltaEcoreReferenceExpression(), 
          EdeltaValidator.INTERPRETER_ACCESS_RENAMED_ELEMENT, 
          input.lastIndexOf("FooClass"), 
          "FooClass".length(), 
          "The element \'FooClass\' is now available as \'foo.Renamed\'");
        this.assertErrorsAsStrings(it, "The element \'FooClass\' is now available as \'foo.Renamed\'");
      } catch (Throwable _e) {
        throw Exceptions.sneakyThrow(_e);
      }
    };
    ObjectExtensions.<EdeltaProgram>operator_doubleArrow(_parseWithTestEcore, _function);
  }
  
  @Test
  public void testReferenceToCreatedEClassRenamed() throws Exception {
    final String input = this.inputs.referenceToCreatedEClassRenamed().toString();
    EdeltaProgram _parseWithTestEcore = this.parseWithTestEcore(input);
    final Procedure1<EdeltaProgram> _function = (EdeltaProgram it) -> {
      try {
        this.interpretProgram(it);
        this.validationTestHelper.assertError(it, 
          EdeltaPackage.eINSTANCE.getEdeltaEcoreReferenceExpression(), 
          EdeltaValidator.INTERPRETER_ACCESS_RENAMED_ELEMENT, 
          input.lastIndexOf("NewClass"), 
          "NewClass".length(), 
          "The element \'NewClass\' is now available as \'foo.changed\'");
        this.assertErrorsAsStrings(it, "The element \'NewClass\' is now available as \'foo.changed\'");
      } catch (Throwable _e) {
        throw Exceptions.sneakyThrow(_e);
      }
    };
    ObjectExtensions.<EdeltaProgram>operator_doubleArrow(_parseWithTestEcore, _function);
  }
  
  @Test
  public void testShowErrorOnExistingEClass() throws Exception {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("metamodel \"foo\"");
    _builder.newLine();
    _builder.newLine();
    _builder.append("modifyEcore aTest epackage foo {");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("val found = EClassifiers.findFirst[");
    _builder.newLine();
    _builder.append("\t\t");
    _builder.append("name == \"FooClass\"");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("]");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("if (found !== null)");
    _builder.newLine();
    _builder.append("\t\t");
    _builder.append("showError(");
    _builder.newLine();
    _builder.append("\t\t\t");
    _builder.append("found,");
    _builder.newLine();
    _builder.append("\t\t\t");
    _builder.append("\"Found class FooClass\")");
    _builder.newLine();
    _builder.append("}");
    _builder.newLine();
    final String input = _builder.toString();
    EdeltaProgram _parseWithTestEcore = this.parseWithTestEcore(input);
    final Procedure1<EdeltaProgram> _function = (EdeltaProgram it) -> {
      try {
        this.interpretProgram(it);
        this.validationTestHelper.assertError(it, 
          XbasePackage.eINSTANCE.getXIfExpression(), 
          EdeltaValidator.LIVE_VALIDATION_ERROR, 
          "Found class FooClass");
        this.assertErrorsAsStrings(it, "Found class FooClass");
      } catch (Throwable _e) {
        throw Exceptions.sneakyThrow(_e);
      }
    };
    ObjectExtensions.<EdeltaProgram>operator_doubleArrow(_parseWithTestEcore, _function);
  }
  
  @Test
  public void testShowErrorOnCreatedEClass() throws Exception {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("metamodel \"foo\"");
    _builder.newLine();
    _builder.newLine();
    _builder.append("modifyEcore aTest epackage foo {");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("addNewEClass(\"NewClass\")");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("val found = EClassifiers.findFirst[");
    _builder.newLine();
    _builder.append("\t\t");
    _builder.append("name == \"NewClass\"");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("]");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("if (found !== null)");
    _builder.newLine();
    _builder.append("\t\t");
    _builder.append("showError(");
    _builder.newLine();
    _builder.append("\t\t\t");
    _builder.append("found,");
    _builder.newLine();
    _builder.append("\t\t\t");
    _builder.append("\"Found class \" + found.name)");
    _builder.newLine();
    _builder.append("}");
    _builder.newLine();
    final String input = _builder.toString();
    EdeltaProgram _parseWithTestEcore = this.parseWithTestEcore(input);
    final Procedure1<EdeltaProgram> _function = (EdeltaProgram it) -> {
      try {
        this.interpretProgram(it);
        this.validationTestHelper.assertError(it, 
          XbasePackage.eINSTANCE.getXFeatureCall(), 
          EdeltaValidator.LIVE_VALIDATION_ERROR, 
          "Found class NewClass");
        this.assertErrorsAsStrings(it, "Found class NewClass");
      } catch (Throwable _e) {
        throw Exceptions.sneakyThrow(_e);
      }
    };
    ObjectExtensions.<EdeltaProgram>operator_doubleArrow(_parseWithTestEcore, _function);
  }
  
  @Test
  public void testShowErrorOnCreatedEClassGeneratedByOperation() throws Exception {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("import org.eclipse.emf.ecore.EPackage");
    _builder.newLine();
    _builder.newLine();
    _builder.append("metamodel \"foo\"");
    _builder.newLine();
    _builder.newLine();
    _builder.append("def myCheck(EPackage it) {");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("val found = EClassifiers.findFirst[");
    _builder.newLine();
    _builder.append("\t\t");
    _builder.append("name == \"NewClass\"");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("]");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("if (found !== null)");
    _builder.newLine();
    _builder.append("\t\t");
    _builder.append("showError(");
    _builder.newLine();
    _builder.append("\t\t\t");
    _builder.append("found,");
    _builder.newLine();
    _builder.append("\t\t\t");
    _builder.append("\"Found class \" + found.name)");
    _builder.newLine();
    _builder.append("}");
    _builder.newLine();
    _builder.newLine();
    _builder.append("modifyEcore aTest epackage foo {");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("addNewEClass(\"NewClass\")");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("myCheck()");
    _builder.newLine();
    _builder.append("}");
    _builder.newLine();
    final String input = _builder.toString();
    EdeltaProgram _parseWithTestEcore = this.parseWithTestEcore(input);
    final Procedure1<EdeltaProgram> _function = (EdeltaProgram it) -> {
      try {
        this.interpretProgram(it);
        this.validationTestHelper.assertError(it, 
          XbasePackage.eINSTANCE.getXFeatureCall(), 
          EdeltaValidator.LIVE_VALIDATION_ERROR, 
          input.lastIndexOf("addNewEClass(\"NewClass\")"), 
          "addNewEClass(\"NewClass\")".length(), 
          "Found class NewClass");
        this.assertErrorsAsStrings(it, "Found class NewClass");
      } catch (Throwable _e) {
        throw Exceptions.sneakyThrow(_e);
      }
    };
    ObjectExtensions.<EdeltaProgram>operator_doubleArrow(_parseWithTestEcore, _function);
  }
  
  @Test
  public void testShowErrorOnCreatedEClassGeneratedByJavaOperation() throws Exception {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("import edelta.tests.additional.MyCustomEdeltaShowingError");
    _builder.newLine();
    _builder.newLine();
    _builder.append("metamodel \"foo\"");
    _builder.newLine();
    _builder.newLine();
    _builder.append("use MyCustomEdeltaShowingError as extension my");
    _builder.newLine();
    _builder.newLine();
    _builder.append("modifyEcore aTest epackage foo {");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("checkClassName(addNewEClass(\"NewClass\"))");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("checkClassName(addNewEClass(\"anotherNewClass\"))");
    _builder.newLine();
    _builder.append("}");
    _builder.newLine();
    final String input = _builder.toString();
    EdeltaProgram _parseWithTestEcore = this.parseWithTestEcore(input);
    final Procedure1<EdeltaProgram> _function = (EdeltaProgram it) -> {
      try {
        this.interpretProgram(it);
        final String offendingString = "checkClassName(addNewEClass(\"anotherNewClass\"))";
        this.validationTestHelper.assertError(it, 
          XbasePackage.eINSTANCE.getXFeatureCall(), 
          EdeltaValidator.LIVE_VALIDATION_ERROR, 
          input.lastIndexOf(offendingString), 
          offendingString.length(), 
          "Name should start with a capital: anotherNewClass");
        this.assertErrorsAsStrings(it, "Name should start with a capital: anotherNewClass");
      } catch (Throwable _e) {
        throw Exceptions.sneakyThrow(_e);
      }
    };
    ObjectExtensions.<EdeltaProgram>operator_doubleArrow(_parseWithTestEcore, _function);
  }
  
  @Test
  public void testIntroducedCycles() throws Exception {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("metamodel \"foo\"");
    _builder.newLine();
    _builder.newLine();
    _builder.append("modifyEcore aTest epackage foo {");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("addNewEClass(\"C1\")");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("addNewEClass(\"C2\") [ ESuperTypes += ecoreref(C1) ]");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("addNewEClass(\"C3\") [ ESuperTypes += ecoreref(C2) ]");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("// cycle!");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("ecoreref(C1).ESuperTypes += ecoreref(C3)");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("addNewESubpackage(\"subpackage\", \"\", \"\")");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("// cycle");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("ecoreref(subpackage).ESubpackages += it");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("// the listener broke the cycle");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("ecoreref(foo.subpackage) // valid");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("ecoreref(subpackage.foo) // NOT valid");
    _builder.newLine();
    _builder.append("}");
    _builder.newLine();
    final String input = _builder.toString();
    EdeltaProgram _parseWithTestEcore = this.parseWithTestEcore(input);
    final Procedure1<EdeltaProgram> _function = (EdeltaProgram it) -> {
      try {
        this.interpretProgram(it);
        this.validationTestHelper.assertError(it, 
          XbasePackage.eINSTANCE.getXBinaryOperation(), 
          EdeltaValidator.ECLASS_CYCLE, 
          input.lastIndexOf("ecoreref(C1).ESuperTypes += ecoreref(C3)"), 
          "ecoreref(C1).ESuperTypes += ecoreref(C3)".length(), 
          "Cycle in inheritance hierarchy: foo.C3");
        this.validationTestHelper.assertError(it, 
          XbasePackage.eINSTANCE.getXBinaryOperation(), 
          EdeltaValidator.EPACKAGE_CYCLE, 
          input.lastIndexOf("ecoreref(subpackage).ESubpackages += it"), 
          "ecoreref(subpackage).ESubpackages += it".length(), 
          "Cycle in superpackage/subpackage: foo.subpackage.foo");
        StringConcatenation _builder_1 = new StringConcatenation();
        _builder_1.append("Cycle in inheritance hierarchy: foo.C3");
        _builder_1.newLine();
        _builder_1.append("Cycle in superpackage/subpackage: foo.subpackage.foo");
        _builder_1.newLine();
        _builder_1.append("foo cannot be resolved.");
        _builder_1.newLine();
        this.assertErrorsAsStrings(it, _builder_1);
      } catch (Throwable _e) {
        throw Exceptions.sneakyThrow(_e);
      }
    };
    ObjectExtensions.<EdeltaProgram>operator_doubleArrow(_parseWithTestEcore, _function);
  }
  
  @Test
  public void testAccessToNotYetExistingElement() throws Exception {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("metamodel \"foo\"");
    _builder.newLine();
    _builder.newLine();
    _builder.append("modifyEcore aTest epackage foo {");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("ecoreref(ANewClass) // doesn\'t exist yet");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("ecoreref(NonExisting) // doesn\'t exist at all");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("addNewEClass(\"ANewClass\")");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("ecoreref(ANewClass) // this is OK");
    _builder.newLine();
    _builder.append("}");
    _builder.newLine();
    final String input = _builder.toString();
    EdeltaProgram _parseWithTestEcore = this.parseWithTestEcore(input);
    final Procedure1<EdeltaProgram> _function = (EdeltaProgram it) -> {
      try {
        this.interpretProgram(it);
        final EdeltaEcoreReference ecoreref1 = this.getAllEcoreReferenceExpressions(it).get(0).getReference();
        final EdeltaEcoreReference ecoreref2 = this.getAllEcoreReferenceExpressions(it).get(1).getReference();
        final EdeltaUnresolvedEcoreReferences unresolved = this.derivedStateHelper.getUnresolvedEcoreReferences(it.eResource());
        Assertions.<EdeltaEcoreReference>assertThat(unresolved).containsOnly(ecoreref1, ecoreref2);
        Assertions.assertThat(ecoreref1.getEnamedelement().eIsProxy()).isFalse();
        Assertions.assertThat(ecoreref2.getEnamedelement().eIsProxy()).isTrue();
        final EdeltaENamedElementXExpressionMap map = this.derivedStateHelper.getEnamedElementXExpressionMap(it.eResource());
        Assertions.<XExpression>assertThat(map.get(ecoreref1.getEnamedelement())).isNotNull().isSameAs(this.getBlock(this.lastModifyEcoreOperation(it).getBody()).getExpressions().get(2));
      } catch (Throwable _e) {
        throw Exceptions.sneakyThrow(_e);
      }
    };
    ObjectExtensions.<EdeltaProgram>operator_doubleArrow(_parseWithTestEcore, _function);
  }
  
  @Test
  public void testAccessibleElements() throws Exception {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("metamodel \"foo\"");
    _builder.newLine();
    _builder.newLine();
    _builder.append("modifyEcore aTest epackage foo {");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("ecoreref(FooClass) // 0");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("addNewEClass(\"ANewClass\")");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("ecoreref(ANewClass) // 1");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("EClassifiers -= ecoreref(FooClass) // 2");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("ecoreref(ANewClass) // 3");
    _builder.newLine();
    _builder.append("}");
    _builder.newLine();
    final String input = _builder.toString();
    EdeltaProgram _parseWithTestEcore = this.parseWithTestEcore(input);
    final Procedure1<EdeltaProgram> _function = (EdeltaProgram it) -> {
      try {
        this.interpretProgram(it);
        final EdeltaEcoreReferenceExpression ecoreref1 = this.getAllEcoreReferenceExpressions(it).get(0);
        final EdeltaEcoreReferenceExpression ecoreref2 = this.getAllEcoreReferenceExpressions(it).get(1);
        final EdeltaEcoreReferenceExpression ecoreref3 = this.getAllEcoreReferenceExpressions(it).get(2);
        final EdeltaEcoreReferenceExpression ecoreref4 = this.getAllEcoreReferenceExpressions(it).get(3);
        final EdeltaAccessibleElements elements1 = this.derivedStateHelper.getAccessibleElements(ecoreref1);
        StringConcatenation _builder_1 = new StringConcatenation();
        _builder_1.append("foo");
        _builder_1.newLine();
        _builder_1.append("foo.FooClass");
        _builder_1.newLine();
        _builder_1.append("foo.FooClass.myAttribute");
        _builder_1.newLine();
        _builder_1.append("foo.FooClass.myReference");
        _builder_1.newLine();
        _builder_1.append("foo.FooDataType");
        _builder_1.newLine();
        _builder_1.append("foo.FooEnum");
        _builder_1.newLine();
        _builder_1.append("foo.FooEnum.FooEnumLiteral");
        _builder_1.newLine();
        this.assertAccessibleElements(elements1, _builder_1);
        final EdeltaAccessibleElements elements2 = this.derivedStateHelper.getAccessibleElements(ecoreref2);
        StringConcatenation _builder_2 = new StringConcatenation();
        _builder_2.append("foo");
        _builder_2.newLine();
        _builder_2.append("foo.ANewClass");
        _builder_2.newLine();
        _builder_2.append("foo.FooClass");
        _builder_2.newLine();
        _builder_2.append("foo.FooClass.myAttribute");
        _builder_2.newLine();
        _builder_2.append("foo.FooClass.myReference");
        _builder_2.newLine();
        _builder_2.append("foo.FooDataType");
        _builder_2.newLine();
        _builder_2.append("foo.FooEnum");
        _builder_2.newLine();
        _builder_2.append("foo.FooEnum.FooEnumLiteral");
        _builder_2.newLine();
        this.assertAccessibleElements(elements2, _builder_2);
        final EdeltaAccessibleElements elements3 = this.derivedStateHelper.getAccessibleElements(ecoreref3);
        Assertions.<EdeltaAccessibleElement>assertThat(elements2).isSameAs(elements3);
        final EdeltaAccessibleElements elements4 = this.derivedStateHelper.getAccessibleElements(ecoreref4);
        StringConcatenation _builder_3 = new StringConcatenation();
        _builder_3.append("foo");
        _builder_3.newLine();
        _builder_3.append("foo.ANewClass");
        _builder_3.newLine();
        _builder_3.append("foo.FooDataType");
        _builder_3.newLine();
        _builder_3.append("foo.FooEnum");
        _builder_3.newLine();
        _builder_3.append("foo.FooEnum.FooEnumLiteral");
        _builder_3.newLine();
        this.assertAccessibleElements(elements4, _builder_3);
      } catch (Throwable _e) {
        throw Exceptions.sneakyThrow(_e);
      }
    };
    ObjectExtensions.<EdeltaProgram>operator_doubleArrow(_parseWithTestEcore, _function);
  }
  
  @Test
  public void testInvalidAmbiguousEcoreref() throws Exception {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("metamodel \"mainpackage\"");
    _builder.newLine();
    _builder.newLine();
    _builder.append("modifyEcore aTest epackage mainpackage {");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("ecoreref(MyClass)");
    _builder.newLine();
    _builder.append("}");
    _builder.newLine();
    final String input = _builder.toString();
    EdeltaProgram _parseWithTestEcoreWithSubPackage = this.parseWithTestEcoreWithSubPackage(input);
    final Procedure1<EdeltaProgram> _function = (EdeltaProgram it) -> {
      try {
        this.interpretProgram(it);
        StringConcatenation _builder_1 = new StringConcatenation();
        _builder_1.append("Ambiguous reference \'MyClass\':");
        _builder_1.newLine();
        _builder_1.append("  ");
        _builder_1.append("mainpackage.MyClass");
        _builder_1.newLine();
        _builder_1.append("  ");
        _builder_1.append("mainpackage.mainsubpackage.MyClass");
        _builder_1.newLine();
        _builder_1.append("  ");
        _builder_1.append("mainpackage.mainsubpackage.subsubpackage.MyClass");
        _builder_1.newLine();
        this.assertErrorsAsStrings(it, _builder_1);
      } catch (Throwable _e) {
        throw Exceptions.sneakyThrow(_e);
      }
    };
    ObjectExtensions.<EdeltaProgram>operator_doubleArrow(_parseWithTestEcoreWithSubPackage, _function);
  }
  
  @Test
  public void testAmbiguousEcorerefAfterRemoval() throws Exception {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("metamodel \"mainpackage\"");
    _builder.newLine();
    _builder.newLine();
    _builder.append("modifyEcore aTest epackage mainpackage {");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("EClassifiers -= ecoreref(mainpackage.MyClass)");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("ecoreref(MyClass) // still ambiguous");
    _builder.newLine();
    _builder.append("}");
    _builder.newLine();
    final String input = _builder.toString();
    EdeltaProgram _parseWithTestEcoreWithSubPackage = this.parseWithTestEcoreWithSubPackage(input);
    final Procedure1<EdeltaProgram> _function = (EdeltaProgram it) -> {
      try {
        this.interpretProgram(it);
        StringConcatenation _builder_1 = new StringConcatenation();
        _builder_1.append("Ambiguous reference \'MyClass\':");
        _builder_1.newLine();
        _builder_1.append("  ");
        _builder_1.append("mainpackage.mainsubpackage.MyClass");
        _builder_1.newLine();
        _builder_1.append("  ");
        _builder_1.append("mainpackage.mainsubpackage.subsubpackage.MyClass");
        _builder_1.newLine();
        this.assertErrorsAsStrings(it, _builder_1);
      } catch (Throwable _e) {
        throw Exceptions.sneakyThrow(_e);
      }
    };
    ObjectExtensions.<EdeltaProgram>operator_doubleArrow(_parseWithTestEcoreWithSubPackage, _function);
  }
  
  @Test
  public void testNonAmbiguousEcorerefAfterRemoval() throws Exception {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("import static org.eclipse.emf.ecore.util.EcoreUtil.remove");
    _builder.newLine();
    _builder.newLine();
    _builder.append("metamodel \"mainpackage\"");
    _builder.newLine();
    _builder.newLine();
    _builder.append("modifyEcore aTest epackage mainpackage {");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("EClassifiers -= ecoreref(mainpackage.MyClass)");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("remove(ecoreref(mainpackage.mainsubpackage.subsubpackage.MyClass))");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("ecoreref(MyClass) // non ambiguous");
    _builder.newLine();
    _builder.append("}");
    _builder.newLine();
    final String input = _builder.toString();
    EdeltaProgram _parseWithTestEcoreWithSubPackage = this.parseWithTestEcoreWithSubPackage(input);
    final Procedure1<EdeltaProgram> _function = (EdeltaProgram it) -> {
      try {
        final EdeltaCopiedEPackagesMap map = this.interpretProgram(it);
        this.validationTestHelper.assertNoErrors(it);
        final EClass mainSubPackageClass = this.getLastEClass(IterableExtensions.<EPackage>head(IterableExtensions.<EPackage>head(map.values()).getESubpackages()));
        final EdeltaEcoreReference lastEcoreRef = IterableExtensions.<EdeltaEcoreReferenceExpression>last(this.getAllEcoreReferenceExpressions(it)).getReference();
        Assert.assertNotNull(lastEcoreRef.getEnamedelement());
        Assert.assertSame(mainSubPackageClass, lastEcoreRef.getEnamedelement());
      } catch (Throwable _e) {
        throw Exceptions.sneakyThrow(_e);
      }
    };
    ObjectExtensions.<EdeltaProgram>operator_doubleArrow(_parseWithTestEcoreWithSubPackage, _function);
  }
  
  @Test
  public void testNonAmbiguousEcorerefAfterRemovalIsCorrectlyTypedInAssignment() throws Exception {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("import org.eclipse.emf.ecore.EAttribute");
    _builder.newLine();
    _builder.append("import org.eclipse.emf.ecore.EReference");
    _builder.newLine();
    _builder.newLine();
    _builder.append("metamodel \"mainpackage\"");
    _builder.newLine();
    _builder.newLine();
    _builder.append("modifyEcore aTest epackage mainpackage {");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("addNewEClass(\"ANewClass\") [");
    _builder.newLine();
    _builder.append("\t\t");
    _builder.append("addNewEAttribute(\"created\", null)");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("]");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("addNewEClass(\"AnotherNewClass\") [");
    _builder.newLine();
    _builder.append("\t\t");
    _builder.append("addNewEReference(\"created\", null)");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("]");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("EClassifiers -= ecoreref(ANewClass)");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("// \"created\" is not ambiguous anymore");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("ecoreref(created)");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("// and it\'s correctly typed (EReference, not EAttribute)");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("val EAttribute a = ecoreref(created) // ERROR");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("val EReference r = ecoreref(created) // OK");
    _builder.newLine();
    _builder.append("}");
    _builder.newLine();
    final String input = _builder.toString();
    EdeltaProgram _parseWithTestEcoreWithSubPackage = this.parseWithTestEcoreWithSubPackage(input);
    final Procedure1<EdeltaProgram> _function = (EdeltaProgram it) -> {
      try {
        this.interpretProgram(it);
        StringConcatenation _builder_1 = new StringConcatenation();
        _builder_1.append("Type mismatch: cannot convert from EReference to EAttribute");
        _builder_1.newLine();
        this.assertErrorsAsStrings(it, _builder_1);
      } catch (Throwable _e) {
        throw Exceptions.sneakyThrow(_e);
      }
    };
    ObjectExtensions.<EdeltaProgram>operator_doubleArrow(_parseWithTestEcoreWithSubPackage, _function);
  }
  
  @Test
  public void testNonAmbiguousEcorerefAfterRemovalIsCorrectlyTypedInFeatureCall2() throws Exception {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("import static org.eclipse.emf.ecore.util.EcoreUtil.remove");
    _builder.newLine();
    _builder.newLine();
    _builder.append("metamodel \"mainpackage\"");
    _builder.newLine();
    _builder.newLine();
    _builder.append("modifyEcore aTest epackage mainpackage {");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("addNewEClass(\"created\")");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("ESubpackages.head.addNewESubpackage(\"created\", null, null)");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("// \"created\" is ambiguous now");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("ecoreref(created)");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("remove(EClassifiers.last)");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("// \"created\" is not ambiguous anymore: linked to EPackage \"created\"");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("ecoreref(created).EStructuralFeatures // ERROR");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("ecoreref(created) => [");
    _builder.newLine();
    _builder.append("\t\t");
    _builder.append("abstract = true // ERROR");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("]");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("ecoreref(created).ESubpackages // OK");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("ecoreref(created).nonExistent // ERROR to cover the last case in the interpreter");
    _builder.newLine();
    _builder.append("}");
    _builder.newLine();
    final String input = _builder.toString();
    EdeltaProgram _parseWithTestEcoreWithSubPackage = this.parseWithTestEcoreWithSubPackage(input);
    final Procedure1<EdeltaProgram> _function = (EdeltaProgram it) -> {
      try {
        this.interpretProgram(it);
        StringConcatenation _builder_1 = new StringConcatenation();
        _builder_1.append("Ambiguous reference \'created\':");
        _builder_1.newLine();
        _builder_1.append("  ");
        _builder_1.append("mainpackage.created");
        _builder_1.newLine();
        _builder_1.append("  ");
        _builder_1.append("mainpackage.mainsubpackage.created");
        _builder_1.newLine();
        _builder_1.append("Cannot refer to org.eclipse.emf.ecore.EClass.getEStructuralFeatures()");
        _builder_1.newLine();
        _builder_1.append("Cannot refer to org.eclipse.emf.ecore.EClass.setAbstract(boolean)");
        _builder_1.newLine();
        _builder_1.append("The method or field nonExistent is undefined for the type EPackage");
        _builder_1.newLine();
        this.assertErrorsAsStrings(it, _builder_1);
      } catch (Throwable _e) {
        throw Exceptions.sneakyThrow(_e);
      }
    };
    ObjectExtensions.<EdeltaProgram>operator_doubleArrow(_parseWithTestEcoreWithSubPackage, _function);
  }
  
  @Test
  public void testInvalidAmbiguousEcorerefWithCreatedElements() throws Exception {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("metamodel \"mainpackage\"");
    _builder.newLine();
    _builder.newLine();
    _builder.append("modifyEcore aTest epackage mainpackage {");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("addNewEClass(\"created\") [");
    _builder.newLine();
    _builder.append("\t\t");
    _builder.append("addNewEAttribute(\"created\", null)");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("]");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("ecoreref(created)");
    _builder.newLine();
    _builder.append("}");
    _builder.newLine();
    final String input = _builder.toString();
    EdeltaProgram _parseWithTestEcoreWithSubPackage = this.parseWithTestEcoreWithSubPackage(input);
    final Procedure1<EdeltaProgram> _function = (EdeltaProgram it) -> {
      try {
        this.interpretProgram(it);
        StringConcatenation _builder_1 = new StringConcatenation();
        _builder_1.append("Ambiguous reference \'created\':");
        _builder_1.newLine();
        _builder_1.append("  ");
        _builder_1.append("mainpackage.created");
        _builder_1.newLine();
        _builder_1.append("  ");
        _builder_1.append("mainpackage.created.created");
        _builder_1.newLine();
        this.assertErrorsAsStrings(it, _builder_1);
      } catch (Throwable _e) {
        throw Exceptions.sneakyThrow(_e);
      }
    };
    ObjectExtensions.<EdeltaProgram>operator_doubleArrow(_parseWithTestEcoreWithSubPackage, _function);
  }
  
  @Test
  public void testNonAmbiguousEcorerefWithQualification() throws Exception {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("metamodel \"mainpackage\"");
    _builder.newLine();
    _builder.newLine();
    _builder.append("modifyEcore aTest epackage mainpackage {");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("addNewEClass(\"created\") [");
    _builder.newLine();
    _builder.append("\t\t");
    _builder.append("addNewEAttribute(\"created\", null)");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("]");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("ecoreref(created.created) // NON ambiguous");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("ecoreref(mainpackage.created) // NON ambiguous");
    _builder.newLine();
    _builder.append("}");
    _builder.newLine();
    final String input = _builder.toString();
    EdeltaProgram _parseWithTestEcoreWithSubPackage = this.parseWithTestEcoreWithSubPackage(input);
    final Procedure1<EdeltaProgram> _function = (EdeltaProgram it) -> {
      try {
        this.interpretProgram(it);
        this.validationTestHelper.assertNoErrors(it);
      } catch (Throwable _e) {
        throw Exceptions.sneakyThrow(_e);
      }
    };
    ObjectExtensions.<EdeltaProgram>operator_doubleArrow(_parseWithTestEcoreWithSubPackage, _function);
  }
  
  @Test
  public void testNonAmbiguousEcoreref() throws Exception {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("metamodel \"mainpackage\"");
    _builder.newLine();
    _builder.newLine();
    _builder.append("modifyEcore aTest epackage mainpackage {");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("addNewEClass(\"WorkPlace\")");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("addNewEClass(\"LivingPlace\")");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("addNewEClass(\"Place\")");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("ecoreref(Place) // NON ambiguous");
    _builder.newLine();
    _builder.append("}");
    _builder.newLine();
    final String input = _builder.toString();
    EdeltaProgram _parseWithTestEcoreWithSubPackage = this.parseWithTestEcoreWithSubPackage(input);
    final Procedure1<EdeltaProgram> _function = (EdeltaProgram it) -> {
      try {
        this.interpretProgram(it);
        this.validationTestHelper.assertNoErrors(it);
      } catch (Throwable _e) {
        throw Exceptions.sneakyThrow(_e);
      }
    };
    ObjectExtensions.<EdeltaProgram>operator_doubleArrow(_parseWithTestEcoreWithSubPackage, _function);
  }
  
  @Test
  public void testCallOperationThatCallsAnotherNonVoidOperation() throws Exception {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("metamodel \"foo\"");
    _builder.newLine();
    _builder.newLine();
    _builder.append("def createANewClassInMyEcore(String name) {");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("if (aCheck()) // this will always return false");
    _builder.newLine();
    _builder.append("\t\t");
    _builder.append("return null // so this won\'t be executed");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("if (aCheck2()) // this will always return false");
    _builder.newLine();
    _builder.append("\t\t");
    _builder.append("return null // so this won\'t be executed");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("ecoreref(foo).addNewEClass(name)");
    _builder.newLine();
    _builder.append("}");
    _builder.newLine();
    _builder.newLine();
    _builder.append("def aCheck() {");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("return false");
    _builder.newLine();
    _builder.append("}");
    _builder.newLine();
    _builder.newLine();
    _builder.append("def aCheck2() : boolean {");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("false");
    _builder.newLine();
    _builder.append("}");
    _builder.newLine();
    _builder.newLine();
    _builder.append("modifyEcore SomeChanges epackage foo {");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("// the ANewClass is not actually created");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("// not shown in the outline");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("\"ANewClass\".createANewClassInMyEcore() => [");
    _builder.newLine();
    _builder.append("\t\t");
    _builder.append("abstract = true");
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
        Assert.assertTrue(it.isAbstract());
      };
      ObjectExtensions.<EClass>operator_doubleArrow(_lastEClass, _function_1);
    };
    this.assertAfterInterpretationOfEdeltaModifyEcoreOperation(_builder, _function);
  }
  
  @Test
  public void testCallOperationThatCallsAnotherNonVoidOperationInAnotherFile() throws Exception {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("import org.eclipse.emf.ecore.EPackage");
    _builder.newLine();
    _builder.newLine();
    _builder.append("def create(EPackage it) {");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("if (aCheck()) // this will always return false");
    _builder.newLine();
    _builder.append("\t\t");
    _builder.append("return null // so this won\'t be executed");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("if (aCheck2()) // this will always return false");
    _builder.newLine();
    _builder.append("\t\t");
    _builder.append("return null // so this won\'t be executed");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("addNewEClass(\"NewClass\")");
    _builder.newLine();
    _builder.append("}");
    _builder.newLine();
    _builder.newLine();
    _builder.append("def aCheck() {");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("return false");
    _builder.newLine();
    _builder.append("}");
    _builder.newLine();
    _builder.newLine();
    _builder.append("def aCheck2() : boolean {");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("false");
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
      try {
        EdeltaCopiedEPackagesMap _interpretProgram = this.interpretProgram(it);
        final Procedure1<EdeltaCopiedEPackagesMap> _function_1 = (EdeltaCopiedEPackagesMap it_1) -> {
          final EClass created = this.getEClassByName(it_1.get("foo"), "NewClass");
          Assert.assertNotNull(created);
        };
        ObjectExtensions.<EdeltaCopiedEPackagesMap>operator_doubleArrow(_interpretProgram, _function_1);
      } catch (Throwable _e) {
        throw Exceptions.sneakyThrow(_e);
      }
    };
    ObjectExtensions.<EdeltaProgram>operator_doubleArrow(_parseSeveralWithTestEcore, _function);
  }
  
  private void assertAfterInterpretationOfEdeltaModifyEcoreOperation(final CharSequence input, final Procedure1<? super EPackage> testExecutor) throws Exception {
    this.assertAfterInterpretationOfEdeltaModifyEcoreOperation(input, true, testExecutor);
  }
  
  private void assertAfterInterpretationOfEdeltaModifyEcoreOperation(final CharSequence input, final boolean doValidate, final Procedure1<? super EPackage> testExecutor) throws Exception {
    final EdeltaProgram program = this.parseWithTestEcore(input);
    this.assertAfterInterpretationOfEdeltaModifyEcoreOperation(program, doValidate, testExecutor);
  }
  
  private void assertAfterInterpretationOfEdeltaModifyEcoreOperation(final List<CharSequence> inputs, final boolean doValidate, final Procedure1<? super EPackage> testExecutor) throws Exception {
    final EdeltaProgram program = this.parseSeveralWithTestEcore(inputs);
    this.assertAfterInterpretationOfEdeltaModifyEcoreOperation(program, doValidate, testExecutor);
  }
  
  private void assertAfterInterpretationOfEdeltaModifyEcoreOperation(final EdeltaProgram program, final boolean doValidate, final Procedure1<? super EPackage> testExecutor) throws Exception {
    final Procedure1<EPackage> _function = (EPackage it) -> {
      if (doValidate) {
        this.validationTestHelper.assertNoErrors(program);
      }
      testExecutor.apply(it);
    };
    this.assertAfterInterpretationOfEdeltaModifyEcoreOperation(program, _function);
  }
  
  private void assertAfterInterpretationOfEdeltaModifyEcoreOperation(final EdeltaProgram program, final Procedure1<? super EPackage> testExecutor) throws Exception {
    final EdeltaModifyEcoreOperation it = this.lastModifyEcoreOperation(program);
    this.interpreter.evaluateModifyEcoreOperations(program);
    final String packageName = it.getEpackage().getName();
    final EPackage epackage = this.derivedStateHelper.getCopiedEPackagesMap(program.eResource()).get(packageName);
    testExecutor.apply(epackage);
  }
  
  private EdeltaCopiedEPackagesMap interpretProgram(final EdeltaProgram program) throws Exception {
    this.interpreter.evaluateModifyEcoreOperations(program);
    return this.derivedStateHelper.getCopiedEPackagesMap(program.eResource());
  }
  
  private void assertEcoreRefExpElementMapsToXExpression(final EdeltaEcoreReference reference, final String expectedFeatureCallSimpleName) throws Exception {
    final XExpression exp = this.derivedStateHelper.getResponsibleExpression(reference);
    Assert.assertNotNull(exp);
    Assert.assertEquals(expectedFeatureCallSimpleName, this.getFeatureCall(exp).getFeature().getSimpleName());
  }
}
