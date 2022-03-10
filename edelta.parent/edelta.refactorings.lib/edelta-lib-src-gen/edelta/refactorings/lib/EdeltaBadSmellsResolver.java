package edelta.refactorings.lib;

import edelta.lib.EdeltaDefaultRuntime;
import edelta.lib.EdeltaRuntime;
import edelta.lib.EdeltaUtils;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.xtext.xbase.lib.Pair;

@SuppressWarnings("all")
public class EdeltaBadSmellsResolver extends EdeltaDefaultRuntime {
  private EdeltaRefactorings refactorings;
  
  private EdeltaBadSmellsFinder finder;
  
  public EdeltaBadSmellsResolver(final EdeltaRuntime other) {
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
    this.finder.findDuplicatedFeatures(ePackage).values().forEach(_function);
  }
  
  /**
   * Removes the dead classifiers.
   */
  public void resolveDeadClassifiers(final EPackage ePackage) {
    final Predicate<EClassifier> _function = (EClassifier it) -> {
      return true;
    };
    this.resolveDeadClassifiers(ePackage, _function);
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
    final Iterable<Pair<EReference, EReference>> findRedundantContainers = this.finder.findRedundantContainers(ePackage);
    final Consumer<Pair<EReference, EReference>> _function = (Pair<EReference, EReference> it) -> {
      EdeltaUtils.makeBidirectional(it.getKey(), it.getValue());
    };
    findRedundantContainers.forEach(_function);
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
    final Consumer<EClass> _function = (EClass it) -> {
      EdeltaUtils.makeAbstract(it);
    };
    this.finder.findConcreteAbstractMetaclasses(ePackage).forEach(_function);
  }
  
  public void resolveAbstractConcreteMetaclass(final EPackage ePackage) {
    final Consumer<EClass> _function = (EClass it) -> {
      EdeltaUtils.makeConcrete(it);
    };
    this.finder.findAbstractConcreteMetaclasses(ePackage).forEach(_function);
  }
  
  public void resolveAbstractSubclassesOfConcreteSuperclasses(final EPackage ePackage) {
    final Consumer<EClass> _function = (EClass it) -> {
      EdeltaUtils.makeConcrete(it);
    };
    this.finder.findAbstractSubclassesOfConcreteSuperclasses(ePackage).forEach(_function);
  }
  
  public void resolveDuplicatedFeaturesInSubclasses(final EPackage ePackage) {
    final BiConsumer<EClass, Map<EStructuralFeature, List<EStructuralFeature>>> _function = (EClass superClass, Map<EStructuralFeature, List<EStructuralFeature>> duplicates) -> {
      final BiConsumer<EStructuralFeature, List<EStructuralFeature>> _function_1 = (EStructuralFeature key, List<EStructuralFeature> values) -> {
        this.refactorings.pullUpFeatures(superClass, values);
      };
      duplicates.forEach(_function_1);
    };
    this.finder.findDuplicatedFeaturesInSubclasses(ePackage).forEach(_function);
  }
}
