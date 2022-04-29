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

@SuppressWarnings("all")
public class ChangeReferenceTypeExample extends EdeltaDefaultRuntime {
  public ChangeReferenceTypeExample(final EdeltaRuntime other) {
    super(other);
  }
  
  public void exampleOfChangeReferenceType(final EPackage it) {
    final EClass otherNameElement = this.stdLib.addNewEClassAsSibling(getEClass("PersonListForChangeType", "NameElement"), "OtherNameElement");
    this.stdLib.<EAttribute>copyToAs(
      getEAttribute("PersonListForChangeType", "NameElement", "nameElementValue"), otherNameElement, 
      "otherNameElementValue");
    final EdeltaModelMigrator.EObjectFunction _function = (EObject oldReferredObject) -> {
      final Consumer<EObject> _function_1 = (EObject newReferredObject) -> {
        EdeltaEcoreUtil.setValueFrom(newReferredObject, getEAttribute("PersonListForChangeType", "OtherNameElement", "otherNameElementValue"), oldReferredObject, getEAttribute("PersonListForChangeType", "NameElement", "nameElementValue"));
      };
      return EdeltaEcoreUtil.createInstance(otherNameElement, _function_1);
    };
    this.stdLib.changeType(getEReference("PersonListForChangeType", "Person", "firstName"), otherNameElement, _function);
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
