# Developing Edelta

This guide covers the Edelta source tree, local builds, testing, code coverage,
and the conventions used to maintain the project. It is intended for developers
working on the repository, including contributors running focused tests before
submitting changes.

Commands below run from the repository root. Most Maven modules are under
`edelta.parent/`; there is no `pom.xml` at the repository root. The Maven
wrapper is `./mvnw`, and the build uses Java 21.

## Repository layout

Edelta is an Xtext/Xbase language for EMF metamodel refactoring and model
migration. The main modules and supporting projects are:

| Path (under `edelta.parent/` unless indicated) | Purpose |
| --- | --- |
| `pom.xml` | Maven/Tycho reactor and shared configuration; inherits the released `xtext-tycho-parent` |
| `edelta/` | Language, grammar, compiler, interpreter, derived state, scoping, and type system |
| `edelta.lib/` | Runtime, model management, migration, and standard library |
| `edelta.refactorings.lib/` | Reusable `.edelta` refactorings and Java helpers |
| `edelta.ide/`, `edelta.ui/` | IDE services and Eclipse workbench integration |
| `edelta.tests/` | Parser, validator, compiler, interpreter, and derived-state tests |
| `edelta.lib.tests/`, `edelta.refactorings.lib.tests/` | Runtime and refactoring regression tests |
| `edelta.ui.tests/` | Eclipse editor and workbench tests |
| `edelta.swtbot.tests/` | Packaged Eclipse product tests |
| `edelta.testutils/`, `edelta.ui.testutils/`, `edelta.tests.parent/` | Shared test infrastructure |
| `edelta.testdata/` | Model/metamodel fixtures and expected outputs |
| `edelta.dependency.analyzer*` | Dependency analyzer, GraphMM, UI/Picto integration, and tests |
| `edelta.examples/`, `edelta.maven.example/` | DSL examples and a plain Maven JAR consumer |
| `edelta.target/edelta.target.target` | Tycho target platform |
| `edelta.feature/`, `edelta.product.ui.feature/`, `edelta.repository/` | Features, product, and update-site packaging |
| `edelta.coverage/` | Focused JaCoCo Ant report/check module |
| `edelta.tests.report/` | Aggregate JaCoCo report for broader CI runs |

The Edelta grammar and MWE2 generator are
`edelta.parent/edelta/src/edelta/Edelta.xtext` and
`edelta.parent/edelta/src/edelta/GenerateEdelta.mwe2`.
Language implementation packages include `jvmmodel/`, `compiler/`,
`interpreter/`, `resource/`, `scoping/`, and `typesystem/`.

The primary CI references are `.github/workflows/maven.yml` and
`.github/workflows/pr.yml`. These and the current POM files take precedence
over older build descriptions in `Jenkinsfile` and
`edelta.parent/releng/old.travis.yml`.

