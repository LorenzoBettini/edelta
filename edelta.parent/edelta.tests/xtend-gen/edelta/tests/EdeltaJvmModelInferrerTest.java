package edelta.tests;

import com.google.inject.Inject;
import edelta.tests.EdeltaAbstractTest;
import edelta.tests.EdeltaInjectorProvider;
import org.eclipse.emf.ecore.EcoreFactory;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.XtextRunner;
import org.eclipse.xtext.xbase.jvmmodel.IJvmModelInferrer;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests corner cases of the JvmModelInferrer
 */
@RunWith(XtextRunner.class)
@InjectWith(EdeltaInjectorProvider.class)
@SuppressWarnings("all")
public class EdeltaJvmModelInferrerTest extends EdeltaAbstractTest {
  @Inject
  private IJvmModelInferrer inferrer;
  
  @Test
  public void testWithANonXExpression() {
    this.inferrer.infer(EcoreFactory.eINSTANCE.createEClass(), null, false);
  }
  
  @Test(expected = IllegalArgumentException.class)
  public void testWithNull() {
    this.inferrer.infer(null, null, false);
  }
}
