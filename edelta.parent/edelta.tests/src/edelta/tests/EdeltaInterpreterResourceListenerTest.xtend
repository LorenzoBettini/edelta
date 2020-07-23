package edelta.tests

import com.google.inject.Inject
import com.google.inject.Provider
import edelta.edelta.EdeltaFactory
import edelta.interpreter.EdeltaInterpreterDiagnostic
import edelta.interpreter.EdeltaInterpreterDiagnosticHelper
import edelta.interpreter.EdeltaInterpreterResourceListener
import edelta.resource.derivedstate.EdeltaENamedElementXExpressionMap
import edelta.validation.EdeltaValidator
import org.eclipse.emf.ecore.EClass
import org.eclipse.emf.ecore.EObject
import org.eclipse.emf.ecore.EPackage
import org.eclipse.emf.ecore.EcoreFactory
import org.eclipse.emf.ecore.resource.Resource
import org.eclipse.emf.ecore.resource.Resource.Diagnostic
import org.eclipse.xtext.linking.impl.XtextLinkingDiagnostic
import org.eclipse.xtext.testing.InjectWith
import org.eclipse.xtext.testing.XtextRunner
import org.eclipse.xtext.util.IResourceScopeCache
import org.eclipse.xtext.validation.EObjectDiagnosticImpl
import org.eclipse.xtext.xbase.XExpression
import org.eclipse.xtext.xbase.XbaseFactory
import org.eclipse.xtext.xbase.XbasePackage
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

import static org.assertj.core.api.Assertions.assertThat
import static org.mockito.Mockito.*
import edelta.resource.derivedstate.EdeltaDerivedStateHelper
import edelta.resource.derivedstate.EdeltaModifiedElements

@RunWith(XtextRunner)
@InjectWith(EdeltaInjectorProvider)
class EdeltaInterpreterResourceListenerTest extends EdeltaAbstractTest {

	static class SpiedProvider implements Provider<String> {
		override get() {
			"a string"
		}
	}

	static val ecoreFactory = EcoreFactory.eINSTANCE
	static val edeltaFactory = EdeltaFactory.eINSTANCE

	@Inject IResourceScopeCache cache
	@Inject EdeltaInterpreterDiagnosticHelper diagnosticHelper
	@Inject EdeltaDerivedStateHelper derivedStateHelper
	var EdeltaInterpreterResourceListener listener
	var EPackage ePackage
	var Resource resource
	var EdeltaENamedElementXExpressionMap enamedElementXExpressionMap
	var EdeltaModifiedElements modifiedElements
	var Provider<String> stringProvider

	@Before
	def void setup() {
		ePackage = ecoreFactory.createEPackage => [
			name = "aPackage"
			EClassifiers += ecoreFactory.createEClass => [
				name = "AClass"
			]
		]
		resource = "".parse.eResource
		enamedElementXExpressionMap = derivedStateHelper.getEnamedElementXExpressionMap(resource)
		modifiedElements = derivedStateHelper.getModifiedElements(resource)
		listener = new EdeltaInterpreterResourceListener(
			cache, resource, derivedStateHelper, diagnosticHelper
		)
		stringProvider = spy(new SpiedProvider)
		ePackage.eAdapters += listener
	}

	@Test
	def void testWhenEPackageDoesNotChangeCacheIsNotCleared() {
		// use the cache the first time
		cache.get("key", resource, stringProvider)
		// don't change the package contents
		ePackage.EClassifiers.get(0)
		// use the cache the second time
		cache.get("key", resource, stringProvider)
		// make sure the Provider is not called again
		verify(stringProvider, times(1)).get
	}

	@Test
	def void testWhenEPackageChangesCacheIsCleared() {
		// use the cache the first time
		cache.get("key", resource, stringProvider)
		// change the package contents
		ePackage.EClassifiers.get(0).name = "Modified"
		// use the cache the second time
		cache.get("key", resource, stringProvider)
		// make sure the Provider is called again
		verify(stringProvider, times(2)).get
	}

