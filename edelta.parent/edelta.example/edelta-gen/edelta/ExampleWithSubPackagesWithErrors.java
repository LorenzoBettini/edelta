package edelta;

import edelta.lib.AbstractEdelta;

@SuppressWarnings("all")
public class ExampleWithSubPackagesWithErrors extends AbstractEdelta {
  public ExampleWithSubPackagesWithErrors() {
    
  }
  
  public ExampleWithSubPackagesWithErrors(final AbstractEdelta other) {
    super(other);
  }
  
  @Override
  public void performSanityChecks() throws Exception {
    ensureEPackageIsLoaded("ecore");
  }
}
