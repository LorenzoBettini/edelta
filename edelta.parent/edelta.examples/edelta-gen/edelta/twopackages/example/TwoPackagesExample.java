package edelta.twopackages.example;

import edelta.lib.EdeltaRuntime;
import edelta.lib.EdeltaDefaultRuntime;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;

@SuppressWarnings("all")
public class TwoPackagesExample extends EdeltaDefaultRuntime {
  public TwoPackagesExample() {
    
  }
  
  public TwoPackagesExample(final EdeltaRuntime other) {
    super(other);
  }
  
  public void aTest1(final EPackage it) {
    EReference _eOpposite = getEReference("person", "Person", "works").getEOpposite();
    _eOpposite.setName("renamedPersons");
  }
  
  public void aTest2(final EPackage it) {
    EReference _eOpposite = getEReference("workplace", "WorkPlace", "renamedPersons").getEOpposite();
    _eOpposite.setName("renamedWorks");
  }
  
  @Override
  public void performSanityChecks() throws Exception {
    ensureEPackageIsLoaded("person");
    ensureEPackageIsLoaded("workplace");
  }
  
  @Override
  protected void doExecute() throws Exception {
    aTest1(getEPackage("person"));
    aTest2(getEPackage("workplace"));
  }
}
