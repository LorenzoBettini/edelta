package edelta.swtbot.tests;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.swtbot.swt.finder.junit.SWTBotJunit4ClassRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Lorenzo Bettini
 *
 */
@RunWith(SWTBotJunit4ClassRunner.class)
public class EdeltaSwtBotTest extends EdeltaAbstractSwtbotTest {

	@Test
	public void canCreateANewProject() throws CoreException {
		createProject();
		assertErrorsInProject(0);
	}

}
