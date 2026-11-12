package edelta.tests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.*;

import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EcoreFactory;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.impl.ResourceImpl;
import org.eclipse.xtext.naming.QualifiedName;
import org.eclipse.xtext.resource.XtextResource;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.XtextRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.inject.Inject;

import edelta.edelta.EdeltaEcoreArgument;
import edelta.edelta.EdeltaFactory;
import edelta.resource.derivedstate.EdeltaAccessibleElement;
import edelta.resource.derivedstate.EdeltaAccessibleElements;
import edelta.resource.derivedstate.EdeltaDerivedState;
import edelta.resource.derivedstate.EdeltaDerivedStateHelper;
import edelta.resource.derivedstate.EdeltaEcoreReferenceExpressionState;

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

	@Test
	public void testAccessibleElementsInEveryEvaluation() {
		var state = new EdeltaEcoreReferenceExpressionState();
		assertThat(state.getAccessibleElementsInEveryEvaluation()).isEmpty();

		var first = new EdeltaAccessibleElements();
		var firstClass = ecoreFactory.createEClass();
		firstClass.setName("Stable");
		first.add(new EdeltaAccessibleElement(firstClass, QualifiedName.create("p", "Stable")));
		state.setAccessibleElements(first);
		assertThat(state.getAccessibleElements()).isSameAs(first);
		assertThat(state.getAccessibleElementsInEveryEvaluation()).hasSize(1);

		var second = new EdeltaAccessibleElements();
		var secondClass = ecoreFactory.createEClass();
		secondClass.setName("Stable");
		second.add(new EdeltaAccessibleElement(secondClass, QualifiedName.create("p", "Stable")));
		var newClass = ecoreFactory.createEClass();
		newClass.setName("New");
		second.add(new EdeltaAccessibleElement(newClass, QualifiedName.create("p", "New")));
		state.setAccessibleElements(second);
		assertThat(state.getAccessibleElements()).isSameAs(second);
		var common = state.getAccessibleElementsInEveryEvaluation();
		assertThat(common).hasSize(1);
		assertThat(common.get(0).getElement()).isSameAs(secondClass);
	}
}
