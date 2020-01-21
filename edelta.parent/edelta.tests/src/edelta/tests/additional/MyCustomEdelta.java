package edelta.tests.additional;

import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.EcorePackage;

import edelta.lib.AbstractEdelta;
import edelta.tests.input.Inputs;

/**
 * This is used in {@link Inputs} for "use ... as ..." expressions
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

	public EClass createANewEClass() {
		return createEClass("foo", "ANewClass", null);
	}

	public void createANewEAttribute(EClass c) {
		EAttribute a = createEAttribute(c, "aNewAttr", null);
		a.setEType(EcorePackage.eINSTANCE.getEString());
	}

	public void setAttributeBounds(EAttribute a, int low, int upper) {
		a.setLowerBound(low);
		a.setUpperBound(upper);
	}
}
