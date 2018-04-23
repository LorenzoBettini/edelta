/**
 * 
 */
package edelta.lib;

import java.util.function.Consumer;

import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EEnum;
import org.eclipse.emf.ecore.EEnumLiteral;
import org.eclipse.emf.ecore.ENamedElement;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EcoreFactory;

/**
 * Library functions to be reused in Edelta programs.
 * 
 * @author Lorenzo Bettini
 *
 */
public class EdeltaLibrary {

	private EcoreFactory ecoreFactory = EcoreFactory.eINSTANCE;

	public EClass newEClass(String name) {
		return newEClass(name, null);
	}

	public EClass newEClass(String name, Consumer<EClass> initializer) {
		EClass c = ecoreFactory.createEClass();
		c.setName(name);
		if (initializer != null) {
			initializer.accept(c);
		}
		return c;
	}

	public EAttribute newEAttribute(String name) {
		return newEAttribute(name, null);
	}

	public EAttribute newEAttribute(String name, Consumer<EAttribute> initiaizer) {
		EAttribute e = ecoreFactory.createEAttribute();
		e.setName(name);
		if (initiaizer != null) {
			initiaizer.accept(e);
		}
		return e;
	}

	public EReference newEReference(String name) {
		return newEReference(name, null);
	}

	public EReference newEReference(String name, Consumer<EReference> initiaizer) {
		EReference e = ecoreFactory.createEReference();
		e.setName(name);
		if (initiaizer != null) {
			initiaizer.accept(e);
		}
		return e;
	}

	public EEnum newEEnum(String name) {
		return newEEnum(name, null);
	}

	public EEnum newEEnum(String name, Consumer<EEnum> initiaizer) {
		EEnum e = ecoreFactory.createEEnum();
		e.setName(name);
		if (initiaizer != null) {
			initiaizer.accept(e);
		}
		return e;
	}

	public EEnumLiteral newEEnumLiteral(String name) {
		return newEEnumLiteral(name, null);
	}

	public EEnumLiteral newEEnumLiteral(String name, Consumer<EEnumLiteral> initiaizer) {
		EEnumLiteral e = ecoreFactory.createEEnumLiteral();
		e.setName(name);
		if (initiaizer != null) {
			initiaizer.accept(e);
		}
		return e;
	}

	/**
	 * Returns a String representation based on the containment relation.
	 * 
	 * @param e
	 * @return
	 */
	public String EObjectToString(EObject e) {
		String info = e.toString();
		if (e instanceof ENamedElement) {
			info = ((ENamedElement) e).getName();
		}
		return e.eContainer() != null ? EObjectToString(e.eContainer()) + ":" + info : info;
	}
}
