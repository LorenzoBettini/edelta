package edelta.ui.launching;

import org.eclipse.core.resources.IResource;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.xtext.xbase.ui.launching.JavaElementDelegate;
import org.eclipse.xtext.xbase.ui.launching.JavaElementDelegateAdapterFactory;
import com.google.inject.Inject;
import com.google.inject.Provider;

public class EdeltaJavaElementDelegateAdapterFactory extends JavaElementDelegateAdapterFactory {
	@Inject
	private Provider<EdeltaJavaElementDelegateMainLaunch> mainDelegateProvider;
	@Override
	public <T> T getAdapter(Object adaptableObject, Class<T> adapterType) {
		JavaElementDelegate result = null;
		if (EdeltaJavaElementDelegateMainLaunch.class.equals(adapterType)) {
			result = this.mainDelegateProvider.get();
		}
		if (result != null) {
			if (adaptableObject instanceof IFileEditorInput fileEditorInput) {
				result.initializeWith(fileEditorInput);
				return adapterType.cast(result);
			}
			if (adaptableObject instanceof IResource resource) {
				result.initializeWith(resource);
				return adapterType.cast(result);
			}
			if (adaptableObject instanceof IEditorPart editorPart) {
				result.initializeWith(editorPart);
				return adapterType.cast(result);
			}
		}
		return super.getAdapter(adaptableObject, adapterType);
	}
}