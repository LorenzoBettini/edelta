package edelta;

import edelta.lib.AbstractEdelta;
import org.eclipse.emf.ecore.EPackage;

@SuppressWarnings("all")
public class ExampleAccessNotYetExistingElements extends AbstractEdelta {
  public ExampleAccessNotYetExistingElements() {
    
  }
  
  public ExampleAccessNotYetExistingElements(final AbstractEdelta other) {
    super(other);
  }
  
  public void creation(final EPackage it) {
    this.lib.addNewEClass(it, "NewClass");
    getEClass("myecore", "NewClass").setAbstract(true);
  }
  
  @Override
  public void performSanityChecks() throws Exception {
    ensureEPackageIsLoaded("ecore");
    ensureEPackageIsLoaded("myecore");
    ensureEPackageIsLoaded("mainpackage");
  }
  
  @Override
  protected void doExecute() throws Exception {
    creation(getEPackage("myecore"));
  }
}
