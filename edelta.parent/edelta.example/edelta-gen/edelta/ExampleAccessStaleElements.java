package edelta;

import edelta.lib.EdeltaDefaultRuntime;
import edelta.lib.EdeltaRuntime;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EPackage;

@SuppressWarnings("all")
public class ExampleAccessStaleElements extends EdeltaDefaultRuntime {
  public ExampleAccessStaleElements(final EdeltaRuntime other) {
    super(other);
  }
  
  public void creation(final EPackage it) {
    this.stdLib.addNewEClass(it, "NewClass");
    this.stdLib.addNewEClass(it, "AnotherNewClass");
  }
  
  public void renaming(final EPackage it) {
    getEClass("myecore", "NewClass").setName("Renamed");
  }
  
  public void remove(final EPackage it) {
    EList<EClassifier> _eClassifiers = it.getEClassifiers();
    _eClassifiers.remove(getEClass("myecore", "MyEClass"));
    EList<EClassifier> _eClassifiers_1 = it.getEClassifiers();
    _eClassifiers_1.remove(getEClass("myecore", "AnotherNewClass"));
  }
  
  public void accessing(final EPackage it) {
  }
  
  public void renaming2(final EPackage it) {
    getEAttribute("mainpackage.subpackage.subsubpackage", "MyClass", "myAttribute").setName("Renamed");
  }
  
  public void accessing2(final EPackage it) {
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
    accessing(getEPackage("myecore"));
    renaming2(getEPackage("mainpackage"));
    accessing2(getEPackage("mainpackage"));
  }
}
