/**
 * 
 */
package edelta.lib;

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
		EClass c = ecoreFactory.createEClass();
		c.setName(name);
		return c;
	}
}
