package edelta;

import edelta.lib.EdeltaDefaultRuntime;
import edelta.lib.EdeltaRuntime;
import java.util.function.Consumer;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EPackage;

@SuppressWarnings("all")
public class ExampleAccessModifiedElements extends EdeltaDefaultRuntime {
  public ExampleAccessModifiedElements(final EdeltaRuntime other) {
    super(other);
  }
  
  public void creation(final EPackage it) {
    this.stdLib.addNewEClass(it, "NewClass");
  }
  
  public void renaming(final EPackage it) {
    getEClass("myecore", "NewClass");
    getEClass("myecore", "NewClass").setName("Renamed");
    getEClass("myecore", "Renamed");
    final Consumer<EPackage> _function = (EPackage it_1) -> {
      this.stdLib.addEClass(it_1, getEClass("myecore", "Renamed"));
    };
    this.stdLib.addNewESubpackage(it, "mysubpackage", 
      "mysubpackage", 
      "http://mysubpackage", _function);
    getEClass("myecore.mysubpackage", "Renamed");
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
