package com.example;

import edelta.lib.AbstractEdelta;
import gssi.refactorings.MMrefactorings;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.xtext.xbase.lib.ObjectExtensions;
import org.eclipse.xtext.xbase.lib.Procedures.Procedure1;

@SuppressWarnings("all")
public class Example extends AbstractEdelta {
  private MMrefactorings refactorings;
  
  public Example() {
    refactorings = new MMrefactorings(this);
  }
  
  public Example(final AbstractEdelta other) {
    super(other);
  }
  
  public EClass createClass(final String name) {
    EClass _newEClass = this.lib.newEClass(name);
    final Procedure1<EClass> _function = new Procedure1<EClass>() {
      public void apply(final EClass it) {
        EList<EClass> _eSuperTypes = it.getESuperTypes();
        _eSuperTypes.add(getEClass("myecore", "MyEClass"));
      }
    };
    return ObjectExtensions.<EClass>operator_doubleArrow(_newEClass, _function);
  }
  
  @Override
  public void performSanityChecks() throws Exception {
    ensureEPackageIsLoaded("myecore");
    ensureEPackageIsLoaded("ecore");
  }
  
  @Override
  protected void doExecute() throws Exception {
    createEClass("myecore", "MyNewClass", createList(this::_createEClass_MyNewClass_in_myecore));
  }
  
  public void _createEClass_MyNewClass_in_myecore(final EClass it) {
    {
      this.refactorings.addMandatoryAttr(it, "ANewAttribute", getEDataType("ecore", "EString"));
      getEAttribute("myecore", "MyNewClass", "ANewAttribute");
      this.lib.addEClass(it.getEPackage(), this.createClass("ANewDerivedEClass"));
    }
  }
}
