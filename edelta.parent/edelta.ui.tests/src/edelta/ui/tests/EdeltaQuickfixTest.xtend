/*******************************************************************************
 * Copyright (c) 2018 itemis AG (http://www.itemis.eu) and others.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package edelta.ui.tests

import edelta.validation.EdeltaValidator
import org.eclipse.xtext.testing.InjectWith
import org.eclipse.xtext.testing.XtextRunner
import org.eclipse.xtext.ui.XtextProjectHelper
import org.eclipse.xtext.ui.testing.AbstractQuickfixTest
import org.eclipse.xtext.ui.testing.util.IResourcesSetupUtil
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

import static extension org.eclipse.xtext.ui.testing.util.IResourcesSetupUtil.addNature
import static extension org.eclipse.xtext.ui.testing.util.JavaProjectSetupUtil.createJavaProject

/**
 * @author miklossy - Initial contribution and API
 */
@RunWith(XtextRunner)
@InjectWith(EdeltaUiInjectorProvider)
class EdeltaQuickfixTest extends AbstractQuickfixTest {

	@Before def void setup() {
		/*
		 * Xbase-based languages require java project
		 */
		projectName.createJavaProject
	}

	@Test def fixSubPackageImport() {
		val ecoreFile = IResourcesSetupUtil
			.createFile(
				projectName,
				"src/MySubPackages", "ecore",
				'''
				<?xml version="1.0" encoding="UTF-8"?>
				<ecore:EPackage xmi:version="2.0" xmlns:xmi="http://www.omg.org/XMI" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
				    xmlns:ecore="http://www.eclipse.org/emf/2002/Ecore" name="mainpackage" nsURI="http://my.mainpackage.org" nsPrefix="mainpackage">
				  <eClassifiers xsi:type="ecore:EClass" name="MyClass">
				    <eStructuralFeatures xsi:type="ecore:EAttribute" name="myAttribute" eType="ecore:EDataType http://www.eclipse.org/emf/2002/Ecore#//EString"/>
				    <eStructuralFeatures xsi:type="ecore:EReference" name="myReference" eType="ecore:EClass http://www.eclipse.org/emf/2002/Ecore#//EObject"/>
				  </eClassifiers>
				  <eSubpackages name="subpackage" nsURI="http://mysubpackage" nsPrefix="subpackage">
				    <eClassifiers xsi:type="ecore:EClass" name="MySubPackageClass"/>
				    <eClassifiers xsi:type="ecore:EClass" name="MyClass">
				      <eStructuralFeatures xsi:type="ecore:EAttribute" name="myAttribute" eType="ecore:EDataType http://www.eclipse.org/emf/2002/Ecore#//EString"/>
				      <eStructuralFeatures xsi:type="ecore:EReference" name="myReference" eType="ecore:EClass http://www.eclipse.org/emf/2002/Ecore#//EObject"/>
				    </eClassifiers>
				    <eSubpackages name="subsubpackage" nsURI="http://mysubsubpackage" nsPrefix="subsubpackage">
				      <eClassifiers xsi:type="ecore:EClass" name="MyClass">
				        <eStructuralFeatures xsi:type="ecore:EAttribute" name="myAttribute" eType="ecore:EDataType http://www.eclipse.org/emf/2002/Ecore#//EString"/>
				        <eStructuralFeatures xsi:type="ecore:EReference" name="myReference" eType="ecore:EClass http://www.eclipse.org/emf/2002/Ecore#//EObject"/>
				      </eClassifiers>
				    </eSubpackages>
				  </eSubpackages>
				</ecore:EPackage>
				'''
			)
		val project = ecoreFile.project
		if(!project.hasNature(XtextProjectHelper.NATURE_ID)) {
			project.addNature(XtextProjectHelper.NATURE_ID)
		}
		'''
			metamodel "mainpackage.subpackage"
		'''.testQuickfixesOn
		(EdeltaValidator.INVALID_SUBPACKAGE_IMPORT,
			new Quickfix("Import root EPackage",
			"Import root EPackage 'mainpackage'", '''
			metamodel "mainpackage"
		'''))
	}

}
