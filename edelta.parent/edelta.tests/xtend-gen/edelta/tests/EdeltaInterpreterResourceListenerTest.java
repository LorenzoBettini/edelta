package edelta.tests;

import com.google.inject.Inject;
import com.google.inject.Provider;
import edelta.edelta.EdeltaFactory;
import edelta.interpreter.EdeltaInterpreterDiagnostic;
import edelta.interpreter.EdeltaInterpreterDiagnosticHelper;
import edelta.interpreter.EdeltaInterpreterResourceListener;
import edelta.lib.EdeltaLibrary;
import edelta.resource.derivedstate.EdeltaDerivedStateHelper;
import edelta.resource.derivedstate.EdeltaENamedElementXExpressionMap;
import edelta.resource.derivedstate.EdeltaModifiedElements;
import edelta.tests.EdeltaAbstractTest;
import edelta.tests.EdeltaInjectorProvider;
import edelta.validation.EdeltaValidator;
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
import org.eclipse.xtext.linking.impl.XtextLinkingDiagnostic;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.XtextRunner;
import org.eclipse.xtext.util.IResourceScopeCache;
import org.eclipse.xtext.validation.EObjectDiagnosticImpl;
import org.eclipse.xtext.validation.Issue;
import org.eclipse.xtext.xbase.XAssignment;
import org.eclipse.xtext.xbase.XExpression;
import org.eclipse.xtext.xbase.XbaseFactory;
import org.eclipse.xtext.xbase.XbasePackage;
import org.eclipse.xtext.xbase.lib.Exceptions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

@RunWith(XtextRunner.class)
@InjectWith(EdeltaInjectorProvider.class)
@SuppressWarnings("all")
public class EdeltaInterpreterResourceListenerTest extends EdeltaAbstractTest {
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
  
  @Inject
  private EdeltaInterpreterDiagnosticHelper diagnosticHelper;
  
  @Inject
  private EdeltaDerivedStateHelper derivedStateHelper;
  
  private EdeltaInterpreterResourceListener listener;
  
  private EPackage ePackage;
  
  private Resource resource;
  
  private EdeltaENamedElementXExpressionMap enamedElementXExpressionMap;
  
  private EdeltaModifiedElements modifiedElements;
  
  private Provider<String> stringProvider;
  
