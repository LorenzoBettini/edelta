package edelta.resource;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.XMLConstants;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;

import edelta.edelta.EdeltaProgram;
import edelta.edelta.EdeltaTestModelPath;
import edelta.util.EdeltaModelUtil;

/**
 * Selects original XMI inputs for an explicitly declared preview. The caller supplies the
 * containing project's filesystem root; no working-directory or workbench lookup occurs here.
 * Each call owns a new resource set. Inputs are read only and never saved.
 *
 * @author Lorenzo Bettini
 */
public class EdeltaModelSourceSelector {
	private static final int CONTENT_SNIFF_SIZE = 8192;

	/** The outcome for a declared source or an XMI file within it. */
	public enum Status { SELECTED, IGNORED, MISSING, INVALID_PATH, UNREADABLE,
		MALFORMED, MIXED_NAMESPACE, LOAD_FAILED }

	/** A selected original model and the first declaration that selected it. */
	public record Selected(Path path, EdeltaTestModelPath entry, Resource resource) {}

	/** A source that was inspected but did not use an imported namespace. */
	public record Ignored(Path path, EdeltaTestModelPath entry) {}

	/** A testable source failure; {@code cause} retains the original parser or I/O exception. */
	public record Failure(Status status, Path path, EdeltaTestModelPath entry, Throwable cause) {}

	private record Candidate(EdeltaTestModelPath entry, boolean explicit) {}

	/**
	 * Ordered selection results. The map includes only selected files and retains the first
	 * declaring entry when declarations overlap. Resources share {@code resourceSet} so references
	 * between selected files can resolve after all files have loaded.
	 */
	public record Result(ResourceSet resourceSet, List<Selected> selected, List<Ignored> ignored,
		List<Failure> failures, Map<Path, EdeltaTestModelPath> sourceEntries) {}

	/**
	 * Selects sources declared by {@code program}, using its metamodel and migrate imports.
	 * With no declaration, returns an empty result without inspecting the filesystem or imports.
	 * Paths must be project relative; absolute paths and paths escaping the root are invalid.
	 * Directory entries are traversed recursively in lexical path order. Explicit files may have
	 * any extension; directory entries are inspected conservatively for XML model content.
	 *
	 * @param program the parsed Edelta program
	 * @param projectRoot the containing Eclipse project's filesystem location
	 * @return ordered selected, ignored, and failed source outcomes
	 */
	public Result select(EdeltaProgram program, Path projectRoot) {
		var resourceSet = new ResourceSetImpl();
		if (program.getTestModels() == null) {
			return new Result(resourceSet, List.of(), List.of(), List.of(), Map.of());
		}
		var packages = EdeltaModelUtil.getMetamodels(program);
		return select(program.getTestModels().getPaths(), packages, projectRoot, resourceSet);
	}

	/**
	 * Selects declared paths with the supplied original EPackages. This overload is useful for
	 * headless callers that already have the resolved import set.
	 *
	 * @param entries quoted paths in program order
	 * @param packages original imported EPackages, including migrate imports
	 * @param projectRoot the containing project's filesystem location
	 * @return ordered selection results
	 */
	public Result select(List<EdeltaTestModelPath> entries, List<EPackage> packages,
			Path projectRoot) {
		return select(entries, packages, projectRoot, new ResourceSetImpl());
	}

