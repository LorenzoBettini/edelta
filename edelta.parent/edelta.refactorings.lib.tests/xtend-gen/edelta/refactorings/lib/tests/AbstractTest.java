package edelta.refactorings.lib.tests;

import com.google.common.base.Objects;
import com.google.common.collect.Iterables;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EEnum;
import org.eclipse.emf.ecore.EEnumLiteral;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.EcoreFactory;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.xtext.xbase.lib.Functions.Function1;
import org.eclipse.xtext.xbase.lib.IterableExtensions;
import org.eclipse.xtext.xbase.lib.ObjectExtensions;
import org.eclipse.xtext.xbase.lib.Procedures.Procedure1;

@SuppressWarnings("all")
public abstract class AbstractTest {
  protected EcoreFactory factory = EcoreFactory.eINSTANCE;
  
  protected EDataType stringDataType = EcorePackage.eINSTANCE.getEString();
  
  protected EDataType intDataType = EcorePackage.eINSTANCE.getEInt();
  
  protected EClass eClassReference = EcorePackage.eINSTANCE.getEClass();
  
  protected static final String MODIFIED = "modified/";
  
  protected static final String TESTECORES = "test-input-models/";
  
  protected static final String EXPECTATIONS = "test-output-expectations/";
  
  protected EClass createEClass(final EPackage epackage, final String name) {
    final EClass c = this.createEClassWithoutPackage(name);
    EList<EClassifier> _eClassifiers = epackage.getEClassifiers();
    _eClassifiers.add(c);
    return c;
  }
  
  protected EClass createEClassWithoutPackage(final String name) {
    EClass _createEClass = this.factory.createEClass();
    final Procedure1<EClass> _function = (EClass it) -> {
      it.setName(name);
    };
    return ObjectExtensions.<EClass>operator_doubleArrow(_createEClass, _function);
  }
  
  protected EEnum createEEnum(final EPackage epackage, final String name) {
    EEnum _createEEnum = this.factory.createEEnum();
    final Procedure1<EEnum> _function = (EEnum it) -> {
      it.setName(name);
    };
    final EEnum e = ObjectExtensions.<EEnum>operator_doubleArrow(_createEEnum, _function);
    EList<EClassifier> _eClassifiers = epackage.getEClassifiers();
    _eClassifiers.add(e);
    return e;
  }
  
  protected EEnumLiteral createEEnumLiteral(final EEnum en, final String name) {
    EEnumLiteral _createEEnumLiteral = this.factory.createEEnumLiteral();
    final Procedure1<EEnumLiteral> _function = (EEnumLiteral it) -> {
      it.setName(name);
    };
    final EEnumLiteral e = ObjectExtensions.<EEnumLiteral>operator_doubleArrow(_createEEnumLiteral, _function);
    EList<EEnumLiteral> _eLiterals = en.getELiterals();
    _eLiterals.add(e);
    return e;
  }
  
  protected EAttribute createEAttribute(final EClass eclass, final String name) {
    EAttribute _createEAttribute = this.factory.createEAttribute();
    final Procedure1<EAttribute> _function = (EAttribute it) -> {
      it.setName(name);
    };
    final EAttribute a = ObjectExtensions.<EAttribute>operator_doubleArrow(_createEAttribute, _function);
    EList<EStructuralFeature> _eStructuralFeatures = eclass.getEStructuralFeatures();
    _eStructuralFeatures.add(a);
    return a;
  }
  
  protected EReference createEReference(final EClass eclass, final String name) {
    EReference _createEReference = this.factory.createEReference();
    final Procedure1<EReference> _function = (EReference it) -> {
      it.setName(name);
    };
    final EReference a = ObjectExtensions.<EReference>operator_doubleArrow(_createEReference, _function);
    EList<EStructuralFeature> _eStructuralFeatures = eclass.getEStructuralFeatures();
    _eStructuralFeatures.add(a);
    return a;
  }
  
  protected Iterable<EClass> EClasses(final EPackage p) {
    return Iterables.<EClass>filter(p.getEClassifiers(), EClass.class);
  }
  
  protected EClassifier findEClassifier(final EPackage p, final String byName) {
    final Function1<EClassifier, Boolean> _function = (EClassifier it) -> {
      String _name = it.getName();
      return Boolean.valueOf(Objects.equal(_name, byName));
    };
    return IterableExtensions.<EClassifier>findFirst(p.getEClassifiers(), _function);
  }
  
  protected EAttribute findEAttribute(final EClass c, final String byName) {
    final Function1<EAttribute, Boolean> _function = (EAttribute it) -> {
      String _name = it.getName();
      return Boolean.valueOf(Objects.equal(_name, byName));
    };
    return IterableExtensions.<EAttribute>findFirst(c.getEAttributes(), _function);
  }
}
