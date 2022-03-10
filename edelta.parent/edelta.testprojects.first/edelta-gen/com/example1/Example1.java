package com.example1;

import edelta.lib.EdeltaRuntime;
import edelta.lib.EdeltaDefaultRuntime;
import edelta.lib.EdeltaUtils;
import java.util.function.Consumer;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.xtext.xbase.lib.ObjectExtensions;
import org.eclipse.xtext.xbase.lib.Procedures.Procedure1;

@SuppressWarnings("all")
public class Example1 extends EdeltaDefaultRuntime {
  public Example1() {
    
  }
  
  public Example1(final EdeltaRuntime other) {
    super(other);
  }
  
  /**
   * Reusable function to create a new EClass with the
   * specified name, setting MyEClass as its superclass
   * @param name
   */
  public EClass myReusableCreateSubclassOfMyEClass(final String name) {
    EClass _newEClass = EdeltaUtils.newEClass(name);
    final Procedure1<EClass> _function = (EClass it) -> {
      EList<EClass> _eSuperTypes = it.getESuperTypes();
      _eSuperTypes.add(getEClass("myecore1", "MyEClass"));
    };
    return ObjectExtensions.<EClass>operator_doubleArrow(_newEClass, _function);
  }
  
  public void someModifications(final EPackage it) {
    final Consumer<EClass> _function = (EClass it_1) -> {
      this.stdLib.addNewEAttribute(it_1, "myStringAttribute", getEDataType("ecore", "EString"));
      final Consumer<EReference> _function_1 = (EReference it_2) -> {
        it_2.setUpperBound((-1));
        it_2.setContainment(true);
        it_2.setLowerBound(0);
      };
      this.stdLib.addNewEReference(it_1, "myReference", getEClass("myecore1", "MyEClass"), _function_1);
    };
    this.stdLib.addNewEClass(it, "NewClass", _function);
  }
  
  @Override
  public void performSanityChecks() throws Exception {
    ensureEPackageIsLoaded("myecore1");
    ensureEPackageIsLoaded("ecore");
  }
  
  @Override
  protected void doExecute() throws Exception {
    someModifications(getEPackage("myecore1"));
  }
}
