package edelta.lib.tests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.emf.common.notify.Adapter;
import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.common.notify.impl.NotificationImpl;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EcoreFactory;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.InternalEObject;
import org.eclipse.emf.ecore.impl.ENotificationImpl;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceImpl;
import org.junit.jupiter.api.Test;

import edelta.lib.EdeltaSourceModelGuard;

/** Direct checks of source notification classification and adapter ownership.
 * @author Lorenzo Bettini
 */
class EdeltaSourceModelGuardTest {
	private final EClass node = EcoreFactory.eINSTANCE.createEClass();
	private final org.eclipse.emf.ecore.EPackage epackage =
		EcoreFactory.eINSTANCE.createEPackage();
	private final EAttribute name = EcoreFactory.eINSTANCE.createEAttribute();
	private final EAttribute tags = EcoreFactory.eINSTANCE.createEAttribute();
	private final EReference children = EcoreFactory.eINSTANCE.createEReference();
	private final EReference link = EcoreFactory.eINSTANCE.createEReference();
	private final Resource source = new XMIResourceImpl(URI.createURI("memory:/source.xmi"));
	private final EObject root;
	private final List<IllegalStateException> detected = new ArrayList<>();
	private final EdeltaSourceModelGuard guard = new EdeltaSourceModelGuard(detected::add);

	EdeltaSourceModelGuardTest() {
		epackage.setName("guard");
		epackage.setNsPrefix("guard");
		epackage.setNsURI("urn:edelta:guard");
		node.setName("Node");
		epackage.getEClassifiers().add(node);
		name.setName("name");
		name.setEType(EcorePackage.Literals.ESTRING);
		tags.setName("tags");
		tags.setEType(EcorePackage.Literals.ESTRING);
		tags.setUpperBound(-1);
		children.setName("children");
		children.setContainment(true);
		children.setUpperBound(-1);
		children.setEType(node);
		link.setName("link");
		link.setEType(node);
		node.getEStructuralFeatures().addAll(List.of(name, tags, children, link));
		root = EcoreUtil.create(node);
		root.eSet(name, "before");
		source.getContents().add(root);
	}

	private void assertViolation(Runnable write, String suffix) {
		guard.watch(List.of(source));
		assertThatThrownBy(write::run).isInstanceOf(IllegalStateException.class)
			.hasMessageMatching("Migration source model is read-only: memory:/source.xmi :: " + suffix);
		assertThat(detected).hasSize(1);
		assertThatThrownBy(guard::check).isSameAs(detected.getFirst());
		guard.close();
		assertThat(source.eAdapters()).isEmpty();
		assertThat(root.eAdapters()).isEmpty();
	}

	@Test
	void detectsAttributeSetAndUnset() {
		assertViolation(() -> root.eSet(name, "after"), "Node#.*\\.name \\(SET\\)");
		var another = new EdeltaSourceModelGuard(detected::add);
		another.watch(List.of(source));
		// Dynamic EMF reports this eUnset as SET; it is still a forbidden write.
		assertThatThrownBy(() -> root.eUnset(name)).hasMessageMatching(
			"Migration source model is read-only: memory:/source.xmi :: Node#.*\\.name \\(SET\\)");
		another.close();
	}

	@Test
	void detectsContainedDescendantAndReferenceWrites() {
		var child = EcoreUtil.create(node);
		list(root, children).add(child);
		guard.watch(List.of(source));
		assertThatThrownBy(() -> child.eSet(name, "changed"))
			.hasMessageMatching("Migration source model is read-only: memory:/source.xmi :: Node#.*\\.name \\(SET\\)");
		guard.close();
		var next = new EdeltaSourceModelGuard(detected::add);
		next.watch(List.of(source));
		assertThatThrownBy(() -> root.eSet(link, child))
			.hasMessageMatching("Migration source model is read-only: memory:/source.xmi :: Node#.*\\.link \\(SET\\)");
		next.close();
	}

	@Test
	void detectsMutableListAndRootEvents() {
		assertViolation(() -> list(root, tags).add("one"), "Node#.*\\.tags \\(ADD\\)");
		var next = new EdeltaSourceModelGuard(detected::add);
		next.watch(List.of(source));
		assertThatThrownBy(() -> source.getContents().remove(root))
			.hasMessageMatching("Migration source model is read-only: memory:/source.xmi :: <roots>\\.contents \\(REMOVE\\)");
		next.close();
	}

	@Test
	void detectsListRemovalMoveAndBatchChanges() {
		EList<String> values = list(root, tags);
		values.addAll(List.of("one", "two", "three"));
		assertViolation(() -> values.remove(0), "Node#.*\\.tags \\(REMOVE\\)");
		var batch = new EdeltaSourceModelGuard(detected::add);
		batch.watch(List.of(source));
		assertThatThrownBy(() -> values.addAll(List.of("four", "five")))
			.hasMessageMatching("Migration source model is read-only: memory:/source.xmi :: Node#.*\\.tags \\(ADD_MANY\\)");
		batch.close();
		var moving = new EdeltaSourceModelGuard(detected::add);
		moving.watch(List.of(source));
		assertThatThrownBy(() -> values.move(0, 1))
			.hasMessageMatching("Migration source model is read-only: memory:/source.xmi :: Node#.*\\.tags \\(MOVE\\)");
		moving.close();
		var clearing = new EdeltaSourceModelGuard(detected::add);
		clearing.watch(List.of(source));
		assertThatThrownBy(values::clear)
			.hasMessageMatching("Migration source model is read-only: memory:/source.xmi :: Node#.*\\.tags \\(REMOVE_MANY\\)");
		clearing.close();
	}

