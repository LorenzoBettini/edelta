package com.example;

import edelta.lib.AbstractEdelta;
import gssi.refactorings.MMrefactorings;
import java.util.Collections;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.xtext.xbase.lib.CollectionLiterals;

@SuppressWarnings("all")
public class ExampleSyntax1 extends AbstractEdelta {
  private MMrefactorings refactorings;
  
  public ExampleSyntax1() {
    refactorings = new MMrefactorings(this);
  }
  
  public ExampleSyntax1(final AbstractEdelta other) {
    super(other);
  }
  
  @Override
  public void performSanityChecks() throws Exception {
    ensureEPackageIsLoaded("PersonList");
    ensureEPackageIsLoaded("ecore");
  }
  
  @Override
  protected void doExecute() throws Exception {
    changeEClass("PersonList", "Person", createList(this::_changeEClass_Person_in_PersonList));
    createEClass("PersonList", "Place", createList(this::_createEClass_Place_in_PersonList));
    createEClass("PersonList", "WorkingPosition", createList(this::_createEClass_WorkingPosition_in_PersonList));
    changeEClass("PersonList", "List", createList(this::_changeEClass_List_in_PersonList));
  }
  
  public void _changeEClass_Person_in_PersonList(final EClass it) {
    {
      this.refactorings.introduceSubclasses(it, 
        getEAttribute("PersonList", "Person", "gender"), 
        getEEnum("PersonList", "Gender"));
      EList<EStructuralFeature> _eStructuralFeatures = it.getEStructuralFeatures();
      EAttribute _mergeAttributes = this.refactorings.mergeAttributes("name", 
        getEAttribute("PersonList", "Person", "firstname").getEAttributeType(), 
        Collections.<EAttribute>unmodifiableList(CollectionLiterals.<EAttribute>newArrayList(getEAttribute("PersonList", "Person", "firstname"), getEAttribute("PersonList", "Person", "lastname"))));
      _eStructuralFeatures.add(_mergeAttributes);
    }
  }
  
  public void _createEClass_Place_in_PersonList(final EClass it) {
    {
      it.setAbstract(true);
      this.refactorings.extractSuperclass(it, 
        Collections.<EAttribute>unmodifiableList(CollectionLiterals.<EAttribute>newArrayList(getEAttribute("PersonList", "LivingPlace", "address"), getEAttribute("PersonList", "WorkPlace", "address"))));
    }
  }
  
  public void _createEClass_WorkingPosition_in_PersonList(final EClass it) {
    {
      createEAttribute(it, "description", 
        createList(
          a -> a.setEType(getEDataType("ecore", "EString")),
          this::_createEAttribute_description_in_createEClass_WorkingPosition_in_PersonList
        )
      );
      this.refactorings.extractMetaClass(it, getEReference("PersonList", "Person", "works"), "position", "works");
    }
  }
  
  public void _createEAttribute_description_in_createEClass_WorkingPosition_in_PersonList(final EAttribute it) {
  }
  
  public void _changeEClass_List_in_PersonList(final EClass it) {
    EList<EStructuralFeature> _eStructuralFeatures = it.getEStructuralFeatures();
    EReference _mergeReferences = this.refactorings.mergeReferences("places", 
      getEClass("PersonList", "Place"), 
      Collections.<EReference>unmodifiableList(CollectionLiterals.<EReference>newArrayList(getEReference("PersonList", "List", "wplaces"), getEReference("PersonList", "List", "lplaces"))));
    _eStructuralFeatures.add(_mergeReferences);
  }
}
