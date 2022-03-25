package edelta.stdlib.examples;

import edelta.lib.EdeltaDefaultRuntime;
import edelta.lib.EdeltaEcoreUtil;
import edelta.lib.EdeltaModelMigrator;
import edelta.lib.EdeltaRuntime;
import java.util.function.Consumer;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EStructuralFeature;

@SuppressWarnings("all")
public class ChangeReferenceTypeManualExample extends EdeltaDefaultRuntime {
  public ChangeReferenceTypeManualExample(final EdeltaRuntime other) {
    super(other);
  }
  
  public void exampleOfChangeReferenceType(final EPackage it) {
    final EClass otherNameElement = this.stdLib.addNewEClassAsSibling(getEClass("PersonListForChangeType", "NameElement"), "OtherNameElement");
    this.stdLib.copyToAs(
      getEAttribute("PersonListForChangeType", "NameElement", "nameElementValue"), otherNameElement, 
      "otherNameElementValue");
    getEReference("PersonListForChangeType", "Person", "firstName").setEType(otherNameElement);
    final Consumer<EdeltaModelMigrator> _function = (EdeltaModelMigrator it_1) -> {
      final EdeltaModelMigrator.CopyProcedure _function_1 = (EStructuralFeature oldFeature, EObject oldObj, EObject newObj) -> {
        boolean _eIsSet = oldObj.eIsSet(oldFeature);
        boolean _not = (!_eIsSet);
        if (_not) {
          return;
        }
        Object _eGet = oldObj.eGet(oldFeature);
        final EObject oldReferred = it_1.getMigrated(((EObject) _eGet));
        final Object oldValue = oldReferred.eGet(getEAttribute("PersonListForChangeType", "NameElement", "nameElementValue"));
        final Consumer<EObject> _function_2 = (EObject newRef) -> {
          newRef.eSet(getEAttribute("PersonListForChangeType", "OtherNameElement", "otherNameElementValue"), oldValue);
        };
        newObj.eSet(getEReference("PersonListForChangeType", "Person", "firstName"), 
          EdeltaEcoreUtil.createInstance(otherNameElement, _function_2));
      };
      it_1.copyRule(
        it_1.<EStructuralFeature>isRelatedTo(getEReference("PersonListForChangeType", "Person", "firstName")), _function_1);
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
