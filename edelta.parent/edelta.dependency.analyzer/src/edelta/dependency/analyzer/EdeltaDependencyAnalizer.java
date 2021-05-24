package edelta.dependency.analyzer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.emf.ecore.EPackage;

import GraphMM.Dependency;
import GraphMM.GraphMMFactory;
import GraphMM.Metamodel;
import GraphMM.Repository;
import edelta.lib.AbstractEdelta;
import edelta.lib.EdeltaLibrary;

public class EdeltaDependencyAnalizer extends AbstractEdelta {

	private static final GraphMMFactory graphFactory = GraphMMFactory.eINSTANCE;

	public Repository analyzeEPackage(String path, String packageName) throws IOException {
		try (var stream = Files.walk(Paths.get(path))) {
			var resources = stream
				.filter(
					file -> !Files.isDirectory(file))
				.filter(
					file -> file.toString().endsWith(".ecore"))
				.map(file -> 
					loadEcoreFile(file.toString()))
				.collect(Collectors.toList());
			var packages = resources.stream()
				.map(r -> (EPackage) r.getContents().get(0))
				.collect(Collectors.toList());
			var packageToAnalyze = packages.stream()
				.filter(p -> packageName.equals(p.getName()))
				.findFirst()
				.orElseThrow(
					() -> new IllegalArgumentException
						("No EPackage with name: " + packageName));
			packages.remove(packageToAnalyze);
			return analyzeEPackages(packageToAnalyze, packages);
		}
	}

	private Repository analyzeEPackages(EPackage ePackage, List<EPackage> packages) {
		var seen = new HashSet<EPackage>();
		var repository = analyzeMainEPackage(ePackage, seen);
		for (var p : packages) {
			analyzeEPackage(repository, p, seen);
		}
		return repository;
	}

	public Repository analyzeMainEPackage(EPackage ePackage) {
		return analyzeMainEPackage(ePackage, new HashSet<>());
	}

	private Repository analyzeMainEPackage(EPackage ePackage, Set<EPackage> seen) {
		var repository = graphFactory.createRepository();
		createGraphMetamodelHighlighted(repository, ePackage);
		analyzeEPackage(repository, ePackage, seen);
		return repository;
	}

	private void analyzeEPackage(Repository repository, EPackage ePackage, Set<EPackage> seen) {
		if (seen.contains(ePackage))
			return;
		seen.add(ePackage);
		var metamodel = createGraphMetamodel(repository, ePackage);
		var usedPackages = EdeltaLibrary.usedPackages(ePackage);
		for (var used : usedPackages) {
			var usedMetamodel = createGraphMetamodel(repository, used);
			repository.getEdges().stream()
				.filter(Dependency.class::isInstance)
				.map(Dependency.class::cast)
				.filter(d -> ((Metamodel) d.getTrg()).getNsURI().equals(ePackage.getNsURI()))
				.findFirst()
				.ifPresentOrElse(
					d -> d.setBidirectional(true),
					() -> createDependency(repository, metamodel, usedMetamodel));
			analyzeEPackage(repository, used, seen);
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
		return repository.getNodes().stream()
			.filter(Metamodel.class::isInstance)
			.map(Metamodel.class::cast)
			.filter(m -> m.getNsURI().equals(ePackage.getNsURI()))
			.findFirst()
			.orElseGet(() -> {
				var metamodel = createGraphMetamodel(ePackage);
				repository.getNodes().add(metamodel);
				return metamodel;
			});
	}

	private Metamodel createGraphMetamodel(EPackage ePackage) {
		var metamodel = graphFactory.createMetamodel();
		metamodel.setName(ePackage.getName());
		metamodel.setNsURI(ePackage.getNsURI());
		return metamodel;
	}

}
