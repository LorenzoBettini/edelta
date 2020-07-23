/**
 * 
 */
package edelta.resource.derivedstate;

import java.util.HashSet;

import org.eclipse.emf.ecore.ENamedElement;

/**
 * Keeps track of {@link ENamedElement}s that have been modified during the
 * interpretation.
 * 
 * @author Lorenzo Bettini
 *
 */
public class EdeltaModifiedElements extends HashSet<ENamedElement> {

	private static final long serialVersionUID = 1L;

}
