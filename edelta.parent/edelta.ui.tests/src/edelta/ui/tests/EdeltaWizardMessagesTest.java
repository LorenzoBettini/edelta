package edelta.ui.tests;

import static org.assertj.core.api.Assertions.assertThat;

import org.eclipse.core.runtime.Platform;
import org.junit.Test;

/**
 * Checks that wizard labels are loaded from the UI message bundle.
 *
 * @author Lorenzo Bettini
 */
public class EdeltaWizardMessagesTest {

	/** Verifies that the example and empty project labels are available. */
	@Test
	public void testProjectLabels() throws ReflectiveOperationException {
		var messages = Platform.getBundle("edelta.ui").loadClass("edelta.ui.wizard.Messages");
		assertThat(messages.getField("EdeltaExampleProjectTemplate_Label").get(null))
			.isEqualTo("Edelta Example Project");
		assertThat(messages.getField("EdeltaEmptyProjectTemplate_Label").get(null))
			.isEqualTo("Edelta Empty Project");
	}
}
