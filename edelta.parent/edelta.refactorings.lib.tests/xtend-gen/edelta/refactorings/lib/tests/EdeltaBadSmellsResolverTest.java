package edelta.refactorings.lib.tests;

import com.google.common.base.Objects;
import edelta.lib.AbstractEdelta;
import edelta.lib.EdeltaLibrary;
import edelta.refactorings.lib.EdeltaBadSmellsResolver;
import edelta.refactorings.lib.tests.AbstractTest;
import edelta.testutils.EdeltaTestUtils;
import java.util.function.Consumer;
import java.util.function.Predicate;
import org.assertj.core.api.Assertions;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.xtext.xbase.lib.Exceptions;
import org.eclipse.xtext.xbase.lib.IterableExtensions;
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
    try {
      this.loadModelFile("resolveConcreteAbstractMetaclass", "TestEcore.ecore");
      this.resolver.resolveConcreteAbstractMetaclass(this.resolver.getEPackage("p"));
      this.resolver.saveModifiedEcores(AbstractTest.MODIFIED);
      this.assertModifiedFile();
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }
  
  @Test
  public void test_resolveAbstractConcreteMetaclass() {
    final Consumer<EPackage> _function = (EPackage it) -> {
      EdeltaLibrary.addNewAbstractEClass(it, "AbstractConcreteMetaclass");
    };
    final EPackage p = this.createEPackage("p", _function);
    final EClass c = IterableExtensions.<EClass>head(this.EClasses(p));
    this.resolver.resolveAbstractConcreteMetaclass(p);
    Assertions.assertThat(c.isAbstract()).isFalse();
  }
  
  @Test
  public void test_resolveAbstractSubclassesOfConcreteSuperclasses() {
    try {
      this.loadModelFile("resolveAbstractSubclassesOfConcreteSuperclasses", "TestEcore.ecore");
      this.resolver.resolveAbstractSubclassesOfConcreteSuperclasses(this.resolver.getEPackage("p"));
      this.resolver.saveModifiedEcores(AbstractTest.MODIFIED);
      this.assertModifiedFile();
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }
  
  @Test
  public void test_resolveDuplicateFeaturesInSubclasses() {
    try {
      this.loadModelFile("resolveDuplicateFeaturesInSubclasses", "TestEcore.ecore");
      this.resolver.resolveDuplicateFeaturesInSubclasses(this.resolver.getEPackage("p"));
      this.resolver.saveModifiedEcores(AbstractTest.MODIFIED);
      this.assertModifiedFile();
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }
}
