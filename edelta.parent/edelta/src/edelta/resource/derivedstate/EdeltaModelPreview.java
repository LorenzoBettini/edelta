package edelta.resource.derivedstate;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.xtext.xbase.XExpression;

import edelta.edelta.EdeltaTestModelPath;
import edelta.resource.EdeltaModelSourceSelector;

/**
 * Final in-memory model resources owned by one installation of derived state.
 * Returned resources are valid only until that derived state is discarded.
 *
 * @author Lorenzo Bettini
 */
public class EdeltaModelPreview {
	/** A final model resource and its selected input declaration. */
	public record Model(Path sourcePath, EdeltaTestModelPath declaration, Resource resource) {}

	/** A preview failure retained for later source-diagnostic presentation. */
	public record Failure(String code, EObject source, Path path, Throwable cause,
			String objectContext) {
		/** Creates a failure without model-object context. */
		public Failure(String code, EObject source, Path path, Throwable cause) {
			this(code, source, path, cause, null);
		}
	}

	private List<Model> models = List.of();
	private final List<Failure> failures = new ArrayList<>();
	private ResourceSet finalResourceSet;
	private final Map<EObject, Map<EAttribute, XExpression>> migrationExpressions =
		new IdentityHashMap<>();

	/**
	 * Associates final resources with their selected sources. Migration stages
	 * create exactly one resource per input in input order; URI equality is an
	 * additional guard against a future change in that order.
	 *
	 * @param selected the original resources in selection order
	 * @param evolved the final resources in migration order
	 * @return the final models with their original declarations
	 */
	public static List<Model> associate(List<EdeltaModelSourceSelector.Selected> selected,
			List<Resource> evolved) {
		if (selected.size() != evolved.size()) {
			throw new IllegalStateException("Preview model resource count changed during migration");
		}
		var models = new ArrayList<Model>();
		for (var i = 0; i < selected.size(); i++) {
			var source = selected.get(i);
			var resource = evolved.get(i);
			if (!Objects.equals(source.resource().getURI(), resource.getURI())) {
				throw new IllegalStateException("Preview model resource order changed during migration");
			}
			models.add(new Model(source.path(), source.entry(), resource));
		}
		return List.copyOf(models);
	}

	/**
	 * Returns the final models in declaration and selection order. The resources
	 * become invalid when the program's derived state is discarded.
	 *
	 * @return an immutable snapshot of final models
	 */
	public List<Model> getModels() {
		return models;
	}

	/** @return an immutable snapshot of structured preview failures */
	public List<Failure> getFailures() {
		return List.copyOf(failures);
	}

	/** @return whether a failure prevents a complete preview */
	public boolean hasFailures() {
		return !failures.isEmpty();
	}

	/**
	 * Records a failure without exposing a partial model preview.
	 *
	 * @param failure the structured failure
	 */
	public void addFailure(Failure failure) {
		failures.add(failure);
	}

	/**
	 * Takes ownership of a nonempty set of final resources after migration.
	 * All resources must belong to the migrator's private final resource set.
	 *
	 * @param models final resources paired with their source declarations
	 */
	public void setModels(List<Model> models) {
		if (hasFailures()) {
			throw new IllegalStateException("Cannot publish a failed model preview");
		}
		this.models = List.copyOf(models);
		finalResourceSet = models.getFirst().resource().getResourceSet();
	}

	/**
	 * Publishes exact final-instance attribute expressions after a successful copy.
	 * Intermediate copier objects are not valid outline owners. For example,
	 * an attribute token from the uppercase stage is stored only under the
	 * corresponding object and attribute from the final preview stage.
	 *
	 * @param expressions final object and attribute identities mapped to user expressions
	 */
	public void setMigrationExpressions(Map<EObject, Map<EAttribute, XExpression>> expressions) {
		if (hasFailures() || finalResourceSet == null)
			throw new IllegalStateException("Cannot publish migration expressions without final models");
		migrationExpressions.clear();
		for (var entry : expressions.entrySet()) {
			if (entry.getKey().eResource() != null &&
					entry.getKey().eResource().getResourceSet() == finalResourceSet)
				migrationExpressions.put(entry.getKey(), new IdentityHashMap<>(entry.getValue()));
		}
	}

	/**
	 * Returns a successful source expression for this exact final object and attribute.
	 * An object from an earlier preview or a different resource set has no link.
	 *
	 * @param object the final preview object
	 * @param attribute its final EAttribute
	 * @return the last applied user expression, or {@code null}
	 */
	public XExpression getMigrationExpression(EObject object, EAttribute attribute) {
		if (object == null || object.eResource() == null ||
				object.eResource().getResourceSet() != finalResourceSet) return null;
		var entries = migrationExpressions.get(object);
		return entries == null ? null : entries.get(attribute);
	}

	/** Unloads the private final resource set and drops all preview references. */
	public void clear() {
		if (finalResourceSet != null) {
			for (var resource : List.copyOf(finalResourceSet.getResources())) {
				resource.unload();
			}
			finalResourceSet.getResources().clear();
		}
		models = List.of();
		migrationExpressions.clear();
		failures.clear();
		finalResourceSet = null;
	}
}
