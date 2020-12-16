package edelta.refactorings.lib.tests;

import com.google.common.base.Objects;
import com.google.common.collect.Iterables;
import edelta.lib.AbstractEdelta;
import edelta.refactorings.lib.EdeltaBadSmellsFinder;
import edelta.refactorings.lib.tests.AbstractTest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiPredicate;
import org.assertj.core.api.Assertions;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.ENamedElement;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.xtext.xbase.lib.CollectionLiterals;
import org.eclipse.xtext.xbase.lib.Conversions;
import org.eclipse.xtext.xbase.lib.Functions.Function1;
import org.eclipse.xtext.xbase.lib.IterableExtensions;
import org.eclipse.xtext.xbase.lib.ObjectExtensions;
import org.eclipse.xtext.xbase.lib.Pair;
import org.eclipse.xtext.xbase.lib.Procedures.Procedure1;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

@SuppressWarnings("all")
public class EdeltaBadSmellsFinderTest extends AbstractTest {
  private EdeltaBadSmellsFinder finder;
  
  @Before
  public void setup() {
    EdeltaBadSmellsFinder _edeltaBadSmellsFinder = new EdeltaBadSmellsFinder();
    this.finder = _edeltaBadSmellsFinder;
  }
  
  @Test
  public void test_ConstructorArgument() {
    EdeltaBadSmellsFinder _edeltaBadSmellsFinder = new EdeltaBadSmellsFinder(new AbstractEdelta() {
    });
    this.finder = _edeltaBadSmellsFinder;
    Assertions.<EdeltaBadSmellsFinder>assertThat(this.finder).isNotNull();
  }
  
  @Test
  public void test_findDuplicateFeatures_whenNoDuplicates() {
    EPackage _createEPackage = this.factory.createEPackage();
    final Procedure1<EPackage> _function = (EPackage it) -> {
      EClass _createEClass = this.createEClass(it, "C1");
      final Procedure1<EClass> _function_1 = (EClass it_1) -> {
        EAttribute _createEAttribute = this.createEAttribute(it_1, "A1");
        final Procedure1<EAttribute> _function_2 = (EAttribute it_2) -> {
          it_2.setEType(this.stringDataType);
        };
        ObjectExtensions.<EAttribute>operator_doubleArrow(_createEAttribute, _function_2);
      };
      ObjectExtensions.<EClass>operator_doubleArrow(_createEClass, _function_1);
      EClass _createEClass_1 = this.createEClass(it, "C2");
      final Procedure1<EClass> _function_2 = (EClass it_1) -> {
        EAttribute _createEAttribute = this.createEAttribute(it_1, "A1");
        final Procedure1<EAttribute> _function_3 = (EAttribute it_2) -> {
          it_2.setEType(this.intDataType);
        };
        ObjectExtensions.<EAttribute>operator_doubleArrow(_createEAttribute, _function_3);
      };
      ObjectExtensions.<EClass>operator_doubleArrow(_createEClass_1, _function_2);
    };
    final EPackage p = ObjectExtensions.<EPackage>operator_doubleArrow(_createEPackage, _function);
    final Map<EStructuralFeature, List<EStructuralFeature>> result = this.finder.findDuplicateFeatures(p);
    Assert.assertTrue(("result: " + result), result.isEmpty());
  }
  
  @Test
  public void test_findDuplicateFeatures_withDuplicates() {
    EPackage _createEPackage = this.factory.createEPackage();
    final Procedure1<EPackage> _function = (EPackage it) -> {
      EClass _createEClass = this.createEClass(it, "C1");
      final Procedure1<EClass> _function_1 = (EClass it_1) -> {
        EAttribute _createEAttribute = this.createEAttribute(it_1, "A1");
        final Procedure1<EAttribute> _function_2 = (EAttribute it_2) -> {
          it_2.setEType(this.stringDataType);
        };
        ObjectExtensions.<EAttribute>operator_doubleArrow(_createEAttribute, _function_2);
      };
      ObjectExtensions.<EClass>operator_doubleArrow(_createEClass, _function_1);
      EClass _createEClass_1 = this.createEClass(it, "C2");
      final Procedure1<EClass> _function_2 = (EClass it_1) -> {
        EAttribute _createEAttribute = this.createEAttribute(it_1, "A1");
        final Procedure1<EAttribute> _function_3 = (EAttribute it_2) -> {
          it_2.setEType(this.stringDataType);
        };
        ObjectExtensions.<EAttribute>operator_doubleArrow(_createEAttribute, _function_3);
      };
      ObjectExtensions.<EClass>operator_doubleArrow(_createEClass_1, _function_2);
    };
    final EPackage p = ObjectExtensions.<EPackage>operator_doubleArrow(_createEPackage, _function);
    final Map<EStructuralFeature, List<EStructuralFeature>> result = this.finder.findDuplicateFeatures(p);
    final Function1<EClass, EList<EStructuralFeature>> _function_1 = (EClass it) -> {
      return it.getEStructuralFeatures();
    };
    final Iterable<EStructuralFeature> expected = Iterables.<EStructuralFeature>concat(IterableExtensions.<EClass, EList<EStructuralFeature>>map(this.EClasses(p), _function_1));
    final Iterable<EStructuralFeature> actual = Iterables.<EStructuralFeature>concat(result.values());
    this.<EStructuralFeature>assertIterable(actual, expected);
  }
  
