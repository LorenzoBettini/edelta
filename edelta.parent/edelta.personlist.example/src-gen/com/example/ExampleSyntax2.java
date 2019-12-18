package com.example;

import edelta.lib.AbstractEdelta;
import gssi.refactorings.MMrefactorings;
import java.util.Collections;
import java.util.function.Consumer;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.xtext.xbase.lib.CollectionLiterals;
import org.eclipse.xtext.xbase.lib.ObjectExtensions;
import org.eclipse.xtext.xbase.lib.Procedures.Procedure1;

@SuppressWarnings("all")
public class ExampleSyntax2 extends AbstractEdelta {
  private MMrefactorings refactorings;
  
  public ExampleSyntax2() {
    refactorings = new MMrefactorings(this);
  }
  
  public ExampleSyntax2(final AbstractEdelta other) {
    super(other);
  }
  
  public void improvePerson(final EPackage it) {
    final Procedure1<EClass> _function = (EClass it_1) -> {
      this.refactorings.introduceSubclasses(it_1, 
        getEAttribute("PersonList", "Person", "gender"), 
        getEEnum("PersonList", "Gender"));
      this.lib.addEAttribute(it_1, 
        this.refactorings.mergeAttributes("name", 
          getEAttribute("PersonList", "Person", "firstname").getEAttributeType(), 
          Collections.<EAttribute>unmodifiableList(CollectionLiterals.<EAttribute>newArrayList(getEAttribute("PersonList", "Person", "firstname"), getEAttribute("PersonList", "Person", "lastname")))));
    };
    ObjectExtensions.<EClass>operator_doubleArrow(
      getEClass("PersonList", "Person"), _function);
  }
  
  public void introducePlace(final EPackage it) {
    final Consumer<EClass> _function = (EClass it_1) -> {
      it_1.setAbstract(true);
      this.refactorings.extractSuperclass(it_1, 
        Collections.<EAttribute>unmodifiableList(CollectionLiterals.<EAttribute>newArrayList(getEAttribute("PersonList", "LivingPlace", "address"), getEAttribute("PersonList", "WorkPlace", "address"))));
    };
    this.lib.addNewEClass(it, "Place", _function);
  }
  
  public void introduceWorkingPosition(final EPackage it) {
    final Consumer<EClass> _function = (EClass it_1) -> {
      EList<EStructuralFeature> _eStructuralFeatures = it_1.getEStructuralFeatures();
      final Consumer<EAttribute> _function_1 = (EAttribute it_2) -> {
        it_2.setEType(getEDataType("ecore", "EString"));
      };
      EAttribute _newEAttribute = this.lib.newEAttribute("description", _function_1);
      _eStructuralFeatures.add(_newEAttribute);
      this.refactorings.extractMetaClass(it_1, 
        getEReference("PersonList", "Person", "works"), "position", "works");
    };
    this.lib.addNewEClass(it, "WorkingPosition", _function);
  }
  
  public void improveList(final EPackage it) {
    this.lib.addEReference(getEClass("PersonList", "List"), 
      this.refactorings.mergeReferences("places", 
        getEClass("PersonList", "Place"), 
        Collections.<EReference>unmodifiableList(CollectionLiterals.<EReference>newArrayList(getEReference("PersonList", "List", "wplaces"), getEReference("PersonList", "List", "lplaces")))));
  }
  
  @Override
  public void performSanityChecks() throws Exception {
    ensureEPackageIsLoaded("PersonList");
    ensureEPackageIsLoaded("ecore");
  }
  
  @Override
  protected void doExecute() throws Exception {
    improvePerson(getEPackage("PersonList"));
    introducePlace(getEPackage("PersonList"));
    introduceWorkingPosition(getEPackage("PersonList"));
    improveList(getEPackage("PersonList"));
  }
}
