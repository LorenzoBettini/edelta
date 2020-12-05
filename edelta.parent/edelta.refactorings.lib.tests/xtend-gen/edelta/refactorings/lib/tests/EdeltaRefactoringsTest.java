package edelta.refactorings.lib.tests;

import com.google.common.base.Objects;
import com.google.common.collect.Iterables;
import edelta.lib.AbstractEdelta;
import edelta.refactorings.lib.EdeltaRefactorings;
import edelta.refactorings.lib.tests.AbstractTest;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
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
import org.eclipse.xtext.xbase.lib.Pair;
import org.eclipse.xtext.xbase.lib.Procedures.Procedure1;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

@SuppressWarnings("all")
public class EdeltaRefactoringsTest extends AbstractTest {
  private EdeltaRefactorings refactorings;
  
  @Before
  public void setup() {
    EdeltaRefactorings _edeltaRefactorings = new EdeltaRefactorings();
    this.refactorings = _edeltaRefactorings;
  }
  
  @Test
  public void test_ConstructorArgument() {
    EdeltaRefactorings _edeltaRefactorings = new EdeltaRefactorings(new AbstractEdelta() {
    });
    this.refactorings = _edeltaRefactorings;
    final EClass c = this.createEClassWithoutPackage("C1");
    this.refactorings.addMandatoryAttr(c, "test", this.stringDataType);
    final EAttribute attr = IterableExtensions.<EAttribute>head(Iterables.<EAttribute>filter(c.getEStructuralFeatures(), EAttribute.class));
    final Function<EAttribute, String> _function = (EAttribute it) -> {
      return it.getName();
    };
    Assertions.<EAttribute>assertThat(attr).<String>returns("test", _function);
  }
  
  @Test
  public void test_addMandatoryAttr() {
    final EClass c = this.createEClassWithoutPackage("C1");
    this.refactorings.addMandatoryAttr(c, "test", this.stringDataType);
    final EAttribute attr = IterableExtensions.<EAttribute>head(Iterables.<EAttribute>filter(c.getEStructuralFeatures(), EAttribute.class));
    final Function<EAttribute, String> _function = (EAttribute it) -> {
      return it.getName();
    };
    final Function<EAttribute, EDataType> _function_1 = (EAttribute it) -> {
      return it.getEAttributeType();
    };
    final Function<EAttribute, Boolean> _function_2 = (EAttribute it) -> {
      return Boolean.valueOf(it.isRequired());
    };
    Assertions.<EAttribute>assertThat(attr).<String>returns("test", _function).<EDataType>returns(this.stringDataType, _function_1).<Boolean>returns(Boolean.valueOf(true), _function_2);
  }
  
  @Test
  public void test_mergeReferences() {
    final EClass refType = this.createEClassWithoutPackage("RefType");
    EClass _createEClassWithoutPackage = this.createEClassWithoutPackage("C1");
    final Procedure1<EClass> _function = (EClass it) -> {
      EReference _createEReference = this.createEReference(it, "ref1");
      final Procedure1<EReference> _function_1 = (EReference it_1) -> {
        it_1.setEType(refType);
      };
      ObjectExtensions.<EReference>operator_doubleArrow(_createEReference, _function_1);
      EReference _createEReference_1 = this.createEReference(it, "ref2");
      final Procedure1<EReference> _function_2 = (EReference it_1) -> {
        it_1.setEType(refType);
      };
      ObjectExtensions.<EReference>operator_doubleArrow(_createEReference_1, _function_2);
    };
    final EClass c = ObjectExtensions.<EClass>operator_doubleArrow(_createEClassWithoutPackage, _function);
    final EReference merged = this.refactorings.mergeReferences("test", refType, 
      IterableExtensions.<EReference>toList(Iterables.<EReference>filter(c.getEStructuralFeatures(), EReference.class)));
    Assertions.<EStructuralFeature>assertThat(c.getEStructuralFeatures()).isEmpty();
    final Function<EReference, String> _function_1 = (EReference it) -> {
      return it.getName();
    };
    final Function<EReference, EClass> _function_2 = (EReference it) -> {
      return it.getEReferenceType();
    };
    Assertions.<EReference>assertThat(merged).<String>returns("test", _function_1).<EClass>returns(refType, _function_2);
  }
  
