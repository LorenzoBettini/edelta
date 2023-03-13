package edelta;

import edelta.lib.EdeltaDefaultRuntime;
import edelta.lib.EdeltaRuntime;
import org.eclipse.emf.ecore.EClass;

@SuppressWarnings("all")
public class ExampleReusableFunctions extends EdeltaDefaultRuntime {
  public ExampleReusableFunctions(final EdeltaRuntime other) {
    super(other);
  }

  public EClass createANewClassInMyEcore(final String name) {
    return this.stdLib.addNewEClass(getEPackage("myecore"), name);
  }

  @Override
  public void performSanityChecks() throws Exception {
    ensureEPackageIsLoaded("ecore");
    ensureEPackageIsLoaded("myecore");
  }
}