  @Test
  public void test_findDuplicateFeatures_withDifferingAttributesByLowerBound() {
    EPackage _createEPackage = this.factory.createEPackage();
    final Procedure1<EPackage> _function = (EPackage it) -> {
      EClass _createEClass = this.createEClass(it, "C1");
      final Procedure1<EClass> _function_1 = (EClass it_1) -> {
        EAttribute _createEAttribute = this.createEAttribute(it_1, "A1");
        final Procedure1<EAttribute> _function_2 = (EAttribute it_2) -> {
          it_2.setEType(this.stringDataType);
          it_2.setLowerBound(1);
        };
        ObjectExtensions.<EAttribute>operator_doubleArrow(_createEAttribute, _function_2);
      };
      ObjectExtensions.<EClass>operator_doubleArrow(_createEClass, _function_1);
      EClass _createEClass_1 = this.createEClass(it, "C2");
      final Procedure1<EClass> _function_2 = (EClass it_1) -> {
        EAttribute _createEAttribute = this.createEAttribute(it_1, "A1");
        final Procedure1<EAttribute> _function_3 = (EAttribute it_2) -> {
          it_2.setEType(this.stringDataType);
          it_2.setLowerBound(2);
        };
        ObjectExtensions.<EAttribute>operator_doubleArrow(_createEAttribute, _function_3);
      };
      ObjectExtensions.<EClass>operator_doubleArrow(_createEClass_1, _function_2);
    };
    final EPackage p = ObjectExtensions.<EPackage>operator_doubleArrow(_createEPackage, _function);
    final Map<EStructuralFeature, List<EStructuralFeature>> result = this.finder.findDuplicateFeatures(p);
    Assert.assertTrue(("result: " + result), result.isEmpty());
  }
  
  @Test
  public void test_findDuplicateFeatures_withDifferingContainment() {
    EPackage _createEPackage = this.factory.createEPackage();
    final Procedure1<EPackage> _function = (EPackage it) -> {
      EClass _createEClass = this.createEClass(it, "C1");
      final Procedure1<EClass> _function_1 = (EClass it_1) -> {
        EReference _createEReference = this.createEReference(it_1, "r1");
        final Procedure1<EReference> _function_2 = (EReference it_2) -> {
          it_2.setEType(this.eClassReference);
          it_2.setContainment(true);
        };
        ObjectExtensions.<EReference>operator_doubleArrow(_createEReference, _function_2);
      };
      ObjectExtensions.<EClass>operator_doubleArrow(_createEClass, _function_1);
      EClass _createEClass_1 = this.createEClass(it, "C2");
      final Procedure1<EClass> _function_2 = (EClass it_1) -> {
        EReference _createEReference = this.createEReference(it_1, "r1");
        final Procedure1<EReference> _function_3 = (EReference it_2) -> {
          it_2.setEType(this.eClassReference);
          it_2.setContainment(false);
        };
        ObjectExtensions.<EReference>operator_doubleArrow(_createEReference, _function_3);
      };
      ObjectExtensions.<EClass>operator_doubleArrow(_createEClass_1, _function_2);
    };
    final EPackage p = ObjectExtensions.<EPackage>operator_doubleArrow(_createEPackage, _function);
    final Map<EStructuralFeature, List<EStructuralFeature>> result = this.finder.findDuplicateFeatures(p);
    Assert.assertTrue(("result: " + result), result.isEmpty());
  }
  
