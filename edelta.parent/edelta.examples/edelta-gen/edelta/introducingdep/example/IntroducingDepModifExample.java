package edelta.introducingdep.example;

import edelta.lib.EdeltaDefaultRuntime;
import edelta.lib.EdeltaRuntime;
import java.util.function.Consumer;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.xtext.xbase.lib.Extension;
import org.eclipse.xtext.xbase.lib.IterableExtensions;

@SuppressWarnings("all")
public class IntroducingDepModifExample extends EdeltaDefaultRuntime {
  @Extension
  private IntroducingDepOpExample operations;

  public IntroducingDepModifExample(final EdeltaRuntime other) {
    super(other);
    operations = new IntroducingDepOpExample(this);
  }

  public void aModificationTest(final EPackage it) {
    this.operations.setBaseClass(getEClass("anothersimple", "AnotherSimpleClass"));
    final EClass referenceToSuperClass = IterableExtensions.<EClass>head(getEClass("anothersimple", "AnotherSimpleClass").getESuperTypes());
    this.stdLib.addNewEReference(getEClass("anothersimple", "AnotherSimpleClass"), 
      "aReferenceToSimpleClass", referenceToSuperClass);
    final Consumer<EReference> _function = (EReference it_1) -> {
      it_1.setEOpposite(getEReference("anothersimple", "AnotherSimpleClass", "aReferenceToSimpleClass"));
      getEReference("anothersimple", "AnotherSimpleClass", "aReferenceToSimpleClass").setEOpposite(it_1);
    };
    this.stdLib.addNewEReference(referenceToSuperClass, "aReferenceToAnotherSimpleClass", getEClass("anothersimple", "AnotherSimpleClass"), _function);
  }

  @Override
  public void performSanityChecks() throws Exception {
    ensureEPackageIsLoaded("anothersimple");
  }

  @Override
  protected void doExecute() throws Exception {
    aModificationTest(getEPackage("anothersimple"));
  }
}
