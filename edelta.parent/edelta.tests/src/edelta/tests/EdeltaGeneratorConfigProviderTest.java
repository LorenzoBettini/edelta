package edelta.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.eclipse.emf.ecore.EcoreFactory;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.extensions.InjectionExtension;
import org.eclipse.xtext.util.JavaVersion;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.google.inject.Inject;

import edelta.compiler.EdeltaGeneratorConfigProvider;
import edelta.tests.injectors.EdeltaInjectorProviderCustom;

@ExtendWith(InjectionExtension.class)
@InjectWith(EdeltaInjectorProviderCustom.class)
class EdeltaGeneratorConfigProviderTest {

	@Inject
	private EdeltaGeneratorConfigProvider generatorConfigProvider;

	@Test
	void testGetReturnsJava21() {
		final var context = EcoreFactory.eINSTANCE.createEClass();
		final var config = generatorConfigProvider.get(context);
		assertEquals(JavaVersion.JAVA21, config.getJavaSourceVersion());
	}
}
