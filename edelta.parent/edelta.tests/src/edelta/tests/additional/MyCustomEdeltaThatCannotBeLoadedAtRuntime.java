package edelta.tests.additional;

import edelta.lib.EdeltaRuntime;

/**
 * Fake implementation that, when loaded at runtime by the interpreter, should
 * not be loaded.
 */
public class MyCustomEdeltaThatCannotBeLoadedAtRuntime extends EdeltaRuntime {
	public void aMethod() {
		// just for testing
	}
}