	@Test
	void protectsEverySelectedResourceWithoutFollowingExternalLinks() {
		var selected = new XMIResourceImpl(URI.createURI("memory:/selected.xmi"));
		var second = EcoreUtil.create(node);
		selected.getContents().add(second);
		var external = EcoreUtil.create(node);
		root.eSet(link, external);
		guard.watch(List.of(source, selected));
		external.eSet(name, "legal");
		assertThatThrownBy(() -> second.eSet(name, "illegal"))
			.hasMessageMatching("Migration source model is read-only: memory:/selected.xmi :: Node#.*\\.name \\(SET\\)");
		guard.close();
		assertThat(source.eAdapters()).isEmpty();
		assertThat(selected.eAdapters()).isEmpty();
	}

	@Test
	void watchesOnlySelectedContainmentAndCanRetarget() {
		var other = new XMIResourceImpl(URI.createURI("memory:/other.xmi"));
		var external = EcoreUtil.create(node);
		other.getContents().add(external);
		root.eSet(link, external);
		guard.watch(List.of(source, source));
		external.eSet(name, "legal external write");
		assertThat(detected).isEmpty();
		guard.watch(List.of(other));
		root.eSet(name, "old source now legal");
		assertThatThrownBy(() -> external.eSet(name, "new source write"))
			.hasMessageMatching("Migration source model is read-only: memory:/other.xmi :: Node#.*\\.name \\(SET\\)");
		guard.close();
		assertThat(root.eAdapters()).isEmpty();
		assertThat(external.eAdapters()).isEmpty();
	}

	@Test
	void classifiesAllStructuralWriteNotifications() {
		var events = new int[] {Notification.SET, Notification.UNSET, Notification.ADD,
			Notification.ADD_MANY, Notification.REMOVE, Notification.REMOVE_MANY,
			Notification.MOVE};
		var names = new String[] {"SET", "UNSET", "ADD", "ADD_MANY", "REMOVE",
			"REMOVE_MANY", "MOVE"};
		for (var index = 0; index < events.length; index++) {
			var current = new EdeltaSourceModelGuard(detected::add);
			current.watch(List.of(source));
			var event = events[index];
			assertThatThrownBy(() -> root.eNotify(event == Notification.MOVE ?
				new ENotificationImpl((InternalEObject) root, event, tags, 0, 1) :
				new ENotificationImpl((InternalEObject) root, event, name, "before", "after")))
				.hasMessageEndingWith((event == Notification.MOVE ? ".tags (" : ".name (") +
					names[index] + ")");
			current.close();
		}
	}

	@Test
	void ignoresResolutionTouchAndResourceHousekeeping() {
		guard.watch(List.of(source));
		root.eNotify(new ENotificationImpl((InternalEObject) root,
			Notification.RESOLVE, link, root, root));
		root.eNotify(new ENotificationImpl((InternalEObject) root,
			Notification.REMOVING_ADAPTER, name, "old", "new"));
		root.eNotify(new ENotificationImpl((InternalEObject) root,
			Notification.SET, name, "before", "before"));
		source.setURI(URI.createURI("memory:/renamed.xmi"));
		Adapter adapter = source.eAdapters().getFirst();
		adapter.notifyChanged(new NotificationImpl(Notification.SET, "old", "new") {
			@Override public Object getNotifier() { return new Object(); }
		});
		root.eNotify(new ENotificationImpl((InternalEObject) root,
			Notification.SET, (org.eclipse.emf.ecore.EStructuralFeature) null,
			"old", "new"));
		guard.check();
		guard.close();
	}

	@Test
	void resolvingExternalReferenceDoesNotGuardTheExternalObject() {
		var set = new ResourceSetImpl();
		set.getResources().add(source);
		var externalResource = new XMIResourceImpl(URI.createURI("memory:/external.xmi"));
		set.getResources().add(externalResource);
		var external = EcoreUtil.create(node);
		externalResource.getContents().add(external);
		var proxy = EcoreUtil.create(node);
		((InternalEObject) proxy).eSetProxyURI(URI.createURI("memory:/external.xmi#/"));
		root.eSet(link, proxy);
		guard.watch(List.of(source));
		assertThat(root.eGet(link)).isSameAs(external);
		external.eSet(name, "outside selected source");
		guard.check();
		guard.close();
	}

	@Test
	void partialAttachmentFailureRemovesAlreadyInstalledAdapters() {
		var broken = new XMIResourceImpl(URI.createURI("memory:/broken.xmi")) {
			@Override
			public EList<Adapter> eAdapters() {
				throw new IllegalStateException("adapter installation failed");
			}
		};
		assertThatThrownBy(() -> guard.watch(List.of(source, broken)))
			.hasMessage("adapter installation failed");
		assertThat(source.eAdapters()).isEmpty();
		assertThat(root.eAdapters()).isEmpty();
	}

	@Test
	void closedGuardCannotBeReattached() {
		guard.watch(List.of(source));
		guard.close();
		assertThatThrownBy(() -> guard.watch(List.of(source)))
			.hasMessage("Source model guard is closed");
		assertThat(source.eAdapters()).isEmpty();
	}

	@SuppressWarnings("unchecked")
	private <T> EList<T> list(EObject object, org.eclipse.emf.ecore.EStructuralFeature feature) {
		return (EList<T>) object.eGet(feature);
	}
}
