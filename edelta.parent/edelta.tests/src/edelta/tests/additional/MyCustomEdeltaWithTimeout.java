package edelta.tests.additional;

import org.eclipse.emf.ecore.EClass;

import edelta.lib.AbstractEdelta;

/**
 * This is used in tests, it contains an operation that never returns and leads
 * to a timeout in the interpreter.
 * 
 * @author Lorenzo Bettini
 *
 */
public class MyCustomEdeltaWithTimeout extends AbstractEdelta {

	public MyCustomEdeltaWithTimeout() {
	}

	public MyCustomEdeltaWithTimeout(AbstractEdelta other) {
		super(other);
	}

	public void op(EClass c) throws InterruptedException {
		int i = 10;
		while (i >= 0) {
			Thread.sleep(1000);
			i++;
		}
		// this will never be executed
		c.setAbstract(true);
	}
}
