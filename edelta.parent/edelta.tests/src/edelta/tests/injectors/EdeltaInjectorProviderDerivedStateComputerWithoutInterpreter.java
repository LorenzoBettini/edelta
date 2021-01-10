package edelta.tests.injectors;

import org.eclipse.xtext.common.types.util.JavaReflectAccess;
import org.eclipse.xtext.resource.IDerivedStateComputer;

import edelta.EdeltaRuntimeModule;
import edelta.interpreter.EdeltaInterpreter;
import edelta.tests.EdeltaInjectorProvider;
import edelta.tests.additional.EdeltaDerivedStateComputerWithoutInterpreter;
import edelta.tests.additional.MockJavaReflectAccess;

/**
 * Avoids the derived state computer run the interpreter since the tests in this
 * class must concern interpreter only and we don't want side effects from the
 * derived state computer running the interpreter. The {@link EdeltaInterpreter} can
 * still be injected in the tests and it will be a {@link EdeltaInterpreter}.
 * 
 * @author Lorenzo Bettini
 *
 */
public class EdeltaInjectorProviderDerivedStateComputerWithoutInterpreter extends EdeltaInjectorProviderCustom {

	@Override
	protected EdeltaRuntimeModule createRuntimeModule() {
		return new EdeltaRuntimeModule() {
			@Override
			public Class<? extends EdeltaInterpreter> bindEdeltaInterpreter() {
				return EdeltaInterpreter.class;
			}

			@Override
			public ClassLoader bindClassLoaderToInstance() {
				return EdeltaInjectorProvider.class.getClassLoader();
			}

			@Override
			public Class<? extends IDerivedStateComputer> bindIDerivedStateComputer() {
				return EdeltaDerivedStateComputerWithoutInterpreter.class;
			}

			@SuppressWarnings("unused")
			public Class<? extends JavaReflectAccess> bindJavaReflectAccess() {
				return MockJavaReflectAccess.class;
			}
		};
	}
}
