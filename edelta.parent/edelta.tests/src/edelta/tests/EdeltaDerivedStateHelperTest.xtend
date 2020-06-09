package edelta.tests

import com.google.inject.Inject
import edelta.edelta.EdeltaFactory
import edelta.resource.derivedstate.EdeltaDerivedState
import edelta.resource.derivedstate.EdeltaDerivedStateHelper
import org.eclipse.emf.ecore.EcoreFactory
import org.eclipse.emf.ecore.resource.impl.ResourceImpl
import org.eclipse.xtext.resource.XtextResource
import org.eclipse.xtext.testing.InjectWith
import org.eclipse.xtext.testing.XtextRunner
import org.junit.Test
import org.junit.runner.RunWith

import static org.junit.Assert.*

@RunWith(XtextRunner)
@InjectWith(EdeltaInjectorProvider)
class EdeltaDerivedStateHelperTest extends EdeltaAbstractTest {

	@Inject extension EdeltaDerivedStateHelper
	val edeltaFactory = EdeltaFactory.eINSTANCE
	val ecoreFactory = EcoreFactory.eINSTANCE

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

	@Test
	def void testGetEdeltaEcoreReferenceState() {
		val res = "".parse.eResource
		val ref = edeltaFactory.createEdeltaEcoreReference
		res.contents += ref
		assertNull(ref.originalEnamedelement)
		val el = ecoreFactory.createEAttribute
		ref.ecoreReferenceState.originalEnamedelement = el
		assertSame(el, ref.originalEnamedelement)
	}
}
