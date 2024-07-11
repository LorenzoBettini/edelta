/**
 * generated by Xtext 2.10.0
 */
package edelta.validation;

import static com.google.common.collect.Iterables.filter;
import static edelta.edelta.EdeltaPackage.Literals.*;
import static org.eclipse.xtext.xbase.lib.IteratorExtensions.head;
import static org.eclipse.xtext.xbase.typesystem.util.Multimaps2.newLinkedHashListMultimap;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.common.types.JvmGenericType;
import org.eclipse.xtext.common.types.JvmOperation;
import org.eclipse.xtext.common.types.JvmTypeReference;
import org.eclipse.xtext.naming.IQualifiedNameProvider;
import org.eclipse.xtext.validation.Check;
import org.eclipse.xtext.xbase.XAbstractFeatureCall;
import org.eclipse.xtext.xbase.jvmmodel.IJvmModelAssociations;
import org.eclipse.xtext.xbase.typesystem.override.OverrideHelper;
import org.eclipse.xtext.xbase.typesystem.references.LightweightTypeReference;
import org.eclipse.xtext.xbase.typesystem.references.StandardTypeReferenceOwner;
import org.eclipse.xtext.xbase.typesystem.util.CommonTypeComputationServices;

import com.google.common.collect.ListMultimap;
import com.google.inject.Inject;

import edelta.edelta.EdeltaEcoreReferenceExpression;
import edelta.edelta.EdeltaMigration;
import edelta.edelta.EdeltaModifyEcoreOperation;
import edelta.edelta.EdeltaProgram;
import edelta.edelta.EdeltaUseAs;
import edelta.lib.EdeltaRuntime;
import edelta.resource.derivedstate.EdeltaDerivedStateHelper;
import edelta.util.EdeltaModelUtil;

/**
 * This class contains custom validation rules.
 * 
 * See
 * https://www.eclipse.org/Xtext/documentation/303_runtime_concepts.html#validation
 */
public class EdeltaValidator extends AbstractEdeltaValidator {
	public static final String PREFIX = "edelta.";

	public static final String TYPE_MISMATCH = PREFIX + "TypeMismatch";

	public static final String INTERPRETER_TIMEOUT = PREFIX + "InterpreterTimeout";

	public static final String INTERPRETER_ACCESS_REMOVED_ELEMENT = PREFIX
			+ "InterpreterAccessRemovedElement";

	public static final String INTERPRETER_ACCESS_RENAMED_ELEMENT = PREFIX
			+ "InterpreterAccessRenamedElement";

	public static final String INTERPRETER_ACCESS_NOT_YET_EXISTING_ELEMENT = PREFIX
			+ "InterpreterAccessNotYetExistingElement";

	public static final String DUPLICATE_DECLARATION = PREFIX + "DuplicateDeclaration";

	public static final String DUPLICATE_METAMODEL_IMPORT = PREFIX + "DuplicateMetamodelImport";

	public static final String DUPLICATE_EPACKAGE_IN_MIGRATE = PREFIX + "DuplicateEPackageInMigrate";

	public static final String INVALID_SUBPACKAGE_IMPORT = PREFIX + "InvalidSubPackageImport";

	public static final String INVALID_SUBPACKAGE_MODIFICATION = PREFIX
			+ "InvalidSubPackageModification";

	public static final String AMBIGUOUS_REFERENCE = PREFIX + "AmbiguousReference";

	public static final String LIVE_VALIDATION_ERROR = PREFIX + "LiveValidationError";

	public static final String LIVE_VALIDATION_WARNING = PREFIX + "LiveValidationWarning";

	public static final String EPACKAGE_CYCLE = PREFIX + "EPackageCycle";

	public static final String ECLASS_CYCLE = PREFIX + "EClassCycle";

	public static final String INVALID_ECOREREF_USAGE = PREFIX
			+ "InvalidEcoreRefUsage";

	public static final String INVALID_NS_URI = PREFIX
			+ "InvalidNsURI";

	@Inject
	private CommonTypeComputationServices services;

	@Inject
	private OverrideHelper overrideHelper;

	@Inject
	private IJvmModelAssociations jvmModelAssociations;

	@Inject
	private IQualifiedNameProvider qualifiedNameProvider;

	@Inject
	private EdeltaDerivedStateHelper edeltaDerivedStateHelper;

	@Check
	public void checkValidUseAs(EdeltaUseAs useAs) {
		if (!isConformant(useAs, EdeltaRuntime.class, useAs.getType())) {
			error("Not a valid type: must be an " + EdeltaRuntime.class.getName(),
				EDELTA_USE_AS__TYPE,
				TYPE_MISMATCH);
		} else {
			var type = useAs.getType().getType();
			if (type instanceof JvmGenericType genericType && genericType.isAbstract()) {
				// otherwise it's a JvmVoid, which means, unresolved
				// and an error is issued by other validators
				error("Cannot be an abstract type",
					EDELTA_USE_AS__TYPE,
					TYPE_MISMATCH);
			}
		}
	}

