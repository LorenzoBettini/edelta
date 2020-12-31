package edelta.tests;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertSame;

import java.util.Collections;

import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EcoreFactory;
import org.junit.Before;
import org.junit.Test;

import edelta.interpreter.EdeltaInterpreterEdeltaImpl;

public class EdeltaInterpreterEdeltaImplTest extends EdeltaAbstractTest {
	private EdeltaInterpreterEdeltaImpl edelta;

	@Before
	public void setup() {
		edelta = new EdeltaInterpreterEdeltaImpl(Collections.emptyList());
	}

	@Test
	public void testFirstEPackageHasPrecedence() {
		EPackage p1 = EcoreFactory.eINSTANCE.createEPackage();
		p1.setName("Test");
		EPackage p2 = EcoreFactory.eINSTANCE.createEPackage();
		p2.setName("Test");
		edelta = new EdeltaInterpreterEdeltaImpl(asList(p1, p2));
		assertSame(p1, edelta.getEPackage("Test"));
	}
}
