package edelta.interpreter;

import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.util.EContentAdapter;
import org.eclipse.xtext.util.IResourceScopeCache;

/**
 * Listens for changes and clears the {@link IResourceScopeCache} of the
 * specified {@link Resource}.
 * 
 * @author Lorenzo Bettini
 *
 */
public class EdeltaInterpreterCacheCleaner extends EContentAdapter {

	private IResourceScopeCache cache;

	private Resource resource;

	public EdeltaInterpreterCacheCleaner(IResourceScopeCache cache, Resource resource) {
		this.cache = cache;
		this.resource = resource;
	}

	@Override
	public void notifyChanged(Notification notification) {
		super.notifyChanged(notification);
		cache.clear(resource);
	}
}