	@Test
	def void testClearXtextLinkingDiagnosticXtextLinkingDiagnosticWhenEPackageChanges() {
		// fill the resource with errors and warnings
		resource.errors.add(mock(Diagnostic))
		resource.errors.add(mock(XtextLinkingDiagnostic))
		resource.warnings.add(mock(Diagnostic))
		resource.warnings.add(mock(XtextLinkingDiagnostic))
		// change the package contents
		ePackage.EClassifiers.get(0).name = "Modified"
		// make sure the XtextLinkingDiagnostics are removed
		assertThat(resource.errors)
			.hasSize(1)
			.allMatch[!(it instanceof XtextLinkingDiagnostic)]
		assertThat(resource.warnings)
			.hasSize(1)
			.allMatch[!(it instanceof XtextLinkingDiagnostic)]
	}

	@Test
	def void testClearEcoreReferenceExpressionDiagnosticWhenEPackageChanges() {
		// fill the resource with EObjectDiagnosticImpl
		val ecoreRefExpDiagnosticError =
			createEObjectDiagnosticMock(edeltaFactory.createEdeltaEcoreReferenceExpression)
		val nonEcoreRefExpDiagnosticError =
			createEObjectDiagnosticMock(ecoreFactory.createEClass)
		val nonEObjectDiagnostic = mock(Diagnostic)
		resource.errors.add(nonEObjectDiagnostic)
		resource.errors.add(ecoreRefExpDiagnosticError)
		resource.errors.add(nonEcoreRefExpDiagnosticError)
		// change the package contents
		ePackage.EClassifiers.get(0).name = "Modified"
		// make sure only EObjectDiagnosticImpl with EdeltaEcoreReferenceExpression are removed
		assertThat(resource.errors)
			.containsExactlyInAnyOrder(nonEObjectDiagnostic, nonEcoreRefExpDiagnosticError)
	}

	@Test
	def void testDoesNotClearEdeltaInterpreterDiagnosticWhenEPackageChanges() {
		// fill the resource with errors and warnings
		resource.errors.add(mock(EdeltaInterpreterDiagnostic))
		resource.warnings.add(mock(EdeltaInterpreterDiagnostic))
		// change the package contents
		ePackage.EClassifiers.get(0).name = "Modified"
		// make sure the XtextLinkingDiagnostics are removed
		assertThat(resource.errors)
			.hasSize(1)
		assertThat(resource.warnings)
			.hasSize(1)
	}

	@Test
	def void testENamedElementXExpressionMapIsUpdatedWithCurrentExpressionWhenNameIsChanged() {
		val currentExpression = mock(XExpression)
		listener.setCurrentExpression(currentExpression)
		val element = ePackage.EClassifiers.get(0)
		assertThat(enamedElementXExpressionMap).isEmpty
		// change the name
		element.name = "Modified"
		assertThat(enamedElementXExpressionMap)
			.hasEntrySatisfying(element) [
				assertThat(it).isSameAs(currentExpression)
			]
	}

	@Test
	def void testENamedElementXExpressionMapUpdatedIfEntryAlreadyPresent() {
		val alreadyMappedExpression = mock(XExpression)
		val currentExpression = mock(XExpression)
		val element = ePackage.EClassifiers.get(0)
		enamedElementXExpressionMap.put(element, alreadyMappedExpression)
		listener.setCurrentExpression(currentExpression)
		// change the name
		element.name = "Modified"
		assertThat(enamedElementXExpressionMap)
			.hasEntrySatisfying(element) [
				assertThat(it).isSameAs(currentExpression)
			]
	}

