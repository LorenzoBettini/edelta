package edelta.personlist.example;

import edelta.lib.AbstractEdelta;
import edelta.lib.EdeltaDefaultRuntime;
import edelta.refactorings.lib.EdeltaRefactorings;
import java.util.Collections;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.xtext.xbase.lib.CollectionLiterals;
import org.eclipse.xtext.xbase.lib.Extension;
import org.eclipse.xtext.xbase.lib.ObjectExtensions;
import org.eclipse.xtext.xbase.lib.Procedures.Procedure1;

@SuppressWarnings("all")
public class PersonListExample extends EdeltaDefaultRuntime {
  @Extension
  private EdeltaRefactorings refactorings;
  
  public PersonListExample() {
    refactorings = new EdeltaRefactorings(this);
  }
  
  public PersonListExample(final AbstractEdelta other) {
    super(other);
    refactorings = new EdeltaRefactorings(other);
  }
  
  public void improvePerson(final EPackage it) {
    this.refactorings.enumToSubclasses(getEAttribute("PersonList", "Person", "gender"));
    this.refactorings.mergeFeatures("name", 
      Collections.<EStructuralFeature>unmodifiableList(CollectionLiterals.<EStructuralFeature>newArrayList(getEAttribute("PersonList", "Person", "firstname"), getEAttribute("PersonList", "Person", "lastname"))));
  }
  
  public void introducePlace(final EPackage it) {
    this.refactorings.extractSuperclass("Place", 
      Collections.<EStructuralFeature>unmodifiableList(CollectionLiterals.<EStructuralFeature>newArrayList(getEAttribute("PersonList", "LivingPlace", "address"), getEAttribute("PersonList", "WorkPlace", "address"))));
  }
  
  public void introduceWorkingPosition(final EPackage it) {
    EClass _referenceToClass = this.refactorings.referenceToClass("WorkingPosition", getEReference("PersonList", "Person", "works"));
    final Procedure1<EClass> _function = (EClass it_1) -> {
      this.stdLib.addNewEAttribute(it_1, "description", getEDataType("ecore", "EString"));
    };
    ObjectExtensions.<EClass>operator_doubleArrow(_referenceToClass, _function);
    getEReference("PersonList", "WorkPlace", "persons").setName("position");
  }
  
  public void improveList(final EPackage it) {
    this.refactorings.mergeFeatures("places", 
      getEClass("PersonList", "Place"), 
      Collections.<EStructuralFeature>unmodifiableList(CollectionLiterals.<EStructuralFeature>newArrayList(getEReference("PersonList", "List", "wplaces"), getEReference("PersonList", "List", "lplaces"))));
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
