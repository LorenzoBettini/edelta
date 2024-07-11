package edelta.util;

import static edelta.edelta.EdeltaPackage.Literals.*;
import static org.eclipse.xtext.EcoreUtil2.getContainerOfType;
import static org.eclipse.xtext.nodemodel.util.NodeModelUtils.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.xtext.nodemodel.INode;
import org.eclipse.xtext.xbase.XBlockExpression;
import org.eclipse.xtext.xbase.XExpression;

import edelta.edelta.EdeltaEcoreReference;
import edelta.edelta.EdeltaEcoreReferenceExpression;
import edelta.edelta.EdeltaMigration;
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

	public static List<EPackage> getMetamodels(final EObject context) {
		return getMetamodels(getProgram(context));
	}

	public static List<EPackage> getMetamodels(EdeltaProgram p) {
		return Stream.concat(
					p.getEPackages().stream(),
					p.getMigrations().stream()
						.map(EdeltaMigration::getNsURI))
				.toList();
	}

	public static String getEcoreReferenceText(EdeltaEcoreReference ref) {
		return getTokenText(findActualNodeFor(ref));
	}

	public static String getMetamodelImportText(EdeltaProgram p, int index) {
		return getTokenText(
			getMetamodelImportNodes(p).get(index));
	}

	public static List<INode> getMetamodelImportNodes(EdeltaProgram p) {
		return findNodesForFeature(p, EDELTA_PROGRAM__EPACKAGES);
	}

	public static boolean hasCycleInSuperPackage(EPackage ePackage) {
		Set<EPackage> seen = new HashSet<>();
		var superPackage = ePackage.getESuperPackage();
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
		var superPackage = ePackage.getESuperPackage();
		if (superPackage == null)
			return null;
		while (superPackage.getESuperPackage() != null) {
			superPackage = superPackage.getESuperPackage();
		}
		return superPackage;
	}

	public static boolean hasCycleInHierarchy(EClass type) {
		return hasCycleInHierarchy(type, new HashSet<>());
	}

	private static boolean hasCycleInHierarchy(EClass type, Set<EClass> processedSuperTypes) {
		processedSuperTypes.add(type);
		for (EClass superType : type.getESuperTypes()) {
			if (processedSuperTypes.contains(superType) ||
					hasCycleInHierarchy(superType, processedSuperTypes)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Returns the containing {@link XExpression} that is contained directly in an
	 * {@link XBlockExpression}.
	 * 
	 * @param reference
	 * @return
	 */
	public static XExpression getContainingBlockXExpression(EdeltaEcoreReference reference) {
		return getContainingBlockXExpression(
			getContainerOfType(reference, EdeltaEcoreReferenceExpression.class));
	}

	/**
	 * Returns the containing {@link XExpression} that is contained directly in an
	 * {@link XBlockExpression}.
	 * 
	 * @param reference
	 * @return
	 */
	public static XExpression getContainingBlockXExpression(XExpression exp) {
		var blockExp = exp;
		var container = blockExp.eContainer();
		while (!(container instanceof XBlockExpression)) {
			blockExp = (XExpression) container;
			container = container.eContainer();
		}
		return blockExp;
	}
}
