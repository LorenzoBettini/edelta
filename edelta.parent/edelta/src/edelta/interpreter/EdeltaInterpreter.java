package edelta.interpreter;

import static edelta.edelta.EdeltaPackage.Literals.EDELTA_ECORE_REFERENCE_EXPRESSION__REFERENCE;
import static edelta.util.EdeltaModelUtil.getProgram;
import static org.eclipse.xtext.EcoreUtil2.getAllContentsOfType;
import static org.eclipse.xtext.xbase.lib.CollectionLiterals.newHashMap;
import static org.eclipse.xtext.xbase.lib.IterableExtensions.exists;
import static org.eclipse.xtext.xbase.lib.IterableExtensions.filter;
import static org.eclipse.xtext.xbase.lib.IterableExtensions.forEach;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.emf.ecore.ENamedElement;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.Resource.Diagnostic;
import org.eclipse.xtext.common.types.JvmField;
import org.eclipse.xtext.common.types.JvmFormalParameter;
import org.eclipse.xtext.common.types.JvmOperation;
import org.eclipse.xtext.diagnostics.Severity;
import org.eclipse.xtext.naming.IQualifiedNameProvider;
import org.eclipse.xtext.naming.QualifiedName;
import org.eclipse.xtext.util.CancelIndicator;
import org.eclipse.xtext.util.IResourceScopeCache;
import org.eclipse.xtext.validation.EObjectDiagnosticImpl;
import org.eclipse.xtext.xbase.XBlockExpression;
import org.eclipse.xtext.xbase.XExpression;
import org.eclipse.xtext.xbase.interpreter.IEvaluationContext;
import org.eclipse.xtext.xbase.interpreter.IEvaluationResult;
import org.eclipse.xtext.xbase.interpreter.impl.InterpreterCanceledException;
import org.eclipse.xtext.xbase.interpreter.impl.XbaseInterpreter;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.inject.Inject;

