package edelta;

import edelta.lib.EdeltaRuntime;
import edelta.lib.EdeltaDefaultRuntime;

@SuppressWarnings("all")
public class ExampleWithSubPackagesWithErrors extends EdeltaDefaultRuntime {
  public ExampleWithSubPackagesWithErrors() {
    
  }
  
  public ExampleWithSubPackagesWithErrors(final EdeltaRuntime other) {
    super(other);
  }
  
  @Override
  public void performSanityChecks() throws Exception {
    ensureEPackageIsLoaded("ecore");
  }
}