  @Test
  public void test_findDuplicateFeatures_withCustomEqualityPredicate() {
    EPackage _createEPackage = this.factory.createEPackage();
    final Procedure1<EPackage> _function = (EPackage it) -> {
      EClass _createEClass = this.createEClass(it, "C1");
      final Procedure1<EClass> _function_1 = (EClass it_1) -> {
        EAttribute _createEAttribute = this.createEAttribute(it_1, "A1");
        final Procedure1<EAttribute> _function_2 = (EAttribute it_2) -> {
          it_2.setEType(this.stringDataType);
          it_2.setLowerBound(1);
        };
        ObjectExtensions.<EAttribute>operator_doubleArrow(_createEAttribute, _function_2);
      };
      ObjectExtensions.<EClass>operator_doubleArrow(_createEClass, _function_1);
      EClass _createEClass_1 = this.createEClass(it, "C2");
      final Procedure1<EClass> _function_2 = (EClass it_1) -> {
        EAttribute _createEAttribute = this.createEAttribute(it_1, "A1");
        final Procedure1<EAttribute> _function_3 = (EAttribute it_2) -> {
          it_2.setEType(this.stringDataType);
          it_2.setLowerBound(2);
        };
        ObjectExtensions.<EAttribute>operator_doubleArrow(_createEAttribute, _function_3);
      };
      ObjectExtensions.<EClass>operator_doubleArrow(_createEClass_1, _function_2);
    };
    final EPackage p = ObjectExtensions.<EPackage>operator_doubleArrow(_createEPackage, _function);
    final BiPredicate<EStructuralFeature, EStructuralFeature> _function_1 = (EStructuralFeature f1, EStructuralFeature f2) -> {
      return (Objects.equal(f1.getName(), f2.getName()) && Objects.equal(f1.getEType(), f2.getEType()));
    };
    final Map<EStructuralFeature, List<EStructuralFeature>> result = this.finder.findDuplicateFeaturesCustom(p, _function_1);
    final Function1<EClass, EList<EStructuralFeature>> _function_2 = (EClass it) -> {
      return it.getEStructuralFeatures();
    };
    final Iterable<EStructuralFeature> expected = Iterables.<EStructuralFeature>concat(IterableExtensions.<EClass, EList<EStructuralFeature>>map(this.EClasses(p), _function_2));
    final Iterable<EStructuralFeature> actual = Iterables.<EStructuralFeature>concat(result.values());
    this.<EStructuralFeature>assertIterable(actual, expected);
  }
  
  @Test
  public void test_findRedundantContainers() {
    EPackage _createEPackage = this.factory.createEPackage();
    final Procedure1<EPackage> _function = (EPackage it) -> {
      final EClass containedWithRedundant = this.createEClass(it, "ContainedWithRedundant");
      final EClass containedWithOpposite = this.createEClass(it, "ContainedWithOpposite");
      final EClass containedWithContained = this.createEClass(it, "ContainedWithContained");
      final EClass containedWithOptional = this.createEClass(it, "ContainedWithOptional");
      final EClass anotherClass = this.createEClass(it, "AnotherClass");
      final EClass containedWithUnrelated = this.createEClass(it, "Unrelated");
      EClass _createEClass = this.createEClass(it, "Container");
      final Procedure1<EClass> _function_1 = (EClass it_1) -> {
        EReference _createEReference = this.createEReference(it_1, "containedWithRedundant");
        final Procedure1<EReference> _function_2 = (EReference it_2) -> {
          it_2.setEType(containedWithRedundant);
          it_2.setContainment(true);
        };
        ObjectExtensions.<EReference>operator_doubleArrow(_createEReference, _function_2);
        EReference _createEReference_1 = this.createEReference(it_1, "containedWithUnrelated");
        final Procedure1<EReference> _function_3 = (EReference it_2) -> {
          it_2.setEType(containedWithUnrelated);
          it_2.setContainment(true);
        };
        ObjectExtensions.<EReference>operator_doubleArrow(_createEReference_1, _function_3);
        EReference _createEReference_2 = this.createEReference(it_1, "containedWithOpposite");
        final Procedure1<EReference> _function_4 = (EReference it_2) -> {
          it_2.setEType(containedWithOpposite);
          it_2.setContainment(true);
        };
        ObjectExtensions.<EReference>operator_doubleArrow(_createEReference_2, _function_4);
        EReference _createEReference_3 = this.createEReference(it_1, "containedWithOptional");
        final Procedure1<EReference> _function_5 = (EReference it_2) -> {
          it_2.setEType(containedWithOptional);
          it_2.setContainment(true);
        };
        ObjectExtensions.<EReference>operator_doubleArrow(_createEReference_3, _function_5);
      };
      final EClass container = ObjectExtensions.<EClass>operator_doubleArrow(_createEClass, _function_1);
      EReference _createEReference = this.createEReference(containedWithRedundant, "redundant");
      final Procedure1<EReference> _function_2 = (EReference it_1) -> {
        it_1.setEType(container);
        it_1.setLowerBound(1);
      };
      ObjectExtensions.<EReference>operator_doubleArrow(_createEReference, _function_2);
      EReference _createEReference_1 = this.createEReference(containedWithUnrelated, "unrelated");
      final Procedure1<EReference> _function_3 = (EReference it_1) -> {
        it_1.setEType(anotherClass);
        it_1.setLowerBound(1);
      };
      ObjectExtensions.<EReference>operator_doubleArrow(_createEReference_1, _function_3);
      EReference _createEReference_2 = this.createEReference(containedWithOpposite, "correctWithOpposite");
      final Procedure1<EReference> _function_4 = (EReference it_1) -> {
        it_1.setEType(container);
        it_1.setLowerBound(1);
        it_1.setEOpposite(IterableExtensions.<EReference>last(container.getEReferences()));
      };
      ObjectExtensions.<EReference>operator_doubleArrow(_createEReference_2, _function_4);
      EReference _createEReference_3 = this.createEReference(containedWithContained, "correctWithContainment");
      final Procedure1<EReference> _function_5 = (EReference it_1) -> {
        it_1.setEType(container);
        it_1.setLowerBound(1);
        it_1.setContainment(true);
      };
      ObjectExtensions.<EReference>operator_doubleArrow(_createEReference_3, _function_5);
      EReference _createEReference_4 = this.createEReference(containedWithOptional, "correctNotRequired");
      final Procedure1<EReference> _function_6 = (EReference it_1) -> {
        it_1.setEType(container);
      };
      ObjectExtensions.<EReference>operator_doubleArrow(_createEReference_4, _function_6);
    };
    final EPackage p = ObjectExtensions.<EPackage>operator_doubleArrow(_createEPackage, _function);
    final Iterable<Pair<EReference, EReference>> result = this.finder.findRedundantContainers(p);
    EReference _head = IterableExtensions.<EReference>head(IterableExtensions.<EClass>head(this.EClasses(p)).getEReferences());
    EReference _head_1 = IterableExtensions.<EReference>head(IterableExtensions.<EClass>last(this.EClasses(p)).getEReferences());
    final Pair<EReference, EReference> expected = Pair.<EReference, EReference>of(_head, _head_1);
    final Pair<EReference, EReference> actual = IterableExtensions.<Pair<EReference, EReference>>head(result);
    Assertions.<Pair<EReference, EReference>>assertThat(result).hasSize(1);
    Assert.assertNotNull(expected);
    Assert.assertNotNull(actual);
    Assert.assertEquals(expected, actual);
  }
  
