package edelta.tests;

import static edelta.lib.EdeltaLibrary.addNewEClass;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EcoreFactory;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.xtext.linking.impl.XtextLinkingDiagnostic;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.XtextRunner;
import org.eclipse.xtext.util.IResourceScopeCache;
import org.eclipse.xtext.validation.EObjectDiagnosticImpl;
import org.eclipse.xtext.xbase.XExpression;
import org.eclipse.xtext.xbase.XbaseFactory;
import org.eclipse.xtext.xbase.XbasePackage;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.inject.Inject;
import com.google.inject.Provider;

import edelta.edelta.EdeltaFactory;
import edelta.interpreter.EdeltaInterpreterDiagnostic;
import edelta.interpreter.EdeltaInterpreterDiagnosticHelper;
import edelta.interpreter.EdeltaInterpreterResourceListener;
import edelta.lib.EdeltaLibrary;
import edelta.resource.derivedstate.EdeltaENamedElementXExpressionMap;
import edelta.resource.derivedstate.EdeltaModifiedElements;
import edelta.validation.EdeltaValidator;

@RunWith(XtextRunner.class)
@InjectWith(EdeltaInjectorProvider.class)
public class EdeltaInterpreterResourceListenerTest extends EdeltaAbstractTest {
	public static class SpiedProvider implements Provider<String> {
		@Override
		public String get() {
			return "a string";
		}
	}

	private static EcoreFactory ecoreFactory = EcoreFactory.eINSTANCE;

	private static EdeltaFactory edeltaFactory = EdeltaFactory.eINSTANCE;

	@Inject
	private IResourceScopeCache cache;

	@Inject
	private EdeltaInterpreterDiagnosticHelper diagnosticHelper;

	private EdeltaInterpreterResourceListener listener;

	private EPackage ePackage;

	private Resource resource;

	private EdeltaENamedElementXExpressionMap enamedElementXExpressionMap;

	private EdeltaModifiedElements modifiedElements;

	private Provider<String> stringProvider;

	@Before
	public void setup() throws Exception {
		ePackage = createEPackage("aPackage",
			p -> addNewEClass(p, "AClass"));
		resource = parseHelper.parse("").eResource();
		enamedElementXExpressionMap = derivedStateHelper.getEnamedElementXExpressionMap(resource);
		modifiedElements = derivedStateHelper.getModifiedElements(resource);
		listener = new EdeltaInterpreterResourceListener(cache, resource,
			derivedStateHelper, diagnosticHelper);
		stringProvider = spy(new SpiedProvider());
		ePackage.eAdapters().add(listener);
	}

	@Test
	public void testWhenEPackageDoesNotChangeCacheIsNotCleared() {
		// use the cache the first time
		cache.get("key", resource, stringProvider);
		// don't change the package contents
		ePackage.getEClassifiers().get(0);
		// use the cache the second time
		cache.get("key", resource, stringProvider);
		// make sure the Provider is not called again
		verify(stringProvider, times(1)).get();
	}

	@Test
	public void testWhenEPackageChangesCacheIsCleared() {
		// use the cache the first time
		cache.get("key", resource, stringProvider);
		// change the package contents
		ePackage.getEClassifiers().get(0).setName("Modified");
		// use the cache the second time
		cache.get("key", resource, stringProvider);
		// make sure the Provider is called again
		verify(stringProvider, times(2)).get();
	}

	@Test
	public void testClearXtextLinkingDiagnosticXtextLinkingDiagnosticWhenEPackageChanges() {
		// fill the resource with errors and warnings
		resource.getErrors().add(mock(Resource.Diagnostic.class));
		resource.getErrors().add(mock(XtextLinkingDiagnostic.class));
		resource.getWarnings().add(mock(Resource.Diagnostic.class));
		resource.getWarnings().add(mock(XtextLinkingDiagnostic.class));
		// change the package contents
		ePackage.getEClassifiers().get(0).setName("Modified");
		// make sure the XtextLinkingDiagnostics are removed
		assertThat(resource.getErrors())
			.hasSize(1)
			.allMatch(it -> (!(it instanceof XtextLinkingDiagnostic)));
		assertThat(resource.getWarnings())
			.hasSize(1)
			.allMatch(it -> (!(it instanceof XtextLinkingDiagnostic)));
	}

