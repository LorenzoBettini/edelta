package edelta.util

import edelta.edelta.EdeltaProgram
import org.eclipse.emf.ecore.EObject
import org.eclipse.xtext.EcoreUtil2
import edelta.edelta.EdeltaEcoreCreateEClassExpression

/**
 * Utilities for navigating an Edelta AST model
 * 
 * @author Lorenzo Bettini
 *
 */
class EdeltaModelUtil {

	def EdeltaProgram getProgram(EObject context) {
		EcoreUtil2.getContainerOfType(context, EdeltaProgram)
	}

	def getEClassesCreatedBefore(EObject o) {
		val p = o.program
		p.main.expressions.takeWhile[
			!EcoreUtil2.isAncestor(o, it)
		].filter(EdeltaEcoreCreateEClassExpression)
	}
}