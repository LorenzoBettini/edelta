/*******************************************************************************
 * Copyright (c) 2016 itemis AG (http://www.itemis.de) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package com.itemis.xtext.generator.vscode

import java.util.Collections
import java.util.List
import java.util.regex.Pattern
import javax.inject.Inject
import org.eclipse.emf.mwe2.runtime.Mandatory
import org.eclipse.xtend.lib.annotations.Accessors
import org.eclipse.xtext.naming.IQualifiedNameConverter
import org.eclipse.xtext.xtext.generator.AbstractXtextGeneratorFragment
import org.eclipse.xtext.xtext.generator.model.FileAccessFactory

import static extension org.eclipse.xtext.GrammarUtil.*
import static extension org.eclipse.xtext.xtext.generator.util.GrammarUtil2.*
import static extension org.eclipse.xtext.xtext.generator.web.RegexpExtensions.*

/** 
 * Generator fragment for Visual Studio Code integration. The fragment produces
 * the code for a VS Code extension into the generic ide project.
 * 
 * @author Karsten Thoms - Initial contribution and API
 */
class VSCodeExtensionFragment extends AbstractXtextGeneratorFragment {
	@Inject FileAccessFactory fileAccessFactory
	@Inject extension IQualifiedNameConverter
	
	@Accessors// (PUBLIC_SETTER)
	static class Versions {
		/** Version of the resulting extension. Default: 0.1.0 */
		String vscExtension = "0.1.0"
		/** Xtext Version. */
		String xtext = "2.17.1"
		/** VSCode Engine version. Default: ^1.2.0*/
		String vscEngine = "^1.2.0"
		/** TypeScript version. Default: ^1.8.10 */
		String typescript = "^1.8.10"
		/** VS Code version. Default: ^0.11.13 */
		String vscode = "^0.11.13"
		/** VS Code Language Client version. Default: ^2.3.0 */
		String vscodeLanguageclient = "^2.3.0"
		/** Node version. Default: 6.2.2. */
		String node = "6.2.2"
		/** NPM version. Default: 3.10.6 */
		String npm = "3.10.6"
		/** Version for Gradle plugin org.xtext:xtext-gradle-plugin. Default: 1.0.5 */
		String xtextGradlePlugin = "1.0.5"
		/** Version for Gradle plugin com.moowork.node. Default: 0.13 */
		String nodeGradlePlugin = "0.13"
		/** Version for Gradle plugin com.github.johnrengelman.shadow. Default: 1.2.3 */
		String shadowJarGradlePlugin = "1.2.3"
		/** Version for Gradle plugin net.researchgate.release. Default: 2.4.0 */
		String releaseGradlePlugin = "2.4.0"
	}
	
	/** Publisher name */
	String publisher
	@Mandatory
	def void setPublisher (String publisher) {
		this.publisher = publisher
	}

	/** Extension License */	
	@Accessors(PUBLIC_SETTER)
	String license
	
	@Accessors(PUBLIC_SETTER)
	Versions versions = new Versions
	
	/** Additional options for the JVM */
	@Accessors(PUBLIC_SETTER)
	String javaOptions

	/** 
	 * If set, add Java remote debugging options to the JVM. 
	 * When 'javaOptions' are set, the debug options are set before the other options.
	 */
	Integer debugPort
	def void setDebugPort (String debugPort) {
		this.debugPort = Integer.valueOf(debugPort)
	}
	
	/** Name of the Language Server. Is used as the name of the output console. */
	@Accessors(PUBLIC_SETTER)
	String languageServerName

	/**
	 * Regular expression for filtering those language keywords that should be highlighted. The default
	 * is {@code \w+}, i.e. keywords consisting only of letters and digits.
	 */
	@Accessors(PUBLIC_SETTER)
	String keywordsFilter = "\\w+"
	
	/**
	 * If set to true, the fragment produces a <code>.travis.yml</code> and <code>.travis-publishOnRelease.sh</code> file.
	 */
	@Accessors(PUBLIC_SETTER)
	boolean useTravis = false

