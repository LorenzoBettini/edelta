package edelta.refactorings.lib;

import com.google.common.collect.Iterables;
import edelta.lib.AbstractEdelta;
import edelta.lib.EdeltaLibrary;
import edelta.refactorings.lib.helper.EdeltaFeatureEqualityHelper;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.xtext.xbase.lib.CollectionLiterals;
import org.eclipse.xtext.xbase.lib.Functions.Function1;
import org.eclipse.xtext.xbase.lib.Functions.Function2;
import org.eclipse.xtext.xbase.lib.IterableExtensions;
import org.eclipse.xtext.xbase.lib.ListExtensions;
import org.eclipse.xtext.xbase.lib.MapExtensions;
import org.eclipse.xtext.xbase.lib.Pair;

@SuppressWarnings("all")
public class EdeltaBadSmellsFinder extends AbstractEdelta {
  public EdeltaBadSmellsFinder() {
    
  }
  
  public EdeltaBadSmellsFinder(final AbstractEdelta other) {
    super(other);
  }
  
  /**
   * Finds all the features that are structurally equal
   * in the given {@link EPackage}.
   * 
   * Note that this takes into consideration the name and type,
   * but also other properties like lowerBound, unique, etc.
   * 
   * For example, given these EClasses
   * 
   * <pre>
   * C1 {
   *   A1 : EString
   * }
   * 
   * C2 {
   *   A1 : EString
   * }
   * </pre>
   * 
   * It returns the map with this entry
   * 
   * <pre>
   * (A1 : EString) -> [ C1:A1, C2:A1 ]
   * </pre>
   * 
   * @param ePackage
   */
  public Map<EStructuralFeature, List<EStructuralFeature>> findDuplicatedFeatures(final EPackage ePackage) {
    final BiPredicate<EStructuralFeature, EStructuralFeature> _function = (EStructuralFeature existing, EStructuralFeature current) -> {
      return new EdeltaFeatureEqualityHelper().equals(existing, current);
    };
    return this.findDuplicatedFeaturesCustom(ePackage, _function);
  }
  
  /**
   * Allows you to specify the lambda checking for equality of features.
   * 
   * @param ePackage
   * @param matcher
   */
  public Map<EStructuralFeature, List<EStructuralFeature>> findDuplicatedFeaturesCustom(final EPackage ePackage, final BiPredicate<EStructuralFeature, EStructuralFeature> matcher) {
    final List<EStructuralFeature> allFeatures = IterableExtensions.<EStructuralFeature>toList(this.allEStructuralFeatures(ePackage));
    final Map<EStructuralFeature, List<EStructuralFeature>> result = this.findDuplicatedFeaturesInCollection(allFeatures, matcher);
    final Consumer<Map.Entry<EStructuralFeature, List<EStructuralFeature>>> _function = (Map.Entry<EStructuralFeature, List<EStructuralFeature>> it) -> {
      final Supplier<String> _function_1 = () -> {
        final Function1<EStructuralFeature, String> _function_2 = (EStructuralFeature it_1) -> {
          return EdeltaLibrary.getEObjectRepr(it_1);
        };
        String _join = IterableExtensions.join(ListExtensions.<EStructuralFeature, String>map(it.getValue(), _function_2), ", ");
        return ("Duplicate features: " + _join);
      };
      this.logInfo(_function_1);
    };
    result.entrySet().forEach(_function);
    return result;
  }
  
