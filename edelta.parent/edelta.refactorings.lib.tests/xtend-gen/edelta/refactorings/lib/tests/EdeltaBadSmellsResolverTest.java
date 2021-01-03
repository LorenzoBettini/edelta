package edelta.refactorings.lib.tests;

import com.google.common.base.Objects;
import com.google.common.collect.Iterables;
import edelta.lib.AbstractEdelta;
import edelta.refactorings.lib.EdeltaBadSmellsResolver;
import edelta.refactorings.lib.tests.AbstractTest;
import edelta.testutils.EdeltaTestUtils;
import java.util.Collections;
import java.util.function.Predicate;
import org.assertj.core.api.Assertions;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.xtext.xbase.lib.CollectionLiterals;
import org.eclipse.xtext.xbase.lib.Exceptions;
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
  
  private String testModelDirectory;
  
  private String testModelFile;
  
  @Before
  public void setup() {
    EdeltaBadSmellsResolver _edeltaBadSmellsResolver = new EdeltaBadSmellsResolver();
    this.resolver = _edeltaBadSmellsResolver;
  }
  
  private void loadModelFile(final String testModelDirectory, final String testModelFile) {
    this.testModelDirectory = testModelDirectory;
    this.testModelFile = testModelFile;
    this.resolver.loadEcoreFile((((AbstractTest.TESTECORES + testModelDirectory) + 
      "/") + testModelFile));
  }
  
  private void assertModifiedFile() {
    try {
      EdeltaTestUtils.assertFilesAreEquals(
        (((AbstractTest.EXPECTATIONS + 
          this.testModelDirectory) + 
          "/") + 
          this.testModelFile), 
        (AbstractTest.MODIFIED + this.testModelFile));
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }
  
  @Test
  public void test_ConstructorArgument() {
    EdeltaBadSmellsResolver _edeltaBadSmellsResolver = new EdeltaBadSmellsResolver(new AbstractEdelta() {
    });
    this.resolver = _edeltaBadSmellsResolver;
    Assertions.<EdeltaBadSmellsResolver>assertThat(this.resolver).isNotNull();
  }
  
  @Test
  public void test_resolveDuplicatedFeatures() {
    try {
      this.loadModelFile("resolveDuplicatedFeatures", "TestEcore.ecore");
      this.resolver.resolveDuplicatedFeatures(this.resolver.getEPackage("p"));
      this.resolver.saveModifiedEcores(AbstractTest.MODIFIED);
      this.assertModifiedFile();
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }
  
  @Test
  public void test_resolveDeadClassifiers() {
    try {
      this.loadModelFile("resolveDeadClassifiers", "TestEcore.ecore");
      final Predicate<EClassifier> _function = (EClassifier it) -> {
        String _name = it.getName();
        return Objects.equal(_name, "Unused2");
      };
      this.resolver.resolveDeadClassifiers(this.resolver.getEPackage("p"), _function);
      this.resolver.saveModifiedEcores(AbstractTest.MODIFIED);
      this.assertModifiedFile();
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }
  
  @Test
  public void test_resolveRedundantContainers() {
    try {
      this.loadModelFile("resolveRedundantContainers", "TestEcore.ecore");
      this.resolver.resolveRedundantContainers(this.resolver.getEPackage("p"));
      this.resolver.saveModifiedEcores(AbstractTest.MODIFIED);
      this.assertModifiedFile();
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }
  
  @Test
  public void test_resolveClassificationByHierarchy() {
    try {
      this.loadModelFile("resolveClassificationByHierarchy", "TestEcore.ecore");
      this.resolver.resolveClassificationByHierarchy(this.resolver.getEPackage("p"));
      this.resolver.saveModifiedEcores(AbstractTest.MODIFIED);
      this.assertModifiedFile();
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
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