The parent POM uses a released `xtext-tycho-parent` (https://github.com/LorenzoBettini/xtext-build-utils/); a sibling
`xtext-build-utils` checkout is not automatically substituted into this build.
The `edelta.maven.example` module also inherits the shared plugin and Xtext
configuration.

## Coding and test conventions

- Java and Xtend sources use tabs, UTF-8, and no trailing whitespace. Lines of
  about 100 characters or fewer are preferred where practical. XML and workflow
  files retain their surrounding formatting; unrelated reformatting is avoided.
- The source baseline is Java 21. `var` is useful when the inferred type is
  obvious; `final` can improve clarity for parameters and local variables.
- New classes include `@author Lorenzo Bettini` in their Javadoc. Public and
  protected methods, and nontrivial lifecycle behavior, have appropriate
  Javadoc. This is especially important for copied EPackages, derived state,
  interpretation, and model migration.
- AssertJ is preferred for new assertions. Existing tests use both JUnit 4
  and JUnit Jupiter; new tests follow the surrounding test class.
- Changes to dependencies or Java baselines generally require coordinated
  changes in Maven and OSGi metadata.

Regression tests for language changes may need to cover parsing, diagnostics,
scoping, interpretation/derived state, complete generated Java, compilation,
runtime behavior, and serialization. Invalid inputs and boundary conditions
are important alongside successful cases.

For compiler tests, `EdeltaAbstractCompilerTest` provides helpers to compare
the complete generated Java and check that it compiles. Grammar changes should
be checked for new ambiguities, including generator warnings such as
`Decision can match input ... using multiple alternatives` and disabled
alternatives.

For refactoring and migration changes, verification includes both evolved Ecore
and migrated instance models, including references and serialized results.
`EdeltaTestUtils` and the shared fixtures under `edelta.testdata/` can be reused.
Expectation changes should be reviewed against the intended semantics. User-
visible changes usually warrant updates to examples or documentation.

## Generated sources and OSGi configuration

Edelta's Xtext grammar declares `generate edelta`, so its AST is inferred from
the grammar. Generated output includes `src-gen/`, `xtend-gen/`, and the
language's generated model. The canonical edit location is normally the
grammar, Xtend, or other generator input, not its generated output.

Two projects have additional generation requirements:

- The dependency analyzer's EMF model is defined in
  `edelta.parent/edelta.dependency.analyzer.model/model/GraphMM.ecore` and
  `GraphMM.genmodel`. Its model/edit code is generated through the owning
  EMF generator.
- The refactoring library's `.edelta` sources are under
  `edelta.parent/edelta.refactorings.lib/src/`. Its generated Java in
  `edelta-lib-src-gen/` is tracked, so DSL changes require regeneration.
  Examples normally generate Java into `edelta-gen/`.

For an incremental build with generated outputs already present,
`-DskipMWE2 -DskipXtend` avoids regeneration. These switches are **not**
appropriate after changing the corresponding grammar/workflow or Xtend inputs.
A fresh checkout or final regeneration check should run the generators.

`-Dedelta.skip=true` separately skips compilation/generation of refactoring
library DSL sources. It is suitable when those `.edelta` sources are unchanged,
but should be omitted when changed sources need regeneration.

Avoid `clean` when an incremental build relies on existing generated outputs.
Generated-directory placeholders and `.gitignore` files can be important to
inherited build profiles.

Tycho resolves OSGi and Maven dependencies together. Local bundle dependencies
may need declarations in both Maven POMs and OSGi manifests to be pulled in by
Maven `-am`. Also check `build.properties`, features, plugin dependencies, and
Tycho `extraRequirements`. Not every partial reactor currently includes all
requirements from OSGi metadata alone. Target definitions are read directly
from `edelta.target`; their relative paths and referenced-repository handling
matter.

The test setup also has some version-specific details:

- In projects migrated to JUnit 6, the Eclipse classpath container is
  `org.eclipse.jdt.junit.JUNIT_CONTAINER/6`. Tycho 5.0.4 does not resolve
  that container itself, so Jupiter imports belong in the manifest and the
  engine/platform bundles in the target.
- Mixed JUnit 4/Jupiter test projects use one Surefire execution with
  `providerHint` set to `junit6`, plus the `org.junit.vintage.engine` import
  and corresponding `junit-vintage-engine` bundle. Without explicit selection,
  Surefire can select the JUnit 4 provider instead.
- Inherited Surefire JVM arguments and platform-specific options, including
  the macOS first-thread setting, are part of the working test configuration.

## Focused builds and tests

**Normal development uses affected test suites, not a full reactor build.**
The Maven `verify` phase is needed for Tycho's tests. One typical language test
command, using existing generated sources, is:

```sh
./mvnw -B -f edelta.parent/pom.xml verify -pl :edelta.tests -am \
  -DskipMWE2 -DskipXtend -Dedelta.skip=true \
  -Dtycho.localArtifacts=ignore -Djgit.dirtyWorkingTree=ignore
```

The target module can instead be `:edelta.lib.tests`,
`:edelta.refactorings.lib.tests`, `:edelta.dependency.analyzer.tests`, or
`:edelta.ui.tests`, depending on the affected functionality. A test class can
be selected during development, for example with `-Dtest=EdeltaCompilerTest`.
Before considering a code change verified, all affected test suites should
run, not only a selected class.

The default `development` profile includes Eclipse integration, tests, and
examples, and is normally kept enabled. `-am` builds required Maven modules
and may also execute tests in required test projects. If a partial reactor
misses a local requirement, include the necessary projects explicitly or use
the full reactor rather than relying on previously installed Edelta artifacts.

For library/compiler changes, UI tests are unnecessary unless editor behavior
or an integration boundary is affected. Focused coverage does not require
SWTBot or product materialization. Test-only changes normally require relevant
tests rather than a new production coverage report; documentation-only changes
normally need no Maven build.

Parallel Maven builds sharing a Maven/p2 cache can interfere with each other
because Tycho locks cached artifacts. The usual Maven wrapper version in the
current repository is 3.9.9; the POMs and CI use Java 21.

### Running Eclipse UI tests

Eclipse UI tests need a graphical display. For `edelta.ui.tests` on headless
Linux, Xvfb is often sufficient:

```sh
env -u WAYLAND_DISPLAY -u XDG_SESSION_TYPE GDK_BACKEND=x11 \
  xvfb-run -a -s "-screen 0 1920x1080x24" \
  ./mvnw -B -f edelta.parent/pom.xml verify -pl :edelta.ui.tests -am \
  -DskipMWE2 -DskipXtend -Dedelta.skip=true \
  -Dtycho.localArtifacts=ignore -Djgit.dirtyWorkingTree=ignore
```

The packaged-product SWTBot tests need a real window manager and should not
use the bare `xvfb-run` arrangement above. A normal desktop session is one
option. A nested X server such as Xephyr is another, **provided a window
manager is running on that display**. For example, on Linux:

```sh
Xephyr -screen 1024x768 :50 &
# Start an available window manager on DISPLAY=:50 before SWTBot.
env -u WAYLAND_DISPLAY -u XDG_SESSION_TYPE GDK_BACKEND=x11 \
  DISPLAY=:50 ./mvnw ...
# Stop the nested display/window manager after testing.
```

`-Ptest-ide` materializes the product and runs the packaged-product tests; it
normally requires a full reactor so the product archive exists.
`local-swtbot-test` is a different, local harness mode.

Picto also needs the native SWT browser runtime. On Ubuntu this includes
`libwebkit2gtk-4.1-0`; a virtual display does not supply WebKitGTK.

### Inspecting test results

Maven success alone does not guarantee every test produced only expected logs.
Useful inspection locations include:

- `**/target/surefire-reports/` for JUnit test results and reruns.
- `**/target/**/*.log` for Eclipse/Tycho logs, including
  `target/work/data/.metadata/.log`.
- Test-project `screenshots/` directories for SWTBot failures.

`edelta.ui.tests` is configured to retry tests up to three times. Results
should distinguish first-attempt failures from tests that passed on retry.
Expected diagnostics from negative tests should not be confused with unexpected
exceptions. When collecting CI logs, hidden paths such as `.metadata/.log`
need to be included.

## Focused JaCoCo coverage

The standard verification for changed production code is **affected tests plus
focused line and branch coverage of the modified classes**. A complete
reactor, product materialization, or SWTBot run is not a prerequisite for
ordinary JaCoCo reporting.

`edelta.parent/edelta.coverage` is an opt-in Maven `pom` module using JaCoCo
Ant `report` and `check` tasks. Unlike the aggregate `edelta.tests.report`
module, it has no Maven test-module dependencies and therefore does not force
unrelated suites into a focused build. It is activated by `-Pcoverage-focused`
during `verify`, with `-Pjacoco` collecting test execution data.

The main report properties are:

| Property | Meaning |
| --- | --- |
| `coverage.bundle` | Production module under `edelta.parent/`, e.g. `edelta.lib` or `edelta` |
| `coverage.exec.files` | Comma-separated JaCoCo `.exec` files, relative to `edelta.parent/` |
| `coverage.classes` | Ant-style compiled class-file patterns; include changed classes and nested/anonymous classes |
| `coverage.line.minimum` | Minimum line ratio; default `1.00` |
| `coverage.branch.minimum` | Minimum branch ratio; default `1.00` |

The reporter does not infer changed classes from Git. The selected patterns
must include **all modified production classes**, including nested classes.
The normal target is 100% line and branch coverage for the selected modified
classes. For a diagnostic report without enforcement, both minimum properties
may be temporarily set to `0.00`; this **does not** count as satisfying the
coverage requirement.

### Run tests and coverage together

For example, a fresh library test run with strict coverage of
`EdeltaModelMigrator` is:

```sh
rm -f edelta.parent/edelta.lib.tests/target/jacoco.exec
./mvnw -B -f edelta.parent/pom.xml verify \
  -Pjacoco,coverage-focused \
  -pl :edelta.lib.tests,:edelta.coverage -am \
  -Dcoverage.bundle=edelta.lib \
  -Dcoverage.exec.files=edelta.lib.tests/target/jacoco.exec \
  '-Dcoverage.classes=edelta/lib/EdeltaModelMigrator*.class' \
  -DskipMWE2 -DskipXtend -Dedelta.skip=true \
  -Dtycho.localArtifacts=ignore -Djgit.dirtyWorkingTree=ignore
```

For the language interpreter or compiler, the corresponding suite is
`:edelta.tests`, the production bundle is `edelta`, and relevant class
patterns might include `edelta/compiler/**/*.class`. Other production bundles
follow the same pattern. UI coverage uses `:edelta.ui.tests` with a suitable
display. If behavior spans modules, the combined build can run multiple test
suites and use their `.exec` files; each production bundle is checked through
its own report invocation.

### Check existing execution data without rerunning tests

A report-only pass can use `.exec` files from an already successful run:

```sh
./mvnw -B -f edelta.parent/pom.xml verify \
  -Pcoverage-focused -pl :edelta.coverage \
  -Dcoverage.bundle=edelta.lib \
  -Dcoverage.exec.files=edelta.lib.tests/target/jacoco.exec \
  '-Dcoverage.classes=edelta/lib/EdeltaModelMigrator*.class'
```

HTML, XML, and CSV outputs are written to
`edelta.parent/edelta.coverage/target/site/jacoco/`. Each run replaces the
previous report; retain copies when comparing multiple modules.

### Coverage reliability

- JaCoCo can append to existing execution data. Before a fresh run, remove
  only the chosen test suites' `target/jacoco.exec` files.
- Execution data must correspond to the class bytes being reported. Stale
  `.exec` files, mismatched builds, and old reports are not reliable evidence.
- Combined partial-reactor test/report builds should run sequentially, not
  with Maven `-T`. The independent report module relies on reactor order;
  list `:edelta.coverage` with the test module(s) and use `-am`.
- Coverage reports and test results are separate checks. A report produced
  after a failed test run is diagnostic rather than final verification.
- If an existing larger class has unrelated uncovered branches, the limitation
  should be documented precisely, including the uncovered code and measured
  coverage. A lower threshold or narrower class pattern must not silently
  substitute for checking changed code.
- The reporter detects missing execution files and empty class selections,
  but cannot itself prove that execution data is fresh.

## Full integration and release verification

Broader verification is relevant for CI, release preparation, changes to
product packaging, or significant integration milestones. It is not the
default for every production change.

The aggregate JaCoCo module, `edelta.tests.report`, has explicit dependencies
on numerous test suites, including SWTBot. The `test-ide` profile also
materializes the Eclipse product and enables its tests. A full
CI-equivalent command is:

```sh
./mvnw -B -f edelta.parent/pom.xml clean verify -Pjacoco,test-ide \
  -Dedelta.skip=true \
  -Dtycho.localArtifacts=ignore -Djgit.dirtyWorkingTree=ignore
```

This workflow requires the appropriate display, window manager, and product
harness. The aggregate coverage output is under
`edelta.parent/edelta.tests.report/target/site/jacoco-aggregate/`.
Focused class coverage and full aggregate coverage are different measurements.

After a build, `git status` helps identify unintended generated-source,
Eclipse metadata, or other changes. A useful verification summary includes
which test suites ran, their results, any UI retries, the production classes
measured for coverage, line and branch results, and any remaining gaps.

## Version and release considerations

An update of the utility parent also calls for checking the Xtext version
range in `edelta.feature`, as noted in the parent POM. Java baseline changes span
Maven, CI, OSGi manifests, Eclipse compiler settings, classpaths, and the
target's `targetJRE`.

The parent POM has an element ordering used by
`edelta.parent/releng/releasing/increment_versions.ant`. Release profiles
and scripts can publish to Maven Central, GitHub Pages, and SourceForge, and
may commit or push changes. They are intended for explicit release operations,
not ordinary local verification.

For change descriptions and commit messages, concise summaries of observable
technical changes are most useful. Reviewing the staged diff and working tree
before committing helps avoid including generated or unrelated files.
