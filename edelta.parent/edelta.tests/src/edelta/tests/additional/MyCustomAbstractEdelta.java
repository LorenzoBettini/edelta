package edelta.tests.additional;

import edelta.lib.EdeltaRuntime;

/**
 * This is references in inputs for "use ... as ..." expressions
 * 
 * @author Lorenzo Bettini
 *
 */
public abstract class MyCustomAbstractEdelta extends EdeltaRuntime {

	public MyCustomAbstractEdelta(EdeltaRuntime other) {
		super(other);
	}

}
