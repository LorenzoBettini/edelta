package edelta;

import edelta.lib.EdeltaDefaultRuntime;
import edelta.lib.EdeltaRuntime;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EPackage;

@SuppressWarnings("all")
public class ExampleContentAssist extends EdeltaDefaultRuntime {
  public ExampleContentAssist(final EdeltaRuntime other) {
    super(other);
  }

  public void SomeChanges(final EPackage it) {
    getEClass("myecore", "MyEClass");
    getEAttribute("myecore", "MyEClass", "astring");
    getEEnum("myecore", "MyEEnum");
    this.stdLib.addNewEClass(it, "NewClass");
    getEClass("myecore", "NewClass");
    EList<EClassifier> _eClassifiers = it.getEClassifiers();
    _eClassifiers.remove(getEClass("myecore", "MyEClass"));
    getEClass("myecore", "NewClass");
    getEEnum("myecore", "MyEEnum");
    this.stdLib.addNewEEnumLiteral(getEEnum("myecore", "MyEEnum"), "second");
    getEEnum("myecore", "MyEEnum").setName("RenamedEnum");
    getEEnum("myecore", "RenamedEnum");
  }

  @Override
  public void performSanityChecks() throws Exception {
    ensureEPackageIsLoaded("ecore");
    ensureEPackageIsLoaded("myecore");
  }

  @Override
  protected void doExecute() throws Exception {
    SomeChanges(getEPackage("myecore"));
  }
}
