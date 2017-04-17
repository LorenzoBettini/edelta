package edelta.tests.additional;

import edelta.lib.AbstractEdelta;

/**
 * This is references in inputs for "use ... as ..." expressions
 * 
 * @author Lorenzo Bettini
 *
 */
public abstract class MyCustomAbstractEdelta extends AbstractEdelta {

	public MyCustomAbstractEdelta() {
	}

	public MyCustomAbstractEdelta(AbstractEdelta other) {
		super(other);
	}

}
