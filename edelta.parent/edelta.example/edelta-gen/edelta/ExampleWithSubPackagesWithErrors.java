package edelta;

import edelta.lib.AbstractEdelta;
import edelta.lib.EdeltaDefaultRuntime;

@SuppressWarnings("all")
public class ExampleWithSubPackagesWithErrors extends EdeltaDefaultRuntime {
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