	override generate() {
		val langId = langNameLower
		generateIgnoreFiles (langId, language.fileExtensions)
		generatePackageJson (langId, language.fileExtensions)
		generateConfigurationJson
		generateTmLanguage (langId, language.fileExtensions)
		generateExtensionJs (langId, language.fileExtensions)
		generateGradleProperties
		generateBuildGradle (langId)
		generateReadMe (langId)
		generateLicense (langId)
		if (useTravis) {
			generateTravis()
		}
	}
	
	/**
	 * Generates .gitignore and .vscodeignore
	 */
	protected def generateIgnoreFiles (String langId, String[] langFileExt) {
		val gitignore = fileAccessFactory.createTextFile(vscodeExtensionPath+"/.gitignore")
		gitignore.content = '''
			*.jar
			*.vsix
			.vscode
			node_modules
		'''
		gitignore.writeTo(projectConfig.genericIde.root)
		
		val vscodeignore = fileAccessFactory.createTextFile(vscodeExtensionPath+"/.vscodeignore")
		vscodeignore.content = '''
			.gitignore
			.gradle/**
			build/**
			*.gradle
			gradle.properties
		'''
		vscodeignore.writeTo(projectConfig.genericIde.root)
	}
	
	protected def generatePackageJson (String langId, String[] langFileExt) {
		val file = fileAccessFactory.createTextFile(vscodeExtensionPath+"/package.json")
		file.content = '''
			{
				"name": "«langId»",
				"displayName": "«langName»",
				"description": "«langName» Language",
				"version": "«versions.vscExtension»",
				"publisher": "«publisher»",
				«IF license!==null»
						"license": "«license»",
					«ENDIF»
				"engines": {
					"vscode": "«versions.vscEngine»"
				},
				"categories": [
					"Languages"
				],
				"activationEvents": [
					"onLanguage:«langId»"
				],
				"main": "src/extension",
				"contributes": {
					"languages": [{
						"id": "«langId»",
						"aliases": ["«langId»"],
						"extensions": [".«FOR ext: langFileExt SEPARATOR ","»«ext»«ENDFOR»"],
						"configuration": "./«langId».configuration.json"
					}],
					"grammars": [{
						"language": "«langId»",
						"scopeName": "text.«langId»",
						"path": "./syntaxes/«langId».tmLanguage"
					}]
				},
				"devDependencies": {
					"typescript": "«versions.typescript»",
					"vscode": "«versions.vscode»"
				},
				"dependencies": {
					"vscode-languageclient": "«versions.vscodeLanguageclient»"
				}
			}
		'''
		file.writeTo(projectConfig.genericIde.root)
	}
	
	def protected getVscodeExtensionPath() {
		"vscode"
	}
	
	protected def generateConfigurationJson () {
		val file = fileAccessFactory.createTextFile(vscodeExtensionPath+"/"+langNameLower+".configuration.json")
		val inheritsTerminals = grammar.inherits(TERMINALS)
		file.content = '''
			{
				"comments": {
						«IF inheritsTerminals»
						// symbol used for single line comment. Remove this entry if your language does not support line comments
						"lineComment": "//",
						// symbols used for start and end a block comment. Remove this entry if your language does not support block comments
						"blockComment": [ "/*", "*/" ]
					«ENDIF»
				},
				// symbols used as brackets
				"brackets": [
					["{", "}"],
					["[", "]"],
					["(", ")"]
				],
				// symbols that are auto closed when typing
				"autoClosingPairs": [
					["{", "}"],
					["[", "]"],
					["(", ")"],
					["\"", "\""],
					["'", "'"]
				],
				// symbols that that can be used to surround a selection
				"surroundingPairs": [
					["{", "}"],
					["[", "]"],
					["(", ")"],
					["\"", "\""],
					["'", "'"]
				]
			}
		'''
		file.writeTo(projectConfig.genericIde.root)
	}
	
