package com.example1;

import edelta.lib.EdeltaDefaultRuntime;
import edelta.lib.EdeltaEngine;
import edelta.lib.EdeltaRuntime;
import edelta.lib.annotation.EdeltaGenerated;
import java.util.List;
import java.util.function.Consumer;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;

@SuppressWarnings("all")
public class ExampleRunnable extends EdeltaDefaultRuntime {
  public ExampleRunnable(final EdeltaRuntime other) {
    super(other);
  }

  public void someModifications(final EPackage it) {
    final Consumer<EClass> _function = (EClass it_1) -> {
      final Consumer<EReference> _function_1 = (EReference it_2) -> {
        it_2.setUpperBound((-1));
        it_2.setContainment(true);
        it_2.setLowerBound(0);
      };
      this.stdLib.addNewEReference(it_1, "myReference", getEClass("myecore1", "MyEClass"), _function_1);
    };
    this.stdLib.addNewEClass(it, "NewClass", _function);
  }

  @Override
  public void performSanityChecks() throws Exception {
    ensureEPackageIsLoadedByNsURI("myecore1", "http://www.eclipse.org/emf/2002/Myecore1");
  }

  @Override
  protected void doExecute() throws Exception {
    someModifications(getEPackage("myecore1"));
    getEPackage("myecore1").setNsURI("http://www.eclipse.org/emf/2002/Myecore1/v2");
  }

  @Override
  public List<String> getMigratedNsURIs() {
    return List.of(
      "http://www.eclipse.org/emf/2002/Myecore1"
    );
  }

  @Override
  public List<String> getMigratedEcorePaths() {
    return List.of(
      "/My1.ecore"
    );
  }

  @EdeltaGenerated
  public static void main(final String[] args) throws Exception {
    var engine = new EdeltaEngine(ExampleRunnable::new);
    engine.loadEcoreFile("My1.ecore",
      ExampleRunnable.class.getResourceAsStream("/My1.ecore"));
    engine.execute();
    engine.save("modified");
  }
}
