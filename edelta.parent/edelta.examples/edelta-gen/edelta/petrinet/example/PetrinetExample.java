package edelta.petrinet.example;

import edelta.lib.EdeltaDefaultRuntime;
import edelta.lib.EdeltaRuntime;
import edelta.lib.EdeltaUtils;
import edelta.refactorings.lib.EdeltaRefactorings;
import java.util.Collections;
import java.util.function.Consumer;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.xtext.xbase.lib.CollectionLiterals;
import org.eclipse.xtext.xbase.lib.ObjectExtensions;
import org.eclipse.xtext.xbase.lib.Procedures.Procedure1;

@SuppressWarnings("all")
public class PetrinetExample extends EdeltaDefaultRuntime {
  private EdeltaRefactorings refactorings;
  
  public PetrinetExample(final EdeltaRuntime other) {
    super(other);
    refactorings = new EdeltaRefactorings(this);
  }
  
  public EAttribute addWeightAttribute(final EClass c) {
    final Consumer<EAttribute> _function = (EAttribute it) -> {
      EdeltaUtils.makeRequired(it);
    };
    return this.stdLib.addNewEAttribute(c, "weight", getEDataType("ecore", "EInt"), _function);
  }
  
  public void modifyNet(final EPackage it) {
    getEClass("petrinet", "Net").setName("Petrinet");
    EdeltaUtils.makeRequired(getEReference("petrinet", "Petrinet", "places"));
    EdeltaUtils.makeRequired(getEReference("petrinet", "Petrinet", "transitions"));
  }
  
  public void introducePTArc(final EPackage it) {
    EClass _referenceToClass = this.refactorings.referenceToClass("PTArc", getEReference("petrinet", "Place", "dst"));
    final Procedure1<EClass> _function = (EClass it_1) -> {
      this.addWeightAttribute(it_1);
    };
    ObjectExtensions.<EClass>operator_doubleArrow(_referenceToClass, _function);
    getEReference("petrinet", "Place", "dst").setName("out");
    getEReference("petrinet", "Transition", "src").setName("in");
    getEReference("petrinet", "PTArc", "transition").setName("dst");
    getEReference("petrinet", "PTArc", "place").setName("src");
  }
  
  public void introduceTPArc(final EPackage it) {
    EClass _referenceToClass = this.refactorings.referenceToClass("TPArc", getEReference("petrinet", "Transition", "dst"));
    final Procedure1<EClass> _function = (EClass it_1) -> {
      this.addWeightAttribute(it_1);
    };
    ObjectExtensions.<EClass>operator_doubleArrow(_referenceToClass, _function);
    getEReference("petrinet", "Place", "src").setName("in");
    getEReference("petrinet", "Transition", "dst").setName("out");
    getEReference("petrinet", "TPArc", "transition").setName("src");
    getEReference("petrinet", "TPArc", "place").setName("dst");
  }
  
  public void introduceAbstractArc(final EPackage it) {
    final EClass arc = this.refactorings.extractSuperclass("Arc", 
      Collections.<EStructuralFeature>unmodifiableList(CollectionLiterals.<EStructuralFeature>newArrayList(getEAttribute("petrinet", "PTArc", "weight"), getEAttribute("petrinet", "TPArc", "weight"))));
    final EReference netRef = this.stdLib.addNewEReference(arc, "net", getEClass("petrinet", "Petrinet"));
    final Consumer<EReference> _function = (EReference it_1) -> {
      EdeltaUtils.makeContainment(it_1);
      EdeltaUtils.makeMultiple(it_1);
      EdeltaUtils.makeBidirectional(it_1, netRef);
    };
    this.stdLib.addNewEReference(getEClass("petrinet", "Petrinet"), "arcs", arc, _function);
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
