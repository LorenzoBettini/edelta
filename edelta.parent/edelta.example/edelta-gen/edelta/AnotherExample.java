package edelta;

import edelta.lib.EdeltaDefaultRuntime;
import edelta.lib.EdeltaRuntime;
import edelta.refactorings.lib.EdeltaRefactorings;

@SuppressWarnings("all")
public class AnotherExample extends EdeltaDefaultRuntime {
  private Example example;
  
  private EdeltaRefactorings std;
  
  public AnotherExample(final EdeltaRuntime other) {
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
