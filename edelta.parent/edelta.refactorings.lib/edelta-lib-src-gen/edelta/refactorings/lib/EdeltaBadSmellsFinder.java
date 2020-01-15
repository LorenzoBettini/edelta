package edelta.refactorings.lib;

import com.google.common.collect.Iterables;
import edelta.lib.AbstractEdelta;
import edelta.refactorings.lib.helper.EstructuralFeatureEqualityHelper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
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
  public Map<EStructuralFeature, List<EStructuralFeature>> findDuplicateFeatures(final EPackage ePackage) {
    final BiPredicate<EStructuralFeature, EStructuralFeature> _function = (EStructuralFeature existing, EStructuralFeature current) -> {
      return new EstructuralFeatureEqualityHelper().equals(existing, current);
    };
    return this.findDuplicateFeaturesCustom(ePackage, _function);
  }
  
  /**
   * Allows you to specify the lambda checking for equality of features.
   * 
   * @param ePackage
   * @param matcher
   */
  public Map<EStructuralFeature, List<EStructuralFeature>> findDuplicateFeaturesCustom(final EPackage ePackage, final BiPredicate<EStructuralFeature, EStructuralFeature> matcher) {
    final Iterable<EStructuralFeature> allFeatures = this.allEStructuralFeatures(ePackage);
    final LinkedHashMap<EStructuralFeature, List<EStructuralFeature>> map = CollectionLiterals.<EStructuralFeature, List<EStructuralFeature>>newLinkedHashMap();
    for (final EStructuralFeature f : allFeatures) {
      {
        final Function1<Map.Entry<EStructuralFeature, List<EStructuralFeature>>, Boolean> _function = (Map.Entry<EStructuralFeature, List<EStructuralFeature>> it) -> {
          return Boolean.valueOf(matcher.test(it.getKey(), f));
        };
        final Map.Entry<EStructuralFeature, List<EStructuralFeature>> found = IterableExtensions.<Map.Entry<EStructuralFeature, List<EStructuralFeature>>>findFirst(map.entrySet(), _function);
        if ((found != null)) {
          List<EStructuralFeature> _value = found.getValue();
          _value.add(f);
        } else {
          map.put(f, CollectionLiterals.<EStructuralFeature>newArrayList(f));
        }
      }
    }
    final Function2<EStructuralFeature, List<EStructuralFeature>, Boolean> _function = (EStructuralFeature p1, List<EStructuralFeature> p2) -> {
      int _size = p2.size();
      return Boolean.valueOf((_size > 1));
    };
    final Map<EStructuralFeature, List<EStructuralFeature>> result = MapExtensions.<EStructuralFeature, List<EStructuralFeature>>filter(map, _function);
    final Consumer<Map.Entry<EStructuralFeature, List<EStructuralFeature>>> _function_1 = (Map.Entry<EStructuralFeature, List<EStructuralFeature>> it) -> {
      final Supplier<String> _function_2 = () -> {
        final Function1<EStructuralFeature, String> _function_3 = (EStructuralFeature it_1) -> {
          return this.lib.getEObjectRepr(it_1);
        };
        String _join = IterableExtensions.join(ListExtensions.<EStructuralFeature, String>map(it.getValue(), _function_3), ", ");
        return ("Duplicate features: " + _join);
      };
      this.logInfo(_function_2);
    };
    result.entrySet().forEach(_function_1);
    return result;
  }
  
  public Iterable<EStructuralFeature> allEStructuralFeatures(final EPackage ePackage) {
    final Function1<EClass, EList<EStructuralFeature>> _function = (EClass it) -> {
      return it.getEStructuralFeatures();
    };
    return Iterables.<EStructuralFeature>concat(IterableExtensions.<EClass, EList<EStructuralFeature>>map(this.allEClasses(ePackage), _function));
  }
  
  public Iterable<EClass> allEClasses(final EPackage ePackage) {
    return Iterables.<EClass>filter(ePackage.getEClassifiers(), EClass.class);
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
    return Iterables.<Pair<EReference, EReference>>concat(IterableExtensions.<EClass, ArrayList<Pair<EReference, EReference>>>map(this.allEClasses(ePackage), _function));
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
            String _eObjectRepr = this.lib.getEObjectRepr(containmentReference);
            String _plus = ("Redundant container: " + _eObjectRepr);
            String _plus_1 = (_plus + " -> ");
            String _eObjectRepr_1 = this.lib.getEObjectRepr(redundant);
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
        String _eObjectRepr = this.lib.getEObjectRepr(cl);
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
}
