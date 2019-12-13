package example.example;

import static org.junit.Assert.assertTrue;

import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EPackage;
import org.junit.Test;

import com.example.Example;

import edelta.lib.AbstractEdelta;

/**
 * Unit test for simple App.
 */
public class AppTest {

	@Test
	public void testRunApp() throws Exception {
		// Create an instance of the generated Java class
		AbstractEdelta edelta = new Example();
		// Make sure you load all the used Ecores
		edelta.loadEcoreFile("model/My.ecore");
		// Execute the actual transformations defined in the DSL
		edelta.execute();
		// Save the modified Ecore model into a new path
		edelta.saveModifiedEcores("modified");

		// load the modified file
		AbstractEdelta verifier = new AbstractEdelta() {
		};
		verifier.loadEcoreFile("modified/My.ecore");
		// verify the structure of the modified ecore
		EPackage ePackage = verifier.getEPackage("myecore");
		EClass myNewClass = (EClass) ePackage.getEClassifier("MyNewClass");
		EAttribute myNewAttribute = (EAttribute) myNewClass.getEStructuralFeature("ANewAttribute");
		assertTrue(myNewAttribute.isRequired());
	}
}
