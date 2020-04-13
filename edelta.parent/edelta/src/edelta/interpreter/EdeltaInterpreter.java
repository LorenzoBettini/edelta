package edelta.interpreter;

import static org.eclipse.xtext.xbase.lib.CollectionLiterals.newHashMap;

import java.util.List;
import java.util.Map;

import org.eclipse.emf.ecore.ENamedElement;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.xtext.common.types.JvmDeclaredType;
import org.eclipse.xtext.common.types.JvmField;
import org.eclipse.xtext.common.types.JvmFormalParameter;
import org.eclipse.xtext.common.types.JvmGenericType;
import org.eclipse.xtext.common.types.JvmOperation;
import org.eclipse.xtext.diagnostics.Severity;
import org.eclipse.xtext.naming.QualifiedName;
import org.eclipse.xtext.util.CancelIndicator;
import org.eclipse.xtext.util.IResourceScopeCache;
import org.eclipse.xtext.util.Wrapper;
import org.eclipse.xtext.validation.EObjectDiagnosticImpl;
import org.eclipse.xtext.xbase.XExpression;
import org.eclipse.xtext.xbase.interpreter.IEvaluationContext;
import org.eclipse.xtext.xbase.interpreter.IEvaluationResult;
import org.eclipse.xtext.xbase.interpreter.impl.XbaseInterpreter;
import org.eclipse.xtext.xbase.lib.Exceptions;

import com.google.common.base.Objects;
import com.google.inject.Inject;

import edelta.compiler.EdeltaCompilerUtil;
import edelta.edelta.EdeltaEcoreReference;
import edelta.edelta.EdeltaEcoreReferenceExpression;
import edelta.edelta.EdeltaModifyEcoreOperation;
import edelta.edelta.EdeltaOperation;
import edelta.edelta.EdeltaPackage;
import edelta.edelta.EdeltaUseAs;
import edelta.jvmmodel.EdeltaJvmModelHelper;
import edelta.validation.EdeltaValidator;

/**
 * Interprets the modifyEcore operations of an EdeltaProgram.
 * 
 * @author Lorenzo Bettini
 */
public class EdeltaInterpreter extends XbaseInterpreter implements IEdeltaInterpreter {
	@Inject
	private EdeltaJvmModelHelper edeltaJvmModelHelper;

	@Inject
	private EdeltaInterpreterHelper edeltaInterpreterHelper;

	@Inject
	private EdeltaCompilerUtil edeltaCompilerUtil;

	@Inject
	private IResourceScopeCache cache;

	private int interpreterTimeout = Integer.parseInt(System.getProperty("edelta.interpreter.timeout", "2000"));

	private JvmGenericType programInferredJavaType;

	private static final QualifiedName IT_QUALIFIED_NAME = QualifiedName.create("it");

	private EdeltaInterpreterEdeltaImpl edelta;

	private Map<EdeltaUseAs, Object> useAsFields;

	@Override
	public void setInterpreterTimeout(final int interpreterTimeout) {
		this.interpreterTimeout = interpreterTimeout;
	}

	@Override
	public void run(final Iterable<EdeltaModifyEcoreOperation> ops, final Map<String, EPackage> nameToCopiedEPackageMap,
			final JvmGenericType jvmGenericType, final List<EPackage> ePackages) {
		class EdeltaInterpreterCancelIndicator implements CancelIndicator {
			long stopAt = System.currentTimeMillis() + EdeltaInterpreter.this.interpreterTimeout;

			@Override
			public boolean isCanceled() {
				return System.currentTimeMillis() > stopAt;
			}
		}

		programInferredJavaType = jvmGenericType;
		edelta = new EdeltaInterpreterEdeltaImpl(ePackages);
		useAsFields = newHashMap();
		for (final EdeltaModifyEcoreOperation op : ops) {
			final EPackage ePackage = nameToCopiedEPackageMap.get(op.getEpackage().getName());
			final EdeltaInterpreterCleaner cacheCleaner = new EdeltaInterpreterCleaner(this.cache, op.eResource());
			// clear the cache as soon as the interpreter modifies
			// the EPackage of the modifyEcore expression
			// since new types might be available after the interpretation
			// and existing types might have been modified or renamed
			// this makes sure that scoping and the type computer
			// is performed again
			ePackage.eAdapters().add(cacheCleaner);
			try {
				IEvaluationContext context = this.createContext();
				context.newValue(IT_QUALIFIED_NAME, ePackage);
				// 'this' and the name of the inferred class are mapped
				// to an instance of AbstractEdelta, so that all reflective
				// accesses, e.g., the inherited field 'lib', work out of the box
				// calls to operations defined in the sources are intercepted
				// in our custom invokeOperation and in that case we interpret the
				// original source's XBlockExpression
				context.newValue(QualifiedName.create("this"), edelta);
				context.newValue(QualifiedName.create(programInferredJavaType.getSimpleName()), this.edelta);
				final IEvaluationResult result = evaluate(op.getBody(), context,
						new EdeltaInterpreterCancelIndicator());
				if (result == null) {
					addWarning(op);
				} else {
					Throwable resultException = result.getException();
					if ((resultException != null)) {
						throw Exceptions.sneakyThrow(resultException);
					}
				}
			} finally {
				ePackage.eAdapters().remove(cacheCleaner);
			}
		}
	}

