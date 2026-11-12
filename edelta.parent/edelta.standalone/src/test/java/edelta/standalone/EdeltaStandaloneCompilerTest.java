package edelta.standalone;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import javax.tools.ToolProvider;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class EdeltaStandaloneCompilerTest {
	@Rule
	public TemporaryFolder temporaryFolder = new TemporaryFolder();

	private final EdeltaStandaloneCompiler compiler = new EdeltaStandaloneCompiler();

	@Test
	public void compilesEcoreAndSeveralEdeltaFilesToJava() throws Exception {
		final var root = copyExample();
		Files.writeString(root.resolve("model/notes.txt"), "ignored");
		compiler.compile(root, sourceFolders(), Path.of("generated"));

		final var generated = root.resolve("generated/com/example");
		assertThat(Files.readString(generated.resolve("Example.java")))
			.isEqualTo(resourceText("expected/com/example/Example.java"));
		assertThat(Files.readString(generated.resolve("ExampleReusableFunctions.java")))
			.isEqualTo(resourceText("expected/com/example/ExampleReusableFunctions.java"));

		final var classes = Files.createDirectory(root.resolve("classes"));
		final var result = ToolProvider.getSystemJavaCompiler().run(null, null, null,
			"-classpath", System.getProperty("surefire.test.class.path"),
			"-d", classes.toString(),
			generated.resolve("Example.java").toString(),
			generated.resolve("ExampleReusableFunctions.java").toString());
		assertThat(result).isZero();
		assertThat(classes.resolve("com/example/Example.class")).exists();
	}

	@Test
	public void acceptsAbsoluteOutputDirectory() throws Exception {
		final var root = copyExample();
		final var output = temporaryFolder.newFolder("outside").toPath();
		compiler.compile(root, sourceFolders(), output);
		assertThat(output.resolve("com/example/Example.java")).exists();
	}

	@Test
	public void rejectsInvalidSourceFolders() throws Exception {
		final var root = copyExample();
		assertThatThrownBy(() -> compiler.compile(root, List.of(), Path.of("generated")))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("At least one source folder");
		assertThatThrownBy(() -> compiler.compile(root, List.of(root.resolve("model")),
			Path.of("generated")))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("relative to the project root");
		assertThatThrownBy(() -> compiler.compile(root, List.of(Path.of("../outside")),
			Path.of("generated")))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("relative to the project root");
		assertThatThrownBy(() -> compiler.compile(root, List.of(Path.of("missing")),
			Path.of("generated")))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("does not exist");
	}

	@Test
	public void reportsInvalidEdeltaWithoutGeneratingJava() throws Exception {
		final var root = copyExample();
		final var source = root.resolve("src/main/java/example/example/Example.edelta");
		Files.writeString(source,
			"package com.example\nmetamodel \"myecore\"\nmodifyEcore broken epackage {\n");
		assertThatThrownBy(() -> compiler.compile(root, sourceFolders(), Path.of("generated")))
			.isInstanceOf(EdeltaCompilationException.class)
			.satisfies(error -> assertThat(((EdeltaCompilationException) error).getDiagnostics())
				.anySatisfy(diagnostic -> assertThat(diagnostic)
					.contains("Example.edelta:").contains(": ")));
		assertThat(root.resolve("generated/com/example/Example.java")).doesNotExist();
	}

	@Test
	public void reportsMissingMetamodelWithoutGeneratingJava() throws Exception {
		final var root = copyExample();
		final var source = root.resolve("src/main/java/example/example/Example.edelta");
		Files.writeString(source, Files.readString(source)
			.replace("metamodel \"myecore\"", "metamodel \"missing\""));
		assertThatThrownBy(() -> compiler.compile(root, sourceFolders(), Path.of("generated")))
			.isInstanceOf(EdeltaCompilationException.class)
			.hasMessageContaining("Example.edelta:");
		assertThat(root.resolve("generated/com/example/Example.java")).doesNotExist();
	}

	@Test
	public void acceptsSourceFolderWithoutEdeltaFiles() throws Exception {
		final var root = copyExample();
		compiler.compile(root, List.of(Path.of("model")), Path.of("generated"));
		assertThat(root.resolve("generated/com/example/Example.java")).doesNotExist();
	}

	@Test
	public void warningsDoNotPreventGeneration() throws Exception {
		final var root = copyExample();
		final var source = root.resolve("src/main/java/example/example/ExampleReusableFunctions.edelta");
		Files.writeString(source, "import java.util.List\n" + Files.readString(source));
		compiler.compile(root, sourceFolders(), Path.of("generated"));
		assertThat(root.resolve("generated/com/example/ExampleReusableFunctions.java")).exists();
	}

	private Path copyExample() throws Exception {
		final var root = temporaryFolder.newFolder("project").toPath();
		copyResource(root, "model/My.ecore");
		copyResource(root, "src/main/java/example/example/Example.edelta");
		copyResource(root, "src/main/java/example/example/ExampleReusableFunctions.edelta");
		return root;
	}

	private void copyResource(final Path root, final String relativePath) throws IOException {
		final var destination = root.resolve(relativePath);
		Files.createDirectories(destination.getParent());
		Files.writeString(destination, resourceText(relativePath));
	}

	private String resourceText(final String path) throws IOException {
		try (var stream = getClass().getClassLoader().getResourceAsStream(path)) {
			return new String(stream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
		}
	}

	private List<Path> sourceFolders() {
		return List.of(Path.of("model"), Path.of("src/main/java"));
	}
}
