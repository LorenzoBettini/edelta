package edelta.refactorings.lib.tests;

import com.google.common.base.Objects;
import com.google.common.collect.Iterables;
import edelta.lib.AbstractEdelta;
import edelta.refactorings.lib.EdeltaBadSmellsResolver;
import edelta.refactorings.lib.tests.AbstractTest;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import org.assertj.core.api.Assertions;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EEnum;
import org.eclipse.emf.ecore.EEnumLiteral;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.xtext.xbase.lib.CollectionLiterals;
import org.eclipse.xtext.xbase.lib.Conversions;
import org.eclipse.xtext.xbase.lib.Functions.Function1;
import org.eclipse.xtext.xbase.lib.IterableExtensions;
import org.eclipse.xtext.xbase.lib.ListExtensions;
import org.eclipse.xtext.xbase.lib.ObjectExtensions;
import org.eclipse.xtext.xbase.lib.Procedures.Procedure1;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

@SuppressWarnings("all")
public class EdeltaBadSmellsResolverTest extends AbstractTest {
  private EdeltaBadSmellsResolver resolver;
  
  @Before
  public void setup() {
    EdeltaBadSmellsResolver _edeltaBadSmellsResolver = new EdeltaBadSmellsResolver();
    this.resolver = _edeltaBadSmellsResolver;
  }
  
  @Test
  public void test_ConstructorArgument() {
    EdeltaBadSmellsResolver _edeltaBadSmellsResolver = new EdeltaBadSmellsResolver(new AbstractEdelta() {
    });
    this.resolver = _edeltaBadSmellsResolver;
    Assertions.<EdeltaBadSmellsResolver>assertThat(this.resolver).isNotNull();
  }
  
