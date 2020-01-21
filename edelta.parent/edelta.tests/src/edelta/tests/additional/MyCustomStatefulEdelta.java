package edelta.tests.additional;

import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EcorePackage;

import edelta.lib.AbstractEdelta;
import edelta.tests.input.Inputs;

/**
 * This is used in {@link Inputs} for "use ... as ..." expressions and keeps a
 * state.
 * 
 * @author Lorenzo Bettini
 *
 */
public class MyCustomStatefulEdelta extends AbstractEdelta {

	private int counter = 0;

	public MyCustomStatefulEdelta() {
	}

	public MyCustomStatefulEdelta(AbstractEdelta other) {
		super(other);
	}

	public EClass createANewEClass() {
		return createEClass("foo", "ANewClass" + (++counter), null);
	}

	public void createANewEAttribute(EClass c) {
		EAttribute a = createEAttribute(c, "aNewAttr" + (++counter), null);
		a.setEType(EcorePackage.eINSTANCE.getEString());
	}

}
