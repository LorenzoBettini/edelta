package edelta;

import edelta.lib.EdeltaLibrary;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.xtext.xbase.lib.Extension;

@SuppressWarnings("all")
public class Example {
  @Extension
  private EdeltaLibrary lib;
  
  public EClass createClass(final String name) {
    return this.lib.newEClass(name);
  }
}
