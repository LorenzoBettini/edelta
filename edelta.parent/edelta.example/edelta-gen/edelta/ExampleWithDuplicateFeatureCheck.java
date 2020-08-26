package edelta;

import edelta.lib.AbstractEdelta;
import edelta.lib.EdeltaLibrary;
import edelta.refactorings.lib.EdeltaBadSmellsChecker;
import java.util.function.Consumer;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.xtext.xbase.lib.ObjectExtensions;
import org.eclipse.xtext.xbase.lib.Procedures.Procedure1;

@SuppressWarnings("all")
public class ExampleWithDuplicateFeatureCheck extends AbstractEdelta {
  private EdeltaBadSmellsChecker checker;
  
  public ExampleWithDuplicateFeatureCheck() {
    checker = new EdeltaBadSmellsChecker(this);
  }
  
  public ExampleWithDuplicateFeatureCheck(final AbstractEdelta other) {
    super(other);
    checker = new EdeltaBadSmellsChecker(other);
  }
  
  public void someChanges(final EPackage it) {
    getEAttribute("myecoreforvalidation", "MyEClass", "astring").setLowerBound(1);
    final Procedure1<EClass> _function = (EClass it_1) -> {
      final Consumer<EAttribute> _function_1 = (EAttribute it_2) -> {
        it_2.setLowerBound(1);
      };
      EdeltaLibrary.addNewEAttribute(it_1, "afield", getEDataType("ecore", "EString"), _function_1);
    };
    ObjectExtensions.<EClass>operator_doubleArrow(
      getEClass("myecoreforvalidation", "MyEClass"), _function);
    final Procedure1<EClass> _function_1 = (EClass it_1) -> {
      EdeltaLibrary.addNewEAttribute(it_1, "afield", getEDataType("ecore", "EString"));
    };
    ObjectExtensions.<EClass>operator_doubleArrow(
      getEClass("myecoreforvalidation", "myOtherEClass"), _function_1);
  }
  
  public void checks(final EPackage it) {
    this.checker.checkDuplicateFeatures(it);
  }
  
  @Override
  public void performSanityChecks() throws Exception {
    ensureEPackageIsLoaded("ecore");
    ensureEPackageIsLoaded("myecoreforvalidation");
  }
  
  @Override
  protected void doExecute() throws Exception {
    someChanges(getEPackage("myecoreforvalidation"));
    checks(getEPackage("myecoreforvalidation"));
  }
}
