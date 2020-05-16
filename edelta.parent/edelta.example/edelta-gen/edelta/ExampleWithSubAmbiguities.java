package edelta;

import edelta.lib.AbstractEdelta;
import java.util.function.Consumer;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EPackage;

@SuppressWarnings("all")
public class ExampleWithSubAmbiguities extends AbstractEdelta {
  public ExampleWithSubAmbiguities() {
    
  }
  
  public ExampleWithSubAmbiguities(final AbstractEdelta other) {
    super(other);
  }
  
  public void SomeChanges(final EPackage it) {
    final Consumer<EClass> _function = (EClass it_1) -> {
      this.lib.addNewEReference(it_1, "refToMainPackageClass", 
        getEClass("mainpackage", "MyClass"));
    };
    this.lib.addNewEClass(getEPackage("mainpackage.subpackage"), "AddedToSubPackage", _function);
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
