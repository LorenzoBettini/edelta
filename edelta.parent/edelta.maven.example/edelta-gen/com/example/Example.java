package com.example;

import edelta.lib.AbstractEdelta;
import edelta.lib.EdeltaDefaultRuntime;
import edelta.refactorings.lib.EdeltaRefactorings;
import java.util.function.Consumer;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.xtext.xbase.lib.Extension;

@SuppressWarnings("all")
public class Example extends EdeltaDefaultRuntime {
  @Extension
  private EdeltaRefactorings refactorings;
  
  @Extension
  private ExampleReusableFunctions myfunctions;
  
  public Example() {
    refactorings = new EdeltaRefactorings(this);
    myfunctions = new ExampleReusableFunctions(this);
  }
  
  public Example(final AbstractEdelta other) {
    super(other);
    refactorings = new EdeltaRefactorings(other);
    myfunctions = new ExampleReusableFunctions(other);
  }
  
  public EClass createSubClassOfMyEClass(final String name) {
    return this.myfunctions.createClassWithSubClass(name, getEClass("myecore", "MyEClass"));
  }
  
  public void aModification(final EPackage it) {
    final Consumer<EClass> _function = (EClass it_1) -> {
      this.refactorings.addMandatoryAttribute(it_1, "ANewAttribute", getEDataType("ecore", "EString"));
      getEAttribute("myecore", "MyNewClass", "ANewAttribute").setEType(getEDataType("ecore", "EInt"));
    };
    this.stdLib.addNewEClass(it, "MyNewClass", _function);
    this.stdLib.addEClass(it, this.createSubClassOfMyEClass("ANewDerivedEClass"));
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
