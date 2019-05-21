package example.example;

import org.eclipse.emf.ecore.EcorePackage;

import edelta.lib.AbstractEdelta;

/**
 * Hello world!
 *
 */
public class App {
	public static void main(String[] args) throws Exception {
		AbstractEdelta edelta = new AbstractEdelta() {
		};
		edelta.execute();
		System.out.println(EcorePackage.Literals.ECLASS);
		System.out.println("Hello World!");
	}
}
