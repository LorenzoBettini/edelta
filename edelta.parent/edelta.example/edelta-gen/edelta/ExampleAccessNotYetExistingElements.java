package edelta;

import edelta.lib.AbstractEdelta;
import edelta.lib.EdeltaUtils;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EPackage;

@SuppressWarnings("all")
public class ExampleAccessNotYetExistingElements extends AbstractEdelta {
  public ExampleAccessNotYetExistingElements() {
    
  }
  
  public ExampleAccessNotYetExistingElements(final AbstractEdelta other) {
    super(other);
  }
  
  public void creation(final EPackage it) {
    EdeltaUtils.addNewEClass(it, "NewClass");
    EList<EClass> _eSuperTypes = getEClass("myecore", "NewClass").getESuperTypes();
    _eSuperTypes.add(getEClass("myecore", "MyEClass"));
  }
  
  public void anotherCreation(final EPackage it) {
    EdeltaUtils.addNewEClass(it, "AnotherNewClass");
    EList<EClass> _eSuperTypes = getEClass("myecore", "AnotherNewClass").getESuperTypes();
    _eSuperTypes.add(getEClass("myecore", "MyEClass"));
  }
  
  @Override
  public void performSanityChecks() throws Exception {
    ensureEPackageIsLoaded("ecore");
    ensureEPackageIsLoaded("myecore");
  }
  
  @Override
  protected void doExecute() throws Exception {
    creation(getEPackage("myecore"));
    anotherCreation(getEPackage("myecore"));
  }
}
