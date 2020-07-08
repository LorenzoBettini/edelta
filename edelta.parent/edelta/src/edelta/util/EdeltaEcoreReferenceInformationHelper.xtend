package edelta.util

import com.google.inject.Inject
import edelta.edelta.EdeltaEcoreReferenceExpression
import edelta.resource.derivedstate.EdeltaDerivedStateHelper
import edelta.resource.derivedstate.EdeltaEcoreReferenceState.EdeltaEcoreReferenceStateInformation
import org.eclipse.emf.ecore.EClassifier
import org.eclipse.emf.ecore.EEnumLiteral
import org.eclipse.emf.ecore.ENamedElement
import org.eclipse.emf.ecore.EPackage
import org.eclipse.emf.ecore.EStructuralFeature
import org.eclipse.xtext.naming.IQualifiedNameProvider
import org.eclipse.xtext.xbase.typesystem.IBatchTypeResolver

import static edelta.util.EdeltaModelUtil.*
import org.eclipse.emf.ecore.util.EcoreSwitch

/**
 * Utilities for an ecore reference information
 * 
 * @author Lorenzo Bettini
 */
class EdeltaEcoreReferenceInformationHelper {
	@Inject extension IBatchTypeResolver
	@Inject extension IQualifiedNameProvider
	@Inject extension EdeltaDerivedStateHelper

	def getOrComputeInformation(EdeltaEcoreReferenceExpression exp) {
		val e = exp.reference
		val ecoreReferenceState = e.getEcoreReferenceState
		var existing = ecoreReferenceState.information
		if (existing !== null)
			return existing
		val info = new EdeltaEcoreReferenceStateInformation
		ecoreReferenceState.information = info
		val type = exp.resolveTypes.getActualType(exp)
		info.type = type.simpleName

		val element = e.enamedelement

		new EcoreSwitch<Void> {

			override caseEPackage(EPackage object) {
				info.EPackageName = object.nameOrEmpty
				return null
			}

			override caseEClassifier(EClassifier object) {
				info.EPackageName = object.EPackage.nameOrEmpty
				info.EClassifierName = object.nameOrEmpty
				return null
			}

			override caseEEnumLiteral(EEnumLiteral object) {
				val eEnum = object.EEnum
				info.EPackageName = eEnum.EPackageOrNull.nameOrEmpty
				info.EClassifierName = eEnum.nameOrEmpty
				info.ENamedElementName = element.nameOrEmpty
				return null
			}

			/**
			 * An unresolved proxy is of type EAttribute so we include it in
			 * this case.
			 */
			override caseEStructuralFeature(EStructuralFeature object) {
				val c = object.EContainingClass
				info.EPackageName = c.EPackageOrNull.nameOrEmpty
				info.EClassifierName = c.nameOrEmpty
				info.ENamedElementName = element.nameOrEmpty
				return null
			}

		}.doSwitch(element)

		return info
	}

	def private EPackageOrNull(EClassifier e) {
		e?.EPackage
	}

	def private nameOrEmpty(EPackage e) {
		if (e === null)
			return ""
		if (hasCycleInSuperPackage(e))
			return e.name
		return e.fullyQualifiedName.toString
	}

	def private nameOrEmpty(ENamedElement e) {
		e?.name ?: ""
	}
}
