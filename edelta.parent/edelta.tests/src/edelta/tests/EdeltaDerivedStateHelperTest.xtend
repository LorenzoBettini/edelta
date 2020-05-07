package edelta.tests

import com.google.inject.Inject
import edelta.resource.EdeltaDerivedStateHelper
import org.eclipse.emf.ecore.resource.impl.ResourceImpl
import org.eclipse.xtext.resource.XtextResource
import org.eclipse.xtext.testing.InjectWith
import org.eclipse.xtext.testing.XtextRunner
import org.junit.Test
import org.junit.runner.RunWith

import static org.junit.Assert.*
import edelta.resource.EdeltaDerivedState

@RunWith(XtextRunner)
@InjectWith(EdeltaInjectorProvider)
class EdeltaDerivedStateHelperTest extends EdeltaAbstractTest {

	@Inject extension EdeltaDerivedStateHelper

	@Test
	def void testGetOrInstallAdapterWithNotXtextResource() {
		assertNotNull(getOrInstallAdapter(new ResourceImpl))
	}

	@Test
	def void testGetOrInstallAdapterWithXtextResourceOfADifferentLanguage() {
		val res = new XtextResource
		res.languageName = "foo"
		assertNotNull(getOrInstallAdapter(res))
	}

	@Test
	def void testIsAdapterFor() {
		val adapter = getOrInstallAdapter(new ResourceImpl)
		assertTrue(adapter.isAdapterForType(EdeltaDerivedState))
		assertFalse(adapter.isAdapterForType(String))
	}

}
