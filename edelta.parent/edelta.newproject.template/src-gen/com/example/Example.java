package com.example;

import edelta.lib.AbstractEdelta;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.xtext.xbase.lib.ObjectExtensions;
import org.eclipse.xtext.xbase.lib.Procedures.Procedure1;

@SuppressWarnings("all")
public class Example extends AbstractEdelta {
  /**
   * Reusable function to create a new EClass with the
   * specified name, setting MyEClass as its superclass
   * @param name
   */
  public EClass createClass(final String name) {
    EClass _newEClass = this.lib.newEClass(name);
    final Procedure1<EClass> _function = (EClass it) -> {
      EList<EClass> _eSuperTypes = it.getESuperTypes();
      _eSuperTypes.add(getEClass("myecore", "MyEClass"));
    };
    return ObjectExtensions.<EClass>operator_doubleArrow(_newEClass, _function);
  }
  
  @Override
  public void execute() throws Exception {
    final EPackage p = getEClass("myecore", "MyEClass").getEPackage();
    EList<EClassifier> _eClassifiers = p.getEClassifiers();
    EClass _createClass = this.createClass("NewClass");
    _eClassifiers.add(_createClass);
  }
}
