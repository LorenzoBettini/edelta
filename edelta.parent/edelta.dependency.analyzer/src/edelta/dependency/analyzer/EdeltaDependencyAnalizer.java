package edelta.dependency.analyzer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;

import GraphMM.Dependency;
import GraphMM.GraphMMFactory;
import GraphMM.Metamodel;
import GraphMM.Repository;
import edelta.lib.AbstractEdelta;
import edelta.lib.EdeltaLibrary;

public class EdeltaDependencyAnalizer extends AbstractEdelta {

	private static final GraphMMFactory graphFactory = GraphMMFactory.eINSTANCE;

	/**
	 * Analyzes the dependencies of the specified {@link EPackage} (by name) together
	 * with all the other {@link EPackage}s found in the specified path.
	 * 
	 * @param path
	 * @param packageName
	 * @return
	 * @throws IOException
	 */
	public Repository analyzeEPackage(String path, String packageName) throws IOException {
		try (var stream = Files.walk(Paths.get(path))) {
			var resources = loadEcoreFiles(stream);
			var packages = getEPackages(resources);
			var packageToAnalyze = getEPackageByName(packages, packageName);
			packages.remove(packageToAnalyze);
			return analyzeEPackages(packageToAnalyze, packages);
		}
	}

	private EPackage getEPackageByName(List<EPackage> packages, String packageName) {
		return packages.stream()
			.filter(p -> packageName.equals(p.getName()))
			.findFirst()
			.orElseThrow(
				() -> new IllegalArgumentException
					("No EPackage with name: " + packageName));
	}

	private List<EPackage> getEPackages(List<Resource> resources) {
		return resources.stream()
			.map(r -> (EPackage) r.getContents().get(0))
			.collect(Collectors.toList());
	}

	private List<Resource> loadEcoreFiles(Stream<Path> stream) {
		return stream
			.filter(file -> !Files.isDirectory(file))
			.filter(file -> file.toString().endsWith(".ecore"))
			.map(file -> loadEcoreFile(file.toString()))
			.collect(Collectors.toList());
	}

	private Repository analyzeEPackages(EPackage ePackage, List<EPackage> packages) {
		var seen = new HashSet<EPackage>();
		var repository = analyzeMainEPackage(ePackage, seen);
		for (var p : packages) {
			analyzeEPackage(repository, p, seen);
		}
		return repository;
	}

	/**
	 * Analyzes the dependencies of the specified {@link EPackage}.
	 * 
	 * @param ePackage
	 * @return
	 */
	public Repository analyzeMainEPackage(EPackage ePackage) {
		return analyzeMainEPackage(ePackage, new HashSet<>());
	}

	private Repository analyzeMainEPackage(EPackage ePackage, Set<EPackage> seen) {
		var repository = graphFactory.createRepository();
		createGraphMetamodelHighlighted(repository, ePackage);
		analyzeEPackage(repository, ePackage, seen);
		return repository;
	}

	private void analyzeEPackage(Repository repository, EPackage current, Set<EPackage> seen) {
		if (seen.contains(current))
			return;
		seen.add(current);
		var metamodel = createGraphMetamodel(repository, current);
		var usedPackages = EdeltaLibrary.usedPackages(current);
		for (var used : usedPackages) {
			var usedMetamodel = createGraphMetamodel(repository, used);
			findDependencyWithTarget(repository, current)
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

	private Optional<Dependency> findDependencyWithTarget(Repository repository, EPackage current) {
		return repository.getEdges().stream()
			.filter(Dependency.class::isInstance)
			.map(Dependency.class::cast)
			.filter(d -> ((Metamodel) d.getTrg()).getNsURI().equals(current.getNsURI()))
			.findFirst();
	}

	private Dependency createDependency(Repository repository, Metamodel source, Metamodel target) {
		var dependency = graphFactory.createDependency();
		dependency.setSrc(source);
		dependency.setTrg(target);
		repository.getEdges().add(dependency);
		return dependency;
	}

}
