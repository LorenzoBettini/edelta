package edelta.mergename.example;

import edelta.lib.EdeltaDefaultRuntime;
import edelta.lib.EdeltaRuntime;
import edelta.refactorings.lib.EdeltaRefactoringsWithPrompt;
import java.util.Collections;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.xtext.xbase.lib.CollectionLiterals;
import org.eclipse.xtext.xbase.lib.Extension;

@SuppressWarnings("all")
public class PersonMargeNameWithPromptExample extends EdeltaDefaultRuntime {
  @Extension
  private EdeltaRefactoringsWithPrompt refactorings;
  
  public PersonMargeNameWithPromptExample(final EdeltaRuntime other) {
    super(other);
    refactorings = new EdeltaRefactoringsWithPrompt(this);
  }
  
  public void mergeName(final EPackage it) {
    this.refactorings.mergeStringAttributes(
      "name", 
      Collections.<EAttribute>unmodifiableList(CollectionLiterals.<EAttribute>newArrayList(getEAttribute("addressbook", "Person", "firstname"), getEAttribute("addressbook", "Person", "lastname"))));
  }
  
  @Override
  public void performSanityChecks() throws Exception {
    ensureEPackageIsLoaded("ecore");
    ensureEPackageIsLoaded("addressbook");
  }
  
  @Override
  protected void doExecute() throws Exception {
    mergeName(getEPackage("addressbook"));
  }
}
