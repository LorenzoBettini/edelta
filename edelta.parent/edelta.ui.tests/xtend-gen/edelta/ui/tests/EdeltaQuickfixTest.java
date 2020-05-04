/**
 * Copyright (c) 2018 itemis AG (http://www.itemis.eu) and others.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 * 
 * SPDX-License-Identifier: EPL-2.0
 */
package edelta.ui.tests;

import edelta.ui.tests.EdeltaUiInjectorProvider;
import edelta.validation.EdeltaValidator;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.xtend2.lib.StringConcatenation;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.XtextRunner;
import org.eclipse.xtext.ui.XtextProjectHelper;
import org.eclipse.xtext.ui.testing.AbstractQuickfixTest;
import org.eclipse.xtext.ui.testing.util.IResourcesSetupUtil;
import org.eclipse.xtext.ui.testing.util.JavaProjectSetupUtil;
import org.eclipse.xtext.xbase.lib.Exceptions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author miklossy - Initial contribution and API
 */
@RunWith(XtextRunner.class)
@InjectWith(EdeltaUiInjectorProvider.class)
@SuppressWarnings("all")
public class EdeltaQuickfixTest extends AbstractQuickfixTest {
  @Before
  public void setup() {
    try {
      JavaProjectSetupUtil.createJavaProject(this.getProjectName());
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }
  
  @Test
  public void fixSubPackageImport() {
    try {
      String _projectName = this.getProjectName();
      StringConcatenation _builder = new StringConcatenation();
      _builder.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
      _builder.newLine();
      _builder.append("<ecore:EPackage xmi:version=\"2.0\" xmlns:xmi=\"http://www.omg.org/XMI\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"");
      _builder.newLine();
      _builder.append("    ");
      _builder.append("xmlns:ecore=\"http://www.eclipse.org/emf/2002/Ecore\" name=\"mainpackage\" nsURI=\"http://my.mainpackage.org\" nsPrefix=\"mainpackage\">");
      _builder.newLine();
      _builder.append("  ");
      _builder.append("<eClassifiers xsi:type=\"ecore:EClass\" name=\"MyClass\">");
      _builder.newLine();
      _builder.append("    ");
      _builder.append("<eStructuralFeatures xsi:type=\"ecore:EAttribute\" name=\"myAttribute\" eType=\"ecore:EDataType http://www.eclipse.org/emf/2002/Ecore#//EString\"/>");
      _builder.newLine();
      _builder.append("    ");
      _builder.append("<eStructuralFeatures xsi:type=\"ecore:EReference\" name=\"myReference\" eType=\"ecore:EClass http://www.eclipse.org/emf/2002/Ecore#//EObject\"/>");
      _builder.newLine();
      _builder.append("  ");
      _builder.append("</eClassifiers>");
      _builder.newLine();
      _builder.append("  ");
      _builder.append("<eSubpackages name=\"subpackage\" nsURI=\"http://mysubpackage\" nsPrefix=\"subpackage\">");
      _builder.newLine();
      _builder.append("    ");
      _builder.append("<eClassifiers xsi:type=\"ecore:EClass\" name=\"MySubPackageClass\"/>");
      _builder.newLine();
      _builder.append("    ");
      _builder.append("<eClassifiers xsi:type=\"ecore:EClass\" name=\"MyClass\">");
      _builder.newLine();
      _builder.append("      ");
      _builder.append("<eStructuralFeatures xsi:type=\"ecore:EAttribute\" name=\"myAttribute\" eType=\"ecore:EDataType http://www.eclipse.org/emf/2002/Ecore#//EString\"/>");
      _builder.newLine();
      _builder.append("      ");
      _builder.append("<eStructuralFeatures xsi:type=\"ecore:EReference\" name=\"myReference\" eType=\"ecore:EClass http://www.eclipse.org/emf/2002/Ecore#//EObject\"/>");
      _builder.newLine();
      _builder.append("    ");
      _builder.append("</eClassifiers>");
      _builder.newLine();
      _builder.append("    ");
      _builder.append("<eSubpackages name=\"subsubpackage\" nsURI=\"http://mysubsubpackage\" nsPrefix=\"subsubpackage\">");
      _builder.newLine();
      _builder.append("      ");
      _builder.append("<eClassifiers xsi:type=\"ecore:EClass\" name=\"MyClass\">");
      _builder.newLine();
      _builder.append("        ");
      _builder.append("<eStructuralFeatures xsi:type=\"ecore:EAttribute\" name=\"myAttribute\" eType=\"ecore:EDataType http://www.eclipse.org/emf/2002/Ecore#//EString\"/>");
      _builder.newLine();
      _builder.append("        ");
      _builder.append("<eStructuralFeatures xsi:type=\"ecore:EReference\" name=\"myReference\" eType=\"ecore:EClass http://www.eclipse.org/emf/2002/Ecore#//EObject\"/>");
      _builder.newLine();
      _builder.append("      ");
      _builder.append("</eClassifiers>");
      _builder.newLine();
      _builder.append("    ");
      _builder.append("</eSubpackages>");
      _builder.newLine();
      _builder.append("  ");
      _builder.append("</eSubpackages>");
      _builder.newLine();
      _builder.append("</ecore:EPackage>");
      _builder.newLine();
      final IFile ecoreFile = IResourcesSetupUtil.createFile(_projectName, 
        "src/MySubPackages", "ecore", _builder.toString());
      final IProject project = ecoreFile.getProject();
      boolean _hasNature = project.hasNature(XtextProjectHelper.NATURE_ID);
      boolean _not = (!_hasNature);
      if (_not) {
        IResourcesSetupUtil.addNature(project, XtextProjectHelper.NATURE_ID);
      }
      StringConcatenation _builder_1 = new StringConcatenation();
      _builder_1.append("metamodel \"mainpackage.subpackage\"");
      _builder_1.newLine();
      StringConcatenation _builder_2 = new StringConcatenation();
      _builder_2.append("metamodel \"mainpackage\"");
      _builder_2.newLine();
      AbstractQuickfixTest.Quickfix _quickfix = new AbstractQuickfixTest.Quickfix("Import root EPackage", 
        "Import root EPackage \'mainpackage\'", _builder_2.toString());
      this.testQuickfixesOn(_builder_1, EdeltaValidator.INVALID_SUBPACKAGE_IMPORT, _quickfix);
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }
}
