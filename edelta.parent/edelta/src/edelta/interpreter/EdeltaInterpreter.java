package edelta.interpreter;

import static edelta.edelta.EdeltaPackage.Literals.EDELTA_ECORE_REFERENCE_EXPRESSION__REFERENCE;
import static edelta.edelta.EdeltaPackage.Literals.EDELTA_MODIFY_ECORE_OPERATION__BODY;
import static edelta.util.EdeltaModelUtil.getProgram;
import static org.eclipse.xtext.xbase.lib.CollectionLiterals.newHashMap;
import static org.eclipse.xtext.xbase.lib.IterableExtensions.exists;
import static org.eclipse.xtext.xbase.lib.IterableExtensions.filter;
import static org.eclipse.xtext.xbase.lib.IterableExtensions.forEach;

import java.util.List;
import java.util.Map;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource.Diagnostic;
import org.eclipse.xtext.common.types.JvmField;
import org.eclipse.xtext.common.types.JvmFormalParameter;
import org.eclipse.xtext.common.types.JvmIdentifiableElement;
import org.eclipse.xtext.common.types.JvmOperation;
import org.eclipse.xtext.diagnostics.Severity;
import org.eclipse.xtext.naming.IQualifiedNameProvider;
import org.eclipse.xtext.naming.QualifiedName;
import org.eclipse.xtext.util.CancelIndicator;
import org.eclipse.xtext.util.IResourceScopeCache;
import org.eclipse.xtext.validation.EObjectDiagnosticImpl;
import org.eclipse.xtext.xbase.XAbstractFeatureCall;
import org.eclipse.xtext.xbase.XExpression;
import org.eclipse.xtext.xbase.interpreter.IEvaluationContext;
import org.eclipse.xtext.xbase.interpreter.IEvaluationResult;
import org.eclipse.xtext.xbase.interpreter.impl.XbaseInterpreter;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.inject.Inject;

import edelta.compiler.EdeltaCompilerUtil;
import edelta.edelta.EdeltaEcoreReferenceExpression;
import edelta.edelta.EdeltaModifyEcoreOperation;
import edelta.edelta.EdeltaOperation;
import edelta.edelta.EdeltaProgram;
import edelta.edelta.EdeltaUseAs;
import edelta.jvmmodel.EdeltaJvmModelHelper;
import edelta.lib.AbstractEdelta;
import edelta.resource.derivedstate.EdeltaCopiedEPackagesMap;
import edelta.resource.derivedstate.EdeltaDerivedStateHelper;
import edelta.util.EdeltaModelUtil;
import edelta.validation.EdeltaValidator;

/**
 * Interprets the modifyEcore operations of an EdeltaProgram.
 * 
 * @author Lorenzo Bettini
 */
public class EdeltaInterpreter extends XbaseInterpreter {
	@Inject
	private EdeltaInterpreterFactory edeltaInterpreterFactory;

	@Inject
	private EdeltaJvmModelHelper edeltaJvmModelHelper;

	@Inject
	private EdeltaInterpreterHelper edeltaInterpreterHelper;

	@Inject
	private EdeltaCompilerUtil edeltaCompilerUtil;

	@Inject
	private IResourceScopeCache cache;

	@Inject
	private IQualifiedNameProvider qualifiedNameProvider;

	@Inject
	private EdeltaDerivedStateHelper derivedStateHelper;

	private int interpreterTimeout =
		Integer.parseInt(System.getProperty("edelta.interpreter.timeout", "2000"));

	private static final QualifiedName IT_QUALIFIED_NAME = QualifiedName.create("it");

	/**
	 * Represents the "this" during the interpretation.
	 */
	private AbstractEdelta thisObject;

	private Map<EdeltaUseAs, Object> useAsFields;

	private EdeltaProgram currentProgram;

	private EdeltaInterpreterResourceListener listener;

	class EdeltaInterpreterCancelIndicator implements CancelIndicator {
		long stopAt = System.currentTimeMillis() +
				interpreterTimeout;

		@Override
		public boolean isCanceled() {
			return System.currentTimeMillis() > stopAt;
		}
	}

	/**
	 * Wraps a {@link Throwable} found in the result of interpretation
	 */
	public static class EdeltaInterpreterWrapperException extends RuntimeException {

		private static final long serialVersionUID = 1L;
		private final Exception exception;

		public EdeltaInterpreterWrapperException(Exception exception) {
			super(exception);
			this.exception = exception;
		}

		public Exception getException() {
			return exception;
		}
	}

	public void setInterpreterTimeout(final int interpreterTimeout) {
		this.interpreterTimeout = interpreterTimeout;
	}

