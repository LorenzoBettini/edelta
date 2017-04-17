package edelta.lib.tests;

import edelta.lib.AbstractEdelta;

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
		AbstractEdelta other = new TestableEdelta();
		edelta = new TestableEdelta(other);
	}
}
