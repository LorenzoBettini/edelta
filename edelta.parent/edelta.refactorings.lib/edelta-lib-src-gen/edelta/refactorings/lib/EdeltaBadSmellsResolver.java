package edelta.refactorings.lib;

import edelta.lib.AbstractEdelta;
import edelta.refactorings.lib.EdeltaBadSmellsFinder;
import edelta.refactorings.lib.EdeltaRefactorings;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.util.EcoreUtil;

@SuppressWarnings("all")
public class EdeltaBadSmellsResolver extends AbstractEdelta {
  private EdeltaRefactorings refactorings;
  
  private EdeltaBadSmellsFinder finder;
  
  public EdeltaBadSmellsResolver() {
    refactorings = new EdeltaRefactorings(this);
    finder = new EdeltaBadSmellsFinder(this);
  }
  
  public EdeltaBadSmellsResolver(final AbstractEdelta other) {
    super(other);
  }
  
  /**
   * Extracts superclasses in the presence of duplicate features
   * considering all the classes of the given package.
   * 
   * @param ePackage
   */
  public void resolveDuplicatedFeatures(final EPackage ePackage) {
    final Consumer<List<EStructuralFeature>> _function = (List<EStructuralFeature> it) -> {
      this.refactorings.extractSuperclass(it);
    };
    this.finder.findDuplicateFeatures(ePackage).values().forEach(_function);
  }
  
  /**
   * Removes the dead classifiers by first checking the passed
   * predicate.
   */
  public void resolveDeadClassifiers(final EPackage ePackage, final Predicate<EClassifier> shouldRemove) {
    final List<EClassifier> deadClassifiers = this.finder.findDeadClassifiers(ePackage);
    final Consumer<EClassifier> _function = (EClassifier cl) -> {
      boolean _test = shouldRemove.test(cl);
      if (_test) {
        EcoreUtil.remove(cl);
      }
    };
    deadClassifiers.forEach(_function);
  }
}
