package edelta.dependency.analyzer;

import org.eclipse.emf.ecore.EPackage;

import GraphMM.Dependency;
import GraphMM.GraphMMFactory;
import GraphMM.Metamodel;
import GraphMM.Repository;
import edelta.lib.AbstractEdelta;
import edelta.lib.EdeltaLibrary;

public class EdeltaDependencyAnalizer extends AbstractEdelta {

	private static final GraphMMFactory graphFactory = GraphMMFactory.eINSTANCE;

	public Repository analyzeEPackage(EPackage ePackage) {
		var repository = graphFactory.createRepository();
		analyzeEPackage(repository, ePackage);
		return repository;
	}

	private void analyzeEPackage(Repository repository, EPackage ePackage) {
		var metamodel = createGraphMetamodelHighlighted(repository, ePackage);
		var usedPackages = EdeltaLibrary.usedPackages(ePackage);
		for (var used : usedPackages) {
			var usedMetamodel = createGraphMetamodel(repository, used);
			var dependency = createDependency(repository, metamodel, usedMetamodel);
			var secondLevel = EdeltaLibrary.usedPackages(used);
			if (secondLevel.contains(ePackage)) {
				dependency.setBidirectional(true);
			}
		}
	}

	private Metamodel createGraphMetamodelHighlighted(Repository repository, EPackage ePackage) {
		var metamodel = createGraphMetamodel(repository, ePackage);
		metamodel.setHighlighted(true);
		return metamodel;
	}

	private Dependency createDependency(Repository repository, Metamodel source, Metamodel target) {
		var dependency = graphFactory.createDependency();
		dependency.setSrc(source);
		dependency.setTrg(target);
		repository.getEdges().add(dependency);
		return dependency;
	}

	private Metamodel createGraphMetamodel(Repository repository, EPackage ePackage) {
		var metamodel = createGraphMetamodel(ePackage);
		repository.getNodes().add(metamodel);
		return metamodel;
	}

	private Metamodel createGraphMetamodel(EPackage ePackage) {
		var metamodel = graphFactory.createMetamodel();
		metamodel.setName(ePackage.getName());
		metamodel.setNsURI(ePackage.getNsURI());
		return metamodel;
	}

}