  @Test
  public void test_findDeadClassifiers() {
    EPackage _createEPackage = this.factory.createEPackage();
    final Procedure1<EPackage> _function = (EPackage it) -> {
      this.createEClass(it, "Unused1");
      final EClass used1 = this.createEClass(it, "Used1");
      final EClass used2 = this.createEClass(it, "Used2");
      EClass _createEClass = this.createEClass(it, "Unused2");
      final Procedure1<EClass> _function_1 = (EClass it_1) -> {
        EReference _createEReference = this.createEReference(it_1, "used1");
        final Procedure1<EReference> _function_2 = (EReference it_2) -> {
          it_2.setEType(used1);
          it_2.setContainment(true);
        };
        ObjectExtensions.<EReference>operator_doubleArrow(_createEReference, _function_2);
        EReference _createEReference_1 = this.createEReference(it_1, "used2");
        final Procedure1<EReference> _function_3 = (EReference it_2) -> {
          it_2.setEType(used2);
          it_2.setContainment(false);
        };
        ObjectExtensions.<EReference>operator_doubleArrow(_createEReference_1, _function_3);
      };
      ObjectExtensions.<EClass>operator_doubleArrow(_createEClass, _function_1);
    };
    final EPackage p = ObjectExtensions.<EPackage>operator_doubleArrow(_createEPackage, _function);
    final List<EClassifier> result = this.finder.findDeadClassifiers(p);
    EClass _head = IterableExtensions.<EClass>head(this.EClasses(p));
    this.<EClassifier>assertIterable(result, Collections.<EClass>unmodifiableList(CollectionLiterals.<EClass>newArrayList(_head)));
  }
  
  @Test
  public void test_hasNoReferenceInThisPackage() {
    final EPackage otherPackage = this.factory.createEPackage();
    final EClass used1 = this.createEClass(otherPackage, "Used1");
    EPackage _createEPackage = this.factory.createEPackage();
    final Procedure1<EPackage> _function = (EPackage it) -> {
      EClass _createEClass = this.createEClass(it, "HasNoReferenceInThisPackage");
      final Procedure1<EClass> _function_1 = (EClass it_1) -> {
        EReference _createEReference = this.createEReference(it_1, "used1");
        final Procedure1<EReference> _function_2 = (EReference it_2) -> {
          it_2.setEType(used1);
          it_2.setContainment(false);
        };
        ObjectExtensions.<EReference>operator_doubleArrow(_createEReference, _function_2);
      };
      ObjectExtensions.<EClass>operator_doubleArrow(_createEClass, _function_1);
    };
    final EPackage p = ObjectExtensions.<EPackage>operator_doubleArrow(_createEPackage, _function);
    Assertions.assertThat(this.finder.hasNoReferenceInThisPackage(IterableExtensions.<EClass>head(this.EClasses(p)))).isTrue();
  }
  
