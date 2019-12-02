package edelta.tests;

import org.eclipse.xtext.resource.IDerivedStateComputer;

import edelta.EdeltaRuntimeModule;
import edelta.tests.additional.EdeltaDerivedStateComputerWithoutInterpreter;

/**
 * Avoids the derived state computer run the interpreter since the tests in this
 * class must concern interpreter only and we don't want side effects from the
 * derived state computer running the interpreter.
 * 
 * @author Lorenzo Bettini
 *
 */
public class EdeltaInjectorProviderDerivedStateComputerWithoutInterpreter extends EdeltaInjectorProviderCustom {

	@Override
	protected EdeltaRuntimeModule createRuntimeModule() {
		return new EdeltaRuntimeModule() {
			@Override
			public ClassLoader bindClassLoaderToInstance() {
				return EdeltaInjectorProvider.class.getClassLoader();
			}

			@Override
			public Class<? extends IDerivedStateComputer> bindIDerivedStateComputer() {
				return EdeltaDerivedStateComputerWithoutInterpreter.class;
			}

		};
	}
}