	@Test
	def void testENamedElementXExpressionMapIsUpdatedWithCurrentExpressionWhenAnElementIsAdded() {
		val currentExpression = mock(XExpression)
		listener.setCurrentExpression(currentExpression)
		val element = ecoreFactory.createEClass
		assertThat(enamedElementXExpressionMap).isEmpty
		// element is added to an existing collection
		ePackage.EClassifiers += element
		assertThat(enamedElementXExpressionMap)
			.hasEntrySatisfying(element) [
				assertThat(it).isSameAs(currentExpression)
			]
	}

	@Test
	def void testENamedElementXExpressionMapIsNotUpdatedWhenNotENamedElementIsAdded() {
		val currentExpression = mock(XExpression)
		listener.setCurrentExpression(currentExpression)
		assertThat(enamedElementXExpressionMap).isEmpty
		val element = ePackage.EClassifiers.get(0) as EClass
		// add supertype
		element.ESuperTypes += ecoreFactory.createEClass
		// this will trigger an ADD event with a EGenericType,
		// which is not an ENamedElement
		assertThat(enamedElementXExpressionMap)
			.doesNotContainKey(element)
	}

	@Test
	def void testENamedElementXExpressionMapIsNotUpdatedWithCurrentExpressionWhenAnotherFeatureIsChanged() {
		val currentExpression = mock(XExpression)
		listener.setCurrentExpression(currentExpression)
		assertThat(enamedElementXExpressionMap).isEmpty
		// change the something different than the name
		ePackage.EClassifiers.get(0).instanceClassName = "foo"
		assertThat(enamedElementXExpressionMap).isEmpty
	}

	@Test
	def void testEPackageCycleWhenAddingSubpackage() {
		val currentExpression = XbaseFactory.eINSTANCE.createXAssignment
		resource.contents += currentExpression
		diagnosticHelper.setCurrentExpression(currentExpression)
		val subpackage = ecoreFactory.createEPackage => [
			name = "subpackage"
		]
		ePackage.ESubpackages += subpackage
		subpackage.ESubpackages += ePackage
		resource.assertError(
			XbasePackage.eINSTANCE.XAssignment,
			EdeltaValidator.EPACKAGE_CYCLE,
			"Cycle in superpackage/subpackage: aPackage.subpackage.aPackage"
		)
		assertThat(resource.validate).hasSize(1)
		// check that the listener broke the cycle in the model
		assertThat(subpackage.ESubpackages).isEmpty
		assertThat(ePackage.ESubpackages).containsOnly(subpackage)
	}

	@Test
	def void testEClassCycleWhenAddingSuperType() {
		val currentExpression = XbaseFactory.eINSTANCE.createXAssignment
		resource.contents += currentExpression
		diagnosticHelper.setCurrentExpression(currentExpression)
		val c1 = ecoreFactory.createEClass => [
			name = "c1"
			ePackage.EClassifiers += it
		]
		val c2 = ecoreFactory.createEClass => [
			name = "c2"
			ePackage.EClassifiers += it
		]
		val c3 = ecoreFactory.createEClass => [
			name = "c3"
			ePackage.EClassifiers += it
		]
		c3.ESuperTypes += c2
		resource.assertNoIssues
		c2.ESuperTypes += c1
		resource.assertNoIssues
		c1.ESuperTypes += c3
		resource.assertError(
			XbasePackage.eINSTANCE.XAssignment,
			EdeltaValidator.ECLASS_CYCLE,
			"Cycle in inheritance hierarchy: aPackage.c3"
		)
		assertThat(resource.validate).hasSize(1)
	}

	@Test
	def void testModifiedElementsIsUpdatedWhenNameIsChanged() {
		val currentExpression = mock(XExpression)
		listener.setCurrentExpression(currentExpression)
		val element = ePackage.EClassifiers.get(0)
		// change the name
		element.name = "Modified"
		assertThat(modifiedElements)
			.containsExactlyInAnyOrder(element, ePackage)
	}

	def createEObjectDiagnosticMock(EObject problematicObject) {
		mock(EObjectDiagnosticImpl) => [
			when(getProblematicObject).thenReturn(problematicObject)
		]
	}
}
