package com.example;

import edelta.lib.EdeltaDefaultRuntime;
import edelta.lib.EdeltaModelMigrator;
import edelta.lib.EdeltaRuntime;
import java.util.function.Consumer;
import java.util.function.Predicate;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;

@SuppressWarnings("all")
public class Example extends EdeltaDefaultRuntime {
  public Example(final EdeltaRuntime other) {
    super(other);
  }

  /**
   * Reusable function
   */
  public void makeItNotRequired(final EStructuralFeature f) {
    f.setLowerBound(0);
  }

  public void someModifications(final EPackage it) {
    final Consumer<EClass> _function = (EClass it_1) -> {
      this.stdLib.addNewEAttribute(it_1, "myStringAttribute", getEDataType("ecore", "EString"));
      final Consumer<EReference> _function_1 = (EReference it_2) -> {
        it_2.setUpperBound((-1));
        it_2.setContainment(true);
        this.makeItNotRequired(it_2);
      };
      this.stdLib.addNewEReference(it_1, "myReference", getEClass("mypackage", "MyClass"), _function_1);
    };
    this.stdLib.addNewEClass(it, "NewClass", _function);
  }

  public void otherModifications(final EPackage it) {
    getEAttribute("mypackage", "MyClass", "myClassStringAttribute").setName("stringAttribute");
    this.makeItNotRequired(getEAttribute("mypackage", "MyClass", "stringAttribute"));
    final EAttribute stringAttr = getEAttribute("mypackage", "MyClass", "stringAttribute");
    final Consumer<EdeltaModelMigrator> _function = (EdeltaModelMigrator it_1) -> {
      final Predicate<EAttribute> _function_1 = (EAttribute f) -> {
        return it_1.isRelatedTo(f, stringAttr);
      };
      final EdeltaModelMigrator.AttributeTransformer _function_2 = (EAttribute feature, EObject oldVal, Object newVal) -> {
        return newVal.toString().toUpperCase();
      };
      it_1.transformAttributeValueRule(_function_1, _function_2);
    };
    this.modelMigration(_function);
  }

  @Override
  public void performSanityChecks() throws Exception {
    ensureEPackageIsLoaded("mypackage");
    ensureEPackageIsLoaded("ecore");
  }

  @Override
  protected void doExecute() throws Exception {
    someModifications(getEPackage("mypackage"));
    otherModifications(getEPackage("mypackage"));
  }
}
