/**
 * 
 */
package edelta.lib.tests;

import java.net.URL;

import org.eclipse.emf.common.util.WrappedException;
import org.junit.Assert;
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
		Assert.assertNotNull(edelta.getEPackage("mypackage"));
		Assert.assertNotNull(edelta.getEPackage("myotherpackage"));
		Assert.assertNull(edelta.getEPackage("foo"));
	}

	private void loadTestEcore(String ecoreFile) {
		URL url = getClass().getClassLoader().getResource(ecoreFile);
		edelta.loadEcoreFile("testecores/"+ecoreFile);
	}
}
