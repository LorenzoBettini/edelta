package edelta.refactorings.lib;

import edelta.lib.EdeltaDefaultRuntime;
import edelta.lib.EdeltaEcoreUtil;
import edelta.lib.EdeltaModelMigrator;
import edelta.lib.EdeltaRuntime;
import edelta.refactorings.lib.helper.EdeltaEObjectHelper;
import edelta.refactorings.lib.helper.EdeltaPromptHelper;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.xtext.xbase.lib.Conversions;
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
  
  /**
   * Merges the given attributes, expected to be of type EString,
   * into a single new attribute in the containing class; For model migration it
   * prompts the user on the console.
   * 
   * @see EdeltaRefactorings#mergeAttributes(String, Collection, Function)
   * 
   * @param newAttributeName
   * @param attributes
   */
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
  
  /**
   * Changes this feature to multiple with the given upper bound; concerning model migration,
   * it makes sure that a collection is created with at most the specified upper bound
   * if the previous model object's value was set, prompting the user to select the
   * values (in case the original values are less than or equal to upperBound it
   * performs the migration automatically).
   * 
   * @param feature
   * @param upperBound
   */
  public void changeUpperBoundInteractive(final EStructuralFeature feature, final int upperBound) {
    feature.setUpperBound(upperBound);
    final Consumer<EdeltaModelMigrator> _function = (EdeltaModelMigrator it) -> {
      final EdeltaModelMigrator.CopyProcedure _function_1 = (EStructuralFeature origFeature, EObject origObj, EObject migratedObj) -> {
        Collection<Object> origValues = EdeltaEcoreUtil.getValueForFeature(origObj, origFeature, (-1));
        int _size = origValues.size();
        boolean _lessEqualsThan = (_size <= upperBound);
        if (_lessEqualsThan) {
          EdeltaEcoreUtil.setValueForFeature(migratedObj, feature, it.<Object>getMigrated(origValues));
          return;
        }
        final EdeltaEObjectHelper helper = new EdeltaEObjectHelper();
        String _represent = helper.represent(origObj);
        String _plus = ("Migrating " + _represent);
        EdeltaPromptHelper.show(_plus);
        final Function1<Object, String> _function_2 = (Object it_1) -> {
          return helper.represent(it_1);
        };
        final List<String> choices = IterableExtensions.<String>toList(IterableExtensions.<Object, String>map(origValues, _function_2));
        final ArrayList<Object> newValues = new ArrayList<Object>(upperBound);
        for (int i = 1; (i <= upperBound); i++) {
          {
            EdeltaPromptHelper.show(((("Choice " + Integer.valueOf(i)) + " of ") + Integer.valueOf(upperBound)));
            final int choice = EdeltaPromptHelper.choiceIndex(choices);
            final Collection<Object> _converted_origValues = (Collection<Object>)origValues;
            final Object chosen = ((Object[])Conversions.unwrapArray(_converted_origValues, Object.class))[choice];
            newValues.add(it.getMigrated(chosen));
          }
        }
        EdeltaEcoreUtil.setValueForFeature(migratedObj, feature, newValues);
      };
      it.copyRule(
        it.<EStructuralFeature>isRelatedTo(feature), _function_1);
    };
    this.modelMigration(_function);
  }
  
  @Override
  public void performSanityChecks() throws Exception {
    ensureEPackageIsLoaded("ecore");
  }
}
