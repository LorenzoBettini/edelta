package edelta.lib;

import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.function.Consumer;

import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.util.EContentAdapter;

/**
 * Watches the resources of one migration source stage and retains the first write.
 * EMF notifications follow a mutation, so this guard rejects completion rather
 * than attempting to undo the changed in-memory object.
 *
 * @author Lorenzo Bettini
 */
public final class EdeltaSourceModelGuard implements AutoCloseable {
	private final Set<Resource> resources = java.util.Collections.newSetFromMap(new IdentityHashMap<>());
	private Consumer<IllegalStateException> onViolation;
	private IllegalStateException violation;
	private final EContentAdapter adapter = new EContentAdapter() {
		@Override
		protected boolean resolve() {
			return false;
		}

		@Override
		public void notifyChanged(Notification notification) {
			// Maintain adapters on newly contained objects before throwing. A caller
			// may catch the exception, but the latched failure still prevents success.
			super.notifyChanged(notification);
			if (violation == null && isWrite(notification)) {
				violation = new IllegalStateException(message(notification));
				onViolation.accept(violation);
				throw violation;
			}
		}
	};

	/** @param onViolation receives the first violation at notification time */
	public EdeltaSourceModelGuard(Consumer<IllegalStateException> onViolation) {
		this.onViolation = onViolation;
	}

	/** Replaces the guarded source resources after a complete backup copy. */
	public void watch(Collection<Resource> sources) {
		if (onViolation == null)
			throw new IllegalStateException("Source model guard is closed");
		check();
		detach();
		try {
			for (var resource : sources) {
				if (!resources.contains(resource)) {
					var adapters = resource.eAdapters();
					resources.add(resource);
					adapters.add(adapter);
				}
			}
		} catch (RuntimeException | Error exception) {
			detach();
			throw exception;
		}
	}

	/** Throws the first violation, including one caught by user migration code. */
	public void check() {
		if (violation != null)
			throw violation;
	}

	IllegalStateException violation() {
		return violation;
	}

	/** Removes the adapter from every guarded resource and contained object. */
	@Override
	public void close() {
		detach();
		// A saved evolving manager may retain this guard's latch. Release the
		// callback so that it cannot retain the migrator and old source stages.
		onViolation = null;
	}

	private void detach() {
		for (var resource : resources)
			resource.eAdapters().remove(adapter);
		resources.clear();
	}

	private boolean isWrite(Notification notification) {
		if (notification.isTouch() || eventName(notification.getEventType()) == null)
			return false;
		if (notification.getNotifier() instanceof Resource)
			return notification.getFeatureID(Resource.class) == Resource.RESOURCE__CONTENTS;
		return notification.getNotifier() instanceof EObject &&
			notification.getFeature() instanceof EStructuralFeature;
	}

	private String message(Notification notification) {
		var notifier = notification.getNotifier();
		Resource resource;
		String owner;
		String feature;
		if (notifier instanceof Resource source) {
			resource = source;
			owner = "<roots>";
			feature = "contents";
		} else {
			var object = (EObject) notifier;
			resource = object.eResource();
			owner = object.eClass().getName() + "#" + resource.getURIFragment(object);
			feature = ((EStructuralFeature) notification.getFeature()).getName();
		}
		return "Migration source model is read-only: " +
			resource.getURI() + " :: " +
			owner + "." + feature + " (" + eventName(notification.getEventType()) + ")";
	}

	private String eventName(int type) {
		return switch (type) {
		case Notification.SET -> "SET";
		case Notification.UNSET -> "UNSET";
		case Notification.ADD -> "ADD";
		case Notification.ADD_MANY -> "ADD_MANY";
		case Notification.REMOVE -> "REMOVE";
		case Notification.REMOVE_MANY -> "REMOVE_MANY";
		case Notification.MOVE -> "MOVE";
		default -> null;
		};
	}
}
