package edelta.refactorings.lib;

import edelta.lib.EdeltaDefaultRuntime;
import edelta.lib.EdeltaEcoreUtil;
import edelta.lib.EdeltaModelMigrator;
import edelta.lib.EdeltaRuntime;
import edelta.refactorings.lib.helper.EdeltaEObjectHelper;
import edelta.refactorings.lib.helper.EdeltaPromptHelper;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.xbase.lib.Functions.Function1;
import org.eclipse.xtext.xbase.lib.IterableExtensions;

@SuppressWarnings("all")
public class EdeltaRefactoringsWithPrompt extends EdeltaDefaultRuntime {
  private EdeltaRefactorings refactorings;
  
  public EdeltaRefactoringsWithPrompt(final EdeltaRuntime other) {
    super(other);
    refactorings = new EdeltaRefactorings(this);
  }
  
  /**
   * Creates the classes with the given names as subclasses of the passed
   * superClass, which will then be made abstract; For model migration it
   * prompts the user on the console.
   * 
   * @see EdeltaRefactorings#introduceSubclasses(EClass, Collection, edelta.lib.EdeltaModelMigrator.EObjectFunction)
   * 
   * @param superClass
   * @param name
   */
  public Collection<EClass> introduceSubclassesInteractive(final EClass superClass, final List<String> names) {
    final EdeltaModelMigrator.EObjectFunction _function = (EObject oldObj) -> {
      final EdeltaEObjectHelper helper = new EdeltaEObjectHelper();
      String _represent = helper.represent(oldObj);
      String _plus = ("Migrating " + _represent);
      EdeltaPromptHelper.show(_plus);
      EdeltaPromptHelper.show(helper.positionInContainter(oldObj));
      final String choice = EdeltaPromptHelper.choice(names);
      return EdeltaEcoreUtil.createInstance(this.getEClass(superClass.getEPackage(), choice));
    };
    return this.refactorings.introduceSubclasses(superClass, names, _function);
  }
  
  public EAttribute mergeStringAttributes(final String newAttributeName, final Collection<EAttribute> attributes) {
    EAttribute _xblockexpression = null;
    {
      EAttribute _head = IterableExtensions.<EAttribute>head(attributes);
      this.refactorings.checkType(_head, getEDataType("ecore", "EString"));
      final Function<Collection<?>, Object> _function = (Collection<?> oldValues) -> {
        final Function1<Object, String> _function_1 = (Object it) -> {
          return it.toString();
        };
        final Iterable<String> stringValues = IterableExtensions.map(IterableExtensions.filterNull(oldValues), _function_1);
        boolean _isEmpty = IterableExtensions.isEmpty(stringValues);
        if (_isEmpty) {
          return null;
        }
        String _join = IterableExtensions.join(stringValues, ", ");
        String _plus = ("Merging values: " + _join);
        EdeltaPromptHelper.show(_plus);
        final String sep = EdeltaPromptHelper.ask("Separator?");
        return IterableExtensions.join(stringValues, sep);
      };
      _xblockexpression = this.refactorings.mergeAttributes(newAttributeName, attributes, _function);
    }
    return _xblockexpression;
  }
  
  @Override
  public void performSanityChecks() throws Exception {
    ensureEPackageIsLoaded("ecore");
  }
}
