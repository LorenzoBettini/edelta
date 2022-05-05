package edelta.extractclass.example;

import edelta.lib.EdeltaDefaultRuntime;
import edelta.lib.EdeltaRuntime;
import edelta.refactorings.lib.EdeltaRefactorings;
import java.util.Collections;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.xtext.xbase.lib.CollectionLiterals;
import org.eclipse.xtext.xbase.lib.Extension;

@SuppressWarnings("all")
public class PersonExtractAddressExample extends EdeltaDefaultRuntime {
  @Extension
  private EdeltaRefactorings refactorings;
  
  public PersonExtractAddressExample(final EdeltaRuntime other) {
    super(other);
    refactorings = new EdeltaRefactorings(this);
  }
  
  public void extractAddress(final EPackage it) {
    this.refactorings.extractClass(
      "Address", 
      Collections.<EStructuralFeature>unmodifiableList(CollectionLiterals.<EStructuralFeature>newArrayList(getEAttribute("addressbook2", "Person", "street"), getEAttribute("addressbook2", "Person", "zip"), getEAttribute("addressbook2", "Person", "city"))));
  }
  
  @Override
  public void performSanityChecks() throws Exception {
    ensureEPackageIsLoaded("addressbook2");
  }
  
  @Override
  protected void doExecute() throws Exception {
    extractAddress(getEPackage("addressbook2"));
  }
}
