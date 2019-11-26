package edelta.interpreter

import com.google.inject.Inject
import edelta.compiler.EdeltaCompilerUtil
import edelta.edelta.EdeltaEcoreBaseEClassManipulationWithBlockExpression
import edelta.edelta.EdeltaEcoreCreateEAttributeExpression
import edelta.edelta.EdeltaEcoreReference
import edelta.edelta.EdeltaEcoreReferenceExpression
import edelta.edelta.EdeltaOperation
import edelta.edelta.EdeltaPackage
import edelta.edelta.EdeltaUseAs
import edelta.util.EdeltaEcoreHelper
import edelta.validation.EdeltaValidator
import java.util.List
import org.eclipse.emf.ecore.EAttribute
import org.eclipse.emf.ecore.EClass
import org.eclipse.emf.ecore.EPackage
import org.eclipse.xtext.common.types.JvmField
import org.eclipse.xtext.common.types.JvmGenericType
import org.eclipse.xtext.common.types.JvmOperation
import org.eclipse.xtext.diagnostics.Severity
import org.eclipse.xtext.naming.QualifiedName
import org.eclipse.xtext.util.CancelIndicator
import org.eclipse.xtext.util.Wrapper
import org.eclipse.xtext.validation.EObjectDiagnosticImpl
import org.eclipse.xtext.xbase.XExpression
import org.eclipse.xtext.xbase.interpreter.IEvaluationContext
import org.eclipse.xtext.xbase.interpreter.impl.XbaseInterpreter
import org.eclipse.xtext.xbase.jvmmodel.IJvmModelAssociations
import edelta.edelta.EdeltaModifyEcoreOperation

class EdeltaInterpreter extends XbaseInterpreter implements IEdeltaInterpreter {

	@Inject extension IJvmModelAssociations
	@Inject extension EdeltaInterpreterHelper
	@Inject extension EdeltaCompilerUtil
	@Inject extension EdeltaEcoreHelper

	var int interpreterTimeout = 2000;

	var JvmGenericType programInferredJavaType;

	val IT_QUALIFIED_NAME = QualifiedName.create("it")

	var EdeltaInterpreterEdeltaImpl edelta

	override void setInterpreterTimeout(int interpreterTimeout) {
		this.interpreterTimeout = interpreterTimeout
	}

	override run(EdeltaEcoreBaseEClassManipulationWithBlockExpression e, EClass c,
		JvmGenericType programInferredJavaType, List<EPackage> packages
	) {
		this.programInferredJavaType = programInferredJavaType
		edelta = new EdeltaInterpreterEdeltaImpl(packages)
		val result = evaluate(
			e,
			createContext() => [
				newValue(IT_QUALIFIED_NAME, c)
				// 'this' and the name of the inferred class are mapped
				// to an instance of AbstractEdelta, so that all reflective
				// accesses, e.g., the inherited field 'lib', work out of the box
				// calls to operations defined in the sources are intercepted
				// in our custom invokeOperation and in that case we interpret the
				// original source's XBlockExpression
				newValue(QualifiedName.create("this"), edelta)
				newValue(QualifiedName.create(programInferredJavaType.simpleName), edelta)
			],
			new CancelIndicator() {
				long stopAt = System.currentTimeMillis() + interpreterTimeout;
				override boolean isCanceled() {
					return System.currentTimeMillis() > stopAt;
				}
			}
		)
		if (result === null) {
			addWarning(e)
		}
		return result
	}