	@Test
	public void testClearEcoreReferenceExpressionDiagnosticWhenEPackageChanges() {
		// fill the resource with EObjectDiagnosticImpl
		var ecoreRefExpDiagnosticError = createEObjectDiagnosticMock(
				edeltaFactory.createEdeltaEcoreReferenceExpression());
		var nonEcoreRefExpDiagnosticError = createEObjectDiagnosticMock(
				ecoreFactory.createEClass());
		var nonEObjectDiagnostic = mock(Resource.Diagnostic.class);
		resource.getErrors().add(nonEObjectDiagnostic);
		resource.getErrors().add(ecoreRefExpDiagnosticError);
		resource.getErrors().add(nonEcoreRefExpDiagnosticError);
		// change the package contents
		ePackage.getEClassifiers().get(0).setName("Modified");
		// make sure only EObjectDiagnosticImpl with EdeltaEcoreReferenceExpression are removed
		assertThat(resource.getErrors())
			.containsExactlyInAnyOrder(
				nonEObjectDiagnostic,
				nonEcoreRefExpDiagnosticError);
	}

	@Test
	public void testDoesNotClearEdeltaInterpreterDiagnosticWhenEPackageChanges() {
		// fill the resource with errors and warnings
		resource.getErrors().add(mock(EdeltaInterpreterDiagnostic.class));
		resource.getWarnings().add(mock(EdeltaInterpreterDiagnostic.class));
		// change the package contents
		ePackage.getEClassifiers().get(0).setName("Modified");
		// make sure the XtextLinkingDiagnostics are removed
		assertThat(resource.getErrors()).hasSize(1);
		assertThat(resource.getWarnings()).hasSize(1);
	}

	@Test
	public void testENamedElementXExpressionMapIsUpdatedWithCurrentExpressionWhenNameIsChanged() {
		var currentExpression = mock(XExpression.class);
		listener.setCurrentExpression(currentExpression);
		var element = ePackage.getEClassifiers().get(0);
		assertThat(enamedElementXExpressionMap).isEmpty();
		// change the name
		element.setName("Modified");
		assertThat(enamedElementXExpressionMap)
			.hasEntrySatisfying(element,
				it -> assertThat(it).isSameAs(currentExpression));
	}

	@Test
	public void testENamedElementXExpressionMapUpdatedIfEntryAlreadyPresent() {
		var alreadyMappedExpression = mock(XExpression.class);
		var currentExpression = mock(XExpression.class);
		var element = ePackage.getEClassifiers().get(0);
		enamedElementXExpressionMap.put(element, alreadyMappedExpression);
		listener.setCurrentExpression(currentExpression);
		// change the name
		element.setName("Modified");
		assertThat(enamedElementXExpressionMap)
			.hasEntrySatisfying(element,
				it -> assertThat(it).isSameAs(currentExpression));
	}

	@Test
	public void testENamedElementXExpressionMapIsUpdatedWithCurrentExpressionWhenAnElementIsAdded() {
		var currentExpression = mock(XExpression.class);
		listener.setCurrentExpression(currentExpression);
		var element = ecoreFactory.createEClass();
		assertThat(enamedElementXExpressionMap).isEmpty();
		// element is added to an existing collection
		ePackage.getEClassifiers().add(element);
		assertThat(enamedElementXExpressionMap)
			.hasEntrySatisfying(element,
				it -> assertThat(it).isSameAs(currentExpression));
	}

	@Test
	public void testENamedElementXExpressionMapIsNotUpdatedWhenNotENamedElementIsAdded() {
		var currentExpression = mock(XExpression.class);
		listener.setCurrentExpression(currentExpression);
		assertThat(enamedElementXExpressionMap).isEmpty();
		var element = (EClass) ePackage.getEClassifiers().get(0);
		// add supertype
		element.getESuperTypes().add(ecoreFactory.createEClass());
		// this will trigger an ADD event with a EGenericType,
		// which is not an ENamedElement
		assertThat(enamedElementXExpressionMap)
			.doesNotContainKey(element);
	}

