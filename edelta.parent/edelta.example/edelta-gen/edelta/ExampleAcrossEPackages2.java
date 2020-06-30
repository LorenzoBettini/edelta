package edelta;

import edelta.lib.AbstractEdelta;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;

@SuppressWarnings("all")
public class ExampleAcrossEPackages2 extends AbstractEdelta {
  public ExampleAcrossEPackages2() {
    
  }
  
  public ExampleAcrossEPackages2(final AbstractEdelta other) {
    super(other);
  }
  
  public void aTest1(final EPackage it) {
    EReference _eOpposite = getEReference("testecoreforreferences1", "Person", "works").getEOpposite();
    _eOpposite.setName("renamedPersons");
    EReference _eOpposite_1 = getEReference("testecoreforreferences2", "WorkPlace", "renamedPersons").getEOpposite();
    _eOpposite_1.setName("renamedWorks");
  }
  
  @Override
  public void performSanityChecks() throws Exception {
    ensureEPackageIsLoaded("testecoreforreferences1");
    ensureEPackageIsLoaded("testecoreforreferences2");
  }
  
  @Override
  protected void doExecute() throws Exception {
    aTest1(getEPackage("testecoreforreferences1"));
  }
}
