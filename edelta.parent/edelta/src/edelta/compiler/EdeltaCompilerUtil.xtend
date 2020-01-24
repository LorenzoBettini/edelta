package edelta.compiler

import com.google.inject.Inject
import edelta.edelta.EdeltaEcoreReference
import edelta.edelta.EdeltaEcoreReferenceExpression
import edelta.util.EdeltaEcoreReferenceInformationHelper
import java.util.List
import org.eclipse.emf.ecore.ENamedElement
import org.eclipse.emf.ecore.EPackage

/**
 * Utilities for Edelta compiler
 * 
 * @author Lorenzo Bettini
 */
class EdeltaCompilerUtil {

	@Inject extension EdeltaEcoreReferenceInformationHelper

	def String getEPackageNameOrNull(EPackage e) {
		e?.name
	}

	def String getNameOrNull(ENamedElement e) {
		e?.name
	}

	def getStringForEcoreReferenceExpression(EdeltaEcoreReferenceExpression e) {
		val reference = e.reference
		if (reference === null || reference.enamedelement === null)
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
