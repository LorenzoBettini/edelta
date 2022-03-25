package edelta.stdlib.examples;

import edelta.lib.EdeltaDefaultRuntime;
import edelta.lib.EdeltaEcoreUtil;
import edelta.lib.EdeltaModelMigrator;
import edelta.lib.EdeltaRuntime;
import java.util.function.Consumer;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;

@SuppressWarnings("all")
public class ChangeReferenceTypeExample extends EdeltaDefaultRuntime {
  public ChangeReferenceTypeExample(final EdeltaRuntime other) {
    super(other);
  }
  
  public void exampleOfChangeReferenceType(final EPackage it) {
    EReference reference = getEReference("PersonListForChangeType", "Person", "firstName");
    final EClass nameElement = getEClass("PersonListForChangeType", "NameElement");
    final EClass otherNameElement = this.stdLib.addNewEClassAsSibling(nameElement, "OtherNameElement");
    final EAttribute nameElementFeature = getEAttribute("PersonListForChangeType", "NameElement", "nameElementValue");
    final EStructuralFeature otherNameElementFeature = this.stdLib.copyToAs(nameElementFeature, otherNameElement, 
      "otherNameElementValue");
    final EdeltaModelMigrator.EObjectFunction _function = (EObject oldReferredObject) -> {
      final Consumer<EObject> _function_1 = (EObject newReferredObject) -> {
        newReferredObject.eSet(otherNameElementFeature, 
          oldReferredObject.eGet(nameElementFeature));
      };
      return EdeltaEcoreUtil.createInstance(otherNameElement, _function_1);
    };
    this.stdLib.changeType(reference, otherNameElement, _function);
  }
  
  @Override
  public void performSanityChecks() throws Exception {
    ensureEPackageIsLoaded("PersonListForChangeType");
  }
  
  @Override
  protected void doExecute() throws Exception {
    exampleOfChangeReferenceType(getEPackage("PersonListForChangeType"));
  }
}
