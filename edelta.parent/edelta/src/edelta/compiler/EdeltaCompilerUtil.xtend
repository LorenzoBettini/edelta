package edelta.compiler

import edelta.edelta.EdeltaEcoreCreateEAttributeExpression
import edelta.edelta.EdeltaEcoreCreateEClassExpression
import org.eclipse.emf.ecore.EClassifier
import org.eclipse.emf.ecore.EPackage
import org.eclipse.emf.ecore.EStructuralFeature
import org.eclipse.xtext.xbase.XExpression

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

	def consumerArgumentForBody(XExpression body) {
		var String consumerArgument = "null"
		if (body !== null)
			consumerArgument = "createList(this::" + (body.eContainer as XExpression).methodName + ")"
		return consumerArgument
	}

	def String getEPackageNameOrNull(EClassifier eClassifier) {
		eClassifier?.EPackage.getEPackageNameOrNull
	}

	def String getEPackageNameOrNull(EPackage e) {
		e?.name
	}

	def String getEClassNameOrNull(EStructuralFeature eFeature) {
		eFeature.EContainingClass?.name
	}
}
