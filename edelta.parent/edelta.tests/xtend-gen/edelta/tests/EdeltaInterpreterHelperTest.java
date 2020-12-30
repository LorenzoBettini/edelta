package edelta.tests;

import com.google.inject.Inject;
import edelta.edelta.EdeltaModifyEcoreOperation;
import edelta.edelta.EdeltaProgram;
import edelta.edelta.EdeltaUseAs;
import edelta.interpreter.EdeltaInterpreterHelper;
import edelta.interpreter.EdeltaInterpreterRuntimeException;
import edelta.lib.AbstractEdelta;
import edelta.tests.EdeltaAbstractTest;
import edelta.tests.EdeltaInjectorProviderForJavaReflectAccess;
import edelta.tests.additional.MyCustomEdelta;
import edelta.tests.additional.MyCustomEdeltaThatCannotBeLoadedAtRuntime;
import org.assertj.core.api.AbstractThrowableAssert;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.ThrowableAssert;
import org.eclipse.xtend2.lib.StringConcatenation;
import org.eclipse.xtext.common.types.util.JavaReflectAccess;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.XtextRunner;
import org.eclipse.xtext.xbase.lib.Exceptions;
import org.eclipse.xtext.xbase.lib.IterableExtensions;
import org.eclipse.xtext.xbase.lib.ObjectExtensions;
import org.eclipse.xtext.xbase.lib.Procedures.Procedure1;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(XtextRunner.class)
@InjectWith(EdeltaInjectorProviderForJavaReflectAccess.class)
@SuppressWarnings("all")
public class EdeltaInterpreterHelperTest extends EdeltaAbstractTest {
  public static class InstantiateExceptionClass {
    public InstantiateExceptionClass() {
      try {
        throw new InstantiationException();
      } catch (Throwable _e) {
        throw Exceptions.sneakyThrow(_e);
      }
    }
  }
  
  @Inject
  private EdeltaInterpreterHelper interpreterHelper;
  
  @Inject
  private JavaReflectAccess javaReflectAccess;
  
  private AbstractEdelta other;
  
  @Before
  public void setup() {
    this.other = new AbstractEdelta() {
    };
  }
  
