package edelta.petrinet.example;

import edelta.lib.AbstractEdelta;
import edelta.refactorings.lib.EdeltaRefactorings;
import java.util.Collections;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.xtext.xbase.lib.CollectionLiterals;

@SuppressWarnings("all")
public class OldPetrinetExample extends AbstractEdelta {
  private EdeltaRefactorings refactorings;
  
  public OldPetrinetExample() {
    refactorings = new EdeltaRefactorings(this);
  }
  
  public OldPetrinetExample(final AbstractEdelta other) {
    super(other);
  }
  
  @Override
  public void performSanityChecks() throws Exception {
    ensureEPackageIsLoaded("petrinet");
    ensureEPackageIsLoaded("ecore");
  }
  
  @Override
  protected void doExecute() throws Exception {
    changeEClass("petrinet", "Net", 
      createList(
        c -> c.setName("Petrinet"),
        this::_changeEClass_Net_in_petrinet
      )
    );
    createEClass("petrinet", "PTArc", createList(this::_createEClass_PTArc_in_petrinet));
    createEClass("petrinet", "TPArc", createList(this::_createEClass_TPArc_in_petrinet));
    createEClass("petrinet", "Arc", createList(this::_createEClass_Arc_in_petrinet));
  }
  
  public void _changeEClass_Net_in_petrinet(final EClass it) {
    {
      getEReference("petrinet", "Petrinet", "places").setLowerBound(1);
      getEReference("petrinet", "Petrinet", "transitions").setLowerBound(1);
    }
  }
  
  public void _createEClass_PTArc_in_petrinet(final EClass it) {
    {
      this.refactorings.extractMetaClass(it, getEReference("petrinet", "Place", "dst"), "in", "out");
      createEAttribute(it, "weight", 
        createList(
          a -> a.setEType(getEDataType("ecore", "EInt")),
          this::_createEAttribute_weight_in_createEClass_PTArc_in_petrinet
        )
      );
    }
  }
  
  public void _createEAttribute_weight_in_createEClass_PTArc_in_petrinet(final EAttribute it) {
    it.setLowerBound(1);
  }
  
  public void _createEClass_TPArc_in_petrinet(final EClass it) {
    {
      this.refactorings.extractMetaClass(it, getEReference("petrinet", "Transition", "dst"), "in", "out");
      createEAttribute(it, "weight", 
        createList(
          a -> a.setEType(getEDataType("ecore", "EInt")),
          this::_createEAttribute_weight_in_createEClass_TPArc_in_petrinet
        )
      );
    }
  }
  
  public void _createEAttribute_weight_in_createEClass_TPArc_in_petrinet(final EAttribute it) {
    it.setLowerBound(1);
  }
  
  public void _createEClass_Arc_in_petrinet(final EClass it) {
    {
      it.setAbstract(true);
      this.refactorings.extractSuperclass(it, 
        Collections.<EAttribute>unmodifiableList(CollectionLiterals.<EAttribute>newArrayList(getEAttribute("petrinet", "PTArc", "weight"), getEAttribute("petrinet", "TPArc", "weight"))));
    }
  }
}
