# Live model preview in Edelta examples

The Edelta programs listed below now have `testmodels` declarations for the
live, in-memory model preview. The input files are **independent samples** under
`preview-models/`: the original `model/` files, `expectations/`, generated Java,
and existing regression tests are unchanged.

Open any listed `.edelta` source in the current Edelta Eclipse editor and expand
**Evolved test models** in the Outline view. Paths are relative to the root of
this Eclipse project, **not** to the directory containing the `.edelta` file.
Selecting an instance whose EClass was renamed in this Edelta source, or a
property row with a directly changed visible Ecore feature, can select the
corresponding metamodel expression. An attribute row without such a link can
select the last successfully applied `transformAttributeValueRule` invocation
for that exact final object and attribute. A successful transformation that
returns the same value still counts. Without a proven copy lineage or a source
expression in the current document, selection goes to the declaring `testmodels`
path. Imported refactoring expressions are never used as offsets in this editor.
Arbitrary direct EObject modifications outside supported migration operations
are not tracked and may leave an earlier attribute link stale.

| Edelta program (under `src/edelta/`) | Sample input | What to inspect in the outline |
| --- | --- | --- |
| `mergename/example/PersonMargeNameExample.edelta` | `merge-name/Founders.persons`, `Researchers.xmi` | Two resources selected recursively; `firstname` and `lastname` become the merged `name` values such as `Ada Lovelace` and `Grace Hopper`. |
| `extractclass/example/PersonExtractAddressExample.edelta` | `extract-address/Offices.xmi` | `street`, `zip`, and `city` move into contained `Address` objects. |
| `statecharts/example/StateChartsExample.edelta` | `statecharts/Workflow.xmi` | `Node` instances are replaced with position-based subclasses (`InitialState`, `State`, `FinalState`). Check nested nodes and transition references. |
| `statechartsenum/example/StateChartsEnumExample.edelta` | `statecharts-enum/Workflow.statechart` | The original `Node.type` enum determines new subclasses; look for `InitialState`, `State`, and `FinalState`. Demonstrates a non-`.xmi` extension. |
| `examxml/example/ExamXMLExample2.edelta` | `exam-split/Exam.xmi` | `OpenElement` instances are split into `OpenElement1` and `OpenElement2`. The instance with two questions yields two results. |
| `petrinet/example/PetrinetExample.edelta` | `petrinet/OrderProcessing.xmi` | Several Ecore refactorings and explicit model migration convert old direct place/transition links into contained PT/TP arc objects. |
| `stdlib/examples/ChangeReferenceTypeExample.edelta` | `change-reference-type/People.persons` | Containment `firstName` changes its target from `NameElement` to `OtherNameElement`, with value copied to `otherNameElementValue`. |
| `stdlib/examples/ChangeToAbstractExample.edelta` | `change-to-abstract/Staff.xmi` | `Person` becomes abstract; old `manager` values select `Manager` versus `Employee`, and the obsolete attribute disappears. |

All paths in the middle column are relative to `preview-models/`. All sample
XMI files include **relative `xsi:schemaLocation` paths** to the metamodel in
`model/`. The original XMI files are never modified by preview interpretation.

