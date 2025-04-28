package edelta.interpreter;

import static edelta.edelta.EdeltaPackage.Literals.EDELTA_ECORE_REFERENCE_EXPRESSION__REFERENCE;
import static edelta.util.EdeltaModelUtil.getEcoreReferenceText;
import static edelta.util.EdeltaModelUtil.getProgram;
import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toCollection;
import static org.eclipse.xtext.EcoreUtil2.getAllContentsOfType;
import static org.eclipse.xtext.xbase.lib.CollectionLiterals.newHashMap;
import static org.eclipse.xtext.xbase.lib.IterableExtensions.exists;
import static org.eclipse.xtext.xbase.lib.IterableExtensions.filter;
import static org.eclipse.xtext.xbase.lib.IterableExtensions.forEach;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceImpl;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.xtext.common.types.JvmField;
import org.eclipse.xtext.common.types.JvmFormalParameter;
import org.eclipse.xtext.common.types.JvmIdentifiableElement;
import org.eclipse.xtext.common.types.JvmOperation;
import org.eclipse.xtext.diagnostics.Diagnostic;
import org.eclipse.xtext.diagnostics.Severity;
import org.eclipse.xtext.naming.IQualifiedNameProvider;
import org.eclipse.xtext.naming.QualifiedName;
import org.eclipse.xtext.util.CancelIndicator;
import org.eclipse.xtext.util.IResourceScopeCache;
import org.eclipse.xtext.util.Strings;
import org.eclipse.xtext.xbase.XAbstractFeatureCall;
import org.eclipse.xtext.xbase.XBlockExpression;
import org.eclipse.xtext.xbase.XExpression;
import org.eclipse.xtext.xbase.XReturnExpression;
import org.eclipse.xtext.xbase.XbasePackage;
import org.eclipse.xtext.xbase.interpreter.IEvaluationContext;
import org.eclipse.xtext.xbase.interpreter.impl.DefaultEvaluationResult;
import org.eclipse.xtext.xbase.interpreter.impl.EvaluationException;
import org.eclipse.xtext.xbase.interpreter.impl.InterpreterCanceledException;
import org.eclipse.xtext.xbase.interpreter.impl.XbaseInterpreter;

import com.google.inject.Inject;

import edelta.compiler.EdeltaCompilerUtil;
import edelta.edelta.EdeltaEcoreDirectReference;
import edelta.edelta.EdeltaEcoreReference;
import edelta.edelta.EdeltaEcoreReferenceExpression;
import edelta.edelta.EdeltaModifyEcoreOperation;
import edelta.edelta.EdeltaOperation;
import edelta.edelta.EdeltaProgram;
import edelta.edelta.EdeltaUseAs;
import edelta.jvmmodel.EdeltaJvmModelHelper;
import edelta.lib.EdeltaDefaultRuntime;
import edelta.lib.EdeltaModelManager;
import edelta.lib.EdeltaRuntime;
import edelta.resource.derivedstate.EdeltaCopiedEPackagesMap;
import edelta.resource.derivedstate.EdeltaDerivedStateHelper;
import edelta.util.EdeltaEcoreHelper;
import edelta.util.EdeltaModelUtil;
import edelta.validation.EdeltaValidator;

/**
 * Interprets the modifyEcore operations of an EdeltaProgram.
 * 
 * @author Lorenzo Bettini
 */
public class EdeltaDefaultInterpreter extends XbaseInterpreter implements EdeltaInterpreter {
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

	@Inject
	private EdeltaInterpreterDiagnosticHelper diagnosticHelper;

	@Inject
	private EdeltaEcoreHelper ecoreHelper;

	private int interpreterTimeout =
		Integer.parseInt(System.getProperty("edelta.interpreter.timeout", "5000"));

	private static final QualifiedName IT_QUALIFIED_NAME = QualifiedName.create("it");

	/**
	 * Represents the "this" during the interpretation.
	 */
	private EdeltaRuntime thisObject;

	private Map<EdeltaUseAs, Object> useAsFields;

	private EdeltaProgram currentProgram;

	private EdeltaInterpreterResourceListener listener;

	/**
	 * Keeps track of {@link EdeltaEcoreReference} already evaluated.
	 */
	private Collection<EdeltaEcoreReferenceExpression> interpretedEcoreReferenceExpressions =
		new HashSet<>();

