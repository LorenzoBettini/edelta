package edelta;

import edelta.lib.AbstractEdelta;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EPackage;

@SuppressWarnings("all")
public class ExampleAccessModifiedElements extends AbstractEdelta {
  public ExampleAccessModifiedElements() {
    
  }
  
  public ExampleAccessModifiedElements(final AbstractEdelta other) {
    super(other);
  }
  
  public void creation(final EPackage it) {
    this.lib.addNewEClass(it, "NewClass");
  }
  
  public void renaming(final EPackage it) {
    getEClass("myecore", "NewClass").setAbstract(true);
    getEClass("myecore", "NewClass").setName("Renamed");
  }
  
  public void remove(final EPackage it) {
    EList<EClassifier> _eClassifiers = it.getEClassifiers();
    _eClassifiers.remove(getEClass("myecore", "MyEClass"));
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
    renaming(getEPackage("myecore"));
    remove(getEPackage("myecore"));
  }
}