	def protected generateTmLanguage (String langId, String[] langFileExt) {
		val file = fileAccessFactory.createTextFile(vscodeExtensionPath+"/syntaxes/"+langId+".tmLanguage")
		file.content = '''
			<?xml version="1.0" encoding="UTF-8"?>
			<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
			<plist version="1.0">
			<dict>
				<key>fileTypes</key>
				<array>
					<string>«FOR ext: langFileExt SEPARATOR ","»*.«ext»«ENDFOR»</string>
				</array>
				<key>name</key>
				<string>«langId»</string>
				<key>patterns</key>
				<array>
					<dict>
						<key>name</key>
						<string>keyword.control.«langId»</string>
						<key>match</key>
						<string>\b(«keywordPattern»)\b</string>
					</dict>
				</array>
				<key>scopeName</key>
				<string>text.«langId»</string>
			</dict>
			</plist>
		'''
		file.writeTo(projectConfig.genericIde.root)
	}
	
	
	protected def generateExtensionJs (String langId, String[] langFileExt) {
		val file = fileAccessFactory.createTextFile(vscodeExtensionPath+"/src/extension.js")
		val jvmOptions = getJVMOptions()
		file.content = '''
			'use strict';
			var net = require('net');
			var path = require('path');
			var vscode_lc = require('vscode-languageclient');
			var spawn = require('child_process').spawn;
			function activate(context) {
				var serverInfo = function () {
					// Connect to the language server via a io channel
					var jar = context.asAbsolutePath(path.join('src', '«langId»-uber.jar'));
					var child = spawn('java', [«FOR opt: jvmOptions»'«opt»',«ENDFOR»'-jar', jar]);
					child.stdout.on('data', function (chunk) {
						console.log(chunk.toString());
					});
					child.stderr.on('data', function (chunk) {
						console.error(chunk.toString());
					});
					return Promise.resolve(child);
				};
				var clientOptions = {
					documentSelector: ['«langId»']
				};
				// Create the language client and start the client.
				var disposable = new vscode_lc.LanguageClient('«IF languageServerName!==null»«languageServerName»«ELSE»«langName»«ENDIF»', serverInfo, clientOptions).start();
				// Push the disposable to the context's subscriptions so that the 
				// client can be deactivated on extension deactivation
				context.subscriptions.push(disposable);
			}
			exports.activate = activate;
		'''
		file.writeTo(projectConfig.genericIde.root)		
	}
	
	/**
	 * Compute the JVM options line.
	 */
	def private List<String> getJVMOptions () {
		val options = newArrayList()
		if (debugPort !== null) {
			options.add("-Xdebug")
			options.add("-Xrunjdwp:server=y,transport=dt_socket,address="+debugPort+",suspend=n,quiet=y")
		}
		if (javaOptions !== null) {
			javaOptions.split("\\s").forEach[options += it]
		}
		
		return options
	}
	
	protected def generateGradleProperties () {
		val file = fileAccessFactory.createTextFile(vscodeExtensionPath+"/gradle.properties")
		file.content = '''
			version = «versions.vscExtension»
		'''
		file.writeTo(projectConfig.genericIde.root)
	}
	
