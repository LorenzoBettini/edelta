# Live model previews

When an Edelta program declares `testmodels`, the editor can show the models
after the program's metamodel changes and model-migration callbacks have run.
The preview is kept in memory; it does not save changes to the input XMI files
or generate Java.

## Declaring inputs

Place one optional `testmodels` block after the metamodel declarations:

```edelta
metamodel "PersonList"

testmodels {
    "models"
    "othermodels/My.xmi"
}

modifyEcore evolve epackage PersonList {
    // Metamodel changes and model migrations
}
```

Paths are resolved relative to the filesystem location of the Eclipse project
containing the Edelta file, not relative to the process working directory.
Paths must be project-relative; absolute paths and paths whose normalized
location escapes the project root are rejected. A quoted path can name an XMI
model file with any extension (or no extension), or a directory. `.xmi` is not
required: files are selected based on their XML/XMI content and imported
namespace URIs, not their filename extension. Directories are searched
recursively in lexical path order. Lightweight content inspection skips
unrelated non-XML and malformed files; recognizable malformed XMI and malformed
`.xmi` files are reported. Overlapping directory and file entries select a
file once, attributed to the first declared path that found it.

Only XMI resources whose namespaces match EPackages imported by the program
(including packages declared by `migrate`) are loaded. Both model-element roots
and `xmi:XMI` document roots containing model elements are supported. Matching
uses namespace URIs, not package names or filenames. Unrelated files are
ignored without an error. An empty XMI file has no model namespace and is
ignored in the same way. A file mixing imported and unknown model namespaces
is unsupported and reports an input error. Invalid, missing, unreadable,
malformed, or un-loadable explicitly declared inputs are reported on their
quoted path. A preview with an input failure is not published.

Resources selected together share a resource set, so references between
selected XMI files can resolve. A resource outside the selected set is not
automatically loaded to follow a reference. Relative `xsi:schemaLocation`
values in XMI are supported when their Ecore locations are available relative
to the model file or the imported package is already available.

## Viewing the result

The outline adds an **Evolved test models** section containing one in-memory
resource for each selected input. All roots remain in their original order.
The tree shows the final containment hierarchy and model-property rows.
Attributes and non-containment references appear as values; reference values
are not expanded as children.

Selecting the section goes to the `testmodels` block; selecting a resource
goes to the quoted path that selected it.
An object with a renamed EClass can instead go to the expression responsible
for that rename. A property row can go to an expression responsible for a
visible change to its Ecore feature: its name, a reference's displayed bounds,
or an attribute's scalar/list shape.

An EAttribute row can also go to the last successfully applied, observed
`transformAttributeValueRule` for that exact surviving object and attribute.
If a visible Ecore feature change and an attribute transformation both have
trustworthy source expressions, the Ecore change takes priority. Different
instances of the same attribute can therefore lead to different expressions.
A transformation that returns the original value still counts as applied;
the link does not establish that the value changed.

Expression links require a clear source location in the current Edelta file.
When no applicable expression is trustworthy because provenance is missing or
ambiguous, or belongs to an older preview or another source file, selection
uses the declaring `testmodels` path. This includes unchanged items without an
observed attribute transformation and unsupported changes. Reference rows use
visible Ecore feature changes, not proven reference-value migration history.
Migration tracing currently covers only observed attribute transformations;
arbitrary direct EObject changes outside supported migrator operations may
leave an earlier attribute link stale. The outline does not open or edit the
original XMI file, keep a full change history, or synchronize editor selection
back to model objects.

The preview shows only the final state after all applicable migration
callbacks. Intermediate snapshots, diffs, and automatic validation of evolved
models are not provided. If a migration or final copy fails, no partial model
preview is published; the error is attached to the relevant transformation
source when available.

Preview requires at least one `modifyEcore` operation. Programs containing
only `migrate` declarations do not currently produce a model preview. Changes
to external XMI files do not trigger a workbench-wide preview refresh; edit or
reinstall the Edelta derived state to recompute it.
