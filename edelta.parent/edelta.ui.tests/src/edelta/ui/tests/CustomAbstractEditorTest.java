package edelta.ui.tests;

import org.eclipse.xtext.ui.testing.AbstractEditorTest;

/**
 * Custom version that avoids removing projects from the workspace.
 * 
 * @author Lorenzo Bettini
 *
 */
public abstract class CustomAbstractEditorTest extends AbstractEditorTest {

	/**
	 * Avoids deleting project
	 */
	@Override
	public void setUp() throws Exception {
		closeWelcomePage();
		closeEditors();
	}

	/**
	 * Avoids deleting project
	 */
	@Override
	public void tearDown() {
		waitForEventProcessing();
		closeEditors();
		waitForEventProcessing();
	}
}
