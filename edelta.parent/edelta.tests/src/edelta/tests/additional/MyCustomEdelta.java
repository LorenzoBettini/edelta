package edelta.tests.additional;

import org.eclipse.emf.ecore.EStructuralFeature;

import edelta.lib.AbstractEdelta;

/**
 * This is references in inputs for "use ... as ..." expressions
 * 
 * @author Lorenzo Bettini
 *
 */
public class MyCustomEdelta extends AbstractEdelta {

	public MyCustomEdelta() {
	}

	public MyCustomEdelta(AbstractEdelta other) {
		super(other);
	}

	public EStructuralFeature myMethod() {
		return getEStructuralFeature("foo", "FooClass", "myAttribute");
	}

	public void createANewEClass() {
		createEClass("foo", "ANewClass", null);
	}
}
