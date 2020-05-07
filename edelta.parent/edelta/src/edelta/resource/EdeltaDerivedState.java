package edelta.resource;

import java.util.Objects;

import org.eclipse.emf.common.notify.impl.AdapterImpl;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.xtext.Constants;
import org.eclipse.xtext.resource.XtextResource;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import edelta.util.EdeltaCopiedEPackagesMap;

/**
 * @author Lorenzo Bettini
 *
 */
@Singleton
public class EdeltaDerivedState {

	@Inject
	@Named(Constants.LANGUAGE_NAME)
	private String languageName;

	public static class EdeltaDerivedStateAdapter extends AdapterImpl {
		private EdeltaCopiedEPackagesMap copiedEPackagesMap = new EdeltaCopiedEPackagesMap();

		@Override
		public boolean isAdapterForType(final Object type) {
			return EdeltaDerivedStateAdapter.class == type;
		}

		public EdeltaCopiedEPackagesMap getCopiedEPackagesMap() {
			return copiedEPackagesMap;
		}
	}

	public EdeltaDerivedStateAdapter getOrInstallAdapter(final Resource resource) {
		if (resource instanceof XtextResource) {
			final String resourceLanguageName = ((XtextResource) resource).getLanguageName();
			if (Objects.equals(resourceLanguageName, this.languageName)) {
				EdeltaDerivedStateAdapter adapter = 
					(EdeltaDerivedStateAdapter) EcoreUtil.getAdapter
						(resource.eAdapters(), EdeltaDerivedStateAdapter.class);
				if (adapter == null) {
					adapter = new EdeltaDerivedStateAdapter();
					resource.eAdapters().add(adapter);
				}
				return adapter;
			}
		}
		return new EdeltaDerivedStateAdapter();
	}
}
