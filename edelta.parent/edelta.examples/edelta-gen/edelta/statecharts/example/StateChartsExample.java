package edelta.statecharts.example;

import com.google.common.base.Objects;
import edelta.lib.EdeltaDefaultRuntime;
import edelta.lib.EdeltaEcoreUtil;
import edelta.lib.EdeltaModelMigrator;
import edelta.lib.EdeltaRuntime;
import edelta.refactorings.lib.EdeltaRefactorings;
import java.util.Collections;
import java.util.List;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.xtext.xbase.lib.CollectionLiterals;
import org.eclipse.xtext.xbase.lib.IterableExtensions;

@SuppressWarnings("all")
public class StateChartsExample extends EdeltaDefaultRuntime {
  private EdeltaRefactorings refactorings;
  
  public StateChartsExample(final EdeltaRuntime other) {
    super(other);
    refactorings = new EdeltaRefactorings(this);
  }
  
  public void introduceNodeSubclasses(final EPackage it) {
    final EPackage ePackage = it;
    final EdeltaModelMigrator.EObjectFunction _function = (EObject oldObj) -> {
      final EObject container = oldObj.eContainer();
      final List<EObject> nodes = EdeltaEcoreUtil.getValueAsList(container, oldObj.eContainingFeature());
      EObject _head = IterableExtensions.<EObject>head(nodes);
      boolean _equals = Objects.equal(_head, oldObj);
      if (_equals) {
        return EdeltaEcoreUtil.createInstance(this.getEClass(ePackage, "InitialState"));
      } else {
        EObject _last = IterableExtensions.<EObject>last(nodes);
        boolean _equals_1 = Objects.equal(_last, oldObj);
        if (_equals_1) {
          return EdeltaEcoreUtil.createInstance(this.getEClass(ePackage, "FinalState"));
        }
      }
      return EdeltaEcoreUtil.createInstance(this.getEClass(ePackage, "State"));
    };
    this.refactorings.introduceSubclasses(
      getEClass("statecharts", "Node"), 
      Collections.<String>unmodifiableList(CollectionLiterals.<String>newArrayList("InitialState", "FinalState", "State")), _function);
  }
  
  @Override
  public void performSanityChecks() throws Exception {
    ensureEPackageIsLoaded("statecharts");
  }
  
  @Override
  protected void doExecute() throws Exception {
    introduceNodeSubclasses(getEPackage("statecharts"));
  }
}