  @Test
  public void test_mergeAttributes() {
    EClass _createEClassWithoutPackage = this.createEClassWithoutPackage("C1");
    final Procedure1<EClass> _function = (EClass it) -> {
      EAttribute _createEAttribute = this.createEAttribute(it, "a1");
      final Procedure1<EAttribute> _function_1 = (EAttribute it_1) -> {
        it_1.setEType(this.stringDataType);
      };
      ObjectExtensions.<EAttribute>operator_doubleArrow(_createEAttribute, _function_1);
      EAttribute _createEAttribute_1 = this.createEAttribute(it, "a2");
      final Procedure1<EAttribute> _function_2 = (EAttribute it_1) -> {
        it_1.setEType(this.stringDataType);
      };
      ObjectExtensions.<EAttribute>operator_doubleArrow(_createEAttribute_1, _function_2);
    };
    final EClass c = ObjectExtensions.<EClass>operator_doubleArrow(_createEClassWithoutPackage, _function);
    final EAttribute merged = this.refactorings.mergeAttributes("test", this.stringDataType, 
      IterableExtensions.<EAttribute>toList(Iterables.<EAttribute>filter(c.getEStructuralFeatures(), EAttribute.class)));
    Assertions.<EStructuralFeature>assertThat(c.getEStructuralFeatures()).isEmpty();
    final Function<EAttribute, String> _function_1 = (EAttribute it) -> {
      return it.getName();
    };
    final Function<EAttribute, EDataType> _function_2 = (EAttribute it) -> {
      return it.getEAttributeType();
    };
    Assertions.<EAttribute>assertThat(merged).<String>returns("test", _function_1).<EDataType>returns(this.stringDataType, _function_2);
  }
  
  @Test
  public void test_introduceSubclasses() {
    final EPackage p = this.factory.createEPackage();
    EEnum _createEEnum = this.createEEnum(p, "AnEnum");
    final Procedure1<EEnum> _function = (EEnum it) -> {
      this.createEEnumLiteral(it, "Lit1");
      this.createEEnumLiteral(it, "Lit2");
    };
    final EEnum enum_ = ObjectExtensions.<EEnum>operator_doubleArrow(_createEEnum, _function);
    EClass _createEClass = this.createEClass(p, "C1");
    final Procedure1<EClass> _function_1 = (EClass it) -> {
      it.setAbstract(false);
    };
    final EClass c = ObjectExtensions.<EClass>operator_doubleArrow(_createEClass, _function_1);
    EAttribute _createEAttribute = this.createEAttribute(c, "attr");
    final Procedure1<EAttribute> _function_2 = (EAttribute it) -> {
      it.setEType(enum_);
    };
    final EAttribute attr = ObjectExtensions.<EAttribute>operator_doubleArrow(_createEAttribute, _function_2);
    this.refactorings.introduceSubclasses(c, attr, enum_);
    Assertions.assertThat(c.isAbstract()).isTrue();
    Assertions.<EStructuralFeature>assertThat(c.getEStructuralFeatures()).isEmpty();
    final Consumer<EClass> _function_3 = (EClass it) -> {
      String _name = it.getName();
      boolean _notEquals = (!Objects.equal(_name, "C1"));
      if (_notEquals) {
        Assertions.assertThat(it.getName()).startsWith("Lit");
        Assertions.<EClass>assertThat(it.getESuperTypes()).containsExactly(c);
      }
    };
    Assertions.<EClass>assertThat(Iterables.<EClass>filter(p.getEClassifiers(), EClass.class)).hasSize(3).allSatisfy(_function_3);
  }
  
