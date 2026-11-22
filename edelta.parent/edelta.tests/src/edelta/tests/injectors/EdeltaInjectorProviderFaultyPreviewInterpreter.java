package edelta.tests.injectors;

import edelta.EdeltaRuntimeModule;
import edelta.interpreter.EdeltaInterpreter;
import edelta.tests.EdeltaInjectorProvider;
import edelta.tests.additional.EdeltaFaultInjectingPreviewInterpreter;

/**
 * Runs derived state with controlled preview failures.
 *
 * @author Lorenzo Bettini
 */
public class EdeltaInjectorProviderFaultyPreviewInterpreter extends EdeltaInjectorProvider {
	@Override
	protected EdeltaRuntimeModule createRuntimeModule() {
		return new EdeltaRuntimeModule() {
			@Override
			public Class<? extends EdeltaInterpreter> bindEdeltaInterpreter() {
				return EdeltaFaultInjectingPreviewInterpreter.class;
			}
		};
	}
}
