package edelta.lib.tests;

import edelta.lib.AbstractEdelta;
import edelta.lib.EdeltaModelManager;

/**
 * Variant that runs all the tests by delegating to another
 * {@link AbstractEdelta}'s package manager.
 * 
 * @author Lorenzo Bettini
 *
 */
public class EdeltaWithOtherEdeltaTest extends EdeltaTest {

	@Override
	public void init() {
		modelManager = new EdeltaModelManager();
		AbstractEdelta other = new TestableEdelta(modelManager);
		edelta = new TestableEdelta(other);
	}
}
