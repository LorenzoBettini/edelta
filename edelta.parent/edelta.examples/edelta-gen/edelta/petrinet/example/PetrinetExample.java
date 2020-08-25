package edelta.petrinet.example;

import edelta.lib.AbstractEdelta;
import edelta.lib.EdeltaLibrary;
import edelta.refactorings.lib.EdeltaRefactorings;
import java.util.Collections;
import java.util.function.Consumer;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.xtext.xbase.lib.CollectionLiterals;

@SuppressWarnings("all")
public class PetrinetExample extends AbstractEdelta {
  private EdeltaRefactorings refactorings;
  
  public PetrinetExample() {
    refactorings = new EdeltaRefactorings(this);
  }
  
  public PetrinetExample(final AbstractEdelta other) {
    super(other);
    refactorings = new EdeltaRefactorings(other);
  }
  
  public EAttribute addWeightAttribute(final EClass c) {
    final Consumer<EAttribute> _function = (EAttribute it) -> {
      it.setLowerBound(1);
    };
    return EdeltaLibrary.addNewEAttribute(c, "weight", getEDataType("ecore", "EInt"), _function);
  }
  
  public void modifyNet(final EPackage it) {
    getEClass("petrinet", "Net").setName("Petrinet");
    getEReference("petrinet", "Petrinet", "places").setLowerBound(1);
    getEReference("petrinet", "Petrinet", "transitions").setLowerBound(1);
  }
  
  public void introducePTArc(final EPackage it) {
    final Consumer<EClass> _function = (EClass it_1) -> {
      this.refactorings.extractMetaClass(it_1, getEReference("petrinet", "Place", "dst"), "in", "out");
      this.addWeightAttribute(it_1);
    };
    EdeltaLibrary.addNewEClass(it, "PTArc", _function);
  }
  
  public void introduceTPArc(final EPackage it) {
    final Consumer<EClass> _function = (EClass it_1) -> {
      this.refactorings.extractMetaClass(it_1, getEReference("petrinet", "Transition", "dst"), "in", "out");
      this.addWeightAttribute(it_1);
    };
    EdeltaLibrary.addNewEClass(it, "TPArc", _function);
  }
  
  public void introduceAbstractArc(final EPackage it) {
    final Consumer<EClass> _function = (EClass it_1) -> {
      it_1.setAbstract(true);
      this.refactorings.extractIntoSuperclass(it_1, 
        Collections.<EAttribute>unmodifiableList(CollectionLiterals.<EAttribute>newArrayList(getEAttribute("petrinet", "PTArc", "weight"), getEAttribute("petrinet", "TPArc", "weight"))));
    };
    EdeltaLibrary.addNewEClass(it, "Arc", _function);
  }
  
  @Override
  public void performSanityChecks() throws Exception {
    ensureEPackageIsLoaded("petrinet");
    ensureEPackageIsLoaded("ecore");
  }
  
  @Override
  protected void doExecute() throws Exception {
    modifyNet(getEPackage("petrinet"));
    introducePTArc(getEPackage("petrinet"));
    introduceTPArc(getEPackage("petrinet"));
    introduceAbstractArc(getEPackage("petrinet"));
  }
}
