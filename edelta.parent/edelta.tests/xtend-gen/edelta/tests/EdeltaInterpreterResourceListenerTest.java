package edelta.tests;

import com.google.inject.Inject;
import com.google.inject.Provider;
import edelta.edelta.EdeltaFactory;
import edelta.interpreter.EdeltaInterpreterDiagnostic;
import edelta.interpreter.EdeltaInterpreterResourceListener;
import edelta.resource.derivedstate.EdeltaENamedElementXExpressionMap;
import edelta.tests.EdeltaInjectorProvider;
import java.util.function.Consumer;
import java.util.function.Predicate;
import org.assertj.core.api.Assertions;
import org.eclipse.emf.common.notify.Adapter;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.ENamedElement;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EcoreFactory;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.impl.ResourceImpl;
import org.eclipse.xtext.linking.impl.XtextLinkingDiagnostic;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.XtextRunner;
import org.eclipse.xtext.util.IResourceScopeCache;
import org.eclipse.xtext.validation.EObjectDiagnosticImpl;
import org.eclipse.xtext.xbase.XExpression;
import org.eclipse.xtext.xbase.lib.ObjectExtensions;
import org.eclipse.xtext.xbase.lib.Procedures.Procedure1;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

@RunWith(XtextRunner.class)
@InjectWith(EdeltaInjectorProvider.class)
@SuppressWarnings("all")
public class EdeltaInterpreterResourceListenerTest {
  public static class SpiedProvider implements Provider<String> {
    @Override
    public String get() {
      return "a string";
    }
  }
  
  private static final EcoreFactory ecoreFactory = EcoreFactory.eINSTANCE;
  
  private static final EdeltaFactory edeltaFactory = EdeltaFactory.eINSTANCE;
  
  @Inject
  private IResourceScopeCache cache;
  
  private EdeltaInterpreterResourceListener listener;
  
  private EPackage ePackage;
  
  private Resource resource;
  
  private EdeltaENamedElementXExpressionMap enamedElementXExpressionMap;
  
  private Provider<String> stringProvider;
  
