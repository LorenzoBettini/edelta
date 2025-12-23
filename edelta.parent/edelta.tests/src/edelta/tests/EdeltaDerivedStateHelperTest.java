package edelta.tests;

import static org.junit.Assert.*;

import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EcoreFactory;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.impl.ResourceImpl;
import org.eclipse.xtext.resource.XtextResource;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.XtextRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.inject.Inject;

import edelta.edelta.EdeltaEcoreArgument;
import edelta.edelta.EdeltaFactory;
import edelta.resource.derivedstate.EdeltaDerivedState;
import edelta.resource.derivedstate.EdeltaDerivedStateHelper;

@RunWith(XtextRunner.class)
@InjectWith(EdeltaInjectorProvider.class)
public class EdeltaDerivedStateHelperTest extends EdeltaAbstractTest {
	@Inject
	private EdeltaDerivedStateHelper edeltaDerivedStateHelper;

	private final EdeltaFactory edeltaFactory = EdeltaFactory.eINSTANCE;

	private final EcoreFactory ecoreFactory = EcoreFactory.eINSTANCE;

	@Test
	public void testGetOrInstallAdapterWithNotXtextResource() {
		assertNotNull(edeltaDerivedStateHelper.getOrInstallAdapter(new ResourceImpl()));
	}

	@Test
	public void testGetOrInstallAdapterWithXtextResourceOfADifferentLanguage() {
		final XtextResource res = new XtextResource();
		res.setLanguageName("foo");
		assertNotNull(edeltaDerivedStateHelper.getOrInstallAdapter(res));
	}

	@Test
	public void testIsAdapterFor() {
		final EdeltaDerivedState adapter = edeltaDerivedStateHelper.getOrInstallAdapter(new ResourceImpl());
		assertTrue(adapter.isAdapterForType(EdeltaDerivedState.class));
		assertFalse(adapter.isAdapterForType(String.class));
	}

	@Test
	public void testGetEdeltaEcoreReferenceState() throws Exception {
		final Resource res = parseHelper.parse("").eResource();
		final EdeltaEcoreArgument ref = edeltaFactory.createEdeltaEcoreArgument();
		res.getContents().add(ref);
		assertNull(edeltaDerivedStateHelper.getOriginalEnamedelement(ref));
		final EAttribute el = ecoreFactory.createEAttribute();
		edeltaDerivedStateHelper.getEcoreReferenceState(ref).setOriginalEnamedelement(el);
		assertSame(el, edeltaDerivedStateHelper.getOriginalEnamedelement(ref));
	}
}
