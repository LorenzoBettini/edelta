package edelta.changeupperbound.example;

import edelta.lib.EdeltaDefaultRuntime;
import edelta.lib.EdeltaRuntime;
import edelta.refactorings.lib.EdeltaRefactoringsWithPrompt;
import org.eclipse.emf.ecore.EPackage;

@SuppressWarnings("all")
public class PersonListChangeUpperBoundExample extends EdeltaDefaultRuntime {
  private EdeltaRefactoringsWithPrompt refactorings;

  public PersonListChangeUpperBoundExample(final EdeltaRuntime other) {
    super(other);
    refactorings = new EdeltaRefactoringsWithPrompt(this);
  }

  public void changeAddressUpperBound(final EPackage it) {
    this.refactorings.changeUpperBoundInteractive(getEReference("PersonListForChangeUpperBound", "Person", "workAddress"), 2);
  }

  @Override
  public void performSanityChecks() throws Exception {
    ensureEPackageIsLoaded("PersonListForChangeUpperBound");
    ensureEPackageIsLoaded("ecore");
  }

  @Override
  protected void doExecute() throws Exception {
    changeAddressUpperBound(getEPackage("PersonListForChangeUpperBound"));
  }
}
