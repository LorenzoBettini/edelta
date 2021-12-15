package edelta.introducingdep.example;

import edelta.lib.AbstractEdelta;
import edelta.lib.EdeltaDefaultRuntime;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EClass;

@SuppressWarnings("all")
public class IntroducingDepOpExample extends EdeltaDefaultRuntime {
  public IntroducingDepOpExample() {
    
  }
  
  public IntroducingDepOpExample(final AbstractEdelta other) {
    super(other);
  }
  
  public void setBaseClass(final EClass c) {
    EList<EClass> _eSuperTypes = c.getESuperTypes();
    _eSuperTypes.add(getEClass("simple", "SimpleClass"));
  }
  
  @Override
  public void performSanityChecks() throws Exception {
    ensureEPackageIsLoaded("simple");
  }
}
