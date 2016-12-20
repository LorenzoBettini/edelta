package edelta.tests;

import org.eclipse.xtext.ecore.EcoreSupportStandaloneSetup;

import com.google.inject.Guice;
import com.google.inject.Injector;

import edelta.EdeltaStandaloneSetup;

public class EdeltaInjectorProviderCustom extends EdeltaInjectorProvider {

	@Override
	protected Injector internalCreateInjector() {
		return new EdeltaStandaloneSetup() {
			public void register(Injector injector) {
				EcoreSupportStandaloneSetup.setup();
				super.register(injector);
			};
			@Override
			public Injector createInjector() {
				return Guice.createInjector(createRuntimeModule());
			}
		}.createInjectorAndDoEMFRegistration();
	}
}
