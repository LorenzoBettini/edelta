package edelta;

import edelta.lib.EdeltaDefaultRuntime;
import edelta.lib.EdeltaRuntime;

@SuppressWarnings("all")
public class ExampleWithSubPackagesWithErrors extends EdeltaDefaultRuntime {
  public ExampleWithSubPackagesWithErrors(final EdeltaRuntime other) {
    super(other);
  }

  @Override
  public void performSanityChecks() throws Exception {
    ensureEPackageIsLoaded("ecore");
  }
}
