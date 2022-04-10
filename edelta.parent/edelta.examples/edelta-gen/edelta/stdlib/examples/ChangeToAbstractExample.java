package edelta.stdlib.examples;

import edelta.lib.EdeltaDefaultRuntime;
import edelta.lib.EdeltaEcoreUtil;
import edelta.lib.EdeltaModelMigrator;
import edelta.lib.EdeltaRuntime;
import edelta.lib.EdeltaUtils;
import java.util.function.Consumer;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;

@SuppressWarnings("all")
public class ChangeToAbstractExample extends EdeltaDefaultRuntime {
  public ChangeToAbstractExample(final EdeltaRuntime other) {
    super(other);
  }
  
  public void exampleOfChangeToAbstract(final EPackage it) {
    final EClass personClass = getEClass("PersonListForChangeToAbstract", "Person");
    final EClass employee = this.stdLib.addNewSubclass(personClass, "Employee");
    final EClass manager = this.stdLib.addNewSubclass(personClass, "Manager");
    personClass.setAbstract(true);
    final EAttribute managerAttribute = getEAttribute("PersonListForChangeToAbstract", "Person", "manager");
    final Consumer<EdeltaModelMigrator> _function = (EdeltaModelMigrator it_1) -> {
      final EdeltaModelMigrator.EObjectFunction _function_1 = (EObject oldObj) -> {
        final EAttribute managerFeature = it_1.<EAttribute>getOriginal(managerAttribute);
        Object _eGet = oldObj.eGet(managerFeature);
        if ((((Boolean) _eGet)).booleanValue()) {
          return EdeltaEcoreUtil.createInstance(manager);
        }
        return EdeltaEcoreUtil.createInstance(employee);
      };
      it_1.createInstanceRule(
        it_1.<EClass>isRelatedTo(personClass), _function_1);
    };
    this.modelMigration(_function);
    EdeltaUtils.removeElement(managerAttribute);
  }
  
  @Override
  public void performSanityChecks() throws Exception {
    ensureEPackageIsLoaded("PersonListForChangeToAbstract");
  }
  
  @Override
  protected void doExecute() throws Exception {
    exampleOfChangeToAbstract(getEPackage("PersonListForChangeToAbstract"));
  }
}
