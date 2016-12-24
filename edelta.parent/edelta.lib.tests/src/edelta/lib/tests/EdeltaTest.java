/**
 * 
 */
package edelta.lib.tests;

import java.net.URL;

import org.eclipse.emf.common.util.WrappedException;
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
		loadTestEcore();
	}

	@Test(expected=WrappedException.class)
	public void testLoadNonExistantEcoreFile() {
		edelta.loadEcoreFile("foo.ecore");
	}

	private void loadTestEcore() {
		URL url = getClass().getClassLoader().getResource("My.ecore");
		edelta.loadEcoreFile(url.getPath());
	}
}