  @Before
  public void setup() {
    try {
      final Consumer<EPackage> _function = (EPackage it) -> {
        EdeltaLibrary.addNewEClass(it, "AClass");
      };
      this.ePackage = this.createEPackage("aPackage", _function);
      this.resource = this.parseHelper.parse("").eResource();
      this.enamedElementXExpressionMap = this.derivedStateHelper.getEnamedElementXExpressionMap(this.resource);
      this.modifiedElements = this.derivedStateHelper.getModifiedElements(this.resource);
      EdeltaInterpreterResourceListener _edeltaInterpreterResourceListener = new EdeltaInterpreterResourceListener(
        this.cache, this.resource, this.derivedStateHelper, this.diagnosticHelper);
      this.listener = _edeltaInterpreterResourceListener;
      EdeltaInterpreterResourceListenerTest.SpiedProvider _spiedProvider = new EdeltaInterpreterResourceListenerTest.SpiedProvider();
      this.stringProvider = Mockito.<Provider<String>>spy(_spiedProvider);
      EList<Adapter> _eAdapters = this.ePackage.eAdapters();
      _eAdapters.add(this.listener);
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
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
  public void testENamedElementXExpressionMapUpdatedIfEntryAlreadyPresent() {
    final XExpression alreadyMappedExpression = Mockito.<XExpression>mock(XExpression.class);
    final XExpression currentExpression = Mockito.<XExpression>mock(XExpression.class);
    final EClassifier element = this.ePackage.getEClassifiers().get(0);
    this.enamedElementXExpressionMap.put(element, alreadyMappedExpression);
    this.listener.setCurrentExpression(currentExpression);
    element.setName("Modified");
    final Consumer<XExpression> _function = (XExpression it) -> {
      Assertions.<XExpression>assertThat(it).isSameAs(currentExpression);
    };
    Assertions.<ENamedElement, XExpression>assertThat(this.enamedElementXExpressionMap).hasEntrySatisfying(element, _function);
  }
  
  @Test
  public void testENamedElementXExpressionMapIsUpdatedWithCurrentExpressionWhenAnElementIsAdded() {
    final XExpression currentExpression = Mockito.<XExpression>mock(XExpression.class);
    this.listener.setCurrentExpression(currentExpression);
    final EClass element = EdeltaInterpreterResourceListenerTest.ecoreFactory.createEClass();
    Assertions.<ENamedElement, XExpression>assertThat(this.enamedElementXExpressionMap).isEmpty();
    EList<EClassifier> _eClassifiers = this.ePackage.getEClassifiers();
    _eClassifiers.add(element);
    final Consumer<XExpression> _function = (XExpression it) -> {
      Assertions.<XExpression>assertThat(it).isSameAs(currentExpression);
    };
    Assertions.<ENamedElement, XExpression>assertThat(this.enamedElementXExpressionMap).hasEntrySatisfying(element, _function);
  }
  
  @Test
  public void testENamedElementXExpressionMapIsNotUpdatedWhenNotENamedElementIsAdded() {
    final XExpression currentExpression = Mockito.<XExpression>mock(XExpression.class);
    this.listener.setCurrentExpression(currentExpression);
    Assertions.<ENamedElement, XExpression>assertThat(this.enamedElementXExpressionMap).isEmpty();
    EClassifier _get = this.ePackage.getEClassifiers().get(0);
    final EClass element = ((EClass) _get);
    EList<EClass> _eSuperTypes = element.getESuperTypes();
    EClass _createEClass = EdeltaInterpreterResourceListenerTest.ecoreFactory.createEClass();
    _eSuperTypes.add(_createEClass);
    Assertions.<ENamedElement, XExpression>assertThat(this.enamedElementXExpressionMap).doesNotContainKey(element);
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
  
  @Test
  public void testEPackageCycleWhenAddingSubpackage() {
    final XAssignment currentExpression = XbaseFactory.eINSTANCE.createXAssignment();
    EList<EObject> _contents = this.resource.getContents();
    _contents.add(currentExpression);
    this.diagnosticHelper.setCurrentExpression(currentExpression);
    final EPackage subpackage = this.createEPackage("subpackage");
    EList<EPackage> _eSubpackages = this.ePackage.getESubpackages();
    _eSubpackages.add(subpackage);
    EList<EPackage> _eSubpackages_1 = subpackage.getESubpackages();
    _eSubpackages_1.add(this.ePackage);
    this.validationTestHelper.assertError(this.resource, 
      XbasePackage.eINSTANCE.getXAssignment(), 
      EdeltaValidator.EPACKAGE_CYCLE, 
      "Cycle in superpackage/subpackage: aPackage.subpackage.aPackage");
    Assertions.<Issue>assertThat(this.validationTestHelper.validate(this.resource)).hasSize(1);
    Assertions.<EPackage>assertThat(subpackage.getESubpackages()).isEmpty();
    Assertions.<EPackage>assertThat(this.ePackage.getESubpackages()).containsOnly(subpackage);
  }
  
  @Test
  public void testEClassCycleWhenAddingSuperType() {
    final XAssignment currentExpression = XbaseFactory.eINSTANCE.createXAssignment();
    EList<EObject> _contents = this.resource.getContents();
    _contents.add(currentExpression);
    this.diagnosticHelper.setCurrentExpression(currentExpression);
    final EClass c1 = EdeltaLibrary.addNewEClass(this.ePackage, "c1");
    final EClass c2 = EdeltaLibrary.addNewEClass(this.ePackage, "c2");
    final EClass c3 = EdeltaLibrary.addNewEClass(this.ePackage, "c3");
    EList<EClass> _eSuperTypes = c3.getESuperTypes();
    _eSuperTypes.add(c2);
    this.validationTestHelper.assertNoIssues(this.resource);
    EList<EClass> _eSuperTypes_1 = c2.getESuperTypes();
    _eSuperTypes_1.add(c1);
    this.validationTestHelper.assertNoIssues(this.resource);
    EList<EClass> _eSuperTypes_2 = c1.getESuperTypes();
    _eSuperTypes_2.add(c3);
    this.validationTestHelper.assertError(this.resource, 
      XbasePackage.eINSTANCE.getXAssignment(), 
      EdeltaValidator.ECLASS_CYCLE, 
      "Cycle in inheritance hierarchy: aPackage.c3");
    Assertions.<Issue>assertThat(this.validationTestHelper.validate(this.resource)).hasSize(1);
  }
  
  @Test
  public void testModifiedElementsIsUpdatedWhenNameIsChanged() {
    final XExpression currentExpression = Mockito.<XExpression>mock(XExpression.class);
    this.listener.setCurrentExpression(currentExpression);
    final EClass element = EdeltaInterpreterResourceListenerTest.ecoreFactory.createEClass();
    EList<EClassifier> _eClassifiers = this.ePackage.getEClassifiers();
    _eClassifiers.add(element);
    Assertions.<ENamedElement>assertThat(this.modifiedElements).containsExactlyInAnyOrder(element, this.ePackage);
  }
  
  @Test
  public void testModifiedElementsIsUpdatedWhenElementIsAdded() {
    final XExpression currentExpression = Mockito.<XExpression>mock(XExpression.class);
    this.listener.setCurrentExpression(currentExpression);
    final EClassifier element = this.ePackage.getEClassifiers().get(0);
    element.setName("Modified");
    Assertions.<ENamedElement>assertThat(this.modifiedElements).containsExactlyInAnyOrder(element, this.ePackage);
  }
  
  @Test
  public void testModifiedElementsIsEmptyWhenNothingIsChanged() {
    EList<Adapter> _eAdapters = this.ePackage.eAdapters();
    _eAdapters.remove(this.listener);
    Assertions.<ENamedElement>assertThat(this.modifiedElements).isEmpty();
  }
  
  public EObjectDiagnosticImpl createEObjectDiagnosticMock(final EObject problematicObject) {
    final EObjectDiagnosticImpl d = Mockito.<EObjectDiagnosticImpl>mock(EObjectDiagnosticImpl.class);
    Mockito.<EObject>when(d.getProblematicObject()).thenReturn(problematicObject);
    return d;
  }
}
