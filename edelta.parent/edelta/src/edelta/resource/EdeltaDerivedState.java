package edelta.resource;

import org.eclipse.emf.common.notify.impl.AdapterImpl;
import org.eclipse.xtext.resource.XtextResource;

import edelta.util.EdeltaCopiedEPackagesMap;

/**
 * Additional derived state installable in an {@link XtextResource}.
 * 
 * @author Lorenzo Bettini
 *
 */
public class EdeltaDerivedState extends AdapterImpl {
	private EdeltaCopiedEPackagesMap copiedEPackagesMap = new EdeltaCopiedEPackagesMap();

	@Override
	public boolean isAdapterForType(final Object type) {
		return EdeltaDerivedState.class == type;
	}

	public EdeltaCopiedEPackagesMap getCopiedEPackagesMap() {
		return copiedEPackagesMap;
	}
}