  @Test
  public void test_extractIntoSuperclass() {
    final EPackage p = this.factory.createEPackage();
    final EClass superClass = this.createEClass(p, "SuperClass");
    final EClass c1 = this.createEClass(p, "C1");
    final EClass c2 = this.createEClass(p, "C2");
    final EAttribute attr1 = this.createEAttribute(c1, "attr");
    final EAttribute attr2 = this.createEAttribute(c2, "attr");
    Assertions.<EStructuralFeature>assertThat(superClass.getEStructuralFeatures()).isEmpty();
    Assertions.<EStructuralFeature>assertThat(c1.getEStructuralFeatures()).isNotEmpty();
    Assertions.<EStructuralFeature>assertThat(c2.getEStructuralFeatures()).isNotEmpty();
    this.refactorings.extractIntoSuperclass(superClass, Collections.<EAttribute>unmodifiableList(CollectionLiterals.<EAttribute>newArrayList(attr1, attr2)));
    Assertions.<EStructuralFeature>assertThat(c1.getEStructuralFeatures()).isEmpty();
    Assertions.<EStructuralFeature>assertThat(c2.getEStructuralFeatures()).isEmpty();
    Assertions.<EStructuralFeature>assertThat(superClass.getEStructuralFeatures()).containsExactly(attr1);
    Assertions.<EClass>assertThat(c1.getESuperTypes()).containsExactly(superClass);
    Assertions.<EClass>assertThat(c2.getESuperTypes()).containsExactly(superClass);
  }
  
  @Test
  public void test_extractMetaClass() {
    final EPackage p = this.factory.createEPackage();
    final EClass person = this.createEClass(p, "Person");
    final EClass workPlace = this.createEClass(p, "WorkPlace");
    EReference _createEReference = this.createEReference(person, "works");
    final Procedure1<EReference> _function = (EReference it) -> {
      it.setLowerBound(1);
    };
    final EReference personWorks = ObjectExtensions.<EReference>operator_doubleArrow(_createEReference, _function);
    EReference _createEReference_1 = this.createEReference(workPlace, "persons");
    final Procedure1<EReference> _function_1 = (EReference it) -> {
      it.setEOpposite(personWorks);
      it.setEType(person);
    };
    final EReference workPlacePersons = ObjectExtensions.<EReference>operator_doubleArrow(_createEReference_1, _function_1);
    personWorks.setEType(workPlace);
    personWorks.setEOpposite(workPlacePersons);
    final EClass workingPosition = this.createEClass(p, "WorkingPosition");
    Assertions.<EStructuralFeature>assertThat(workingPosition.getEStructuralFeatures()).isEmpty();
    Assertions.<EStructuralFeature>assertThat(workPlace.getEStructuralFeatures()).contains(workPlacePersons);
    this.refactorings.extractMetaClass(workingPosition, personWorks, "position", "works");
    final Consumer<EStructuralFeature> _function_2 = (EStructuralFeature it) -> {
      final Function<EStructuralFeature, String> _function_3 = (EStructuralFeature it_1) -> {
        return it_1.getName();
      };
      final Function<EStructuralFeature, EClassifier> _function_4 = (EStructuralFeature it_1) -> {
        return it_1.getEType();
      };
      final Function<EStructuralFeature, Integer> _function_5 = (EStructuralFeature it_1) -> {
        return Integer.valueOf(it_1.getLowerBound());
      };
      Assertions.<EStructuralFeature>assertThat(it).<String>returns("works", _function_3).<EClassifier>returns(workPlace, _function_4).<Integer>returns(Integer.valueOf(1), _function_5);
    };
    Assertions.<EStructuralFeature>assertThat(workingPosition.getEStructuralFeatures()).hasSize(2).contains(workPlacePersons).anySatisfy(_function_2);
    final Consumer<EStructuralFeature> _function_3 = (EStructuralFeature it) -> {
      final Function<EStructuralFeature, String> _function_4 = (EStructuralFeature it_1) -> {
        return it_1.getName();
      };
      final Function<EStructuralFeature, EClassifier> _function_5 = (EStructuralFeature it_1) -> {
        return it_1.getEType();
      };
      Assertions.<EStructuralFeature>assertThat(it).<String>returns("position", _function_4).<EClassifier>returns(workingPosition, _function_5);
    };
    Assertions.<EStructuralFeature>assertThat(workPlace.getEStructuralFeatures()).hasSize(1).doesNotContain(workPlacePersons).anySatisfy(_function_3);
    Assertions.<EStructuralFeature>assertThat(person.getEStructuralFeatures()).containsExactly(personWorks);
  }
  