  /**
   * Finds all the features that are structurally equal
   * in the given collection.
   * 
   * Note that this takes into consideration the name and type,
   * but also other properties like lowerBound, unique, etc.
   * 
   * For example, given these EClasses
   * 
   * <pre>
   * C1 {
   *   A1 : EString
   * }
   * 
   * C2 {
   *   A1 : EString
   * }
   * </pre>
   * 
   * And the collection of features [ C1:A1, C2:A1 ]
   * it returns the map with this entry
   * 
   * <pre>
   * (A1 : EString) -> [ C1:A1, C2:A1 ]
   * </pre>
   * 
   * It allows you to specify the lambda checking for equality of features.
   * 
   * @param features
   * @param matcher
   */
  public Map<EStructuralFeature, List<EStructuralFeature>> findDuplicatedFeaturesInCollection(final Collection<EStructuralFeature> features, final BiPredicate<EStructuralFeature, EStructuralFeature> matcher) {
    final LinkedHashMap<EStructuralFeature, List<EStructuralFeature>> map = CollectionLiterals.<EStructuralFeature, List<EStructuralFeature>>newLinkedHashMap();
    for (final EStructuralFeature f : features) {
      {
        final Function1<Map.Entry<EStructuralFeature, List<EStructuralFeature>>, Boolean> _function = (Map.Entry<EStructuralFeature, List<EStructuralFeature>> it) -> {
          return Boolean.valueOf(matcher.test(it.getKey(), f));
        };
        final Map.Entry<EStructuralFeature, List<EStructuralFeature>> existing = IterableExtensions.<Map.Entry<EStructuralFeature, List<EStructuralFeature>>>findFirst(map.entrySet(), _function);
        if ((existing != null)) {
          List<EStructuralFeature> _value = existing.getValue();
          _value.add(f);
        } else {
          map.put(f, CollectionLiterals.<EStructuralFeature>newArrayList(f));
        }
      }
    }
    final Function2<EStructuralFeature, List<EStructuralFeature>, Boolean> _function = (EStructuralFeature key, List<EStructuralFeature> values) -> {
      int _size = values.size();
      return Boolean.valueOf((_size > 1));
    };
    return MapExtensions.<EStructuralFeature, List<EStructuralFeature>>filter(map, _function);
  }
  
  /**
   * If a class has more than one direct subclass, it finds duplicate features in all
   * of its direct subclasses.
   * 
   * Returns a map where the key is the class with more than one direct subclass
   * with duplicate features; the value is another map as returned by
   * {@link #findDuplicatedFeaturesInCollection(Collection, BiPredicate)}
   */
  public LinkedHashMap<EClass, Map<EStructuralFeature, List<EStructuralFeature>>> findDuplicatedFeaturesInSubclasses(final EPackage ePackage) {
    final LinkedHashMap<EClass, Map<EStructuralFeature, List<EStructuralFeature>>> map = CollectionLiterals.<EClass, Map<EStructuralFeature, List<EStructuralFeature>>>newLinkedHashMap();
    List<EClass> _allEClasses = EdeltaLibrary.allEClasses(ePackage);
    for (final EClass c : _allEClasses) {
      {
        final Iterable<EClass> directSubclasses = this.directSubclasses(c);
        final int numOfSubclasses = IterableExtensions.size(directSubclasses);
        if ((numOfSubclasses > 1)) {
          final Function1<EClass, EList<EStructuralFeature>> _function = (EClass it) -> {
            return it.getEStructuralFeatures();
          };
          final BiPredicate<EStructuralFeature, EStructuralFeature> _function_1 = (EStructuralFeature existing, EStructuralFeature current) -> {
            return new EdeltaFeatureEqualityHelper().equals(existing, current);
          };
          final Map<EStructuralFeature, List<EStructuralFeature>> candidates = this.findDuplicatedFeaturesInCollection(
            IterableExtensions.<EStructuralFeature>toList(Iterables.<EStructuralFeature>concat(IterableExtensions.<EClass, EList<EStructuralFeature>>map(directSubclasses, _function))), _function_1);
          final Function2<EStructuralFeature, List<EStructuralFeature>, Boolean> _function_2 = (EStructuralFeature key, List<EStructuralFeature> values) -> {
            int _size = values.size();
            return Boolean.valueOf((_size == numOfSubclasses));
          };
          final Map<EStructuralFeature, List<EStructuralFeature>> duplicates = MapExtensions.<EStructuralFeature, List<EStructuralFeature>>filter(candidates, _function_2);
          boolean _isEmpty = duplicates.isEmpty();
          boolean _not = (!_isEmpty);
          if (_not) {
            map.put(c, duplicates);
            final Consumer<Map.Entry<EStructuralFeature, List<EStructuralFeature>>> _function_3 = (Map.Entry<EStructuralFeature, List<EStructuralFeature>> it) -> {
              final Supplier<String> _function_4 = () -> {
                String _eObjectRepr = EdeltaLibrary.getEObjectRepr(c);
                String _plus = ("In subclasses of " + _eObjectRepr);
                String _plus_1 = (_plus + ", duplicate features: ");
                final Function1<EStructuralFeature, String> _function_5 = (EStructuralFeature it_1) -> {
                  return EdeltaLibrary.getEObjectRepr(it_1);
                };
                String _join = IterableExtensions.join(ListExtensions.<EStructuralFeature, String>map(it.getValue(), _function_5), ", ");
                return (_plus_1 + _join);
              };
              this.logInfo(_function_4);
            };
            duplicates.entrySet().forEach(_function_3);
          }
        }
      }
    }
    return map;
  }
  
