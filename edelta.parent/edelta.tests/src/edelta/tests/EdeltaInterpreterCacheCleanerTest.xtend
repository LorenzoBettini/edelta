package edelta.tests

import com.google.inject.Inject
import com.google.inject.Provider
import edelta.interpreter.EdeltaInterpreterCacheCleaner
import org.eclipse.emf.ecore.EPackage
import org.eclipse.emf.ecore.EcoreFactory
import org.eclipse.emf.ecore.resource.Resource
import org.eclipse.emf.ecore.resource.Resource.Diagnostic
import org.eclipse.emf.ecore.resource.impl.ResourceImpl
import org.eclipse.xtext.linking.impl.XtextLinkingDiagnostic
import org.eclipse.xtext.testing.InjectWith
import org.eclipse.xtext.testing.XtextRunner
import org.eclipse.xtext.util.IResourceScopeCache
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

import static org.assertj.core.api.Assertions.assertThat
import static org.mockito.Mockito.*

@RunWith(XtextRunner)
@InjectWith(EdeltaInjectorProvider)
class EdeltaInterpreterCacheCleanerTest {

	static class SpiedProvider implements Provider<String> {
		override get() {
			"a string"
		}
	}

	static val ecoreFactory = EcoreFactory.eINSTANCE

	@Inject IResourceScopeCache cache
	var EdeltaInterpreterCacheCleaner cacheCleaner
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
		cacheCleaner = new EdeltaInterpreterCacheCleaner(cache, resource)
		stringProvider = spy(new SpiedProvider)
		ePackage.eAdapters += cacheCleaner
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
		assertThat(resource.errors).hasSize(1)
		assertThat(resource.warnings).hasSize(1)
	}
}
