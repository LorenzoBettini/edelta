package edelta;

import edelta.lib.AbstractEdelta;
import edelta.lib.EdeltaDefaultRuntime;
import org.eclipse.emf.ecore.EPackage;

@SuppressWarnings("all")
public class ExampleErrorRecovery extends EdeltaDefaultRuntime {
  public ExampleErrorRecovery() {
    
  }
  
  public ExampleErrorRecovery(final AbstractEdelta other) {
    super(other);
  }
  
  public void creation(final EPackage it) {
    this.stdLib.addNewEClass(it, "NewClass");
    getEClass("myecore", "NewClass").setAbstract(true);
  }
  
  @Override
  public void performSanityChecks() throws Exception {
    ensureEPackageIsLoaded("ecore");
    ensureEPackageIsLoaded("myecore");
  }
  
  @Override
  protected void doExecute() throws Exception {
    creation(getEPackage("myecore"));
  }
}