  @Test
  public void test_resolveDuplicateFeatures() {
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
      EClass _createEClass_2 = this.createEClass(it, "C3");
      final Procedure1<EClass> _function_3 = (EClass it_1) -> {
        EAttribute _createEAttribute = this.createEAttribute(it_1, "A1");
        final Procedure1<EAttribute> _function_4 = (EAttribute it_2) -> {
          it_2.setEType(this.stringDataType);
          it_2.setLowerBound(2);
        };
        ObjectExtensions.<EAttribute>operator_doubleArrow(_createEAttribute, _function_4);
      };
      ObjectExtensions.<EClass>operator_doubleArrow(_createEClass_2, _function_3);
      EClass _createEClass_3 = this.createEClass(it, "C4");
      final Procedure1<EClass> _function_4 = (EClass it_1) -> {
        EAttribute _createEAttribute = this.createEAttribute(it_1, "A1");
        final Procedure1<EAttribute> _function_5 = (EAttribute it_2) -> {
          it_2.setEType(this.stringDataType);
          it_2.setLowerBound(2);
        };
        ObjectExtensions.<EAttribute>operator_doubleArrow(_createEAttribute, _function_5);
      };
      ObjectExtensions.<EClass>operator_doubleArrow(_createEClass_3, _function_4);
    };
    final EPackage p = ObjectExtensions.<EPackage>operator_doubleArrow(_createEPackage, _function);
    this.resolver.resolveDuplicatedFeatures(p);
    final Function1<EClassifier, String> _function_1 = (EClassifier it) -> {
      return it.getName();
    };
    final List<String> classifiersNames = ListExtensions.<EClassifier, String>map(p.getEClassifiers(), _function_1);
    Assertions.<String>assertThat(classifiersNames).containsExactly("C1", "C2", "C3", "C4", "A1Element", "A1Element1");
    final Iterable<EClass> classes = this.EClasses(p);
    Assertions.<EAttribute>assertThat((((EClass[])Conversions.unwrapArray(classes, EClass.class))[0]).getEAttributes()).isEmpty();
    Assertions.<EAttribute>assertThat((((EClass[])Conversions.unwrapArray(classes, EClass.class))[1]).getEAttributes()).isEmpty();
    Assertions.<EAttribute>assertThat((((EClass[])Conversions.unwrapArray(classes, EClass.class))[2]).getEAttributes()).isEmpty();
    Assertions.<EAttribute>assertThat((((EClass[])Conversions.unwrapArray(classes, EClass.class))[3]).getEAttributes()).isEmpty();
    Assertions.<EAttribute>assertThat((((EClass[])Conversions.unwrapArray(classes, EClass.class))[4]).getEAttributes()).hasSize(1);
    Assertions.<EAttribute>assertThat((((EClass[])Conversions.unwrapArray(classes, EClass.class))[5]).getEAttributes()).hasSize(1);
    final EAttribute extractedA1NoLowerBound = IterableExtensions.<EAttribute>head((((EClass[])Conversions.unwrapArray(classes, EClass.class))[4]).getEAttributes());
    final Function<EAttribute, String> _function_2 = (EAttribute it) -> {
      return it.getName();
    };
    final Function<EAttribute, EDataType> _function_3 = (EAttribute it) -> {
      return it.getEAttributeType();
    };
    final Function<EAttribute, Integer> _function_4 = (EAttribute it) -> {
      return Integer.valueOf(it.getLowerBound());
    };
    Assertions.<EAttribute>assertThat(extractedA1NoLowerBound).<String>returns("A1", _function_2).<EDataType>returns(this.stringDataType, _function_3).<Integer>returns(Integer.valueOf(0), _function_4);
    final EAttribute extractedA1WithLowerBound = IterableExtensions.<EAttribute>head((((EClass[])Conversions.unwrapArray(classes, EClass.class))[5]).getEAttributes());
    final Function<EAttribute, String> _function_5 = (EAttribute it) -> {
      return it.getName();
    };
    final Function<EAttribute, EDataType> _function_6 = (EAttribute it) -> {
      return it.getEAttributeType();
    };
    final Function<EAttribute, Integer> _function_7 = (EAttribute it) -> {
      return Integer.valueOf(it.getLowerBound());
    };
    Assertions.<EAttribute>assertThat(extractedA1WithLowerBound).<String>returns("A1", _function_5).<EDataType>returns(this.stringDataType, _function_6).<Integer>returns(Integer.valueOf(2), _function_7);
  }
  
  @Test
  public void test_resolveDeadClassifiers() {
    EPackage _createEPackage = this.factory.createEPackage();
    final Procedure1<EPackage> _function = (EPackage it) -> {
      this.createEClass(it, "Unused1");
      this.createEClass(it, "Unused2");
      final EClass used1 = this.createEClass(it, "Used1");
      final EClass used2 = this.createEClass(it, "Used2");
      EClass _createEClass = this.createEClass(it, "Unused3");
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
    final Predicate<EClassifier> _function_1 = (EClassifier it) -> {
      String _name = it.getName();
      return Objects.equal(_name, "Unused2");
    };
    this.resolver.resolveDeadClassifiers(p, _function_1);
    final Predicate<EClassifier> _function_2 = (EClassifier it) -> {
      String _name = it.getName();
      return Objects.equal(_name, "Unused2");
    };
    Assertions.<EClassifier>assertThat(p.getEClassifiers()).hasSize(4).noneMatch(_function_2);
  }
  
  @Test
  public void test_resolveRedundantContainers() {
    EPackage _createEPackage = this.factory.createEPackage();
    final Procedure1<EPackage> _function = (EPackage it) -> {
      final EClass containedWithRedundant = this.createEClass(it, "ContainedWithRedundant");
      EClass _createEClass = this.createEClass(it, "Container");
      final Procedure1<EClass> _function_1 = (EClass it_1) -> {
        EReference _createEReference = this.createEReference(it_1, "containedWithRedundant");
        final Procedure1<EReference> _function_2 = (EReference it_2) -> {
          it_2.setEType(containedWithRedundant);
          it_2.setContainment(true);
        };
        ObjectExtensions.<EReference>operator_doubleArrow(_createEReference, _function_2);
      };
      final EClass container = ObjectExtensions.<EClass>operator_doubleArrow(_createEClass, _function_1);
      EReference _createEReference = this.createEReference(containedWithRedundant, "redundant");
      final Procedure1<EReference> _function_2 = (EReference it_1) -> {
        it_1.setEType(container);
        it_1.setLowerBound(1);
      };
      ObjectExtensions.<EReference>operator_doubleArrow(_createEReference, _function_2);
    };
    final EPackage p = ObjectExtensions.<EPackage>operator_doubleArrow(_createEPackage, _function);
    final EReference redundant = IterableExtensions.<EReference>head(IterableExtensions.<EClass>head(this.EClasses(p)).getEReferences());
    final EReference opposite = IterableExtensions.<EReference>head(IterableExtensions.<EClass>last(this.EClasses(p)).getEReferences());
    Assert.assertNull(redundant.getEOpposite());
    Assert.assertNull(opposite.getEOpposite());
    this.resolver.resolveRedundantContainers(p);
    Assert.assertNotNull(redundant.getEOpposite());
    Assert.assertSame(redundant.getEOpposite(), opposite);
    Assert.assertSame(opposite.getEOpposite(), redundant);
  }
  
  @Test
  public void test_resolveClassificationByHierarchy() {
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
    };
    final EPackage p = ObjectExtensions.<EPackage>operator_doubleArrow(_createEPackage, _function);
    this.resolver.resolveClassificationByHierarchy(p);
    Assert.assertEquals(2, p.getEClassifiers().size());
    EClassifier _last = IterableExtensions.<EClassifier>last(p.getEClassifiers());
    final EEnum enum_ = ((EEnum) _last);
    Assert.assertEquals("BaseType", enum_.getName());
    final EList<EEnumLiteral> eLiterals = enum_.getELiterals();
    Assert.assertEquals(2, eLiterals.size());
    Assert.assertEquals("DERIVED1", eLiterals.get(0).getName());
    Assert.assertEquals("DERIVED2", eLiterals.get(1).getName());
    Assert.assertEquals(0, eLiterals.get(0).getValue());
    Assert.assertEquals(1, eLiterals.get(1).getValue());
    EClassifier _head = IterableExtensions.<EClassifier>head(p.getEClassifiers());
    final EClass c = ((EClass) _head);
    final EAttribute attr = this.findEAttribute(c, "baseType");
    Assert.assertSame(enum_, attr.getEType());
  }
  
  @Test
  public void test_resolveConcreteAbstractMetaclass() {
    EPackage _createEPackage = this.factory.createEPackage();
    final Procedure1<EPackage> _function = (EPackage it) -> {
      final EClass base = this.createEClass(it, "ConcreteAbstractMetaclass");
      EClass _createEClass = this.createEClass(it, "Derived1");
      final Procedure1<EClass> _function_1 = (EClass it_1) -> {
        EList<EClass> _eSuperTypes = it_1.getESuperTypes();
        _eSuperTypes.add(base);
      };
      ObjectExtensions.<EClass>operator_doubleArrow(_createEClass, _function_1);
    };
    final EPackage p = ObjectExtensions.<EPackage>operator_doubleArrow(_createEPackage, _function);
    final EClass c = IterableExtensions.<EClass>head(this.EClasses(p));
    Assert.assertFalse(c.isAbstract());
    this.resolver.resolveConcreteAbstractMetaclass(p);
    Assert.assertTrue(c.isAbstract());
  }
  
  @Test
  public void test_resolveAbstractConcreteMetaclass() {
    EPackage _createEPackage = this.factory.createEPackage();
    final Procedure1<EPackage> _function = (EPackage it) -> {
      EClass _createEClass = this.createEClass(it, "AbstractConcreteMetaclass");
      final Procedure1<EClass> _function_1 = (EClass it_1) -> {
        it_1.setAbstract(true);
      };
      ObjectExtensions.<EClass>operator_doubleArrow(_createEClass, _function_1);
    };
    final EPackage p = ObjectExtensions.<EPackage>operator_doubleArrow(_createEPackage, _function);
    final EClass c = IterableExtensions.<EClass>head(this.EClasses(p));
    Assert.assertTrue(c.isAbstract());
    this.resolver.resolveAbstractConcreteMetaclass(p);
    Assert.assertFalse(c.isAbstract());
  }
  
  @Test
  public void test_resolveAbstractSubclassesOfConcreteSuperclasses() {
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
    Assertions.assertThat(IterableExtensions.<EClass>last(this.EClasses(p)).isAbstract()).isTrue();
    this.resolver.resolveAbstractSubclassesOfConcreteSuperclasses(p);
    Assertions.assertThat(IterableExtensions.<EClass>last(this.EClasses(p)).isAbstract()).isFalse();
  }
  
  @Test
  public void test_resolveDuplicateFeaturesInSubclasses() {
    EPackage _createEPackage = this.createEPackage("p");
    final Procedure1<EPackage> _function = (EPackage it) -> {
      final EClass superclassWithDuplicatesInSubclasses = this.createEClass(it, "SuperClassWithDuplicatesInSubclasses");
      EClass _createEClass = this.createEClass(it, "C1");
      final Procedure1<EClass> _function_1 = (EClass it_1) -> {
        EList<EClass> _eSuperTypes = it_1.getESuperTypes();
        _eSuperTypes.add(superclassWithDuplicatesInSubclasses);
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
        _eSuperTypes.add(superclassWithDuplicatesInSubclasses);
        EAttribute _createEAttribute = this.createEAttribute(it_1, "A1");
        final Procedure1<EAttribute> _function_3 = (EAttribute it_2) -> {
          it_2.setEType(this.stringDataType);
        };
        ObjectExtensions.<EAttribute>operator_doubleArrow(_createEAttribute, _function_3);
      };
      ObjectExtensions.<EClass>operator_doubleArrow(_createEClass_1, _function_2);
      final EClass superclassWithoutDuplicatesInAllSubclasses = this.createEClass(it, "SuperClassWithoutDuplicatesInAllSubclasses");
      EClass _createEClass_2 = this.createEClass(it, "D1");
      final Procedure1<EClass> _function_3 = (EClass it_1) -> {
        EList<EClass> _eSuperTypes = it_1.getESuperTypes();
        _eSuperTypes.add(superclassWithoutDuplicatesInAllSubclasses);
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
        _eSuperTypes.add(superclassWithoutDuplicatesInAllSubclasses);
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
        _eSuperTypes.add(superclassWithoutDuplicatesInAllSubclasses);
        EAttribute _createEAttribute = this.createEAttribute(it_1, "A1");
        final Procedure1<EAttribute> _function_6 = (EAttribute it_2) -> {
          it_2.setEType(this.intDataType);
        };
        ObjectExtensions.<EAttribute>operator_doubleArrow(_createEAttribute, _function_6);
      };
      ObjectExtensions.<EClass>operator_doubleArrow(_createEClass_4, _function_5);
    };
    final EPackage p = ObjectExtensions.<EPackage>operator_doubleArrow(_createEPackage, _function);
    final EClass superClass = IterableExtensions.<EClass>head(this.EClasses(p));
    final Function1<EClass, Boolean> _function_1 = (EClass it) -> {
      return Boolean.valueOf(it.getName().startsWith("C"));
    };
    final Iterable<EClass> classesWithDuplicates = IterableExtensions.<EClass>filter(this.EClasses(p), _function_1);
    final Function1<EClass, EList<EStructuralFeature>> _function_2 = (EClass it) -> {
      return it.getEStructuralFeatures();
    };
    final Function1<EStructuralFeature, String> _function_3 = (EStructuralFeature it) -> {
      return it.getName();
    };
    Assertions.<String>assertThat(
      IterableExtensions.<String>toSet(IterableExtensions.<EStructuralFeature, String>map(Iterables.<EStructuralFeature>concat(IterableExtensions.<EClass, EList<EStructuralFeature>>map(classesWithDuplicates, _function_2)), _function_3))).containsOnly("A1");
    Assertions.<EStructuralFeature>assertThat(superClass.getEStructuralFeatures()).isEmpty();
    this.resolver.resolveDuplicateFeaturesInSubclasses(p);
    final Function1<EClass, EList<EStructuralFeature>> _function_4 = (EClass it) -> {
      return it.getEStructuralFeatures();
    };
    Assertions.<EStructuralFeature>assertThat(Iterables.<EStructuralFeature>concat(IterableExtensions.<EClass, EList<EStructuralFeature>>map(classesWithDuplicates, _function_4))).isEmpty();
    final Function1<EStructuralFeature, String> _function_5 = (EStructuralFeature it) -> {
      return it.getName();
    };
    Assertions.<String>assertThat(ListExtensions.<EStructuralFeature, String>map(superClass.getEStructuralFeatures(), _function_5)).containsOnly("A1");
  }
}