  @Test
  public void test_extractMetaClass2() {
    final EPackage p = this.factory.createEPackage();
    final EClass person = this.createEClass(p, "Person");
    final EClass workPlace = this.createEClass(p, "WorkPlace");
    EReference _createEReference = this.createEReference(person, "works");
    final Procedure1<EReference> _function = (EReference it) -> {
      it.setLowerBound(1);
    };
    final EReference personWorks = ObjectExtensions.<EReference>operator_doubleArrow(_createEReference, _function);
    EReference _createEReference_1 = this.createEReference(workPlace, "persons");
    final Procedure1<EReference> _function_1 = (EReference it) -> {
      it.setEOpposite(personWorks);
      it.setEType(person);
      it.setUpperBound((-1));
    };
    final EReference workPlacePersons = ObjectExtensions.<EReference>operator_doubleArrow(_createEReference_1, _function_1);
    personWorks.setEType(workPlace);
    personWorks.setEOpposite(workPlacePersons);
    Assertions.<EStructuralFeature>assertThat(workPlace.getEStructuralFeatures()).contains(workPlacePersons);
    final EClass workingPosition = this.refactorings.extractMetaClass("WorkingPosition", personWorks);
    final Consumer<EReference> _function_2 = (EReference it) -> {
      final Function<EReference, String> _function_3 = (EReference it_1) -> {
        return it_1.getName();
      };
      final Function<EReference, EClassifier> _function_4 = (EReference it_1) -> {
        return it_1.getEType();
      };
      final Function<EReference, Integer> _function_5 = (EReference it_1) -> {
        return Integer.valueOf(it_1.getLowerBound());
      };
      final Function<EReference, Integer> _function_6 = (EReference it_1) -> {
        return Integer.valueOf(it_1.getUpperBound());
      };
      final Function<EReference, String> _function_7 = (EReference it_1) -> {
        return it_1.getEOpposite().getName();
      };
      final Function<EReference, EClass> _function_8 = (EReference it_1) -> {
        return it_1.getEOpposite().getEReferenceType();
      };
      Assertions.<EReference>assertThat(it).<String>returns("works", _function_3).<EClassifier>returns(workPlace, _function_4).<Integer>returns(Integer.valueOf(1), _function_5).<Integer>returns(Integer.valueOf(1), _function_6).<String>returns("workingPosition", _function_7).<EClass>returns(workingPosition, _function_8);
    };
    final Consumer<EReference> _function_3 = (EReference it) -> {
      final Function<EReference, String> _function_4 = (EReference it_1) -> {
        return it_1.getName();
      };
      final Function<EReference, EClassifier> _function_5 = (EReference it_1) -> {
        return it_1.getEType();
      };
      final Function<EReference, Integer> _function_6 = (EReference it_1) -> {
        return Integer.valueOf(it_1.getLowerBound());
      };
      final Function<EReference, Integer> _function_7 = (EReference it_1) -> {
        return Integer.valueOf(it_1.getUpperBound());
      };
      final Function<EReference, String> _function_8 = (EReference it_1) -> {
        return it_1.getEOpposite().getName();
      };
      final Function<EReference, EClass> _function_9 = (EReference it_1) -> {
        return it_1.getEOpposite().getEReferenceType();
      };
      Assertions.<EReference>assertThat(it).<String>returns("persons", _function_4).<EClassifier>returns(person, _function_5).<Integer>returns(Integer.valueOf(0), _function_6).<Integer>returns(Integer.valueOf((-1)), _function_7).<String>returns("workingPosition", _function_8).<EClass>returns(workingPosition, _function_9);
    };
    Assertions.<EReference>assertThat(Iterables.<EReference>filter(workingPosition.getEStructuralFeatures(), EReference.class)).hasSize(2).contains(workPlacePersons).anySatisfy(_function_2).anySatisfy(_function_3);
    final Consumer<EReference> _function_4 = (EReference it) -> {
      final Function<EReference, String> _function_5 = (EReference it_1) -> {
        return it_1.getName();
      };
      final Function<EReference, EClassifier> _function_6 = (EReference it_1) -> {
        return it_1.getEType();
      };
      final Function<EReference, Integer> _function_7 = (EReference it_1) -> {
        return Integer.valueOf(it_1.getLowerBound());
      };
      final Function<EReference, Integer> _function_8 = (EReference it_1) -> {
        return Integer.valueOf(it_1.getUpperBound());
      };
      final Function<EReference, String> _function_9 = (EReference it_1) -> {
        return it_1.getEOpposite().getName();
      };
      final Function<EReference, EClassifier> _function_10 = (EReference it_1) -> {
        return it_1.getEOpposite().getEType();
      };
      Assertions.<EReference>assertThat(it).<String>returns("workingPosition", _function_5).<EClassifier>returns(workingPosition, _function_6).<Integer>returns(Integer.valueOf(0), _function_7).<Integer>returns(Integer.valueOf(1), _function_8).<String>returns("works", _function_9).<EClassifier>returns(workPlace, _function_10);
    };
    Assertions.<EReference>assertThat(Iterables.<EReference>filter(workPlace.getEStructuralFeatures(), EReference.class)).hasSize(1).doesNotContain(workPlacePersons).anySatisfy(_function_4);
    final Consumer<EReference> _function_5 = (EReference it) -> {
      final Function<EReference, String> _function_6 = (EReference it_1) -> {
        return it_1.getName();
      };
      final Function<EReference, EClassifier> _function_7 = (EReference it_1) -> {
        return it_1.getEType();
      };
      final Function<EReference, Integer> _function_8 = (EReference it_1) -> {
        return Integer.valueOf(it_1.getLowerBound());
      };
      final Function<EReference, Integer> _function_9 = (EReference it_1) -> {
        return Integer.valueOf(it_1.getUpperBound());
      };
      final Function<EReference, String> _function_10 = (EReference it_1) -> {
        return it_1.getEOpposite().getName();
      };
      final Function<EReference, EClass> _function_11 = (EReference it_1) -> {
        return it_1.getEOpposite().getEReferenceType();
      };
      Assertions.<EReference>assertThat(it).<String>returns("workingPosition", _function_6).<EClassifier>returns(workingPosition, _function_7).<Integer>returns(Integer.valueOf(1), _function_8).<Integer>returns(Integer.valueOf(1), _function_9).<String>returns("persons", _function_10).<EClass>returns(person, _function_11);
    };
    Assertions.<EReference>assertThat(Iterables.<EReference>filter(person.getEStructuralFeatures(), EReference.class)).containsExactly(personWorks).anySatisfy(_function_5);
  }
  
