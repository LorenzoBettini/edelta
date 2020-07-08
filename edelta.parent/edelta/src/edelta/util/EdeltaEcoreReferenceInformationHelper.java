package edelta.util;

import static edelta.util.EdeltaModelUtil.hasCycleInSuperPackage;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EEnum;
import org.eclipse.emf.ecore.EEnumLiteral;
import org.eclipse.emf.ecore.ENamedElement;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.util.EcoreSwitch;
import org.eclipse.xtext.naming.IQualifiedNameProvider;
import org.eclipse.xtext.xbase.typesystem.IBatchTypeResolver;

import com.google.inject.Inject;

import edelta.edelta.EdeltaEcoreReference;
import edelta.edelta.EdeltaEcoreReferenceExpression;
import edelta.resource.derivedstate.EdeltaDerivedStateHelper;
import edelta.resource.derivedstate.EdeltaEcoreReferenceState;
import edelta.resource.derivedstate.EdeltaEcoreReferenceState.EdeltaEcoreReferenceStateInformation;

/**
 * Utilities for an ecore reference information
 * 
 * @author Lorenzo Bettini
 */
public class EdeltaEcoreReferenceInformationHelper {
	@Inject
	private IBatchTypeResolver typeResolver;

	@Inject
	private IQualifiedNameProvider qualifiedNameProvider;

	@Inject
	private EdeltaDerivedStateHelper derivedStateHelper;

	public EdeltaEcoreReferenceState.EdeltaEcoreReferenceStateInformation getOrComputeInformation(
			final EdeltaEcoreReferenceExpression exp) {
		final EdeltaEcoreReference reference = exp.getReference();
		final EdeltaEcoreReferenceState ecoreReferenceState =
			derivedStateHelper.getEcoreReferenceState(reference);
		var existing = ecoreReferenceState.getInformation();
		if (existing != null) {
			return existing;
		}
		final var info = new EdeltaEcoreReferenceStateInformation();
		ecoreReferenceState.setInformation(info);
		final var type = typeResolver.resolveTypes(exp).getActualType(exp);
		info.setType(type.getSimpleName());
		final var element = reference.getEnamedelement();

		new EcoreSwitch<Void>() {
			@Override
			public Void caseEPackage(final EPackage object) {
				info.setEPackageName(EdeltaEcoreReferenceInformationHelper.this.nameOrEmpty(object));
				return null;
			}

			@Override
			public Void caseEClassifier(final EClassifier object) {
				info.setEPackageName(EdeltaEcoreReferenceInformationHelper.this.nameOrEmpty(object.getEPackage()));
				info.setEClassifierName(EdeltaEcoreReferenceInformationHelper.this.nameOrEmpty(object));
				return null;
			}

			@Override
			public Void caseEEnumLiteral(final EEnumLiteral object) {
				final EEnum eEnum = object.getEEnum();
				info.setEPackageName(EdeltaEcoreReferenceInformationHelper.this
						.nameOrEmpty(EdeltaEcoreReferenceInformationHelper.this.getEPackageOrNull(eEnum)));
				info.setEClassifierName(EdeltaEcoreReferenceInformationHelper.this.nameOrEmpty(eEnum));
				info.setENamedElementName(EdeltaEcoreReferenceInformationHelper.this.nameOrEmpty(element));
				return null;
			}

			/**
			 * An unresolved proxy is of type EAttribute so we include it in this case.
			 */
			@Override
			public Void caseEStructuralFeature(final EStructuralFeature object) {
				final EClass c = object.getEContainingClass();
				info.setEPackageName(EdeltaEcoreReferenceInformationHelper.this
						.nameOrEmpty(EdeltaEcoreReferenceInformationHelper.this.getEPackageOrNull(c)));
				info.setEClassifierName(EdeltaEcoreReferenceInformationHelper.this.nameOrEmpty(c));
				info.setENamedElementName(EdeltaEcoreReferenceInformationHelper.this.nameOrEmpty(element));
				return null;
			}
		}.doSwitch(element);

		return info;
	}

	private EPackage getEPackageOrNull(final EClassifier e) {
		if (e != null) {
			return e.getEPackage();
		}
		return null;
	}

	private String nameOrEmpty(final EPackage e) {
		if ((e == null)) {
			return "";
		}
		if (hasCycleInSuperPackage(e)) {
			return e.getName();
		}
		return qualifiedNameProvider.getFullyQualifiedName(e).toString();
	}

	private String nameOrEmpty(final ENamedElement e) {
		String name = null;
		if (e != null) {
			name = e.getName();
		}
		if (name != null) {
			return name;
		} else {
			return "";
		}
	}
}
