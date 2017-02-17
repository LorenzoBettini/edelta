package edelta.typesystem

import org.eclipse.emf.ecore.EAttribute
import org.eclipse.emf.ecore.EClass
import org.eclipse.emf.ecore.EDataType
import org.eclipse.emf.ecore.EEnum
import org.eclipse.emf.ecore.EEnumLiteral
import org.eclipse.emf.ecore.ENamedElement
import org.eclipse.emf.ecore.EPackage
import org.eclipse.emf.ecore.EReference
import com.google.inject.Singleton

@Singleton
class EdeltaEcoreTypeHelper {

	def getCorrespondingENamedElement(ENamedElement enamedelement) {
		switch (enamedelement) {
			case enamedelement.eIsProxy: ENamedElement
			EPackage: EPackage
			EClass: EClass
			EEnum: EEnum
			EDataType: EDataType
			EReference: EReference
			EEnumLiteral: EEnumLiteral
			default: EAttribute
		}
	}

}
