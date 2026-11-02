# Edelta

An Xtext DSL for EMF metamodels refactoring and migration

![Java CI with Maven and SonarCloud](https://github.com/LorenzoBettini/edelta/workflows/Java%20CI%20with%20Maven%20and%20SonarCloud/badge.svg)

[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=io.github.lorenzobettini.edelta%3Aedelta.parent&metric=coverage)](https://sonarcloud.io/dashboard?id=io.github.lorenzobettini.edelta%3Aedelta.parent) 
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=io.github.lorenzobettini.edelta%3Aedelta.parent&metric=alert_status)](https://sonarcloud.io/dashboard?id=io.github.lorenzobettini.edelta%3Aedelta.parent)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.lorenzobettini.edelta/edelta.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/io.github.lorenzobettini.edelta/edelta)

### Requirements

The current development version uses Xtext 2.44.0, MWE2 2.27.0, and Eclipse
2026-09, built with the released `xtext-tycho-parent` 0.26.0 (Tycho 5.0.4).
Jupiter tests use JUnit 6 to match Xtext's testing bundles.

The mixed JUnit 4/Jupiter projects (`edelta.tests`, `edelta.lib.tests`,
`edelta.refactorings.lib.tests`, and `edelta.examples`) run both generations in
one Tycho Surefire execution. Their POMs select `providerHint` `junit6`, since
Tycho 5.0.4 otherwise selects JUnit 4 for mixed projects. Their manifests import
`org.junit.vintage.engine` to expose Vintage to test discovery, and the target
platform includes `junit-vintage-engine` at the same version as Jupiter.

Since version 3.9.0, Edelta requires Java 21.

Since version 3.7.0, the Edelta Eclipse distribution requires Java 21.

Since version 3.1.0, Edelta requires Java 17.

### Eclipse update site (new URL)

Copy the following URL and paste it into your Eclipse "Install New Software" dialog ("Help" => "Install New Software..."), wait for the list of available features to show and select the latest version of "Edelta Feature" (You can install additional Edelta features listed, including the "Edelta IDE External Components" to install additional external features to have the same installed software as the one in the Edelta Eclipse distribution, see below). (The URL is NOT meant to be opened with a browser.)

https://lorenzobettini.github.io/edelta-releases/

The above update site aggregates all the available versions. To restrict to only some versions, e.g., based on the major version, use "updates/<majorversion>.x". To restrict to "major.minor", use "updates/<majorversion>.x/<majorversion>.<minorversion>.x".
For example,

https://lorenzobettini.github.io/edelta-releases/updates/3.x/ (for major versions 3.x)

https://lorenzobettini.github.io/edelta-releases/updates/3.x/3.8.x/ (for versions 3.8.x)

IMPORTANT: Edelta requires Xtext; if this is not already installed, all dependencies will be automatically installed from this update site, which is self-contained.

WARNING: The previous update site hosted on bintray does not exist anymore, so please make sure you update your existing Eclipse distribution where you were already using Edelta.

### Pre-configured Eclipse distributions with Edelta installed, for several architectures.

Download a complete Eclipse distribution with Edelta installed; choose the one for your OS and architecture (since version 2.8.0, we also provide a version for the `aarch64` architecture, for Linux and macOS):

https://sourceforge.net/projects/edelta/files/products

Such distributions are already configured with the update site(s) of the shape "major.minor" (see above).
So, if a new "major.minor" is released, you only need to "Check for Updates".
If you want to update to a new "major.minor", either you download a new distribution, or you add the proper new update site in "Install New Software" and "Check for Updates".

**WARNING** If you downloaded one of the Edelta Eclipse distributions earlier than version 0.3.x you won't be able to update it; please download a brand new Edelta Eclipse distribution with version at least 0.3.x (issue [#13](https://github.com/LorenzoBettini/edelta/issues/13)).

**For macOS users**: depending on the version of your macOS, when you try to run the `edelta.app`, you may run into an error that says "the application is damaged and can't be opened". This problem can be overcome by running the following command from the terminal (from the directory where the `edelta.app` is located): `xattr -c edelta.app`.

### Development Snapshots (new URL)

These are temporary development snapshots, which might be unstable (Note: the snapshots update site always includes the releases update site):

https://lorenzobettini.github.io/edelta-snapshots/

### Maven artifacts

Since version 0.7.1, Maven artifacts for Edelta (mainly the compiler and the libraries) are available on Maven Central (the groupId is `io.github.lorenzobettini.edelta`), and can be used, for instance, together with the `xtext-maven-plugin`, to compile `.edelta` files into Java files during a Maven build, both a Maven/Tycho build and a pure Maven build. An example of a pure Maven project using this mechanism can be found here: https://github.com/LorenzoBettini/edelta/tree/master/edelta.parent/edelta.maven.example.

### Picto integration tests

The dependency analyzer uses Epsilon/Picto 2.9. Its
`EdeltaRunDependencyAnalysisHandler` schedules editor and Picto opening in a UI
job after the workspace modification operation finishes. Starting Picto outside
that operation avoids the cancelled rendering that displayed "Terminated by the
user" instead of the graph.

The packaged IDE tests in
`edelta.parent/edelta.swtbot.tests/src/edelta/swtbot/tests/EdeltaSwtbotTest.java`
check that Picto renders the generated dependency analysis, keeps focus after
rendering, and can render it again when its editor is closed and reopened. They
also fail on asynchronous Epsilon and Eclipse UI errors recorded in the Eclipse
log, including failures to create the Picto view. Cleanup waits for Picto rendering
and closes the view before deleting workspace files retained by its background jobs.
The tests also wait for the command's queued UI job before checking a reopened
view, so they cannot inspect stale content from the previous editor.

On Ubuntu, install the native SWT browser runtime before running Picto tests:

```sh
sudo apt-get update
sudo apt-get install --no-install-recommends -y libwebkit2gtk-4.1-0
```

Xvfb provides a display for headless tests; Picto's embedded browser additionally
requires WebKitGTK. On headless Linux, prefix the Maven command below with
`xvfb-run -a`. CI log artifacts include hidden `.metadata/.log` workspace logs so
native browser initialization failures retain their original diagnostics.

Run the complete build, including these tests and aggregate coverage, with a
working display:

```sh
./mvnw -B -f edelta.parent/pom.xml clean verify -Pjacoco,test-ide
```

### Interpreter timeouts

See [Interpreter timeouts](docs/interpreter-timeouts.md) for timeout cleanup,
interrupt handling, and the regression tests for intermittent compilation failures.

### Contributors

- Lorenzo Bettini
- Ludovico Iovino
