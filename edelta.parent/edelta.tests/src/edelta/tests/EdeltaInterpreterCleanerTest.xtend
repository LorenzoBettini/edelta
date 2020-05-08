package edelta.tests

import com.google.inject.Inject
import com.google.inject.Provider
import edelta.edelta.EdeltaFactory
import edelta.interpreter.EdeltaInterpreterCleaner
import org.eclipse.emf.ecore.EObject
import org.eclipse.emf.ecore.EPackage
import org.eclipse.emf.ecore.EcoreFactory
import org.eclipse.emf.ecore.resource.Resource
import org.eclipse.emf.ecore.resource.Resource.Diagnostic
import org.eclipse.emf.ecore.resource.impl.ResourceImpl
import org.eclipse.xtext.linking.impl.XtextLinkingDiagnostic
import org.eclipse.xtext.testing.InjectWith
import org.eclipse.xtext.testing.XtextRunner
import org.eclipse.xtext.util.IResourceScopeCache
import org.eclipse.xtext.validation.EObjectDiagnosticImpl
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

import static org.assertj.core.api.Assertions.assertThat
import static org.mockito.Mockito.*

@RunWith(XtextRunner)
@InjectWith(EdeltaInjectorProvider)
class EdeltaInterpreterCleanerTest {

	static class SpiedProvider implements Provider<String> {
		override get() {
			"a string"
		}
	}

	static val ecoreFactory = EcoreFactory.eINSTANCE
	static val edeltaFactory = EdeltaFactory.eINSTANCE

	@Inject IResourceScopeCache cache
	var EdeltaInterpreterCleaner cleaner
	var EPackage ePackage
	var Resource resource
	var Provider<String> stringProvider

	@Before
	def void setup() {
		ePackage = ecoreFactory.createEPackage => [
			EClassifiers += ecoreFactory.createEClass => [
				name = "AClass"
			]
		]
		resource = new ResourceImpl
		cleaner = new EdeltaInterpreterCleaner(cache, resource)
		stringProvider = spy(new SpiedProvider)
		ePackage.eAdapters += cleaner
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

	def createEObjectDiagnosticMock(EObject problematicObject) {
		mock(EObjectDiagnosticImpl) => [
			when(getProblematicObject).thenReturn(problematicObject)
		]
	}
}