  @Test
  public void testSafeInstantiateOfValidUseAs() {
    try {
      StringConcatenation _builder = new StringConcatenation();
      _builder.append("import edelta.tests.additional.MyCustomEdelta");
      _builder.newLine();
      _builder.newLine();
      _builder.append("use MyCustomEdelta as my");
      _builder.newLine();
      EdeltaUseAs _head = IterableExtensions.<EdeltaUseAs>head(this._parseHelper.parse(_builder).getUseAsClauses());
      final Procedure1<EdeltaUseAs> _function = (EdeltaUseAs it) -> {
        Assert.assertEquals(
          MyCustomEdelta.class, 
          this.interpreterHelper.safeInstantiate(this.javaReflectAccess, it, this.other).getClass());
      };
      ObjectExtensions.<EdeltaUseAs>operator_doubleArrow(_head, _function);
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }
  
  @Test
  public void testSafeInstantiateOfUseAsWithoutType() {
    try {
      StringConcatenation _builder = new StringConcatenation();
      _builder.append("use as my");
      _builder.newLine();
      EdeltaUseAs _head = IterableExtensions.<EdeltaUseAs>head(this._parseHelper.parse(_builder).getUseAsClauses());
      final Procedure1<EdeltaUseAs> _function = (EdeltaUseAs it) -> {
        Assertions.assertThat(
          this.interpreterHelper.safeInstantiate(this.javaReflectAccess, it, this.other).getClass()).isNotNull();
      };
      ObjectExtensions.<EdeltaUseAs>operator_doubleArrow(_head, _function);
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }
  
  @Test
  public void testSafeInstantiateOfValidUseAsWithoutType() {
    try {
      StringConcatenation _builder = new StringConcatenation();
      _builder.append("import edelta.tests.EdeltaInterpreterHelperTest.InstantiateExceptionClass");
      _builder.newLine();
      _builder.append("use InstantiateExceptionClass as my");
      _builder.newLine();
      EdeltaUseAs _head = IterableExtensions.<EdeltaUseAs>head(this._parseHelper.parse(_builder).getUseAsClauses());
      final Procedure1<EdeltaUseAs> _function = (EdeltaUseAs it) -> {
        Assertions.assertThat(
          this.interpreterHelper.safeInstantiate(this.javaReflectAccess, it, this.other).getClass()).isNotNull();
      };
      ObjectExtensions.<EdeltaUseAs>operator_doubleArrow(_head, _function);
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }
  
  @Test
  public void testSafeInstantiateOfUnresolvedUseAsType() {
    final ThrowableAssert.ThrowingCallable _function = () -> {
      StringConcatenation _builder = new StringConcatenation();
      _builder.append("use NonExistent as my");
      _builder.newLine();
      EdeltaUseAs _head = IterableExtensions.<EdeltaUseAs>head(this._parseHelper.parse(_builder).getUseAsClauses());
      final Procedure1<EdeltaUseAs> _function_1 = (EdeltaUseAs it) -> {
        this.interpreterHelper.safeInstantiate(this.javaReflectAccess, it, this.other).getClass();
      };
      ObjectExtensions.<EdeltaUseAs>operator_doubleArrow(_head, _function_1);
    };
    Assertions.assertThatThrownBy(_function).isInstanceOf(IllegalStateException.class).hasMessageContaining("Cannot resolve proxy");
  }
  
  @Test
  public void testSafeInstantiateOfValidUseAsButNotFoundAtRuntime() {
    final ThrowableAssert.ThrowingCallable _function = () -> {
      StringConcatenation _builder = new StringConcatenation();
      _builder.append("import edelta.tests.additional.MyCustomEdeltaThatCannotBeLoadedAtRuntime");
      _builder.newLine();
      _builder.newLine();
      _builder.append("use MyCustomEdeltaThatCannotBeLoadedAtRuntime as my");
      _builder.newLine();
      EdeltaUseAs _head = IterableExtensions.<EdeltaUseAs>head(this._parseHelper.parse(_builder).getUseAsClauses());
      final Procedure1<EdeltaUseAs> _function_1 = (EdeltaUseAs it) -> {
        this.interpreterHelper.safeInstantiate(this.javaReflectAccess, it, this.other).getClass();
      };
      ObjectExtensions.<EdeltaUseAs>operator_doubleArrow(_head, _function_1);
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
  public void testFilterOperationsWithNullEPackage() {
    try {
      StringConcatenation _builder = new StringConcatenation();
      _builder.append("modifyEcore first epackage {}");
      _builder.newLine();
      _builder.append("modifyEcore second epackage foo {}");
      _builder.newLine();
      EdeltaProgram _parse = this._parseHelper.parse(_builder);
      final Procedure1<EdeltaProgram> _function = (EdeltaProgram it) -> {
        Assertions.<EdeltaModifyEcoreOperation>assertThat(this.interpreterHelper.filterOperations(it.getModifyEcoreOperations())).containsExactly(IterableExtensions.<EdeltaModifyEcoreOperation>last(it.getModifyEcoreOperations()));
      };
      ObjectExtensions.<EdeltaProgram>operator_doubleArrow(_parse, _function);
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }
  
  @Test
  public void testFilterOperationsWithSubPackage() {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("metamodel \"mainpackage.mainsubpackage\"");
    _builder.newLine();
    _builder.newLine();
    _builder.append("modifyEcore aTest epackage mainsubpackage {");
    _builder.newLine();
    _builder.append("\t");
    _builder.newLine();
    _builder.append("}");
    _builder.newLine();
    EdeltaProgram _parseWithTestEcoreWithSubPackage = this.parseWithTestEcoreWithSubPackage(_builder);
    final Procedure1<EdeltaProgram> _function = (EdeltaProgram it) -> {
      Assertions.<EdeltaModifyEcoreOperation>assertThat(this.interpreterHelper.filterOperations(it.getModifyEcoreOperations())).isEmpty();
    };
    ObjectExtensions.<EdeltaProgram>operator_doubleArrow(_parseWithTestEcoreWithSubPackage, _function);
  }
}