  @Test
  public void test_extractMetaClassWithoutEOpposite() {
    final EPackage p = this.factory.createEPackage();
    final EClass person = this.createEClass(p, "Person");
    final EClass workPlace = this.createEClass(p, "WorkPlace");
    EReference _createEReference = this.createEReference(person, "works");
    final Procedure1<EReference> _function = (EReference it) -> {
      it.setLowerBound(1);
      it.setEType(workPlace);
    };
    final EReference personWorks = ObjectExtensions.<EReference>operator_doubleArrow(_createEReference, _function);
    Assertions.<EStructuralFeature>assertThat(workPlace.getEStructuralFeatures()).isEmpty();
    final EClass workingPosition = this.refactorings.extractMetaClass("WorkingPosition", personWorks);
    final Consumer<EReference> _function_1 = (EReference it) -> {
      final Function<EReference, String> _function_2 = (EReference it_1) -> {
        return it_1.getName();
      };
      final Function<EReference, EClassifier> _function_3 = (EReference it_1) -> {
        return it_1.getEType();
      };
      final Function<EReference, Integer> _function_4 = (EReference it_1) -> {
        return Integer.valueOf(it_1.getLowerBound());
      };
      final Function<EReference, Integer> _function_5 = (EReference it_1) -> {
        return Integer.valueOf(it_1.getUpperBound());
      };
      final Function<EReference, String> _function_6 = (EReference it_1) -> {
        return it_1.getEOpposite().getName();
      };
      final Function<EReference, EClass> _function_7 = (EReference it_1) -> {
        return it_1.getEOpposite().getEReferenceType();
      };
      Assertions.<EReference>assertThat(it).<String>returns("works", _function_2).<EClassifier>returns(workPlace, _function_3).<Integer>returns(Integer.valueOf(1), _function_4).<Integer>returns(Integer.valueOf(1), _function_5).<String>returns("workingPosition", _function_6).<EClass>returns(workingPosition, _function_7);
    };
    Assertions.<EReference>assertThat(Iterables.<EReference>filter(workingPosition.getEStructuralFeatures(), EReference.class)).hasSize(1).anySatisfy(_function_1);
    final Consumer<EReference> _function_2 = (EReference it) -> {
      final Function<EReference, String> _function_3 = (EReference it_1) -> {
        return it_1.getName();
      };
      final Function<EReference, EClassifier> _function_4 = (EReference it_1) -> {
        return it_1.getEType();
      };
      final Function<EReference, Integer> _function_5 = (EReference it_1) -> {
        return Integer.valueOf(it_1.getLowerBound());
      };
      final Function<EReference, Integer> _function_6 = (EReference it_1) -> {
        return Integer.valueOf(it_1.getUpperBound());
      };
      final Function<EReference, String> _function_7 = (EReference it_1) -> {
        return it_1.getEOpposite().getName();
      };
      final Function<EReference, EClassifier> _function_8 = (EReference it_1) -> {
        return it_1.getEOpposite().getEType();
      };
      Assertions.<EReference>assertThat(it).<String>returns("workingPosition", _function_3).<EClassifier>returns(workingPosition, _function_4).<Integer>returns(Integer.valueOf(0), _function_5).<Integer>returns(Integer.valueOf(1), _function_6).<String>returns("works", _function_7).<EClassifier>returns(workPlace, _function_8);
    };
    Assertions.<EReference>assertThat(Iterables.<EReference>filter(workPlace.getEStructuralFeatures(), EReference.class)).hasSize(1).anySatisfy(_function_2);
    final Consumer<EReference> _function_3 = (EReference it) -> {
      final Function<EReference, String> _function_4 = (EReference it_1) -> {
        return it_1.getName();
      };
      final Function<EReference, EClassifier> _function_5 = (EReference it_1) -> {
        return it_1.getEType();
      };
      final Function<EReference, Integer> _function_6 = (EReference it_1) -> {
        return Integer.valueOf(it_1.getLowerBound());
      };
      final Function<EReference, Integer> _function_7 = (EReference it_1) -> {
        return Integer.valueOf(it_1.getUpperBound());
      };
      Assertions.<EReference>assertThat(it).<String>returns("workingPosition", _function_4).<EClassifier>returns(workingPosition, _function_5).<Integer>returns(Integer.valueOf(1), _function_6).<Integer>returns(Integer.valueOf(1), _function_7);
    };
    Assertions.<EReference>assertThat(Iterables.<EReference>filter(person.getEStructuralFeatures(), EReference.class)).containsExactly(personWorks).anySatisfy(_function_3);
  }
  