  public Iterable<EStructuralFeature> allEStructuralFeatures(final EPackage ePackage) {
    final Function1<EClass, EList<EStructuralFeature>> _function = (EClass it) -> {
      return it.getEStructuralFeatures();
    };
    return Iterables.<EStructuralFeature>concat(ListExtensions.<EClass, EList<EStructuralFeature>>map(EdeltaLibrary.allEClasses(ePackage), _function));
  }
  
  /**
   * Finds all the features corresponding to a redundant container,
   * that is, a missed opposite reference to the container.
   * 
   * The result consists of an iterable of pairs where the key
   * is the reference corresponding to the redundant container
   * and the value is the reference that should correspond to the
   * opposite reference.
   * 
   * For example, if "Bank" has a containment feature "clients",
   * and the "Client" has a non-containment feature "bank", which
   * is not set as the opposite of "clients", then the detected
   * redundant container will be the pair "Client:bank" -> "Bank:clients".
   * 
   * This form should make the corresponding refactoring trivial to
   * implement, since all the information are in the pair.
   */
  public Iterable<Pair<EReference, EReference>> findRedundantContainers(final EPackage ePackage) {
    final Function1<EClass, ArrayList<Pair<EReference, EReference>>> _function = (EClass it) -> {
      return this.findRedundantContainers(it);
    };
    return Iterables.<Pair<EReference, EReference>>concat(ListExtensions.<EClass, ArrayList<Pair<EReference, EReference>>>map(EdeltaLibrary.allEClasses(ePackage), _function));
  }
  
  /**
   * see {@link #findRedundantContainers(EPackage)}
   */
  public ArrayList<Pair<EReference, EReference>> findRedundantContainers(final EClass cl) {
    final ArrayList<Pair<EReference, EReference>> redundantContainers = CollectionLiterals.<Pair<EReference, EReference>>newArrayList();
    final Function1<EReference, Boolean> _function = (EReference it) -> {
      return Boolean.valueOf(it.isContainment());
    };
    final Iterable<EReference> containmentReferences = IterableExtensions.<EReference>filter(cl.getEReferences(), _function);
    for (final EReference containmentReference : containmentReferences) {
      {
        final Function1<EReference, Boolean> _function_1 = (EReference it) -> {
          return Boolean.valueOf(((((!it.isContainment()) && 
            it.isRequired()) && 
            (it.getEOpposite() == null)) && 
            (it.getEReferenceType() == cl)));
        };
        final EReference redundant = IterableExtensions.<EReference>head(IterableExtensions.<EReference>filter(containmentReference.getEReferenceType().getEReferences(), _function_1));
        if ((redundant != null)) {
          Pair<EReference, EReference> _mappedTo = Pair.<EReference, EReference>of(redundant, containmentReference);
          redundantContainers.add(_mappedTo);
          final Supplier<String> _function_2 = () -> {
            String _eObjectRepr = EdeltaLibrary.getEObjectRepr(containmentReference);
            String _plus = ("Redundant container: " + _eObjectRepr);
            String _plus_1 = (_plus + " -> ");
            String _eObjectRepr_1 = EdeltaLibrary.getEObjectRepr(redundant);
            return (_plus_1 + _eObjectRepr_1);
          };
          this.logInfo(_function_2);
        }
      }
    }
    return redundantContainers;
  }
  
  /**
   * see {@link #isDeadClassifier(EClassifier)}
   */
  public List<EClassifier> findDeadClassifiers(final EPackage ePackage) {
    final Function1<EClassifier, Boolean> _function = (EClassifier it) -> {
      return Boolean.valueOf(this.isDeadClassifier(it));
    };
    return IterableExtensions.<EClassifier>toList(IterableExtensions.<EClassifier>filter(ePackage.getEClassifiers(), _function));
  }
  
