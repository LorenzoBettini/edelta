package edelta.tests;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EcoreFactory;
import org.eclipse.emf.ecore.InternalEObject;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.EcoreResourceFactoryImpl;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.XtextRunner;
import org.junit.Rule;
import org.junit.Test;
import org.junit.Assume;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;

import edelta.edelta.EdeltaFactory;
import edelta.edelta.EdeltaTestModelPath;
import edelta.resource.EdeltaModelSourceSelector;
import edelta.resource.EdeltaModelSourceSelector.Status;

/**
 * Headless source selection checks using a temporary project and existing model fixtures.
 *
 * @author Lorenzo Bettini
 */
@RunWith(XtextRunner.class)
@InjectWith(EdeltaInjectorProvider.class)
public class EdeltaModelSourceSelectorTest extends EdeltaAbstractTest {
	private static final Path FIXTURES = Path.of(
		"../edelta.testdata/testdata/classesWithTheSameName");

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	private final EdeltaModelSourceSelector selector = new EdeltaModelSourceSelector();

	@Test
	public void noDeclarationIsOptIn() throws Exception {
		var program = parseHelper.parse("metamodel \"ecore\"");
		var result = selector.select(program, folder.getRoot().toPath().resolve("absent"));
		assertThat(result.selected()).isEmpty();
		assertThat(result.ignored()).isEmpty();
		assertThat(result.failures()).isEmpty();
	}

	@Test
	public void recursiveSelectionDeduplicatesAndKeepsFirstEntry() throws Exception {
		var root = folder.getRoot().toPath();
		var source = FIXTURES.resolve("MyRoot1.xmi");
		var nested = root.resolve("models/nested/MyRoot1.xmi");
		Files.createDirectories(nested.getParent());
		Files.copy(source, nested);
		Files.writeString(root.resolve("models/notes.txt"), "ignored");
		var before = Files.readAllBytes(nested);
		var directory = entry("models");
		var duplicate = entry("models/nested/MyRoot1.xmi");
		var result = selector.select(List.of(directory, duplicate), List.of(myPackage("My1.ecore")),
			root);
		assertThat(result.selected()).hasSize(1);
		assertThat(result.selected().get(0).entry()).isSameAs(directory);
		assertThat(result.selected().get(0).resource().getContents()).hasSize(1);
		assertThat(result.sourceEntries()).containsEntry(nested, directory);
		assertThat(result.ignored()).isEmpty();
		assertThat(result.failures()).isEmpty();
		assertThat(Files.readAllBytes(nested)).isEqualTo(before);
	}

	@Test
	public void namesDoNotOverrideNamespaceUris() throws Exception {
		var root = folder.getRoot().toPath();
		Files.copy(FIXTURES.resolve("MyRoot1.xmi"), root.resolve("one.xmi"));
		Files.copy(FIXTURES.resolve("MyRoot2.xmi"), root.resolve("two.xmi"));
		var imported = myPackage("My1.ecore");
		imported.setName(myPackage("My2.ecore").getName());
		var result = selector.select(List.of(entry(".")), List.of(imported), root);
		assertThat(result.selected()).extracting(s -> s.path().getFileName().toString())
			.containsExactly("one.xmi");
		assertThat(result.ignored()).extracting(i -> i.path().getFileName().toString())
			.containsExactly("two.xmi");
		assertThat(result.failures()).isEmpty();
	}

	@Test
	public void migrateImportSelectsOriginalNamespace() throws Exception {
		var root = folder.getRoot().toPath();
		Files.copy(FIXTURES.resolve("MyRoot1.xmi"), root.resolve("model.xmi"));
		var program = parseWithLoadedEcore(FIXTURES.resolve("My1.ecore").toString(), """
			testmodels { "model.xmi" }
			migrate "http://my.package1.org" to "http://new.package.org"
			""");
		assertThat(program.getMigrations().get(0).getNsURI().eIsProxy()).isFalse();
		var result = selector.select(program, root);
		assertThat(result.selected()).hasSize(1);
		assertThat(result.failures()).isEmpty();
	}