	/**
	 * Produces <code>build.gradle</code> file.
	 */
	protected def generateBuildGradle (String langId) {
		val file = fileAccessFactory.createTextFile(vscodeExtensionPath+"/build.gradle")
		file.content = '''
			buildscript {
				repositories {
					jcenter()
				}
				dependencies {
					classpath 'org.xtext:xtext-gradle-plugin:«versions.xtextGradlePlugin»'
					classpath 'com.moowork.gradle:gradle-node-plugin:«versions.nodeGradlePlugin»'
				}
			}
			
			plugins {
				id 'com.github.johnrengelman.shadow' version '«versions.shadowJarGradlePlugin»'
				id 'com.moowork.node' version '«versions.nodeGradlePlugin»'
				id 'net.researchgate.release' version '«versions.releaseGradlePlugin»'
			}
			
			node {
				version = '«versions.node»'
				npmVersion = '«versions.npm»'
				download = true
				workDir = file("${project.buildDir}/nodejs")
				nodeModulesDir = file("${project.projectDir}")
			}
			
			apply plugin: 'java'
			apply plugin: 'com.moowork.node'
			
			ext.xtextVersion = '«versions.xtext»'
			
			repositories {
				jcenter()
				«IF useSnapshotRepositories»
				maven { url 'http://services.typefox.io/open-source/jenkins/job/lsapi/lastStableBuild/artifact/build/maven-repository/' }
				maven { url 'http://services.typefox.io/open-source/jenkins/job/xtext-lib/job/master/lastStableBuild/artifact/build/maven-repository/' }
				maven { url 'http://services.typefox.io/open-source/jenkins/job/xtext-core/job/master/lastStableBuild/artifact/build/maven-repository/' }
				maven { url 'http://services.typefox.io/open-source/jenkins/job/xtext-extras/job/master/lastStableBuild/artifact/build/maven-repository/' }
				maven { url 'http://services.typefox.io/open-source/jenkins/job/xtext-xtend/job/master/lastStableBuild/artifact/build/maven-repository/' }
				maven {
					url 'https://oss.sonatype.org/content/repositories/snapshots'
				}
				«ENDIF»
				mavenLocal()
			}
			
			dependencies {
				compile ("«projectConfig.runtime.name»:«projectConfig.genericIde.name»:+") {
					exclude group:'org.antlr', module:'stringtemplate'
					exclude group:'com.ibm.icu', module:'icu4j'
					exclude group:'com.itemis.xtext', module:'generator-vscode'
				}
			}
			
			task packageShadowJar(type: com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar, dependsOn: assemble) {
				manifest.attributes 'Main-Class': 'org.eclipse.xtext.ide.server.ServerLauncher'
				from(project.convention.getPlugin(JavaPluginConvention).sourceSets.main.output)
				configurations = [project.configurations.runtime]
				exclude('META-INF/INDEX.LIST', 'META-INF/*.SF', 'META-INF/*.DSA', 'META-INF/*.RSA')
				baseName = '«langId»-uber'
				classifier = null
				version = null
				destinationDir = file("$projectDir/src")
				append('plugin.properties')
			}
			
			clean.doFirst {
				delete tasks.packageShadowJar.archivePath
			}
			
			task npmInstallVsce(type: NpmTask, dependsOn: npmSetup) {
				group 'Node'
				description 'Installs the NodeJS package "Visual Studio Code Extension Manager"'
				args = [ 'install', 'vsce' ]
			}
			
			npmInstall.dependsOn 'packageShadowJar'
			
			def vsce = file("$node.nodeModulesDir.path/node_modules/vsce/out/vsce")
			
			task vscodeExtension(type: Exec, dependsOn: [nodeSetup,npmInstall, npmInstallVsce]) {
				ext.destDir = new File(buildDir, 'vscode')
				ext.archiveName = "«langId»-${project.version}.vsix"
				ext.destPath = "$destDir/$archiveName"
			
				workingDir projectDir
				doFirst {
					destDir.mkdirs()
					commandLine nodeSetup.variant.nodeExec, vsce, 'package', '--out', destPath
					//commandLine 'cmd.exe','/k','xxx.bat'
				}
			}
			
			plugins.withType(com.moowork.gradle.node.NodePlugin) {
				node {
					workDir = file("$project.buildDir/nodejs")
					nodeModulesDir = projectDir
				}
			}
			
			task installExtension(type: Exec, dependsOn: vscodeExtension) {
				if (System.getProperty('os.name').toLowerCase().contains('windows')) {
					commandLine 'cmd', '/c', 'code.cmd', '--install-extension', vscodeExtension.destPath
				} else {
					commandLine 'code', '--install-extension', vscodeExtension.destPath
				}
			}
			
			task startCode(type:Exec, dependsOn: installExtension) {
				if (System.getProperty('os.name').toLowerCase().contains('windows')) {
					commandLine 'cmd', '/k', 'code.cmd', '--new-window'
				} else {
					commandLine 'code', '--new-window'
				}
			}
			
			task publish(dependsOn: vscodeExtension, type: NodeTask) {
				script = file("$projectDir/node_modules/vsce/out/vsce")
				args = [ 'publish', '-p', System.getenv('ACCESS_TOKEN'), project.version ]
				execOverrides {
					workingDir = projectDir
				}
			}
		'''
		file.writeTo(projectConfig.genericIde.root)
	}

