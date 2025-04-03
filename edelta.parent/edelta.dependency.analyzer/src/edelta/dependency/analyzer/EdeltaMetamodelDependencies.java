package edelta.dependency.analyzer;

import java.util.List;

import GraphMM.Metamodel;

/**
 * Represents a {@link Metamodel} and all the other {@link Metamodel}s that are
 * its dependencies (references or incoming references).
 *
 * @author Lorenzo Bettini
 *
 */
public class EdeltaMetamodelDependencies {

	private Metamodel highlighted;
	private List<Metamodel> dependencies;

	public EdeltaMetamodelDependencies(Metamodel highlighted, List<Metamodel> dependencies) {
		this.highlighted = highlighted;
		this.dependencies = dependencies;
	}

	public Metamodel getHighlighted() {
		return highlighted;
	}

	public List<Metamodel> getDependencies() {
		return dependencies;
	}
}
