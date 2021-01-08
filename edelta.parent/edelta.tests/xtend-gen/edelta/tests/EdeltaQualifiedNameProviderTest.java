package edelta.tests;

import com.google.inject.Inject;
import edelta.tests.EdeltaAbstractTest;
import edelta.tests.EdeltaInjectorProvider;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EPackage;
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
      Assert.assertEquals("edelta.__synthetic0", this._iQualifiedNameProvider.getFullyQualifiedName(this.parseHelper.parse(_builder)).toString());
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }
  
  @Test
  public void testProgramWithPackage() {
    try {
      StringConcatenation _builder = new StringConcatenation();
      _builder.append("package foo");
      Assert.assertEquals("foo.__synthetic0", this._iQualifiedNameProvider.getFullyQualifiedName(this.parseHelper.parse(_builder)).toString());
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }
  
  @Test
  public void testEPackageWithCycle() {
    EPackage _createEPackage = EcoreFactory.eINSTANCE.createEPackage();
    final Procedure1<EPackage> _function = (EPackage it) -> {
      it.setName("p1");
    };
    final EPackage p1 = ObjectExtensions.<EPackage>operator_doubleArrow(_createEPackage, _function);
    EPackage _createEPackage_1 = EcoreFactory.eINSTANCE.createEPackage();
    final Procedure1<EPackage> _function_1 = (EPackage it) -> {
      it.setName("p2");
    };
    final EPackage p2 = ObjectExtensions.<EPackage>operator_doubleArrow(_createEPackage_1, _function_1);
    EList<EPackage> _eSubpackages = p1.getESubpackages();
    _eSubpackages.add(p2);
    Assert.assertEquals("p1.p2", this._iQualifiedNameProvider.getFullyQualifiedName(p2).toString());
    EList<EPackage> _eSubpackages_1 = p2.getESubpackages();
    _eSubpackages_1.add(p1);
    Assert.assertEquals("p2", this._iQualifiedNameProvider.getFullyQualifiedName(p2).toString());
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
