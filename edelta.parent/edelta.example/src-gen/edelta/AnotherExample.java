package edelta;

import edelta.Example;
import edelta.lib.AbstractEdelta;

@SuppressWarnings("all")
public class AnotherExample extends AbstractEdelta {
  private Example example;
  
  public AnotherExample() {
    example = new Example(this);
  }
  
  public AnotherExample(final AbstractEdelta other) {
    super(other);
  }
  
  @Override
  public void performSanityChecks() throws Exception {
    ensureEPackageIsLoaded("ecore");
    ensureEPackageIsLoaded("myexample");
    ensureEPackageIsLoaded("myecore");
  }
  
  @Override
  protected void doExecute() throws Exception {
    this.example.createClass("Foo");
  }
}
