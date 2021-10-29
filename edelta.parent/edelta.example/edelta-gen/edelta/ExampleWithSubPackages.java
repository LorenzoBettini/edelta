package edelta;

import edelta.lib.AbstractEdelta;
import edelta.lib.EdeltaUtils;
import java.util.function.Consumer;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EPackage;

@SuppressWarnings("all")
public class ExampleWithSubPackages extends AbstractEdelta {
  public ExampleWithSubPackages() {
    
  }
  
  public ExampleWithSubPackages(final AbstractEdelta other) {
    super(other);
  }
  
  public void SomeChanges(final EPackage it) {
    final Consumer<EClass> _function = (EClass it_1) -> {
      EdeltaUtils.addNewEReference(it_1, "refToMainPackageClass", 
        getEClass("mainpackage", "MyClass"));
    };
    EdeltaUtils.addNewEClass(getEPackage("mainpackage.subpackage"), "AddedToSubPackage", _function);
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
