package edelta.tests.additional;

import edelta.lib.AbstractEdelta;

/**
 * Fake implementation that, when loaded at runtime by the interpreter, should
 * not be loaded.
 */
public class MyCustomEdeltaThatCannotBeLoadedAtRuntime extends AbstractEdelta {
	public void aMethod() {
		// just for testing
	}
}