  @Test
  public void test_extractSuperClass() {
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
    final Function1<EClass, EList<EAttribute>> _function_1 = (EClass it) -> {
      return it.getEAttributes();
    };
    final List<EAttribute> duplicates = IterableExtensions.<EAttribute>toList(Iterables.<EAttribute>concat(IterableExtensions.<EClass, EList<EAttribute>>map(this.EClasses(p), _function_1)));
    this.refactorings.extractSuperclass(duplicates);
    final Function1<EClassifier, String> _function_2 = (EClassifier it) -> {
      return it.getName();
    };
    final List<String> classifiersNames = ListExtensions.<EClassifier, String>map(p.getEClassifiers(), _function_2);
    Assertions.<String>assertThat(classifiersNames).hasSize(3).containsExactly("C1", "C2", "A1Element");
    final Iterable<EClass> classes = this.EClasses(p);
    Assertions.<EAttribute>assertThat((((EClass[])Conversions.unwrapArray(classes, EClass.class))[0]).getEAttributes()).isEmpty();
    Assertions.<EAttribute>assertThat((((EClass[])Conversions.unwrapArray(classes, EClass.class))[1]).getEAttributes()).isEmpty();
    Assertions.<EAttribute>assertThat((((EClass[])Conversions.unwrapArray(classes, EClass.class))[2]).getEAttributes()).hasSize(1);
    final EAttribute extracted = IterableExtensions.<EAttribute>head((((EClass[])Conversions.unwrapArray(classes, EClass.class))[2]).getEAttributes());
    Assertions.assertThat(extracted.getName()).isEqualTo("A1");
    Assertions.<EDataType>assertThat(extracted.getEAttributeType()).isEqualTo(this.stringDataType);
  }
  
