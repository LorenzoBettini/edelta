package edelta.tests.additional;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EcorePackage;

import edelta.lib.AbstractEdelta;
import edelta.lib.EdeltaDefaultRuntime;
import edelta.tests.input.Inputs;

/**
 * This is used in {@link Inputs} for "use ... as ..." expressions and keeps a
 * state.
 * 
 * @author Lorenzo Bettini
 *
 */
public class MyCustomStatefulEdelta extends EdeltaDefaultRuntime {

	private int counter = 0;

	public MyCustomStatefulEdelta() {
	}

	public MyCustomStatefulEdelta(AbstractEdelta other) {
		super(other);
	}

	public EClass createANewEClass() {
		return stdLib.addNewEClass(getEPackage("foo"), "ANewClass" + (++counter));
	}

	public void createANewEAttribute(EClass c) {
		stdLib.addNewEAttribute(c, "aNewAttr" + (++counter),
				EcorePackage.eINSTANCE.getEString());
	}

}