import edelta.compiler.EdeltaCompilerUtil;
import edelta.edelta.EdeltaEcoreReference;
import edelta.edelta.EdeltaEcoreReferenceExpression;
import edelta.edelta.EdeltaModifyEcoreOperation;
import edelta.edelta.EdeltaOperation;
import edelta.edelta.EdeltaProgram;
import edelta.edelta.EdeltaUseAs;
import edelta.jvmmodel.EdeltaJvmModelHelper;
import edelta.resource.derivedstate.EdeltaCopiedEPackagesMap;
import edelta.resource.derivedstate.EdeltaDerivedStateHelper;
import edelta.resource.derivedstate.EdeltaENamedElementXExpressionMap;
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
	private EdeltaInterpreterEdeltaImpl thisObject;

	private Map<EdeltaUseAs, Object> useAsFields;

	private EdeltaProgram currentProgram;

	private EdeltaInterpreterResourceListener listener;

	/**
	 * The current {@link XExpression} being interpreted that is worthwhile to keep
	 * track of.
	 */
	private XExpression currentExpression;

	class EdeltaInterpreterCancelIndicator implements CancelIndicator {
		long stopAt = System.currentTimeMillis() +
				interpreterTimeout;

		@Override
		public boolean isCanceled() {
			return System.currentTimeMillis() > stopAt;
		}
	}

	public void setInterpreterTimeout(final int interpreterTimeout) {
		this.interpreterTimeout = interpreterTimeout;
	}

	public void evaluateModifyEcoreOperations(final EdeltaProgram program, final EdeltaCopiedEPackagesMap copiedEPackagesMap) {
		this.currentProgram = program;
		final Collection<EPackage> copiedEPackages = copiedEPackagesMap.values();
		thisObject = new EdeltaInterpreterEdeltaImpl
			(Lists.newArrayList(
				Iterables.concat(copiedEPackages,
						program.getMetamodels())),
			derivedStateHelper);
		useAsFields = newHashMap();
		List<EdeltaModifyEcoreOperation> filteredOperations =
			edeltaInterpreterHelper.filterOperations(program.getModifyEcoreOperations());
		final Resource eResource = program.eResource();
		listener = new EdeltaInterpreterResourceListener(cache, eResource,
				derivedStateHelper.getEnamedElementXExpressionMap(eResource));
		try {
			addResourceListener(copiedEPackages);
			for (final EdeltaModifyEcoreOperation op : filteredOperations) {
				evaluateModifyEcoreOperation(op, copiedEPackagesMap);
			}
		} finally {
			removeResourceListener(copiedEPackages);
		}
	}

	private void removeResourceListener(final Collection<EPackage> copiedEPackages) {
		// this will also trigger the last event caught by our adapter
		// implying a final clearing, which is required to avoid
		// duplicate errors
		for (EPackage ePackage : copiedEPackages) {
			ePackage.eAdapters().remove(listener);
		}
	}

	private void addResourceListener(final Collection<EPackage> copiedEPackages) {
		// The listener clears the cache as soon as the interpreter modifies
		// the EPackage of the modifyEcore expression
		// since new types might be available after the interpretation
		// and existing types might have been modified or renamed
		// this makes sure that scoping and the type computer
		// is performed again
		for (EPackage ePackage : copiedEPackages) {
			ePackage.eAdapters().add(listener);
		}
	}

	private void evaluateModifyEcoreOperation(final EdeltaModifyEcoreOperation op,
			final EdeltaCopiedEPackagesMap copiedEPackagesMap) {
		final EPackage ePackage = copiedEPackagesMap.
				get(op.getEpackage().getName());
		IEvaluationContext context = createContext();
		context.newValue(IT_QUALIFIED_NAME, ePackage);
		configureContextForJavaThis(context);
		Thread interpreterThread = Thread.currentThread();
		// the following thread checks timeout when interpreting
		// external Java code
		// see https://github.com/LorenzoBettini/edelta/issues/179
		Thread timeoutGuardThread = new Thread() {
			@Override
			public void run() {
				try {
					interpreterThread.join(interpreterTimeout);
					interpreterThread.interrupt();
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			}
		};
		timeoutGuardThread.start();
		final IEvaluationResult result = evaluate(op.getBody(), context,
				new EdeltaInterpreterCancelIndicator());
		timeoutGuardThread.interrupt();
		if (result == null) {
			// our cancel indicator reached timeout
			addTimeoutWarning(op.eResource());
		} else {
			handleResultException(result.getException(), op.eResource());
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

	private void handleResultException(Throwable resultException, Resource resource) {
		if (resultException instanceof InterruptedException) {
			// our timeoutGuardThread interrupted us
			addTimeoutWarning(resource);
		} else if (resultException != null) {
			throw new EdeltaInterpreterWrapperException
				((Exception) resultException);
		}
	}

	private boolean addTimeoutWarning(final Resource resource) {
		return resource.getWarnings().add(
			new EObjectDiagnosticImpl(Severity.WARNING,
				EdeltaValidator.INTERPRETER_TIMEOUT,
				"Timeout while interpreting (" +
						Integer.valueOf(interpreterTimeout) + "ms).",
				currentExpression,
				null,
				-1,
				new String[] {}));
	}

	@Override
	protected Object doEvaluate(final XExpression expression, final IEvaluationContext context,
			final CancelIndicator indicator) {
		if (expression instanceof EdeltaEcoreReferenceExpression) {
			return evaluateEcoreReferenceExpression(
				((EdeltaEcoreReferenceExpression) expression),
				context, indicator);
		}
		updateListenerCurrentExpression(expression);
		return super.doEvaluate(expression, context, indicator);
	}

	private void updateListenerCurrentExpression(XExpression expression) {
		if (listener != null && shouldTrackExpression(expression)) {
			listener.setCurrentExpression(expression);
			thisObject.setCurrentExpression(expression);
			this.currentExpression = expression;
		}
	}

	private boolean shouldTrackExpression(XExpression expression) {
		return expression.eContainer() instanceof XBlockExpression;
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
					postProcess(result, ecoreReferenceExpression);
					checkStaleAccess(result, ecoreReferenceExpression);
				}
				return result;
			});
	}

	private void postProcess(Object result, EdeltaEcoreReferenceExpression exp) {
		if (result != null) {
			// takes a snapshot of the mapping EEnamedElement -> XExpression
			// and associates it to this EdeltaEcoreReferenceExpression
			List<ENamedElement> enamedElements =
				getAllContentsOfType(exp, EdeltaEcoreReference.class)
					.stream().map(EdeltaEcoreReference::getEnamedelement)
					.collect(Collectors.toList());
			EdeltaENamedElementXExpressionMap expMap = derivedStateHelper
				.getEcoreReferenceExpressionState(exp)
				.getEnamedElementXExpressionMap();
			EdeltaENamedElementXExpressionMap elMap = derivedStateHelper
				.getEnamedElementXExpressionMap(exp.eResource());
			elMap.entrySet().stream()
				.filter(entry -> enamedElements.contains(entry.getKey()))
				.forEach(entry -> expMap.put(entry.getKey(), entry.getValue()));
		}
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

	protected Object evaluateEdeltaOperation(EdeltaInterpreterEdeltaImpl thisObject,
			EdeltaProgram program, EdeltaOperation edeltaOperation,
			List<Object> argumentValues, CancelIndicator indicator) {
		this.currentProgram = program;
		this.thisObject = thisObject;
		IEvaluationContext context = createContext();
		configureContextForJavaThis(context);
		configureContextForParameterArguments(context,
				edeltaOperation.getParams(), argumentValues);
		final IEvaluationResult result = evaluate(edeltaOperation.getBody(), context,
				indicator);
		if (result == null ||
				// our timeoutGuardThread interrupted us
				result.getException() instanceof InterruptedException)
			throw new InterpreterCanceledException();
		return result.getResult();
	}

}
