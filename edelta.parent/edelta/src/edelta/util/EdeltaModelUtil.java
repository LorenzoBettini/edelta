package edelta.util;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.EcoreUtil2;

import edelta.edelta.EdeltaProgram;

/**
 * Some utilities for navigating an Edelta AST model and Ecore model.
 * 
 * @author Lorenzo Bettini
 */
public class EdeltaModelUtil {

	private EdeltaModelUtil() {
	}

	public static EdeltaProgram getProgram(final EObject context) {
		return EcoreUtil2.getContainerOfType(context, EdeltaProgram.class);
	}
}
