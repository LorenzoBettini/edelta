package edelta;

import edelta.lib.AbstractEdelta;
import org.eclipse.emf.ecore.EClass;

@SuppressWarnings("all")
public class Example extends AbstractEdelta {
  public EClass createClass(final String name) {
    return this.lib.newEClass(name);
  }
  
  @Override
  public void execute() throws Exception {
    getEClass("myexample", "MyExampleEClass");
    getEClass("myecore", "MyEClass");
  }
}
