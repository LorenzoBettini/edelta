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
public class ChangeReferenceTypeManualExample extends EdeltaDefaultRuntime {
  public ChangeReferenceTypeManualExample(final EdeltaRuntime other) {
    super(other);
  }
  
  public void exampleOfChangeReferenceType(final EPackage it) {
    final EAttribute nameElementValue = getEAttribute("PersonListForChangeType", "NameElement", "nameElementValue");
    final EClass otherNameElement = this.stdLib.addNewEClassAsSibling(getEClass("PersonListForChangeType", "NameElement"), "OtherNameElement");
    final EAttribute otherNameElementValue = this.stdLib.<EAttribute>copyToAs(
      getEAttribute("PersonListForChangeType", "NameElement", "nameElementValue"), otherNameElement, 
      "otherNameElementValue");
    final EReference firstName = getEReference("PersonListForChangeType", "Person", "firstName");
    firstName.setEType(otherNameElement);
    final Consumer<EdeltaModelMigrator> _function = (EdeltaModelMigrator it_1) -> {
      final EdeltaModelMigrator.CopyProcedure _function_1 = (EStructuralFeature oldFeature, EObject oldObj, EObject newObj) -> {
        boolean _eIsSet = oldObj.eIsSet(oldFeature);
        boolean _not = (!_eIsSet);
        if (_not) {
          return;
        }
        final EObject oldReferred = it_1.getMigrated(
          EdeltaEcoreUtil.getValueAsEObject(oldObj, oldFeature));
        final Object oldValue = oldReferred.eGet(nameElementValue);
        final Consumer<EObject> _function_2 = (EObject newRef) -> {
          newRef.eSet(otherNameElementValue, oldValue);
        };
        newObj.eSet(firstName, 
          EdeltaEcoreUtil.createInstance(otherNameElement, _function_2));
      };
      it_1.copyRule(
        it_1.<EStructuralFeature>isRelatedTo(firstName), _function_1);
    };
    this.modelMigration(_function);
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
