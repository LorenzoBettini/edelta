/**
 * 
 */
package edelta.lib;

import java.util.function.Consumer;

import org.eclipse.emf.ecore.EClass;
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
}
