package edelta;

import edelta.lib.AbstractEdelta;
import org.eclipse.emf.ecore.EClass;

@SuppressWarnings("all")
public class Example extends AbstractEdelta {
  public Example() {
    
  }
  
  public Example(final AbstractEdelta other) {
    super(other);
  }
  
  public EClass createClass(final String name) {
    return this.lib.newEClass(name);
  }
  
  @Override
  public void performSanityChecks() throws Exception {
    ensureEPackageIsLoaded("ecore");
    ensureEPackageIsLoaded("myexample");
    ensureEPackageIsLoaded("myecore");
  }
  
  @Override
  protected void doExecute() throws Exception {
    getEClass("myexample", "MyExampleEClass");
    getEClass("myecore", "MyEClass");
    getEClass("ecore", "EAnnotation");
    getEPackage("myexample").getEClassifiers();
  }
}