  @Test
  public void test_findClassificationByHierarchy() {
    EPackage _createEPackage = this.factory.createEPackage();
    final Procedure1<EPackage> _function = (EPackage it) -> {
      final EClass base = this.createEClass(it, "Base");
      EClass _createEClass = this.createEClass(it, "Derived1");
      final Procedure1<EClass> _function_1 = (EClass it_1) -> {
        EList<EClass> _eSuperTypes = it_1.getESuperTypes();
        _eSuperTypes.add(base);
      };
      ObjectExtensions.<EClass>operator_doubleArrow(_createEClass, _function_1);
      EClass _createEClass_1 = this.createEClass(it, "Derived2");
      final Procedure1<EClass> _function_2 = (EClass it_1) -> {
        EList<EClass> _eSuperTypes = it_1.getESuperTypes();
        _eSuperTypes.add(base);
      };
      ObjectExtensions.<EClass>operator_doubleArrow(_createEClass_1, _function_2);
      EClass _createEClass_2 = this.createEClass(it, "DerivedOK");
      final Procedure1<EClass> _function_3 = (EClass it_1) -> {
        EList<EClass> _eSuperTypes = it_1.getESuperTypes();
        _eSuperTypes.add(base);
        EAttribute _createEAttribute = this.createEAttribute(it_1, "anAttribute");
        final Procedure1<EAttribute> _function_4 = (EAttribute it_2) -> {
          it_2.setEType(this.stringDataType);
        };
        ObjectExtensions.<EAttribute>operator_doubleArrow(_createEAttribute, _function_4);
      };
      ObjectExtensions.<EClass>operator_doubleArrow(_createEClass_2, _function_3);
      EClass _createEClass_3 = this.createEClass(it, "DerivedOK2");
      final Procedure1<EClass> _function_4 = (EClass it_1) -> {
        EList<EClass> _eSuperTypes = it_1.getESuperTypes();
        _eSuperTypes.add(base);
      };
      final EClass referenced = ObjectExtensions.<EClass>operator_doubleArrow(_createEClass_3, _function_4);
      EClass _createEClass_4 = this.createEClass(it, "Another");
      final Procedure1<EClass> _function_5 = (EClass it_1) -> {
        EReference _createEReference = this.createEReference(it_1, "aRef");
        final Procedure1<EReference> _function_6 = (EReference it_2) -> {
          it_2.setEType(referenced);
        };
        ObjectExtensions.<EReference>operator_doubleArrow(_createEReference, _function_6);
      };
      final EClass another = ObjectExtensions.<EClass>operator_doubleArrow(_createEClass_4, _function_5);
      EClass _createEClass_5 = this.createEClass(it, "DerivedOK3");
      final Procedure1<EClass> _function_6 = (EClass it_1) -> {
        EList<EClass> _eSuperTypes = it_1.getESuperTypes();
        _eSuperTypes.add(base);
        EList<EClass> _eSuperTypes_1 = it_1.getESuperTypes();
        _eSuperTypes_1.add(another);
      };
      ObjectExtensions.<EClass>operator_doubleArrow(_createEClass_5, _function_6);
    };
    final EPackage p = ObjectExtensions.<EPackage>operator_doubleArrow(_createEPackage, _function);
    final Map<EClass, List<EClass>> result = this.finder.findClassificationByHierarchy(p);
    EClass _head = IterableExtensions.<EClass>head(this.EClasses(p));
    ArrayList<EClass> _newArrayList = CollectionLiterals.<EClass>newArrayList(
      ((EClass[])Conversions.unwrapArray(this.EClasses(p), EClass.class))[1], 
      ((EClass[])Conversions.unwrapArray(this.EClasses(p), EClass.class))[2]);
    Pair<EClass, ArrayList<EClass>> _mappedTo = Pair.<EClass, ArrayList<EClass>>of(_head, _newArrayList);
    Assertions.<EClass, List<EClass>>assertThat(result).containsExactlyEntriesOf(
      CollectionLiterals.<EClass, ArrayList<EClass>>newHashMap(_mappedTo));
  }
  
  @Test
  public void test_findClassificationByHierarchy_withOneSubclass() {
    EPackage _createEPackage = this.factory.createEPackage();
    final Procedure1<EPackage> _function = (EPackage it) -> {
      final EClass base = this.createEClass(it, "Base");
      EClass _createEClass = this.createEClass(it, "Derived1");
      final Procedure1<EClass> _function_1 = (EClass it_1) -> {
        EList<EClass> _eSuperTypes = it_1.getESuperTypes();
        _eSuperTypes.add(base);
      };
      ObjectExtensions.<EClass>operator_doubleArrow(_createEClass, _function_1);
    };
    final EPackage p = ObjectExtensions.<EPackage>operator_doubleArrow(_createEPackage, _function);
    final Map<EClass, List<EClass>> result = this.finder.findClassificationByHierarchy(p);
    Assertions.<EClass, List<EClass>>assertThat(result).isEmpty();
  }
  
