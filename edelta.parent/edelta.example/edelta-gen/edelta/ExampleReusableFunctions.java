package edelta;

import edelta.lib.AbstractEdelta;
import edelta.lib.EdeltaDefaultRuntime;
import org.eclipse.emf.ecore.EClass;

@SuppressWarnings("all")
public class ExampleReusableFunctions extends EdeltaDefaultRuntime {
  public ExampleReusableFunctions() {
    
  }
  
  public ExampleReusableFunctions(final AbstractEdelta other) {
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
