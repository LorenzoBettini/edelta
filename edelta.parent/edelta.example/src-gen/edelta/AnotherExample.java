package edelta;

import edelta.Example;
import edelta.lib.AbstractEdelta;
import gssi.refactorings.MMrefactorings;
import org.eclipse.emf.ecore.EClass;

@SuppressWarnings("all")
public class AnotherExample extends AbstractEdelta {
  private Example example;
  
  private MMrefactorings std;
  
  public AnotherExample() {
    example = new Example(this);
    std = new MMrefactorings(this);
  }
  
  public AnotherExample(final AbstractEdelta other) {
    super(other);
  }
  
  @Override
  public void performSanityChecks() throws Exception {
    ensureEPackageIsLoaded("ecore");
    ensureEPackageIsLoaded("myexample");
    ensureEPackageIsLoaded("myecore");
  }
  
  @Override
  protected void doExecute() throws Exception {
    this.example.createClass("Foo");
    createEClass("myecore", "ANewClass", createList(this::_createEClass_ANewClass_in_myecore));
  }
  
  public void _createEClass_ANewClass_in_myecore(final EClass it) {
    this.std.addMandatoryAttr("name", getEDataType("ecore", "EString"), it);
  }
}
