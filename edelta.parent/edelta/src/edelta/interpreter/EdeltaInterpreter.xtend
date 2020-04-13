package edelta.interpreter

import com.google.inject.Inject
import edelta.compiler.EdeltaCompilerUtil
import edelta.edelta.EdeltaEcoreReference
import edelta.edelta.EdeltaEcoreReferenceExpression
import edelta.edelta.EdeltaModifyEcoreOperation
import edelta.edelta.EdeltaPackage
import edelta.edelta.EdeltaUseAs
import edelta.jvmmodel.EdeltaJvmModelHelper
import edelta.validation.EdeltaValidator
import java.util.List
import java.util.Map
import org.eclipse.emf.ecore.EPackage
import org.eclipse.xtext.common.types.JvmField
import org.eclipse.xtext.common.types.JvmGenericType
import org.eclipse.xtext.common.types.JvmOperation
import org.eclipse.xtext.diagnostics.Severity
import org.eclipse.xtext.naming.QualifiedName
import org.eclipse.xtext.util.CancelIndicator
import org.eclipse.xtext.util.IResourceScopeCache
import org.eclipse.xtext.util.Wrapper
import org.eclipse.xtext.validation.EObjectDiagnosticImpl
import org.eclipse.xtext.xbase.XExpression
import org.eclipse.xtext.xbase.interpreter.IEvaluationContext
import org.eclipse.xtext.xbase.interpreter.impl.XbaseInterpreter

/**
 * Interprets the modifyEcore operations of an EdeltaProgram.
 * 
 * @author Lorenzo Bettini
 */
class EdeltaInterpreter extends XbaseInterpreter implements IEdeltaInterpreter {

	@Inject extension EdeltaJvmModelHelper
	@Inject extension EdeltaInterpreterHelper
	@Inject extension EdeltaCompilerUtil
	@Inject IResourceScopeCache cache

	var int interpreterTimeout =
		Integer.parseInt(System.getProperty("edelta.interpreter.timeout", "2000"));

	var JvmGenericType programInferredJavaType;

	val IT_QUALIFIED_NAME = QualifiedName.create("it")

	var EdeltaInterpreterEdeltaImpl edelta

	var Map<EdeltaUseAs, Object> useAsFields;

	override void setInterpreterTimeout(int interpreterTimeout) {
		this.interpreterTimeout = interpreterTimeout
	}

	override run(Iterable<EdeltaModifyEcoreOperation> ops,
			Map<String, EPackage> nameToCopiedEPackageMap,
			JvmGenericType jvmGenericType, List<EPackage> ePackages) {
		this.programInferredJavaType = jvmGenericType
		edelta = new EdeltaInterpreterEdeltaImpl(ePackages)
		useAsFields = newHashMap
		for (op : ops) {
			val ePackage = nameToCopiedEPackageMap.get(op.epackage.name)
			val cacheCleaner = new EdeltaInterpreterCleaner(cache, op.eResource)
			// clear the cache as soon as the interpreter modifies
			// the EPackage of the modifyEcore expression
			// since new types might be available after the interpretation
			// and existing types might have been modified or renamed
			// this makes sure that scoping and the type computer
			// is performed again
			ePackage.eAdapters += cacheCleaner
			try {
				val result = evaluate(
					op.body,
					createContext() => [
						newValue(IT_QUALIFIED_NAME, ePackage)
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
				} else if (result.exception !== null)
					throw result.exception
			} finally {
				ePackage.eAdapters.remove(cacheCleaner)
			}
		}
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
		if (expression instanceof EdeltaEcoreReferenceExpression) {
			return doEvaluate(expression.reference, context, indicator)
		} else if (expression instanceof EdeltaEcoreReference) {
			val elementWrapper = new Wrapper
			if (expression.enamedelement !== null) {
				buildMethodToCallForEcoreReference(expression) [
					methodName, args |
					val op = programInferredJavaType.findJvmOperation(methodName)
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
			}
			return elementWrapper.get
		}
		return super.doEvaluate(expression, context, indicator)
	}

	override protected featureCallField(JvmField jvmField, Object receiver) {
		val useAs = jvmField.findEdeltaUseAs
		if (useAs !== null) {
			return useAsFields.computeIfAbsent(useAs)
				[safeInstantiate(javaReflectAccess, useAs, edelta)]
		}
		return super.featureCallField(jvmField, receiver)
	}

	override protected invokeOperation(JvmOperation operation, Object receiver, List<Object> argumentValues,
			IEvaluationContext parentContext, CancelIndicator indicator) {
		val declaringType = operation.declaringType
		if (declaringType == programInferredJavaType) {
			val originalOperation = operation.findEdeltaOperation
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