  @Test
  public void test_findConcreteAbstractMetaclasses() {
    EPackage _createEPackage = this.factory.createEPackage();
    final Procedure1<EPackage> _function = (EPackage it) -> {
      final EClass base = this.createEClass(it, "ConcreteAbstractMetaclass");
      EClass _createEClass = this.createEClass(it, "CorrectAbstractMetaclass");
      final Procedure1<EClass> _function_1 = (EClass it_1) -> {
        it_1.setAbstract(true);
      };
      final EClass other = ObjectExtensions.<EClass>operator_doubleArrow(_createEClass, _function_1);
      final EClass referred = this.createEClass(it, "NonBaseClass");
      EClass _createEClass_1 = this.createEClass(it, "Derived1");
      final Procedure1<EClass> _function_2 = (EClass it_1) -> {
        EList<EClass> _eSuperTypes = it_1.getESuperTypes();
        _eSuperTypes.add(base);
      };
      ObjectExtensions.<EClass>operator_doubleArrow(_createEClass_1, _function_2);
      EClass _createEClass_2 = this.createEClass(it, "Derived2");
      final Procedure1<EClass> _function_3 = (EClass it_1) -> {
        EList<EClass> _eSuperTypes = it_1.getESuperTypes();
        _eSuperTypes.add(other);
      };
      ObjectExtensions.<EClass>operator_doubleArrow(_createEClass_2, _function_3);
      EClass _createEClass_3 = this.createEClass(it, "Another");
      final Procedure1<EClass> _function_4 = (EClass it_1) -> {
        EReference _createEReference = this.createEReference(it_1, "aRef");
        final Procedure1<EReference> _function_5 = (EReference it_2) -> {
          it_2.setEType(referred);
        };
        ObjectExtensions.<EReference>operator_doubleArrow(_createEReference, _function_5);
      };
      ObjectExtensions.<EClass>operator_doubleArrow(_createEClass_3, _function_4);
    };
    final EPackage p = ObjectExtensions.<EPackage>operator_doubleArrow(_createEPackage, _function);
    Iterable<EClass> result = this.finder.findConcreteAbstractMetaclasses(p);
    EClass _head = IterableExtensions.<EClass>head(this.EClasses(p));
    this.<EClass>assertIterable(result, Collections.<EClass>unmodifiableList(CollectionLiterals.<EClass>newArrayList(_head)));
  }
  
  @Test
  public void test_findAbstractConcreteMetaclasses() {
    EPackage _createEPackage = this.factory.createEPackage();
    final Procedure1<EPackage> _function = (EPackage it) -> {
      EClass _createEClass = this.createEClass(it, "AbstractConcreteMetaclass");
      final Procedure1<EClass> _function_1 = (EClass it_1) -> {
        it_1.setAbstract(true);
      };
      ObjectExtensions.<EClass>operator_doubleArrow(_createEClass, _function_1);
      EClass _createEClass_1 = this.createEClass(it, "AbstractMetaclass");
      final Procedure1<EClass> _function_2 = (EClass it_1) -> {
        it_1.setAbstract(true);
      };
      final EClass base = ObjectExtensions.<EClass>operator_doubleArrow(_createEClass_1, _function_2);
      EClass _createEClass_2 = this.createEClass(it, "Derived1");
      final Procedure1<EClass> _function_3 = (EClass it_1) -> {
        EList<EClass> _eSuperTypes = it_1.getESuperTypes();
        _eSuperTypes.add(base);
      };
      ObjectExtensions.<EClass>operator_doubleArrow(_createEClass_2, _function_3);
    };
    final EPackage p = ObjectExtensions.<EPackage>operator_doubleArrow(_createEPackage, _function);
    Iterable<EClass> result = this.finder.findAbstractConcreteMetaclasses(p);
    EClass _head = IterableExtensions.<EClass>head(this.EClasses(p));
    this.<EClass>assertIterable(result, Collections.<EClass>unmodifiableList(CollectionLiterals.<EClass>newArrayList(_head)));
  }
  
