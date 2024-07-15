package edelta.dependency.analyzer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.log4j.Logger;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;

import GraphMM.Dependency;
import GraphMM.GraphMMFactory;
import GraphMM.GraphMMPackage;
import GraphMM.Metamodel;
import GraphMM.Repository;
import edelta.lib.EdeltaModelManager;
import edelta.lib.EdeltaResourceUtils;
import edelta.lib.EdeltaUtils;

/**
 * Analyzes the dependencies of {@link EPackage}s and creates a {@link Repository}.
 * 
 * @author Lorenzo Bettini
 *
 */
public class EdeltaDependencyAnalizer {

	private static final GraphMMFactory graphFactory = GraphMMFactory.eINSTANCE;

	private static final Logger LOG = Logger.getLogger(EdeltaDependencyAnalizer.class);

	private EdeltaModelManager modelManager = new EdeltaModelManager();

	/**
	 * Analyzes the dependencies of the specified Ecore file together
	 * with all the other {@link EPackage}s found in the specified path.
	 * 
	 * @param ecoreFile
	 * @return
	 * @throws IOException
	 */
	public Repository analyzeEPackage(String ecoreFile) throws IOException {
		var loaded = modelManager.loadEcoreFile(ecoreFile);
		var packageToAnalyze = EdeltaResourceUtils.getEPackage(loaded);
		String path = new File(ecoreFile).getParent();
		return analyzeEPackage(path, packageToAnalyze.getName());
	}

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
			var packages = EdeltaResourceUtils.getEPackages(resources);
			var packageToAnalyze = getEPackageByName(packages, packageName);
			packages.remove(packageToAnalyze);
			return analyzeEPackages(packageToAnalyze, packages);
		}
	}

	private EPackage getEPackageByName(Collection<EPackage> packages, String packageName) {
		return packages.stream()
			.filter(p -> packageName.equals(p.getName()))
			.findFirst()
			.orElseThrow(
				() -> new IllegalArgumentException
					("No EPackage with name: " + packageName));
	}

	private List<Resource> loadEcoreFiles(Stream<Path> stream) {
		return stream
			.filter(file -> !Files.isDirectory(file))
			.filter(file -> file.toString().endsWith(".ecore"))
			.map(file -> modelManager.loadEcoreFile(file.toString()))
			.toList();
	}

	private Repository analyzeEPackages(EPackage ePackage, Collection<EPackage> packages) {
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
		var usedPackages = usedPackages(current);
		for (var used : usedPackages) {
			var usedMetamodel = createGraphMetamodel(repository, used);
			findOppositeDependency(repository, current, used)
				.ifPresentOrElse(
					d -> d.setBidirectional(true),
					() -> createDependency(repository, metamodel, usedMetamodel));
			analyzeEPackage(repository, used, seen);
		}
	}

	private Collection<EPackage> usedPackages(EPackage ePackage) {
		return EdeltaUtils.usedPackages(ePackage)
				.stream()
				.sorted(EdeltaResourceUtils.ePackageComparator()) // we must be deterministic
				.toList();
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

	/**
	 * Whether there already exists a dependency from target to source (that is,
	 * the opposite dependency that will then have to be set as bidirectional).
	 * 
	 * @param repository
	 * @param src
	 * @param trg
	 * @return
	 */
	private Optional<Dependency> findOppositeDependency(Repository repository, EPackage src, EPackage trg) {
		return repository.getEdges().stream()
			.filter(Dependency.class::isInstance)
			.map(Dependency.class::cast)
			.filter(d ->
				((Metamodel) d.getTrg()).getNsURI().equals(src.getNsURI()) &&
				((Metamodel) d.getSrc()).getNsURI().equals(trg.getNsURI()))
			.findFirst();
	}

	private Dependency createDependency(Repository repository, Metamodel source, Metamodel target) {
		var dependency = graphFactory.createDependency();
		dependency.setSrc(source);
		dependency.setTrg(target);
		repository.getEdges().add(dependency);
		return dependency;
	}

	public void saveRepository(Repository repository, String outputPath, String fileName) throws IOException {
		// Create a resource set to hold the resources.
		var resourceSet = new ResourceSetImpl();
		
		// Register the appropriate resource factory to handle all file extensions.
		resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put
			(Resource.Factory.Registry.DEFAULT_EXTENSION, 
			 new XMIResourceFactoryImpl());

		// Register the package to ensure it is available during loading.
		resourceSet.getPackageRegistry().put
			(GraphMMPackage.eNS_URI, 
			 GraphMMPackage.eINSTANCE);

		LOG.info("Saving " + outputPath + "/" + fileName);

		var newFile = new File(outputPath, fileName);
		newFile.getParentFile().mkdirs();
		var fos = new FileOutputStream(newFile);

		var resource = resourceSet.createResource(URI.createURI("http:///My.graphmm"));
		resource.getContents().add(repository);
		resource.save(fos, null);
		fos.flush();
		fos.close();
	}

	// for testing
	public void loadEcoreFile(String path) {
		modelManager.loadEcoreFile(path);
	}

	// for testing
	public EPackage getEPackage(String packageName) {
		return modelManager.getEPackage(packageName);
	}

}
