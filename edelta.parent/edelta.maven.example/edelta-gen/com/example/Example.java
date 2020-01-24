package com.example;

import edelta.lib.AbstractEdelta;
import edelta.refactorings.lib.EdeltaRefactorings;
import java.util.function.Consumer;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.xtext.xbase.lib.Extension;
import org.eclipse.xtext.xbase.lib.ObjectExtensions;
import org.eclipse.xtext.xbase.lib.Procedures.Procedure1;

@SuppressWarnings("all")
public class Example extends AbstractEdelta {
  @Extension
  private EdeltaRefactorings refactorings;
  
  public Example() {
    refactorings = new EdeltaRefactorings(this);
  }
  
  public Example(final AbstractEdelta other) {
    super(other);
  }
  
  public EClass createClass(final String name) {
    EClass _newEClass = this.lib.newEClass(name);
    final Procedure1<EClass> _function = (EClass it) -> {
      EList<EClass> _eSuperTypes = it.getESuperTypes();
      _eSuperTypes.add(getEClass("myecore", "MyEClass"));
    };
    return ObjectExtensions.<EClass>operator_doubleArrow(_newEClass, _function);
  }
  
  public void aModification(final EPackage it) {
    final Consumer<EClass> _function = (EClass it_1) -> {
      this.refactorings.addMandatoryAttr(it_1, "ANewAttribute", getEDataType("ecore", "EString"));
      getEAttribute("myecore", "MyNewClass", "ANewAttribute").setEType(getEDataType("ecore", "EInt"));
    };
    this.lib.addNewEClass(it, "MyNewClass", _function);
    this.lib.addEClass(it, this.createClass("ANewDerivedEClass"));
    getEClass("myecore", "ANewDerivedEClass").setAbstract(true);
  }
  
  @Override
  public void performSanityChecks() throws Exception {
    ensureEPackageIsLoaded("myecore");
    ensureEPackageIsLoaded("ecore");
  }
  
  @Override
  protected void doExecute() throws Exception {
    aModification(getEPackage("myecore"));
  }
}
