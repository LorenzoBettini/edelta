package edelta.ui.tests;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.eclipse.emf.ecore.ENamedElement;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EcoreFactory;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.impl.DynamicEObjectImpl;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.XtextRunner;
import org.eclipse.xtext.testing.util.ParseHelper;
import org.eclipse.xtext.ui.editor.outline.IOutlineNode;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.inject.Inject;

import edelta.edelta.EdeltaProgram;
import edelta.ui.outline.EdeltaEcorePropertyRows;
import edelta.ui.outline.EdeltaOutlineTreeProvider;

@RunWith(XtextRunner.class)
@InjectWith(EdeltaUiInjectorProvider.class)
public class EdeltaEcorePropertyRowsTest {

	private final EdeltaEcorePropertyRows rows = new EdeltaEcorePropertyRows();
	private final EcoreFactory factory = EcoreFactory.eINSTANCE;

	@Inject
	private EdeltaOutlineTreeProvider treeProvider;

	@Inject
	private ParseHelper<EdeltaProgram> parseHelper;

	@Test
	public void editableDefaultsAndUnsetValuesAreShown() {
		var eClass = factory.createEClass();
		eClass.setName("A");
		assertThat(labels(eClass)).contains("name = A", "abstract = false", "interface = false")
			.doesNotContain("ePackage = <unset>");

		var attribute = factory.createEAttribute();
		assertThat(labels(attribute)).contains("name = <unset>", "lowerBound = 0", "upperBound = 1")
			.noneMatch(label -> label.startsWith("eContainingClass = "));
	}

	@Test
	public void referenceValuesAreLabelsAndNeverTraversed() {
		var first = factory.createEClass();
		first.setName("First");
		var second = factory.createEClass();
		second.setName("Second");
		first.getESuperTypes().add(second);
		second.getESuperTypes().add(first);
		var reference = factory.createEReference();
		reference.setName("forward");
		var opposite = factory.createEReference();
		opposite.setName("back");
		reference.setEOpposite(opposite);

		assertThat(labels(reference)).contains("eOpposite = back");
		assertThat(labels(reference)).noneMatch(label -> label.startsWith("eType = "));
		assertThat(labels(first)).contains("eSuperTypes = [Second]");
		assertThat(labels(second)).contains("eSuperTypes = [First]");
	}

	@Test
	public void multipleReferenceValuesAreBounded() {
		var reference = factory.createEReference();
		for (int i = 0; i < 7; i++) {
			var key = factory.createEAttribute();
			key.setName("key" + i);
			reference.getEKeys().add(key);
		}
		assertThat(labels(reference)).contains(
			"eKeys = [key0, key1, key2, key3, key4, ... (+2)]");
	}

	@Test
	public void readOnlyComputedAndContainmentFeaturesAreExcluded() {
		var dynamicPackage = factory.createEPackage();
		var type = factory.createEClass();
		dynamicPackage.getEClassifiers().add(type);
		var readOnly = factory.createEAttribute();
		readOnly.setChangeable(false);
		var derived = factory.createEAttribute();
		derived.setDerived(true);
		var transientFeature = factory.createEAttribute();
		transientFeature.setTransient(true);
		var volatileFeature = factory.createEAttribute();
		volatileFeature.setVolatile(true);
		var visible = factory.createEAttribute();
		visible.setName("visible");
		visible.setEType(EcorePackage.Literals.ESTRING);
		type.getEStructuralFeatures().addAll(
			List.of(readOnly, derived, transientFeature, volatileFeature, visible));
		assertThat(labels(new DynamicEObjectImpl(type))).containsExactly("visible = <unset>");

		var parent = factory.createEClass();
		var child = factory.createEClass();
		dynamicPackage.getEClassifiers().add(parent);
		dynamicPackage.getEClassifiers().add(child);
		var contains = factory.createEReference();
		contains.setContainment(true);
		contains.setEType(child);
		var container = factory.createEReference();
		container.setEType(parent);
		contains.setEOpposite(container);
		container.setEOpposite(contains);
		parent.getEStructuralFeatures().add(contains);
		child.getEStructuralFeatures().add(container);
		assertThat(labels(new DynamicEObjectImpl(parent))).isEmpty();
		assertThat(labels(new DynamicEObjectImpl(child))).isEmpty();
	}

	@Test
	public void emptyEcoreObjectIsALeafAndOtherModelTypesAreIgnored() throws Exception {
		var eFactory = factory.createEFactory();
		assertThat(labels(eFactory)).isEmpty();
		assertThat(treeProvider._isLeaf(eFactory)).isTrue();
		var program = parseHelper.parse("");
		new ExposedTreeProvider().createNonEcoreChildren(program);
	}

	private static class ExposedTreeProvider extends EdeltaOutlineTreeProvider {
		void createNonEcoreChildren(EdeltaProgram program) {
			super._createChildren((IOutlineNode) null, (EObject) program);
		}
	}

	private List<String> labels(EObject object) {
		return rows.get(object, value -> value instanceof ENamedElement named
			? named.getName() : value.eClass().getName()).stream()
			.map(EdeltaEcorePropertyRows.Row::label)
			.toList();
	}
}
