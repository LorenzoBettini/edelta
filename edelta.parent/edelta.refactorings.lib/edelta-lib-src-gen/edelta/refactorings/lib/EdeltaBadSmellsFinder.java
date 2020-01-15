package edelta.refactorings.lib;

import com.google.common.collect.Iterables;
import edelta.lib.AbstractEdelta;
import edelta.refactorings.lib.EstructuralFeatureEqualityHelper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.xtext.xbase.lib.CollectionLiterals;
import org.eclipse.xtext.xbase.lib.Functions.Function1;
import org.eclipse.xtext.xbase.lib.Functions.Function2;
import org.eclipse.xtext.xbase.lib.IterableExtensions;
import org.eclipse.xtext.xbase.lib.ListExtensions;
import org.eclipse.xtext.xbase.lib.MapExtensions;

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
}
