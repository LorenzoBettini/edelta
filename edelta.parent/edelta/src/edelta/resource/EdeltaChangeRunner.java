/**
 * 
 */
package edelta.resource;

import org.eclipse.emf.ecore.EClass;

import edelta.edelta.EdeltaEcoreChangeEClassExpression;

/**
 * Performs changes specified in a change expression.
 * 
 * @author Lorenzo Bettini
 *
 */
public class EdeltaChangeRunner {

	public void performChanges(EClass c, EdeltaEcoreChangeEClassExpression changeExp) {
		if (changeExp.getName() != null) {
			c.setName(changeExp.getName());
		}
	}
}
