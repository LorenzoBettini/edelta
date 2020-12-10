package edelta.refactorings.lib.tests;

import com.google.common.base.Objects;
import com.google.common.collect.Iterables;
import edelta.lib.AbstractEdelta;
import edelta.refactorings.lib.EdeltaRefactorings;
import edelta.refactorings.lib.tests.AbstractTest;
import edelta.refactorings.lib.tests.utils.InMemoryLoggerAppender;
import edelta.testutils.EdeltaTestUtils;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.assertj.core.api.AbstractStringAssert;
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
import org.eclipse.xtend2.lib.StringConcatenation;
import org.eclipse.xtext.xbase.lib.CollectionLiterals;
import org.eclipse.xtext.xbase.lib.Conversions;
import org.eclipse.xtext.xbase.lib.Exceptions;
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
  
  private InMemoryLoggerAppender appender;
  
  private String testModelDirectory;
  
  private String testModelFile;
  
  @Before
  public void setup() {
    EdeltaRefactorings _edeltaRefactorings = new EdeltaRefactorings();
    this.refactorings = _edeltaRefactorings;
    InMemoryLoggerAppender _inMemoryLoggerAppender = new InMemoryLoggerAppender();
    this.appender = _inMemoryLoggerAppender;
    this.refactorings.getLogger().addAppender(this.appender);
  }
  
  private String withInputModel(final String testModelDirectory, final String testModelFile) {
    String _xblockexpression = null;
    {
      this.testModelDirectory = testModelDirectory;
      _xblockexpression = this.testModelFile = testModelFile;
    }
    return _xblockexpression;
  }
  
  private void loadModelFile() {
    this.checkInputModelSettings();
    this.refactorings.loadEcoreFile(
      (((AbstractTest.TESTECORES + this.testModelDirectory) + "/") + this.testModelFile));
  }
  
  private void assertModifiedFile() {
    try {
      this.checkInputModelSettings();
      EdeltaTestUtils.compareFileContents(
        (((AbstractTest.EXPECTATIONS + this.testModelDirectory) + "/") + this.testModelFile), 
        (AbstractTest.MODIFIED + this.testModelFile));
      Assertions.assertThat(this.appender.getResult()).isEmpty();
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }
  
  private void assertModifiedFileIsSameAsOriginal() {
    try {
      this.checkInputModelSettings();
      EdeltaTestUtils.compareFileContents(
        (((AbstractTest.TESTECORES + this.testModelDirectory) + "/") + this.testModelFile), 
        (AbstractTest.MODIFIED + this.testModelFile));
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }
  
  private AbstractStringAssert<?> checkInputModelSettings() {
    AbstractStringAssert<?> _xblockexpression = null;
    {
      Assertions.assertThat(this.testModelDirectory).isNotNull();
      _xblockexpression = Assertions.assertThat(this.testModelFile).isNotNull();
    }
    return _xblockexpression;
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
  public void test_extractMetaClassBidirectional() {
    try {
      this.withInputModel("extractMetaClassBidirectional", "PersonList.ecore");
      this.loadModelFile();
      final EReference ref = this.refactorings.getEReference("PersonList", "Person", "works");
      this.refactorings.extractMetaClass("WorkingPosition", ref, "worksAs", "position");
      this.refactorings.saveModifiedEcores(AbstractTest.MODIFIED);
      this.assertModifiedFile();
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }
  
  @Test
  public void test_extractMetaClassUnidirectional() {
    try {
      this.withInputModel("extractMetaClassUnidirectional", "PersonList.ecore");
      this.loadModelFile();
      final EReference ref = this.refactorings.getEReference("PersonList", "Person", "works");
      this.refactorings.extractMetaClass("WorkingPosition", ref, "worksAs", "position");
      this.refactorings.saveModifiedEcores(AbstractTest.MODIFIED);
      this.assertModifiedFile();
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }
  
  @Test
  public void test_extractMetaClassWithContainmentReference() {
    try {
      this.withInputModel("extractMetaClassWithContainmentReference", "PersonList.ecore");
      this.loadModelFile();
      final EReference ref = this.refactorings.getEReference("PersonList", "Person", "works");
      this.refactorings.extractMetaClass("WorkingPosition", ref, "worksAs", "position");
      this.refactorings.saveModifiedEcores(AbstractTest.MODIFIED);
      this.assertModifiedFileIsSameAsOriginal();
      Assertions.assertThat(this.appender.getResult().trim()).isEqualTo("ERROR: PersonList.Person.works: Cannot apply extractMetaClass on containment reference: PersonList.Person.works");
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }
  
  @Test
  public void test_extractMetaClassWithAttributes() {
    try {
      this.withInputModel("extractMetaClassWithAttributes", "PersonList.ecore");
      this.loadModelFile();
      EAttribute _eAttribute = this.refactorings.getEAttribute("PersonList", "Person", "street");
      EAttribute _eAttribute_1 = this.refactorings.getEAttribute("PersonList", "Person", "houseNumber");
      this.refactorings.extractMetaClass("Address", 
        Collections.<EAttribute>unmodifiableList(CollectionLiterals.<EAttribute>newArrayList(_eAttribute, _eAttribute_1)), 
        "address");
      this.refactorings.saveModifiedEcores(AbstractTest.MODIFIED);
      this.assertModifiedFile();
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }
  
  @Test
  public void test_extractMetaClassWithAttributesContainedInDifferentClasses() {
    try {
      this.withInputModel("extractMetaClassWithAttributesContainedInDifferentClasses", "PersonList.ecore");
      this.loadModelFile();
      EAttribute _eAttribute = this.refactorings.getEAttribute("PersonList", "Person", "street");
      EAttribute _eAttribute_1 = this.refactorings.getEAttribute("PersonList", "Person2", "street");
      this.refactorings.extractMetaClass("Address", 
        Collections.<EAttribute>unmodifiableList(CollectionLiterals.<EAttribute>newArrayList(_eAttribute, _eAttribute_1)), 
        "address");
      this.refactorings.saveModifiedEcores(AbstractTest.MODIFIED);
      this.assertModifiedFileIsSameAsOriginal();
      StringConcatenation _builder = new StringConcatenation();
      _builder.append("ERROR: PersonList.Person: Extracted attributes must belong to the same class: PersonList.Person");
      _builder.newLine();
      _builder.append("ERROR: PersonList.Person2: Extracted attributes must belong to the same class: PersonList.Person2");
      Assertions.assertThat(this.appender.getResult().trim()).isEqualTo(
        _builder.toString());
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }
  
  @Test
  public void test_extractMetaClassWithAttributesEmpty() {
    final EClass result = this.refactorings.extractMetaClass("Address", 
      Collections.<EAttribute>unmodifiableList(CollectionLiterals.<EAttribute>newArrayList()), 
      "address");
    Assertions.<EClass>assertThat(result).isNull();
  }
  
  @Test
  public void test_referenceToClassBidirectional() {
    try {
      this.withInputModel("referenceToClassBidirectional", "PersonList.ecore");
      this.loadModelFile();
      final EReference ref = this.refactorings.getEReference("PersonList", "Person", "works");
      this.refactorings.referenceToClass("WorkingPosition", ref);
      this.refactorings.saveModifiedEcores(AbstractTest.MODIFIED);
      this.assertModifiedFile();
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }
  
  @Test
  public void test_referenceToClassUnidirectional() {
    try {
      this.withInputModel("referenceToClassUnidirectional", "PersonList.ecore");
      this.loadModelFile();
      final EReference ref = this.refactorings.getEReference("PersonList", "Person", "works");
      this.refactorings.referenceToClass("WorkingPosition", ref);
      this.refactorings.saveModifiedEcores(AbstractTest.MODIFIED);
      this.assertModifiedFile();
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }
  
  @Test
  public void test_referenceToClassWithContainmentReference() {
    try {
      this.withInputModel("referenceToClassWithContainmentReference", "PersonList.ecore");
      this.loadModelFile();
      final EReference ref = this.refactorings.getEReference("PersonList", "Person", "works");
      this.refactorings.referenceToClass("WorkingPosition", ref);
      this.refactorings.saveModifiedEcores(AbstractTest.MODIFIED);
      this.assertModifiedFileIsSameAsOriginal();
      Assertions.assertThat(this.appender.getResult().trim()).isEqualTo("ERROR: PersonList.Person.works: Cannot apply referenceToClass on containment reference: PersonList.Person.works");
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
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
