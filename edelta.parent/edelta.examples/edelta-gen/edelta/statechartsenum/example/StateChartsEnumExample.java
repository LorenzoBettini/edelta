package edelta.statechartsenum.example;

import edelta.lib.EdeltaDefaultRuntime;
import edelta.lib.EdeltaRuntime;
import edelta.refactorings.lib.EdeltaRefactorings;
import org.eclipse.emf.ecore.EPackage;

@SuppressWarnings("all")
public class StateChartsEnumExample extends EdeltaDefaultRuntime {
  private EdeltaRefactorings refactorings;
  
  public StateChartsEnumExample(final EdeltaRuntime other) {
    super(other);
    refactorings = new EdeltaRefactorings(this);
  }
  
  public void introduceNodeSubclasses(final EPackage it) {
    this.refactorings.enumToSubclasses(getEAttribute("statechartsenum", "Node", "type"));
    getEClass("statechartsenum", "Normal").setName("State");
    getEClass("statechartsenum", "Initial").setName("InitialState");
    getEClass("statechartsenum", "Final").setName("FinalState");
  }
  
  @Override
  public void performSanityChecks() throws Exception {
    ensureEPackageIsLoaded("statechartsenum");
  }
  
  @Override
  protected void doExecute() throws Exception {
    introduceNodeSubclasses(getEPackage("statechartsenum"));
  }
}
