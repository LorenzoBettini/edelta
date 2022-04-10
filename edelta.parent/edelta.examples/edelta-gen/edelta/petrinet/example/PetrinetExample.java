package edelta.petrinet.example;

import edelta.lib.EdeltaDefaultRuntime;
import edelta.lib.EdeltaEcoreUtil;
import edelta.lib.EdeltaModelMigrator;
import edelta.lib.EdeltaRuntime;
import edelta.lib.EdeltaUtils;
import edelta.refactorings.lib.EdeltaRefactorings;
import java.util.Collection;
import java.util.Collections;
import java.util.function.Consumer;
import java.util.function.Predicate;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
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
    final EReference arcs = this.stdLib.addNewEReference(getEClass("petrinet", "Petrinet"), "arcs", arc, _function);
    final EReference placeOut = getEReference("petrinet", "Place", "out");
    EdeltaUtils.dropContainment(placeOut);
    final EReference transitionOut = getEReference("petrinet", "Transition", "out");
    EdeltaUtils.dropContainment(transitionOut);
    final Consumer<EdeltaModelMigrator> _function_1 = (EdeltaModelMigrator it_1) -> {
      final Predicate<EStructuralFeature> _function_2 = (EStructuralFeature f) -> {
        return (it_1.isRelatedTo(f, placeOut) || it_1.isRelatedTo(f, transitionOut));
      };
      final EdeltaModelMigrator.CopyProcedure _function_3 = (EStructuralFeature f, EObject oldObj, EObject newObj) -> {
        final EObject migratedNet = it_1.getMigrated(oldObj.eContainer());
        final Collection<EObject> migratedArcs = it_1.<EObject>getMigrated(EdeltaEcoreUtil.getValueAsList(oldObj, f));
        EdeltaEcoreUtil.getValueAsList(migratedNet, arcs).addAll(migratedArcs);
      };
      it_1.copyRule(_function_2, _function_3);
    };
    this.modelMigration(_function_1);
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