	public void evaluateModifyEcoreOperations(final EdeltaProgram program, final EdeltaCopiedEPackagesMap copiedEPackagesMap) {
		this.currentProgram = program;
		thisObject = new EdeltaInterpreterEdeltaImpl
			(Lists.newArrayList(
				Iterables.concat(copiedEPackagesMap.values(),
						program.getMetamodels())));
		useAsFields = newHashMap();
		List<EdeltaModifyEcoreOperation> filteredOperations =
			edeltaInterpreterHelper.filterOperations(program.getModifyEcoreOperations());
		for (final EdeltaModifyEcoreOperation op : filteredOperations) {
			evaluateModifyEcoreOperation(op, copiedEPackagesMap);
		}
	}

	private void evaluateModifyEcoreOperation(final EdeltaModifyEcoreOperation op,
			final EdeltaCopiedEPackagesMap copiedEPackagesMap) {
		final EPackage ePackage = copiedEPackagesMap.
				get(op.getEpackage().getName());
		listener = new EdeltaInterpreterResourceListener(cache, op.eResource(),
			derivedStateHelper.getEnamedElementXExpressionMap(op.eResource()));
		// The listener clears the cache as soon as the interpreter modifies
		// the EPackage of the modifyEcore expression
		// since new types might be available after the interpretation
		// and existing types might have been modified or renamed
		// this makes sure that scoping and the type computer
		// is performed again
		ePackage.eAdapters().add(listener);
		try {
			IEvaluationContext context = createContext();
			context.newValue(IT_QUALIFIED_NAME, ePackage);
			configureContextForJavaThis(context);
			final IEvaluationResult result = evaluate(op.getBody(), context,
					new EdeltaInterpreterCancelIndicator());
			if (result == null) {
				addTimeoutWarning(op);
			} else {
				handleResultException(result.getException());
			}
		} finally {
			// this will also trigger the last event caught by our adapter
			// implying a final clearing, which is required to avoid
			// duplicate errors
			ePackage.eAdapters().remove(listener);
		}
	}

	private void configureContextForJavaThis(IEvaluationContext context) {
		// 'this' and the name of the inferred class are mapped
		// to an instance of AbstractEdelta, so that all reflective
		// accesses, e.g., the inherited field 'lib', work out of the box
		// calls to operations defined in the sources are intercepted
		// in our custom invokeOperation and in that case we interpret the
		// original source's XBlockExpression
		context.newValue(QualifiedName.create("this"), thisObject);
		context.newValue(QualifiedName.create(
				edeltaJvmModelHelper.findJvmGenericType(currentProgram).getSimpleName()),
				thisObject);
	}

	private void handleResultException(Throwable resultException) {
		if (resultException != null) {
			throw new EdeltaInterpreterWrapperException
				((Exception) resultException);
		}
	}

	private boolean addTimeoutWarning(final EdeltaModifyEcoreOperation op) {
		return op.eResource().getWarnings().add(
			new EObjectDiagnosticImpl(Severity.WARNING,
				EdeltaValidator.INTERPRETER_TIMEOUT,
				"Timeout interpreting initialization block (" +
						Integer.valueOf(interpreterTimeout) + "ms).",
				op,
				EDELTA_MODIFY_ECORE_OPERATION__BODY,
				-1,
				new String[] {}));
	}

	@Override
	protected Object assignValueTo(JvmIdentifiableElement feature, XAbstractFeatureCall assignment, Object value,
			IEvaluationContext context, CancelIndicator indicator) {
		if (listener != null)
			listener.setCurrentExpression(assignment);
		return super.assignValueTo(feature, assignment, value, context, indicator);
	}

	@Override
	protected Object doEvaluate(final XExpression expression, final IEvaluationContext context,
			final CancelIndicator indicator) {
		if (expression instanceof EdeltaEcoreReferenceExpression) {
			return evaluateEcoreReferenceExpression(
				((EdeltaEcoreReferenceExpression) expression),
				context, indicator);
		}
		return super.doEvaluate(expression, context, indicator);
	}

	private Object evaluateEcoreReferenceExpression(EdeltaEcoreReferenceExpression ecoreReferenceExpression, final IEvaluationContext context,
			final CancelIndicator indicator) {
		if (ecoreReferenceExpression.getReference() == null ||
			ecoreReferenceExpression.getReference().getEnamedelement() == null)
			return null;
		return edeltaCompilerUtil.buildMethodToCallForEcoreReference(
			ecoreReferenceExpression,
			(methodName, args) -> {
				Object result = null;
				final JvmOperation op = edeltaJvmModelHelper
					.findJvmOperation(
						edeltaJvmModelHelper.findJvmGenericType(currentProgram),
						methodName);
				// it could be null due to an unresolved reference
				// the returned op would be 'getENamedElement'
				// which does not exist in AbstractEdelta
				if (op != null) {
					result = super.invokeOperation
						(op, thisObject, args, context, indicator);
					checkStaleAccess(result, ecoreReferenceExpression);
				}
				return result;
			});
	}