  @Before
  public void setup() {
    EPackage _createEPackage = EdeltaInterpreterResourceListenerTest.ecoreFactory.createEPackage();
    final Procedure1<EPackage> _function = (EPackage it) -> {
      EList<EClassifier> _eClassifiers = it.getEClassifiers();
      EClass _createEClass = EdeltaInterpreterResourceListenerTest.ecoreFactory.createEClass();
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
    EdeltaENamedElementXExpressionMap _edeltaENamedElementXExpressionMap = new EdeltaENamedElementXExpressionMap();
    this.enamedElementXExpressionMap = _edeltaENamedElementXExpressionMap;
    EdeltaInterpreterResourceListener _edeltaInterpreterResourceListener = new EdeltaInterpreterResourceListener(this.cache, this.resource, this.enamedElementXExpressionMap);
    this.listener = _edeltaInterpreterResourceListener;
    EdeltaInterpreterResourceListenerTest.SpiedProvider _spiedProvider = new EdeltaInterpreterResourceListenerTest.SpiedProvider();
    this.stringProvider = Mockito.<Provider<String>>spy(_spiedProvider);
    EList<Adapter> _eAdapters = this.ePackage.eAdapters();
    _eAdapters.add(this.listener);
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
  
  @Test
  public void testClearEcoreReferenceExpressionDiagnosticWhenEPackageChanges() {
    final EObjectDiagnosticImpl ecoreRefExpDiagnosticError = this.createEObjectDiagnosticMock(EdeltaInterpreterResourceListenerTest.edeltaFactory.createEdeltaEcoreReferenceExpression());
    final EObjectDiagnosticImpl nonEcoreRefExpDiagnosticError = this.createEObjectDiagnosticMock(EdeltaInterpreterResourceListenerTest.ecoreFactory.createEClass());
    final Resource.Diagnostic nonEObjectDiagnostic = Mockito.<Resource.Diagnostic>mock(Resource.Diagnostic.class);
    this.resource.getErrors().add(nonEObjectDiagnostic);
    this.resource.getErrors().add(ecoreRefExpDiagnosticError);
    this.resource.getErrors().add(nonEcoreRefExpDiagnosticError);
    EClassifier _get = this.ePackage.getEClassifiers().get(0);
    _get.setName("Modified");
    Assertions.<Resource.Diagnostic>assertThat(this.resource.getErrors()).containsExactlyInAnyOrder(nonEObjectDiagnostic, nonEcoreRefExpDiagnosticError);
  }
  
  @Test
  public void testDoesNotClearEdeltaInterpreterDiagnosticWhenEPackageChanges() {
    this.resource.getErrors().add(Mockito.<EdeltaInterpreterDiagnostic>mock(EdeltaInterpreterDiagnostic.class));
    this.resource.getWarnings().add(Mockito.<EdeltaInterpreterDiagnostic>mock(EdeltaInterpreterDiagnostic.class));
    EClassifier _get = this.ePackage.getEClassifiers().get(0);
    _get.setName("Modified");
    Assertions.<Resource.Diagnostic>assertThat(this.resource.getErrors()).hasSize(1);
    Assertions.<Resource.Diagnostic>assertThat(this.resource.getWarnings()).hasSize(1);
  }
  
  @Test
  public void testENamedElementXExpressionMapIsUpdatedWithCurrentExpressionWhenNameIsChanged() {
    final XExpression currentExpression = Mockito.<XExpression>mock(XExpression.class);
    this.listener.setCurrentExpression(currentExpression);
    final EClassifier element = this.ePackage.getEClassifiers().get(0);
    Assertions.<ENamedElement, XExpression>assertThat(this.enamedElementXExpressionMap).isEmpty();
    element.setName("Modified");
    final Consumer<XExpression> _function = (XExpression it) -> {
      Assertions.<XExpression>assertThat(it).isSameAs(currentExpression);
    };
    Assertions.<ENamedElement, XExpression>assertThat(this.enamedElementXExpressionMap).hasEntrySatisfying(element, _function);
  }
  
  @Test
  public void testENamedElementXExpressionMapIsNotUpdatedIfEntryAlreadyPresent() {
    final XExpression alreadyMappedExpression = Mockito.<XExpression>mock(XExpression.class);
    final XExpression anotherExpression = Mockito.<XExpression>mock(XExpression.class);
    final EClassifier element = this.ePackage.getEClassifiers().get(0);
    this.enamedElementXExpressionMap.put(element, alreadyMappedExpression);
    this.listener.setCurrentExpression(anotherExpression);
    element.setName("Modified");
    final Consumer<XExpression> _function = (XExpression it) -> {
      Assertions.<XExpression>assertThat(it).isSameAs(alreadyMappedExpression);
    };
    Assertions.<ENamedElement, XExpression>assertThat(this.enamedElementXExpressionMap).hasEntrySatisfying(element, _function);
  }
  
  @Test
  public void testENamedElementXExpressionMapIsNotUpdatedWithCurrentExpressionWhenAnotherFeatureIsChanged() {
    final XExpression currentExpression = Mockito.<XExpression>mock(XExpression.class);
    this.listener.setCurrentExpression(currentExpression);
    Assertions.<ENamedElement, XExpression>assertThat(this.enamedElementXExpressionMap).isEmpty();
    EClassifier _get = this.ePackage.getEClassifiers().get(0);
    _get.setInstanceClassName("foo");
    Assertions.<ENamedElement, XExpression>assertThat(this.enamedElementXExpressionMap).isEmpty();
  }
  
  public EObjectDiagnosticImpl createEObjectDiagnosticMock(final EObject problematicObject) {
    EObjectDiagnosticImpl _mock = Mockito.<EObjectDiagnosticImpl>mock(EObjectDiagnosticImpl.class);
    final Procedure1<EObjectDiagnosticImpl> _function = (EObjectDiagnosticImpl it) -> {
      Mockito.<EObject>when(it.getProblematicObject()).thenReturn(problematicObject);
    };
    return ObjectExtensions.<EObjectDiagnosticImpl>operator_doubleArrow(_mock, _function);
  }
}