	@Override
	public void setInterpreterTimeout(final int interpreterTimeout) {
		this.interpreterTimeout = interpreterTimeout;
	}

	@Override
	public void setCurrentProgram(EdeltaProgram currentProgram) {
		this.currentProgram = currentProgram;
	}

	@Override
	public void setThisObject(EdeltaRuntime thisObject) {
		this.thisObject = thisObject;
	}

	@Override
	public void setDiagnosticHelper(EdeltaInterpreterDiagnosticHelper diagnosticHelper) {
		this.diagnosticHelper = diagnosticHelper;
	}

	/**
	 * Made it public to be part of our interpreter interface
	 */
	@Override
	public IEvaluationContext createContext() {
		return super.createContext();
	}

	@Override
	public void evaluateModifyEcoreOperations(final EdeltaProgram program) {
		currentProgram = program;
		final var eResource = program.eResource();
		final var copiedEPackagesMap = derivedStateHelper
				.getCopiedEPackagesMap(eResource);
		final var copiedEPackages = copiedEPackagesMap.values();
		thisObject = createThisObject(copiedEPackagesMap);
		useAsFields = newHashMap();
		var filteredOperations =
			edeltaInterpreterHelper.filterOperations(program.getModifyEcoreOperations());
		listener = new EdeltaInterpreterResourceListener(cache, eResource,
				derivedStateHelper,
				diagnosticHelper);
		var sandboxResourceSet = createSandboxResourceSet(copiedEPackages);
		try {
			addResourceListener(copiedEPackages);
			for (final var op : filteredOperations) {
				evaluateModifyEcoreOperation(op, copiedEPackagesMap);
			}
		} finally {
			removeResourceListener(copiedEPackages);
			releaseSandboxResourceSet(sandboxResourceSet, eResource, copiedEPackagesMap);
		}
	}

	/**
	 * The copied {@link EPackage}s are stored in {@link Resource}s and in a
	 * {@link ResourceSet}, temporarily for the interpretation time (otherwise
	 * they're stored in the same resource of the current program) so that during
	 * the interpretation we cannot try to access other contents (they cannot access
	 * the original EPackage for instance). This way, the interpreted code's access
	 * to EPackages is restricted to this sandbox (the resource set).
	 * 
	 * @param copiedEPackages
	 * @return
	 */
	private ResourceSet createSandboxResourceSet(final Collection<EPackage> copiedEPackages) {
		var sandboxResourceSet = new ResourceSetImpl();
		for (var copiedEPackage : copiedEPackages) {
			var sandBoxResource = new ResourceImpl();
			sandboxResourceSet.getResources().add(sandBoxResource);
			sandBoxResource.getContents().add(copiedEPackage);
		}
		return sandboxResourceSet;
	}

	/**
	 * The sandbox {@link ResourceSet} is cleared after putting back the copied
	 * {@link EPackage}s into the program's {@link Resource}.
	 * 
	 * @param sandboxResourceSet
	 * @param eResource
	 * @param copiedEPackagesMap
	 */
	private void releaseSandboxResourceSet(ResourceSet sandboxResourceSet, final Resource eResource,
			final EdeltaCopiedEPackagesMap copiedEPackagesMap) {
		derivedStateHelper.addToProgramResource(eResource, copiedEPackagesMap);
		sandboxResourceSet.getResources().clear();
	}

	/**
	 * Uses the passed {@link EdeltaCopiedEPackagesMap} to create an
	 * {@link EdeltaDefaultRuntime} that implements
	 * {@link EdeltaDefaultRuntime#getEPackage(String)} by simply delegating to the
	 * passed map. Note that the passed map will be shared, so that updates to that
	 * map are automatically used.
	 * 
	 * This simulates what the final generated code would do with real loaded ecore
	 * files, but, instead, it uses the copied EPackages in memory.
	 * 
	 * Moreover, it uses an {@link EdeltaInterpreterIssuePresenter}, so that we
	 * catch possible reported warnings and errors to create markers on the fly.
	 * 
	 * @param copiedEPackagesMap
	 */
	private EdeltaRuntime createThisObject(final EdeltaCopiedEPackagesMap copiedEPackagesMap) {
		var edeltaRuntime = new EdeltaDefaultRuntime(new EdeltaModelManager() {
				@Override
				public EPackage getEPackage(String packageName) {
					return copiedEPackagesMap.get(packageName);
				}
			});
		edeltaRuntime.setIssuePresenter(new EdeltaInterpreterIssuePresenter(diagnosticHelper));
		return edeltaRuntime;
	}

