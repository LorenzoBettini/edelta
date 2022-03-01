package edelta.ui.wizard;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "edelta.ui.wizard.messages"; //$NON-NLS-1$
	
	public static String HelloWorldProject_Label;
	public static String HelloWorldProject_Description;
	public static String EdeltaExampleProjectTemplate_Label;
	public static String EdeltaExampleProjectTemplate_Description;
	
	static {
	// initialize resource bundle
	NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}
	
	private Messages() {
	}
}
