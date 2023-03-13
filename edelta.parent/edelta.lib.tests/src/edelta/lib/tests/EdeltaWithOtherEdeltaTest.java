package edelta.lib.tests;

import edelta.lib.EdeltaRuntime;
import edelta.lib.EdeltaModelManager;

/**
 * Variant that runs all the tests by delegating to another
 * {@link EdeltaRuntime}'s package manager.
 * 
 * @author Lorenzo Bettini
 *
 */
public class EdeltaWithOtherEdeltaTest extends EdeltaTest {

	@Override
	public void init() {
		modelManager = new EdeltaModelManager();
		EdeltaRuntime other = new TestableEdelta(modelManager);
		edelta = new TestableEdelta(other);
	}
}