	override run(EdeltaModifyEcoreOperation op, EPackage p,
		JvmGenericType programInferredJavaType, List<EPackage> packages
	) {
		this.programInferredJavaType = programInferredJavaType
		edelta = new EdeltaInterpreterEdeltaImpl(packages)
		val result = evaluate(
			op.body,
			createContext() => [
				newValue(IT_QUALIFIED_NAME, p)
				// 'this' and the name of the inferred class are mapped
				// to an instance of AbstractEdelta, so that all reflective
				// accesses, e.g., the inherited field 'lib', work out of the box
				// calls to operations defined in the sources are intercepted
				// in our custom invokeOperation and in that case we interpret the
				// original source's XBlockExpression
				newValue(QualifiedName.create("this"), edelta)
				newValue(QualifiedName.create(programInferredJavaType.simpleName), edelta)
			],
			new CancelIndicator() {
				long stopAt = System.currentTimeMillis() + interpreterTimeout;
				override boolean isCanceled() {
					return System.currentTimeMillis() > stopAt;
				}
			}
		)
		if (result === null) {
			addWarning(op)
		}
		return result
	}

	def private addWarning(EdeltaEcoreBaseEClassManipulationWithBlockExpression e) {
		e.eResource.getWarnings().add(
			new EObjectDiagnosticImpl(
				Severity.WARNING,
				EdeltaValidator.INTERPRETER_TIMEOUT,
				"Timeout interpreting initialization block ("+interpreterTimeout+"ms).",
				e,
				EdeltaPackage.eINSTANCE.edeltaEcoreBaseManipulationWithBlockExpression_Body,
				-1,
				#[])
		)
	}

	def private addWarning(EdeltaModifyEcoreOperation op) {
		op.eResource.getWarnings().add(
			new EObjectDiagnosticImpl(
				Severity.WARNING,
				EdeltaValidator.INTERPRETER_TIMEOUT,
				"Timeout interpreting initialization block ("+interpreterTimeout+"ms).",
				op,
				EdeltaPackage.eINSTANCE.edeltaModifyEcoreOperation_Body,
				-1,
				#[])
		)
	}
	override protected doEvaluate(XExpression expression, IEvaluationContext context, CancelIndicator indicator) {
		if (expression === null)
			return null
		if (expression instanceof EdeltaEcoreBaseEClassManipulationWithBlockExpression) {
			return doEvaluate(expression.body, context, indicator)
		} else if (expression instanceof EdeltaEcoreReferenceExpression) {
			return doEvaluate(expression.reference, context, indicator)
		} else if (expression instanceof EdeltaEcoreReference) {
			val elementWrapper = new Wrapper
			buildMethodToCallForEcoreReference(expression) [
				methodName, args |
				val op = findJvmOperation(methodName)
				// it could be null due to an unresolved reference
				// the returned op would be getENamedElement
				// which does not exist in AbstractEdelta
				if (op !== null) {
					val ref = super.invokeOperation(
						op, edelta,
						args, context, indicator
					)
					elementWrapper.set(ref)
				}
			]
			return elementWrapper.get
		} else if (expression instanceof EdeltaEcoreCreateEAttributeExpression) {
			val eclass = context.getValue(IT_QUALIFIED_NAME) as EClass
			val attr = eclass.EStructuralFeatures.filter(EAttribute).
				getByName(expression.name)
			safeSetEAttributeType(attr, expression.ecoreReferenceDataType)
			val newContext = context.fork
			newContext.newValue(IT_QUALIFIED_NAME, attr)
			return internalEvaluate(expression.body, newContext, indicator)
		}
		return super.doEvaluate(expression, context, indicator)
	}

	def private findJvmOperation(String methodName) {
		programInferredJavaType.allFeatures.filter(JvmOperation).
				findFirst[simpleName == methodName]
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
		val declaringType = operation.declaringType
		if (declaringType == programInferredJavaType) {
			val originalOperation = operation.sourceElements.head as EdeltaOperation
			val context = parentContext.fork
			var index = 0
			for (param : operation.parameters) {
				context.newValue(QualifiedName.create(param.name), argumentValues.get(index))
				index = index + 1	
			}
			return internalEvaluate(originalOperation.body, context, indicator)
		}
		return super.invokeOperation(operation, receiver, argumentValues, parentContext, indicator)
	}

}