  /**
   * Whether {@link #hasNoReferenceInThisPackage(EClassifier)} and
   * {@link #isNotReferenced(EClassifier)}
   */
  public boolean isDeadClassifier(final EClassifier cl) {
    if ((this.hasNoReferenceInThisPackage(cl) && this.isNotReferenced(cl))) {
      final Supplier<String> _function = () -> {
        String _eObjectRepr = EdeltaLibrary.getEObjectRepr(cl);
        return ("Dead classifier: " + _eObjectRepr);
      };
      this.logInfo(_function);
      return true;
    }
    return false;
  }
  
  /**
   * Whether the passed EClassifier does not refer to anything in its
   * EPackage.
   */
  public boolean hasNoReferenceInThisPackage(final EClassifier c) {
    boolean _xblockexpression = false;
    {
      final EPackage thisPackage = c.getEPackage();
      final Function1<EClassifier, Boolean> _function = (EClassifier it) -> {
        EPackage _ePackage = it.getEPackage();
        return Boolean.valueOf((_ePackage == thisPackage));
      };
      _xblockexpression = IterableExtensions.isEmpty(IterableExtensions.<EClassifier>filter(Iterables.<EClassifier>filter(EcoreUtil.CrossReferencer.find(CollectionLiterals.<EClassifier>newArrayList(c)).keySet(), EClassifier.class), _function));
    }
    return _xblockexpression;
  }
  
  /**
   * Whether the passed EClassifier is not referenced in its
   * EPackage.
   */
  public boolean isNotReferenced(final EClassifier cl) {
    return EcoreUtil.UsageCrossReferencer.find(cl, cl.getEPackage()).isEmpty();
  }
  
  /**
   * Returns a map where the key is an EClass (superclass)
   * and the associated value is a list of subclasses that are
   * considered matching the "classification by hierarchy" bad smell.
   */
  public Map<EClass, List<EClass>> findClassificationByHierarchy(final EPackage ePackage) {
    final Function1<EClass, Boolean> _function = (EClass it) -> {
      return Boolean.valueOf((((it.getESuperTypes().size() == 1) && 
        it.getEStructuralFeatures().isEmpty()) && 
        this.isNotReferenced(it)));
    };
    final Function1<EClass, EClass> _function_1 = (EClass it) -> {
      return IterableExtensions.<EClass>head(it.getESuperTypes());
    };
    final Function2<EClass, List<EClass>, Boolean> _function_2 = (EClass base, List<EClass> subclasses) -> {
      int _size = subclasses.size();
      return Boolean.valueOf((_size > 1));
    };
    final Map<EClass, List<EClass>> classification = MapExtensions.<EClass, List<EClass>>filter(IterableExtensions.<EClass, EClass>groupBy(IterableExtensions.<EClass>filter(EdeltaLibrary.allEClasses(ePackage), _function), _function_1), _function_2);
    final Consumer<Map.Entry<EClass, List<EClass>>> _function_3 = (Map.Entry<EClass, List<EClass>> it) -> {
      final Supplier<String> _function_4 = () -> {
        String _eObjectRepr = EdeltaLibrary.getEObjectRepr(it.getKey());
        String _plus = ("Classification by hierarchy: " + _eObjectRepr);
        String _plus_1 = (_plus + " - ");
        String _plus_2 = (_plus_1 + "subclasses[");
        final Function1<EClass, String> _function_5 = (EClass it_1) -> {
          return EdeltaLibrary.getEObjectRepr(it_1);
        };
        String _join = IterableExtensions.join(ListExtensions.<EClass, String>map(it.getValue(), _function_5), ",");
        String _plus_3 = (_plus_2 + _join);
        return (_plus_3 + "]");
      };
      this.logInfo(_function_4);
    };
    classification.entrySet().forEach(_function_3);
    return classification;
  }
  