	private void removeResourceListener(final Collection<EPackage> copiedEPackages) {
		// this will also trigger the last event caught by our adapter
		// implying a final clearing, which is required to avoid
		// duplicate errors
		for (var ePackage : copiedEPackages) {
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
		for (var ePackage : copiedEPackages) {
			ePackage.eAdapters().add(listener);
		}
	}

	private void evaluateModifyEcoreOperation(final EdeltaModifyEcoreOperation op,
			final EdeltaCopiedEPackagesMap copiedEPackagesMap) {
		final var ePackage = copiedEPackagesMap.get(op.getEpackage().getName());
		var context = createContext();
		context.newValue(IT_QUALIFIED_NAME, ePackage);
		configureContextForJavaThis(context);
		var interpreterThread = Thread.currentThread();
		// the following thread checks timeout when interpreting
		// external Java code
		// see https://github.com/LorenzoBettini/edelta/issues/179
		var timeoutGuardThread = new Thread() {
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
		final var result = evaluate(op.getBody(), context,
				CancelIndicator.NullImpl);
		timeoutGuardThread.interrupt();
		if (result == null) {
			// our cancel indicator reached timeout
			addTimeoutWarning();
		} else {
			handleResultException(result.getException());
		}
	}

	@Override
	public void configureContextForJavaThis(IEvaluationContext context) {
		/*
		 * 'this' and the name of the inferred class are mapped to an instance of
		 * AbstractEdelta, so that all reflective accesses, e.g., the inherited field
		 * 'lib', work out of the box calls to operations defined in the sources are
		 * intercepted in our custom invokeOperation and in that case we interpret the
		 * original source's XBlockExpression
		 */
		context.newValue(QualifiedName.create("this"), thisObject);
		context.newValue(QualifiedName.create(
				edeltaJvmModelHelper.findJvmGenericType(currentProgram).getSimpleName()),
				thisObject);
	}

	private void handleResultException(Throwable resultException) {
		if (resultException instanceof InterruptedException) {
			// our timeoutGuardThread interrupted us
			addTimeoutWarning();
		} else if (resultException != null) {
			throw new EdeltaInterpreterWrapperException
				((Exception) resultException);
		}
	}

	private void addTimeoutWarning() {
		diagnosticHelper.addWarning(null, EdeltaValidator.INTERPRETER_TIMEOUT,
			"Timeout while interpreting (" +
					Integer.valueOf(interpreterTimeout) + "ms).");
	}

	@Override
	protected Object doEvaluate(final XExpression expression, final IEvaluationContext context,
			final CancelIndicator indicator) {
		if (expression instanceof EdeltaEcoreReferenceExpression ecoreRefExp) {
			return evaluateEcoreReferenceExpression(
				ecoreRefExp, context, indicator);
		}
		updateListenerCurrentExpression(expression);
		try {
			return super.doEvaluate(expression, context, indicator);
		} catch (IllegalArgumentException | IllegalStateException e) {
			/*
			 * first make sure to interpret all ecoreref expressions so that we still
			 * collect information about them this is required because in some expressions,
			 * the evaluation might terminate early with an IAE without interpreting
			 * ecorerefs, e.g., in ecoreref(NonExistant).abstract = true the IAE is thrown
			 * because abstract cannot be resolved without interpreting
			 * ecoreref(NonExistant) and we cannot collect information about that
			 */
			getAllContentsOfType(expression, EdeltaEcoreReferenceExpression.class)
				.stream()
				.filter(not(interpretedEcoreReferenceExpressions::contains))
				.forEach(ecoreRefExp -> {
					try {
						evaluateEcoreReferenceExpression(ecoreRefExp, context, indicator);
					} catch (IllegalArgumentException | IllegalStateException e1) {
						// we might get exceptions also when trying to evaluating ecoreref
						recordUnresolvedReference(ecoreRefExp);
					}
				});
			// we let the interpreter go on as much as possible
			return new DefaultEvaluationResult(null, null);
		}
	}

	private void updateListenerCurrentExpression(XExpression expression) {
		if (listener != null && shouldTrackExpression(expression)) {
			listener.setCurrentExpression(expression);
			diagnosticHelper.setCurrentExpression(expression);
		}
	}

	private boolean shouldTrackExpression(XExpression expression) {
		return expression.eContainer() instanceof XBlockExpression;
	}

	private Object evaluateEcoreReferenceExpression(EdeltaEcoreReferenceExpression ecoreReferenceExpression, final IEvaluationContext context,
			final CancelIndicator indicator) {
		// always make sure to record the currently available elements
		derivedStateHelper.setAccessibleElements(ecoreReferenceExpression,
				ecoreHelper.createSnapshotOfAccessibleElements(ecoreReferenceExpression));
		if (!checkValidReference(ecoreReferenceExpression))
			return null;
		checkLinking(ecoreReferenceExpression);
		return edeltaCompilerUtil.buildMethodToCallForEcoreReference(
			ecoreReferenceExpression,
			(methodName, args) -> {
				Object result = null;
				final var op = edeltaJvmModelHelper
					.findJvmOperation(
						edeltaJvmModelHelper.findJvmGenericType(currentProgram),
						methodName);
				/*
				 * it could be null due to an unresolved reference the returned op would be
				 * 'getENamedElement' which does not exist in AbstractEdelta
				 */
				if (op != null) {
					result = super.invokeOperation
						(op, thisObject, args, context, indicator);
					postProcess(result, ecoreReferenceExpression);
					checkStaleAccess(result, ecoreReferenceExpression);
				} else {
					recordUnresolvedReference(ecoreReferenceExpression);
				}
				interpretedEcoreReferenceExpressions.add(ecoreReferenceExpression);
				return result;
			});
	}

	private boolean checkValidReference(EdeltaEcoreReferenceExpression ecoreReferenceExpression) {
		final var ecoreReference = ecoreReferenceExpression.getReference();
		if (ecoreReference == null)
			return false;
		if (ecoreReference.getEnamedelement() == null) {
			// it might be due to an element removed with EcoreUtil.delete ...
			// (see https://github.com/LorenzoBettini/edelta/issues/271)
			final var ecoreReferenceText = getEcoreReferenceText(ecoreReference);
			// ... if the program text contains something ...
			if (!Strings.isEmpty(ecoreReferenceText))
				addNotAvailableAnymoreError(ecoreReferenceExpression, ecoreReferenceText);
			// ... otherwise it's simply an incomplete program with ecoreref()
			return false;
		}
		return true;
	}

	/**
	 * Checks whether the linked reference is ambiguous in the current context and
	 * also makes sure to relink the reference in case in this context there's no
	 * ambiguity.
	 * 
	 * @param ecoreReferenceExpression
	 */
	private void checkLinking(EdeltaEcoreReferenceExpression ecoreReferenceExpression) {
		final var ecoreReference = ecoreReferenceExpression.getReference();
		// qualified references are not considered since they cannot be ambiguous
		if (ecoreReference instanceof EdeltaEcoreDirectReference) {
			var refText = EdeltaModelUtil.getEcoreReferenceText(ecoreReference);
			// qualification '.' is the boundary for searching for matches
			var toSearch = "." + refText;
			final var matching = ecoreHelper.getCurrentAccessibleElements(ecoreReferenceExpression)
				.stream()
				.filter(e -> e.getQualifiedName().toString().endsWith(toSearch))
				.toList();
			if (matching.size() > 1) {
				Collection<String> matchingNames = matching.stream()
					.map(e -> e.getQualifiedName().toString())
					.collect(toCollection(LinkedHashSet::new));
				ecoreReferenceExpression.eResource().getErrors().add(
					new EdeltaInterpreterDiagnostic(Severity.ERROR,
						EdeltaValidator.AMBIGUOUS_REFERENCE,
						"Ambiguous reference '" + refText + "':\n" +
							matchingNames.stream()
								.map(m -> "  " + m)
								.collect(joining("\n")),
						ecoreReferenceExpression,
						EDELTA_ECORE_REFERENCE_EXPRESSION__REFERENCE,
						-1,
						matchingNames.toArray(new String[0])));
			} else if (matching.size() == 1) {
				var newCandidate = matching.get(0).getElement();
				if (newCandidate != ecoreReference.getEnamedelement())
					ecoreReference.setEnamedelement(newCandidate);
			}
		}
	}

	/**
	 * Record the unresolved reference in the derived state; subsequent type
	 * computations or relinking might make it resolvable but if it's not resolvable
	 * now, it means that in this part of the program it is not available and we'll
	 * have to issue a validation error explicitly in the validator
	 * 
	 * @param ecoreReferenceExpression
	 */
	private void recordUnresolvedReference(EdeltaEcoreReferenceExpression ecoreReferenceExpression) {
		derivedStateHelper
			.getUnresolvedEcoreReferences(ecoreReferenceExpression.eResource())
			.add(ecoreReferenceExpression.getReference());
	}

	private void postProcess(Object result, EdeltaEcoreReferenceExpression exp) {
		if (result != null) {
			// takes a snapshot of the mapping EEnamedElement -> XExpression
			// and associates it to this EdeltaEcoreReferenceExpression
			var enamedElements =
				getAllContentsOfType(exp, EdeltaEcoreReference.class)
					.stream().map(EdeltaEcoreReference::getEnamedelement)
					.toList();
			var expMap = derivedStateHelper
				.getEcoreReferenceExpressionState(exp)
				.getEnamedElementXExpressionMap();
			var elMap = derivedStateHelper
				.getEnamedElementXExpressionMap(exp.eResource());
			elMap.entrySet().stream()
				.filter(entry -> enamedElements.contains(entry.getKey()))
				.forEach(entry -> expMap.put(entry.getKey(), entry.getValue()));
		}
	}

	private void checkStaleAccess(Object result, EdeltaEcoreReferenceExpression ecoreReferenceExpression) {
		final var ecoreReferenceText = getEcoreReferenceText
				(ecoreReferenceExpression.getReference());
		if (result == null) {
			addNotAvailableAnymoreError(ecoreReferenceExpression, ecoreReferenceText);
		} else {
			// the effective qualified name of the EObject
			var currentQualifiedName = qualifiedNameProvider
				.getFullyQualifiedName((EObject) result).toString();
			// rely on the reference string in the Edelta program,
			// which is different in case the EObject has been renamed
			if (!currentQualifiedName.endsWith(ecoreReferenceText))
				addStaleAccessError(
					ecoreReferenceExpression,
					String.format(
						"The element '%s' is now available as '%s'",
						ecoreReferenceText, currentQualifiedName),
					EdeltaValidator.INTERPRETER_ACCESS_RENAMED_ELEMENT,
					new String[] {currentQualifiedName});
		}
	}

	private void addNotAvailableAnymoreError(EdeltaEcoreReferenceExpression ecoreReferenceExpression, String name) {
		addStaleAccessError(
			ecoreReferenceExpression,
			"The element is not available anymore in this context: '" +
				name + "'",
				EdeltaValidator.INTERPRETER_ACCESS_REMOVED_ELEMENT,
				new String[] {});
	}

	private void addStaleAccessError(EdeltaEcoreReferenceExpression ecoreReferenceExpression,
			String errorMessage, String errorCode, String[] errorData) {
		var errors = ecoreReferenceExpression.eResource().getErrors();
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
		final var useAs = edeltaJvmModelHelper.findEdeltaUseAs(jvmField);
		if (useAs != null) {
			var useAsTypeProgram = edeltaJvmModelHelper.findEdeltaProgram(useAs.getType());
			// it refers to an external edelta program
			if (useAsTypeProgram != null)
				return useAsTypeProgram;
			// it refers to a Java implementation, instantiated by reflection.
			// Note that since we pass thisObject, the instantiated implementation
			// will share the same package manager (see creatThisObject) and
			// the same issue representer.
			return useAsFields.computeIfAbsent(useAs,
				it -> edeltaInterpreterHelper.safeInstantiate(
					getJavaReflectAccess(), useAs, thisObject));
		}
		return super.featureCallField(jvmField, receiver);
	}

	@Override
	protected Object invokeFeature(JvmIdentifiableElement feature, XAbstractFeatureCall featureCall, Object receiverObj,
			IEvaluationContext context, CancelIndicator indicator) {
		try {
			return super.invokeFeature(feature, featureCall, receiverObj, context, indicator);
		} catch (IllegalStateException e) {
			checkUnresolvedFeatureDueToRelinking(feature, featureCall, e);
			throw e;
		}
	}

	@Override
	protected Object assignValueTo(JvmIdentifiableElement feature, XAbstractFeatureCall assignment, Object value,
			IEvaluationContext context, CancelIndicator indicator) {
		try {
			return super.assignValueTo(feature, assignment, value, context, indicator);
		} catch (IllegalStateException e) {
			checkUnresolvedFeatureDueToRelinking(feature, assignment, e);
			throw e;
		}
	}

	private void checkUnresolvedFeatureDueToRelinking(JvmIdentifiableElement feature, XAbstractFeatureCall featureCall,
			IllegalStateException e) {
		if (e.getCause() instanceof IllegalArgumentException) {
			/* it means that receiver expression (an ecoreref) has been relinked
			 * (see checkLinking) and the previously resolved feature is not
			 * in the new type of the relinked ecoreref. The type computer will
			 * not detect this changed, since the feature had already been linked,
			 * so we must explicitly add an error. */
			featureCall.eResource().getErrors().add(
				new EdeltaInterpreterDiagnostic(Severity.ERROR,
					Diagnostic.LINKING_DIAGNOSTIC,
					"Cannot refer to " + feature.getIdentifier(),
					featureCall,
					XbasePackage.Literals.XABSTRACT_FEATURE_CALL__FEATURE,
					-1,
					new String[] {}));
		}
	}

	@Override
	protected Object invokeOperation(final JvmOperation operation, final Object receiver,
			final List<Object> argumentValues, final IEvaluationContext parentContext,
			final CancelIndicator indicator) {
		final var edeltaOperation =
				edeltaJvmModelHelper.findEdeltaOperation(operation);
		if (edeltaOperation != null) {
			var containingProgram = getProgram(edeltaOperation);
			if (containingProgram == currentProgram) {
				return evaluateEdeltaOperation(edeltaOperation, argumentValues,
						parentContext.fork(), indicator);
			} else {
				// create a new interpreter since the edelta operation is in
				// another edelta source file.
				// First copy the other program's imported metamodels into
				// the current program's derived state
				final var eResource = currentProgram.eResource();
				// update copied packages with possible new ones from
				// the containing program
				derivedStateHelper
						.copyEPackages(containingProgram, eResource);

				var newInterpreter =
						edeltaInterpreterFactory.create(containingProgram.eResource());
				newInterpreter.setCurrentProgram(containingProgram);
				// since we pass thisObject, the new interpreter will share
				// the same package manager (see creatThisObject) and
				// the same issue representer.
				newInterpreter.setThisObject(new EdeltaDefaultRuntime(thisObject));
				// it is crucial to share the diagnosticHelper where the
				// currentExpression is correctly set to avoid a NPE
				// see testExecutionOfSeveralFilesWithUseAsAndIssuePresenter
				newInterpreter.setDiagnosticHelper(diagnosticHelper);
				var context = newInterpreter.createContext();
				newInterpreter.configureContextForJavaThis(context);
				return newInterpreter.evaluateEdeltaOperation(edeltaOperation, argumentValues,
						context, indicator);
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

	/**
	 * Since this is meant to execute a method, we must call evaluate, not
	 * internalEvaluate, otherwise if in the method there's a return it will
	 * terminate the whole interpretation, since a return is interpreted with a
	 * throw of a ReturnValue (see
	 * {@link XbaseInterpreter#_doEvaluate(XReturnExpression, IEvaluationContext, CancelIndicator)}).
	 * If we call evaluate then it will take care of catching the exception
	 * ReturnValue and will wrap it in a returned {@link DefaultEvaluationResult}.
	 * 
	 * @param edeltaOperation
	 * @param argumentValues
	 * @param context
	 * @param indicator
	 * @return
	 */
	@Override
	public Object evaluateEdeltaOperation(EdeltaOperation edeltaOperation, List<Object> argumentValues,
			IEvaluationContext context, CancelIndicator indicator) {
		configureContextForParameterArguments(context,
				edeltaOperation.getParams(), argumentValues);
		final var result = evaluate(edeltaOperation.getBody(), context,
				indicator);
		if (result == null ||
				// our timeoutGuardThread interrupted us
				result.getException() instanceof InterruptedException) {
			throw new InterpreterCanceledException();
		}
		if (result.getException() != null) {
			throw new EvaluationException(result.getException());
		}
		return result.getResult();
	}

}
