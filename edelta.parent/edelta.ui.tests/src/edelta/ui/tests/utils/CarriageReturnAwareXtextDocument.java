package edelta.ui.tests.utils;

import org.eclipse.xtext.resource.OutdatedStateManager;
import org.eclipse.xtext.service.OperationCanceledManager;
import org.eclipse.xtext.ui.editor.model.DocumentTokenSource;
import org.eclipse.xtext.ui.editor.model.XtextDocument;
import org.eclipse.xtext.ui.editor.model.edit.ITextEditComposer;

import com.google.inject.Inject;

import edelta.ui.quickfix.EdeltaQuickfixProvider;
import edelta.ui.tests.EdeltaQuickfixTest;

/**
 * Makes sure that the returned String with {@link #get()} does not contain '\r'
 * characters that might be inserted by our quickfix provider method
 * {@link EdeltaQuickfixProvider#moveToRightPosition} in Windows.
 * 
 * In the presence of Java Text Block in {@link EdeltaQuickfixTest}, such
 * characters would make the test fail.
 * 
 * @author Lorenzo Bettini
 *
 */
public class CarriageReturnAwareXtextDocument extends XtextDocument {

	public CarriageReturnAwareXtextDocument(DocumentTokenSource tokenSource, ITextEditComposer composer,
			OutdatedStateManager outdatedStateManager, OperationCanceledManager operationCanceledManager) {
		super(tokenSource, composer, outdatedStateManager, operationCanceledManager);
	}

	@Inject
	public CarriageReturnAwareXtextDocument(DocumentTokenSource tokenSource, ITextEditComposer composer) {
		super(tokenSource, composer);
	}

	@Override
	public String get() {
		return super.get().replace("\r", "");
	}
}
