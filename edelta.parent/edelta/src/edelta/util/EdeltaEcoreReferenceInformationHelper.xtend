package edelta.util

import com.google.inject.Inject
import edelta.edelta.EdeltaEcoreReference
import edelta.resource.EdeltaDerivedStateHelper
import edelta.util.EdeltaEcoreReferenceState.EdeltaEcoreReferenceStateInformation
import org.eclipse.emf.ecore.EClassifier
import org.eclipse.emf.ecore.EEnumLiteral
import org.eclipse.emf.ecore.ENamedElement
import org.eclipse.emf.ecore.EPackage
import org.eclipse.emf.ecore.EStructuralFeature
import org.eclipse.xtext.naming.IQualifiedNameProvider
import org.eclipse.xtext.xbase.typesystem.IBatchTypeResolver

import static edelta.util.EdeltaModelUtil.*

/**
 * Utilities for an ecore reference information
 * 
 * @author Lorenzo Bettini
 */
class EdeltaEcoreReferenceInformationHelper {
	@Inject extension IBatchTypeResolver
	@Inject extension IQualifiedNameProvider
	@Inject extension EdeltaDerivedStateHelper

	def getOrComputeInformation(EdeltaEcoreReference e) {
		val ecoreReferenceState = e.getEcoreReferenceState
		var info = ecoreReferenceState.information
		if (info !== null)
			return info
		info = new EdeltaEcoreReferenceStateInformation
		ecoreReferenceState.information = info
		val type = e.resolveTypes.getActualType(e)
		info.type = type.simpleName

		val element = e.enamedelement
		switch (element) {
			EPackage:
				info.EPackageName = element.nameOrEmpty
			EClassifier: {
				info.EPackageName = element.EPackage.nameOrEmpty
				info.EClassifierName = element.nameOrEmpty
			}
			EEnumLiteral: {
				val eEnum = element.EEnum
				info.EPackageName = eEnum.EPackageOrNull.nameOrEmpty
				info.EClassifierName = eEnum.nameOrEmpty
				info.ENamedElementName = element.nameOrEmpty
			}
			default: {
				// unresolved proxies are of type EAttribute so we cast it to EStructuralFeature
				val f = element as EStructuralFeature
				val c = f.EContainingClass
				info.EPackageName = c.EPackageOrNull.nameOrEmpty
				info.EClassifierName = c.nameOrEmpty
				info.ENamedElementName = element.nameOrEmpty
			}
		}

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