  @Test
  public void test_findAbstractSubclassesOfConcreteSuperclasses() {
    EPackage _createEPackage = this.factory.createEPackage();
    final Procedure1<EPackage> _function = (EPackage it) -> {
      EClass _createEClass = this.createEClass(it, "AbstractSuperclass");
      final Procedure1<EClass> _function_1 = (EClass it_1) -> {
        it_1.setAbstract(true);
      };
      final EClass abstractSuperclass = ObjectExtensions.<EClass>operator_doubleArrow(_createEClass, _function_1);
      final EClass concreteSuperclass1 = this.createEClass(it, "ConcreteSuperclass1");
      final EClass concreteSuperclass2 = this.createEClass(it, "ConcreteSuperclass2");
      EClass _createEClass_1 = this.createEClass(it, "WithoutSmell");
      final Procedure1<EClass> _function_2 = (EClass it_1) -> {
        it_1.setAbstract(true);
        EList<EClass> _eSuperTypes = it_1.getESuperTypes();
        Iterables.<EClass>addAll(_eSuperTypes, Collections.<EClass>unmodifiableList(CollectionLiterals.<EClass>newArrayList(concreteSuperclass1, abstractSuperclass)));
      };
      ObjectExtensions.<EClass>operator_doubleArrow(_createEClass_1, _function_2);
      EClass _createEClass_2 = this.createEClass(it, "WithSmell");
      final Procedure1<EClass> _function_3 = (EClass it_1) -> {
        it_1.setAbstract(true);
        EList<EClass> _eSuperTypes = it_1.getESuperTypes();
        Iterables.<EClass>addAll(_eSuperTypes, Collections.<EClass>unmodifiableList(CollectionLiterals.<EClass>newArrayList(concreteSuperclass1, concreteSuperclass2)));
      };
      ObjectExtensions.<EClass>operator_doubleArrow(_createEClass_2, _function_3);
    };
    final EPackage p = ObjectExtensions.<EPackage>operator_doubleArrow(_createEPackage, _function);
    Iterable<EClass> result = this.finder.findAbstractSubclassesOfConcreteSuperclasses(p);
    Assertions.<EClass>assertThat(result).containsOnly(IterableExtensions.<EClass>last(this.EClasses(p)));
  }
  
  @Test
  public void test_directSubclasses() {
    EPackage _createEPackage = this.factory.createEPackage();
    final Procedure1<EPackage> _function = (EPackage it) -> {
      final EClass superclass = this.createEClass(it, "ASuperclass");
      EClass _createEClass = this.createEClass(it, "ASubclass1");
      final Procedure1<EClass> _function_1 = (EClass it_1) -> {
        EList<EClass> _eSuperTypes = it_1.getESuperTypes();
        _eSuperTypes.add(superclass);
      };
      final EClass subclass1 = ObjectExtensions.<EClass>operator_doubleArrow(_createEClass, _function_1);
      EClass _createEClass_1 = this.createEClass(it, "ASubclass1Subclass");
      final Procedure1<EClass> _function_2 = (EClass it_1) -> {
        EList<EClass> _eSuperTypes = it_1.getESuperTypes();
        _eSuperTypes.add(subclass1);
      };
      ObjectExtensions.<EClass>operator_doubleArrow(_createEClass_1, _function_2);
      EClass _createEClass_2 = this.createEClass(it, "ASubclass2");
      final Procedure1<EClass> _function_3 = (EClass it_1) -> {
        EList<EClass> _eSuperTypes = it_1.getESuperTypes();
        _eSuperTypes.add(superclass);
      };
      ObjectExtensions.<EClass>operator_doubleArrow(_createEClass_2, _function_3);
    };
    final EPackage p = ObjectExtensions.<EPackage>operator_doubleArrow(_createEPackage, _function);
    final Function1<EClass, String> _function_1 = (EClass it) -> {
      return it.getName();
    };
    Assertions.<String>assertThat(IterableExtensions.<EClass, String>map(this.finder.directSubclasses(IterableExtensions.<EClass>head(this.EClasses(p))), _function_1)).containsExactlyInAnyOrder("ASubclass1", "ASubclass2");
    final Function1<EClass, String> _function_2 = (EClass it) -> {
      return it.getName();
    };
    Assertions.<String>assertThat(IterableExtensions.<EClass, String>map(this.finder.directSubclasses(((EClass[])Conversions.unwrapArray(this.EClasses(p), EClass.class))[1]), _function_2)).containsExactlyInAnyOrder("ASubclass1Subclass");
    Assertions.<EClass>assertThat(this.finder.directSubclasses(((EClass[])Conversions.unwrapArray(this.EClasses(p), EClass.class))[2])).isEmpty();
  }
  
