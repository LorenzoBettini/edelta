package edelta;

import edelta.lib.EdeltaDefaultRuntime;
import edelta.lib.EdeltaRuntime;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;

@SuppressWarnings("all")
public class ExampleAcrossEPackages extends EdeltaDefaultRuntime {
  public ExampleAcrossEPackages() {
    
  }
  
  public ExampleAcrossEPackages(final EdeltaRuntime other) {
    super(other);
  }
  
  public void aTest1(final EPackage it) {
    EReference _eOpposite = getEReference("testecoreforreferences1", "Person", "works").getEOpposite();
    _eOpposite.setName("renamedPersons");
  }
  
  public void aTest2(final EPackage it) {
    EReference _eOpposite = getEReference("testecoreforreferences2", "WorkPlace", "renamedPersons").getEOpposite();
    _eOpposite.setName("renamedWorks");
  }
  
  @Override
  public void performSanityChecks() throws Exception {
    ensureEPackageIsLoaded("testecoreforreferences1");
    ensureEPackageIsLoaded("testecoreforreferences2");
  }
  
  @Override
  protected void doExecute() throws Exception {
    aTest1(getEPackage("testecoreforreferences1"));
    aTest2(getEPackage("testecoreforreferences2"));
  }
}
