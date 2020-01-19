package edelta.compiler

import com.google.inject.Inject
import edelta.edelta.EdeltaEcoreChangeEClassExpression
import edelta.edelta.EdeltaEcoreCreateEAttributeExpression
import edelta.edelta.EdeltaEcoreCreateEClassExpression
import edelta.edelta.EdeltaEcoreReference
import edelta.edelta.EdeltaEcoreReferenceExpression
import edelta.util.EdeltaEcoreReferenceInformationHelper
import edelta.util.EdeltaModelUtil
import java.util.List
import org.eclipse.emf.ecore.ENamedElement
import org.eclipse.emf.ecore.EPackage
import org.eclipse.xtext.xbase.XExpression

/**
 * Utilities for Edelta compiler
 * 
 * @author Lorenzo Bettini
 */
class EdeltaCompilerUtil {

	@Inject extension EdeltaEcoreReferenceInformationHelper
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

	def String getEPackageNameOrNull(EPackage e) {
		e?.name
	}

	def String getNameOrNull(ENamedElement e) {
		e?.name
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
		val info = e.getOrComputeInformation
		acceptor.apply(
			"get" + info.type,
			<Object>newArrayList(info.EPackageName, info.EClassifierName, info.ENamedElementName)
				.filterNull
				.toList
		)
	}
}