	private boolean addWarning(final EdeltaModifyEcoreOperation op) {
		return op.eResource().getWarnings().add(
			new EObjectDiagnosticImpl(Severity.WARNING,
				EdeltaValidator.INTERPRETER_TIMEOUT,
				"Timeout interpreting initialization block (" +
						Integer.valueOf(this.interpreterTimeout) + "ms).",
				op,
				EdeltaPackage.eINSTANCE.getEdeltaModifyEcoreOperation_Body(),
				-1,
				new String[] {}));
	}

	@Override
	protected Object doEvaluate(final XExpression expression, final IEvaluationContext context,
			final CancelIndicator indicator) {
		if ((expression == null)) {
			return null;
		}
		if (expression instanceof EdeltaEcoreReferenceExpression) {
			return doEvaluate(
				((EdeltaEcoreReferenceExpression) expression).getReference(),
				context, indicator);
		} else if (expression instanceof EdeltaEcoreReference) {
			return evaluateEcoreReference((EdeltaEcoreReference) expression,
				context, indicator);
		}
		return super.doEvaluate(expression, context, indicator);
	}

	private Object evaluateEcoreReference(EdeltaEcoreReference ecoreReference, final IEvaluationContext context,
			final CancelIndicator indicator) {
		final Wrapper<Object> elementWrapper = new Wrapper<>();
		ENamedElement enamedElement = ecoreReference.getEnamedelement();
		if (enamedElement != null) {
			edeltaCompilerUtil.buildMethodToCallForEcoreReference(
				ecoreReference,
				(methodName, args) -> {
					final JvmOperation op = edeltaJvmModelHelper
						.findJvmOperation(programInferredJavaType,
							methodName);
					// it could be null due to an unresolved reference
					// the returned op would be 'getENamedElement'
					// which does not exist in AbstractEdelta
					if (op != null) {
						final Object ref = super.invokeOperation
							(op, edelta, args, context, indicator);
						elementWrapper.set(ref);
					}
				});
		}
		return elementWrapper.get();
	}

	@Override
	protected Object featureCallField(final JvmField jvmField, final Object receiver) {
		final EdeltaUseAs useAs = edeltaJvmModelHelper.findEdeltaUseAs(jvmField);
		if (useAs != null) {
			return useAsFields.computeIfAbsent(useAs,
				it -> edeltaInterpreterHelper.safeInstantiate(
					getJavaReflectAccess(), useAs, edelta));
		}
		return super.featureCallField(jvmField, receiver);
	}

	@Override
	protected Object invokeOperation(final JvmOperation operation, final Object receiver,
			final List<Object> argumentValues, final IEvaluationContext parentContext,
			final CancelIndicator indicator) {
		final JvmDeclaredType declaringType = operation.getDeclaringType();
		if (Objects.equal(declaringType, programInferredJavaType)) {
			final EdeltaOperation originalOperation =
				edeltaJvmModelHelper.findEdeltaOperation(operation);
			final IEvaluationContext context = parentContext.fork();
			int index = 0;
			List<JvmFormalParameter> params = operation.getParameters();
			for (final JvmFormalParameter param : params) {
				context.newValue(
					QualifiedName.create(param.getName()),
					argumentValues.get(index++));
			}
			return internalEvaluate(originalOperation.getBody(), context, indicator);
		}
		return super.invokeOperation(operation, receiver, argumentValues, parentContext, indicator);
	}
}