  @Test
  public void test_findDuplicateFeaturesInSubclasses() {
    EPackage _createEPackage = this.factory.createEPackage();
    final Procedure1<EPackage> _function = (EPackage it) -> {
      final EClass superclassWithSuplicatesInSubclasses = this.createEClass(it, "SuperClassWithDuplicatesInSubclasses");
      EClass _createEClass = this.createEClass(it, "C1");
      final Procedure1<EClass> _function_1 = (EClass it_1) -> {
        EList<EClass> _eSuperTypes = it_1.getESuperTypes();
        _eSuperTypes.add(superclassWithSuplicatesInSubclasses);
        EAttribute _createEAttribute = this.createEAttribute(it_1, "A1");
        final Procedure1<EAttribute> _function_2 = (EAttribute it_2) -> {
          it_2.setEType(this.stringDataType);
        };
        ObjectExtensions.<EAttribute>operator_doubleArrow(_createEAttribute, _function_2);
      };
      ObjectExtensions.<EClass>operator_doubleArrow(_createEClass, _function_1);
      EClass _createEClass_1 = this.createEClass(it, "C2");
      final Procedure1<EClass> _function_2 = (EClass it_1) -> {
        EList<EClass> _eSuperTypes = it_1.getESuperTypes();
        _eSuperTypes.add(superclassWithSuplicatesInSubclasses);
        EAttribute _createEAttribute = this.createEAttribute(it_1, "A1");
        final Procedure1<EAttribute> _function_3 = (EAttribute it_2) -> {
          it_2.setEType(this.stringDataType);
        };
        ObjectExtensions.<EAttribute>operator_doubleArrow(_createEAttribute, _function_3);
      };
      ObjectExtensions.<EClass>operator_doubleArrow(_createEClass_1, _function_2);
      final EClass superclassWithoutSuplicatesInAllSubclasses = this.createEClass(it, "SuperClassWithoutDuplicatesInAllSubclasses");
      EClass _createEClass_2 = this.createEClass(it, "D1");
      final Procedure1<EClass> _function_3 = (EClass it_1) -> {
        EList<EClass> _eSuperTypes = it_1.getESuperTypes();
        _eSuperTypes.add(superclassWithoutSuplicatesInAllSubclasses);
        EAttribute _createEAttribute = this.createEAttribute(it_1, "A1");
        final Procedure1<EAttribute> _function_4 = (EAttribute it_2) -> {
          it_2.setEType(this.stringDataType);
        };
        ObjectExtensions.<EAttribute>operator_doubleArrow(_createEAttribute, _function_4);
      };
      ObjectExtensions.<EClass>operator_doubleArrow(_createEClass_2, _function_3);
      EClass _createEClass_3 = this.createEClass(it, "D2");
      final Procedure1<EClass> _function_4 = (EClass it_1) -> {
        EList<EClass> _eSuperTypes = it_1.getESuperTypes();
        _eSuperTypes.add(superclassWithoutSuplicatesInAllSubclasses);
        EAttribute _createEAttribute = this.createEAttribute(it_1, "A1");
        final Procedure1<EAttribute> _function_5 = (EAttribute it_2) -> {
          it_2.setEType(this.stringDataType);
        };
        ObjectExtensions.<EAttribute>operator_doubleArrow(_createEAttribute, _function_5);
      };
      ObjectExtensions.<EClass>operator_doubleArrow(_createEClass_3, _function_4);
      EClass _createEClass_4 = this.createEClass(it, "D3");
      final Procedure1<EClass> _function_5 = (EClass it_1) -> {
        EList<EClass> _eSuperTypes = it_1.getESuperTypes();
        _eSuperTypes.add(superclassWithoutSuplicatesInAllSubclasses);
        EAttribute _createEAttribute = this.createEAttribute(it_1, "A1");
        final Procedure1<EAttribute> _function_6 = (EAttribute it_2) -> {
          it_2.setEType(this.intDataType);
        };
        ObjectExtensions.<EAttribute>operator_doubleArrow(_createEAttribute, _function_6);
      };
      ObjectExtensions.<EClass>operator_doubleArrow(_createEClass_4, _function_5);
    };
    final EPackage p = ObjectExtensions.<EPackage>operator_doubleArrow(_createEPackage, _function);
    final LinkedHashMap<EClass, Map<EStructuralFeature, List<EStructuralFeature>>> result = this.finder.findDuplicateFeaturesInSubclasses(p);
    final Function1<EClass, EList<EStructuralFeature>> _function_1 = (EClass it) -> {
      return it.getEStructuralFeatures();
    };
    final Iterable<EStructuralFeature> expected = Iterables.<EStructuralFeature>concat(IterableExtensions.<EClass, EList<EStructuralFeature>>map(IterableExtensions.<EClass>take(this.EClasses(p), 3), _function_1));
    final Iterable<EStructuralFeature> actual = Iterables.<EStructuralFeature>concat(result.get(IterableExtensions.<EClass>head(this.EClasses(p))).values());
    this.<EStructuralFeature>assertIterable(actual, expected);
    final EClass notMatched = ((EClass[])Conversions.unwrapArray(this.EClasses(p), EClass.class))[3];
    Assertions.<EStructuralFeature, List<EStructuralFeature>>assertThat(result.get(notMatched)).isNull();
  }
  
  private <T extends ENamedElement> void assertIterable(final Iterable<T> actual, final Iterable<? extends T> expected) {
    Assertions.<T>assertThat(actual).containsExactlyInAnyOrder(((T[])Conversions.unwrapArray(expected, ENamedElement.class)));
  }
}
