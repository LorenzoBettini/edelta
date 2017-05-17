package edelta.interpreter

import com.google.inject.Inject
import edelta.edelta.EdeltaEcoreCreateEClassExpression
import edelta.edelta.EdeltaEcoreReference
import edelta.edelta.EdeltaEcoreReferenceExpression
import edelta.edelta.EdeltaOperation
import edelta.edelta.EdeltaUseAs
import edelta.lib.AbstractEdelta
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
import edelta.edelta.EdeltaEcoreCreateEAttributeExpression
import edelta.resource.IEdeltaEcoreModelAssociations

class EdeltaInterpreter extends XbaseInterpreter {

	val edelta = new AbstractEdelta() {
		
	}

	@Inject extension IJvmModelAssociations
	@Inject extension EdeltaInterpreterHelper
	@Inject extension IEdeltaEcoreModelAssociations

	def run(EdeltaEcoreCreateEClassExpression e, EClass c, JvmGenericType javaType) {
		evaluate(
			e,
			createContext() => [
				newValue(QualifiedName.create("it"), c)
				// 'this' and the name of the inferred class are mapped
				// to an instance of AbstractEdelta, so that all reflective
				// accesses, e.g., the inherited field 'lib', work out of the box
				// calls to operations defined in the sources are intercepted
				// in our custom invokeOperation and in that case we interpret the
				// original source's XBlockExpression
				newValue(QualifiedName.create("this"), edelta)
				newValue(QualifiedName.create(javaType.simpleName), edelta)
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
		} else if (expression instanceof EdeltaEcoreCreateEAttributeExpression) {
			val attr = expression.getEAttributeElement
			safeSetEAttributeType(attr, expression.ecoreReferenceDataType)
			val newContext = context.fork
			newContext.newValue(QualifiedName.create("it"), attr)
			return evaluate(expression.body, newContext, indicator)
		}
		return super.doEvaluate(expression, context, indicator)
	}

	override protected featureCallField(JvmField jvmField, Object receiver) {
		val useAs = jvmField.sourceElements.filter(EdeltaUseAs).head
		if (useAs !== null) {
			return safeInstantiate(javaReflectAccess, useAs)
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
			return evaluate(originalOperation.body, context, indicator)
		}
		return super.invokeOperation(operation, receiver, argumentValues, parentContext, indicator)
	}

}
