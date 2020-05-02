package edelta.util;

import edelta.edelta.EdeltaProgram;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.EcoreUtil2;

/**
 * Some utilities for navigating an Edelta AST model and Ecore model.
 * 
 * @author Lorenzo Bettini
 */
public class EdeltaModelUtil {
	public EdeltaProgram getProgram(final EObject context) {
		return EcoreUtil2.getContainerOfType(context, EdeltaProgram.class);
	}
}