	@Check
	public void checkProgram(EdeltaProgram p) {
		var metamodelIndex = 0;
		Set<String> metamodelImportSet = new HashSet<>();
		var metamodels = p.getEPackages();
		for (var metamodel : metamodels) {
			var rootPackage = EdeltaModelUtil.findRootSuperPackage(metamodel);
			if (rootPackage != null) {
				error("Invalid subpackage import \'" + metamodel.getName() + "\'",
					p,
					EDELTA_PROGRAM__EPACKAGES,
					metamodelIndex,
					INVALID_SUBPACKAGE_IMPORT,
					rootPackage.getName() // the fix for the import
				);
			}
			var metamodelImport = EdeltaModelUtil.getMetamodelImportText(p, metamodelIndex);
			if (metamodelImportSet.contains(metamodelImport)) {
				error("Duplicate metamodel import " + metamodelImport,
					p,
					EDELTA_PROGRAM__EPACKAGES,
					metamodelIndex,
					DUPLICATE_METAMODEL_IMPORT,
					"" + metamodelIndex // the fix for the import
				);
			}
			metamodelImportSet.add(metamodelImport);
			metamodelIndex++;
		}
		var migrations = p.getMigrations();
		Set<String> migrationImports = new HashSet<>();
		for (var migration : migrations) {
			var ePackage = migration.getNsURI();
			if (!ePackage.eIsProxy()) {
				var ePackageName = ePackage.getName();
				if (migrationImports.contains(ePackageName)) {
					error(String.format("Duplicate EPackage import with name '%s'", ePackageName),
						migration,
						EDELTA_MIGRATION__NS_URI,
						DUPLICATE_EPACKAGE_IN_MIGRATE
					);
				}
				migrationImports.add(ePackageName);
			}
		}
		var javaClass = head(
			filter(jvmModelAssociations.getJvmElements(p), JvmGenericType.class).iterator());
		var methods = overrideHelper.getResolvedFeatures(javaClass)
				.getDeclaredOperations();
		ListMultimap<String, JvmOperation> map = newLinkedHashListMultimap();
		for (var d : methods) {
			map.put(d.getResolvedErasureSignature(), d.getDeclaration());
		}
		for (var entry : map.asMap().entrySet()) {
			var duplicates = entry.getValue();
			if (duplicates.size() > 1) {
				for (var dup : duplicates) {
					var source = jvmModelAssociations.getPrimarySourceElement(dup);
					error("Duplicate definition \'" + dup.getSimpleName() + "\'",
						source,
						source.eClass().getEStructuralFeature("name"),
						DUPLICATE_DECLARATION);
				}
			}
		}
		var unresolvedEcoreReferences = edeltaDerivedStateHelper
				.getUnresolvedEcoreReferences(p.eResource());
		for (var ecoreRef : unresolvedEcoreReferences) {
			if (!ecoreRef.getEnamedelement().eIsProxy()) {
				// it wasn't resolved during interpretation but it is
				// in the end
				error("Element not yet available in this context: " +
						qualifiedNameProvider
							.getFullyQualifiedName(ecoreRef.getEnamedelement()),
					ecoreRef,
					EDELTA_ECORE_REFERENCE__ENAMEDELEMENT,
					INTERPRETER_ACCESS_NOT_YET_EXISTING_ELEMENT);
			}
		}
	}

	@Check
	public void checkModifyEcore(EdeltaModifyEcoreOperation op) {
		if (EdeltaModelUtil.findRootSuperPackage(op.getEpackage()) != null) {
			error("Invalid direct subpackage modification \'" + op.getEpackage().getName() + "\'",
				op,
				EDELTA_MODIFY_ECORE_OPERATION__EPACKAGE,
				INVALID_SUBPACKAGE_MODIFICATION);
		}
	}

	@Check
	public void checkInvalidUseOfEcorerefInModelMigration(XAbstractFeatureCall call) {
		if (call.getFeature().getSimpleName().equals("modelMigration")) {
			EcoreUtil2.eAllOfType(call, EdeltaEcoreReferenceExpression.class).forEach(ref ->
				error("Invalid use of ecoreref() inside model migration",
					ref,
					null,
					INVALID_ECOREREF_USAGE)
			);
		}
	}

	@Check
	public void checkMigration(EdeltaMigration migration) {
		var toNsURI = migration.getTo();
		if (toNsURI != null) {
			if (toNsURI.isBlank())
				error("Invalid blank nsURI",
					EDELTA_MIGRATION__TO,
					INVALID_NS_URI);
			else if (Objects.equals(migration.getNsURI().getNsURI(), toNsURI))
				error("The nsURI must be different from the original one",
					EDELTA_MIGRATION__TO,
					INVALID_NS_URI);
		}
	}

	private boolean isConformant(EObject context, Class<?> expected, JvmTypeReference actual) {
		return toLightweightTypeReference(actual, context).isSubtypeOf(expected);
	}

	private LightweightTypeReference toLightweightTypeReference(JvmTypeReference typeRef, EObject context) {
		return newTypeReferenceOwner(context).toLightweightTypeReference(typeRef);
	}

	private StandardTypeReferenceOwner newTypeReferenceOwner(EObject context) {
		return new StandardTypeReferenceOwner(services, context);
	}
}
