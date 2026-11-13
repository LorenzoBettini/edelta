package edelta.ui.tests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EcoreFactory;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.XtextRunner;
import org.eclipse.xtext.testing.util.ParseHelper;
import org.eclipse.xtext.ui.editor.outline.IOutlineNode;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.inject.Inject;

import edelta.edelta.EdeltaProgram;
import edelta.ui.outline.EdeltaEcoreReferenceTargets;
import edelta.ui.outline.EdeltaOutlineTreeProvider;

@RunWith(XtextRunner.class)
@InjectWith(EdeltaUiInjectorProvider.class)
public class EdeltaEcoreReferenceTargetsTest {

	private final EdeltaEcoreReferenceTargets targets = new EdeltaEcoreReferenceTargets();
	private final EcoreFactory factory = EcoreFactory.eINSTANCE;

	@Inject
	private EdeltaOutlineTreeProvider treeProvider;

	@Inject
	private ParseHelper<EdeltaProgram> parseHelper;

	@Test
	public void emptyAndContainmentReferencesAreNotIncluded() {
		assertThat(targets.get(factory.createEDataType())).isEmpty();
		var ePackage = factory.createEPackage();
		var eClass = factory.createEClass();
		ePackage.getEClassifiers().add(eClass);

		assertThat(targets.get(ePackage)).containsExactly(ePackage.getEFactoryInstance());
		assertThat(targets.get(eClass)).containsExactly(ePackage);
	}

	@Test
	public void singleAndManyReferencesPreserveTheirTargets() {
		var eClass = factory.createEClass();
		var first = factory.createEClass();
		var second = factory.createEClass();
		var attribute = factory.createEAttribute();
		attribute.setEType(EcorePackage.Literals.ESTRING);
		eClass.getEStructuralFeatures().add(attribute);
		eClass.getESuperTypes().add(first);
		eClass.getESuperTypes().add(second);

		assertThat(targets.get(eClass)).containsExactly(first, second);
		assertThat(targets.get(attribute))
			.containsExactly(EcorePackage.Literals.ESTRING, eClass);
	}

	@Test
	public void computedReferencesAndCyclesDoNotExpandTheResult() {
		var first = factory.createEClass();
		var second = factory.createEClass();
		var attribute = factory.createEAttribute();
		first.getEStructuralFeatures().add(attribute);
		first.getESuperTypes().add(second);
		second.getESuperTypes().add(first);

		assertThat(targets.get(first)).containsExactly(second);
		assertThat(targets.get(second)).containsExactly(first);
		assertThat(targets.get(first)).doesNotContain(attribute);
	}

	@Test
	public void ecoreObjectWithoutChildrenIsALeaf() {
		assertThat(treeProvider._isLeaf(factory.createEDataType())).isTrue();
	}

	@Test
	public void nonEcoreReferencesDoNotBecomeOutlineChildren() throws Exception {
		var program = parseHelper.parse("");
		program.getEPackages().add(EcorePackage.eINSTANCE);
		assertThatCode(() -> new ExposedTreeProvider().createNonEcoreChildren(program))
			.doesNotThrowAnyException();
	}

	private static class ExposedTreeProvider extends EdeltaOutlineTreeProvider {
		void createNonEcoreChildren(EdeltaProgram program) {
			super._createChildren((IOutlineNode) null, (EObject) program);
		}
	}
}