	protected def generateReadMe (String langId) {
		val file = fileAccessFactory.createTextFile(vscodeExtensionPath+"/README.md")
		file.content = '''
			# «langName» Language
			
			This extension integrates the «langName» Language into Visual Studio Code.
			
			Supports:
			
			* Syntax Coloring
			* Content Assist
			* Go To Definition
			
			The language is integrated by a separate java process via the Language Server Protocol.
		'''
		file.writeTo(projectConfig.genericIde.root)
	}
	
	protected def generateLicense (String langId) {
		val file = fileAccessFactory.createTextFile(vscodeExtensionPath+"/LICENSE.txt")
		file.content = '''
			«licenseText»
		'''
		file.writeTo(projectConfig.genericIde.root)
	}
	
	protected def generateTravis () {
		val travis_yml = fileAccessFactory.createTextFile(vscodeExtensionPath+"/.travis.yml")
		travis_yml.content = '''
			language: java
			jdk:
			  - oraclejdk8
			os:
			  - linux
			cache:
			  directories:
			  - $HOME/.gradle
			env:
			  - NODE_VERSION=6.1
			install:
			  - nvm install $NODE_VERSION
			  - npm install
			script:
			  - nvm use $NODE_VERSION
			  - ./gradlew vscodeExtension --refresh-dependencies
			after_success:
			  - ./.travis-publishOnRelease.sh
		'''
		travis_yml.writeTo(projectConfig.genericIde.root)
		
		val travis_publishOnRelease_sh = fileAccessFactory.createTextFile(vscodeExtensionPath+"/.travis-publishOnRelease.sh")
		travis_publishOnRelease_sh.content = '''
			#!/bin/bash
			# Execute only on tag builds where the tag starts with 'v'
			
			if [[ -n "$TRAVIS_TAG" && "$TRAVIS_TAG" == v* ]]; then
				echo "Publishing version: $TRAVIS_TAG"
				./gradlew publish
			fi
		'''
		travis_publishOnRelease_sh.writeTo(projectConfig.genericIde.root)
	}
	
	def protected isUseSnapshotRepositories() {
		versions.xtext.endsWith("-SNAPSHOT")
	}
	
	@Pure
	def getLangName () {
		grammar.name.toQualifiedName.lastSegment
	}

	@Pure
	def getLangNameLower () {
		grammar.name.toQualifiedName.lastSegment.toLowerCase
	}

	def protected String getKeywordPattern() {
		val allKeywords = grammar.allKeywords
		val wordKeywords = newArrayList
		val nonWordKeywords = newArrayList
		val keywordsFilterPattern = Pattern.compile(keywordsFilter)
		val wordKeywordPattern = Pattern.compile('\\w(.*\\w)?')
		allKeywords.filter[keywordsFilterPattern.matcher(it).matches].forEach[
			if (wordKeywordPattern.matcher(it).matches)
				wordKeywords += it
			else
				nonWordKeywords += it
		]
		Collections.sort(wordKeywords)
		Collections.sort(nonWordKeywords)

		val result = (wordKeywords+nonWordKeywords).map[it.toRegexpString(false)].join('|')
		result
	}

	def protected getLicenseText () {
		if (license===null) return ""
		license
	}
}