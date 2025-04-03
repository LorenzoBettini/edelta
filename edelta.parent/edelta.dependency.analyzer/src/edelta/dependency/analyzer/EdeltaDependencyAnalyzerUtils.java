package edelta.dependency.analyzer;

import static java.util.Comparator.comparing;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.emf.ecore.EPackage;

import GraphMM.Dependency;
import GraphMM.Edge;
import GraphMM.Metamodel;
import GraphMM.Node;
import GraphMM.Repository;

/**
 * Utilities for {@link EPackage} and {@link Repository}
 *
 * @author Lorenzo Bettini
 *
 */
public class EdeltaDependencyAnalyzerUtils {

	private EdeltaDependencyAnalyzerUtils() {
		// only static methods
	}

	public static EdeltaMetamodelDependencies computeMetamodelDependencies(Repository repository) {
		var highlighted = metamodelStream(repository.getNodes().stream())
			.findFirst()
			.orElseThrow(() ->  new IllegalArgumentException
					("No highlighted Metamodel found"));
		var dependencies = computeDependencies(repository, highlighted);
		return new EdeltaMetamodelDependencies(highlighted, dependencies);
	}

	private static List<Metamodel> computeDependencies(Repository repository, Metamodel subject) {
		return metamodelStream(dependencyStream(repository.getEdges().stream())
			.filter(d -> d.getSrc() == subject || d.getTrg() == subject)
			.flatMap(d -> Stream.of(d.getSrc(), d.getTrg())))
			.filter(m -> m != subject)
			.sorted(comparing(Metamodel::getName))
			.collect(Collectors.toList());
	}

	private static Stream<Metamodel> metamodelStream(Stream<Node> nodes) {
		return nodes
			.filter(Metamodel.class::isInstance)
			.map(Metamodel.class::cast);
	}

	private static Stream<Dependency> dependencyStream(Stream<Edge> edges) {
		return edges
			.filter(Dependency.class::isInstance)
			.map(Dependency.class::cast);
	}
}