	private void checkStaleAccess(Object result, EdeltaEcoreReferenceExpression ecoreReferenceExpression) {
		if (result == null) {
			addStaleAccessError(
				ecoreReferenceExpression,
				"The element is not available anymore in this context: '" +
					ecoreReferenceExpression.getReference()
					.getEnamedelement().getName() + "'",
					EdeltaValidator.INTERPRETER_ACCESS_REMOVED_ELEMENT,
					new String[] {});
		} else {
			// the effective qualified name of the EObject
			String currentQualifiedName = qualifiedNameProvider
				.getFullyQualifiedName((EObject) result).toString();
			// the reference string in the Edelta program,
			// which is different in case the EObject has been renamed
			String originalReferenceText =
				EdeltaModelUtil.getEcoreReferenceText
					(ecoreReferenceExpression.getReference());
			if (!currentQualifiedName.endsWith(originalReferenceText))
				addStaleAccessError(
					ecoreReferenceExpression,
					String.format(
						"The element '%s' is now available as '%s'",
						originalReferenceText, currentQualifiedName),
					EdeltaValidator.INTERPRETER_ACCESS_RENAMED_ELEMENT,
					new String[] {currentQualifiedName});
		}
	}

	private void addStaleAccessError(EdeltaEcoreReferenceExpression ecoreReferenceExpression,
			String errorMessage, String errorCode, String[] errorData) {
		List<Diagnostic> errors = ecoreReferenceExpression.eResource().getErrors();
		// Avoid adding the same errors several times on the same expression.
		// This can happen if we're interpreting a loop, removing the same element
		if (exists(
				filter(errors, EdeltaInterpreterDiagnostic.class),
				error -> error.getProblematicObject() == ecoreReferenceExpression))
			return;
		errors.add(
			new EdeltaInterpreterDiagnostic(Severity.ERROR,
				errorCode,
				errorMessage,
				ecoreReferenceExpression,
				EDELTA_ECORE_REFERENCE_EXPRESSION__REFERENCE,
				-1,
				errorData));
	}

	@Override
	protected Object featureCallField(final JvmField jvmField, final Object receiver) {
		final EdeltaUseAs useAs = edeltaJvmModelHelper.findEdeltaUseAs(jvmField);
		if (useAs != null) {
			EdeltaProgram useAsTypeProgram = edeltaJvmModelHelper.findEdeltaProgram(useAs.getType());
			// it refers to an external edelta program
			if (useAsTypeProgram != null)
				return useAsTypeProgram;
			// it refers to a Java implementation
			return useAsFields.computeIfAbsent(useAs,
				it -> edeltaInterpreterHelper.safeInstantiate(
					getJavaReflectAccess(), useAs, thisObject));
		}
		return super.featureCallField(jvmField, receiver);
	}

	@Override
	protected Object invokeOperation(final JvmOperation operation, final Object receiver,
			final List<Object> argumentValues, final IEvaluationContext parentContext,
			final CancelIndicator indicator) {
		final EdeltaOperation edeltaOperation =
				edeltaJvmModelHelper.findEdeltaOperation(operation);
		if (edeltaOperation != null) {
			EdeltaProgram containingProgram = getProgram(edeltaOperation);
			if (containingProgram == currentProgram) {
				final IEvaluationContext context = parentContext.fork();
				configureContextForParameterArguments(context,
						operation.getParameters(), argumentValues);
				return internalEvaluate(edeltaOperation.getBody(), context, indicator);
			} else {
				// create a new interpreter since the edelta operation is in
				// another edelta source file.
				EdeltaInterpreter newInterpreter =
						edeltaInterpreterFactory.create(containingProgram.eResource());
				return newInterpreter
					.evaluateEdeltaOperation(thisObject, containingProgram, edeltaOperation, argumentValues, indicator);
			}
		}
		return super.invokeOperation(
			operation, receiver, argumentValues, parentContext, indicator);
	}

	private void configureContextForParameterArguments(final IEvaluationContext context, List<JvmFormalParameter> params,
			final List<Object> argumentValues) {
		forEach(params,
			(param, index) ->
				context.newValue(
					QualifiedName.create(param.getName()),
					argumentValues.get(index))
		);
	}

	protected Object evaluateEdeltaOperation(AbstractEdelta other,
			EdeltaProgram program, EdeltaOperation edeltaOperation,
			List<Object> argumentValues, CancelIndicator indicator) {
		this.currentProgram = program;
		this.thisObject = new AbstractEdelta(other) {
		};
		IEvaluationContext context = createContext();
		configureContextForJavaThis(context);
		configureContextForParameterArguments(context,
				edeltaOperation.getParams(), argumentValues);
		final IEvaluationResult result = evaluate(edeltaOperation.getBody(), context,
				indicator);
		return result.getResult();
	}

}
