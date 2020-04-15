package edelta.jvmmodel

import com.google.inject.Inject
import edelta.edelta.EdeltaOperation
import edelta.edelta.EdeltaUseAs
import org.eclipse.xtext.common.types.JvmField
import org.eclipse.xtext.common.types.JvmGenericType
import org.eclipse.xtext.common.types.JvmOperation
import org.eclipse.xtext.xbase.jvmmodel.IJvmModelAssociations
import edelta.edelta.EdeltaProgram

class EdeltaJvmModelHelper {
	@Inject extension IJvmModelAssociations

	def findJvmGenericType(EdeltaProgram program) {
		program.jvmElements.filter(JvmGenericType).head
	}

	def findJvmOperation(JvmGenericType jvmGenericType, String methodName) {
		jvmGenericType.allFeatures.filter(JvmOperation).
				findFirst[simpleName == methodName]
	}

	def findEdeltaUseAs(JvmField jvmField) {
		jvmField.sourceElements.filter(EdeltaUseAs).head
	}

	def findEdeltaOperation(JvmOperation operation) {
		operation.sourceElements.head as EdeltaOperation
	}
}
