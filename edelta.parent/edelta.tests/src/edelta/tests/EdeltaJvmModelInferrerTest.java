package edelta.tests;

import org.eclipse.emf.ecore.EcoreFactory;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.XtextRunner;
import org.eclipse.xtext.xbase.jvmmodel.IJvmModelInferrer;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.inject.Inject;

/**
 * Tests corner cases of the JvmModelInferrer
 */
@RunWith(XtextRunner.class)
@InjectWith(EdeltaInjectorProvider.class)
public class EdeltaJvmModelInferrerTest extends EdeltaAbstractTest {
	@Inject
	private IJvmModelInferrer inferrer;

	@Test
	public void testWithANonXExpression() { // NOSONAR it's enough it doesn't throw
		inferrer.infer(EcoreFactory.eINSTANCE.createEClass(), null, false);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testWithNull() {
		inferrer.infer(null, null, false);
	}
}
