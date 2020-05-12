package edelta;

import edelta.lib.AbstractEdelta;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EPackage;

@SuppressWarnings("all")
public class ExampleAccessStaleElements extends AbstractEdelta {
  public ExampleAccessStaleElements() {
    
  }
  
  public ExampleAccessStaleElements(final AbstractEdelta other) {
    super(other);
  }
  
  public void creation(final EPackage it) {
    this.lib.addNewEClass(it, "NewClass");
  }
  
  public void renaming(final EPackage it) {
    getEClass("myecore", "NewClass").setName("Renamed");
  }
  
  public void remove(final EPackage it) {
    EList<EClassifier> _eClassifiers = it.getEClassifiers();
    _eClassifiers.remove(getEClass("myecore", "MyEClass"));
  }
  
  public void accessing(final EPackage it) {
  }
  
  @Override
  public void performSanityChecks() throws Exception {
    ensureEPackageIsLoaded("ecore");
    ensureEPackageIsLoaded("myecore");
  }
  
  @Override
  protected void doExecute() throws Exception {
    creation(getEPackage("myecore"));
    renaming(getEPackage("myecore"));
    remove(getEPackage("myecore"));
    accessing(getEPackage("myecore"));
  }
}
