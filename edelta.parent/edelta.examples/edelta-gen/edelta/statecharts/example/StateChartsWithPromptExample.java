package edelta.statecharts.example;

import edelta.lib.EdeltaDefaultRuntime;
import edelta.lib.EdeltaRuntime;
import edelta.refactorings.lib.EdeltaRefactoringsWithPrompt;
import java.util.Collections;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.xtext.xbase.lib.CollectionLiterals;

@SuppressWarnings("all")
public class StateChartsWithPromptExample extends EdeltaDefaultRuntime {
  private EdeltaRefactoringsWithPrompt refactorings;
  
  public StateChartsWithPromptExample(final EdeltaRuntime other) {
    super(other);
    refactorings = new EdeltaRefactoringsWithPrompt(this);
  }
  
  public void introduceNodeSubclasses(final EPackage it) {
    this.refactorings.introduceSubclassesInteractive(
      getEClass("statecharts", "Node"), 
      Collections.<String>unmodifiableList(CollectionLiterals.<String>newArrayList("InitialState", "State", "FinalState")));
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
