/**
 * 
 */
package edelta.lib;

import java.util.function.Consumer;

import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EcoreFactory;

/**
 * Library functions to be reused in Edelta programs.
 * 
 * @author Lorenzo Bettini
 *
 */
public class EdeltaLibrary {

	private EcoreFactory ecoreFactory = EcoreFactory.eINSTANCE;

	public EClass newEClass(String name) {
		return newEClass(name, null);
	}

	public EClass newEClass(String name, Consumer<EClass> initializer) {
		EClass c = ecoreFactory.createEClass();
		c.setName(name);
		if (initializer != null) {
			initializer.accept(c);
		}
		return c;
	}

	public EAttribute newEAttribute(String name) {
		return newEAttribute(name, null);
	}

	public EAttribute newEAttribute(String name, Consumer<EAttribute> initiaizer) {
		EAttribute e = ecoreFactory.createEAttribute();
		e.setName(name);
		if (initiaizer != null) {
			initiaizer.accept(e);
		}
		return e;
	}

	public EReference newEReference(String name) {
		return newEReference(name, null);
	}

	public EReference newEReference(String name, Consumer<EReference> initiaizer) {
		EReference e = ecoreFactory.createEReference();
		e.setName(name);
		if (initiaizer != null) {
			initiaizer.accept(e);
		}
		return e;
	}
}
