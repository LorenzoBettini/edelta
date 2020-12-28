package edelta.refactorings.lib;

import edelta.lib.AbstractEdelta;
import edelta.refactorings.lib.EdeltaBadSmellsFinder;
import edelta.refactorings.lib.EdeltaRefactorings;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;
import org.eclipse.emf.ecore.EClass;
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
    refactorings = new EdeltaRefactorings(other);
    finder = new EdeltaBadSmellsFinder(other);
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
  
  /**
   * Applies redundantContainerToEOpposite to redundant containers
   */
  public void resolveRedundantContainers(final EPackage ePackage) {
    this.refactorings.redundantContainerToEOpposite(
      this.finder.findRedundantContainers(ePackage));
  }
  
  /**
   * Applies subclassesToEnum to findClassificationByHierarchy
   */
  public void resolveClassificationByHierarchy(final EPackage ePackage) {
    final Map<EClass, List<EClass>> findClassificationByHierarchy = this.finder.findClassificationByHierarchy(ePackage);
    final Consumer<Map.Entry<EClass, List<EClass>>> _function = (Map.Entry<EClass, List<EClass>> it) -> {
      String _name = it.getKey().getName();
      String _plus = (_name + "Type");
      this.refactorings.subclassesToEnum(_plus, it.getValue());
    };
    findClassificationByHierarchy.entrySet().forEach(_function);
  }
  
  public void resolveConcreteAbstractMetaclass(final EPackage ePackage) {
    this.refactorings.makeAbstract(
      this.finder.findConcreteAbstractMetaclasses(ePackage));
  }
  
  public void resolveAbstractConcreteMetaclass(final EPackage ePackage) {
    this.refactorings.makeConcrete(
      this.finder.findAbstractConcreteMetaclasses(ePackage));
  }
  
  public void resolveAbstractSubclassesOfConcreteSuperclasses(final EPackage ePackage) {
    this.refactorings.makeConcrete(
      this.finder.findAbstractSubclassesOfConcreteSuperclasses(ePackage));
  }
  
  public void resolveDuplicateFeaturesInSubclasses(final EPackage ePackage) {
    final BiConsumer<EClass, Map<EStructuralFeature, List<EStructuralFeature>>> _function = (EClass superClass, Map<EStructuralFeature, List<EStructuralFeature>> duplicates) -> {
      final BiConsumer<EStructuralFeature, List<EStructuralFeature>> _function_1 = (EStructuralFeature key, List<EStructuralFeature> values) -> {
        this.refactorings.pullUpFeatures(superClass, values);
      };
      duplicates.forEach(_function_1);
    };
    this.finder.findDuplicateFeaturesInSubclasses(ePackage).forEach(_function);
  }
}
