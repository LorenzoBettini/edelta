package edelta;

import edelta.lib.AbstractEdelta;
import edelta.lib.EdeltaLibrary;
import java.util.function.Consumer;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.util.EcoreUtil;

@SuppressWarnings("all")
public class ExampleWithSubAmbiguities extends AbstractEdelta {
  public ExampleWithSubAmbiguities() {
    
  }
  
  public ExampleWithSubAmbiguities(final AbstractEdelta other) {
    super(other);
  }
  
  public void SomeChanges(final EPackage it) {
    final Consumer<EClass> _function = (EClass it_1) -> {
      EdeltaLibrary.addNewEReference(it_1, "refToMainPackageClass", 
        getEClass("mainpackage", "MyClass"));
    };
    EdeltaLibrary.addNewEClass(getEPackage("mainpackage.subpackage"), "AddedToSubPackage", _function);
    EcoreUtil.remove(getEAttribute("mainpackage", "MyClass", "myAttribute"));
    EcoreUtil.remove(getEAttribute("mainpackage.subpackage", "MyClass", "myAttribute"));
    getEAttribute("mainpackage.subpackage.subsubpackage", "MyClass", "myAttribute");
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
