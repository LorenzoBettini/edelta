/**
 * 
 */
package edelta.lib.tests;

import static org.junit.Assert.*;

import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EReference;
import org.junit.Before;
import org.junit.Test;

import edelta.lib.EdeltaLibrary;

/**
 * Library functions for manipulating an Ecore model.
 * 
 * @author Lorenzo Bettini
 *
 */
public class EdeltaLibraryTest {

	private EdeltaLibrary lib;

	@Before
	public void initLib() {
		lib = new EdeltaLibrary();
	}

	@Test
	public void testNewEClass() {
		EClass c = lib.newEClass("test");
		assertEquals("test", c.getName());
	}

	@Test
	public void testNewEClassWithInitializer() {
		EClass c = lib.newEClass("test", cl -> {
			cl.setName("changed");
		});
		assertEquals("changed", c.getName());
	}

	@Test
	public void testNewEAttribute() {
		EAttribute e = lib.newEAttribute("test");
		assertEquals("test", e.getName());
	}

	@Test
	public void testNewEAttributeWithInitializer() {
		EAttribute e = lib.newEAttribute("test", ee -> {
			ee.setName("changed");
		});
		assertEquals("changed", e.getName());
	}

	@Test
	public void testNewEReference() {
		EReference e = lib.newEReference("test");
		assertEquals("test", e.getName());
	}

	@Test
	public void testNewEReferenceWithInitializer() {
		EReference e = lib.newEReference("test", ee -> {
			ee.setName("changed");
		});
		assertEquals("changed", e.getName());
	}

}
