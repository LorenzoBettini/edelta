package edelta.tests;

import static org.junit.Assert.assertEquals;

import org.eclipse.emf.ecore.EcoreFactory;
import org.eclipse.xtext.naming.IQualifiedNameProvider;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.XtextRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.inject.Inject;

@RunWith(XtextRunner.class)
@InjectWith(EdeltaInjectorProvider.class)
public class EdeltaQualifiedNameProviderTest extends EdeltaAbstractTest {
	@Inject
	private IQualifiedNameProvider qualifiedNameProvider;

	@Test
	public void testProgramWithoutPackage() throws Exception {
		assertEquals("edelta.__synthetic0",
				qualifiedNameProvider.getFullyQualifiedName(parseHelper.parse("")).toString());
	}

	@Test
	public void testProgramWithPackage() throws Exception {
		assertEquals("foo.__synthetic0",
			qualifiedNameProvider.getFullyQualifiedName(
				parseHelper.parse("package foo")).toString());
	}

	@Test
	public void testEPackageWithCycle() {
		var p1 = EcoreFactory.eINSTANCE.createEPackage();
		p1.setName("p1");
		var p2 = EcoreFactory.eINSTANCE.createEPackage();
		p2.setName("p2");
		p1.getESubpackages().add(p2);
		assertEquals("p1.p2",
			qualifiedNameProvider.getFullyQualifiedName(p2).toString());
		p2.getESubpackages().add(p1);
		assertEquals("p2",
			qualifiedNameProvider.getFullyQualifiedName(p2).toString());
	}

	@Test
	public void testAnyOtherElement() {
		var c = EcoreFactory.eINSTANCE.createEClass();
		c.setName("foo");
		assertEquals("foo",
			qualifiedNameProvider.getFullyQualifiedName(c).toString());
	}
}
