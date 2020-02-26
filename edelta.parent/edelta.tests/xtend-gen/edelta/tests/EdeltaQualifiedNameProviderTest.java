package edelta.tests;

import com.google.inject.Inject;
import edelta.tests.EdeltaAbstractTest;
import edelta.tests.EdeltaInjectorProvider;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EcoreFactory;
import org.eclipse.xtend2.lib.StringConcatenation;
import org.eclipse.xtext.naming.IQualifiedNameProvider;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.XtextRunner;
import org.eclipse.xtext.xbase.lib.Exceptions;
import org.eclipse.xtext.xbase.lib.Extension;
import org.eclipse.xtext.xbase.lib.ObjectExtensions;
import org.eclipse.xtext.xbase.lib.Procedures.Procedure1;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(XtextRunner.class)
@InjectWith(EdeltaInjectorProvider.class)
@SuppressWarnings("all")
public class EdeltaQualifiedNameProviderTest extends EdeltaAbstractTest {
  @Inject
  @Extension
  private IQualifiedNameProvider _iQualifiedNameProvider;
  
  @Test
  public void testProgramWithoutPackage() {
    try {
      StringConcatenation _builder = new StringConcatenation();
      Assert.assertEquals("edelta.__synthetic0", this._iQualifiedNameProvider.getFullyQualifiedName(this._parseHelper.parse(_builder)).toString());
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }
  
  @Test
  public void testProgramWithPackage() {
    try {
      StringConcatenation _builder = new StringConcatenation();
      _builder.append("package foo");
      Assert.assertEquals("foo.__synthetic0", this._iQualifiedNameProvider.getFullyQualifiedName(this._parseHelper.parse(_builder)).toString());
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }
  
  @Test
  public void testAnyOtherElement() {
    EClass _createEClass = EcoreFactory.eINSTANCE.createEClass();
    final Procedure1<EClass> _function = (EClass it) -> {
      it.setName("foo");
    };
    final EClass c = ObjectExtensions.<EClass>operator_doubleArrow(_createEClass, _function);
    Assert.assertEquals("foo", 
      this._iQualifiedNameProvider.getFullyQualifiedName(c).toString());
  }
}
