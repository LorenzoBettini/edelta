package edelta.tests.injectors;

import org.eclipse.xtext.common.types.util.JavaReflectAccess;

import edelta.EdeltaRuntimeModule;
import edelta.tests.EdeltaInjectorProvider;
import edelta.tests.additional.MockJavaReflectAccess;

public class EdeltaInjectorProviderForJavaReflectAccess extends EdeltaInjectorProviderCustom {
	@Override
	protected EdeltaRuntimeModule createRuntimeModule() {
		return new EdeltaRuntimeModule() {
			@Override
			public ClassLoader bindClassLoaderToInstance() {
				return EdeltaInjectorProvider.class.getClassLoader();
			}

			@SuppressWarnings("unused")
			public Class<? extends JavaReflectAccess> bindJavaReflectAccess() {
				return MockJavaReflectAccess.class;
			}
		};
	}
}