package edelta.refactorings.lib.tests;

import com.google.common.base.Objects;
import com.google.common.collect.Iterables;
import edelta.lib.AbstractEdelta;
import edelta.lib.EdeltaLibrary;
import edelta.refactorings.lib.EdeltaBadSmellsFinder;
import edelta.refactorings.lib.tests.AbstractTest;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.IterableAssert;
import org.assertj.core.util.Maps;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.xtext.xbase.lib.CollectionLiterals;
import org.eclipse.xtext.xbase.lib.Functions.Function1;
import org.eclipse.xtext.xbase.lib.IterableExtensions;
import org.eclipse.xtext.xbase.lib.ObjectExtensions;
import org.eclipse.xtext.xbase.lib.Pair;
import org.eclipse.xtext.xbase.lib.Procedures.Procedure1;
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
    final Consumer<EPackage> _function = (EPackage it) -> {
      final Consumer<EClass> _function_1 = (EClass it_1) -> {
        EdeltaLibrary.addNewEAttribute(it_1, "a1", this.stringDataType);
      };
      EdeltaLibrary.addNewEClass(it, "C1", _function_1);
      final Consumer<EClass> _function_2 = (EClass it_1) -> {
        EdeltaLibrary.addNewEAttribute(it_1, "a1", this.intDataType);
      };
      EdeltaLibrary.addNewEClass(it, "C2", _function_2);
    };
    final EPackage p = this.createEPackage("p", _function);
    final Map<EStructuralFeature, List<EStructuralFeature>> result = this.finder.findDuplicateFeatures(p);
    Assertions.<EStructuralFeature, List<EStructuralFeature>>assertThat(result).isEmpty();
  }
  
  @Test
  public void test_findDuplicateFeatures_withDuplicates() {
    final Consumer<EPackage> _function = (EPackage it) -> {
      final Consumer<EClass> _function_1 = (EClass it_1) -> {
        EdeltaLibrary.addNewEAttribute(it_1, "a1", this.stringDataType);
      };
      EdeltaLibrary.addNewEClass(it, "C1", _function_1);
      final Consumer<EClass> _function_2 = (EClass it_1) -> {
        EdeltaLibrary.addNewEAttribute(it_1, "a1", this.stringDataType);
      };
      EdeltaLibrary.addNewEClass(it, "C2", _function_2);
    };
    final EPackage p = this.createEPackage("p", _function);
    final Map<EStructuralFeature, List<EStructuralFeature>> result = this.finder.findDuplicateFeatures(p);
    EStructuralFeature _findEStructuralFeature = this.findEStructuralFeature(p, "C1", "a1");
    EStructuralFeature _findEStructuralFeature_1 = this.findEStructuralFeature(p, "C2", "a1");
    Assertions.<EStructuralFeature, List<EStructuralFeature>>assertThat(result).containsExactly(
      Assertions.<EStructuralFeature, List<EStructuralFeature>>entry(this.findEStructuralFeature(p, "C1", "a1"), 
        Collections.<EStructuralFeature>unmodifiableList(CollectionLiterals.<EStructuralFeature>newArrayList(_findEStructuralFeature, _findEStructuralFeature_1))));
  }
  
  @Test
  public void test_findDuplicateFeatures_withDifferingAttributesByLowerBound() {
    final Consumer<EPackage> _function = (EPackage it) -> {
      final Consumer<EClass> _function_1 = (EClass it_1) -> {
        final Consumer<EAttribute> _function_2 = (EAttribute it_2) -> {
          it_2.setLowerBound(1);
        };
        EdeltaLibrary.addNewEAttribute(it_1, "a1", this.stringDataType, _function_2);
      };
      EdeltaLibrary.addNewEClass(it, "C1", _function_1);
      final Consumer<EClass> _function_2 = (EClass it_1) -> {
        final Consumer<EAttribute> _function_3 = (EAttribute it_2) -> {
          it_2.setLowerBound(2);
        };
        EdeltaLibrary.addNewEAttribute(it_1, "a1", this.stringDataType, _function_3);
      };
      EdeltaLibrary.addNewEClass(it, "C2", _function_2);
    };
    final EPackage p = this.createEPackage("p", _function);
    final Map<EStructuralFeature, List<EStructuralFeature>> result = this.finder.findDuplicateFeatures(p);
    Assertions.<EStructuralFeature, List<EStructuralFeature>>assertThat(result).isEmpty();
  }
  
  @Test
  public void test_findDuplicateFeatures_withDifferingContainment() {
    final Consumer<EPackage> _function = (EPackage it) -> {
      EClass _addNewEClass = EdeltaLibrary.addNewEClass(it, "C1");
      final Procedure1<EClass> _function_1 = (EClass it_1) -> {
        EdeltaLibrary.addNewContainmentEReference(it_1, "r1", this.eClassReference);
      };
      ObjectExtensions.<EClass>operator_doubleArrow(_addNewEClass, _function_1);
      EClass _addNewEClass_1 = EdeltaLibrary.addNewEClass(it, "C2");
      final Procedure1<EClass> _function_2 = (EClass it_1) -> {
        EdeltaLibrary.addNewEReference(it_1, "r1", this.eClassReference);
      };
      ObjectExtensions.<EClass>operator_doubleArrow(_addNewEClass_1, _function_2);
    };
    final EPackage p = this.createEPackage("p", _function);
    final Map<EStructuralFeature, List<EStructuralFeature>> result = this.finder.findDuplicateFeatures(p);
    Assertions.<EStructuralFeature, List<EStructuralFeature>>assertThat(result).isEmpty();
  }
  
  @Test
  public void test_findDuplicateFeatures_withCustomEqualityPredicate() {
    final Consumer<EPackage> _function = (EPackage it) -> {
      final Consumer<EClass> _function_1 = (EClass it_1) -> {
        final Consumer<EAttribute> _function_2 = (EAttribute it_2) -> {
          it_2.setLowerBound(1);
        };
        EdeltaLibrary.addNewEAttribute(it_1, "a1", this.stringDataType, _function_2);
      };
      EdeltaLibrary.addNewEClass(it, "C1", _function_1);
      final Consumer<EClass> _function_2 = (EClass it_1) -> {
        final Consumer<EAttribute> _function_3 = (EAttribute it_2) -> {
          it_2.setLowerBound(2);
        };
        EdeltaLibrary.addNewEAttribute(it_1, "a1", this.stringDataType, _function_3);
      };
      EdeltaLibrary.addNewEClass(it, "C2", _function_2);
    };
    final EPackage p = this.createEPackage("p", _function);
    final BiPredicate<EStructuralFeature, EStructuralFeature> _function_1 = (EStructuralFeature f1, EStructuralFeature f2) -> {
      return (Objects.equal(f1.getName(), f2.getName()) && Objects.equal(f1.getEType(), f2.getEType()));
    };
    final Map<EStructuralFeature, List<EStructuralFeature>> result = this.finder.findDuplicateFeaturesCustom(p, _function_1);
    EStructuralFeature _findEStructuralFeature = this.findEStructuralFeature(p, "C1", "a1");
    EStructuralFeature _findEStructuralFeature_1 = this.findEStructuralFeature(p, "C2", "a1");
    Assertions.<EStructuralFeature, List<EStructuralFeature>>assertThat(result).containsExactly(
      Assertions.<EStructuralFeature, List<EStructuralFeature>>entry(this.findEStructuralFeature(p, "C1", "a1"), 
        Collections.<EStructuralFeature>unmodifiableList(CollectionLiterals.<EStructuralFeature>newArrayList(_findEStructuralFeature, _findEStructuralFeature_1))));
  }
  
  @Test
  public void test_findRedundantContainers() {
    final Consumer<EPackage> _function = (EPackage it) -> {
      final EClass containedWithRedundant = EdeltaLibrary.addNewEClass(it, "ContainedWithRedundant");
      final EClass containedWithOpposite = EdeltaLibrary.addNewEClass(it, "ContainedWithOpposite");
      final EClass containedWithContained = EdeltaLibrary.addNewEClass(it, "ContainedWithContained");
      final EClass containedWithOptional = EdeltaLibrary.addNewEClass(it, "ContainedWithOptional");
      final EClass anotherClass = EdeltaLibrary.addNewEClass(it, "AnotherClass");
      final EClass containedWithUnrelated = EdeltaLibrary.addNewEClass(it, "Unrelated");
      final Consumer<EClass> _function_1 = (EClass it_1) -> {
        EdeltaLibrary.addNewContainmentEReference(it_1, "containedWithRedundant", containedWithRedundant);
        EdeltaLibrary.addNewContainmentEReference(it_1, "containedWithUnrelated", containedWithUnrelated);
        EdeltaLibrary.addNewContainmentEReference(it_1, "containedWithOpposite", containedWithOpposite);
        EdeltaLibrary.addNewContainmentEReference(it_1, "containedWithOptional", containedWithOptional);
      };
      final EClass container = EdeltaLibrary.addNewEClass(it, "Container", _function_1);
      final Consumer<EReference> _function_2 = (EReference it_1) -> {
        it_1.setLowerBound(1);
      };
      EdeltaLibrary.addNewEReference(containedWithRedundant, "redundant", container, _function_2);
      final Consumer<EReference> _function_3 = (EReference it_1) -> {
        it_1.setLowerBound(1);
      };
      EdeltaLibrary.addNewEReference(containedWithUnrelated, "unrelated", anotherClass, _function_3);
      final Consumer<EReference> _function_4 = (EReference it_1) -> {
        it_1.setLowerBound(1);
        it_1.setEOpposite(IterableExtensions.<EReference>last(container.getEReferences()));
      };
      EdeltaLibrary.addNewEReference(containedWithOpposite, "correctWithOpposite", container, _function_4);
      final Consumer<EReference> _function_5 = (EReference it_1) -> {
        it_1.setLowerBound(1);
      };
      EdeltaLibrary.addNewContainmentEReference(containedWithContained, "correctWithContainment", container, _function_5);
      EdeltaLibrary.addNewEReference(containedWithOptional, "correctNotRequired", container);
    };
    final EPackage p = this.createEPackage("p", _function);
    final Iterable<Pair<EReference, EReference>> result = this.finder.findRedundantContainers(p);
    IterableAssert<Pair<EReference, EReference>> _assertThat = Assertions.<Pair<EReference, EReference>>assertThat(result);
    EReference _findEReference = this.findEReference(p, "ContainedWithRedundant", "redundant");
    EReference _findEReference_1 = this.findEReference(p, "Container", "containedWithRedundant");
    Pair<EReference, EReference> _mappedTo = Pair.<EReference, EReference>of(_findEReference, _findEReference_1);
    _assertThat.containsExactly(_mappedTo);
  }
  
  @Test
  public void test_findDeadClassifiers() {
    final Consumer<EPackage> _function = (EPackage it) -> {
      EdeltaLibrary.addNewEClass(it, "Unused1");
      final EClass used1 = EdeltaLibrary.addNewEClass(it, "Used1");
      final EClass used2 = EdeltaLibrary.addNewEClass(it, "Used2");
      final Consumer<EClass> _function_1 = (EClass it_1) -> {
        EdeltaLibrary.addNewContainmentEReference(it_1, "used1", used1);
        EdeltaLibrary.addNewEReference(it_1, "used2", used2);
      };
      EdeltaLibrary.addNewEClass(it, "Unused2", _function_1);
    };
    final EPackage p = this.createEPackage("p", _function);
    final List<EClassifier> result = this.finder.findDeadClassifiers(p);
    Assertions.<EClassifier>assertThat(result).containsExactly(this.findEClass(p, "Unused1"));
  }
  
  @Test
  public void test_hasNoReferenceInThisPackage() {
    final EPackage otherPackage = this.createEPackage("otherPackage");
    final EClass used1 = EdeltaLibrary.addNewEClass(otherPackage, "Used1");
    final Consumer<EPackage> _function = (EPackage it) -> {
      final Consumer<EClass> _function_1 = (EClass it_1) -> {
        EdeltaLibrary.addNewEReference(it_1, "used1", used1);
      };
      EdeltaLibrary.addNewEClass(it, "HasNoReferenceInThisPackage", _function_1);
    };
    final EPackage p = this.createEPackage("p", _function);
    Assertions.assertThat(this.finder.hasNoReferenceInThisPackage(IterableExtensions.<EClass>head(this.EClasses(p)))).isTrue();
  }
  
  @Test
  public void test_findClassificationByHierarchy() {
    final Consumer<EPackage> _function = (EPackage it) -> {
      final EClass base = EdeltaLibrary.addNewEClass(it, "Base");
      EdeltaLibrary.addNewSubclass(base, "Derived1");
      EdeltaLibrary.addNewSubclass(base, "Derived2");
      final Consumer<EClass> _function_1 = (EClass it_1) -> {
        EdeltaLibrary.addNewEAttribute(it_1, "anAttribute", this.stringDataType);
      };
      EdeltaLibrary.addNewSubclass(base, "DerivedOK", _function_1);
      final EClass referenced = EdeltaLibrary.addNewSubclass(base, "DerivedOK2");
      final Consumer<EClass> _function_2 = (EClass it_1) -> {
        EdeltaLibrary.addNewEReference(it_1, "aRef", referenced);
      };
      final EClass another = EdeltaLibrary.addNewEClass(it, "Another", _function_2);
      final Consumer<EClass> _function_3 = (EClass it_1) -> {
        EList<EClass> _eSuperTypes = it_1.getESuperTypes();
        _eSuperTypes.add(another);
      };
      EdeltaLibrary.addNewSubclass(base, "DerivedOK3", _function_3);
    };
    final EPackage p = this.createEPackage("p", _function);
    final Map<EClass, List<EClass>> result = this.finder.findClassificationByHierarchy(p);
    EClass _findEClass = this.findEClass(p, "Derived1");
    EClass _findEClass_1 = this.findEClass(p, "Derived2");
    Assertions.<EClass, List<EClass>>assertThat(result).containsExactly(
      Assertions.<EClass, List<EClass>>entry(this.findEClass(p, "Base"), 
        Collections.<EClass>unmodifiableList(CollectionLiterals.<EClass>newArrayList(_findEClass, _findEClass_1))));
  }
  
  @Test
  public void test_findClassificationByHierarchy_withOneSubclass() {
    final Consumer<EPackage> _function = (EPackage it) -> {
      final Consumer<EClass> _function_1 = (EClass it_1) -> {
        EdeltaLibrary.addNewSubclass(it_1, "Derived1");
      };
      EdeltaLibrary.addNewEClass(it, "Base", _function_1);
    };
    final EPackage p = this.createEPackage("p", _function);
    final Map<EClass, List<EClass>> result = this.finder.findClassificationByHierarchy(p);
    Assertions.<EClass, List<EClass>>assertThat(result).isEmpty();
  }
  
  @Test
  public void test_findConcreteAbstractMetaclasses() {
    final Consumer<EPackage> _function = (EPackage it) -> {
      final EClass base = EdeltaLibrary.addNewEClass(it, "ConcreteAbstractMetaclass");
      final EClass other = EdeltaLibrary.addNewAbstractEClass(it, "CorrectAbstractMetaclass");
      final EClass referred = EdeltaLibrary.addNewEClass(it, "NonBaseClass");
      EdeltaLibrary.addNewSubclass(base, "Derived1");
      EdeltaLibrary.addNewSubclass(other, "Derived2");
      final Consumer<EClass> _function_1 = (EClass it_1) -> {
        EdeltaLibrary.addNewEReference(it_1, "aRef", referred);
      };
      EdeltaLibrary.addNewEClass(it, "Another", _function_1);
    };
    final EPackage p = this.createEPackage("p", _function);
    Iterable<EClass> result = this.finder.findConcreteAbstractMetaclasses(p);
    Assertions.<EClass>assertThat(result).containsExactly(this.findEClass(p, "ConcreteAbstractMetaclass"));
  }
  
  @Test
  public void test_findAbstractConcreteMetaclasses() {
    final Consumer<EPackage> _function = (EPackage it) -> {
      EdeltaLibrary.addNewAbstractEClass(it, "AbstractConcreteMetaclass");
      final Consumer<EClass> _function_1 = (EClass it_1) -> {
        EdeltaLibrary.addNewSubclass(it_1, "Derived1");
      };
      EdeltaLibrary.addNewAbstractEClass(it, "AbstractMetaclass", _function_1);
    };
    final EPackage p = this.createEPackage("p", _function);
    Iterable<EClass> result = this.finder.findAbstractConcreteMetaclasses(p);
    Assertions.<EClass>assertThat(result).containsExactly(this.findEClass(p, "AbstractConcreteMetaclass"));
  }
  
  @Test
  public void test_findAbstractSubclassesOfConcreteSuperclasses() {
    final Consumer<EPackage> _function = (EPackage it) -> {
      final EClass abstractSuperclass = EdeltaLibrary.addNewAbstractEClass(it, "AbstractSuperclass");
      final EClass concreteSuperclass1 = EdeltaLibrary.addNewEClass(it, "ConcreteSuperclass1");
      final EClass concreteSuperclass2 = EdeltaLibrary.addNewEClass(it, "ConcreteSuperclass2");
      final Consumer<EClass> _function_1 = (EClass it_1) -> {
        EList<EClass> _eSuperTypes = it_1.getESuperTypes();
        Iterables.<EClass>addAll(_eSuperTypes, Collections.<EClass>unmodifiableList(CollectionLiterals.<EClass>newArrayList(concreteSuperclass1, abstractSuperclass)));
      };
      EdeltaLibrary.addNewAbstractEClass(it, "WithoutSmell", _function_1);
      final Consumer<EClass> _function_2 = (EClass it_1) -> {
        EList<EClass> _eSuperTypes = it_1.getESuperTypes();
        Iterables.<EClass>addAll(_eSuperTypes, Collections.<EClass>unmodifiableList(CollectionLiterals.<EClass>newArrayList(concreteSuperclass1, concreteSuperclass2)));
      };
      EdeltaLibrary.addNewAbstractEClass(it, "WithSmell", _function_2);
    };
    final EPackage p = this.createEPackage("p", _function);
    Iterable<EClass> result = this.finder.findAbstractSubclassesOfConcreteSuperclasses(p);
    Assertions.<EClass>assertThat(result).containsOnly(this.findEClass(p, "WithSmell"));
  }
  
  @Test
  public void test_directSubclasses() {
    final Consumer<EPackage> _function = (EPackage it) -> {
      final EClass superclass = EdeltaLibrary.addNewEClass(it, "ASuperclass");
      final EClass subclass1 = EdeltaLibrary.addNewSubclass(superclass, "ASubclass1");
      EdeltaLibrary.addNewSubclass(subclass1, "ASubclass1Subclass");
      EdeltaLibrary.addNewSubclass(superclass, "ASubclass2");
    };
    final EPackage p = this.createEPackage("p", _function);
    final Function1<EClass, String> _function_1 = (EClass it) -> {
      return it.getName();
    };
    Assertions.<String>assertThat(IterableExtensions.<EClass, String>map(this.finder.directSubclasses(this.findEClass(p, "ASuperclass")), _function_1)).containsExactlyInAnyOrder("ASubclass1", "ASubclass2");
    final Function1<EClass, String> _function_2 = (EClass it) -> {
      return it.getName();
    };
    Assertions.<String>assertThat(IterableExtensions.<EClass, String>map(this.finder.directSubclasses(this.findEClass(p, "ASubclass1")), _function_2)).containsExactlyInAnyOrder("ASubclass1Subclass");
    Assertions.<EClass>assertThat(this.finder.directSubclasses(this.findEClass(p, "ASubclass1Subclass"))).isEmpty();
  }
  
  @Test
  public void test_findDuplicateFeaturesInSubclasses() {
    final Consumer<EPackage> _function = (EPackage it) -> {
      final EClass superclassWithDuplicatesInSubclasses = EdeltaLibrary.addNewEClass(it, "SuperClassWithDuplicatesInSubclasses");
      final Consumer<EClass> _function_1 = (EClass it_1) -> {
        EdeltaLibrary.addNewEAttribute(it_1, "A1", this.stringDataType);
      };
      EdeltaLibrary.addNewSubclass(superclassWithDuplicatesInSubclasses, "C1", _function_1);
      final Consumer<EClass> _function_2 = (EClass it_1) -> {
        EdeltaLibrary.addNewEAttribute(it_1, "A1", this.stringDataType);
      };
      EdeltaLibrary.addNewSubclass(superclassWithDuplicatesInSubclasses, "C2", _function_2);
      final EClass superclassWithoutDuplicatesInAllSubclasses = EdeltaLibrary.addNewEClass(it, "SuperClassWithoutDuplicatesInAllSubclasses");
      final Consumer<EClass> _function_3 = (EClass it_1) -> {
        EdeltaLibrary.addNewEAttribute(it_1, "A1", this.stringDataType);
      };
      EdeltaLibrary.addNewSubclass(superclassWithoutDuplicatesInAllSubclasses, "D1", _function_3);
      final Consumer<EClass> _function_4 = (EClass it_1) -> {
        EdeltaLibrary.addNewEAttribute(it_1, "A1", this.stringDataType);
      };
      EdeltaLibrary.addNewSubclass(superclassWithoutDuplicatesInAllSubclasses, "D2", _function_4);
      final Consumer<EClass> _function_5 = (EClass it_1) -> {
        EdeltaLibrary.addNewEAttribute(it_1, "A1", this.intDataType);
      };
      EdeltaLibrary.addNewSubclass(superclassWithoutDuplicatesInAllSubclasses, "D3", _function_5);
    };
    final EPackage p = this.createEPackage("p", _function);
    final LinkedHashMap<EClass, Map<EStructuralFeature, List<EStructuralFeature>>> result = this.finder.findDuplicateFeaturesInSubclasses(p);
    EStructuralFeature _findEStructuralFeature = this.findEStructuralFeature(p, "C1", "A1");
    EStructuralFeature _findEStructuralFeature_1 = this.findEStructuralFeature(p, "C2", "A1");
    Assertions.<EClass, Map<EStructuralFeature, List<EStructuralFeature>>>assertThat(result).containsExactly(
      Assertions.<EClass, Map<EStructuralFeature, List<EStructuralFeature>>>entry(
        this.findEClass(p, "SuperClassWithDuplicatesInSubclasses"), 
        Maps.<EStructuralFeature, List<EStructuralFeature>>newHashMap(
          this.findEStructuralFeature(p, "C1", "A1"), 
          Collections.<EStructuralFeature>unmodifiableList(CollectionLiterals.<EStructuralFeature>newArrayList(_findEStructuralFeature, _findEStructuralFeature_1)))));
  }
}
