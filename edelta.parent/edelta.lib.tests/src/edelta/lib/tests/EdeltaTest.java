/**
 * 
 */
package edelta.lib.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.eclipse.emf.common.util.WrappedException;
import org.eclipse.emf.ecore.EPackage;
import org.junit.Before;
import org.junit.Test;

import edelta.lib.AbstractEdelta;

/**
 * Tests for the base class of generated Edelta programs.
 * 
 * @author Lorenzo Bettini
 *
 */
public class EdeltaTest {

	private AbstractEdelta edelta;

	@Before
	public void init() {
		edelta = new AbstractEdelta() {
		};
	}

	@Test
	public void testLoadEcoreFile() {
		loadTestEcore("My.ecore");
	}

	@Test(expected=WrappedException.class)
	public void testLoadNonExistantEcoreFile() {
		edelta.loadEcoreFile("foo.ecore");
	}

	@Test
	public void testGetEPackage() {
		loadTestEcore("My.ecore");
		loadTestEcore("My2.ecore");
		EPackage ePackage = edelta.getEPackage("mypackage");
		assertEquals("mypackage", ePackage.getName());
		assertNotNull(ePackage);
		assertNotNull(edelta.getEPackage("myotherpackage"));
		assertNull(edelta.getEPackage("foo"));
	}

	@Test
	public void testGetEClassifier() {
		loadTestEcore("My.ecore");
		loadTestEcore("My2.ecore");
		assertNotNull(edelta.getEClassifier("mypackage", "MyClass"));
		assertNotNull(edelta.getEClassifier("mypackage", "MyDataType"));
		// wrong package
		assertNull(edelta.getEClassifier("myotherpackage", "MyDataType"));
		// package does not exist
		assertNull(edelta.getEClassifier("foo", "MyDataType"));
	}

	@Test
	public void testGetEClass() {
		loadTestEcore("My.ecore");
		assertNotNull(edelta.getEClass("mypackage", "MyClass"));
		assertNull(edelta.getEClass("mypackage", "MyDataType"));
	}

	@Test
	public void testGetEDataType() {
		loadTestEcore("My.ecore");
		assertNull(edelta.getEDataType("mypackage", "MyClass"));
		assertNotNull(edelta.getEDataType("mypackage", "MyDataType"));
	}

	private void loadTestEcore(String ecoreFile) {
		edelta.loadEcoreFile("testecores/"+ecoreFile);
	}
}
