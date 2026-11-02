package edelta.tests.additional;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.eclipse.emf.ecore.EClass;

import edelta.lib.EdeltaRuntime;

/**
 * Test runtime whose blocking operation is interrupted by the interpreter timeout.
 * A bounded wait also lets the test fail if timeout delivery is broken.
 * 
 * @author Lorenzo Bettini
 *
 */
public class MyCustomEdeltaWithTimeout extends EdeltaRuntime {

	public MyCustomEdeltaWithTimeout(EdeltaRuntime other) {
		super(other);
	}

	/** Blocks until interrupted; reaching the model change indicates a missing timeout. */
	public void op(EClass c) throws InterruptedException {
		new CountDownLatch(1).await(30, TimeUnit.SECONDS);
		// A working interpreter timeout prevents this change.
		c.setAbstract(true);
	}
}
