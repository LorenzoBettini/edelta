package edelta.compiler

import com.google.inject.Inject
import edelta.edelta.EdeltaEcoreChangeEClassExpression
import edelta.edelta.EdeltaEcoreCreateEAttributeExpression
import edelta.edelta.EdeltaEcoreCreateEClassExpression
import edelta.edelta.EdeltaEcoreReference
import edelta.edelta.EdeltaEcoreReferenceExpression
import edelta.util.EdeltaModelUtil
import org.eclipse.emf.ecore.EClassifier
import org.eclipse.emf.ecore.EEnumLiteral
import org.eclipse.emf.ecore.ENamedElement
import org.eclipse.emf.ecore.EPackage
import org.eclipse.emf.ecore.EStructuralFeature
import org.eclipse.xtext.xbase.XExpression
import org.eclipse.xtext.xbase.typesystem.IBatchTypeResolver
import java.util.List

/**
 * Utilities for Edelta compiler
 * 
 * @author Lorenzo Bettini
 */
class EdeltaCompilerUtil {

	@Inject extension IBatchTypeResolver
	@Inject extension EdeltaModelUtil

	def dispatch String methodName(XExpression e) {
	}

	def dispatch String methodName(EdeltaEcoreCreateEClassExpression e) {
		'''_createEClass_«e.name»_in_«e.epackage.EPackageNameOrNull»'''
	}

	def dispatch String methodName(EdeltaEcoreChangeEClassExpression e) {
		'''_changeEClass_«e.original.nameOrNull»_in_«e.epackage.EPackageNameOrNull»'''
	}

	def dispatch String methodName(EdeltaEcoreCreateEAttributeExpression e) {
		'''_createEAttribute_«e.name»_in«e.getEClassManipulation.methodName»'''
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
			    «e.referenceToMethodName»
			  )
			'''
		}
		return e.createListWithReferenceToMethodName
	}

	def consumerArguments(EdeltaEcoreChangeEClassExpression e) {
		if (e.name !== null) {
			return '''
			
			  createList(
			    c -> c.setName("«e.name»"),
			    «e.referenceToMethodName»
			  )
			'''
		}
		return e.createListWithReferenceToMethodName
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
		return e.createListWithReferenceToMethodName
	}

	def private referenceToMethodName(XExpression e) {
		'''this::«e.methodName»'''
	}

	def private createListWithReferenceToMethodName(XExpression e) {
		"createList(this::" + e.methodName + ")"
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

	def String getNameOrNull(ENamedElement e) {
		e?.name
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
		val builder = new StringBuilder
		buildMethodToCallForEcoreReference(e) [
			name, args |
			builder.append(name)
			builder.append("(")
			builder.append(args.map['''"«it»"'''].join(", "))
			builder.append(")")
		]
		builder.toString
	}

	def void buildMethodToCallForEcoreReference(EdeltaEcoreReference e, (String, List<Object>)=>void acceptor) {
		val type = e.resolveTypes.getActualType(e)
		// in case there's the original referred ENamedElement we use that one
		// since the interpreter could have changed the container of the ENamedElement
		val enamedelement = e.originalEnamedelement ?: e.enamedelement
		if (enamedelement instanceof EClassifier) {
			acceptor.apply(
				'''get«type.simpleName»''',
				#[enamedelement.EPackageNameOrNull, enamedelement.name])
		} else if (enamedelement instanceof EPackage) {
			acceptor.apply(
				'''get«type.simpleName»''',
				#[enamedelement.EPackageNameOrNull])
		} else if (enamedelement instanceof EEnumLiteral) {
			acceptor.apply(
				'''get«type.simpleName»''',
				#[enamedelement.EEnum.EPackageNameOrNull, enamedelement.EEnumNameOrNull, enamedelement.name])
		} else {
			// unresolved proxies are of type EAttribute so we cast it to EStructuralFeature
			val f = enamedelement as EStructuralFeature
			acceptor.apply(
				'''get«type.simpleName»''',
				#[f.EContainingClass.EPackageNameOrNull, f.EClassNameOrNull, f.name])
		}
	}
}
