package example.example;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EcorePackage;
import org.junit.Test;

import com.example.Example;

import edelta.lib.AbstractEdelta;
import edelta.lib.EdeltaDefaultRuntime;
import edelta.lib.EdeltaEngine;

/**
 * Unit test for simple App.
 */
public class AppTest {

	@Test
	public void testRunApp() throws Exception {
		// create the engine specifying the generated Java class
		EdeltaEngine engine = new EdeltaEngine(Example::new);
		// Make sure you load all the used Ecores (Ecore.ecore is always loaded)
		engine.loadEcoreFile("model/My.ecore");
		// Execute the actual transformations defined in the DSL
		engine.execute();
		// Save the modified Ecores and models into a new path
		engine.save("modified");

		// load the modified file
		AbstractEdelta verifier = new EdeltaDefaultRuntime();
		verifier.loadEcoreFile("modified/My.ecore");
		// verify the structure of the modified ecore
		EPackage ePackage = verifier.getEPackage("myecore");
		EClass myNewClass = (EClass) ePackage.getEClassifier("MyNewClass");
		EAttribute myNewAttribute = (EAttribute) myNewClass.getEStructuralFeature("ANewAttribute");
		assertTrue(myNewAttribute.isRequired());
		assertSame(EcorePackage.Literals.EINT, myNewAttribute.getEAttributeType());
		EClass aNewDerivedClass = (EClass) ePackage.getEClassifier("ANewDerivedEClass");
		assertSame(verifier.getEClass("myecore", "MyEClass"), aNewDerivedClass.getESuperTypes().get(0));
		assertTrue(aNewDerivedClass.isAbstract());
	}
}