  @Test
  public void test_extractSuperClassUnique() {
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
    final Function1<EClass, EList<EAttribute>> _function_1 = (EClass it) -> {
      return it.getEAttributes();
    };
    final List<EAttribute> attributes = IterableExtensions.<EAttribute>toList(Iterables.<EAttribute>concat(IterableExtensions.<EClass, EList<EAttribute>>map(this.EClasses(p), _function_1)));
    this.refactorings.extractSuperclass(
      IterableExtensions.<EAttribute>toList(IterableExtensions.<EAttribute>take(attributes, 2)));
    this.refactorings.extractSuperclass(
      attributes.stream().skip(2).collect(Collectors.<EAttribute>toList()));
    final Function1<EClassifier, String> _function_2 = (EClassifier it) -> {
      return it.getName();
    };
    final List<String> classifiersNames = ListExtensions.<EClassifier, String>map(p.getEClassifiers(), _function_2);
    Assertions.<String>assertThat(classifiersNames).containsExactly("C1", "C2", "C3", "C4", "A1Element", "A1Element1");
    final Iterable<EClass> classes = this.EClasses(p);
    Assertions.<EAttribute>assertThat((((EClass[])Conversions.unwrapArray(classes, EClass.class))[0]).getEAttributes()).isEmpty();
    Assertions.<EAttribute>assertThat((((EClass[])Conversions.unwrapArray(classes, EClass.class))[1]).getEAttributes()).isEmpty();
    Assertions.<EAttribute>assertThat((((EClass[])Conversions.unwrapArray(classes, EClass.class))[2]).getEAttributes()).isEmpty();
    Assertions.<EAttribute>assertThat((((EClass[])Conversions.unwrapArray(classes, EClass.class))[3]).getEAttributes()).isEmpty();
    Assertions.<EAttribute>assertThat((((EClass[])Conversions.unwrapArray(classes, EClass.class))[4]).getEAttributes()).hasSize(1);
    Assertions.<EAttribute>assertThat((((EClass[])Conversions.unwrapArray(classes, EClass.class))[5]).getEAttributes()).hasSize(1);
    final EAttribute extractedA1NoLowerBound = IterableExtensions.<EAttribute>head((((EClass[])Conversions.unwrapArray(classes, EClass.class))[4]).getEAttributes());
    Assertions.assertThat(extractedA1NoLowerBound.getName()).isEqualTo("A1");
    Assertions.<EDataType>assertThat(extractedA1NoLowerBound.getEAttributeType()).isEqualTo(this.stringDataType);
    Assertions.assertThat(extractedA1NoLowerBound.getLowerBound()).isZero();
    final EAttribute extractedA1WithLowerBound = IterableExtensions.<EAttribute>head((((EClass[])Conversions.unwrapArray(classes, EClass.class))[5]).getEAttributes());
    Assertions.assertThat(extractedA1WithLowerBound.getName()).isEqualTo("A1");
    Assertions.<EDataType>assertThat(extractedA1WithLowerBound.getEAttributeType()).isEqualTo(this.stringDataType);
    Assertions.assertThat(extractedA1WithLowerBound.getLowerBound()).isEqualTo(2);
  }
  
