package edelta.dependency.analyzer;

import org.eclipse.emf.ecore.EPackage;

import GraphMM.GraphMMFactory;
import GraphMM.Metamodel;
import GraphMM.Repository;
import edelta.lib.AbstractEdelta;
import edelta.lib.EdeltaLibrary;

public class EdeltaDependencyAnalizer extends AbstractEdelta {

	private static final GraphMMFactory graphFactory = GraphMMFactory.eINSTANCE;

	public Repository analyzeEPackage(EPackage ePackage) {
		var usedPackages = EdeltaLibrary.usedPackages(ePackage);
		var repository = graphFactory.createRepository();
		var metamodel = createGraphMetamodel(repository, ePackage);
		for (var used : usedPackages) {
			var usedMetamodel = createGraphMetamodel(repository, used);
			createDependency(repository, metamodel, usedMetamodel);
		}
		return repository;
	}

	private void createDependency(Repository repository, Metamodel source, Metamodel target) {
		var dependency = graphFactory.createDependency();
		dependency.setSrc(source);
		dependency.setTrg(target);
		repository.getEdges().add(dependency);
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
