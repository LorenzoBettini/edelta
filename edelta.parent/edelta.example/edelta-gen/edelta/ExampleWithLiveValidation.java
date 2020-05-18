package edelta;

import edelta.lib.AbstractEdelta;
import java.util.function.Consumer;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.xtext.xbase.lib.ObjectExtensions;
import org.eclipse.xtext.xbase.lib.Procedures.Procedure1;
import org.eclipse.xtext.xbase.lib.StringExtensions;

@SuppressWarnings("all")
public class ExampleWithLiveValidation extends AbstractEdelta {
  public ExampleWithLiveValidation() {
    
  }
  
  public ExampleWithLiveValidation(final AbstractEdelta other) {
    super(other);
  }
  
  public void SomeChanges(final EPackage it) {
    final Procedure1<EClass> _function = (EClass it_1) -> {
      it_1.setName(StringExtensions.toFirstUpper(it_1.getName()));
    };
    ObjectExtensions.<EClass>operator_doubleArrow(
      getEClass("myecoreforvalidation", "myOtherEClass"), _function);
  }
  
  public void SomeLiveChecks(final EPackage it) {
    final Consumer<EClassifier> _function = (EClassifier eClass) -> {
      boolean _isLowerCase = Character.isLowerCase(eClass.getName().charAt(0));
      if (_isLowerCase) {
        String _name = eClass.getName();
        String _plus = ("EClass name should start with a capital: " + _name);
        this.showWarning(eClass, _plus);
      }
    };
    it.getEClassifiers().forEach(_function);
  }
  
  @Override
  public void performSanityChecks() throws Exception {
    ensureEPackageIsLoaded("ecore");
    ensureEPackageIsLoaded("myecoreforvalidation");
  }
  
  @Override
  protected void doExecute() throws Exception {
    SomeChanges(getEPackage("myecoreforvalidation"));
    SomeLiveChecks(getEPackage("myecoreforvalidation"));
  }
}