  /**
   * Finds base classes that should be set as abstract,
   * since they have subclasses.
   */
  public Iterable<EClass> findConcreteAbstractMetaclasses(final EPackage ePackage) {
    final Function1<EClass, Boolean> _function = (EClass cl) -> {
      return Boolean.valueOf(((!cl.isAbstract()) && 
        this.hasSubclasses(cl)));
    };
    final Iterable<EClass> classes = IterableExtensions.<EClass>filter(EdeltaLibrary.allEClasses(ePackage), _function);
    final Consumer<EClass> _function_1 = (EClass it) -> {
      final Supplier<String> _function_2 = () -> {
        String _eObjectRepr = EdeltaLibrary.getEObjectRepr(it);
        return ("Concrete abstract class: " + _eObjectRepr);
      };
      this.logInfo(_function_2);
    };
    classes.forEach(_function_1);
    return classes;
  }
  
  public boolean hasSubclasses(final EClass cl) {
    final Function1<EStructuralFeature.Setting, Boolean> _function = (EStructuralFeature.Setting it) -> {
      EStructuralFeature _eStructuralFeature = it.getEStructuralFeature();
      EReference _eClass_ESuperTypes = EcorePackage.eINSTANCE.getEClass_ESuperTypes();
      return Boolean.valueOf((_eStructuralFeature == _eClass_ESuperTypes));
    };
    boolean _isEmpty = IterableExtensions.isEmpty(IterableExtensions.<EStructuralFeature.Setting>filter(EcoreUtil.UsageCrossReferencer.find(cl, cl.getEPackage()), _function));
    return (!_isEmpty);
  }
  
  public Iterable<EClass> directSubclasses(final EClass cl) {
    final Function1<EStructuralFeature.Setting, Boolean> _function = (EStructuralFeature.Setting it) -> {
      EStructuralFeature _eStructuralFeature = it.getEStructuralFeature();
      EReference _eClass_ESuperTypes = EcorePackage.eINSTANCE.getEClass_ESuperTypes();
      return Boolean.valueOf((_eStructuralFeature == _eClass_ESuperTypes));
    };
    final Function1<EStructuralFeature.Setting, EClass> _function_1 = (EStructuralFeature.Setting it) -> {
      EObject _eObject = it.getEObject();
      return ((EClass) _eObject);
    };
    return IterableExtensions.<EStructuralFeature.Setting, EClass>map(IterableExtensions.<EStructuralFeature.Setting>filter(EcoreUtil.UsageCrossReferencer.find(cl, cl.getEPackage()), _function), _function_1);
  }
  
  /**
   * Finds abstract classes that should be concrete,
   * since they have no subclasses.
   */
  public Iterable<EClass> findAbstractConcreteMetaclasses(final EPackage ePackage) {
    final Function1<EClass, Boolean> _function = (EClass cl) -> {
      return Boolean.valueOf((cl.isAbstract() && 
        (!this.hasSubclasses(cl))));
    };
    final Iterable<EClass> classes = IterableExtensions.<EClass>filter(EdeltaLibrary.allEClasses(ePackage), _function);
    final Consumer<EClass> _function_1 = (EClass it) -> {
      final Supplier<String> _function_2 = () -> {
        String _eObjectRepr = EdeltaLibrary.getEObjectRepr(it);
        return ("Abstract concrete class: " + _eObjectRepr);
      };
      this.logInfo(_function_2);
    };
    classes.forEach(_function_1);
    return classes;
  }
  
  /**
   * Finds classes that are abstract though they have only concrete superclasses.
   */
  public Iterable<EClass> findAbstractSubclassesOfConcreteSuperclasses(final EPackage ePackage) {
    final Function1<EClass, Boolean> _function = (EClass cl) -> {
      return Boolean.valueOf(((cl.isAbstract() && 
        (!cl.getESuperTypes().isEmpty())) && 
        IterableExtensions.<EClass>forall(cl.getESuperTypes(), ((Function1<EClass, Boolean>) (EClass it) -> {
          boolean _isAbstract = it.isAbstract();
          return Boolean.valueOf((!_isAbstract));
        }))));
    };
    final Iterable<EClass> classes = IterableExtensions.<EClass>filter(EdeltaLibrary.allEClasses(ePackage), _function);
    final Consumer<EClass> _function_1 = (EClass it) -> {
      final Supplier<String> _function_2 = () -> {
        String _eObjectRepr = EdeltaLibrary.getEObjectRepr(it);
        return ("Abstract class with concrete superclasses: " + _eObjectRepr);
      };
      this.logInfo(_function_2);
    };
    classes.forEach(_function_1);
    return classes;
  }
}
