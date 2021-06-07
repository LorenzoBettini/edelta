package edelta.dependency.analyzer;

import static java.util.Comparator.comparing;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.emf.ecore.EPackage;

import GraphMM.Dependency;
import GraphMM.Metamodel;
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
		var highlighted = repository.getNodes().stream()
			.filter(Metamodel.class::isInstance)
			.map(Metamodel.class::cast)
			.findFirst()
			.orElseThrow(() ->  new IllegalArgumentException
					("No highlighted Metamodel found"));
		var dependencies = repository.getEdges().stream()
			.filter(Dependency.class::isInstance)
			.map(Dependency.class::cast)
			.filter(d -> d.getSrc() == highlighted || d.getTrg() == highlighted)
			.flatMap(d -> Stream.of(d.getSrc(), d.getTrg()))
			.filter(Metamodel.class::isInstance)
			.map(Metamodel.class::cast)
			.filter(m -> m != highlighted)
			.sorted(comparing(Metamodel::getName))
			.collect(Collectors.toList());
		return new EdeltaMetamodelDependencies(highlighted, dependencies);
	}
}