	@Test
	public void missingInvalidAndMalformedSourcesAreTiedToEntries() throws Exception {
		var root = folder.getRoot().toPath();
		Files.writeString(root.resolve("bad.xmi"), "<broken");
		Files.writeString(root.resolve("wrong.txt"), "text");
		var missing = entry("absent.xmi");
		var invalid = entry("../outside.xmi");
		var absolute = entry(root.resolve("wrong.txt").toString());
		var nullPath = entry(null);
		var illegal = entry("illegal\0.xmi");
		var blank = entry("  ");
		var wrong = entry("wrong.txt");
		var malformed = entry("bad.xmi");
		var result = selector.select(List.of(missing, invalid, absolute, nullPath, illegal,
			blank, wrong, malformed),
			List.of(myPackage("My1.ecore")), root);
		assertThat(result.failures()).extracting(f -> f.status())
			.containsExactly(Status.MISSING, Status.INVALID_PATH, Status.INVALID_PATH,
				Status.INVALID_PATH, Status.INVALID_PATH, Status.INVALID_PATH,
				Status.INVALID_PATH, Status.MALFORMED);
		assertThat(result.failures()).extracting(f -> f.entry())
			.containsExactly(missing, invalid, absolute, nullPath, illegal, blank, wrong,
				malformed);
		assertThat(result.failures().get(4).cause()).isNotNull();
		assertThat(result.failures().get(7).cause()).isNotNull();
		assertThat(result.selected()).isEmpty();
	}

	@Test
	public void mixedNamespacesFailWithoutLoading() throws Exception {
		var root = folder.getRoot().toPath();
		Files.writeString(root.resolve("mixed.xmi"), """
			<xmi:XMI xmlns:xmi="http://www.omg.org/XMI"
			 xmlns:one="http://my.package1.org" xmlns:other="http://other.example">
			 <one:MyClass/><other:Unknown/>
			</xmi:XMI>
			""");
		var result = selector.select(List.of(entry("mixed.xmi")),
			List.of(myPackage("My1.ecore")), root);
		assertThat(result.failures()).extracting(f -> f.status())
			.containsExactly(Status.MIXED_NAMESPACE);
		assertThat(result.selected()).isEmpty();
		assertThat(result.resourceSet().getResources()).isEmpty();
	}

	@Test
	public void selectedLoadFailuresAreNotIgnored() throws Exception {
		var root = folder.getRoot().toPath();
		Files.writeString(root.resolve("unknownType.xmi"), """
			<one:Unknown xmlns:one="http://my.package1.org"/>
			""");
		var result = selector.select(List.of(entry("unknownType.xmi")),
			List.of(myPackage("My1.ecore")), root);
		assertThat(result.failures()).extracting(f -> f.status())
			.containsExactly(Status.LOAD_FAILED);
		assertThat(result.failures().get(0).cause()).isNotNull();
		assertThat(result.ignored()).isEmpty();
	}

	@Test
	public void emptyImportsAndUnresolvablePackagesAreIgnored() throws Exception {
		var root = folder.getRoot().toPath();
		Files.copy(FIXTURES.resolve("MyRoot1.xmi"), root.resolve("model.xmi"));
		var noNsURI = EcoreFactory.eINSTANCE.createEPackage();
		var proxy = EcoreFactory.eINSTANCE.createEPackage();
		((InternalEObject) proxy).eSetProxyURI(URI.createURI("missing.ecore#//missing"));
		var result = selector.select(List.of(entry("model.xmi")),
			Arrays.asList(null, noNsURI, proxy), root);
		assertThat(result.ignored()).hasSize(1);
		assertThat(result.failures()).isEmpty();
	}

	@Test
	public void subpackageImportsAreRegistered() throws Exception {
		var root = folder.getRoot().toPath();
		Files.copy(FIXTURES.resolve("MyRoot1.xmi"), root.resolve("model.xmi"));
		var parent = EcoreFactory.eINSTANCE.createEPackage();
		parent.setNsURI("http://parent.example");
		parent.getESubpackages().add(myPackage("My1.ecore"));
		var result = selector.select(List.of(entry("model.xmi")), List.of(parent), root);
		assertThat(result.selected()).hasSize(1);
		assertThat(result.failures()).isEmpty();
	}

	@Test
	public void xsiTypesAndUnboundPrefixesAreInspected() throws Exception {
		var root = folder.getRoot().toPath();
		Files.writeString(root.resolve("mixedType.xmi"), """
			<xmi:XMI xmlns:xmi="http://www.omg.org/XMI"
			 xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
			 xmlns:one="http://my.package1.org" xmlns:other="http://other.example">
			 <one:MyClass><feature xsi:type="other:Unknown"/></one:MyClass>
			</xmi:XMI>
			""");
		Files.writeString(root.resolve("unbound.xmi"), """
			<one:MyClass xmlns:one="http://my.package1.org"
			 xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
			 xsi:type="bad:Unknown"/>
			""");
		Files.writeString(root.resolve("unqualified.xmi"), """
			<one:MyClass xmlns:one="http://my.package1.org"
			 xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
			 xsi:type="Unknown"/>
			""");
		var result = selector.select(List.of(entry("mixedType.xmi"), entry("unbound.xmi"),
			entry("unqualified.xmi")),
			List.of(myPackage("My1.ecore")), root);
		assertThat(result.failures()).extracting(f -> f.status())
			.containsExactly(Status.MIXED_NAMESPACE, Status.MALFORMED, Status.MALFORMED);
	}