	@Test
	public void testENamedElementXExpressionMapIsNotUpdatedWithCurrentExpressionWhenAnotherFeatureIsChanged() {
		var currentExpression = mock(XExpression.class);
		listener.setCurrentExpression(currentExpression);
		assertThat(enamedElementXExpressionMap).isEmpty();
		// change the something different than the name
		ePackage.getEClassifiers().get(0).setInstanceClassName("foo");
		assertThat(enamedElementXExpressionMap).isEmpty();
	}

	@Test
	public void testEPackageCycleWhenAddingSubpackage() {
		var currentExpression = XbaseFactory.eINSTANCE.createXAssignment();
		resource.getContents().add(currentExpression);
		diagnosticHelper.setCurrentExpression(currentExpression);
		var subpackage = createEPackage("subpackage");
		ePackage.getESubpackages().add(subpackage);
		subpackage.getESubpackages().add(ePackage);
		validationTestHelper.assertError(
			resource,
			XbasePackage.eINSTANCE.getXAssignment(),
			EdeltaValidator.EPACKAGE_CYCLE,
			"Cycle in superpackage/subpackage: aPackage.subpackage.aPackage");
		assertThat(validationTestHelper.validate(resource)).hasSize(1);
		// check that the listener broke the cycle in the model
		assertThat(subpackage.getESubpackages()).isEmpty();
		assertThat(ePackage.getESubpackages()).containsOnly(subpackage);
	}

	@Test
	public void testEClassCycleWhenAddingSuperType() {
		var currentExpression = XbaseFactory.eINSTANCE.createXAssignment();
		resource.getContents().add(currentExpression);
		diagnosticHelper.setCurrentExpression(currentExpression);
		var c1 = EdeltaLibrary.addNewEClass(ePackage, "c1");
		var c2 = EdeltaLibrary.addNewEClass(ePackage, "c2");
		var c3 = EdeltaLibrary.addNewEClass(ePackage, "c3");
		c3.getESuperTypes().add(c2);
		validationTestHelper.assertNoIssues(resource);
		c2.getESuperTypes().add(c1);
		validationTestHelper.assertNoIssues(resource);
		c1.getESuperTypes().add(c3);
		validationTestHelper.assertError(
			resource,
			XbasePackage.eINSTANCE.getXAssignment(),
			EdeltaValidator.ECLASS_CYCLE,
			"Cycle in inheritance hierarchy: aPackage.c3");
		assertThat(validationTestHelper.validate(resource)).hasSize(1);
	}

	@Test
	public void testModifiedElementsIsUpdatedWhenNameIsChanged() {
		var currentExpression = mock(XExpression.class);
		listener.setCurrentExpression(currentExpression);
		var element = ePackage.getEClassifiers().get(0);
		// change the name
		element.setName("Modified");
		assertThat(modifiedElements)
			.containsExactlyInAnyOrder(element, ePackage);
	}

	@Test
	public void testModifiedElementsIsUpdatedWhenElementIsAdded() {
		var currentExpression = mock(XExpression.class);
		listener.setCurrentExpression(currentExpression);
		var element = ecoreFactory.createEClass();
		ePackage.getEClassifiers().add(element);
		assertThat(modifiedElements)
			.containsExactlyInAnyOrder(element, ePackage);
	}

	@Test
	public void testModifiedElementsIsUpdatedWhenSeveralElementsAreAdded() {
		var currentExpression = mock(XExpression.class);
		listener.setCurrentExpression(currentExpression);
		var element = (EClass) ePackage.getEClassifiers().get(0);
		var f1 = ecoreFactory.createEAttribute();
		var f2 = ecoreFactory.createEReference();
		element.getEStructuralFeatures().addAll(Arrays.asList(f1, f2));
		assertThat(modifiedElements)
			.containsExactlyInAnyOrder(element, ePackage);
		// TODO: should contain also f1 and f2
	}

	@Test
	public void testModifiedElementsIsEmptyWhenNothingIsChanged() {
		ePackage.eAdapters().remove(listener);
		assertThat(modifiedElements).isEmpty();
	}

	public EObjectDiagnosticImpl createEObjectDiagnosticMock(EObject problematicObject) {
		var d = mock(EObjectDiagnosticImpl.class);
		when(d.getProblematicObject()).thenReturn(problematicObject);
		return d;
	}
}
