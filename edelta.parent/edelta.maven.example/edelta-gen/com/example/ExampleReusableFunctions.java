package com.example;

import edelta.lib.AbstractEdelta;
import edelta.lib.EdeltaLibrary;
import java.util.function.Consumer;
import org.eclipse.emf.ecore.EClass;

@SuppressWarnings("all")
public class ExampleReusableFunctions extends AbstractEdelta {
  public ExampleReusableFunctions() {
    
  }
  
  public ExampleReusableFunctions(final AbstractEdelta other) {
    super(other);
  }
  
  public EClass createClassWithSubClass(final String name, final EClass superClass) {
    final Consumer<EClass> _function = (EClass it) -> {
      EdeltaLibrary.addESuperType(it, superClass);
    };
    return EdeltaLibrary.newEClass(name, _function);
  }
  
  @Override
  public void performSanityChecks() throws Exception {
    ensureEPackageIsLoaded("ecore");
  }
}
