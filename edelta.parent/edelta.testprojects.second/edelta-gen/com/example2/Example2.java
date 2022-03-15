package com.example2;

import com.example1.Example1;
import edelta.lib.EdeltaDefaultRuntime;
import edelta.lib.EdeltaRuntime;
import java.util.function.Consumer;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.xtext.xbase.lib.ObjectExtensions;
import org.eclipse.xtext.xbase.lib.Procedures.Procedure1;

@SuppressWarnings("all")
public class Example2 extends EdeltaDefaultRuntime {
  private Example1 example1;
  
  public Example2(final EdeltaRuntime other) {
    super(other);
    example1 = new Example1(this);
  }
  
  public void someModifications(final EPackage it) {
    final Consumer<EClass> _function = (EClass it_1) -> {
      this.stdLib.addNewEAttribute(it_1, "myStringAttribute", getEDataType("ecore", "EString"));
      final Consumer<EReference> _function_1 = (EReference it_2) -> {
        it_2.setUpperBound((-1));
        it_2.setContainment(true);
        it_2.setLowerBound(0);
      };
      this.stdLib.addNewEReference(it_1, "myReference", getEClass("myecore2", "MyEClass"), _function_1);
    };
    this.stdLib.addNewEClass(it, "NewClass", _function);
  }
  
  public void otherModifications(final EPackage it) {
    this.stdLib.addEClass(it, this.example1.myReusableCreateSubclassOfMyEClass("ASubclassOfMyEClass"));
    EClass _myReusableCreateSubclassOfMyEClass = this.example1.myReusableCreateSubclassOfMyEClass("AnotherSubclassOfMyEClass");
    final Procedure1<EClass> _function = (EClass it_1) -> {
      EList<EClass> _eSuperTypes = it_1.getESuperTypes();
      _eSuperTypes.add(getEClass("myecore2", "NewClass"));
    };
    EClass _doubleArrow = ObjectExtensions.<EClass>operator_doubleArrow(_myReusableCreateSubclassOfMyEClass, _function);
    this.stdLib.addEClass(it, _doubleArrow);
  }
  
  @Override
  public void performSanityChecks() throws Exception {
    ensureEPackageIsLoaded("myecore2");
    ensureEPackageIsLoaded("ecore");
  }
  
  @Override
  protected void doExecute() throws Exception {
    someModifications(getEPackage("myecore2"));
    otherModifications(getEPackage("myecore2"));
  }
}
