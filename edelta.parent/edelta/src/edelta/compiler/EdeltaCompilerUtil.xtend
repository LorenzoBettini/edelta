package edelta.compiler

import edelta.edelta.EdeltaEcoreCreateEClassExpression
import org.eclipse.xtext.xbase.XExpression
import edelta.edelta.EdeltaEcoreCreateEAttributeExpression

import static extension org.eclipse.xtext.EcoreUtil2.*

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

	def dispatch String methodName(EdeltaEcoreCreateEAttributeExpression e) {
		'''_createEAttribute_«e.name»_in«e.getContainerOfType(EdeltaEcoreCreateEClassExpression).methodName»'''
	}
}
