package edelta.personlist.example;

import edelta.lib.AbstractEdelta;
import edelta.refactorings.lib.EdeltaRefactorings;
import java.util.Collections;
import java.util.function.Consumer;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.xtext.xbase.lib.CollectionLiterals;
import org.eclipse.xtext.xbase.lib.Extension;
import org.eclipse.xtext.xbase.lib.ObjectExtensions;
import org.eclipse.xtext.xbase.lib.Procedures.Procedure1;

@SuppressWarnings("all")
public class PersonListExample extends AbstractEdelta {
  @Extension
  private EdeltaRefactorings refactorings;
  
  public PersonListExample() {
    refactorings = new EdeltaRefactorings(this);
  }
  
  public PersonListExample(final AbstractEdelta other) {
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
      this.refactorings.extractIntoSuperclass(it_1, Collections.<EAttribute>unmodifiableList(CollectionLiterals.<EAttribute>newArrayList(getEAttribute("PersonList", "LivingPlace", "address"), getEAttribute("PersonList", "WorkPlace", "address"))));
    };
    this.lib.addNewEClass(it, "Place", _function);
  }
  
  public void introduceWorkingPosition(final EPackage it) {
    final Consumer<EClass> _function = (EClass it_1) -> {
      this.lib.addNewEAttribute(it_1, "description", getEDataType("ecore", "EString"));
      this.refactorings.extractMetaClass(it_1, getEReference("PersonList", "Person", "works"), "position", "works");
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
