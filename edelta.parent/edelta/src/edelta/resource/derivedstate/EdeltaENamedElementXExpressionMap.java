package edelta.resource.derivedstate;

import java.util.HashMap;

import org.eclipse.emf.ecore.ENamedElement;
import org.eclipse.xtext.xbase.XExpression;

/**
 * Associates an {@link ENamedElement} to the {@link XExpression} that created it
 * during the interpretation.
 * 
 * @author Lorenzo Bettini
 *
 */
public class EdeltaENamedElementXExpressionMap extends HashMap<ENamedElement, XExpression> {

	private static final long serialVersionUID = 1L;

}
