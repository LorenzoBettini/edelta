package com.example;

import edelta.lib.AbstractEdelta;
import edelta.lib.EdeltaUtils;
import java.util.function.Consumer;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EEnumLiteral;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EcoreFactory;
import org.eclipse.xtext.xbase.lib.ObjectExtensions;
import org.eclipse.xtext.xbase.lib.Procedures.Procedure1;

@SuppressWarnings("all")
public class Example extends AbstractEdelta {
  public Example() {
    
  }
  
  public Example(final AbstractEdelta other) {
    super(other);
  }
  
  /**
   * Reusable function to create a new EClass with the
   * specified name, setting MyEClass as its superclass
   * @param name
   */
  public EClass myReusableCreateSubclassOfMyEClass(final String name) {
    EClass _newEClass = EdeltaUtils.newEClass(name);
    final Procedure1<EClass> _function = (EClass it) -> {
      EList<EClass> _eSuperTypes = it.getESuperTypes();
      _eSuperTypes.add(getEClass("myecore", "MyEClass"));
    };
    return ObjectExtensions.<EClass>operator_doubleArrow(_newEClass, _function);
  }
  
  public void someModifications(final EPackage it) {
    final Consumer<EClass> _function = (EClass it_1) -> {
      EdeltaUtils.addNewEAttribute(it_1, "myStringAttribute", getEDataType("ecore", "EString"));
      final Consumer<EReference> _function_1 = (EReference it_2) -> {
        it_2.setUpperBound((-1));
        it_2.setContainment(true);
        it_2.setLowerBound(0);
      };
      EdeltaUtils.addNewEReference(it_1, "myReference", getEClass("myecore", "MyEClass"), _function_1);
    };
    EdeltaUtils.addNewEClass(it, "NewClass", _function);
    EList<EEnumLiteral> _eLiterals = getEEnum("myecore", "MyENum").getELiterals();
    EEnumLiteral _createEEnumLiteral = EcoreFactory.eINSTANCE.createEEnumLiteral();
    final Procedure1<EEnumLiteral> _function_1 = (EEnumLiteral it_1) -> {
      it_1.setName("ANewEnumLiteral");
      it_1.setValue(3);
    };
    EEnumLiteral _doubleArrow = ObjectExtensions.<EEnumLiteral>operator_doubleArrow(_createEEnumLiteral, _function_1);
    _eLiterals.add(_doubleArrow);
    final Consumer<EEnumLiteral> _function_2 = (EEnumLiteral it_1) -> {
      it_1.setValue(4);
    };
    EdeltaUtils.addNewEEnumLiteral(getEEnum("myecore", "MyENum"), "AnotherNewEnumLiteral", _function_2);
  }
  
  public void otherModifications(final EPackage it) {
    EdeltaUtils.addEClass(it, this.myReusableCreateSubclassOfMyEClass("ASubclassOfMyEClass"));
    EClass _myReusableCreateSubclassOfMyEClass = this.myReusableCreateSubclassOfMyEClass("AnotherSubclassOfMyEClass");
    final Procedure1<EClass> _function = (EClass it_1) -> {
      EList<EClass> _eSuperTypes = it_1.getESuperTypes();
      _eSuperTypes.add(getEClass("myecore", "NewClass"));
    };
    EClass _doubleArrow = ObjectExtensions.<EClass>operator_doubleArrow(_myReusableCreateSubclassOfMyEClass, _function);
    EdeltaUtils.addEClass(it, _doubleArrow);
    getEClass("myecore", "MyOtherEClass").setName("RenamedClass");
    EdeltaUtils.addNewEAttribute(getEClass("myecore", "RenamedClass"), "addedNow", getEDataType("ecore", "EInt"));
  }
  
  @Override
  public void performSanityChecks() throws Exception {
    ensureEPackageIsLoaded("myecore");
    ensureEPackageIsLoaded("ecore");
  }
  
  @Override
  protected void doExecute() throws Exception {
    someModifications(getEPackage("myecore"));
    otherModifications(getEPackage("myecore"));
  }
}
