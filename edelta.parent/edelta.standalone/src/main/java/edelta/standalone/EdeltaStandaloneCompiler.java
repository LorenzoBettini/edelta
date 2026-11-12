package edelta.standalone;

import java.io.IOException;
import java.util.Objects;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.xtext.diagnostics.Severity;
import org.eclipse.xtext.ecore.EcoreSupportStandaloneSetup;
import org.eclipse.xtext.generator.GeneratorContext;
import org.eclipse.xtext.generator.GeneratorDelegate;
import org.eclipse.xtext.generator.JavaIoFileSystemAccess;
import org.eclipse.xtext.resource.IResourceServiceProvider;
import org.eclipse.xtext.resource.XtextResourceSet;
import org.eclipse.xtext.util.CancelIndicator;
import org.eclipse.xtext.validation.CheckMode;
import org.eclipse.xtext.workspace.FileProjectConfig;
import org.eclipse.xtext.workspace.ProjectConfigAdapter;

import com.google.inject.Injector;

import edelta.EdeltaStandaloneSetup;

/**
 * Compiles all Edelta sources in a set of project source folders to Java files.
 * Each call uses a fresh resource set; Java types are resolved through the calling
 * thread's context class loader.
 */
public class EdeltaStandaloneCompiler {
	private final Injector injector;

	public EdeltaStandaloneCompiler() {
		EcoreSupportStandaloneSetup.setup();
		injector = new EdeltaStandaloneSetup().createInjectorAndDoEMFRegistration();
	}

	/**
	 * Reads Ecore and Edelta files recursively, validates them, and generates Java.
	 * No Java files are generated if any input contains an error.
	 *
	 * @param projectRoot the root of the source project
	 * @param sourceFolders nonempty list of source folders relative to the root
	 * @param outputDirectory Java output directory, relative to the root or absolute
	 * @throws IOException if a source folder cannot be read
	 * @throws IllegalArgumentException if a source folder is invalid
	 * @throws EdeltaCompilationException if source validation fails
	 */
	public void compile(final Path projectRoot, final List<Path> sourceFolders,
			final Path outputDirectory) throws IOException {
		final var root = projectRoot.toAbsolutePath().normalize();
		final var ecoreFiles = new ArrayList<Path>();
		final var edeltaFiles = new ArrayList<Path>();
		final var projectConfig = new FileProjectConfig(root.toFile());
		if (sourceFolders.isEmpty()) {
			throw new IllegalArgumentException("At least one source folder is required");
		}
		for (final var sourceFolder : sourceFolders) {
			if (sourceFolder.isAbsolute() || sourceFolder.normalize().startsWith("..")) {
				throw new IllegalArgumentException("Source folder must be relative to the project root: "
					+ sourceFolder);
			}
			final var folder = root.resolve(sourceFolder).normalize();
			if (!Files.isDirectory(folder)) {
				throw new IllegalArgumentException("Source folder does not exist: " + folder);
			}
			projectConfig.addSourceFolder(sourceFolder.toString().replace('\\', '/'));
			try (Stream<Path> paths = Files.walk(folder)) {
				paths.filter(Files::isRegularFile).sorted(Comparator.naturalOrder()).forEach(path -> {
					final var name = path.getFileName().toString();
					if (name.endsWith(".ecore")) {
						ecoreFiles.add(path);
					} else if (name.endsWith(".edelta")) {
						edeltaFiles.add(path);
					}
				});
			}
		}

		final var resourceSet = injector.getInstance(XtextResourceSet.class);
		resourceSet.setClasspathURIContext(Thread.currentThread().getContextClassLoader());
		ProjectConfigAdapter.install(resourceSet, projectConfig);
		// Edelta's metamodel "ecore" resolves through the resource index. The
		// generated EcorePackage in the global registry is not indexed by itself.
		loadBuiltInEcore(resourceSet, root.resolve(sourceFolders.get(0)));
		final var resources = new ArrayList<Resource>();
		for (final var file : ecoreFiles) {
			resources.add(load(resourceSet, file));
		}
		for (final var file : edeltaFiles) {
			resources.add(load(resourceSet, file));
		}

		final var diagnostics = new ArrayList<String>();
		for (final var resource : resources) {
			final var serviceProvider = IResourceServiceProvider.Registry.INSTANCE
				.getResourceServiceProvider(resource.getURI());
			for (final var issue : serviceProvider.getResourceValidator()
					.validate(resource, CheckMode.ALL, CancelIndicator.NullImpl)) {
				if (issue.getSeverity() == Severity.ERROR) {
					diagnostics.add(resource.getURI().toFileString() + ":"
						+ issue.getLineNumber() + ": " + issue.getMessage());
				}
			}
		}
		if (!diagnostics.isEmpty()) {
			throw new EdeltaCompilationException(diagnostics);
		}

		final var fileAccess = injector.getInstance(JavaIoFileSystemAccess.class);
		fileAccess.setOutputPath(root.resolve(outputDirectory).normalize().toString());
		fileAccess.setWriteTrace(false);
		final var generator = injector.getInstance(GeneratorDelegate.class);
		final var context = new GeneratorContext();
		for (final var resource : resources) {
			if ("edelta".equals(resource.getURI().fileExtension())) {
				generator.generate(resource, fileAccess, context);
			}
		}
	}

	private Resource load(final XtextResourceSet resourceSet, final Path file) {
		return resourceSet.getResource(URI.createFileURI(file.toString()), true);
	}

	private void loadBuiltInEcore(final XtextResourceSet resourceSet,
			final Path sourceFolder) throws IOException {
		final var uri = URI.createFileURI(sourceFolder.resolve("__builtin__/Ecore.ecore").toString());
		final var resource = resourceSet.createResource(uri);
		try (var input = Objects.requireNonNull(
			EcorePackage.class.getResourceAsStream("/model/Ecore.ecore"))) {
			resource.load(input, resourceSet.getLoadOptions());
		}
	}
}
