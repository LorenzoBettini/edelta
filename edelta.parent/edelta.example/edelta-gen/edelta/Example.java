package edelta;

import edelta.lib.EdeltaDefaultRuntime;
import edelta.lib.EdeltaRuntime;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.xtext.xbase.lib.Extension;
import org.eclipse.xtext.xbase.lib.ObjectExtensions;
import org.eclipse.xtext.xbase.lib.Procedures.Procedure1;

@SuppressWarnings("all")
public class Example extends EdeltaDefaultRuntime {
  @Extension
  private ExampleReusableFunctions myfunctions;

  public Example(final EdeltaRuntime other) {
    super(other);
    myfunctions = new ExampleReusableFunctions(this);
  }

  public void SomeChanges(final EPackage it) {
    EClass _createANewClassInMyEcore = this.myfunctions.createANewClassInMyEcore("ANewClass");
    final Procedure1<EClass> _function = (EClass it_1) -> {
      it_1.setAbstract(false);
    };
    ObjectExtensions.<EClass>operator_doubleArrow(_createANewClassInMyEcore, _function);
  }

  @Override
  public void performSanityChecks() throws Exception {
    ensureEPackageIsLoaded("ecore");
    ensureEPackageIsLoaded("myexample");
    ensureEPackageIsLoaded("myecore");
  }

  @Override
  protected void doExecute() throws Exception {
    SomeChanges(getEPackage("myecore"));
  }
}
