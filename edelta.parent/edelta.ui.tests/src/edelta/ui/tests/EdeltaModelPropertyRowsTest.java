package edelta.ui.tests;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EcoreFactory;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.InternalEObject;
import org.eclipse.emf.ecore.impl.DynamicEObjectImpl;
import org.eclipse.emf.ecore.resource.impl.ResourceImpl;
import org.junit.Test;

import edelta.ui.outline.EdeltaModelPropertyRows;

/** Tests finite model value labels without proxy resolution. @author Lorenzo Bettini */
public class EdeltaModelPropertyRowsTest {
	private final EdeltaModelPropertyRows rows = new EdeltaModelPropertyRows();

	@Test
	public void displaysUnsetEmptyAndLongValues() {
		var type = EcoreFactory.eINSTANCE.createEClass();
		EcoreFactory.eINSTANCE.createEPackage().getEClassifiers().add(type);
		var name = EcoreFactory.eINSTANCE.createEAttribute();
		name.setName("name");
		name.setEType(EcorePackage.Literals.ESTRING);
		var tags = EcoreFactory.eINSTANCE.createEAttribute();
		tags.setName("tags");
		tags.setEType(EcorePackage.Literals.ESTRING);
		tags.setUpperBound(-1);
		type.getEStructuralFeatures().addAll(List.of(name, tags));
		var object = new DynamicEObjectImpl(type);
		assertThat(labels(object)).containsExactly("name = <unset>", "tags = []");
		object.eSet(name, "");
		assertThat(labels(object)).contains("name = \"\"");
		object.eSet(name, "x".repeat(150));
		assertThat(labels(object).getFirst()).hasSizeLessThan(140).endsWith("...");
		object.eSet(tags, List.of("a", "b", "c", "d", "e", "f", "g"));
		assertThat(labels(object)).contains("tags = [\"a\", \"b\", \"c\", \"d\", \"e\", ... (+2)]");
		var plainList = new DynamicEObjectImpl(type) {
			@Override
			public Object eGet(EStructuralFeature feature, boolean resolve) {
				return feature == tags ? List.of("plain") : super.eGet(feature, resolve);
			}
		};
		assertThat(labels(plainList)).contains("tags = [\"plain\"]");
	}

	@Test
	public void unsettableAndContainerFeaturesHaveDistinctRows() {
		var type = EcoreFactory.eINSTANCE.createEClass();
		EcoreFactory.eINSTANCE.createEPackage().getEClassifiers().add(type);
		var flag = EcoreFactory.eINSTANCE.createEAttribute();
		flag.setName("flag");
		flag.setEType(EcorePackage.Literals.EBOOLEAN);
		flag.setUnsettable(true);
		var parent = EcoreFactory.eINSTANCE.createEReference();
		parent.setName("parent");
		parent.setEType(type);
		var children = EcoreFactory.eINSTANCE.createEReference();
		children.setName("children");
		children.setEType(type);
		children.setContainment(true);
		children.setUpperBound(-1);
		parent.setEOpposite(children);
		children.setEOpposite(parent);
		type.getEStructuralFeatures().addAll(List.of(flag, parent, children));
		var object = new DynamicEObjectImpl(type);
		assertThat(labels(object)).containsExactly("flag = <unset>");
		object.eSet(flag, false);
		assertThat(labels(object)).containsExactly("flag = false");
	}

	@Test
	public void displaysReferenceTargetsAndKeepsProxiesUnresolved() {
		var type = EcoreFactory.eINSTANCE.createEClass();
		EcoreFactory.eINSTANCE.createEPackage().getEClassifiers().add(type);
		type.setName("Node");
		var name = EcoreFactory.eINSTANCE.createEAttribute();
		name.setName("name");
		name.setEType(EcorePackage.Literals.ESTRING);
		var link = EcoreFactory.eINSTANCE.createEReference();
		link.setName("link");
		link.setEType(type);
		link.setUpperBound(-1);
		var children = EcoreFactory.eINSTANCE.createEReference();
		children.setName("children");
		children.setEType(type);
		children.setContainment(true);
		children.setUpperBound(-1);
		type.getEStructuralFeatures().addAll(List.of(name, link, children));
		var owner = new DynamicEObjectImpl(type);
		var target = new DynamicEObjectImpl(type);
		target.eSet(name, "other");
		var resource = new ResourceImpl(URI.createURI("memory:/one.xmi"));
		resource.getContents().add(owner);
		var other = new ResourceImpl(URI.createURI("memory:/two.xmi"));
		other.getContents().add(target);
		var proxy = new DynamicEObjectImpl(type);
		((InternalEObject) proxy).eSetProxyURI(URI.createURI("memory:/missing.xmi#//@children.0"));
		owner.eSet(link, List.of(owner, target, proxy));
		assertThat(labels(owner)).containsExactly("name = <unset>",
			"link [0..*] = [Node (#/), Node other (two.xmi#/), " +
				"Node (missing.xmi#//@children.0)]");
		assertThat(proxy.eIsProxy()).isTrue();
		assertThat(rows.hasRows(owner)).isTrue();
		var unnamed = new DynamicEObjectImpl(type);
		owner.eSet(link, List.of(unnamed));
		assertThat(labels(owner).getLast()).contains("Node (");
	}

	@Test
	public void targetIdAndEmptyNameAreDistinguished() {
		var type = EcoreFactory.eINSTANCE.createEClass();
		type.setName("Item");
		EcoreFactory.eINSTANCE.createEPackage().getEClassifiers().add(type);
		var id = EcoreFactory.eINSTANCE.createEAttribute();
		id.setName("key");
		id.setID(true);
		id.setEType(EcorePackage.Literals.ESTRING);
		var name = EcoreFactory.eINSTANCE.createEAttribute();
		name.setName("name");
		name.setEType(EcorePackage.Literals.ESTRING);
		var next = EcoreFactory.eINSTANCE.createEReference();
		next.setName("next");
		next.setEType(type);
		type.getEStructuralFeatures().addAll(List.of(id, name, next));
		var owner = new DynamicEObjectImpl(type);
		var target = new DynamicEObjectImpl(type);
		target.eSet(id, "t1");
		target.eSet(name, "");
		owner.eSet(next, target);
		assertThat(labels(owner).getLast()).contains("Item #t1");
		target.eSet(id, "");
		assertThat(labels(owner).getLast()).contains("Item (");
		var manyNameType = EcoreFactory.eINSTANCE.createEClass();
		manyNameType.setName("ManyName");
		type.getEPackage().getEClassifiers().add(manyNameType);
		var manyName = EcoreFactory.eINSTANCE.createEAttribute();
		manyName.setName("name");
		manyName.setEType(EcorePackage.Literals.ESTRING);
		manyName.setUpperBound(-1);
		manyNameType.getEStructuralFeatures().add(manyName);
		next.setEType(manyNameType);
		var manyTarget = new DynamicEObjectImpl(manyNameType);
		owner.eSet(next, manyTarget);
		assertThat(labels(owner).getLast()).contains("ManyName (");
	}

	private List<String> labels(org.eclipse.emf.ecore.EObject object) {
		return rows.get(object).stream().map(EdeltaModelPropertyRows.Row::label).toList();
	}
}
