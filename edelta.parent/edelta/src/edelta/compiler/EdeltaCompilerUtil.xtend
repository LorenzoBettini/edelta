package edelta.compiler

import com.google.inject.Inject
import edelta.edelta.EdeltaEcoreChangeEClassExpression
import edelta.edelta.EdeltaEcoreCreateEAttributeExpression
import edelta.edelta.EdeltaEcoreCreateEClassExpression
import edelta.edelta.EdeltaEcoreReference
import edelta.edelta.EdeltaEcoreReferenceExpression
import org.eclipse.emf.ecore.EClassifier
import org.eclipse.emf.ecore.EEnumLiteral
import org.eclipse.emf.ecore.EPackage
import org.eclipse.emf.ecore.EStructuralFeature
import org.eclipse.xtext.xbase.XExpression
import org.eclipse.xtext.xbase.typesystem.IBatchTypeResolver

import static extension org.eclipse.xtext.EcoreUtil2.*

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

	def dispatch String methodName(EdeltaEcoreChangeEClassExpression e) {
		'''_changeEClass_«e.name»_in_«e.epackage?.name»'''
	}

	def dispatch String methodName(EdeltaEcoreCreateEAttributeExpression e) {
		'''_createEAttribute_«e.name»_in«e.getContainerOfType(EdeltaEcoreCreateEClassExpression).methodName»'''
	}

	def consumerArguments(EdeltaEcoreCreateEClassExpression e) {
		val ecoreRefSuperTypes = e.ecoreReferenceSuperTypes
		if (!ecoreRefSuperTypes.empty) {
			return '''
			
			  createList(
			    c -> {
			      «FOR ref : ecoreRefSuperTypes»
			      c.getESuperTypes().add(«ref.stringForEcoreReference»);
			      «ENDFOR»
			    },
			    this::«e.methodName»
			  )
			'''
		}
		return "createList(this::" + e.methodName + ")"
	}

	def consumerArguments(EdeltaEcoreChangeEClassExpression e) {
		if (e.name !== null) {
			return '''
			  createList(
			    c -> c.setName(«e.name»),
			    this::«e.methodName»
			  )
			'''
		}
		return "createList(this::" + e.methodName + ")"
	}

	def consumerArguments(EdeltaEcoreCreateEAttributeExpression e) {
		val ecoreRefType = e.ecoreReferenceDataType
		if (ecoreRefType !== null) {
			return '''
			
			  createList(
			    a -> a.setEType(«ecoreRefType.stringForEcoreReference»),
			    this::«e.methodName»
			  )
			'''
		}
		return "createList(this::" + e.methodName + ")"
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
		val reference = e.reference
		if (reference === null)
			return "null"
		getStringForEcoreReference(reference)
	}

	def getStringForEcoreReference(EdeltaEcoreReference e) {
		val type = e.resolveTypes.getActualType(e)
		val enamedelement = e.enamedelement
		if (enamedelement instanceof EClassifier) {
			return '''get«type.simpleName»("«enamedelement.EPackageNameOrNull»", "«enamedelement.name»")'''
		} else if (enamedelement instanceof EEnumLiteral) {
			return '''get«type.simpleName»("«enamedelement.EEnum.EPackageNameOrNull»", "«enamedelement.EEnumNameOrNull»", "«enamedelement.name»")'''
		} else {
			// unresolved proxies are of type EAttribute so we cast it to EStructuralFeature
			val f = enamedelement as EStructuralFeature
			return '''get«type.simpleName»("«f.EContainingClass.EPackageNameOrNull»", "«f.EClassNameOrNull»", "«f.name»")'''
		}
	}
}
