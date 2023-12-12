package edelta.compiler;

import org.eclipse.xtext.xbase.XExpression;
import org.eclipse.xtext.xbase.compiler.XbaseCompiler;
import org.eclipse.xtext.xbase.compiler.output.ITreeAppendable;

import com.google.inject.Inject;

import edelta.edelta.EdeltaEcoreReferenceExpression;

public class EdeltaXbaseCompiler extends XbaseCompiler {
	@Inject
	private EdeltaCompilerUtil edeltaCompilerUtil;

	@Override
	protected void doInternalToJavaStatement(final XExpression obj, final ITreeAppendable appendable,
			final boolean isReferenced) {
		if (obj instanceof EdeltaEcoreReferenceExpression ecoreRefExp) {
			if (!isReferenced) {
				appendable.newLine();
				compileEdeltaEcoreReferenceExpression(
					ecoreRefExp, appendable);
				appendable.append(";");
			}
		} else {
			super.doInternalToJavaStatement(obj, appendable, isReferenced);
		}
	}

	@Override
	protected void internalToConvertedExpression(final XExpression obj, final ITreeAppendable appendable) {
		if (obj instanceof EdeltaEcoreReferenceExpression ecoreRefExp) {
			compileEdeltaEcoreReferenceExpression(
				ecoreRefExp, appendable);
		} else {
			super.internalToConvertedExpression(obj, appendable);
		}
	}

	private void compileEdeltaEcoreReferenceExpression(final EdeltaEcoreReferenceExpression obj,
			final ITreeAppendable appendable) {
		appendable.append(edeltaCompilerUtil.getStringForEcoreReferenceExpression(obj));
	}
}
