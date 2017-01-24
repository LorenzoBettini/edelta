package edelta.compiler

import com.google.inject.Inject
import edelta.edelta.EdeltaEcoreCreateEAttributeExpression
import edelta.edelta.EdeltaEcoreCreateEClassExpression
import edelta.edelta.EdeltaEcoreReferenceExpression
import org.eclipse.emf.ecore.EClassifier
import org.eclipse.emf.ecore.EPackage
import org.eclipse.emf.ecore.EStructuralFeature
import org.eclipse.xtext.xbase.XExpression
import org.eclipse.xtext.xbase.typesystem.IBatchTypeResolver

import static extension org.eclipse.xtext.EcoreUtil2.*
import org.eclipse.emf.ecore.EEnumLiteral

/**
 * Utilities for Edelta compiler
 * 
 * @author Lorenzo Bettini
 */
class EdeltaCompilerUtil {

	@Inject extension IBatchTypeResolver

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

	def String getEEnumNameOrNull(EEnumLiteral literal) {
		literal.EEnum?.name
	}

	def getStringForEcoreReferenceExpression(EdeltaEcoreReferenceExpression e) {
		val type = e.resolveTypes.getActualType(e)
		val enamedelement = e.reference?.enamedelement
		if (enamedelement instanceof EClassifier) {
			return '''get«type.simpleName»("«enamedelement.EPackageNameOrNull»", "«enamedelement.name»")'''
		} else if (enamedelement instanceof EStructuralFeature) {
			return '''get«type.simpleName»("«enamedelement.EContainingClass.EPackageNameOrNull»", "«enamedelement.EClassNameOrNull»", "«enamedelement.name»")'''
		} else if (enamedelement instanceof EEnumLiteral) {
			return '''get«type.simpleName»("«enamedelement.EEnum.EPackageNameOrNull»", "«enamedelement.EEnumNameOrNull»", "«enamedelement.name»")'''
		} else
			return "null"
	}
}
