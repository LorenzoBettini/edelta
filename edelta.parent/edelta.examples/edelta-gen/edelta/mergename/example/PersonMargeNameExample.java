package edelta.mergename.example;

import edelta.lib.EdeltaDefaultRuntime;
import edelta.lib.EdeltaRuntime;
import edelta.refactorings.lib.EdeltaRefactorings;
import java.util.Collection;
import java.util.Collections;
import java.util.function.Function;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.xtext.xbase.lib.CollectionLiterals;
import org.eclipse.xtext.xbase.lib.Extension;
import org.eclipse.xtext.xbase.lib.Functions.Function1;
import org.eclipse.xtext.xbase.lib.IterableExtensions;

@SuppressWarnings("all")
public class PersonMargeNameExample extends EdeltaDefaultRuntime {
  @Extension
  private EdeltaRefactorings refactorings;

  public PersonMargeNameExample(final EdeltaRuntime other) {
    super(other);
    refactorings = new EdeltaRefactorings(this);
  }

  public void mergeName(final EPackage it) {
    final Function<Collection<Object>, Object> _function = (Collection<Object> oldValues) -> {
      final Function1<Object, String> _function_1 = (Object it_1) -> {
        return it_1.toString();
      };
      return IterableExtensions.join(IterableExtensions.<Object, String>map(IterableExtensions.<Object>filterNull(oldValues), _function_1), " ");
    };
    this.refactorings.mergeAttributes(
      "name", 
      Collections.<EAttribute>unmodifiableList(CollectionLiterals.<EAttribute>newArrayList(getEAttribute("addressbook", "Person", "firstname"), getEAttribute("addressbook", "Person", "lastname"))), _function);
  }

  @Override
  public void performSanityChecks() throws Exception {
    ensureEPackageIsLoaded("addressbook");
  }

  @Override
  protected void doExecute() throws Exception {
    mergeName(getEPackage("addressbook"));
  }
}
