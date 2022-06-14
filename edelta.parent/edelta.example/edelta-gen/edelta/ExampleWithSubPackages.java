package edelta;

import edelta.lib.EdeltaDefaultRuntime;
import edelta.lib.EdeltaRuntime;
import java.util.function.Consumer;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EPackage;

@SuppressWarnings("all")
public class ExampleWithSubPackages extends EdeltaDefaultRuntime {
  public ExampleWithSubPackages(final EdeltaRuntime other) {
    super(other);
  }

  public void SomeChanges(final EPackage it) {
    final Consumer<EClass> _function = (EClass it_1) -> {
      this.stdLib.addNewEReference(it_1, "refToMainPackageClass", 
        getEClass("mainpackage", "MyClass"));
    };
    this.stdLib.addNewEClass(getEPackage("mainpackage.subpackage"), "AddedToSubPackage", _function);
  }

  @Override
  public void performSanityChecks() throws Exception {
    ensureEPackageIsLoaded("ecore");
    ensureEPackageIsLoaded("mainpackage");
  }

  @Override
  protected void doExecute() throws Exception {
    SomeChanges(getEPackage("mainpackage"));
  }
}
