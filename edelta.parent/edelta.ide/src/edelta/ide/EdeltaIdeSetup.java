/*
 * generated by Xtext 2.24.0
 */
package edelta.ide;

import com.google.inject.Guice;
import com.google.inject.Injector;
import edelta.EdeltaRuntimeModule;
import edelta.EdeltaStandaloneSetup;
import org.eclipse.xtext.util.Modules2;

/**
 * Initialization support for running Xtext languages as language servers.
 */
public class EdeltaIdeSetup extends EdeltaStandaloneSetup {

	@Override
	public Injector createInjector() {
		return Guice.createInjector(Modules2.mixin(new EdeltaRuntimeModule(), new EdeltaIdeModule()));
	}

}
