package edelta.compiler

import edelta.edelta.EdeltaEcoreCreateEClassExpression
import org.eclipse.xtext.xbase.XExpression

/**
 * Utilities for Edelta compiler
 * 
 * @author Lorenzo Bettini
 */
class EdeltaCompilerUtil {
	def dispatch String methodName(XExpression e) {
		
	}
	def dispatch String methodName(EdeltaEcoreCreateEClassExpression e) {
		'''_createEClass_«e.name»_in_«e.epackage?.name»'''
	}
}