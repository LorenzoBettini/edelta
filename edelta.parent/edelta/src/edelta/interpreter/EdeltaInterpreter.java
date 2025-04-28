package edelta.interpreter;

import java.util.List;

import org.eclipse.xtext.util.CancelIndicator;
import org.eclipse.xtext.xbase.interpreter.IEvaluationContext;
import org.eclipse.xtext.xbase.interpreter.IExpressionInterpreter;

import edelta.edelta.EdeltaOperation;
import edelta.edelta.EdeltaProgram;
import edelta.lib.EdeltaRuntime;

/**
 * We need this fat interface to break the cycle between the interpreter and the factory.
 * 
 * @author Lorenzo Bettini
 */
public interface EdeltaInterpreter extends IExpressionInterpreter {

	void setInterpreterTimeout(int interpreterTimeout);

	void setCurrentProgram(EdeltaProgram currentProgram);

	void setThisObject(EdeltaRuntime thisObject);

	void setDiagnosticHelper(EdeltaInterpreterDiagnosticHelper diagnosticHelper);

	void evaluateModifyEcoreOperations(EdeltaProgram program);

	Object evaluateEdeltaOperation(EdeltaOperation edeltaOperation, List<Object> argumentValues, IEvaluationContext context, CancelIndicator indicator);

	IEvaluationContext createContext();

	void configureContextForJavaThis(IEvaluationContext context);

	void setClassLoader(ClassLoader cl);

}