# Edelta

An Xtext DSL for EMF metamodels refactoring and migration

![Java CI with Maven and SonarCloud](https://github.com/LorenzoBettini/edelta/workflows/Java%20CI%20with%20Maven%20and%20SonarCloud/badge.svg)

[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=io.github.lorenzobettini.edelta%3Aedelta.parent&metric=coverage)](https://sonarcloud.io/dashboard?id=io.github.lorenzobettini.edelta%3Aedelta.parent) 
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=io.github.lorenzobettini.edelta%3Aedelta.parent&metric=alert_status)](https://sonarcloud.io/dashboard?id=io.github.lorenzobettini.edelta%3Aedelta.parent)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.lorenzobettini.edelta/edelta.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22io.github.lorenzobettini.edelta%22%20AND%20a:%22edelta%22)

### Requirements

Since version 3.7.0, the Edelta Eclipse distribution requires Java 21.

Since version 3.1.0, Edelta requires Java 17.

### Eclipse update site (new URL)

Copy the following URL and paste it into your Eclipse "Install New Software" dialog ("Help" => "Install New Software..."), wait for the list of available features to show and select the latest version of "Edelta Feature". (The URL is NOT meant to be opened with a browser)

https://lorenzobettini.github.io/edelta-releases/

IMPORTANT: Edelta requires Xtext, if this is not already installed, all dependencies will be automatically installed from this update site, which is self-contained.

WARNING: the previous update site hosted on bintray does not exist anymore, so please make sure you update your existing Eclipse distribution where you were already using Edelta.

### Pre-configured Eclipse distributions with Edelta installed, for several architectures.

Download a complete Eclipse distribution with Edelta installed; choose the one for your OS and architecture (since version 2.8.0, we also provide a version for the `aarch64` architecture, for Linux and macOS): https://sourceforge.net/projects/edelta/files/products

**WARNING** If you downloaded one of Edelta Eclipse distributions earlier than version 0.3.x you won't be able to update it; please download a brand new Edelta Eclipse distribution with version at least 0.3.x (issue [#13](https://github.com/LorenzoBettini/edelta/issues/13)).

**For macOS users**: depending on the version of your macOS, when you try to run the `edelta.app` you may run into an error that says "the application is damaged and can't be opened". This problem can be overcome by running the following command from the terminal (from the directory where the `edelta.app` is located): `xattr -c edelta.app`.

### Development Snapshots (new URL)

These are temporary development snapshots, which might be unstable (Note: the snapshots update site always includes the releases update site):

https://lorenzobettini.github.io/edelta-snapshots/

### Maven artifacts

Since version 0.7.1, Maven artifacts for Edelta (mainly the compiler and the libraries) are avaialable on Maven Central (the groupId is `io.github.lorenzobettini.edelta`), and can be used, for instance, together with the `xtext-maven-plugin`, to compile `.edelta` files into Java files during a Maven build, both a Maven/Tycho build and a pure Maven build. An example of a pure Maven project using this mechanism can be found here: https://github.com/LorenzoBettini/edelta/tree/master/edelta.parent/edelta.maven.example.