	private Result select(List<EdeltaTestModelPath> entries, List<EPackage> packages,
			Path projectRoot, ResourceSet resourceSet) {
		var selected = new ArrayList<Selected>();
		var ignored = new ArrayList<Ignored>();
		var failures = new ArrayList<Failure>();
		var sourceEntries = new LinkedHashMap<Path, EdeltaTestModelPath>();
		var candidates = new LinkedHashMap<Path, Candidate>();
		var root = projectRoot.toAbsolutePath().normalize();
		var namespaces = new LinkedHashSet<String>();
		resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap()
			.putIfAbsent("xmi", new XMIResourceFactoryImpl());
		resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap()
			.putIfAbsent("*", new XMIResourceFactoryImpl());
		for (var ePackage : packages) {
			register(ePackage, namespaces, resourceSet);
		}
		for (var entry : entries) {
			Path path;
			try {
				var literal = entry.getPath();
				if (literal == null || literal.isBlank()) {
					failures.add(new Failure(Status.INVALID_PATH, null, entry, null));
					continue;
				}
				var relative = Path.of(literal);
				path = root.resolve(relative).normalize();
				if (relative.isAbsolute() || !path.startsWith(root)) {
					failures.add(new Failure(Status.INVALID_PATH, path, entry, null));
					continue;
				}
			} catch (InvalidPathException exception) {
				failures.add(new Failure(Status.INVALID_PATH, null, entry, exception));
				continue;
			}
			if (!Files.exists(path)) {
				failures.add(new Failure(Status.MISSING, path, entry, null));
			} else if (Files.isDirectory(path)) {
				try (var files = Files.walk(path)) {
					var discovered = files.filter(Files::isRegularFile)
						.sorted().toList();
					for (var file : discovered) {
						addCandidate(candidates, file, entry, false);
					}
				} catch (IOException | UncheckedIOException exception) {
					failures.add(new Failure(Status.UNREADABLE, path, entry, exception));
				}
			} else if (Files.isRegularFile(path)) {
				addCandidate(candidates, path, entry, true);
			} else {
				failures.add(new Failure(Status.INVALID_PATH, path, entry, null));
			}
		}
		for (var candidate : candidates.entrySet()) {
			var path = candidate.getKey();
			var source = candidate.getValue();
			var entry = source.entry();
			var xmiExtension = path.getFileName().toString().endsWith(".xmi");
			var contentPrefix = new byte[0];
			Set<String> found;
			try (var input = Files.newInputStream(path)) {
				contentPrefix = input.readNBytes(CONTENT_SNIFF_SIZE);
				if (!source.explicit() && !xmiExtension && !looksLikeXml(contentPrefix)) {
					ignored.add(new Ignored(path, entry));
					continue;
				}
				found = namespaces(new SequenceInputStream(
					new ByteArrayInputStream(contentPrefix), input));
			} catch (XMLStreamException exception) {
				if (source.explicit() || xmiExtension
						|| looksLikeXmi(contentPrefix, namespaces)) {
					failures.add(new Failure(Status.MALFORMED, path, entry, exception));
				} else {
					ignored.add(new Ignored(path, entry));
				}
				continue;
			} catch (IOException exception) {
				if (source.explicit() || xmiExtension) {
					failures.add(new Failure(Status.UNREADABLE, path, entry, exception));
				} else {
					ignored.add(new Ignored(path, entry));
				}
				continue;
			}
			if (Collections.disjoint(found, namespaces)) {
				ignored.add(new Ignored(path, entry));
				continue;
			}
			if (!namespaces.containsAll(found)) {
				failures.add(new Failure(Status.MIXED_NAMESPACE, path, entry, null));
				continue;
			}
			var uri = URI.createFileURI(path.toString());
			var resource = resourceSet.createResource(uri);
			try {
				resource.load(Map.of());
				selected.add(new Selected(path, entry, resource));
				sourceEntries.put(path, entry);
			} catch (IOException | RuntimeException exception) {
				resourceSet.getResources().remove(resource);
				failures.add(new Failure(Status.LOAD_FAILED, path, entry, exception));
			}
		}
		return new Result(resourceSet, List.copyOf(selected), List.copyOf(ignored),
			List.copyOf(failures), Collections.unmodifiableMap(sourceEntries));
	}

	private void addCandidate(Map<Path, Candidate> candidates, Path path,
			EdeltaTestModelPath entry, boolean explicit) {
		var existing = candidates.get(path);
		if (existing == null) {
			candidates.put(path, new Candidate(entry, explicit));
		} else if (explicit && !existing.explicit()) {
			candidates.put(path, new Candidate(existing.entry(), true));
		}
	}

	private void register(EPackage ePackage, Set<String> namespaces, ResourceSet resourceSet) {
		if (ePackage == null || ePackage.eIsProxy() || ePackage.getNsURI() == null) {
			return;
		}
		namespaces.add(ePackage.getNsURI());
		resourceSet.getPackageRegistry().put(ePackage.getNsURI(), ePackage);
		for (var subpackage : ePackage.getESubpackages()) {
			register(subpackage, namespaces, resourceSet);
		}
	}

	private boolean looksLikeXml(byte[] prefix) {
		var content = contentText(prefix);
		if (content.startsWith("\uFEFF")) {
			content = content.substring(1);
		}
		return content.stripLeading().startsWith("<");
	}

	private boolean looksLikeXmi(byte[] prefix, Set<String> namespaces) {
		var content = contentText(prefix);
		return content.contains("xmi:XMI")
			|| content.contains("http://www.omg.org/XMI")
			|| namespaces.stream().anyMatch(content::contains);
	}

	private String contentText(byte[] prefix) {
		if (prefix.length >= 2 && prefix[0] == (byte) 0xFE && prefix[1] == (byte) 0xFF) {
			return new String(prefix, 2, prefix.length - 2, StandardCharsets.UTF_16BE);
		}
		if (prefix.length >= 2 && prefix[0] == (byte) 0xFF && prefix[1] == (byte) 0xFE) {
			return new String(prefix, 2, prefix.length - 2, StandardCharsets.UTF_16LE);
		}
		return new String(prefix, StandardCharsets.UTF_8);
	}

	private Set<String> namespaces(InputStream input) throws IOException, XMLStreamException {
		var factory = XMLInputFactory.newFactory();
		factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
		factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
		var found = new LinkedHashSet<String>();
		var reader = factory.createXMLStreamReader(input);
		try {
			while (reader.hasNext()) {
				if (reader.next() != XMLStreamConstants.START_ELEMENT) {
					continue;
				}
				var namespace = reader.getNamespaceURI();
				if (namespace != null && !namespace.equals("http://www.omg.org/XMI")) {
					found.add(namespace);
				}
				var type = reader.getAttributeValue(XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI,
					"type");
				if (type != null) {
					var colon = type.indexOf(':');
					if (colon < 1) {
						throw new XMLStreamException("Unqualified xsi:type: " + type);
					}
					var prefix = type.substring(0, colon);
					var typeNamespace = reader.getNamespaceURI(prefix);
					if (typeNamespace == null) {
						throw new XMLStreamException("Unbound xsi:type prefix: " + prefix);
					}
					found.add(typeNamespace);
				}
			}
		} finally {
			reader.close();
		}
		found.remove("");
		return found;
	}
}
