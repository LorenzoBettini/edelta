package edelta;

import edelta.Example;
import edelta.lib.AbstractEdelta;
import edelta.refactorings.lib.EdeltaRefactorings;

@SuppressWarnings("all")
public class AnotherExample extends AbstractEdelta {
  private Example example;
  
  private EdeltaRefactorings std;
  
  public AnotherExample() {
    example = new Example(this);
    std = new EdeltaRefactorings(this);
  }
  
  public AnotherExample(final AbstractEdelta other) {
    super(other);
    example = new Example(other);
    std = new EdeltaRefactorings(other);
  }
  
  @Override
  public void performSanityChecks() throws Exception {
    ensureEPackageIsLoaded("ecore");
    ensureEPackageIsLoaded("myexample");
    ensureEPackageIsLoaded("myecore");
  }
}
