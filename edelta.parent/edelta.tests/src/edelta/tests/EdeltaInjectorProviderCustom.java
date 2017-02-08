package edelta.tests;

import org.eclipse.xtext.ecore.EcoreSupportStandaloneSetup;
import org.eclipse.xtext.util.JavaVersion;
import org.eclipse.xtext.xbase.testing.OnTheFlyJavaCompiler2;

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import edelta.EdeltaRuntimeModule;
import edelta.EdeltaStandaloneSetup;

public class EdeltaInjectorProviderCustom extends EdeltaInjectorProvider {

	@Singleton
	private static class Java8OnTheFlyJavaCompiler2 extends OnTheFlyJavaCompiler2 {
		@Inject
		public Java8OnTheFlyJavaCompiler2(ClassLoader scope) {
			super(scope, JavaVersion.JAVA8);
		}
	}

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

	@Override
	protected EdeltaRuntimeModule createRuntimeModule() {
		return new EdeltaRuntimeModule() {
			@Override
			public ClassLoader bindClassLoaderToInstance() {
				return EdeltaInjectorProvider.class.getClassLoader();
			}

			@SuppressWarnings("unused")
			public Class<? extends OnTheFlyJavaCompiler2> bindOnTheFlyJavaCompiler2() {
				return Java8OnTheFlyJavaCompiler2.class;
			}
		};
	}
}
