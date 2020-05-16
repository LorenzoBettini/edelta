package edelta.util;

import static edelta.edelta.EdeltaPackage.Literals.EDELTA_PROGRAM__METAMODELS;
import static org.eclipse.xtext.EcoreUtil2.getContainerOfType;
import static org.eclipse.xtext.nodemodel.util.NodeModelUtils.*;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;

import edelta.edelta.EdeltaEcoreReference;
import edelta.edelta.EdeltaProgram;

/**
 * Some utilities for navigating an Edelta AST model and Ecore model.
 * 
 * @author Lorenzo Bettini
 */
public class EdeltaModelUtil {

	private EdeltaModelUtil() {
	}

	public static EdeltaProgram getProgram(final EObject context) {
		return getContainerOfType(context, EdeltaProgram.class);
	}

	public static String getEcoreReferenceText(EdeltaEcoreReference ref) {
		return getTokenText(findActualNodeFor(ref));
	}

	public static String getMetamodelImportText(EdeltaProgram p, int index) {
		return getTokenText(
			findNodesForFeature(p, EDELTA_PROGRAM__METAMODELS).get(index));
	}

	public static boolean hasCycleInSuperPackage(EPackage ePackage) {
		Set<EPackage> seen = new HashSet<>();
		EPackage superPackage = ePackage.getESuperPackage();
		while (superPackage != null) {
			if (seen.contains(superPackage))
				return true;
			seen.add(superPackage);
			superPackage = superPackage.getESuperPackage();
		}
		return false;
	}

	/**
	 * This assumes that the super package relation contains no cycle, so it should
	 * be called only on {@link EPackage}s read from an Ecore file, not on EPackages
	 * that can be modified by the interpreter.
	 * 
	 * @param ePackage
	 * @return
	 */
	public static EPackage findRootSuperPackage(EPackage ePackage) {
		EPackage superPackage = ePackage.getESuperPackage();
		if (superPackage == null)
			return null;
		while (superPackage.getESuperPackage() != null) {
			superPackage = superPackage.getESuperPackage();
		}
		return superPackage;
	}
}