	@Test
	public void unreadableInputsHaveStructuredFailures() throws Exception {
		var root = folder.getRoot().toPath();
		var file = root.resolve("unreadable.xmi");
		Files.writeString(file, "<model/>");
		var folderPath = root.resolve("unreadableFolder");
		Files.createDirectory(folderPath);
		try {
			Files.setPosixFilePermissions(file, Set.of());
			Files.setPosixFilePermissions(folderPath, Set.of());
			var result = selector.select(List.of(entry("unreadable.xmi"),
				entry("unreadableFolder")), List.of(myPackage("My1.ecore")), root);
			assertThat(result.failures()).extracting(f -> f.status())
				.containsExactly(Status.UNREADABLE, Status.UNREADABLE);
		} finally {
			Files.setPosixFilePermissions(file, Set.of(PosixFilePermission.OWNER_READ,
				PosixFilePermission.OWNER_WRITE));
			Files.setPosixFilePermissions(folderPath, Set.of(PosixFilePermission.OWNER_READ,
				PosixFilePermission.OWNER_WRITE, PosixFilePermission.OWNER_EXECUTE));
		}
	}

	@Test
	public void nonRegularExistingPathIsInvalid() throws Exception {
		Assume.assumeTrue(Files.exists(Path.of("/dev/null")));
		var root = folder.getRoot().toPath();
		Files.createSymbolicLink(root.resolve("device.xmi"), Path.of("/dev/null"));
		var result = selector.select(List.of(entry("device.xmi")),
			List.of(myPackage("My1.ecore")), root);
		assertThat(result.failures()).extracting(f -> f.status())
			.containsExactly(Status.INVALID_PATH);
	}

	@Test
	public void emptyXmlNamespaceDoesNotCountAsModelNamespace() throws Exception {
		var root = folder.getRoot().toPath();
		Files.writeString(root.resolve("wrapper.xmi"), """
			<xmi:XMI xmlns:xmi="http://www.omg.org/XMI"
			 xmlns:one="http://my.package1.org">
			 <wrapper xmlns=""><one:MyClass/></wrapper>
			</xmi:XMI>
			""");
		var result = selector.select(List.of(entry("wrapper.xmi")),
			List.of(myPackage("My1.ecore")), root);
		assertThat(result.failures()).extracting(f -> f.status())
			.containsExactly(Status.LOAD_FAILED);
	}

	@Test
	public void selectedCrossResourceReferencesResolveInSharedSet() throws Exception {
		var root = folder.getRoot().toPath();
		Files.writeString(root.resolve("A.xmi"), """
			<one:MyRoot xmlns:one="http://my.package1.org" myReferences="B.xmi#/"/>
			""");
		Files.writeString(root.resolve("B.xmi"), """
			<one:MyClass xmlns:one="http://my.package1.org"/>
			""");
		var beforeA = Files.readAllBytes(root.resolve("A.xmi"));
		var beforeB = Files.readAllBytes(root.resolve("B.xmi"));
		var result = selector.select(List.of(entry(".")), List.of(myPackage("My1.ecore")), root);
		assertThat(result.selected()).hasSize(2);
		assertThat(result.selected().get(0).resource().getResourceSet())
			.isSameAs(result.selected().get(1).resource().getResourceSet());
		EObject a = result.selected().get(0).resource().getContents().get(0);
		EObject b = result.selected().get(1).resource().getContents().get(0);
		var reference = (EReference) a.eClass().getEStructuralFeature("myReferences");
		assertThat((List<?>) a.eGet(reference)).hasSize(1);
		assertThat(((List<?>) a.eGet(reference)).get(0)).isSameAs(b);
		assertThat(Files.readAllBytes(root.resolve("A.xmi"))).isEqualTo(beforeA);
		assertThat(Files.readAllBytes(root.resolve("B.xmi"))).isEqualTo(beforeB);
	}

	private EdeltaTestModelPath entry(String path) {
		var entry = EdeltaFactory.eINSTANCE.createEdeltaTestModelPath();
		entry.setPath(path);
		return entry;
	}

	private EPackage myPackage(String filename) throws Exception {
		var resourceSet = new ResourceSetImpl();
		resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap()
			.put("ecore", new EcoreResourceFactoryImpl());
		var uri = org.eclipse.emf.common.util.URI.createFileURI(
			FIXTURES.resolve(filename).toAbsolutePath().toString());
		return (EPackage) resourceSet.getResource(uri, true).getContents().get(0);
	}
}
