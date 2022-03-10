package edelta.introducingdep.example;

import edelta.lib.EdeltaDefaultRuntime;
import edelta.lib.EdeltaRuntime;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EClass;

@SuppressWarnings("all")
public class IntroducingDepOpExample extends EdeltaDefaultRuntime {
  public IntroducingDepOpExample(final EdeltaRuntime other) {
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
