package edelta.refactorings.lib;

import edelta.lib.AbstractEdelta;
import edelta.refactorings.lib.EdeltaBadSmellsFinder;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.xtext.xbase.lib.Functions.Function1;
import org.eclipse.xtext.xbase.lib.IterableExtensions;

@SuppressWarnings("all")
public class EdeltaBadSmellsChecker extends AbstractEdelta {
  private EdeltaBadSmellsFinder finder;
  
  public EdeltaBadSmellsChecker() {
    finder = new EdeltaBadSmellsFinder(this);
  }
  
  public EdeltaBadSmellsChecker(final AbstractEdelta other) {
    super(other);
  }
  
  /**
   * Shows warnings in case duplicate features are found;
   * for each feature that has duplicates shows a warning and the
   * list of duplicates.
   * 
   * @param ePackage
   */
  public void checkDuplicateFeatures(final EPackage ePackage) {
    final Consumer<Map.Entry<EStructuralFeature, List<EStructuralFeature>>> _function = (Map.Entry<EStructuralFeature, List<EStructuralFeature>> entry) -> {
      final List<EStructuralFeature> duplicates = entry.getValue();
      final Consumer<EStructuralFeature> _function_1 = (EStructuralFeature currentDuplicate) -> {
        final Function1<EStructuralFeature, Boolean> _function_2 = (EStructuralFeature it) -> {
          return Boolean.valueOf((it != currentDuplicate));
        };
        final Function1<EStructuralFeature, String> _function_3 = (EStructuralFeature it) -> {
          return this.lib.getEObjectRepr(it);
        };
        String _join = IterableExtensions.join(IterableExtensions.<EStructuralFeature, String>map(IterableExtensions.<EStructuralFeature>filter(duplicates, _function_2), _function_3), ", ");
        String _plus = ("Duplicate features: " + _join);
        this.showWarning(currentDuplicate, _plus);
      };
      duplicates.forEach(_function_1);
    };
    this.finder.findDuplicateFeatures(ePackage).entrySet().forEach(_function);
  }
}
