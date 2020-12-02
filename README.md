# Edelta

An Xtext DSL for EMF metamodels refactoring and migration

![Java CI with Maven and SonarCloud](https://github.com/LorenzoBettini/edelta/workflows/Java%20CI%20with%20Maven%20and%20SonarCloud/badge.svg)

[![Coverage Status](https://coveralls.io/repos/github/LorenzoBettini/edelta/badge.svg?branch=master)](https://coveralls.io/github/LorenzoBettini/edelta?branch=master)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=io.github.lorenzobettini.edelta%3Aedelta.parent&metric=alert_status)](https://sonarcloud.io/dashboard?id=io.github.lorenzobettini.edelta%3Aedelta.parent)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.lorenzobettini.edelta/edelta.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22io.github.lorenzobettini.edelta%22%20AND%20a:%22edelta%22)

### Requirements

Edelta requires Java 11.

### Eclipse update site

Copy the following URL and paste it into your Eclipse "Install New Software" dialog ("Help" => "Install New Software..."), wait for the list of available features to show and select the latest version of "Edelta Feature". (The URL is NOT meant to be opened with a browser)

https://dl.bintray.com/lorenzobettini/edelta/updates/

IMPORTANT: Edelta requires Xtext, if this is not already installed, all dependencies will be automatically installed from this update site, which is self-contained.

### Pre-configured Eclipse distributions with Edelta installed, for several architectures.

Download a complete Eclipse distribution with Edelta installed; choose the one for your OS and architecture: https://sourceforge.net/projects/edelta/files/products

**WARNING** If you downloaded one of Edelta Eclipse distributions earlier than version 0.3.x you won't be able to update it; please download a brand new Edelta Eclipse distribution with version at least 0.3.x (issue [#13](https://github.com/LorenzoBettini/edelta/issues/13)).

### Development Snapshots

These are temporary development snapshots, which might be unstable (Note: the snapshots update site always includes the releases update site):

https://dl.bintray.com/lorenzobettini/edelta/snapshots/updates/

### Maven artifacts

Since version 0.7.1, Maven artifacts for Edelta (mainly the compiler and the libraries) are avaialable on Maven Central (the groupId is `io.github.lorenzobettini.edelta`), and can be used, for instance, together with the `xtext-maven-plugin`, to compile `.edelta` files into Java files during a Maven build, both a Maven/Tycho build and a pure Maven build. An example of a pure Maven project using this mechanism can be found here: https://github.com/LorenzoBettini/edelta/tree/master/edelta.parent/edelta.maven.example.
