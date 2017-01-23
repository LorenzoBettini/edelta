package com.example;

import edelta.lib.AbstractEdelta;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EEnumLiteral;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.EcoreFactory;
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
  public void performSanityChecks() throws Exception {
    ensureEPackageIsLoaded("myecore");
    ensureEPackageIsLoaded("ecore");
  }
  
  @Override
  protected void doExecute() throws Exception {
    final EPackage p = getEClass("myecore", "MyEClass").getEPackage();
    EList<EClassifier> _eClassifiers = p.getEClassifiers();
    EClass _createClass = this.createClass("NewClass");
    final Procedure1<EClass> _function = (EClass it) -> {
      EList<EStructuralFeature> _eStructuralFeatures = it.getEStructuralFeatures();
      EAttribute _newEAttribute = this.lib.newEAttribute("myStringAttribute");
      final Procedure1<EAttribute> _function_1 = (EAttribute it_1) -> {
        it_1.setEType(getEDataType("ecore", "EString"));
      };
      EAttribute _doubleArrow = ObjectExtensions.<EAttribute>operator_doubleArrow(_newEAttribute, _function_1);
      _eStructuralFeatures.add(_doubleArrow);
      EList<EStructuralFeature> _eStructuralFeatures_1 = it.getEStructuralFeatures();
      EReference _newEReference = this.lib.newEReference("myReference");
      final Procedure1<EReference> _function_2 = (EReference it_1) -> {
        it_1.setEType(getEClass("myecore", "MyEClass"));
        it_1.setUpperBound((-1));
        it_1.setContainment(true);
        it_1.setLowerBound(0);
      };
      EReference _doubleArrow_1 = ObjectExtensions.<EReference>operator_doubleArrow(_newEReference, _function_2);
      _eStructuralFeatures_1.add(_doubleArrow_1);
    };
    EClass _doubleArrow = ObjectExtensions.<EClass>operator_doubleArrow(_createClass, _function);
    _eClassifiers.add(_doubleArrow);
    createEClass("myecore", "MyNewClass", null);
    createEClass("myecore", "MyDerivedNewClass", createList(this::_createEClass_MyDerivedNewClass_in_myecore));
    getEAttribute("myecore", "MyDerivedNewClass", "myNewAttribute");
    EList<EEnumLiteral> _eLiterals = getEEnum("myecore", "MyENum").getELiterals();
    EEnumLiteral _createEEnumLiteral = EcoreFactory.eINSTANCE.createEEnumLiteral();
    final Procedure1<EEnumLiteral> _function_1 = (EEnumLiteral it) -> {
      it.setName("AnotherEnumLiteral");
      it.setValue(3);
    };
    EEnumLiteral _doubleArrow_1 = ObjectExtensions.<EEnumLiteral>operator_doubleArrow(_createEEnumLiteral, _function_1);
    _eLiterals.add(_doubleArrow_1);
    createEClass("myecore", "MyOtherNewClass", null);
  }
  
  public void _createEClass_MyDerivedNewClass_in_myecore(final EClass it) {
    {
      EList<EClass> _eSuperTypes = it.getESuperTypes();
      _eSuperTypes.add(getEClass("myecore", "MyNewClass"));
      EList<EClass> _eSuperTypes_1 = it.getESuperTypes();
      _eSuperTypes_1.add(getEClass("myecore", "MyOtherNewClass"));
      createEAttribute(it, "myNewAttribute", createList(this::_createEAttribute_myNewAttribute_in_createEClass_MyDerivedNewClass_in_myecore));
    }
  }
  
  public void _createEAttribute_myNewAttribute_in_createEClass_MyDerivedNewClass_in_myecore(final EAttribute it) {
    {
      it.setEType(getEDataType("ecore", "EInt"));
      it.setUpperBound((-1));
    }
  }
}