  @Test
  public void test_redundantContainerToEOpposite() {
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
    Pair<EReference, EReference> _mappedTo = Pair.<EReference, EReference>of(redundant, opposite);
    this.refactorings.redundantContainerToEOpposite(Collections.<Pair<EReference, EReference>>unmodifiableList(CollectionLiterals.<Pair<EReference, EReference>>newArrayList(_mappedTo)));
    Assert.assertNotNull(redundant.getEOpposite());
    Assert.assertSame(redundant.getEOpposite(), opposite);
    Assert.assertSame(opposite.getEOpposite(), redundant);
  }
  
  @Test
  public void test_classificationByHierarchyToEnum() {
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
    final EClass base = ((EClass[])Conversions.unwrapArray(this.EClasses(p), EClass.class))[0];
    final EClass derived1 = ((EClass[])Conversions.unwrapArray(this.EClasses(p), EClass.class))[1];
    final EClass derived2 = ((EClass[])Conversions.unwrapArray(this.EClasses(p), EClass.class))[2];
    Pair<EClass, List<EClass>> _mappedTo = Pair.<EClass, List<EClass>>of(base, Collections.<EClass>unmodifiableList(CollectionLiterals.<EClass>newArrayList(derived1, derived2)));
    this.refactorings.classificationByHierarchyToEnum(Collections.<EClass, List<EClass>>unmodifiableMap(CollectionLiterals.<EClass, List<EClass>>newHashMap(_mappedTo)));
    Assert.assertEquals(2, p.getEClassifiers().size());
    EClassifier _last = IterableExtensions.<EClassifier>last(p.getEClassifiers());
    final EEnum enum_ = ((EEnum) _last);
    Assert.assertEquals("BaseType", enum_.getName());
    final EList<EEnumLiteral> eLiterals = enum_.getELiterals();
    Assert.assertEquals(2, eLiterals.size());
    Assert.assertEquals("DERIVED1", eLiterals.get(0).getName());
    Assert.assertEquals("DERIVED2", eLiterals.get(1).getName());
    Assert.assertEquals(1, eLiterals.get(0).getValue());
    Assert.assertEquals(2, eLiterals.get(1).getValue());
    EClassifier _head = IterableExtensions.<EClassifier>head(p.getEClassifiers());
    final EClass c = ((EClass) _head);
    final EAttribute attr = this.findEAttribute(c, "baseType");
    Assert.assertSame(enum_, attr.getEType());
  }
  
  @Test
  public void test_concreteBaseMetaclassToAbstract() {
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
    this.refactorings.concreteBaseMetaclassToAbstract(Collections.<EClass>unmodifiableList(CollectionLiterals.<EClass>newArrayList(c)));
    Assert.assertTrue(c.isAbstract());
  }
  
  @Test
  public void test_abstractBaseMetaclassToConcrete() {
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
    this.refactorings.abstractBaseMetaclassToConcrete(Collections.<EClass>unmodifiableList(CollectionLiterals.<EClass>newArrayList(c)));
    Assert.assertFalse(c.isAbstract());
  }
}
