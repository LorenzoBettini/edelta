package edelta;

import edelta.lib.EdeltaDefaultRuntime;
import edelta.lib.EdeltaRuntime;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.xtext.xbase.lib.ObjectExtensions;
import org.eclipse.xtext.xbase.lib.Procedures.Procedure1;
import org.eclipse.xtext.xbase.lib.StringExtensions;

@SuppressWarnings("all")
public class ExampleWithLiveValidation extends EdeltaDefaultRuntime {
  public ExampleWithLiveValidation(final EdeltaRuntime other) {
    super(other);
  }
  
  public void someChanges(final EPackage it) {
    final Procedure1<EClass> _function = (EClass it_1) -> {
      it_1.setName(StringExtensions.toFirstUpper(it_1.getName()));
    };
    ObjectExtensions.<EClass>operator_doubleArrow(
      getEClass("myecoreforvalidation", "myOtherEClass"), _function);
  }
  
  public void someLiveChecks(final EPackage it) {
    EList<EClassifier> _eClassifiers = it.getEClassifiers();
    for (final EClassifier eClassifier : _eClassifiers) {
      {
        boolean _isLowerCase = Character.isLowerCase(eClassifier.getName().charAt(0));
        if (_isLowerCase) {
          String _name = eClassifier.getName();
          String _plus = ("EClassifier\'s name should start with a capital: " + _name);
          this.showWarning(eClassifier, _plus);
        }
        if ((eClassifier instanceof EClass)) {
          EList<EStructuralFeature> _eStructuralFeatures = ((EClass)eClassifier).getEStructuralFeatures();
          for (final EStructuralFeature feature : _eStructuralFeatures) {
            boolean _isUpperCase = Character.isUpperCase(feature.getName().charAt(0));
            if (_isUpperCase) {
              String _name_1 = feature.getName();
              String _plus_1 = ("EStructuralFeature\'s name should start with a lowercase: " + _name_1);
              this.showWarning(feature, _plus_1);
            }
          }
        }
      }
    }
  }
  
  @Override
  public void performSanityChecks() throws Exception {
    ensureEPackageIsLoaded("ecore");
    ensureEPackageIsLoaded("myecoreforvalidation");
  }
  
  @Override
  protected void doExecute() throws Exception {
    someChanges(getEPackage("myecoreforvalidation"));
    someLiveChecks(getEPackage("myecoreforvalidation"));
  }
}
