package edelta.tests.injectors;

import org.eclipse.xtext.resource.IDerivedStateComputer;

import edelta.EdeltaRuntimeModule;
import edelta.tests.EdeltaInjectorProvider;
import edelta.tests.additional.TestableEdeltaDerivedStateComputer;

/**
 * Uses the {@link TestableEdeltaDerivedStateComputer}.
 * 
 * @author Lorenzo Bettini
 *
 */
public class EdeltaInjectorProviderTestableDerivedStateComputer extends EdeltaInjectorProviderCustom {

	@Override
	protected EdeltaRuntimeModule createRuntimeModule() {
		return new EdeltaRuntimeModule() {
			@Override
			public ClassLoader bindClassLoaderToInstance() {
				return EdeltaInjectorProvider.class.getClassLoader();
			}

			@Override
			public Class<? extends IDerivedStateComputer> bindIDerivedStateComputer() {
				return TestableEdeltaDerivedStateComputer.class;
			}

		};
	}
}
