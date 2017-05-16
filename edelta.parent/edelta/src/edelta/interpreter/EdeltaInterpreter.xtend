package edelta.interpreter

import com.google.inject.Inject
import edelta.edelta.EdeltaEcoreCreateEClassExpression
import edelta.edelta.EdeltaEcoreReference
import edelta.edelta.EdeltaEcoreReferenceExpression
import edelta.lib.EdeltaLibrary
import java.util.List
import org.eclipse.emf.ecore.EClass
import org.eclipse.xtext.common.types.JvmField
import org.eclipse.xtext.common.types.JvmGenericType
import org.eclipse.xtext.common.types.JvmOperation
import org.eclipse.xtext.naming.QualifiedName
import org.eclipse.xtext.util.CancelIndicator
import org.eclipse.xtext.xbase.XExpression
import org.eclipse.xtext.xbase.interpreter.IEvaluationContext
import org.eclipse.xtext.xbase.interpreter.impl.XbaseInterpreter
import org.eclipse.xtext.xbase.jvmmodel.IJvmModelAssociations
import edelta.edelta.EdeltaOperation

class EdeltaInterpreter extends XbaseInterpreter {

	val lib = new EdeltaLibrary

	@Inject extension IJvmModelAssociations

	def run(EdeltaEcoreCreateEClassExpression e, EClass c, JvmGenericType javaType) {
		evaluate(
			e,
			createContext() => [
				newValue(QualifiedName.create("it"), c)
				newValue(QualifiedName.create("this"), javaType)
				newValue(QualifiedName.create(javaType.simpleName), javaType)
			],
			CancelIndicator.NullImpl
		)
	}

	override protected doEvaluate(XExpression expression, IEvaluationContext context, CancelIndicator indicator) {
		if (expression instanceof EdeltaEcoreCreateEClassExpression) {
			return super.doEvaluate(expression.body, context, indicator)
		} else if (expression instanceof EdeltaEcoreReferenceExpression) {
			return doEvaluate(expression.reference, context, indicator)
		} else if (expression instanceof EdeltaEcoreReference) {
			return expression.enamedelement
		}
		return super.doEvaluate(expression, context, indicator)
	}

	override protected featureCallField(JvmField jvmField, Object receiver) {
		if (receiver instanceof JvmGenericType) {
			if (jvmField.simpleName == "lib") {
				return lib
			}
		}
		return super.featureCallField(jvmField, receiver)
	}

	override protected invokeOperation(JvmOperation operation, Object receiver, List<Object> argumentValues,
			IEvaluationContext parentContext, CancelIndicator indicator) {
		val originalOperation = operation.sourceElements.head
		if (originalOperation instanceof EdeltaOperation) {
			val context = parentContext.fork
			var index = 0
			for (param : operation.parameters) {
				context.newValue(QualifiedName.create(param.name), argumentValues.get(index))
				index = index + 1	
			}
			val result = evaluate(originalOperation.body, context, CancelIndicator.NullImpl)
			if(result.exception !== null)
				throw result.exception
			return result.result
		}
		return super.invokeOperation(operation, receiver, argumentValues, parentContext, indicator)
	}

}
