package edelta.tests;

import com.google.inject.Inject;
import com.google.inject.Provider;
import edelta.interpreter.EdeltaInterpreterCleaner;
import edelta.tests.EdeltaInjectorProvider;
import java.util.function.Predicate;
import org.assertj.core.api.Assertions;
import org.eclipse.emf.common.notify.Adapter;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EcoreFactory;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.impl.ResourceImpl;
import org.eclipse.xtext.linking.impl.XtextLinkingDiagnostic;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.XtextRunner;
import org.eclipse.xtext.util.IResourceScopeCache;
import org.eclipse.xtext.xbase.lib.ObjectExtensions;
import org.eclipse.xtext.xbase.lib.Procedures.Procedure1;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

@RunWith(XtextRunner.class)
@InjectWith(EdeltaInjectorProvider.class)
@SuppressWarnings("all")
public class EdeltaInterpreterCleanerTest {
  public static class SpiedProvider implements Provider<String> {
    @Override
    public String get() {
      return "a string";
    }
  }
  
  private static final EcoreFactory ecoreFactory = EcoreFactory.eINSTANCE;
  
  @Inject
  private IResourceScopeCache cache;
  
  private EdeltaInterpreterCleaner cleaner;
  
  private EPackage ePackage;
  
  private Resource resource;
  
  private Provider<String> stringProvider;
  
  @Before
  public void setup() {
    EPackage _createEPackage = EdeltaInterpreterCleanerTest.ecoreFactory.createEPackage();
    final Procedure1<EPackage> _function = (EPackage it) -> {
      EList<EClassifier> _eClassifiers = it.getEClassifiers();
      EClass _createEClass = EdeltaInterpreterCleanerTest.ecoreFactory.createEClass();
      final Procedure1<EClass> _function_1 = (EClass it_1) -> {
        it_1.setName("AClass");
      };
      EClass _doubleArrow = ObjectExtensions.<EClass>operator_doubleArrow(_createEClass, _function_1);
      _eClassifiers.add(_doubleArrow);
    };
    EPackage _doubleArrow = ObjectExtensions.<EPackage>operator_doubleArrow(_createEPackage, _function);
    this.ePackage = _doubleArrow;
    ResourceImpl _resourceImpl = new ResourceImpl();
    this.resource = _resourceImpl;
    EdeltaInterpreterCleaner _edeltaInterpreterCleaner = new EdeltaInterpreterCleaner(this.cache, this.resource);
    this.cleaner = _edeltaInterpreterCleaner;
    EdeltaInterpreterCleanerTest.SpiedProvider _spiedProvider = new EdeltaInterpreterCleanerTest.SpiedProvider();
    this.stringProvider = Mockito.<Provider<String>>spy(_spiedProvider);
    EList<Adapter> _eAdapters = this.ePackage.eAdapters();
    _eAdapters.add(this.cleaner);
  }
  
  @Test
  public void testWhenEPackageDoesNotChangeCacheIsNotCleared() {
    this.cache.<String>get("key", this.resource, this.stringProvider);
    this.ePackage.getEClassifiers().get(0);
    this.cache.<String>get("key", this.resource, this.stringProvider);
    Mockito.<Provider<String>>verify(this.stringProvider, Mockito.times(1)).get();
  }
  
  @Test
  public void testWhenEPackageChangesCacheIsCleared() {
    this.cache.<String>get("key", this.resource, this.stringProvider);
    EClassifier _get = this.ePackage.getEClassifiers().get(0);
    _get.setName("Modified");
    this.cache.<String>get("key", this.resource, this.stringProvider);
    Mockito.<Provider<String>>verify(this.stringProvider, Mockito.times(2)).get();
  }
  
  @Test
  public void testClearXtextLinkingDiagnosticXtextLinkingDiagnosticWhenEPackageChanges() {
    this.resource.getErrors().add(Mockito.<Resource.Diagnostic>mock(Resource.Diagnostic.class));
    this.resource.getErrors().add(Mockito.<XtextLinkingDiagnostic>mock(XtextLinkingDiagnostic.class));
    this.resource.getWarnings().add(Mockito.<Resource.Diagnostic>mock(Resource.Diagnostic.class));
    this.resource.getWarnings().add(Mockito.<XtextLinkingDiagnostic>mock(XtextLinkingDiagnostic.class));
    EClassifier _get = this.ePackage.getEClassifiers().get(0);
    _get.setName("Modified");
    final Predicate<Resource.Diagnostic> _function = (Resource.Diagnostic it) -> {
      return (!(it instanceof XtextLinkingDiagnostic));
    };
    Assertions.<Resource.Diagnostic>assertThat(this.resource.getErrors()).hasSize(1).allMatch(_function);
    final Predicate<Resource.Diagnostic> _function_1 = (Resource.Diagnostic it) -> {
      return (!(it instanceof XtextLinkingDiagnostic));
    };
    Assertions.<Resource.Diagnostic>assertThat(this.resource.getWarnings()).hasSize(1).allMatch(_function_1);
  }
}
